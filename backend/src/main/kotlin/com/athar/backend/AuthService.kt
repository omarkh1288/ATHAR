package com.athar.backend

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.HttpStatusCode
import java.time.Instant
import java.security.SecureRandom
import java.util.Date
import java.util.UUID
import org.mindrot.jbcrypt.BCrypt

internal class AuthService(private val store: InMemoryStore) {
  private val issuer = "athar-backend"
  private val audience = "athar-mobile"
  private val realmName = "athar-api"
  private val jwtSecret = System.getenv("ATHAR_JWT_SECRET")?.takeIf { it.isNotBlank() }
    ?: System.getProperty("athar.jwt.secret")?.takeIf { it.isNotBlank() }
    ?: "athar-dev-local-secret"
  private val algorithm = Algorithm.HMAC256(jwtSecret)
  private val accessTokenTtlSeconds = 60L * 60L
  private val refreshTokenTtlSeconds = 60L * 60L * 24L * 30L
  private val emailOtpSender = EmailOtpSender()
  private val otpCodeGenerator = SecureRandom()
  private val otpCodeLength = 6
  private val otpTtlSeconds = 10L * 60L
  private val otpResendCooldownSeconds = 45L
  private val otpAttemptsAllowed = 5

  val verifier: JWTVerifier = JWT.require(algorithm)
    .withIssuer(issuer)
    .withAudience(audience)
    .build()

  fun realm(): String = realmName

  fun startUserRegistration(request: RegisterUserRequest): ServiceResult<RegistrationStartResponseDto> {
    val now = Instant.now().epochSecond
    val code = generateOtpCode()
    val challengeResult = store.createPendingUserRegistration(
      request = request,
      passwordHash = BCrypt.hashpw(request.password, BCrypt.gensalt()),
      code = code,
      expiresAtEpochSeconds = now + otpTtlSeconds,
      resendAvailableAtEpochSeconds = now + otpResendCooldownSeconds,
      attemptsRemaining = otpAttemptsAllowed
    )
    return when (challengeResult) {
      is ServiceResult.Success -> {
        val challenge = challengeResult.value
        val sendResult = emailOtpSender.sendRegistrationOtp(
          email = challenge.email,
          fullName = request.fullName.trim(),
          code = code,
          expiresAtEpochSeconds = challenge.expiresAtEpochSeconds
        )
        if (sendResult.isFailure) {
          store.removePendingRegistration(challenge.challengeId)
          ServiceResult.Failure(
            HttpStatusCode.InternalServerError,
            "We couldn't send the verification email. Please try again."
          )
        } else {
          ServiceResult.Success(
            RegistrationStartResponseDto(
              message = challenge.message,
              challenge = challenge
            )
          )
        }
      }
      is ServiceResult.Failure -> challengeResult
    }
  }

  fun startVolunteerRegistration(request: RegisterVolunteerRequest): ServiceResult<RegistrationStartResponseDto> {
    val now = Instant.now().epochSecond
    val code = generateOtpCode()
    val challengeResult = store.createPendingVolunteerRegistration(
      request = request,
      passwordHash = BCrypt.hashpw(request.password, BCrypt.gensalt()),
      code = code,
      expiresAtEpochSeconds = now + otpTtlSeconds,
      resendAvailableAtEpochSeconds = now + otpResendCooldownSeconds,
      attemptsRemaining = otpAttemptsAllowed
    )
    return when (challengeResult) {
      is ServiceResult.Success -> {
        val challenge = challengeResult.value
        val sendResult = emailOtpSender.sendRegistrationOtp(
          email = challenge.email,
          fullName = request.fullName.trim(),
          code = code,
          expiresAtEpochSeconds = challenge.expiresAtEpochSeconds
        )
        if (sendResult.isFailure) {
          store.removePendingRegistration(challenge.challengeId)
          ServiceResult.Failure(
            HttpStatusCode.InternalServerError,
            "We couldn't send the verification email. Please try again."
          )
        } else {
          ServiceResult.Success(
            RegistrationStartResponseDto(
              message = challenge.message,
              challenge = challenge
            )
          )
        }
      }
      is ServiceResult.Failure -> challengeResult
    }
  }

