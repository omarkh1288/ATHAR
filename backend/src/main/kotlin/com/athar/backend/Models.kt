package com.athar.backend

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable

@Serializable
enum class UserRole {
  User,
  Volunteer
}

@Serializable
data class ApiError(
  val message: String
)

@Serializable
data class ContributionStatsDto(
  val ratingsSubmitted: Int,
  val reportsSubmitted: Int,
  val helpfulVotes: Int
)

@Serializable
data class NotificationSettingsDto(
  val pushEnabled: Boolean = true,
  val emailEnabled: Boolean = true,
  val smsEnabled: Boolean = false,
  val volunteerRequests: Boolean = true,
  val volunteerAccepted: Boolean = true,
  val locationUpdates: Boolean = true,
  val newRatings: Boolean = true,
  val communityUpdates: Boolean = false,
  val marketingEmails: Boolean = false,
  val soundEnabled: Boolean = true,
  val vibrationEnabled: Boolean = true
)

@Serializable
data class PrivacySettingsDto(
  val locationSharing: Boolean = true,
  val profileVisibility: Boolean = true,
  val showRatings: Boolean = true,
  val activityStatus: Boolean = true,
  val twoFactorAuth: Boolean = false
)

@Serializable
data class AuthUserDto(
  val id: String,
  val role: UserRole,
  val fullName: String,
  val email: String,
  val phone: String,
  val location: String,
  val disabilityType: String? = null,
  val memberSince: String,
  val volunteerLive: Boolean = false,
  val roleVerifiedAt: String? = null,
  val contributionStats: ContributionStatsDto,
  val notificationSettings: NotificationSettingsDto,
  val privacySettings: PrivacySettingsDto
)

@Serializable
data class TokenPairDto(
  val accessToken: String,
  val refreshToken: String,
  val expiresAtEpochSeconds: Long
)

@Serializable
data class AuthResponseDto(
  val user: AuthUserDto,
  val tokens: TokenPairDto
)

@Serializable
data class RegisterUserRequest(
  val fullName: String,
  val email: String,
  val phone: String,
  val location: String,
  val password: String,
  val disabilityType: String,
  val emergencyContactName: String,
  val emergencyContactPhone: String
)

@Serializable
data class RegisterVolunteerRequest(
  val fullName: String,
  val email: String,
  val phone: String,
  val location: String,
  val password: String,
  val idNumber: String,
  val dateOfBirth: String,
  val motivation: String,
  val languages: List<String>,
  val availability: List<String>,
  val idDocumentFileName: String? = null,
  val idDocumentContentType: String? = null,
  val idDocumentSizeBytes: Long? = null,
  val idDocumentBytes: ByteArray? = null
)

@Serializable
data class LoginRequest(
  val email: String,
  val password: String,
  val deviceName: String? = null
)

@Serializable
data class RefreshTokenRequest(
  val refreshToken: String
)

@Serializable
data class LogoutRequest(
  val refreshToken: String? = null
)

@Serializable
data class UpdateProfileRequest(
  val fullName: String? = null,
  val phone: String? = null,
  val location: String? = null,
  val disabilityType: String? = null
)

@Serializable
data class ChangePasswordRequest(
  val currentPassword: String,
  val newPassword: String
)

@Serializable
data class SessionDto(
  val id: String,
  val deviceName: String,
  val createdAtEpochSeconds: Long,
  val lastSeenAtEpochSeconds: Long,
  val isCurrent: Boolean
)

@Serializable
data class UpdateNotificationSettingsRequest(
  val pushEnabled: Boolean? = null,
  val emailEnabled: Boolean? = null,
  val smsEnabled: Boolean? = null,
  val volunteerRequests: Boolean? = null,
  val volunteerAccepted: Boolean? = null,
  val locationUpdates: Boolean? = null,
  val newRatings: Boolean? = null,
  val communityUpdates: Boolean? = null,
  val marketingEmails: Boolean? = null,
  val soundEnabled: Boolean? = null,
  val vibrationEnabled: Boolean? = null
)

@Serializable
data class UpdatePrivacySettingsRequest(
  val locationSharing: Boolean? = null,
  val profileVisibility: Boolean? = null,
  val showRatings: Boolean? = null,
  val activityStatus: Boolean? = null,
  val twoFactorAuth: Boolean? = null
)

