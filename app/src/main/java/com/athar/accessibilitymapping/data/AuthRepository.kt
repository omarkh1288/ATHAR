package com.athar.accessibilitymapping.data

import android.content.Context
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
        if (result.statusCode == null) {
          return@withContext when (val fallback = localAccounts.login(email, password)) {
            is AuthOperationResult.Success -> {
              saveLocalSession(fallback.session)
              fallback
            }
            is AuthOperationResult.Error -> fallback
          }
        }
        AuthOperationResult.Error(result.message)
      }
    }
  }

  suspend fun registerUser(payload: UserRegistrationPayload): AuthOperationResult = withContext(Dispatchers.IO) {
    when (val result = api.registerUser(payload)) {
      is ApiCallResult.Success -> {
        val auth = result.data
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
        AuthOperationResult.Success(session)
      }
      is ApiCallResult.Failure -> {
        if (result.statusCode == null) {
          return@withContext when (val fallback = localAccounts.registerUser(payload)) {
            is AuthOperationResult.Success -> {
              saveLocalSession(fallback.session)
              fallback
            }
            is AuthOperationResult.Error -> fallback
          }
        }
        AuthOperationResult.Error(result.message)
      }
    }
  }

  suspend fun registerVolunteer(payload: VolunteerRegistrationPayload): AuthOperationResult = withContext(Dispatchers.IO) {
    when (val result = api.registerVolunteer(payload)) {
      is ApiCallResult.Success -> {
        val auth = result.data
        if (auth.user.role != ApiUserRole.Volunteer) {
          return@withContext AuthOperationResult.Error(
            "Volunteer registration requires admin activation. Your account was created, but is not yet a volunteer account."
          )
        }
        val session = saveRemoteSession(auth)
        appPreferences.saveProfileLocation(payload.location.trim())
        appPreferences.readOrCreateMemberSince()
        AuthOperationResult.Success(session)
      }
      is ApiCallResult.Failure -> {
        if (result.statusCode == null) {
          return@withContext when (val fallback = localAccounts.registerVolunteer(payload)) {
            is AuthOperationResult.Success -> {
              saveLocalSession(fallback.session)
              fallback
            }
            is AuthOperationResult.Error -> fallback
          }
        }
        AuthOperationResult.Error(result.message)
      }
    }
  }

  suspend fun restoreSession(): AuthSession? = withContext(Dispatchers.IO) {
    sessionStore.readAuthSession()
  }

  suspend fun logout() = withContext(Dispatchers.IO) {
    val accessToken = sessionStore.getAccessToken()
    val refreshToken = sessionStore.getRefreshToken()
    if (!accessToken.isNullOrBlank()) {
      api.logout(accessToken = accessToken, refreshToken = refreshToken)
    }
    sessionStore.clearSession()
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
}
