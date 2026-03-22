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

sealed class AuthOperationResult {
  data class Success(val session: AuthSession) : AuthOperationResult()
  data class Error(val message: String) : AuthOperationResult()
}
