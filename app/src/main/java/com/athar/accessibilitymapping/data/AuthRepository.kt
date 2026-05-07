package com.athar.accessibilitymapping.data

import android.content.Context
import com.athar.accessibilitymapping.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(
  context: Context,
  private val api: BackendApiClient = BackendApiClient(context.applicationContext),
  private val sessionStore: AuthSessionStore = AuthSessionStore(context.applicationContext),
  private val localAccounts: LocalAccountStore = LocalAccountStore(context.applicationContext),
  private val appPreferences: AppPreferencesStore = AppPreferencesStore(context.applicationContext)
) {
  suspend fun login(email: String, password: String): AuthOperationResult = withContext(Dispatchers.IO) {
    when (val result = api.login(email, password)) {
      is ApiCallResult.Success -> {
        val session = saveRemoteSession(result.data)
        appPreferences.readOrCreateMemberSince()
        AuthOperationResult.Success(session)
      }
      is ApiCallResult.Failure -> {
        if (result.statusCode == 401) {
          return@withContext AuthOperationResult.Error("Email or password is wrong.")
        }
        if (shouldAllowOfflineAuthFallback(result)) {
          return@withContext when (val fallback = localAccounts.login(email, password)) {
            is AuthOperationResult.Success -> {
              saveLocalSession(fallback.session)
              fallback
            }
            is AuthOperationResult.Error -> fallback
          }
        }
        AuthOperationResult.Error(resolveAuthFailureMessage(result.message))
      }
    }
  }

  suspend fun registerUser(payload: UserRegistrationPayload): RegistrationOperationResult = withContext(Dispatchers.IO) {
    when (val result = api.registerUser(payload)) {
      is ApiCallResult.Success -> {
        when (val response = result.data) {
          is ApiRegistrationStartResponse.Authenticated -> {
            val auth = response.auth
            val session = saveRemoteSession(auth)
            appPreferences.saveProfileLocation(payload.location.trim())
            appPreferences.readOrCreateMemberSince()
            if (auth.tokens.accessToken.isNotBlank()) {
              api.patchProfile(
                accessToken = auth.tokens.accessToken,
                request = ApiUpdateProfileRequest(
                  fullName = payload.fullName,
                  phone = payload.phone,
                  location = payload.location,
                  disabilityType = payload.disabilityType
                )
              )
            }
            RegistrationOperationResult.Authenticated(session)
          }
          is ApiRegistrationStartResponse.VerificationRequired -> {
            RegistrationOperationResult.VerificationRequired(response.challenge.toDomainModel())
          }
        }
      }
      is ApiCallResult.Failure -> {
        RegistrationOperationResult.Error(resolveRegistrationFailureMessage(result))
      }
    }
  }

  suspend fun registerVolunteer(payload: VolunteerRegistrationPayload): RegistrationOperationResult = withContext(Dispatchers.IO) {
    when (val result = api.registerVolunteer(payload)) {
      is ApiCallResult.Success -> {
        when (val response = result.data) {
          is ApiRegistrationStartResponse.Authenticated -> {
            val auth = response.auth
            if (auth.user.role != ApiUserRole.Volunteer) {
              return@withContext RegistrationOperationResult.Error(
                "Volunteer registration requires admin activation. Your account was created, but is not yet a volunteer account."
              )
            }
            val session = saveRemoteSession(auth)
            appPreferences.saveProfileLocation(payload.location.trim())
            appPreferences.readOrCreateMemberSince()
            RegistrationOperationResult.Authenticated(session)
          }
          is ApiRegistrationStartResponse.VerificationRequired -> {
            RegistrationOperationResult.VerificationRequired(response.challenge.toDomainModel())
          }
        }
      }
      is ApiCallResult.Failure -> {
        RegistrationOperationResult.Error(resolveRegistrationFailureMessage(result))
      }
    }
  }

  suspend fun verifyEmailChallenge(challengeId: String, code: String): AuthOperationResult = withContext(Dispatchers.IO) {
    when (val result = api.verifyEmailChallenge(challengeId, code)) {
      is ApiCallResult.Success -> {
        val session = saveRemoteSession(result.data)
        appPreferences.saveProfileLocation(result.data.user.location.trim())
        appPreferences.readOrCreateMemberSince()
        AuthOperationResult.Success(session)
      }
      is ApiCallResult.Failure -> {
        AuthOperationResult.Error(resolveVerificationFailureMessage(result))
      }
    }
  }

  suspend fun resendEmailChallenge(challengeId: String): ApiCallResult<EmailVerificationChallenge> = withContext(Dispatchers.IO) {
    when (val result = api.resendEmailChallenge(challengeId)) {
      is ApiCallResult.Success -> ApiCallResult.Success(result.data.toDomainModel())
      is ApiCallResult.Failure -> ApiCallResult.Failure(
        message = resolveVerificationFailureMessage(result),
        statusCode = result.statusCode,
        validationField = result.validationField,
        validationErrors = result.validationErrors
      )
    }
  }

  suspend fun restoreSession(): AuthSession? = withContext(Dispatchers.IO) {
    sessionStore.readAuthSession()
  }

  suspend fun hasLocalSession(): Boolean = withContext(Dispatchers.IO) {
    sessionStore.getAccessToken()?.startsWith("local-") == true
  }

  suspend fun isBackendReachable(): Boolean = withContext(Dispatchers.IO) {
    api.isBackendReachable()
  }

  suspend fun clearStoredSession() = withContext(Dispatchers.IO) {
    sessionStore.clearSession()
  }

  suspend fun logout() = withContext(Dispatchers.IO) {
    val accessToken = sessionStore.getAccessToken()
    val refreshToken = sessionStore.getRefreshToken()
    if (!accessToken.isNullOrBlank()) {
      api.logout(accessToken = accessToken, refreshToken = refreshToken)
    }
    sessionStore.clearSession()
  }

  private fun ApiEmailVerificationChallenge.toDomainModel(): EmailVerificationChallenge {
    return EmailVerificationChallenge(
      challengeId = challengeId,
      email = email,
      role = when (role) {
        ApiUserRole.Volunteer -> UserRole.Volunteer
        ApiUserRole.User -> UserRole.User
      },
      expiresAtEpochSeconds = expiresAtEpochSeconds,
      resendAvailableAtEpochSeconds = resendAvailableAtEpochSeconds,
      codeLength = codeLength,
      message = message
    )
  }

  private suspend fun saveRemoteSession(authResponse: ApiAuthResponse): AuthSession {
    val session = authResponse.toAuthSession()
    sessionStore.saveSession(
      userId = session.userId,
      accessToken = authResponse.tokens.accessToken,
      refreshToken = authResponse.tokens.refreshToken,
      expiresAtEpochSeconds = authResponse.tokens.expiresAtEpochSeconds,
      role = session.role,
      fullName = session.fullName,
      email = session.email,
      phone = session.phone,
      disabilityType = session.disabilityType,
      volunteerLive = session.volunteerLive
    )
    return session
  }

  private suspend fun saveLocalSession(session: AuthSession) {
    val localToken = "local-${session.userId}"
    sessionStore.saveSession(
      userId = session.userId,
      accessToken = localToken,
      refreshToken = localToken,
      expiresAtEpochSeconds = Long.MAX_VALUE,
      role = session.role,
      fullName = session.fullName,
      email = session.email,
      phone = session.phone,
      disabilityType = session.disabilityType,
      volunteerLive = session.volunteerLive
    )
  }

  private suspend fun shouldAllowOfflineAuthFallback(result: ApiCallResult.Failure): Boolean {
    if (result.statusCode != null) return false
    val configuredBaseUrl = BuildConfig.BACKEND_BASE_URL.trim().lowercase()
    if ("127.0.0.1" in configuredBaseUrl || "localhost" in configuredBaseUrl) {
      return false
    }
    return !api.isBackendReachable()
  }

  private fun resolveRegistrationFailureMessage(result: ApiCallResult.Failure): String {
    return if (result.statusCode == null && shouldExplainVerificationConnectivity()) {
      "Email verification requires a live backend connection. Check your internet and try again."
    } else {
      resolveAuthFailureMessage(result.message)
    }
  }

  private fun resolveVerificationFailureMessage(result: ApiCallResult.Failure): String {
    return if (result.statusCode == null && shouldExplainVerificationConnectivity()) {
      "Email verification requires a live backend connection. Check your internet and try again."
    } else {
      result.message
    }
  }

  private fun shouldExplainVerificationConnectivity(): Boolean {
    val configuredBaseUrl = BuildConfig.BACKEND_BASE_URL.trim().lowercase()
    return !("127.0.0.1" in configuredBaseUrl || "localhost" in configuredBaseUrl)
  }

  private fun resolveAuthFailureMessage(defaultMessage: String): String {
    val configuredBaseUrl = BuildConfig.BACKEND_BASE_URL.trim().lowercase()
    return if ("127.0.0.1" in configuredBaseUrl || "localhost" in configuredBaseUrl) {
      "Cannot reach the local backend. Keep the backend running and launch the app with run-app.bat."
    } else {
      defaultMessage
    }
  }
}
