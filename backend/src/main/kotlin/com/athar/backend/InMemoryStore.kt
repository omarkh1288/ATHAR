package com.athar.backend

import io.ktor.http.HttpStatusCode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt
import org.mindrot.jbcrypt.BCrypt

private const val DEFAULT_EMAIL_VERIFICATION_MESSAGE = "We sent a verification code to your email."

internal data class AccountRecord(
  val id: String,
  val role: UserRole,
  var fullName: String,
  val email: String,
  var passwordHash: String,
  var phone: String,
  var location: String,
  var disabilityType: String?,
  val memberSince: String,
  var volunteerLive: Boolean,
  var roleVerifiedAt: String?,
  var contributionStats: ContributionStatsDto,
  var notificationSettings: NotificationSettingsDto,
  var privacySettings: PrivacySettingsDto
)

internal data class SessionRecord(
  val id: String,
  val userId: String,
  var refreshToken: String,
  val createdAtEpochSeconds: Long,
  var lastSeenAtEpochSeconds: Long,
  var expiresAtEpochSeconds: Long,
  val deviceName: String,
  var revoked: Boolean = false
)

internal data class AssistanceRequestRecord(
  val id: String,
  val userId: String,
  val userName: String,
  val userType: String,
  val location: String,
  val destination: String,
  val distance: String,
  val urgency: String,
  val helpType: String,
  val description: String,
  var paymentMethod: PaymentMethod,
  val serviceFee: Double,
  val hours: Int = 1,
  val pricePerHour: Int = 50,
  val createdAtEpochSeconds: Long,
  var completedAtEpochSeconds: Long? = null,
  var status: String = "pending",
  var volunteerId: String? = null,
  var volunteerName: String? = null,
  val declinedVolunteerIds: MutableSet<String> = mutableSetOf()
)

internal data class HelpRequestMessageRecord(
  val id: String,
  val requestId: String,
  val senderId: String,
  val senderName: String,
  val message: String,
  val createdAtEpochSeconds: Long
)

internal data class PaymentRecord(
  val id: String,
  val requestId: String?,
  val userId: String,
  val amount: Double,
  val currency: String,
  val paymentMethod: PaymentMethod,
  var status: String,
  var success: Boolean,
  val checkoutUrl: String?,
  val createdAtEpochSeconds: Long
)

internal data class WithdrawalRecord(
  val id: String,
  val volunteerId: String,
  val amount: Double,
  val method: String,
  var status: String,
  val createdAtEpochSeconds: Long
)

internal data class VolunteerReviewRecord(
  val id: String,
  val requestId: String,
  val volunteerId: String,
  val userId: String,
  val userName: String,
  var rating: Int,
  var comment: String,
  var issues: List<String>,
  val createdAtEpochSeconds: Long
)

internal data class LocationRecord(
  val id: String,
  val name: String,
  val category: String,
  val lat: Double,
  val lng: Double,
  var rating: Double,
  var totalRatings: Int,
  val features: LocationFeaturesDto,
  val recentReports: MutableList<String>,
  val distance: String
)

internal data class SupportMessageRecord(
  val id: String,
  val userId: String,
  val userEmail: String,
  val subject: String,
  val message: String,
  val createdAtEpochSeconds: Long
)

internal data class PendingRegistrationRecord(
  val challengeId: String,
  val role: UserRole,
  val email: String,
  val fullName: String,
  var code: String,
  var expiresAtEpochSeconds: Long,
  var resendAvailableAtEpochSeconds: Long,
  var attemptsRemaining: Int,
  val passwordHash: String,
  val userRequest: RegisterUserRequest? = null,
  val volunteerRequest: RegisterVolunteerRequest? = null
) {
  fun toChallengeDto(message: String = DEFAULT_EMAIL_VERIFICATION_MESSAGE): EmailVerificationChallengeDto {
    return EmailVerificationChallengeDto(
      challengeId = challengeId,
      email = email,
      role = role,
      expiresAtEpochSeconds = expiresAtEpochSeconds,
      resendAvailableAtEpochSeconds = resendAvailableAtEpochSeconds,
      message = message
    )
  }
}