  fun registerUser(request: RegisterUserRequest): ServiceResult<AuthResponseDto> {
    val passwordHash = BCrypt.hashpw(request.password, BCrypt.gensalt())
    return when (val result = store.registerUser(request, passwordHash)) {
      is ServiceResult.Success -> ServiceResult.Success(createAuthResponse(result.value, null))
      is ServiceResult.Failure -> result
    }
  }

  fun registerVolunteer(request: RegisterVolunteerRequest): ServiceResult<AuthResponseDto> {
    val passwordHash = BCrypt.hashpw(request.password, BCrypt.gensalt())
    return when (val result = store.registerVolunteer(request, passwordHash)) {
      is ServiceResult.Success -> ServiceResult.Success(createAuthResponse(result.value, null))
      is ServiceResult.Failure -> result
    }
  }

  fun login(request: LoginRequest): ServiceResult<AuthResponseDto> {
    return when (val result = store.authenticate(request.email, request.password)) {
      is ServiceResult.Success -> ServiceResult.Success(createAuthResponse(result.value, request.deviceName))
      is ServiceResult.Failure -> result
    }
  }

  fun refresh(refreshToken: String): ServiceResult<AuthResponseDto> {
    if (refreshToken.isBlank()) {
      return ServiceResult.Failure(HttpStatusCode.BadRequest, "Refresh token is required.")
    }
    val now = Instant.now().epochSecond
    val session = store.getSessionByRefreshToken(refreshToken)
      ?: return ServiceResult.Failure(HttpStatusCode.Unauthorized, "Refresh token is invalid.")
    if (session.revoked || session.expiresAtEpochSeconds <= now) {
      return ServiceResult.Failure(HttpStatusCode.Unauthorized, "Refresh token has expired.")
    }
    val account = store.getAccountById(session.userId)
      ?: return ServiceResult.Failure(HttpStatusCode.Unauthorized, "Account no longer exists.")

    val newRefreshToken = UUID.randomUUID().toString()
    val rotated = store.rotateRefreshToken(
      sessionId = session.id,
      newRefreshToken = newRefreshToken,
      nowEpochSeconds = now,
      newExpiresAtEpochSeconds = now + refreshTokenTtlSeconds
    ) ?: return ServiceResult.Failure(HttpStatusCode.Unauthorized, "Session is no longer valid.")

    val accessTokenExpiry = now + accessTokenTtlSeconds
    val accessToken = createAccessToken(account, rotated.id, accessTokenExpiry)
    val user = store.asUserDto(account)
    return ServiceResult.Success(
      AuthResponseDto(
        user = user,
        tokens = TokenPairDto(
          accessToken = accessToken,
          refreshToken = newRefreshToken,
          expiresAtEpochSeconds = accessTokenExpiry
        )
      )
    )
  }

  fun logout(userId: String, currentSessionId: String, request: LogoutRequest): ServiceResult<ActionResultDto> {
    val token = request.refreshToken?.trim().orEmpty()
    if (token.isNotBlank()) {
      val success = store.revokeSessionByRefreshToken(userId, token)
      return if (success) {
        ServiceResult.Success(ActionResultDto(true, "Logged out successfully."))
      } else {
        ServiceResult.Failure(HttpStatusCode.BadRequest, "Refresh token does not belong to your account.")
      }
    }

    val success = store.revokeSession(currentSessionId)
    return if (success) {
      ServiceResult.Success(ActionResultDto(true, "Logged out successfully."))
    } else {
      ServiceResult.Failure(HttpStatusCode.BadRequest, "Session already ended.")
    }
  }

  fun verifyEmail(request: VerifyEmailRequest): ServiceResult<AuthResponseDto> {
    val challengeId = request.challengeId.trim()
    val code = request.code.trim()
    if (challengeId.isBlank()) {
      return ServiceResult.Failure(HttpStatusCode.BadRequest, "Verification request id is required.")
    }
    if (code.isBlank()) {
      return ServiceResult.Failure(HttpStatusCode.BadRequest, "Verification code is required.")
    }

    return when (val result = store.verifyPendingRegistration(challengeId, code, Instant.now().epochSecond)) {
      is ServiceResult.Success -> ServiceResult.Success(createAuthResponse(result.value, request.deviceName))
      is ServiceResult.Failure -> result
    }
  }