@Serializable
data class ToggleVolunteerLiveRequest(
  val isLive: Boolean
)

@Serializable
data class VolunteerRequestDto(
  val id: String,
  val userId: String,
  val userName: String,
  val userType: String,
  val location: String,
  val requestTime: String,
  val status: String,
  val volunteerName: String? = null,
  val description: String,
  val hours: Int = 1,
  val price_per_hour: Int = 50
)

@Serializable
data class AssistanceRequestDto(
  val id: String,
  val userName: String,
  val userType: String,
  val location: String,
  val destination: String,
  val distance: String,
  val urgency: String,
  val helpType: String,
  val requestTime: String,
  val status: String,
  val hours: Int = 1,
  val price_per_hour: Int = 50
)

@Serializable
data class MyRequestsResponse(
  val role: UserRole,
  val userRequests: List<VolunteerRequestDto> = emptyList(),
  val volunteerRequests: List<AssistanceRequestDto> = emptyList()
)

@Serializable
data class VolunteerCountsDto(
  val incoming: Int = 0,
  val active: Int = 0,
  val history: Int = 0
)

@Serializable
data class IncomingAlertDto(
  val count: Int = 0,
  val message: String = ""
)

@Serializable
data class VolunteerImpactDto(
  val totalAssists: Int = 0,
  val avgRating: Float = 0f,
  val thisWeek: Int = 0
)

@Serializable
data class VolunteerIncomingResponseDto(
  val counts: VolunteerCountsDto = VolunteerCountsDto(),
  val incoming_alert: IncomingAlertDto = IncomingAlertDto(),
  val requests: List<AssistanceRequestDto> = emptyList()
)

@Serializable
data class VolunteerActiveResponseDto(
  val counts: VolunteerCountsDto = VolunteerCountsDto(),
  val status_banner: String = "Assistance in Progress",
  val requests: List<AssistanceRequestDto> = emptyList()
)

@Serializable
data class VolunteerHistoryResponseDto(
  val counts: VolunteerCountsDto = VolunteerCountsDto(),
  val impact: VolunteerImpactDto = VolunteerImpactDto(),
  val requests: List<AssistanceRequestDto> = emptyList()
)

@Serializable
data class VolunteerImpactResponseDto(
  val counts: VolunteerCountsDto = VolunteerCountsDto(),
  val impact: VolunteerImpactDto = VolunteerImpactDto()
)

@Serializable
data class VolunteerAnalyticsMonthlyEarningDto(
  val month: String,
  val gross: Double,
  val net: Double,
  val fee: Double
)

@Serializable
data class VolunteerAnalyticsWeeklyActivityDto(
  val day: String,
  val completed: Int
)

@Serializable
data class VolunteerAnalyticsRequestTypeShareDto(
  val name: String,
  val value: Int
)

@Serializable
data class VolunteerAnalyticsPaymentRecordDto(
  val id: String,
  val date: String,
  val user: String,
  val hours: Int,
  val gross: Double,
  val net: Double,
  val status: String
)

@Serializable
data class VolunteerAnalyticsWithdrawalRecordDto(
  val id: String,
  val date: String,
  val amount: Double,
  val method: String,
  val status: String
)

@Serializable
data class VolunteerAnalyticsEarningsResponseDto(
  val available_balance: Double = 0.0,
  val pending_balance: Double = 0.0,
  val total_gross: Double = 0.0,
  val total_fees: Double = 0.0,
  val total_net: Double = 0.0,
  val this_week_net: Double = 0.0,
  val current_month_label: String = "",
  val current_month_net: Double = 0.0,
  val last_month_net: Double = 0.0,
  val monthly_change_percent: Double = 0.0,
  val monthly_earnings: List<VolunteerAnalyticsMonthlyEarningDto> = emptyList(),
  val withdrawal_history: List<VolunteerAnalyticsWithdrawalRecordDto> = emptyList(),
  val payment_history: List<VolunteerAnalyticsPaymentRecordDto> = emptyList()
)

