package com.athar.accessibilitymapping.data

data class UserRegistrationPayload(
  val fullName: String,
  val email: String,
  val phone: String,
  val location: String,
  val password: String,
  val disabilityType: String,
  val emergencyContactName: String,
  val emergencyContactPhone: String
)

data class VolunteerRegistrationPayload(
  val fullName: String,
  val email: String,
  val phone: String,
  val location: String,
  val password: String,
  val idNumber: String,
  val dateOfBirth: String,
  val motivation: String,
  val languages: Set<String>,
  val availability: Set<String>,
  val idDocumentUri: String? = null
)

data class AuthSession(
  val userId: String,
  val role: UserRole,
  val fullName: String,
  val email: String,
  val phone: String = "",
  val disabilityType: String?,
  val volunteerLive: Boolean = false
)

data class EmailVerificationChallenge(
  val challengeId: String,
  val email: String,
  val role: UserRole,
  val expiresAtEpochSeconds: Long,
  val resendAvailableAtEpochSeconds: Long,
  val codeLength: Int = 6,
  val message: String = "We sent a verification code to your email."
)

sealed class AuthOperationResult {
  data class Success(val session: AuthSession) : AuthOperationResult()
  data class Error(val message: String) : AuthOperationResult()
}

sealed class RegistrationOperationResult {
  data class Authenticated(val session: AuthSession) : RegistrationOperationResult()
  data class VerificationRequired(val challenge: EmailVerificationChallenge) : RegistrationOperationResult()
  data class Error(val message: String) : RegistrationOperationResult()
}