  fun resendEmailVerification(request: ResendEmailVerificationRequest): ServiceResult<RegistrationStartResponseDto> {
    val challengeId = request.challengeId.trim()
    if (challengeId.isBlank()) {
      return ServiceResult.Failure(HttpStatusCode.BadRequest, "Verification request id is required.")
    }

    val now = Instant.now().epochSecond
    val pending = when (val result = store.getPendingRegistration(challengeId, now, requireResendReady = true)) {
      is ServiceResult.Success -> result.value
      is ServiceResult.Failure -> return result
    }

    val code = generateOtpCode()
    val expiresAtEpochSeconds = now + otpTtlSeconds
    val resendAvailableAtEpochSeconds = now + otpResendCooldownSeconds
    val sendResult = emailOtpSender.sendRegistrationOtp(
      email = pending.email,
      fullName = pending.fullName,
      code = code,
      expiresAtEpochSeconds = expiresAtEpochSeconds
    )
    if (sendResult.isFailure) {
      return ServiceResult.Failure(
        HttpStatusCode.InternalServerError,
        "We couldn't send the verification email. Please try again."
      )
    }

    return when (
      val refreshResult = store.refreshPendingRegistrationChallenge(
        challengeId = challengeId,
        code = code,
        expiresAtEpochSeconds = expiresAtEpochSeconds,
        resendAvailableAtEpochSeconds = resendAvailableAtEpochSeconds,
        attemptsRemaining = otpAttemptsAllowed
      )
    ) {
      is ServiceResult.Success -> ServiceResult.Success(
        RegistrationStartResponseDto(
          message = "A new verification code is on its way.",
          challenge = refreshResult.value.copy(message = "A new verification code is on its way.")
        )
      )
      is ServiceResult.Failure -> refreshResult
    }
  }

  fun validateAccessPrincipal(userId: String, sessionId: String): Boolean {
    val session = store.getSessionById(sessionId) ?: return false
    val now = Instant.now().epochSecond
    if (session.userId != userId || session.revoked || session.expiresAtEpochSeconds <= now) {
      return false
    }
    store.touchSession(session.id, now)
    return store.getAccountById(userId) != null
  }

  private fun createAuthResponse(account: AccountRecord, deviceName: String?): AuthResponseDto {
    val now = Instant.now().epochSecond
    val refreshToken = UUID.randomUUID().toString()
    val session = store.createSession(
      userId = account.id,
      refreshToken = refreshToken,
      expiresAtEpochSeconds = now + refreshTokenTtlSeconds,
      deviceName = deviceName,
      nowEpochSeconds = now
    )
    val accessTokenExpiry = now + accessTokenTtlSeconds
    val accessToken = createAccessToken(account, session.id, accessTokenExpiry)
    return AuthResponseDto(
      user = store.asUserDto(account),
      tokens = TokenPairDto(
        accessToken = accessToken,
        refreshToken = refreshToken,
        expiresAtEpochSeconds = accessTokenExpiry
      )
    )
  }

  private fun generateOtpCode(): String {
    val lowerBound = buildString {
      append('1')
      repeat(otpCodeLength - 1) { append('0') }
    }.toInt()
    return (lowerBound + otpCodeGenerator.nextInt(lowerBound * 9)).toString()
  }

  private fun createAccessToken(account: AccountRecord, sessionId: String, expiryEpochSeconds: Long): String {
    return JWT.create()
      .withIssuer(issuer)
      .withAudience(audience)
      .withClaim("uid", account.id)
      .withClaim("sid", sessionId)
      .withClaim("role", account.role.name)
      .withExpiresAt(Date.from(Instant.ofEpochSecond(expiryEpochSeconds)))
      .sign(algorithm)
  }
}