@Serializable
data class VolunteerAnalyticsPerformanceResponseDto(
  val grade: String = "C",
  val headline: String = "",
  val percentile: Int = 50,
  val response_rate: Float = 0f,
  val completion_rate: Float = 0f,
  val average_rating: Float = 0f,
  val on_time_rate: Float = 0f,
  val completed: Int = 0,
  val pending: Int = 0,
  val users_helped: Int = 0,
  val positive_reviews: Int = 0,
  val five_star_ratings: Int = 0,
  val total_reviews: Int = 0,
  val badges: List<String> = emptyList(),
  val weekly_activity: List<VolunteerAnalyticsWeeklyActivityDto> = emptyList(),
  val request_types: List<VolunteerAnalyticsRequestTypeShareDto> = emptyList()
)

@Serializable
data class VolunteerAnalyticsReviewDto(
  val id: String,
  val user_name: String,
  val rating: Int,
  val comment: String,
  val date: String,
  val issues: List<String> = emptyList()
)

@Serializable
data class VolunteerAnalyticsReviewsResponseDto(
  val reviews: List<VolunteerAnalyticsReviewDto> = emptyList(),
  val page: Int = 1,
  val per_page: Int = 10,
  val total: Int = 0,
  val average_rating: Double = 0.0
)

@Serializable
enum class PaymentMethod {
  CARD,
  CASH,
  WALLET
}

@Serializable
data class CreateAssistanceRequest(
  val userType: String,
  val location: String,
  val destination: String,
  val distance: String,
  val urgency: String,
  val helpType: String,
  val description: String,
  val payment_method: PaymentMethod,
  val service_fee: Double,
  val hours: Int = 1,
  val price_per_hour: Int = 50
)

@Serializable
data class HelpRequestMessageDto(
  val id: String,
  val senderId: String,
  val senderName: String,
  val message: String,
  val createdAtEpochSeconds: Long
)

@Serializable
data class CreateMessageRequest(
  val message: String
)

@Serializable
data class CheckoutResponseDto(
  val checkout_url: String,
  val payment_id: String
)

@Serializable
data class PayRequestResponseDto(
  val payment_method: String,
  val status: String,
  val message: String,
  val checkout_url: String? = null,
  val payment_id: String? = null
)

@Serializable
data class PaymentStatusDto(
  val id: String,
  val status: String,
  val amount: Double,
  val currency: String,
  val success: Boolean
)

@Serializable
data class ActionResultDto(
  val success: Boolean,
  val message: String
)

@Serializable
data class LocationFeaturesDto(
  val ramp: Boolean,
  val elevator: Boolean,
  val accessibleToilet: Boolean,
  val accessibleParking: Boolean,
  val wideEntrance: Boolean,
  val brailleSignage: Boolean
)

@Serializable
data class LocationDto(
  val id: String,
  val name: String,
  val category: String,
  val lat: Double,
  val lng: Double,
  val rating: Double,
  val totalRatings: Int,
  val features: LocationFeaturesDto,
  val recentReports: List<String>,
  val distance: String
)

@Serializable
data class LocationSearchResponse(
  val query: String,
  val results: List<LocationDto>
)

@Serializable
data class SubmitRatingRequest(
  val score: Int,
  val comment: String? = null
)

@Serializable
data class SubmitVolunteerRatingRequest(
  val rating: Int,
  val comment: String? = null,
  val issues: List<String> = emptyList()
)

@Serializable
data class SupportMessageRequest(
  val subject: String,
  val message: String
)

@Serializable
data class SignLandmarkDto(
  val index: Int,
  val x: Float,
  val y: Float,
  val z: Float
)

@Serializable
data class SignObservationDto(
  val timestampMs: Long,
  val gestureLabel: String? = null,
  val localEnglish: String? = null,
  val localArabic: String? = null,
  val confidencePercent: Int,
  val landmarks: List<SignLandmarkDto> = emptyList()
)

@Serializable
data class InterpretEgyptianSignRequest(
  val sessionId: String,
  val locale: String = "ar-EG",
  val observations: List<SignObservationDto>
)

@Serializable
data class InterpretEgyptianSignResponse(
  val sessionId: String,
  val mode: String,
  val arabicSentence: String,
  val englishSentence: String,
  val confidencePercent: Int,
  val dominantGestureLabel: String? = null,
  val notes: List<String> = emptyList()
)

sealed class ServiceResult<out T> {
  data class Success<T>(val value: T) : ServiceResult<T>()
  data class Failure(
    val status: HttpStatusCode,
    val message: String
  ) : ServiceResult<Nothing>()
}