internal class InMemoryStore(
  private val accountDatabase: AccountDatabase = AccountDatabase()
) {
  private val lock = Any()
  private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")

  private val accountsById = linkedMapOf<String, AccountRecord>()
  private val accountIdByEmail = linkedMapOf<String, String>()
  private val pendingRegistrationsByChallengeId = linkedMapOf<String, PendingRegistrationRecord>()
  private val pendingChallengeIdByEmail = linkedMapOf<String, String>()
  private val sessionsById = linkedMapOf<String, SessionRecord>()
  private val sessionIdByRefreshToken = linkedMapOf<String, String>()
  private val requestsById = linkedMapOf<String, AssistanceRequestRecord>()
  private val paymentsById = linkedMapOf<String, PaymentRecord>()
  private val withdrawalsById = linkedMapOf<String, WithdrawalRecord>()
  private val volunteerReviewsById = linkedMapOf<String, VolunteerReviewRecord>()
  private val locationsById = linkedMapOf<String, LocationRecord>()
  private val supportMessages = mutableListOf<SupportMessageRecord>()
  private val analyticsZone = ZoneId.systemDefault()

  init {
    accountDatabase.loadAccounts().forEach { saveAccountInMemory(it) }
    accountDatabase.loadHelpRequests().forEach { persistence ->
      val record = persistence.toRecord()
      requestsById[record.id] = record
    }
    accountDatabase.loadPayments().forEach { persistence ->
      val record = persistence.toRecord()
      paymentsById[record.id] = record
    }
    accountDatabase.loadWithdrawals().forEach { persistence ->
      val record = persistence.toRecord()
      withdrawalsById[record.id] = record
    }
    accountDatabase.loadVolunteerReviews().forEach { persistence ->
      val record = persistence.toRecord()
      volunteerReviewsById[record.id] = record
    }
    if (accountsById.isEmpty()) {
      seedAccounts()
    }
    seedLocationsAndRequests()
    refreshSeedTimestamps()
  }

  /**
   * On every startup, slide the seed request/payment timestamps forward so that
   * the weekly activity chart always has recent data.  The relative spacing
   * (2 days ago, 5 days ago, …) stays the same as when they were first created.
   */
  private fun refreshSeedTimestamps() {
    val daySeconds = 86400L
    val now = nowEpochSeconds()
    val seedOffsets = mapOf(
      "req-seed-1" to 0L,
      "req-seed-2" to 0L,
      "req-seed-3" to 2L,
      "req-seed-4" to 5L,
      "req-seed-5" to 8L,
      "req-seed-6" to 12L,
      "req-seed-7" to 18L,
      "req-seed-8" to 25L,
      "req-seed-9" to 35L,
      "req-seed-10" to 50L
    )
    seedOffsets.forEach { (id, daysAgo) ->
      val existing = requestsById[id] ?: return@forEach
      val freshEpoch = now - daySeconds * daysAgo
      if (existing.createdAtEpochSeconds != freshEpoch) {
        val updated = existing.copy(
          createdAtEpochSeconds = freshEpoch,
          completedAtEpochSeconds = if (existing.status in completedVolunteerStatuses()) freshEpoch else existing.completedAtEpochSeconds
        )
        requestsById[id] = updated
        accountDatabase.upsertHelpRequest(updated.toPersistence())
      }
    }
    // Also refresh payment timestamps to match
    val paymentOffsets = mapOf(
      "pay-seed-1" to 2L,
      "pay-seed-2" to 5L,
      "pay-seed-3" to 8L,
      "pay-seed-4" to 12L,
      "pay-seed-5" to 18L,
      "pay-seed-6" to 25L,
      "pay-seed-7" to 35L,
      "pay-seed-8" to 50L
    )
    paymentOffsets.forEach { (id, daysAgo) ->
      val existing = paymentsById[id] ?: return@forEach
      val freshEpoch = now - daySeconds * daysAgo + 1800
      if (existing.createdAtEpochSeconds != freshEpoch) {
        val updated = existing.copy(createdAtEpochSeconds = freshEpoch)
        paymentsById[id] = updated
        accountDatabase.upsertPayment(updated.toPersistence())
      }
    }
  }

  private fun HelpRequestPersistence.toRecord() = AssistanceRequestRecord(
    id = id,
    userId = userId,
    userName = userName,
    userType = userType,
    location = location,
    destination = destination,
    distance = distance,
    urgency = urgency,
    helpType = helpType,
    description = description,
    paymentMethod = runCatching { PaymentMethod.valueOf(paymentMethod) }.getOrDefault(PaymentMethod.CASH),
    serviceFee = serviceFee,
    hours = hours,
    pricePerHour = pricePerHour,
    createdAtEpochSeconds = createdAtEpochSeconds,
    completedAtEpochSeconds = completedAtEpochSeconds,
    status = status,
    volunteerId = volunteerId,
    volunteerName = volunteerName,
    declinedVolunteerIds = declinedVolunteerIds.toMutableSet()
  )

  private fun AssistanceRequestRecord.toPersistence() = HelpRequestPersistence(
    id = id,
    userId = userId,
    userName = userName,
    userType = userType,
    location = location,
    destination = destination,
    distance = distance,
    urgency = urgency,
    helpType = helpType,
    description = description,
    paymentMethod = paymentMethod.name,
    serviceFee = serviceFee,
    hours = hours,
    pricePerHour = pricePerHour,
    createdAtEpochSeconds = createdAtEpochSeconds,
    completedAtEpochSeconds = completedAtEpochSeconds,
    status = status,
    volunteerId = volunteerId,
    volunteerName = volunteerName,
    declinedVolunteerIds = declinedVolunteerIds.toList()
  )

  private fun PaymentPersistence.toRecord() = PaymentRecord(
    id = id,
    requestId = requestId,
    userId = userId,
    amount = amount,
    currency = currency,
    paymentMethod = runCatching { PaymentMethod.valueOf(paymentMethod) }.getOrDefault(PaymentMethod.CARD),
    status = status,
    success = success,
    checkoutUrl = checkoutUrl,
    createdAtEpochSeconds = createdAtEpochSeconds
  )

  private fun PaymentRecord.toPersistence() = PaymentPersistence(
    id = id,
    requestId = requestId,
    userId = userId,
    amount = amount,
    currency = currency,
    paymentMethod = paymentMethod.name,
    status = status,
    success = success,
    checkoutUrl = checkoutUrl,
    createdAtEpochSeconds = createdAtEpochSeconds
  )

  private fun WithdrawalPersistence.toRecord() = WithdrawalRecord(
    id = id,
    volunteerId = volunteerId,
    amount = amount,
    method = method,
    status = status,
    createdAtEpochSeconds = createdAtEpochSeconds
  )

  private fun WithdrawalRecord.toPersistence() = WithdrawalPersistence(
    id = id,
    volunteerId = volunteerId,
    amount = amount,
    method = method,
    status = status,
    createdAtEpochSeconds = createdAtEpochSeconds
  )

  private fun VolunteerReviewPersistence.toRecord() = VolunteerReviewRecord(
    id = id,
    requestId = requestId,
    volunteerId = volunteerId,
    userId = userId,
    userName = userName,
    rating = rating,
    comment = comment,
    issues = issues,
    createdAtEpochSeconds = createdAtEpochSeconds
  )

  private fun VolunteerReviewRecord.toPersistence() = VolunteerReviewPersistence(
    id = id,
    requestId = requestId,
    volunteerId = volunteerId,
    userId = userId,
    userName = userName,
    rating = rating,
    comment = comment,
    issues = issues,
    createdAtEpochSeconds = createdAtEpochSeconds
  )

  fun registerUser(request: RegisterUserRequest, passwordHash: String): ServiceResult<AccountRecord> = synchronized(lock) {
    val fullName = request.fullName.trim()
    val email = request.email.trim().lowercase(Locale.getDefault())
    val phone = request.phone.trim()
    val location = request.location.trim()
    val disabilityType = request.disabilityType.trim().ifBlank { null }

    validateCommonRegistration(fullName, email, request.password)?.let { return failure(HttpStatusCode.BadRequest, it) }
    if (phone.isBlank()) return failure(HttpStatusCode.BadRequest, "Phone is required.")
    ensureEmailAvailable(email)?.let { return failure(HttpStatusCode.Conflict, it) }

    val account = AccountRecord(
      id = "user-${UUID.randomUUID()}",
      role = UserRole.User,
      fullName = fullName,
      email = email,
      passwordHash = passwordHash,
      phone = phone,
      location = location,
      disabilityType = disabilityType,
      memberSince = currentMonthYear(),
      volunteerLive = false,
      roleVerifiedAt = null,
      contributionStats = ContributionStatsDto(0, 0, 0),
      notificationSettings = NotificationSettingsDto(),
      privacySettings = PrivacySettingsDto()
    )
    saveAccount(account)
    accountDatabase.upsertUserProfile(
      accountId = account.id,
      profile = UserProfilePersistence(
        emergencyContactName = request.emergencyContactName.trim().ifBlank { null },
        emergencyContactPhone = request.emergencyContactPhone.trim().ifBlank { null }
      )
    )
    ServiceResult.Success(account)
  }

  fun registerVolunteer(request: RegisterVolunteerRequest, passwordHash: String): ServiceResult<AccountRecord> = synchronized(lock) {
    val fullName = request.fullName.trim()
    val email = request.email.trim().lowercase(Locale.getDefault())
    val phone = request.phone.trim()
    val location = request.location.trim()

    validateCommonRegistration(fullName, email, request.password)?.let { return failure(HttpStatusCode.BadRequest, it) }
    if (phone.isBlank()) return failure(HttpStatusCode.BadRequest, "Phone is required.")
    ensureEmailAvailable(email)?.let { return failure(HttpStatusCode.Conflict, it) }

    val account = AccountRecord(
      id = "vol-${UUID.randomUUID()}",
      role = UserRole.Volunteer,
      fullName = fullName,
      email = email,
      passwordHash = passwordHash,
      phone = phone,
      location = location,
      disabilityType = null,
      memberSince = currentMonthYear(),
      volunteerLive = false,
      roleVerifiedAt = Instant.now().toString(),
      contributionStats = ContributionStatsDto(0, 0, 0),
      notificationSettings = NotificationSettingsDto(),
      privacySettings = PrivacySettingsDto()
    )
    saveAccount(account)
    accountDatabase.upsertVolunteerProfile(
      accountId = account.id,
      profile = VolunteerProfilePersistence(
        nationalId = request.idNumber.trim().ifBlank { null },
        dateOfBirth = request.dateOfBirth.trim().ifBlank { null },
        motivation = request.motivation.trim().ifBlank { null },
        languages = request.languages.map { it.trim() }.filter { it.isNotBlank() }.distinct(),
        availability = request.availability.map { it.trim() }.filter { it.isNotBlank() }.distinct(),
        idDocumentFileName = request.idDocumentFileName?.trim()?.ifBlank { null },
        idDocumentContentType = request.idDocumentContentType?.trim()?.ifBlank { null },
        idDocumentSizeBytes = request.idDocumentSizeBytes?.takeIf { it > 0L },
        idDocumentBytes = request.idDocumentBytes?.takeIf { it.isNotEmpty() }
      )
    )
    ServiceResult.Success(account)
  }

  fun createPendingUserRegistration(
    request: RegisterUserRequest,
    passwordHash: String,
    code: String,
    expiresAtEpochSeconds: Long,
    resendAvailableAtEpochSeconds: Long,
    attemptsRemaining: Int
  ): ServiceResult<EmailVerificationChallengeDto> = synchronized(lock) {
    pruneExpiredPendingRegistrationsLocked()
    val fullName = request.fullName.trim()
    val email = request.email.trim().lowercase(Locale.getDefault())
    validateCommonRegistration(fullName, email, request.password)?.let { return failure(HttpStatusCode.BadRequest, it) }
    if (request.phone.trim().isBlank()) return failure(HttpStatusCode.BadRequest, "Phone is required.")
    ensureEmailAvailable(email)?.let { return failure(HttpStatusCode.Conflict, it) }

    clearPendingRegistrationByEmailLocked(email)
    val record = PendingRegistrationRecord(
      challengeId = "verify-${UUID.randomUUID()}",
      role = UserRole.User,
      email = email,
      fullName = fullName,
      code = code,
      expiresAtEpochSeconds = expiresAtEpochSeconds,
      resendAvailableAtEpochSeconds = resendAvailableAtEpochSeconds,
      attemptsRemaining = attemptsRemaining,
      passwordHash = passwordHash,
      userRequest = request.copy(email = email, fullName = fullName)
    )
    savePendingRegistrationLocked(record)
    ServiceResult.Success(record.toChallengeDto())
  }

  fun createPendingVolunteerRegistration(
    request: RegisterVolunteerRequest,
    passwordHash: String,
    code: String,
    expiresAtEpochSeconds: Long,
    resendAvailableAtEpochSeconds: Long,
    attemptsRemaining: Int
  ): ServiceResult<EmailVerificationChallengeDto> = synchronized(lock) {
    pruneExpiredPendingRegistrationsLocked()
    val fullName = request.fullName.trim()
    val email = request.email.trim().lowercase(Locale.getDefault())
    validateCommonRegistration(fullName, email, request.password)?.let { return failure(HttpStatusCode.BadRequest, it) }
    if (request.phone.trim().isBlank()) return failure(HttpStatusCode.BadRequest, "Phone is required.")
    ensureEmailAvailable(email)?.let { return failure(HttpStatusCode.Conflict, it) }

    clearPendingRegistrationByEmailLocked(email)
    val record = PendingRegistrationRecord(
      challengeId = "verify-${UUID.randomUUID()}",
      role = UserRole.Volunteer,
      email = email,
      fullName = fullName,
      code = code,
      expiresAtEpochSeconds = expiresAtEpochSeconds,
      resendAvailableAtEpochSeconds = resendAvailableAtEpochSeconds,
      attemptsRemaining = attemptsRemaining,
      passwordHash = passwordHash,
      volunteerRequest = request.copy(email = email, fullName = fullName)
    )
    savePendingRegistrationLocked(record)
    ServiceResult.Success(record.toChallengeDto())
  }

  fun getPendingRegistration(
    challengeId: String,
    nowEpochSeconds: Long,
    requireResendReady: Boolean = false
  ): ServiceResult<PendingRegistrationRecord> = synchronized(lock) {
    pruneExpiredPendingRegistrationsLocked(nowEpochSeconds)
    val record = pendingRegistrationsByChallengeId[challengeId]
      ?: return failure(HttpStatusCode.NotFound, "Verification request was not found. Please register again.")
    if (record.expiresAtEpochSeconds <= nowEpochSeconds) {
      clearPendingRegistrationLocked(challengeId)
      return failure(HttpStatusCode.BadRequest, "Verification code expired. Please register again.")
    }
    if (requireResendReady && record.resendAvailableAtEpochSeconds > nowEpochSeconds) {
      return failure(HttpStatusCode.TooManyRequests, "Please wait before requesting another code.")
    }
    ServiceResult.Success(record)
  }

  fun refreshPendingRegistrationChallenge(
    challengeId: String,
    code: String,
    expiresAtEpochSeconds: Long,
    resendAvailableAtEpochSeconds: Long,
    attemptsRemaining: Int
  ): ServiceResult<EmailVerificationChallengeDto> = synchronized(lock) {
    val record = pendingRegistrationsByChallengeId[challengeId]
      ?: return failure(HttpStatusCode.NotFound, "Verification request was not found. Please register again.")
    record.code = code
    record.expiresAtEpochSeconds = expiresAtEpochSeconds
    record.resendAvailableAtEpochSeconds = resendAvailableAtEpochSeconds
    record.attemptsRemaining = attemptsRemaining
    ServiceResult.Success(record.toChallengeDto())
  }

  fun verifyPendingRegistration(
    challengeId: String,
    code: String,
    nowEpochSeconds: Long
  ): ServiceResult<AccountRecord> = synchronized(lock) {
    pruneExpiredPendingRegistrationsLocked(nowEpochSeconds)
    val normalizedCode = code.trim()
    val record = pendingRegistrationsByChallengeId[challengeId]
      ?: return failure(HttpStatusCode.NotFound, "Verification request was not found. Please register again.")

    if (record.expiresAtEpochSeconds <= nowEpochSeconds) {
      clearPendingRegistrationLocked(challengeId)
      return failure(HttpStatusCode.BadRequest, "Verification code expired. Please register again.")
    }

    if (normalizedCode != record.code) {
      record.attemptsRemaining -= 1
      if (record.attemptsRemaining <= 0) {
        clearPendingRegistrationLocked(challengeId)
        return failure(HttpStatusCode.BadRequest, "Too many incorrect attempts. Please register again.")
      }
      return failure(
        HttpStatusCode.BadRequest,
        "Incorrect verification code. ${record.attemptsRemaining} attempt(s) remaining."
      )
    }

    val result = when (record.role) {
      UserRole.User -> {
        val userRequest = record.userRequest
          ?: return failure(HttpStatusCode.BadRequest, "Verification request is invalid.")
        registerUser(userRequest, record.passwordHash)
      }
      UserRole.Volunteer -> {
        val volunteerRequest = record.volunteerRequest
          ?: return failure(HttpStatusCode.BadRequest, "Verification request is invalid.")
        registerVolunteer(volunteerRequest, record.passwordHash)
      }
    }

    if (result is ServiceResult.Success) {
      clearPendingRegistrationLocked(challengeId)
    } else if (result is ServiceResult.Failure && result.status == HttpStatusCode.Conflict) {
      clearPendingRegistrationLocked(challengeId)
    }
    result
  }

  fun removePendingRegistration(challengeId: String) = synchronized(lock) {
    clearPendingRegistrationLocked(challengeId)
  }

  fun authenticate(emailRaw: String, password: String): ServiceResult<AccountRecord> = synchronized(lock) {
    val email = emailRaw.trim().lowercase(Locale.getDefault())
    val accountId = accountIdByEmail[email] ?: return failure(HttpStatusCode.Unauthorized, "No account found for this email.")
    val account = accountsById[accountId] ?: return failure(HttpStatusCode.Unauthorized, "Invalid credentials.")
    if (!BCrypt.checkpw(password, account.passwordHash)) {
      return failure(HttpStatusCode.Unauthorized, "Incorrect password.")
    }
    ServiceResult.Success(account)
  }

  fun getAccountById(userId: String): AccountRecord? = synchronized(lock) {
    accountsById[userId]
  }

  fun getProfile(userId: String): ServiceResult<AuthUserDto> = synchronized(lock) {
    val account = accountsById[userId] ?: return failure(HttpStatusCode.NotFound, "Account not found.")
    ServiceResult.Success(account.toDto())
  }

  fun asUserDto(account: AccountRecord): AuthUserDto = synchronized(lock) {
    account.toDto()
  }

  fun createSession(
    userId: String,
    refreshToken: String,
    expiresAtEpochSeconds: Long,
    deviceName: String?,
    nowEpochSeconds: Long
  ): SessionRecord = synchronized(lock) {
    val session = SessionRecord(
      id = "ses-${UUID.randomUUID()}",
      userId = userId,
      refreshToken = refreshToken,
      createdAtEpochSeconds = nowEpochSeconds,
      lastSeenAtEpochSeconds = nowEpochSeconds,
      expiresAtEpochSeconds = expiresAtEpochSeconds,
      deviceName = deviceName?.takeIf { it.isNotBlank() } ?: "Android device",
      revoked = false
    )
    sessionsById[session.id] = session
    sessionIdByRefreshToken[refreshToken] = session.id
    session
  }

  fun getSessionById(sessionId: String): SessionRecord? = synchronized(lock) {
    sessionsById[sessionId]
  }

  fun touchSession(sessionId: String, nowEpochSeconds: Long) = synchronized(lock) {
    sessionsById[sessionId]?.lastSeenAtEpochSeconds = nowEpochSeconds
  }

  fun getSessionByRefreshToken(refreshToken: String): SessionRecord? = synchronized(lock) {
    val sessionId = sessionIdByRefreshToken[refreshToken] ?: return null
    sessionsById[sessionId]
  }

  fun rotateRefreshToken(
    sessionId: String,
    newRefreshToken: String,
    nowEpochSeconds: Long,
    newExpiresAtEpochSeconds: Long
  ): SessionRecord? = synchronized(lock) {
    val session = sessionsById[sessionId] ?: return null
    sessionIdByRefreshToken.remove(session.refreshToken)
    session.refreshToken = newRefreshToken
    session.lastSeenAtEpochSeconds = nowEpochSeconds
    session.expiresAtEpochSeconds = newExpiresAtEpochSeconds
    sessionIdByRefreshToken[newRefreshToken] = sessionId
    session
  }

  fun revokeSession(sessionId: String): Boolean = synchronized(lock) {
    val session = sessionsById[sessionId] ?: return false
    if (!session.revoked) {
      session.revoked = true
      sessionIdByRefreshToken.remove(session.refreshToken)
    }
    true
  }

  fun revokeSessionByRefreshToken(userId: String, refreshToken: String): Boolean = synchronized(lock) {
    val session = getSessionByRefreshToken(refreshToken) ?: return false
    if (session.userId != userId) return false
    revokeSession(session.id)
  }

  fun listSessions(userId: String, currentSessionId: String): List<SessionDto> = synchronized(lock) {
    sessionsById.values
      .filter { it.userId == userId && !it.revoked }
      .sortedByDescending { it.lastSeenAtEpochSeconds }
      .map {
        SessionDto(
          id = it.id,
          deviceName = it.deviceName,
          createdAtEpochSeconds = it.createdAtEpochSeconds,
          lastSeenAtEpochSeconds = it.lastSeenAtEpochSeconds,
          isCurrent = it.id == currentSessionId
        )
      }
  }

  fun deleteSession(userId: String, sessionId: String, currentSessionId: String): ServiceResult<ActionResultDto> = synchronized(lock) {
    if (sessionId == currentSessionId) {
      return failure(HttpStatusCode.BadRequest, "Use logout to end the current session.")
    }
    val session = sessionsById[sessionId] ?: return failure(HttpStatusCode.NotFound, "Session not found.")
    if (session.userId != userId) return failure(HttpStatusCode.Forbidden, "You can only revoke your own sessions.")
    revokeSession(sessionId)
    ServiceResult.Success(ActionResultDto(true, "Session revoked."))
  }

  fun updateProfile(userId: String, request: UpdateProfileRequest): ServiceResult<AuthUserDto> = synchronized(lock) {
    val account = accountsById[userId] ?: return failure(HttpStatusCode.NotFound, "Account not found.")
    request.fullName?.trim()?.takeIf { it.isNotBlank() }?.let { account.fullName = it }
    request.phone?.trim()?.takeIf { it.isNotBlank() }?.let { account.phone = it }
    request.location?.trim()?.takeIf { it.isNotBlank() }?.let { account.location = it }
    if (account.role == UserRole.User) {
      request.disabilityType?.trim()?.let { account.disabilityType = it.ifBlank { null } }
    }
    persistAccount(account)
    ServiceResult.Success(account.toDto())
  }

  fun changePassword(userId: String, currentPassword: String, newPassword: String): ServiceResult<ActionResultDto> = synchronized(lock) {
    val account = accountsById[userId] ?: return failure(HttpStatusCode.NotFound, "Account not found.")
    if (!BCrypt.checkpw(currentPassword, account.passwordHash)) {
      return failure(HttpStatusCode.BadRequest, "Current password is incorrect.")
    }
    if (newPassword.length < 8) {
      return failure(HttpStatusCode.BadRequest, "New password must be at least 8 characters.")
    }
    account.passwordHash = BCrypt.hashpw(newPassword, BCrypt.gensalt())
    persistAccount(account)
    ServiceResult.Success(ActionResultDto(true, "Password updated successfully."))
  }

  fun updateNotificationSettings(userId: String, request: UpdateNotificationSettingsRequest): ServiceResult<NotificationSettingsDto> = synchronized(lock) {
    val account = accountsById[userId] ?: return failure(HttpStatusCode.NotFound, "Account not found.")
    val current = account.notificationSettings
    val updated = NotificationSettingsDto(
      pushEnabled = request.pushEnabled ?: current.pushEnabled,
      emailEnabled = request.emailEnabled ?: current.emailEnabled,
      smsEnabled = request.smsEnabled ?: current.smsEnabled,
      volunteerRequests = request.volunteerRequests ?: current.volunteerRequests,
      volunteerAccepted = request.volunteerAccepted ?: current.volunteerAccepted,
      locationUpdates = request.locationUpdates ?: current.locationUpdates,
      newRatings = request.newRatings ?: current.newRatings,
      communityUpdates = request.communityUpdates ?: current.communityUpdates,
      marketingEmails = request.marketingEmails ?: current.marketingEmails,
      soundEnabled = request.soundEnabled ?: current.soundEnabled,
      vibrationEnabled = request.vibrationEnabled ?: current.vibrationEnabled
    )
    account.notificationSettings = updated
    persistAccount(account)
    ServiceResult.Success(updated)
  }

  fun updatePrivacySettings(userId: String, request: UpdatePrivacySettingsRequest): ServiceResult<PrivacySettingsDto> = synchronized(lock) {
    val account = accountsById[userId] ?: return failure(HttpStatusCode.NotFound, "Account not found.")
    val current = account.privacySettings
    val updated = PrivacySettingsDto(
      locationSharing = request.locationSharing ?: current.locationSharing,
      profileVisibility = request.profileVisibility ?: current.profileVisibility,
      showRatings = request.showRatings ?: current.showRatings,
      activityStatus = request.activityStatus ?: current.activityStatus,
      twoFactorAuth = request.twoFactorAuth ?: current.twoFactorAuth
    )
    account.privacySettings = updated
    persistAccount(account)
    ServiceResult.Success(updated)
  }

  fun setVolunteerLive(userId: String, request: ToggleVolunteerLiveRequest): ServiceResult<AuthUserDto> = synchronized(lock) {
    val account = accountsById[userId] ?: return failure(HttpStatusCode.NotFound, "Account not found.")
    if (account.role != UserRole.Volunteer) {
      return failure(HttpStatusCode.Forbidden, "Only volunteers can toggle live status.")
    }
    account.volunteerLive = request.isLive
    persistAccount(account)
    ServiceResult.Success(account.toDto())
  }

  fun createAssistanceRequest(userId: String, request: CreateAssistanceRequest): ServiceResult<VolunteerRequestDto> = synchronized(lock) {
    val account = accountsById[userId] ?: return failure(HttpStatusCode.NotFound, "Account not found.")
    if (account.role != UserRole.User) {
      return failure(HttpStatusCode.Forbidden, "Only users can create assistance requests.")
    }
    if (request.location.isBlank() || request.destination.isBlank() || request.helpType.isBlank()) {
      return failure(HttpStatusCode.BadRequest, "Location, destination, and help type are required.")
    }
    if (request.payment_method !in setOf(PaymentMethod.CARD, PaymentMethod.CASH)) {
      return failure(HttpStatusCode.BadRequest, "payment_method must be card or cash.")
    }
    if (request.service_fee < 0) {
      return failure(HttpStatusCode.BadRequest, "service_fee must be greater than or equal to 0.")
    }

    val now = nowEpochSeconds()
    val entity = AssistanceRequestRecord(
      id = "req-${UUID.randomUUID()}",
      userId = account.id,
      userName = account.fullName,
      userType = request.userType.ifBlank { account.disabilityType ?: "User" },
      location = request.location,
      destination = request.destination,
      distance = request.distance.ifBlank { "Unknown" },
      urgency = request.urgency.ifBlank { "medium" },
      helpType = request.helpType,
      description = request.description.ifBlank { request.helpType },
      paymentMethod = request.payment_method,
      serviceFee = request.service_fee,
      hours = request.hours,
      pricePerHour = request.price_per_hour,
      createdAtEpochSeconds = now,
      status = "pending"
    )
    requestsById[entity.id] = entity
    accountDatabase.upsertHelpRequest(entity.toPersistence())
    ServiceResult.Success(entity.toVolunteerRequestDto())
  }

  fun getMyRequests(userId: String, statusFilter: String? = "all", perPage: Int = 15): ServiceResult<MyRequestsResponse> = synchronized(lock) {
    val account = accountsById[userId] ?: return failure(HttpStatusCode.NotFound, "Account not found.")
    val statusSet = when (statusFilter?.lowercase(Locale.getDefault())) {
      "pending" -> setOf("pending", "pending_payment")
      "active" -> setOf("active", "accepted", "in_progress", "confirmed")
      "completed" -> setOf("completed", "rated", "archived")
      "cancelled" -> setOf("cancelled", "no_volunteer")
      "history" -> setOf("completed", "rated", "archived", "cancelled", "no_volunteer")
      else -> null // all
    }

    if (account.role == UserRole.User) {
      val userRequests = requestsById.values
        .filter { it.userId == userId }
        .filter { statusSet == null || statusSet.contains(it.status) }
        .sortedByDescending { it.createdAtEpochSeconds }
        .take(perPage)
        .map { it.toVolunteerRequestDto() }
      return ServiceResult.Success(MyRequestsResponse(role = UserRole.User, userRequests = userRequests))
    }

    val volunteerRequests = requestsById.values
      .filter { it.volunteerId == userId || (it.status == "pending" && !it.declinedVolunteerIds.contains(userId)) }
      .filter { statusSet == null || statusSet.contains(it.status) }
      .sortedByDescending { it.createdAtEpochSeconds }
      .take(perPage)
      .map { it.toAssistanceRequestDto() }
    ServiceResult.Success(MyRequestsResponse(role = UserRole.Volunteer, volunteerRequests = volunteerRequests))
  }

  fun getIncomingVolunteerRequests(userId: String): ServiceResult<List<AssistanceRequestDto>> = synchronized(lock) {
    val account = accountsById[userId] ?: return failure(HttpStatusCode.NotFound, "Account not found.")
    if (account.role != UserRole.Volunteer) {
      return failure(HttpStatusCode.Forbidden, "Only volunteers can view incoming requests.")
    }
    if (account.roleVerifiedAt == null) {
      return failure(HttpStatusCode.Forbidden, "Volunteer account not verified yet.")
    }
    if (!account.volunteerLive) {
      return ServiceResult.Success(emptyList())
    }
    val incoming = requestsById.values
      .filter { it.status == "pending" && it.volunteerId == null && !it.declinedVolunteerIds.contains(userId) }
      .sortedByDescending { it.createdAtEpochSeconds }
      .map { it.toAssistanceRequestDto() }
    ServiceResult.Success(incoming)
  }

  fun getVolunteerIncomingDashboard(
    userId: String,
    perPage: Int = 15
  ): ServiceResult<VolunteerIncomingResponseDto> = synchronized(lock) {
    val account = accountsById[userId] ?: return failure(HttpStatusCode.NotFound, "Account not found.")
    if (account.role != UserRole.Volunteer) {
      return failure(HttpStatusCode.Forbidden, "Only volunteers can access incoming dashboard.")
    }
    if (account.roleVerifiedAt == null) {
      return failure(HttpStatusCode.Forbidden, "Volunteer account not verified yet.")
    }

    val cappedPerPage = perPage.coerceIn(1, 100)
    val incomingRequests = if (account.volunteerLive) {
      requestsById.values
        .filter { it.status == "pending" && it.volunteerId == null && !it.declinedVolunteerIds.contains(userId) }
        .sortedByDescending { it.createdAtEpochSeconds }
        .take(cappedPerPage)
        .map { it.toAssistanceRequestDto() }
    } else {
      emptyList()
    }

    val counts = buildVolunteerCounts(userId = userId, includeIncoming = account.volunteerLive)
    val message = if (counts.incoming > 0) {
      "${counts.incoming} ${if (counts.incoming == 1) "person needs" else "people need"} your help nearby"
    } else {
      "No incoming requests nearby."
    }

    ServiceResult.Success(
      VolunteerIncomingResponseDto(
        counts = counts,
        incoming_alert = IncomingAlertDto(
          count = counts.incoming,
          message = message
        ),
        requests = incomingRequests
      )
    )
  }

  fun getVolunteerActiveDashboard(
    userId: String,
    perPage: Int = 15
  ): ServiceResult<VolunteerActiveResponseDto> = synchronized(lock) {
    val account = accountsById[userId] ?: return failure(HttpStatusCode.NotFound, "Account not found.")
    if (account.role != UserRole.Volunteer) {
      return failure(HttpStatusCode.Forbidden, "Only volunteers can access active dashboard.")
    }
    if (account.roleVerifiedAt == null) {
      return failure(HttpStatusCode.Forbidden, "Volunteer account not verified yet.")
    }

    val cappedPerPage = perPage.coerceIn(1, 100)
    val activeRequests = requestsById.values
      .filter { it.volunteerId == userId && it.status in activeVolunteerStatuses() }
      .sortedByDescending { it.createdAtEpochSeconds }
      .take(cappedPerPage)
      .map { it.toAssistanceRequestDto() }

    ServiceResult.Success(
      VolunteerActiveResponseDto(
        counts = buildVolunteerCounts(userId = userId, includeIncoming = account.volunteerLive),
        status_banner = if (activeRequests.isNotEmpty()) "Assistance in Progress" else "No active assistance right now.",
        requests = activeRequests
      )
    )
  }

  fun getVolunteerHistoryDashboard(
    userId: String,
    perPage: Int = 15
  ): ServiceResult<VolunteerHistoryResponseDto> = synchronized(lock) {
    val account = accountsById[userId] ?: return failure(HttpStatusCode.NotFound, "Account not found.")
    if (account.role != UserRole.Volunteer) {
      return failure(HttpStatusCode.Forbidden, "Only volunteers can access history dashboard.")
    }
    if (account.roleVerifiedAt == null) {
      return failure(HttpStatusCode.Forbidden, "Volunteer account not verified yet.")
    }

    val cappedPerPage = perPage.coerceIn(1, 100)
    val historyRecords = requestsById.values
      .filter { it.volunteerId == userId && it.status in historyVolunteerStatuses() }
      .sortedByDescending { it.createdAtEpochSeconds }
    val historyRequests = historyRecords
      .take(cappedPerPage)
      .map { it.toAssistanceRequestDto() }

    val completedCount = historyRecords.count { it.status in completedVolunteerStatuses() }
    val thisWeekCount = historyRecords.count {
      it.status in completedVolunteerStatuses() && isWithinLastDays(it.createdAtEpochSeconds, 7)
    }

    ServiceResult.Success(
      VolunteerHistoryResponseDto(
        counts = buildVolunteerCounts(userId = userId, includeIncoming = account.volunteerLive),
        impact = VolunteerImpactDto(
          totalAssists = completedCount,
          avgRating = averageVolunteerRating(userId),
          thisWeek = thisWeekCount
        ),
        requests = historyRequests
      )
    )
  }

  fun getVolunteerImpactDashboard(userId: String): ServiceResult<VolunteerImpactResponseDto> = synchronized(lock) {
    val resolvedVolunteerId = resolveAnalyticsVolunteerId(userId)
    val account = accountsById[resolvedVolunteerId] ?: return failure(HttpStatusCode.NotFound, "Account not found.")

    val historyRecords = requestsById.values
      .filter { it.volunteerId == resolvedVolunteerId && it.status in historyVolunteerStatuses() }
    val completedCount = historyRecords.count { it.status in completedVolunteerStatuses() }
    val thisWeekCount = historyRecords.count {
      it.status in completedVolunteerStatuses() && isWithinLastDays(it.createdAtEpochSeconds, 7)
    }

    ServiceResult.Success(
      VolunteerImpactResponseDto(
        counts = buildVolunteerCounts(userId = resolvedVolunteerId, includeIncoming = account.volunteerLive),
        impact = VolunteerImpactDto(
          totalAssists = completedCount,
          avgRating = averageVolunteerRating(resolvedVolunteerId),
          thisWeek = thisWeekCount
        )
      )
    )
  }

  fun getVolunteerAnalyticsEarnings(userId: String): ServiceResult<VolunteerAnalyticsEarningsResponseDto> = synchronized(lock) {
    val resolvedVolunteerId = resolveAnalyticsVolunteerId(userId)
    val account = accountsById[resolvedVolunteerId] ?: return failure(HttpStatusCode.NotFound, "Account not found.")

    val settledPayments = settledVolunteerPayments(resolvedVolunteerId)
    val pendingPayments = pendingVolunteerPayments(resolvedVolunteerId)
    val unpaidPendingRequests = requestsById.values
      .filter { it.volunteerId == resolvedVolunteerId && latestSuccessfulPaymentForRequest(it.id) == null && it.status in activeVolunteerStatuses() }
      .sortedByDescending { it.createdAtEpochSeconds }
    val withdrawalHistory = withdrawalsById.values
      .filter { it.volunteerId == resolvedVolunteerId }
      .sortedByDescending { it.createdAtEpochSeconds }

    val totalGross = settledPayments.sumOf { it.request.grossAmount() }
    val totalFees = settledPayments.sumOf { it.request.platformFeeAmount() }
    val totalNet = settledPayments.sumOf { it.request.netAmount() }
    val pendingBalance = pendingPayments.sumOf { it.request.netAmount() } +
      unpaidPendingRequests.sumOf { it.netAmount() }
    val reservedWithdrawals = withdrawalHistory
      .filter { it.status != "failed" && it.status != "cancelled" }
      .sumOf { it.amount }
    val availableBalance = (totalNet - reservedWithdrawals).coerceAtLeast(0.0)

    val currentMonth = YearMonth.now(analyticsZone)
    val recentMonths = (5L downTo 0L).map { currentMonth.minusMonths(it) }
    val monthlyBreakdown = recentMonths.map { month ->
      val monthPayments = settledPayments.filter { it.request.yearMonth() == month }
      VolunteerAnalyticsMonthlyEarningDto(
        month = month.format(monthLabelFormatter()),
        gross = monthPayments.sumOf { it.request.grossAmount() },
        net = monthPayments.sumOf { it.request.netAmount() },
        fee = monthPayments.sumOf { it.request.platformFeeAmount() }
      )
    }
    val currentMonthNet = monthlyBreakdown.lastOrNull()?.net ?: 0.0
    val lastMonthNet = monthlyBreakdown.dropLast(1).lastOrNull()?.net ?: 0.0
    val monthlyChangePercent = percentageChange(currentMonthNet, lastMonthNet)
    val thisWeekNet = settledPayments
      .filter { isWithinLastDays(it.request.analyticsEpochSeconds(), 7) }
      .sumOf { it.request.netAmount() }

    val paymentHistory = volunteerAnalyticsPaymentHistory(resolvedVolunteerId)

    ServiceResult.Success(
      VolunteerAnalyticsEarningsResponseDto(
        available_balance = availableBalance,
        pending_balance = pendingBalance,
        total_gross = totalGross,
        total_fees = totalFees,
        total_net = totalNet,
        this_week_net = thisWeekNet,
        current_month_label = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)),
        current_month_net = currentMonthNet,
        last_month_net = lastMonthNet,
        monthly_change_percent = monthlyChangePercent,
        monthly_earnings = monthlyBreakdown,
        withdrawal_history = withdrawalHistory.map {
          VolunteerAnalyticsWithdrawalRecordDto(
            id = it.id,
            date = it.analyticsDate(),
            amount = it.amount,
            method = it.method,
            status = it.status
          )
        },
        payment_history = paymentHistory.take(20)
      )
    )
  }

  fun submitVolunteerWithdrawal(
    userId: String,
    amount: Double,
    method: String
  ): ServiceResult<ActionResultDto> = synchronized(lock) {
    val resolvedVolunteerId = resolveAnalyticsVolunteerId(userId)
    val account = accountsById[resolvedVolunteerId] ?: return failure(HttpStatusCode.NotFound, "Account not found.")
    if (amount < 100.0) {
      return failure(HttpStatusCode.BadRequest, "Minimum withdrawal is 100 EGP.")
    }

    val normalizedMethod = method.trim().lowercase(Locale.getDefault())
    val resolvedMethod = when (normalizedMethod) {
      "bank", "bank_transfer", "bank transfer" -> "Bank Transfer"
      "wallet", "paymob", "paymob_wallet", "paymob wallet" -> "Paymob Wallet"
      else -> return failure(HttpStatusCode.BadRequest, "Withdrawal method must be bank or wallet.")
    }

    val earningsResult = getVolunteerAnalyticsEarnings(resolvedVolunteerId)
    val earnings = (earningsResult as? ServiceResult.Success)?.value
      ?: return failure(HttpStatusCode.Conflict, "Unable to calculate available balance.")
    if (amount > earnings.available_balance + 0.0001) {
      return failure(HttpStatusCode.Conflict, "Withdrawal amount exceeds available balance.")
    }

    val status = if (resolvedMethod == "Paymob Wallet") "completed" else "pending"
    val withdrawal = WithdrawalRecord(
      id = "wd-${UUID.randomUUID()}",
      volunteerId = resolvedVolunteerId,
      amount = amount,
      method = resolvedMethod,
      status = status,
      createdAtEpochSeconds = nowEpochSeconds()
    )
    withdrawalsById[withdrawal.id] = withdrawal
    accountDatabase.upsertWithdrawal(withdrawal.toPersistence())
    val message = if (status == "completed") {
      "Withdrawal sent to your Paymob Wallet."
    } else {
      "Withdrawal request submitted. Bank transfers take 2-3 business days."
    }
    return ServiceResult.Success(ActionResultDto(true, message))
  }

  fun getVolunteerAnalyticsPerformance(userId: String): ServiceResult<VolunteerAnalyticsPerformanceResponseDto> = synchronized(lock) {
    val resolvedVolunteerId = resolveAnalyticsVolunteerId(userId)
    val handledRequests = requestsById.values
      .filter { it.volunteerId == resolvedVolunteerId }
      .sortedByDescending { it.createdAtEpochSeconds }
    val completedRequests = successfulPaidVolunteerRequests(resolvedVolunteerId)
      .sortedByDescending { it.createdAtEpochSeconds }
    val allCompletedRequests = handledRequests
      .filter { it.status in completedVolunteerStatuses() }
    val pendingRequests = handledRequests.filter { it.status in activeVolunteerStatuses() }
    val declinedCount = requestsById.values.count { it.declinedVolunteerIds.contains(resolvedVolunteerId) }
    val acceptedCount = handledRequests.size
    val responseDenominator = acceptedCount + declinedCount
    val responseRate = if (responseDenominator == 0) 100f else (acceptedCount * 100f / responseDenominator)
    val completionRate = if (acceptedCount == 0) 0f else (completedRequests.size * 100f / acceptedCount)
    val reviews = volunteerReviewsFor(resolvedVolunteerId)
    val averageRating = averageVolunteerRating(resolvedVolunteerId)
    val positiveReviews = reviews.count { it.rating >= 4 }
    val fiveStarRatings = reviews.count { it.rating == 5 }
    val lateArrivalFlags = reviews.count { review ->
      review.issues.any { issue -> issue.equals("Late arrival", ignoreCase = true) }
    }
    val onTimeRate = if (reviews.isEmpty()) {
      completionRate.coerceAtLeast(80f)
    } else {
      ((reviews.size - lateArrivalFlags).coerceAtLeast(0) * 100f / reviews.size)
    }
    val score = ((responseRate + completionRate + onTimeRate + (averageRating * 20f)) / 4f).coerceIn(0f, 100f)
    val badges = performanceBadges(
      completedCount = completedRequests.size,
      usersHelped = completedRequests.map { it.userId }.distinct().size,
      averageRating = averageRating,
      responseRate = responseRate,
      completionRate = completionRate
    )

    ServiceResult.Success(
      VolunteerAnalyticsPerformanceResponseDto(
        grade = performanceGrade(score),
        headline = performanceHeadline(score),
        percentile = performancePercentile(score),
        response_rate = responseRate,
        completion_rate = completionRate,
        average_rating = averageRating,
        on_time_rate = onTimeRate,
        completed = completedRequests.size,
        pending = pendingRequests.size,
        users_helped = completedRequests.map { it.userId }.distinct().size,
        positive_reviews = positiveReviews,
        five_star_ratings = fiveStarRatings,
        total_reviews = reviews.size,
        badges = badges,
        weekly_activity = buildWeeklyActivity(allCompletedRequests),
        request_types = buildRequestTypeShares(handledRequests)
      )
    )
  }

  fun getVolunteerAnalyticsReviews(
    userId: String,
    page: Int = 1,
    perPage: Int = 10,
    rating: Int? = null
  ): ServiceResult<VolunteerAnalyticsReviewsResponseDto> = synchronized(lock) {
    val resolvedVolunteerId = resolveAnalyticsVolunteerId(userId)
    val safePerPage = perPage.coerceIn(1, 100)
    val safePage = page.coerceAtLeast(1)
    val filteredReviews = volunteerReviewsFor(resolvedVolunteerId)
      .filter { rating == null || it.rating == rating }
      .sortedByDescending { it.createdAtEpochSeconds }
    val pagedReviews = filteredReviews
      .drop((safePage - 1) * safePerPage)
      .take(safePerPage)

    ServiceResult.Success(
      VolunteerAnalyticsReviewsResponseDto(
        reviews = pagedReviews.map { it.toAnalyticsReviewDto() },
        page = safePage,
        per_page = safePerPage,
        total = filteredReviews.size,
        average_rating = if (filteredReviews.isEmpty()) 0.0 else filteredReviews.map { it.rating }.average()
      )
    )
  }

  fun submitVolunteerRating(
    userId: String,
    requestId: String,
    request: SubmitVolunteerRatingRequest
  ): ServiceResult<ActionResultDto> = synchronized(lock) {
    val account = accountsById[userId] ?: return failure(HttpStatusCode.NotFound, "Account not found.")
    if (account.role != UserRole.User) {
      return failure(HttpStatusCode.Forbidden, "Only users can rate volunteers.")
    }
    if (request.rating !in 1..5) {
      return failure(HttpStatusCode.BadRequest, "rating must be between 1 and 5.")
    }

    val helpRequest = requestsById[requestId] ?: return failure(HttpStatusCode.NotFound, "Request not found.")
    if (helpRequest.userId != userId) {
      return failure(HttpStatusCode.Forbidden, "You can only rate volunteers for your own requests.")
    }
    if (helpRequest.volunteerId.isNullOrBlank()) {
      return failure(HttpStatusCode.Conflict, "Cannot rate a request without an assigned volunteer.")
    }
    if (helpRequest.status !in completedVolunteerStatuses()) {
      return failure(HttpStatusCode.Conflict, "Volunteer can only be rated after the request is completed.")
    }
    if (volunteerReviewsById.values.any { it.requestId == requestId }) {
      return failure(HttpStatusCode.Conflict, "Volunteer has already been rated for this request.")
    }

    val review = VolunteerReviewRecord(
      id = "review-${UUID.randomUUID()}",
      requestId = requestId,
      volunteerId = helpRequest.volunteerId.orEmpty(),
      userId = userId,
      userName = account.fullName,
      rating = request.rating,
      comment = request.comment?.trim().orEmpty(),
      issues = request.issues.map { it.trim() }.filter { it.isNotBlank() }.distinct(),
      createdAtEpochSeconds = nowEpochSeconds()
    )
    volunteerReviewsById[review.id] = review
    accountDatabase.upsertVolunteerReview(review.toPersistence())
    helpRequest.status = "rated"
    accountDatabase.upsertHelpRequest(helpRequest.toPersistence())

    ServiceResult.Success(ActionResultDto(success = true, message = "Volunteer rated successfully."))
  }

  fun acceptRequest(userId: String, requestId: String): ServiceResult<ActionResultDto> = synchronized(lock) {
    val account = accountsById[userId] ?: return failure(HttpStatusCode.NotFound, "Account not found.")
    if (account.role != UserRole.Volunteer) {
      return failure(HttpStatusCode.Forbidden, "Only volunteers can accept requests.")
    }
    if (account.roleVerifiedAt == null) {
      return failure(HttpStatusCode.Forbidden, "Volunteer account not verified yet.")
    }
    val request = requestsById[requestId] ?: return failure(HttpStatusCode.NotFound, "Request not found.")
    if (request.status != "pending" || request.volunteerId != null) {
      return failure(HttpStatusCode.Conflict, "Request is no longer available.")
    }
    request.volunteerId = userId
    request.volunteerName = account.fullName

    if (request.paymentMethod == PaymentMethod.CASH) {
      // Cash requests become active on acceptance, but the cash payment is only
      // captured once the volunteer marks the request as completed.
      request.status = "active"
      accountDatabase.upsertHelpRequest(request.toPersistence())
      return ServiceResult.Success(ActionResultDto(true, "Request accepted successfully."))
    }

    request.status = "accepted"
    accountDatabase.upsertHelpRequest(request.toPersistence())
    ServiceResult.Success(ActionResultDto(true, "Request accepted successfully."))
  }

  fun payRequest(
    userId: String,
    requestId: String,
    requestedPaymentMethod: PaymentMethod? = null
  ): ServiceResult<PayRequestResponseDto> = synchronized(lock) {
    val account = accountsById[userId] ?: return failure(HttpStatusCode.NotFound, "Account not found.")
    if (account.role != UserRole.User) {
      return failure(HttpStatusCode.Forbidden, "Only users can pay for service.")
    }
    val request = requestsById[requestId] ?: return failure(HttpStatusCode.NotFound, "Request not found.")
    if (request.userId != userId) {
      return failure(HttpStatusCode.Forbidden, "You can only pay for your own requests.")
    }
    if (request.volunteerId.isNullOrBlank()) {
      return failure(HttpStatusCode.Conflict, "Cannot pay before a volunteer accepts the request.")
    }

    val allowedPaymentStatuses = setOf("accepted", "pending_payment")
    if (request.status !in allowedPaymentStatuses) {
      // If already active/completed with a successful payment, return idempotent success.
      val existingPayment = latestSuccessfulPaymentForRequest(requestId)
        ?: latestSuccessfulPaymentForRequestFromDatabase(requestId)
      if (existingPayment != null) {
        return ServiceResult.Success(
          PayRequestResponseDto(
            payment_method = existingPayment.paymentMethod.name.lowercase(Locale.getDefault()),
            status = request.status,
            message = "Payment already confirmed for this request.",
            payment_id = existingPayment.id
          )
        )
      }
      return failure(HttpStatusCode.Conflict, "Request cannot be paid in its current status.")
    }

    val paymentMethodToUse = requestedPaymentMethod ?: request.paymentMethod
    if (paymentMethodToUse !in setOf(PaymentMethod.CARD, PaymentMethod.CASH)) {
      return failure(HttpStatusCode.BadRequest, "payment_method must be card or cash.")
    }
    request.paymentMethod = paymentMethodToUse

    latestSuccessfulPaymentForRequest(requestId)?.let { payment ->
      request.completedAtEpochSeconds = null
      request.status = "active"
      accountDatabase.upsertHelpRequest(request.toPersistence())
      return ServiceResult.Success(
        PayRequestResponseDto(
          payment_method = payment.paymentMethod.name.lowercase(Locale.getDefault()),
          status = request.status,
          message = "Payment already confirmed for this request.",
          checkout_url = payment.checkoutUrl,
          payment_id = payment.id
        )
      )
    }

    if (paymentMethodToUse == PaymentMethod.CASH) {
      cancelPendingPaymentsForRequest(requestId)
      val payment = PaymentRecord(
        id = "pay-${UUID.randomUUID()}",
        requestId = requestId,
        userId = userId,
        amount = request.serviceFee,
        currency = "EGP",
        paymentMethod = PaymentMethod.CASH,
        status = "captured",
        success = true,
        checkoutUrl = null,
        createdAtEpochSeconds = nowEpochSeconds()
      )
      paymentsById[payment.id] = payment
      accountDatabase.upsertPayment(payment.toPersistence())
      request.completedAtEpochSeconds = null
      request.status = "active"
      accountDatabase.upsertHelpRequest(request.toPersistence())
      return ServiceResult.Success(
        PayRequestResponseDto(
          payment_method = "cash",
          status = request.status,
          message = "Cash payment confirmed. Request is now active.",
          payment_id = payment.id
        )
      )
    }

    latestPendingCardPaymentForRequest(requestId)?.let { payment ->
      request.completedAtEpochSeconds = null
      request.status = "pending_payment"
      accountDatabase.upsertHelpRequest(request.toPersistence())
      return ServiceResult.Success(
        PayRequestResponseDto(
          payment_method = "card",
          status = request.status,
          message = "Proceed to Paymob checkout.",
          checkout_url = payment.checkoutUrl,
          payment_id = payment.id
        )
      )
    }

    cancelPendingPaymentsForRequest(requestId)
    request.completedAtEpochSeconds = null
    request.status = "pending_payment"
    accountDatabase.upsertHelpRequest(request.toPersistence())

    val paymentId = "pay-${UUID.randomUUID()}"
    val checkoutUrl = "https://checkout.paymob.com/confirm/$paymentId"
    val payment = PaymentRecord(
      id = paymentId,
      requestId = requestId,
      userId = userId,
      amount = request.serviceFee,
      currency = "EGP",
      paymentMethod = PaymentMethod.CARD,
      status = "pending",
      success = false,
      checkoutUrl = checkoutUrl,
      createdAtEpochSeconds = nowEpochSeconds()
    )
    paymentsById[paymentId] = payment
    accountDatabase.upsertPayment(payment.toPersistence())

    ServiceResult.Success(
      PayRequestResponseDto(
        payment_method = "card",
        status = request.status,
        message = "Proceed to Paymob checkout.",
        checkout_url = checkoutUrl,
        payment_id = paymentId
      )
    )
  }

  fun declineRequest(userId: String, requestId: String): ServiceResult<ActionResultDto> = synchronized(lock) {
    val account = accountsById[userId] ?: return failure(HttpStatusCode.NotFound, "Account not found.")
    if (account.role != UserRole.Volunteer) {
      return failure(HttpStatusCode.Forbidden, "Only volunteers can decline requests.")
    }
    if (account.roleVerifiedAt == null) {
      return failure(HttpStatusCode.Forbidden, "Volunteer account not verified yet.")
    }
    val request = requestsById[requestId] ?: return failure(HttpStatusCode.NotFound, "Request not found.")
    if (request.status != "pending" || request.volunteerId != null) {
      return failure(HttpStatusCode.Conflict, "Request is no longer pending.")
    }
    request.declinedVolunteerIds += userId
    accountDatabase.upsertHelpRequest(request.toPersistence())
    ServiceResult.Success(ActionResultDto(true, "Request declined."))
  }

  fun cancelRequest(userId: String, requestId: String): ServiceResult<ActionResultDto> = synchronized(lock) {
    val account = accountsById[userId] ?: return failure(HttpStatusCode.NotFound, "Account not found.")
    val request = requestsById[requestId] ?: return failure(HttpStatusCode.NotFound, "Request not found.")
    if (account.role != UserRole.User || request.userId != userId) {
      return failure(HttpStatusCode.Forbidden, "You can only cancel your own requests.")
    }
    if (request.status !in setOf("pending", "accepted", "pending_payment")) {
      return failure(HttpStatusCode.Conflict, "Only pending or accepted requests can be cancelled.")
    }
    request.completedAtEpochSeconds = null
    request.status = "cancelled"
    accountDatabase.upsertHelpRequest(request.toPersistence())
    ServiceResult.Success(ActionResultDto(true, "Request cancelled."))
  }

  fun completeRequest(userId: String, requestId: String): ServiceResult<ActionResultDto> = synchronized(lock) {
    val account = accountsById[userId] ?: return failure(HttpStatusCode.NotFound, "Account not found.")
    if (account.role == UserRole.Volunteer && account.roleVerifiedAt == null) {
      return failure(HttpStatusCode.Forbidden, "Volunteer account not verified yet.")
    }
    val request = requestsById[requestId] ?: return failure(HttpStatusCode.NotFound, "Request not found.")
    val isOwner = request.userId == userId
    val isAssignedVolunteer = request.volunteerId == userId
    if (!isOwner && !isAssignedVolunteer) {
      return failure(HttpStatusCode.Forbidden, "You are not allowed to complete this request.")
    }
    // Auto-transition: if payment was confirmed but status is lagging, promote to active.
    if (request.status in setOf("accepted", "pending_payment")) {
      request.healStaleStatus()
      if (request.status !in setOf("active", "confirmed", "in_progress")) {
        return failure(HttpStatusCode.Conflict, "Payment has not been completed yet. Please wait for the user to pay before marking as completed.")
      }
    }
    if (request.status !in setOf("active", "confirmed", "in_progress")) {
      return failure(HttpStatusCode.Conflict, "Request can only be completed when active or confirmed.")
    }
    if (request.paymentMethod == PaymentMethod.CASH) {
      // Completing a cash request is the business event that settles volunteer earnings.
      captureCashPaymentForRequest(request)
    }
    request.completedAtEpochSeconds = nowEpochSeconds()
    request.status = "completed"
    accountDatabase.upsertHelpRequest(request.toPersistence())
    if (isAssignedVolunteer) {
      account.contributionStats = account.contributionStats.copy(
        helpfulVotes = account.contributionStats.helpfulVotes + 1
      )
      persistAccount(account)
    }
    ServiceResult.Success(ActionResultDto(true, "Request marked as completed."))
  }

  fun getRequestMessages(userId: String, requestId: String, perPage: Int = 20): ServiceResult<List<HelpRequestMessageDto>> = synchronized(lock) {
    val request = requestsById[requestId] ?: return failure(HttpStatusCode.NotFound, "Request not found.")
    if (request.userId != userId && request.volunteerId != userId) {
      return failure(HttpStatusCode.Forbidden, "You are not a participant in this request.")
    }
    val messages = accountDatabase.loadHelpRequestMessages(requestId)
      .sortedByDescending { it.createdAtEpochSeconds }
      .take(perPage)
      .reversed()
      .map {
        HelpRequestMessageDto(it.id, it.senderId, it.senderName, it.message, it.createdAtEpochSeconds)
      }
    ServiceResult.Success(messages)
  }

  fun sendMessage(userId: String, requestId: String, messageText: String): ServiceResult<HelpRequestMessageDto> = synchronized(lock) {
    val account = accountsById[userId] ?: return failure(HttpStatusCode.NotFound, "Account not found.")
    val request = requestsById[requestId] ?: return failure(HttpStatusCode.NotFound, "Request not found.")
    if (request.userId != userId && request.volunteerId != userId) {
      return failure(HttpStatusCode.Forbidden, "You are not a participant in this request.")
    }

    val message = HelpRequestMessageRecord(
      id = "msg-${UUID.randomUUID()}",
      requestId = requestId,
      senderId = userId,
      senderName = account.fullName,
      message = messageText,
      createdAtEpochSeconds = nowEpochSeconds()
    )
    accountDatabase.insertHelpRequestMessage(HelpRequestMessagePersistence(
      message.id, message.requestId, message.senderId, message.senderName, message.message, message.createdAtEpochSeconds
    ))
    ServiceResult.Success(HelpRequestMessageDto(message.id, message.senderId, message.senderName, message.message, message.createdAtEpochSeconds))
  }

  fun checkoutCard(userId: String, requestId: String?, amountEgp: Double?): ServiceResult<CheckoutResponseDto> = synchronized(lock) {
    val resolvedAmount = resolveCheckoutAmount(userId, requestId, amountEgp)
    val resolvedRequestId = requestId ?: findLatestPendingRequestId(userId)
    if (resolvedRequestId != null) {
      val request = requestsById[resolvedRequestId]
      if (request != null && request.status in setOf("accepted", "pending_payment")) {
        request.status = "pending_payment"
        accountDatabase.upsertHelpRequest(request.toPersistence())
      }
    }
    val paymentId = "pay-${UUID.randomUUID()}"
    val checkoutUrl = "https://checkout.paymob.com/confirm/$paymentId"
    val payment = PaymentRecord(
      id = paymentId,
      requestId = resolvedRequestId,
      userId = userId,
      amount = resolvedAmount,
      currency = "EGP",
      paymentMethod = PaymentMethod.CARD,
      status = "pending",
      success = false,
      checkoutUrl = checkoutUrl,
      createdAtEpochSeconds = nowEpochSeconds()
    )
    paymentsById[paymentId] = payment
    accountDatabase.upsertPayment(payment.toPersistence())
    ServiceResult.Success(CheckoutResponseDto(checkoutUrl, paymentId))
  }

  fun checkoutWallet(userId: String, requestId: String?, amountEgp: Double?): ServiceResult<CheckoutResponseDto> = synchronized(lock) {
    val resolvedAmount = resolveCheckoutAmount(userId, requestId, amountEgp)
    val resolvedRequestId = requestId ?: findLatestPendingRequestId(userId)
    if (resolvedRequestId != null) {
      val request = requestsById[resolvedRequestId]
      if (request != null && request.status in setOf("accepted", "pending_payment")) {
        request.status = "pending_payment"
        accountDatabase.upsertHelpRequest(request.toPersistence())
      }
    }
    val paymentId = "pay-${UUID.randomUUID()}"
    val checkoutUrl = "https://checkout.paymob.com/wallet/$paymentId"
    val payment = PaymentRecord(
      id = paymentId,
      requestId = resolvedRequestId,
      userId = userId,
      amount = resolvedAmount,
      currency = "EGP",
      paymentMethod = PaymentMethod.WALLET,
      status = "pending",
      success = false,
      checkoutUrl = checkoutUrl,
      createdAtEpochSeconds = nowEpochSeconds()
    )
    paymentsById[paymentId] = payment
    accountDatabase.upsertPayment(payment.toPersistence())
    ServiceResult.Success(CheckoutResponseDto(checkoutUrl, paymentId))
  }

  private fun resolveCheckoutAmount(userId: String, requestId: String?, amountEgp: Double?): Double {
    if (amountEgp != null && amountEgp > 0.0) return amountEgp
    if (requestId != null) {
      val request = requestsById[requestId]
      if (request != null) return request.serviceFee
    }
    val latestRequest = requestsById.values
      .filter { it.userId == userId && it.status in setOf("accepted", "pending_payment") }
      .maxByOrNull { it.createdAtEpochSeconds }
    return latestRequest?.serviceFee ?: 0.0
  }

  private fun findLatestPendingRequestId(userId: String): String? {
    return requestsById.values
      .filter { it.userId == userId && it.status in setOf("accepted", "pending_payment") }
      .maxByOrNull { it.createdAtEpochSeconds }
      ?.id
  }

  fun getPaymentStatus(paymentId: String): ServiceResult<PaymentStatusDto> = synchronized(lock) {
    val payment = paymentsById[paymentId] ?: return failure(HttpStatusCode.NotFound, "Payment not found.")
    ServiceResult.Success(PaymentStatusDto(payment.id, payment.status, payment.amount, payment.currency, payment.success))
  }

  fun refreshPayment(paymentId: String): ServiceResult<PaymentStatusDto> = synchronized(lock) {
    val payment = paymentsById[paymentId] ?: return failure(HttpStatusCode.NotFound, "Payment not found.")
    if (!payment.success && payment.status == "pending" && payment.paymentMethod in setOf(PaymentMethod.CARD, PaymentMethod.WALLET)) {
      // This backend uses an app-driven checkout flow in addition to server callbacks.
      // When the client refreshes a pending digital payment after completing checkout,
      // treat that as the confirmation point and unlock the volunteer's active request.
      payment.success = true
      payment.status = "captured"
      if (payment.requestId != null) {
        val request = requestsById[payment.requestId]
        if (request != null && request.status == "pending_payment") {
          request.completedAtEpochSeconds = null
          request.status = "active"
          accountDatabase.upsertHelpRequest(request.toPersistence())
        }
      }
    }
    accountDatabase.upsertPayment(payment.toPersistence())
    ServiceResult.Success(PaymentStatusDto(payment.id, payment.status, payment.amount, payment.currency, payment.success))
  }

  fun handlePaymobCallback(paymentId: String, success: Boolean): ServiceResult<ActionResultDto> = synchronized(lock) {
    val payment = paymentsById[paymentId] ?: return failure(HttpStatusCode.NotFound, "Payment not found.")
    payment.success = success
    payment.status = if (success) "captured" else "failed"
    accountDatabase.upsertPayment(payment.toPersistence())

    if (success && payment.requestId != null) {
      val request = requestsById[payment.requestId]
      if (request != null && request.status == "pending_payment") {
        request.completedAtEpochSeconds = null
        request.status = "active"
        accountDatabase.upsertHelpRequest(request.toPersistence())
      }
    }
    ServiceResult.Success(ActionResultDto(true, "Payment updated."))
  }

  private fun latestSuccessfulPaymentForRequest(requestId: String): PaymentRecord? {
    return paymentsForRequest(requestId)
      .filter { it.success }
      .maxByOrNull { it.createdAtEpochSeconds }
  }

  private fun latestSuccessfulPaymentForRequestFromDatabase(requestId: String): PaymentRecord? {
    val persisted = accountDatabase.findLatestSuccessfulPaymentForRequest(requestId) ?: return null
    val record = persisted.toRecord()
    paymentsById[record.id] = record
    return record
  }

  private fun hasPersistedSuccessfulPaymentForRequest(requestId: String): Boolean {
    return latestSuccessfulPaymentForRequest(requestId) != null ||
      latestSuccessfulPaymentForRequestFromDatabase(requestId) != null
  }

  private fun latestRelevantPaymentForRequest(requestId: String): PaymentRecord? {
    return latestSuccessfulPaymentForRequest(requestId)
      ?: latestSuccessfulPaymentForRequestFromDatabase(requestId)
      ?: paymentsForRequest(requestId).maxByOrNull { it.createdAtEpochSeconds }
  }

  private fun latestPendingCardPaymentForRequest(requestId: String): PaymentRecord? {
    return paymentsForRequest(requestId)
      .filter { it.paymentMethod == PaymentMethod.CARD && it.status == "pending" && !it.success }
      .maxByOrNull { it.createdAtEpochSeconds }
  }

  private fun captureCashPaymentForRequest(request: AssistanceRequestRecord): PaymentRecord {
    latestSuccessfulPaymentForRequest(request.id)?.takeIf { it.paymentMethod == PaymentMethod.CASH }?.let { payment ->
      return payment
    }

    cancelPendingPaymentsForRequest(request.id)
    val payment = PaymentRecord(
      id = "pay-${UUID.randomUUID()}",
      requestId = request.id,
      userId = request.userId,
      amount = request.serviceFee,
      currency = "EGP",
      paymentMethod = PaymentMethod.CASH,
      status = "captured",
      success = true,
      checkoutUrl = null,
      createdAtEpochSeconds = nowEpochSeconds()
    )
    paymentsById[payment.id] = payment
    accountDatabase.upsertPayment(payment.toPersistence())
    return payment
  }

  private fun cancelPendingPaymentsForRequest(requestId: String) {
    paymentsForRequest(requestId)
      .filter { it.status == "pending" && !it.success }
      .forEach { payment ->
        payment.status = "cancelled"
        payment.success = false
        accountDatabase.upsertPayment(payment.toPersistence())
      }
  }

  private fun paymentsForRequest(requestId: String): List<PaymentRecord> {
    return paymentsById.values.filter { it.requestId == requestId }
  }

  fun getLocations(): List<LocationDto> = synchronized(lock) {
    locationsById.values.map { it.toDto() }
  }

  fun searchLocations(queryRaw: String): List<LocationDto> = synchronized(lock) {
    val query = queryRaw.trim().lowercase(Locale.getDefault())
    if (query.isBlank()) return locationsById.values.map { it.toDto() }
    locationsById.values
      .filter {
        it.name.lowercase(Locale.getDefault()).contains(query) ||
          it.category.lowercase(Locale.getDefault()).contains(query)
      }
      .map { it.toDto() }
  }

  fun submitLocationRating(userId: String, locationId: String, request: SubmitRatingRequest): ServiceResult<LocationDto> = synchronized(lock) {
    val account = accountsById[userId] ?: return failure(HttpStatusCode.NotFound, "Account not found.")
    val location = locationsById[locationId] ?: return failure(HttpStatusCode.NotFound, "Location not found.")
    if (request.score !in 1..5) {
      return failure(HttpStatusCode.BadRequest, "Score must be between 1 and 5.")
    }

    val total = location.totalRatings
    val newTotal = total + 1
    val newAverage = ((location.rating * total) + request.score) / newTotal
    location.totalRatings = newTotal
    location.rating = ((newAverage * 10.0).roundToInt()) / 10.0
    val report = request.comment?.takeIf { it.isNotBlank() }
      ?: "Rated ${request.score}/5 by ${account.fullName}"
    location.recentReports.add(0, report)
    if (location.recentReports.size > 10) {
      location.recentReports.removeAt(location.recentReports.lastIndex)
    }

    account.contributionStats = account.contributionStats.copy(
      ratingsSubmitted = account.contributionStats.ratingsSubmitted + 1,
      reportsSubmitted = account.contributionStats.reportsSubmitted + if (request.comment.isNullOrBlank()) 0 else 1
    )
    persistAccount(account)

    ServiceResult.Success(location.toDto())
  }

  fun saveSupportMessage(userId: String, request: SupportMessageRequest): ServiceResult<ActionResultDto> = synchronized(lock) {
    val account = accountsById[userId] ?: return failure(HttpStatusCode.NotFound, "Account not found.")
    if (request.subject.isBlank() || request.message.isBlank()) {
      return failure(HttpStatusCode.BadRequest, "Subject and message are required.")
    }
    supportMessages += SupportMessageRecord(
      id = "msg-${UUID.randomUUID()}",
      userId = userId,
      userEmail = account.email,
      subject = request.subject.trim(),
      message = request.message.trim(),
      createdAtEpochSeconds = nowEpochSeconds()
    )
    ServiceResult.Success(ActionResultDto(true, "Support message received."))
  }

  private fun buildVolunteerCounts(userId: String, includeIncoming: Boolean): VolunteerCountsDto {
    val incomingCount = if (includeIncoming) {
      requestsById.values.count {
        it.status == "pending" &&
          it.volunteerId == null &&
          !it.declinedVolunteerIds.contains(userId)
      }
    } else {
      0
    }

    val activeCount = requestsById.values.count {
      it.volunteerId == userId && it.status in activeVolunteerStatuses()
    }
    val historyCount = requestsById.values.count {
      it.volunteerId == userId && it.status in historyVolunteerStatuses()
    }

    return VolunteerCountsDto(
      incoming = incomingCount,
      active = activeCount,
      history = historyCount
    )
  }

  private fun volunteerReviewsFor(volunteerId: String): List<VolunteerReviewRecord> {
    return volunteerReviewsById.values
      .filter { it.volunteerId == volunteerId }
      .sortedByDescending { it.createdAtEpochSeconds }
  }

  private fun successfulPaidVolunteerRequests(volunteerId: String): List<AssistanceRequestRecord> {
    return requestsById.values
      .filter { request ->
        request.volunteerId == volunteerId &&
          request.status in setOf("completed", "rated", "archived") &&
          latestSuccessfulPaymentForRequest(request.id) != null
      }
  }

  private data class VolunteerSettledPayment(
    val payment: PaymentRecord,
    val request: AssistanceRequestRecord
  )

  private fun settledVolunteerPayments(volunteerId: String): List<VolunteerSettledPayment> {
    return requestsById.values
      .asSequence()
      .filter { it.volunteerId == volunteerId && it.status in historyVolunteerStatuses() }
      .mapNotNull { request ->
        latestSuccessfulPaymentForRequest(request.id)?.let { payment ->
          VolunteerSettledPayment(payment = payment, request = request)
        }
      }
      .sortedByDescending { it.payment.createdAtEpochSeconds }
      .toList()
  }

  private fun pendingVolunteerPayments(volunteerId: String): List<VolunteerSettledPayment> {
    return requestsById.values
      .asSequence()
      .filter { it.volunteerId == volunteerId && it.status in activeVolunteerStatuses() }
      .mapNotNull { request ->
        latestSuccessfulPaymentForRequest(request.id)?.let { payment ->
          VolunteerSettledPayment(payment = payment, request = request)
        }
      }
      .sortedByDescending { it.payment.createdAtEpochSeconds }
      .toList()
  }

  private fun volunteerAnalyticsPaymentHistory(userId: String): List<VolunteerAnalyticsPaymentRecordDto> {
    return paymentsById.values
      .mapNotNull { payment ->
        val requestId = payment.requestId ?: return@mapNotNull null
        val request = requestsById[requestId] ?: return@mapNotNull null
        if (request.volunteerId != userId) return@mapNotNull null
        VolunteerAnalyticsPaymentRecordDto(
          id = payment.id,
          date = Instant.ofEpochSecond(payment.createdAtEpochSeconds)
            .atZone(ZoneOffset.UTC)
            .format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)),
          user = request.userName,
          hours = request.hours,
          gross = request.grossAmount(),
          net = request.netAmount(),
          status = when {
            payment.success -> "completed"
            payment.status.equals("cancelled", ignoreCase = true) ||
              payment.status.equals("failed", ignoreCase = true) -> "cancelled"
            else -> "pending"
          }
        )
      }
      .sortedByDescending { record ->
        paymentsById[record.id]?.createdAtEpochSeconds ?: 0L
      }
      .take(20)
  }

  private fun averageVolunteerRating(volunteerId: String): Float {
    val reviews = volunteerReviewsFor(volunteerId)
    return if (reviews.isEmpty()) 0f else reviews.map { it.rating }.average().toFloat()
  }

  private fun AssistanceRequestRecord.grossAmount(): Double = hours * pricePerHour.toDouble()

  private fun AssistanceRequestRecord.platformFeeAmount(): Double = grossAmount() * 0.30

  private fun AssistanceRequestRecord.netAmount(): Double = grossAmount() - platformFeeAmount()

  private fun AssistanceRequestRecord.yearMonth(): YearMonth {
    return YearMonth.from(Instant.ofEpochSecond(analyticsEpochSeconds()).atZone(analyticsZone))
  }

  private fun AssistanceRequestRecord.analyticsDate(): String {
    return Instant.ofEpochSecond(analyticsEpochSeconds())
      .atZone(analyticsZone)
      .format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH))
  }

  private fun AssistanceRequestRecord.analyticsEpochSeconds(): Long {
    return completedAtEpochSeconds ?: createdAtEpochSeconds
  }

  private fun WithdrawalRecord.analyticsDate(): String {
    return Instant.ofEpochSecond(createdAtEpochSeconds)
      .atZone(analyticsZone)
      .format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH))
  }

  private fun VolunteerReviewRecord.toAnalyticsReviewDto(): VolunteerAnalyticsReviewDto {
    return VolunteerAnalyticsReviewDto(
      id = id,
      user_name = userName,
      rating = rating,
      comment = comment,
      date = Instant.ofEpochSecond(createdAtEpochSeconds)
        .atZone(analyticsZone)
        .format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)),
      issues = issues
    )
  }

  private fun monthLabelFormatter(): DateTimeFormatter {
    return DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)
  }

  private fun percentageChange(current: Double, previous: Double): Double {
    return when {
      previous == 0.0 && current == 0.0 -> 0.0
      previous == 0.0 -> 100.0
      else -> ((current - previous) / previous) * 100.0
    }
  }

  private fun performanceGrade(score: Float): String {
    return when {
      score >= 96f -> "A+"
      score >= 90f -> "A"
      score >= 85f -> "B+"
      score >= 80f -> "B"
      score >= 75f -> "C+"
      score >= 70f -> "C"
      else -> "D"
    }
  }

  private fun performanceHeadline(score: Float): String {
    return when {
      score >= 95f -> "Excellent Performance"
      score >= 85f -> "Strong Performance"
      score >= 75f -> "Good Performance"
      score >= 65f -> "Improving Performance"
      else -> "Needs Attention"
    }
  }

  private fun performancePercentile(score: Float): Int {
    return when {
      score >= 95f -> 10
      score >= 90f -> 15
      score >= 85f -> 25
      score >= 80f -> 35
      score >= 75f -> 45
      score >= 70f -> 55
      else -> 70
    }
  }

  private fun performanceBadges(
    completedCount: Int,
    usersHelped: Int,
    averageRating: Float,
    responseRate: Float,
    completionRate: Float
  ): List<String> {
    val badges = mutableListOf<String>()
    if (averageRating >= 4.7f) badges += "Top Rated"
    if (responseRate >= 90f) badges += "Quick Responder"
    if (completionRate >= 90f) badges += "Reliable"
    if (completedCount >= 50) badges += "50 Requests"
    if (usersHelped >= 25) badges += "Community Hero"
    if (badges.isEmpty() && completedCount > 0) badges += "Rising Star"
    return badges
  }

  private fun buildWeeklyActivity(requests: List<AssistanceRequestRecord>): List<VolunteerAnalyticsWeeklyActivityDto> {
    val today = LocalDate.now(analyticsZone)
    return (6L downTo 0L).map { offset ->
      val day = today.minusDays(offset)
      val completedCount = requests.count {
        Instant.ofEpochSecond(it.analyticsEpochSeconds()).atZone(analyticsZone).toLocalDate() == day
      }
      VolunteerAnalyticsWeeklyActivityDto(
        day = day.format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)),
        completed = completedCount
      )
    }
  }

  private fun buildRequestTypeShares(requests: List<AssistanceRequestRecord>): List<VolunteerAnalyticsRequestTypeShareDto> {
    if (requests.isEmpty()) return emptyList()
    return requests
      .groupingBy { it.helpType.ifBlank { "Other" } }
      .eachCount()
      .entries
      .sortedByDescending { it.value }
      .map { (name, count) ->
        VolunteerAnalyticsRequestTypeShareDto(
          name = name,
          value = count
        )
      }
  }

  private fun activeVolunteerStatuses(): Set<String> {
    return setOf("active", "accepted", "in_progress", "pending_payment", "confirmed")
  }

  private fun historyVolunteerStatuses(): Set<String> {
    return setOf("completed", "rated", "archived", "cancelled", "no_volunteer")
  }

  private fun completedVolunteerStatuses(): Set<String> {
    return setOf("completed", "rated", "archived")
  }

  private fun isWithinLastDays(epochSeconds: Long, days: Long): Boolean {
    val eventDate = Instant.ofEpochSecond(epochSeconds).atZone(analyticsZone).toLocalDate()
    val today = LocalDate.now(analyticsZone)
    val threshold = today.minusDays(days - 1)
    return !eventDate.isBefore(threshold)
  }

  private fun validateCommonRegistration(fullName: String, email: String, password: String): String? {
    if (fullName.isBlank()) return "Full name is required."
    if (!emailRegex.matches(email)) return "Please enter a valid email address."
    if (password.length < 8) return "Password must be at least 8 characters."
    return null
  }

  private fun ensureEmailAvailable(email: String): String? {
    val accountId = accountIdByEmail[email] ?: return null
    val existing = accountsById[accountId] ?: return null
    val roleName = if (existing.role == UserRole.Volunteer) "volunteer" else "user"
    return "This email is already registered as $roleName. Please sign in."
  }

  private fun savePendingRegistrationLocked(record: PendingRegistrationRecord) {
    pendingRegistrationsByChallengeId[record.challengeId] = record
    pendingChallengeIdByEmail[record.email] = record.challengeId
  }

  private fun clearPendingRegistrationByEmailLocked(email: String) {
    val challengeId = pendingChallengeIdByEmail[email] ?: return
    clearPendingRegistrationLocked(challengeId)
  }

  private fun clearPendingRegistrationLocked(challengeId: String) {
    val removed = pendingRegistrationsByChallengeId.remove(challengeId) ?: return
    pendingChallengeIdByEmail.remove(removed.email, challengeId)
  }

  private fun pruneExpiredPendingRegistrationsLocked(nowEpochSeconds: Long = nowEpochSeconds()) {
    val expiredIds = pendingRegistrationsByChallengeId.values
      .filter { it.expiresAtEpochSeconds <= nowEpochSeconds }
      .map { it.challengeId }
    expiredIds.forEach { clearPendingRegistrationLocked(it) }
  }

  private fun saveAccount(account: AccountRecord) {
    clearPendingRegistrationByEmailLocked(account.email)
    saveAccountInMemory(account)
    persistAccount(account)
  }

  private fun saveAccountInMemory(account: AccountRecord) {
    accountsById[account.id] = account
    accountIdByEmail[account.email] = account.id
  }

  private fun persistAccount(account: AccountRecord) {
    accountDatabase.upsertAccount(account)
  }

  private fun failure(status: HttpStatusCode, message: String): ServiceResult.Failure {
    return ServiceResult.Failure(status = status, message = message)
  }

  private fun nowEpochSeconds(): Long = Instant.now().epochSecond

  private fun currentMonthYear(): String {
    val now = Instant.now().atZone(ZoneOffset.UTC)
    return now.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))
  }

  private fun AssistanceRequestRecord.relativeRequestTime(nowEpochSeconds: Long = nowEpochSeconds()): String {
    val diff = (nowEpochSeconds - createdAtEpochSeconds).coerceAtLeast(0)
    val minutes = diff / 60
    val hours = diff / 3600
    val days = diff / 86400
    return when {
      minutes < 1 -> "Just now"
      minutes < 60 -> "$minutes mins ago"
      hours < 24 -> "$hours hours ago"
      else -> "$days days ago"
    }
  }

  /**
   * Auto-heal: if a request is stuck in accepted/pending_payment but has a
   * successful payment (or is cash), promote it to "active" so every consumer
   * sees the correct state.  Also auto-confirm any pending card/wallet payment
   * when the request is read, so that checkout-then-close-app scenarios recover.
   */
  private fun AssistanceRequestRecord.healStaleStatus() {
    if (status !in setOf("accepted", "pending_payment")) return

    // 1. If there is already a successful payment, just promote.
    if (hasPersistedSuccessfulPaymentForRequest(id)) {
      status = "active"
      completedAtEpochSeconds = null
      accountDatabase.upsertHelpRequest(toPersistence())
      return
    }

    // 2. Cash requests that were accepted are active-equivalent.
    if (paymentMethod == PaymentMethod.CASH && volunteerId != null) {
      status = "active"
      completedAtEpochSeconds = null
      accountDatabase.upsertHelpRequest(toPersistence())
      return
    }

    // 3. Auto-confirm any pending card/wallet payment (simulates user returning
    //    from checkout without tapping "refresh").
    val pendingDigital = paymentsForRequest(id)
      .filter { !it.success && it.status == "pending" && it.paymentMethod in setOf(PaymentMethod.CARD, PaymentMethod.WALLET) }
      .maxByOrNull { it.createdAtEpochSeconds }
    if (pendingDigital != null) {
      pendingDigital.success = true
      pendingDigital.status = "captured"
      accountDatabase.upsertPayment(pendingDigital.toPersistence())
      status = "active"
      completedAtEpochSeconds = null
      accountDatabase.upsertHelpRequest(toPersistence())
    }
  }

  private fun AssistanceRequestRecord.toVolunteerRequestDto(): VolunteerRequestDto {
    healStaleStatus()
    val paymentSnapshot = latestRelevantPaymentForRequest(id)
    return VolunteerRequestDto(
      id = id,
      userId = userId,
      userName = userName,
      userType = userType,
      location = location,
      requestTime = relativeRequestTime(),
      status = status,
      volunteerName = volunteerName,
      description = description,
      hours = hours,
      price_per_hour = pricePerHour,
      total_amount_egp = serviceFee,
      payment_method = paymentMethod.name.lowercase(),
      payment_status = paymentSnapshot?.status,
      is_paid = paymentSnapshot?.success == true
    )
  }

  private fun AssistanceRequestRecord.toAssistanceRequestDto(): AssistanceRequestDto {
    healStaleStatus()
    val paymentSnapshot = latestRelevantPaymentForRequest(id)
    return AssistanceRequestDto(
      id = id,
      userName = userName,
      userType = userType,
      location = location,
      destination = destination,
      distance = distance,
      urgency = urgency,
      helpType = helpType,
      requestTime = relativeRequestTime(),
      status = status,
      hours = hours,
      price_per_hour = pricePerHour,
      total_amount_egp = serviceFee,
      payment_method = paymentMethod.name.lowercase(),
      payment_status = paymentSnapshot?.status,
      is_paid = paymentSnapshot?.success == true
    )
  }

  private fun LocationRecord.toDto(): LocationDto {
    return LocationDto(
      id = id,
      name = name,
      category = category,
      lat = lat,
      lng = lng,
      rating = rating,
      totalRatings = totalRatings,
      features = features,
      recentReports = recentReports.toList(),
      distance = distance
    )
  }

  internal fun AccountRecord.toDto(): AuthUserDto {
    return AuthUserDto(
      id = id,
      role = role,
      fullName = fullName,
      email = email,
      phone = phone,
      location = location,
      disabilityType = disabilityType,
      memberSince = memberSince,
      volunteerLive = volunteerLive,
      roleVerifiedAt = roleVerifiedAt,
      contributionStats = contributionStats,
      notificationSettings = notificationSettings,
      privacySettings = privacySettings
    )
  }

  private fun seedAccounts() {
    if (!accountsById.containsKey("user-seed-1")) {
      val user = AccountRecord(
        id = "user-seed-1",
        role = UserRole.User,
        fullName = "Layla Abdullah",
        email = "user@athar.app",
        passwordHash = BCrypt.hashpw("Password123!", BCrypt.gensalt()),
        phone = "+966 50 123 4567",
        location = "Riyadh, Saudi Arabia",
        disabilityType = "Wheelchair user",
        memberSince = "March 2024",
        volunteerLive = false,
        roleVerifiedAt = null,
        contributionStats = ContributionStatsDto(12, 8, 34),
        notificationSettings = NotificationSettingsDto(),
        privacySettings = PrivacySettingsDto()
      )
      saveAccount(user)
      accountDatabase.upsertUserProfile(
        accountId = user.id,
        profile = UserProfilePersistence(
          emergencyContactName = "Maha Abdullah",
          emergencyContactPhone = "+966 50 000 0000"
        )
      )
    }

    if (!accountsById.containsKey("vol-seed-1")) {
      val volunteer = AccountRecord(
        id = "vol-seed-1",
        role = UserRole.Volunteer,
        fullName = "Sara Mohammed",
        email = "volunteer@athar.app",
        passwordHash = BCrypt.hashpw("Password123!", BCrypt.gensalt()),
        phone = "+966 55 987 6543",
        location = "Riyadh, Saudi Arabia",
        disabilityType = null,
        memberSince = "April 2024",
        volunteerLive = true,
        roleVerifiedAt = "2024-04-01T10:00:00Z",
        contributionStats = ContributionStatsDto(4, 2, 11),
        notificationSettings = NotificationSettingsDto(),
        privacySettings = PrivacySettingsDto()
      )
      saveAccount(volunteer)
      accountDatabase.upsertVolunteerProfile(
        accountId = volunteer.id,
        profile = VolunteerProfilePersistence(
          nationalId = "1234567890",
          dateOfBirth = "1998-10-10",
          motivation = "I want to help people in my community.",
          languages = listOf("Arabic", "English"),
          availability = listOf("Weekday mornings", "Weekend evenings"),
          idDocumentFileName = null,
          idDocumentContentType = null,
          idDocumentSizeBytes = null,
          idDocumentBytes = null
        )
      )
    }
  }

  private fun seedLocationsAndRequests() {
    locationsById["1"] = LocationRecord(
      id = "1",
      name = "City Stars Mall",
      category = "Shopping",
      lat = 30.0726,
      lng = 31.3498,
      rating = 4.5,
      totalRatings = 127,
      features = LocationFeaturesDto(
        ramp = true,
        elevator = true,
        accessibleToilet = true,
        accessibleParking = true,
        wideEntrance = true,
        brailleSignage = false
      ),
      recentReports = mutableListOf("Elevator working", "Clean accessible toilet"),
      distance = "0.5 km"
    )
    locationsById["2"] = LocationRecord(
      id = "2",
      name = "Giza Public Library",
      category = "Public Service",
      lat = 30.0131,
      lng = 31.2189,
      rating = 4.8,
      totalRatings = 89,
      features = LocationFeaturesDto(
        ramp = true,
        elevator = true,
        accessibleToilet = true,
        accessibleParking = false,
        wideEntrance = true,
        brailleSignage = true
      ),
      recentReports = mutableListOf("Excellent braille signage", "Staff very helpful"),
      distance = "1.2 km"
    )
    locationsById["3"] = LocationRecord(
      id = "3",
      name = "Orman Garden",
      category = "Recreation",
      lat = 30.0350,
      lng = 31.2136,
      rating = 3.9,
      totalRatings = 54,
      features = LocationFeaturesDto(
        ramp = true,
        elevator = false,
        accessibleToilet = true,
        accessibleParking = true,
        wideEntrance = true,
        brailleSignage = false
      ),
      recentReports = mutableListOf("Some paths uneven", "Accessible toilet needs repair"),
      distance = "2.1 km"
    )

    // Only seed requests if they don't already exist in the database
    if (!requestsById.containsKey("req-seed-1")) {
      val user = accountsById["user-seed-1"] ?: return
      val volunteer = seedVolunteerAccount()
      val now = nowEpochSeconds()
      val seed1 = AssistanceRequestRecord(
        id = "req-seed-1",
        userId = user.id,
        userName = user.fullName,
        userType = user.disabilityType ?: "User",
        location = "Central Mall entrance",
        destination = "Central Mall Level 2",
        distance = "0.3 km",
        urgency = "medium",
        helpType = "Navigation assistance",
        description = "Need assistance navigating to accessible entrance.",
        paymentMethod = PaymentMethod.CASH,
        serviceFee = 0.0,
        createdAtEpochSeconds = now - 600,
        status = "pending"
      )
      requestsById[seed1.id] = seed1
      accountDatabase.upsertHelpRequest(seed1.toPersistence())

      if (volunteer != null && !requestsById.containsKey("req-seed-2")) {
        val seed2 = AssistanceRequestRecord(
          id = "req-seed-2",
          userId = user.id,
          userName = user.fullName,
          userType = user.disabilityType ?: "User",
          location = "City Library",
          destination = "Braille Section",
          distance = "0.8 km",
          urgency = "low",
          helpType = "Finding location",
          description = "Help finding braille section.",
          paymentMethod = PaymentMethod.CARD,
          serviceFee = 50.0,
          createdAtEpochSeconds = now - 3600,
          status = "active",
          volunteerId = volunteer.id,
          volunteerName = volunteer.fullName
        )
        requestsById[seed2.id] = seed2
        accountDatabase.upsertHelpRequest(seed2.toPersistence())
      }

      // Seed completed requests with payments for analytics data
      if (volunteer != null && !requestsById.containsKey("req-seed-3")) {
        val daySeconds = 86400L
        val completedSeeds = listOf(
          Triple("req-seed-3", "Navigation assistance" to "Helped navigate to metro station.", now - daySeconds * 2),
          Triple("req-seed-4", "Wheelchair assistance" to "Wheelchair ramp guidance at mall.", now - daySeconds * 5),
          Triple("req-seed-5", "Finding location" to "Guided to pharmacy counter.", now - daySeconds * 8),
          Triple("req-seed-6", "Navigation assistance" to "Helped cross busy intersection.", now - daySeconds * 12),
          Triple("req-seed-7", "Document reading" to "Read prescription labels.", now - daySeconds * 18),
          Triple("req-seed-8", "Navigation assistance" to "Guided through airport terminal.", now - daySeconds * 25),
          Triple("req-seed-9", "Shopping assistance" to "Helped with grocery shopping.", now - daySeconds * 35),
          Triple("req-seed-10", "Wheelchair assistance" to "Assisted boarding the bus.", now - daySeconds * 50)
        )
        val prices = listOf(50, 75, 60, 80, 45, 100, 55, 70)
        val hours = listOf(1, 2, 1, 2, 1, 3, 1, 2)
        completedSeeds.forEachIndexed { index, (id, typeDesc, createdAt) ->
          val request = AssistanceRequestRecord(
            id = id,
            userId = user.id,
            userName = user.fullName,
            userType = user.disabilityType ?: "User",
            location = "Cairo, Egypt",
            destination = "Destination ${index + 1}",
            distance = "${(index + 1) * 0.3} km",
            urgency = if (index % 2 == 0) "medium" else "low",
            helpType = typeDesc.first,
            description = typeDesc.second,
            paymentMethod = if (index % 3 == 0) PaymentMethod.CASH else PaymentMethod.CARD,
            serviceFee = (hours[index] * prices[index]).toDouble(),
            hours = hours[index],
            pricePerHour = prices[index],
            createdAtEpochSeconds = createdAt,
            completedAtEpochSeconds = createdAt,
            status = if (index < 6) "completed" else "rated",
            volunteerId = volunteer.id,
            volunteerName = volunteer.fullName
          )
          requestsById[request.id] = request
          accountDatabase.upsertHelpRequest(request.toPersistence())

          val payment = PaymentRecord(
            id = "pay-seed-${index + 1}",
            requestId = request.id,
            userId = user.id,
            amount = request.serviceFee,
            currency = "EGP",
            paymentMethod = request.paymentMethod,
            status = "captured",
            success = true,
            checkoutUrl = null,
            createdAtEpochSeconds = createdAt + 1800
          )
          paymentsById[payment.id] = payment
          accountDatabase.upsertPayment(payment.toPersistence())
        }

        // Seed some volunteer reviews
        if (!volunteerReviewsById.containsKey("review-seed-1")) {
          val reviewSeeds = listOf(
            Triple("review-seed-1", 5 to "Excellent help, very patient!", "req-seed-3"),
            Triple("review-seed-2", 4 to "Good assistance, arrived quickly.", "req-seed-4"),
            Triple("review-seed-3", 5 to "Outstanding service!", "req-seed-5"),
            Triple("review-seed-4", 4 to "Very helpful and kind.", "req-seed-6"),
            Triple("review-seed-5", 5 to "Best volunteer experience.", "req-seed-8")
          )
          reviewSeeds.forEachIndexed { index, (id, ratingComment, requestId) ->
            val review = VolunteerReviewRecord(
              id = id,
              requestId = requestId,
              volunteerId = volunteer.id,
              userId = user.id,
              userName = user.fullName,
              rating = ratingComment.first,
              comment = ratingComment.second,
              issues = emptyList(),
              createdAtEpochSeconds = now - daySeconds * (index + 2)
            )
            volunteerReviewsById[review.id] = review
            accountDatabase.upsertVolunteerReview(review.toPersistence())
          }
        }
      }
    }
  }

  private fun seedVolunteerAccount(): AccountRecord? {
    return accountsById["vol-seed-1"]
  }

  private fun resolveAnalyticsVolunteerId(userId: String): String {
    return userId
  }
}
