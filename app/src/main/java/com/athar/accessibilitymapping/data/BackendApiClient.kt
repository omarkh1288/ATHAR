package com.athar.accessibilitymapping.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import com.athar.accessibilitymapping.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import java.io.File
import java.net.URLEncoder
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

sealed class ApiCallResult<out T> {
  data class Success<T>(val data: T) : ApiCallResult<T>()
  data class Failure(
    val message: String,
    val statusCode: Int? = null,
    val validationField: String? = null,
    val validationErrors: Map<String, String> = emptyMap()
  ) : ApiCallResult<Nothing>()
}

@Serializable
enum class ApiUserRole {
  User,
  Volunteer
}

@Serializable
data class ApiContributionStats(
  val ratingsSubmitted: Int,
  val reportsSubmitted: Int,
  val helpfulVotes: Int
)

@Serializable
data class ApiNotificationSettings(
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
data class ApiPrivacySettings(
  val locationSharing: Boolean = true,
  val profileVisibility: Boolean = true,
  val showRatings: Boolean = true,
  val activityStatus: Boolean = true,
  val twoFactorAuth: Boolean = false
)

@Serializable
data class ApiAuthUser(
  val id: String,
  val role: ApiUserRole,
  val fullName: String,
  val email: String,
  val phone: String,
  val location: String,
  val profilePhotoPath: String? = null,
  val disabilityType: String? = null,
  val memberSince: String,
  val passwordChangedAt: String? = null,
  val volunteerLive: Boolean = false,
  val roleVerifiedAt: String? = null,
  val volunteerStatus: String? = null,
  val isActive: Boolean = true,
  val contributionStats: ApiContributionStats = ApiContributionStats(0, 0, 0),
  val notificationSettings: ApiNotificationSettings = ApiNotificationSettings(),
  val privacySettings: ApiPrivacySettings = ApiPrivacySettings()
)

@Serializable
data class ApiActionResult(
  val success: Boolean,
  val message: String
)

@Serializable
data class ApiAccessibilityContribution(
  val id: Int? = null,
  val locationId: String? = null,
  val wheelchairAccessible: Boolean = false,
  val rampAvailable: Boolean = false,
  val elevatorAvailable: Boolean = false,
  val parking: Boolean = false,
  val accessibleToilet: Boolean = false,
  val wideEntrance: Boolean = false,
  val status: String? = null,
  val pendingVerification: Boolean = true
)

data class ApiLocationReportResult(
  val action: ApiActionResult,
  val contribution: ApiAccessibilityContribution? = null,
  val locationId: String? = null
)

@Serializable
data class ApiTokenPair(
  val accessToken: String,
  val refreshToken: String,
  val expiresAtEpochSeconds: Long
)

@Serializable
data class ApiAuthResponse(
  val user: ApiAuthUser,
  val tokens: ApiTokenPair
)

data class ApiEmailVerificationChallenge(
  val challengeId: String,
  val email: String,
  val role: ApiUserRole,
  val expiresAtEpochSeconds: Long,
  val resendAvailableAtEpochSeconds: Long,
  val codeLength: Int = 6,
  val message: String = "We sent a verification code to your email."
)

sealed class ApiRegistrationStartResponse {
  data class Authenticated(val auth: ApiAuthResponse) : ApiRegistrationStartResponse()
  data class VerificationRequired(val challenge: ApiEmailVerificationChallenge) : ApiRegistrationStartResponse()
}

@Serializable
data class ApiUpdateProfileRequest(
  val fullName: String? = null,
  val phone: String? = null,
  val location: String? = null,
  val disabilityType: String? = null
)

@Serializable
data class ApiChangePasswordRequest(
  val currentPassword: String,
  val newPassword: String
)

@Serializable
data class ApiSessionDto(
  val id: String,
  val deviceName: String,
  val createdAtEpochSeconds: Long,
  val lastSeenAtEpochSeconds: Long,
  val isCurrent: Boolean
)

@Serializable
data class ApiUpdateNotificationSettingsRequest(
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
data class ApiUpdatePrivacySettingsRequest(
  val locationSharing: Boolean? = null,
  val profileVisibility: Boolean? = null,
  val showRatings: Boolean? = null,
  val activityStatus: Boolean? = null,
  val twoFactorAuth: Boolean? = null
)

@Serializable
data class ApiHelpRequestMessage(
  val id: String,
  val senderId: String,
  val senderName: String,
  val message: String,
  val createdAtEpochSeconds: Long
)

@Serializable
data class ApiCheckoutResponse(
  val checkout_url: String,
  val payment_id: String
)

@Serializable
data class ApiPayRequestResponse(
  @SerialName("payment_method") val paymentMethod: String,
  val status: String,
  val message: String,
  @SerialName("checkout_url") val checkoutUrl: String? = null,
  @SerialName("payment_id") val paymentId: String? = null
)

data class ApiPaymentCustomerDetails(
  val firstName: String,
  val lastName: String,
  val email: String,
  val phoneNumber: String
)

@Serializable
data class ApiPaymentStatus(
  val id: String,
  val status: String,
  val amount: Double,
  val currency: String,
  val success: Boolean
)

@Serializable
data class ApiVolunteerRequest(
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
  val pricePerHour: Int = 50,
  val totalAmountEgp: Int? = null,
  val paymentMethod: String = "cash",
  val paymentStatus: String? = null,
  val isPaid: Boolean = false
)

@Serializable
data class ApiAssistanceRequest(
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
  val pricePerHour: Int = 50,
  val totalAmountEgp: Int? = null,
  val paymentMethod: String = "cash",
  val paymentStatus: String? = null,
  val isPaid: Boolean = false
)

@Serializable
data class ApiMyRequestsResponse(
  val role: ApiUserRole,
  val userRequests: List<ApiVolunteerRequest> = emptyList(),
  val volunteerRequests: List<ApiAssistanceRequest> = emptyList()
)

@Serializable
data class ApiVolunteerCounts(
  val incoming: Int = 0,
  val active: Int = 0,
  val history: Int = 0
)

@Serializable
data class ApiIncomingAlert(
  val count: Int = 0,
  val message: String = ""
)

@Serializable
data class ApiVolunteerImpact(
  val totalAssists: Int = 0,
  val avgRating: Float = 0f,
  val thisWeek: Int = 0
)

@Serializable
data class ApiVolunteerIncomingResponse(
  val counts: ApiVolunteerCounts = ApiVolunteerCounts(),
  val incomingAlert: ApiIncomingAlert = ApiIncomingAlert(),
  val requests: List<ApiAssistanceRequest> = emptyList()
)

@Serializable
data class ApiVolunteerActiveResponse(
  val counts: ApiVolunteerCounts = ApiVolunteerCounts(),
  val statusBanner: String = "Assistance in Progress",
  val requests: List<ApiAssistanceRequest> = emptyList()
)

@Serializable
data class ApiVolunteerHistoryResponse(
  val counts: ApiVolunteerCounts = ApiVolunteerCounts(),
  val impact: ApiVolunteerImpact = ApiVolunteerImpact(),
  val requests: List<ApiAssistanceRequest> = emptyList()
)

@Serializable
data class ApiVolunteerImpactResponse(
  val counts: ApiVolunteerCounts = ApiVolunteerCounts(),
  val impact: ApiVolunteerImpact = ApiVolunteerImpact()
)

@Serializable
data class ApiVolunteerAnalyticsMonthlyEarning(
  val month: String = "",
  val gross: Double = 0.0,
  val net: Double = 0.0,
  val fee: Double = 0.0
)

@Serializable
data class ApiVolunteerAnalyticsWeeklyActivity(
  val day: String = "",
  val label: String = "",
  val date: String = "",
  val completed: Int = 0,
  @SerialName("completed_requests") val completedRequests: Int = 0,
  val count: Int = 0,
  val requests: Int = 0,
  val value: Int = 0
) {
  /** Resolved count using fallback order: completed > completed_requests > count > requests > value */
  val effectiveCompleted: Int
    get() = when {
      completed != 0 -> completed
      completedRequests != 0 -> completedRequests
      count != 0 -> count
      requests != 0 -> requests
      value != 0 -> value
      else -> 0
    }

  /** Resolved day label using fallback order: day > label > date */
  val effectiveDay: String
    get() = day.ifBlank { label.ifBlank { date } }
}

@Serializable
data class ApiVolunteerAnalyticsRequestTypeShare(
  val name: String = "",
  val value: Int = 0
)

@Serializable
data class ApiVolunteerAnalyticsPaymentRecord(
  val id: String = "",
  val date: String = "",
  val user: String = "",
  val hours: Int = 0,
  val gross: Double = 0.0,
  val net: Double = 0.0,
  val status: String = "pending"
)

@Serializable
data class ApiVolunteerAnalyticsWithdrawalRecord(
  val id: String = "",
  val date: String = "",
  val amount: Double = 0.0,
  val method: String = "",
  val status: String = "pending"
)

@Serializable
data class ApiVolunteerAnalyticsEarningsResponse(
  @SerialName("available_balance") val availableBalance: Double = 0.0,
  @SerialName("pending_balance") val pendingBalance: Double = 0.0,
  @SerialName("total_gross") val totalGross: Double = 0.0,
  @SerialName("total_fees") val totalFees: Double = 0.0,
  @SerialName("total_net") val totalNet: Double = 0.0,
  @SerialName("this_week_net") val thisWeekNet: Double = 0.0,
  @SerialName("current_month_label") val currentMonthLabel: String = "",
  @SerialName("current_month_net") val currentMonthNet: Double = 0.0,
  @SerialName("last_month_net") val lastMonthNet: Double = 0.0,
  @SerialName("monthly_change_percent") val monthlyChangePercent: Double = 0.0,
  @SerialName("monthly_earnings") val monthlyEarnings: List<ApiVolunteerAnalyticsMonthlyEarning> = emptyList(),
  @SerialName("withdrawal_history") val withdrawalHistory: List<ApiVolunteerAnalyticsWithdrawalRecord> = emptyList(),
  @SerialName("payment_history") val paymentHistory: List<ApiVolunteerAnalyticsPaymentRecord> = emptyList()
)

@Serializable
data class ApiVolunteerAnalyticsPerformanceResponse(
  val grade: String = "C",
  val headline: String = "",
  val percentile: Int = 50,
  @SerialName("response_rate") val responseRate: Float = 0f,
  @SerialName("completion_rate") val completionRate: Float = 0f,
  @SerialName("average_rating") val averageRating: Float = 0f,
  @SerialName("on_time_rate") val onTimeRate: Float = 0f,
  val completed: Int = 0,
  val pending: Int = 0,
  @SerialName("users_helped") val usersHelped: Int = 0,
  @SerialName("positive_reviews") val positiveReviews: Int = 0,
  @SerialName("five_star_ratings") val fiveStarRatings: Int = 0,
  @SerialName("total_reviews") val totalReviews: Int = 0,
  val badges: List<String> = emptyList(),
  @SerialName("weekly_activity") val weeklyActivity: List<ApiVolunteerAnalyticsWeeklyActivity> = emptyList(),
  @SerialName("request_types") val requestTypes: List<ApiVolunteerAnalyticsRequestTypeShare> = emptyList()
)

@Serializable
data class ApiVolunteerAnalyticsReview(
  val id: String = "",
  @SerialName("user_name") val userName: String = "",
  val rating: Int = 0,
  val comment: String = "",
  val date: String = "",
  val issues: List<String> = emptyList()
)

@Serializable
data class ApiVolunteerAnalyticsReviewsResponse(
  val reviews: List<ApiVolunteerAnalyticsReview> = emptyList(),
  val page: Int = 1,
  @SerialName("per_page") val perPage: Int = 10,
  val total: Int = 0,
  @SerialName("average_rating") val averageRating: Double = 0.0
)

@Serializable
data class ApiCreateRequest(
  val userType: String,
  val location: String,
  val destination: String,
  val distance: String,
  val urgency: String,
  val helpType: String,
  val description: String,
  val paymentMethod: String = "cash",
  val serviceFee: Double = 0.0,
  val hours: Int = 1,
  val pricePerHour: Int = 50,
  val fromLat: Double? = null,
  val fromLng: Double? = null,
  val toLat: Double? = null,
  val toLng: Double? = null
)

@Serializable
data class ApiLocationReportRequest(
  val name: String,
  val address: String,
  val governmentId: Int,
  val latitude: Double,
  val longitude: Double,
  val categoryId: Int? = null,
  val rating: Int,
  val comment: String? = null,
  val rampAvailable: Boolean = false,
  val elevatorAvailable: Boolean = false,
  val parking: Boolean = false,
  val wheelchairAccessible: Boolean = false,
  val wideEntrance: Boolean = false,
  val accessibleToilet: Boolean = false,
  val notes: String? = null
)

data class ApiGovernment(
  val id: Int,
  val name: String,
  val arabicName: String? = null
)

data class ApiCategory(
  val id: Int,
  val name: String,
  val icon: String? = null
)

@Serializable
data class ApiLocationFeatures(
  val ramp: Boolean,
  val elevator: Boolean,
  val accessibleToilet: Boolean,
  val accessibleParking: Boolean,
  val wideEntrance: Boolean,
  val brailleSignage: Boolean
)

@Serializable
data class ApiLocation(
  val id: String,
  val name: String,
  val category: String,
  val lat: Double,
  val lng: Double,
  val rating: Double,
  val totalRatings: Int,
  val features: ApiLocationFeatures,
  val recentReports: List<String>,
  val distance: String
)

@Serializable
data class ApiLocationSearchResponse(
  val query: String,
  val results: List<ApiLocation>
)

@Serializable
data class ApiSignLandmark(
  val index: Int,
  val x: Float,
  val y: Float,
  val z: Float
)

@Serializable
data class ApiSignObservation(
  val timestampMs: Long,
  val gestureLabel: String? = null,
  val localEnglish: String? = null,
  val localArabic: String? = null,
  val confidencePercent: Int,
  val landmarks: List<ApiSignLandmark> = emptyList()
)

@Serializable
data class ApiInterpretEgyptianSignRequest(
  val sessionId: String,
  val locale: String = "ar-EG",
  val observations: List<ApiSignObservation>
)

@Serializable
data class ApiInterpretEgyptianSignResponse(
  val sessionId: String,
  val mode: String,
  val arabicSentence: String,
  val englishSentence: String,
  val confidencePercent: Int,
  val dominantGestureLabel: String? = null,
  val notes: List<String> = emptyList()
)

class BackendApiClient(private val appContext: Context? = null) {
  companion object {
    const val DEFAULT_LAT = 30.0444
    const val DEFAULT_LNG = 31.2357
    // Max valid EGP total: 200 EGP/h * 8h = 1600. Anything above is likely piasters.
    private const val MAX_VALID_TOTAL_EGP = 1600.0
  }

  private data class UploadFilePart(
    val fieldName: String,
    val fileName: String,
    val contentType: String,
    val bytes: ByteArray
  )

  private var cachedRole: Pair<String, ApiUserRole>? = null

  private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
    explicitNulls = false
  }

  private val requestTimeoutMillis = 15_000L
  private val connectTimeoutMillis = 10_000L
  private val socketTimeoutMillis = 15_000L

  private val client = HttpClient(OkHttp) {
    install(ContentNegotiation) {
      json(json)
    }
    install(HttpTimeout) {
      requestTimeoutMillis = this@BackendApiClient.requestTimeoutMillis
      connectTimeoutMillis = this@BackendApiClient.connectTimeoutMillis
      socketTimeoutMillis = this@BackendApiClient.socketTimeoutMillis
    }
    defaultRequest {
      header(HttpHeaders.Accept, ContentType.Application.Json.toString())
    }
  }

  private val baseUrl = BuildConfig.BACKEND_BASE_URL.trimEnd('/')

  suspend fun isBackendReachable(): Boolean {
    return when (get("/health")) {
      is ApiCallResult.Success -> true
      is ApiCallResult.Failure -> false
    }
  }

  suspend fun login(email: String, password: String): ApiCallResult<ApiAuthResponse> {
    val fields = mapOf(
      "email" to email.trim(),
      "password" to password
    )
    return when (val response = postMultipart("/auth/login", fields, filePart = null)) {
      is ApiCallResult.Success -> parseAuthResponse(response.data)
      is ApiCallResult.Failure -> response
    }
  }

  suspend fun registerUser(request: UserRegistrationPayload): ApiCallResult<ApiRegistrationStartResponse> {
    val trimmedLocation = request.location.trim()
    val fields = linkedMapOf(
      "name" to request.fullName.trim(),
      "full_name" to request.fullName.trim(),
      "email" to request.email.trim(),
      "password" to request.password,
      "password_confirmation" to request.password,
      "role" to "user",
      "phone" to request.phone.trim(),
      "city" to trimmedLocation,
      "location" to trimmedLocation,
      "disability_type" to request.disabilityType.trim(),
      "assistance_needs" to request.disabilityType.trim(),
      "emergency_contact_name" to request.emergencyContactName.trim(),
      "emergency_contact_phone" to request.emergencyContactPhone.trim()
    )
    return when (val response = postMultipart("/auth/register-user", fields, filePart = null)) {
      is ApiCallResult.Success -> parseRegistrationStartResponse(response.data)
      is ApiCallResult.Failure -> response
    }
  }

  suspend fun registerVolunteer(request: VolunteerRegistrationPayload): ApiCallResult<ApiRegistrationStartResponse> {
    val trimmedLocation = request.location.trim()
    val languages = request.languages
      .map { it.trim() }
      .filter { it.isNotBlank() }
      .sorted()
    val availability = request.availability
      .map { it.trim() }
      .filter { it.isNotBlank() }
      .sorted()

    val multipartFields = linkedMapOf<String, String>(
      "name" to request.fullName.trim(),
      "full_name" to request.fullName.trim(),
      "email" to request.email.trim(),
      "password" to request.password,
      "password_confirmation" to request.password,
      "phone" to request.phone.trim(),
      "role" to "volunteer",
      "city" to trimmedLocation,
      "location" to trimmedLocation,
      "national_id" to request.idNumber.trim(),
      "date_of_birth" to request.dateOfBirth.trim(),
      "volunteer_motivation" to request.motivation.trim(),
      "motivation" to request.motivation.trim()
    )
    languages.forEachIndexed { index, value ->
      multipartFields["volunteer_languages[$index]"] = value
    }
    availability.forEachIndexed { index, value ->
      multipartFields["volunteer_availability[$index]"] = value
    }

    val idDocumentPart = readIdDocumentPart(request.idDocumentUri)
    if (idDocumentPart != null) {
      return when (
        val response = postMultipart(
          path = "/auth/register-volunteer",
          fields = multipartFields,
          filePart = idDocumentPart
        )
      ) {
        is ApiCallResult.Success -> parseRegistrationStartResponse(response.data)
        is ApiCallResult.Failure -> response
      }
    }

    return when (val response = postMultipart("/auth/register-volunteer", multipartFields, filePart = null)) {
      is ApiCallResult.Success -> parseRegistrationStartResponse(response.data)
      is ApiCallResult.Failure -> response
    }
  }

  suspend fun verifyEmailChallenge(challengeId: String, code: String): ApiCallResult<ApiAuthResponse> {
    val fields = linkedMapOf(
      "challenge_id" to challengeId.trim(),
      "code" to code.trim(),
      "otp" to code.trim(),
      "device_name" to "Athar Android"
    )
    return when (val response = postMultipart("/auth/verify-email", fields, filePart = null)) {
      is ApiCallResult.Success -> parseAuthResponse(response.data)
      is ApiCallResult.Failure -> response
    }
  }

  suspend fun resendEmailChallenge(challengeId: String): ApiCallResult<ApiEmailVerificationChallenge> {
    val fields = mapOf("challenge_id" to challengeId.trim())
    return when (val response = postMultipart("/auth/verify-email/resend", fields, filePart = null)) {
      is ApiCallResult.Success -> {
        val challenge = parseEmailVerificationChallenge(response.data)
        if (challenge != null) {
          ApiCallResult.Success(challenge)
        } else {
          ApiCallResult.Failure("Failed to parse verification challenge.")
        }
      }
      is ApiCallResult.Failure -> response
    }
  }

  suspend fun refresh(refreshToken: String): ApiCallResult<ApiAuthResponse> {
    return ApiCallResult.Failure(
      message = "Token refresh is not supported by this backend.",
      statusCode = 404
    )
  }

  suspend fun logout(accessToken: String, refreshToken: String?): ApiCallResult<ApiActionResult> {
    val fields = if (refreshToken.isNullOrBlank()) emptyMap() else mapOf("refresh_token" to refreshToken)
    return when (val response = postMultipart("/auth/logout", fields, filePart = null, token = accessToken)) {
      is ApiCallResult.Success -> ApiCallResult.Success(
        ApiActionResult(success = true, message = "Logged out.")
      )
      is ApiCallResult.Failure -> response
    }
  }

  suspend fun getMe(accessToken: String): ApiCallResult<ApiAuthUser> {
    return when (val response = get("/me", token = accessToken)) {
      is ApiCallResult.Success -> {
        val user = parseUser(response.data)
        if (user != null) {
          ApiCallResult.Success(user)
        } else {
          ApiCallResult.Failure("Failed to parse account details.")
        }
      }
      is ApiCallResult.Failure -> response
    }
  }

  suspend fun getProfileStats(accessToken: String): ApiCallResult<ApiContributionStats> {
    return when (val response = get("/profile/stats", token = accessToken)) {
      is ApiCallResult.Success -> {
        val payload = response.data?.asObjectOrNull()
        val stats = payload?.get("stats")?.asObjectOrNull()
          ?: payload?.get("data")?.asObjectOrNull()
          ?: payload
        if (stats == null) {
          ApiCallResult.Success(ApiContributionStats(0, 0, 0))
        } else {
          ApiCallResult.Success(
            ApiContributionStats(
              ratingsSubmitted = stats.readInt("ratings_count", "ratingsCount") ?: 0,
              reportsSubmitted = stats.readInt("reports_count", "reportsCount") ?: 0,
              helpfulVotes = stats.readInt("helpful_count", "helpfulCount") ?: 0
            )
          )
        }
      }
      is ApiCallResult.Failure -> response
    }
  }

  suspend fun patchProfile(accessToken: String, request: ApiUpdateProfileRequest): ApiCallResult<ApiAuthUser> {
    val content = linkedMapOf<String, JsonElement>()
    request.fullName?.takeIf { it.isNotBlank() }?.let {
      content["name"] = JsonPrimitive(it.trim())
      content["full_name"] = JsonPrimitive(it.trim())
    }
    request.phone?.let {
      content["phone"] = JsonPrimitive(it.trim())
    }
    request.disabilityType?.let {
      content["disability_type"] = JsonPrimitive(it.trim())
    }
    request.location?.takeIf { it.isNotBlank() }?.let {
      content["city"] = JsonPrimitive(it.trim())
      content["location"] = JsonPrimitive(it.trim())
    }
    val body = JsonObject(content)
    return when (val response = put("/profile", body = body, token = accessToken)) {
      is ApiCallResult.Success -> {
        val user = parseUser(response.data)?.copy(location = request.location.orEmpty())
        if (user != null) {
          ApiCallResult.Success(user)
        } else {
          ApiCallResult.Failure("Failed to parse updated profile.")
        }
      }
      is ApiCallResult.Failure -> response
    }
  }

  suspend fun uploadProfilePhoto(accessToken: String, photoUri: String): ApiCallResult<ApiAuthUser> {
    val photoPart = readProfilePhotoPart(photoUri)
      ?: return ApiCallResult.Failure("Upload failed, please try again")

    return when (val response = postMultipart("/profile/photo", emptyMap(), photoPart, token = accessToken)) {
      is ApiCallResult.Success -> {
        val user = parseUser(response.data)
        if (user != null) {
          ApiCallResult.Success(user)
        } else {
          ApiCallResult.Failure("Failed to parse updated profile photo.")
        }
      }
      is ApiCallResult.Failure -> response
    }
  }

  suspend fun changePassword(accessToken: String, request: ApiChangePasswordRequest): ApiCallResult<ApiActionResult> {
    val body = JsonObject(
      mapOf(
        "current_password" to JsonPrimitive(request.currentPassword),
        "new_password" to JsonPrimitive(request.newPassword),
        "new_password_confirmation" to JsonPrimitive(request.newPassword)
      )
    )
    return when (val response = put("/auth/password", body = body, token = accessToken)) {
      is ApiCallResult.Success -> {
        val message = response.data?.asObjectOrNull()?.readString("message")
          ?: "Password updated successfully."
        ApiCallResult.Success(ApiActionResult(success = true, message = message))
      }
      is ApiCallResult.Failure -> response
    }
  }

  suspend fun getSessions(accessToken: String): ApiCallResult<List<ApiSessionDto>> {
    val now = Instant.now().epochSecond
    return when (val response = get("/auth/sessions", token = accessToken)) {
      is ApiCallResult.Success -> ApiCallResult.Success(
        extractItems(response.data).mapNotNull { parseSession(it, now) }
      )
      is ApiCallResult.Failure -> response
    }
  }

  suspend fun deleteSession(accessToken: String, sessionId: String): ApiCallResult<ApiActionResult> {
    val path = if (sessionId == "others") "/auth/sessions/others" else "/auth/sessions/$sessionId"
    return when (val response = delete(path = path, token = accessToken)) {
      is ApiCallResult.Success -> {
        val message = response.data?.asObjectOrNull()?.readString("message")
          ?: "Session revoked."
        ApiCallResult.Success(ApiActionResult(success = true, message = message))
      }
      is ApiCallResult.Failure -> response
    }
  }

  suspend fun getNotificationSettings(accessToken: String): ApiCallResult<ApiNotificationSettings> {
    return when (val response = get("/notification-preferences", token = accessToken)) {
      is ApiCallResult.Success -> {
        val settings = parseNotificationSettings(response.data)
          ?: return ApiCallResult.Failure("Failed to parse notification settings.")
        ApiCallResult.Success(settings)
      }
      is ApiCallResult.Failure -> response
    }
  }

  suspend fun patchNotificationSettings(
    accessToken: String,
    request: ApiUpdateNotificationSettingsRequest
  ): ApiCallResult<ApiNotificationSettings> {
    val content = linkedMapOf<String, JsonElement>()
    request.pushEnabled?.let { content["push_enabled"] = JsonPrimitive(it) }
    request.emailEnabled?.let { content["email_enabled"] = JsonPrimitive(it) }
    request.smsEnabled?.let { content["sms_enabled"] = JsonPrimitive(it) }
    request.volunteerRequests?.let { content["volunteer_requests"] = JsonPrimitive(it) }
    request.volunteerAccepted?.let { content["volunteer_accepted"] = JsonPrimitive(it) }
    request.locationUpdates?.let { content["location_updates"] = JsonPrimitive(it) }
    request.newRatings?.let { content["new_ratings"] = JsonPrimitive(it) }
    request.communityUpdates?.let { content["community_updates"] = JsonPrimitive(it) }
    request.marketingEmails?.let { content["marketing_emails"] = JsonPrimitive(it) }
    request.soundEnabled?.let { content["sound_enabled"] = JsonPrimitive(it) }
    request.vibrationEnabled?.let { content["vibration_enabled"] = JsonPrimitive(it) }
    if (content.isEmpty()) {
      return getNotificationSettings(accessToken)
    }

    return when (val response = put("/notification-preferences", JsonObject(content), accessToken)) {
      is ApiCallResult.Success -> {
        val settings = parseNotificationSettings(response.data)
        if (settings != null) {
          ApiCallResult.Success(settings)
        } else {
          getNotificationSettings(accessToken)
        }
      }
      is ApiCallResult.Failure -> response
    }
  }

  suspend fun getPrivacySettings(accessToken: String): ApiCallResult<ApiPrivacySettings> {
    return when (val response = get("/privacy/settings", token = accessToken)) {
      is ApiCallResult.Success -> {
        val settings = parsePrivacySettings(response.data)
          ?: return ApiCallResult.Failure("Failed to parse privacy settings.")
        ApiCallResult.Success(settings)
      }
      is ApiCallResult.Failure -> response
    }
  }

  suspend fun patchPrivacySettings(
    accessToken: String,
    request: ApiUpdatePrivacySettingsRequest
  ): ApiCallResult<ApiPrivacySettings> {
    val content = linkedMapOf<String, JsonElement>()
    request.locationSharing?.let { content["location_sharing"] = JsonPrimitive(it) }
    request.profileVisibility?.let { content["profile_visibility"] = JsonPrimitive(it) }
    request.showRatings?.let { content["show_ratings"] = JsonPrimitive(it) }
    request.activityStatus?.let { content["activity_status"] = JsonPrimitive(it) }
    request.twoFactorAuth?.let { content["two_factor_auth"] = JsonPrimitive(it) }
    if (content.isEmpty()) {
      return getPrivacySettings(accessToken)
    }

    return when (val response = put("/privacy/settings", JsonObject(content), accessToken)) {
      is ApiCallResult.Success -> {
        val settings = parsePrivacySettings(response.data)
        if (settings != null) {
          ApiCallResult.Success(settings)
        } else {
          getPrivacySettings(accessToken)
        }
      }
      is ApiCallResult.Failure -> response
    }
  }

  suspend fun setVolunteerLive(accessToken: String, isLive: Boolean): ApiCallResult<ApiAuthUser> {
    val body = JsonObject(
      mapOf(
        "is_live" to JsonPrimitive(isLive),
        "lat" to JsonPrimitive(DEFAULT_LAT),
        "lng" to JsonPrimitive(DEFAULT_LNG)
      )
    )
    return when (val response = post("/volunteer/status", body = body, token = accessToken)) {
      is ApiCallResult.Success -> {
        when (val refreshed = getMe(accessToken)) {
          is ApiCallResult.Success -> ApiCallResult.Success(refreshed.data.copy(volunteerLive = isLive))
          is ApiCallResult.Failure -> {
            val fallback = parseUser(response.data)?.copy(volunteerLive = isLive)
            if (fallback != null) {
              ApiCallResult.Success(fallback)
            } else {
              ApiCallResult.Failure(refreshed.message, refreshed.statusCode)
            }
          }
        }
      }
      is ApiCallResult.Failure -> response
    }
  }

  suspend fun getLocations(
    lat: Double = DEFAULT_LAT,
    lng: Double = DEFAULT_LNG,
    radiusKm: Int = 20
  ): ApiCallResult<List<ApiLocation>> {
    return getNearbyLocations(
      search = null,
      lat = lat,
      lng = lng,
      radiusKm = radiusKm
    )
  }

  suspend fun getAllLocations(): ApiCallResult<List<ApiLocation>> {
    return when (val response = get("/locations")) {
      is ApiCallResult.Success -> ApiCallResult.Success(
        extractItems(response.data).mapNotNull { parseLocation(it) }
      )
      is ApiCallResult.Failure -> {
        when (val fallback = get("/locations?page=1&per_page=500")) {
          is ApiCallResult.Success -> {
            val items = extractItems(fallback.data)
            ApiCallResult.Success(items.mapNotNull { parseLocation(it) })
          }
          is ApiCallResult.Failure -> response
        }
      }
    }
  }

  suspend fun getGovernments(token: String? = null): ApiCallResult<List<ApiGovernment>> {
    return when (val response = get("/governments", token = token)) {
      is ApiCallResult.Success -> {
        val governments = extractObjectListFromElement(response.data)
          .mapNotNull { parseGovernment(it) }
        if (governments.isEmpty()) {
          ApiCallResult.Failure("Failed to parse governments list.")
        } else {
          ApiCallResult.Success(governments)
        }
      }
      is ApiCallResult.Failure -> response
    }
  }

  suspend fun getCategories(token: String? = null): ApiCallResult<List<ApiCategory>> {
    return when (val response = get("/categories", token = token)) {
      is ApiCallResult.Success -> {
        val categories = extractObjectListFromElement(response.data)
          .mapNotNull { parseCategory(it) }
        if (categories.isEmpty()) {
          ApiCallResult.Failure("Failed to parse categories list.")
        } else {
          ApiCallResult.Success(categories)
        }
      }
      is ApiCallResult.Failure -> response
    }
  }

  suspend fun searchLocations(
    query: String
  ): ApiCallResult<ApiLocationSearchResponse> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isBlank()) {
      return when (val allLocations = getAllLocations()) {
        is ApiCallResult.Success -> ApiCallResult.Success(
          ApiLocationSearchResponse(query = normalizedQuery, results = allLocations.data)
        )
        is ApiCallResult.Failure -> allLocations
      }
    }

    return when (val response = get("/locations/search?q=${encodeQuery(normalizedQuery)}")) {
      is ApiCallResult.Success -> {
        val items = extractItems(response.data)
        ApiCallResult.Success(
          ApiLocationSearchResponse(
            query = normalizedQuery,
            results = items.mapNotNull { parseLocation(it) }
          )
        )
      }
      is ApiCallResult.Failure -> {
        when (val fallback = getAllLocations()) {
          is ApiCallResult.Success -> ApiCallResult.Success(
            ApiLocationSearchResponse(query = normalizedQuery, results = fallback.data)
          )
          is ApiCallResult.Failure -> response
        }
      }
    }
  }

  suspend fun interpretEgyptianSign(
    request: ApiInterpretEgyptianSignRequest
  ): ApiCallResult<ApiInterpretEgyptianSignResponse> {
    return when (val response = post("/sign-language/interpret", json.encodeToJsonElement(request))) {
      is ApiCallResult.Success -> {
        val model = decodeApiModel<ApiInterpretEgyptianSignResponse>(response.data)
          ?: return ApiCallResult.Failure("Failed to parse sign interpretation response.")
        ApiCallResult.Success(model)
      }
      is ApiCallResult.Failure -> response
    }
  }

  suspend fun getMyRequests(accessToken: String): ApiCallResult<ApiMyRequestsResponse> {
    val cached = cachedRole
    val role = if (cached != null && cached.first == accessToken) {
      cached.second
    } else {
      when (val me = getMe(accessToken)) {
        is ApiCallResult.Success -> {
          cachedRole = accessToken to me.data.role
          me.data.role
        }
        is ApiCallResult.Failure -> return me
      }
    }
    return when (
      val response = get(
        "/help-requests/mine?status=all&page=1&per_page=15",
        token = accessToken
      )
    ) {
      is ApiCallResult.Success -> {
        logApiPayload("GET /help-requests/mine", response.data)
        val root = response.data?.asObjectOrNull()
        val payload = root?.get("data")?.asObjectOrNull() ?: root
        val genericItems = extractVolunteerEndpointItems(response.data)

        val userRequestItems = payload.readObjectList(
          "user_requests",
          "userRequests",
          "requests",
          "help_requests",
          "helpRequests",
          "items",
          "results"
        ).ifEmpty {
          root.readObjectList(
            "user_requests",
            "userRequests",
            "requests",
            "help_requests",
            "helpRequests"
          )
        }.ifEmpty { genericItems }
        val volunteerRequestItems = payload.readObjectList(
          "volunteer_requests",
          "volunteerRequests",
          "requests",
          "help_requests",
          "helpRequests",
          "items",
          "results"
        ).ifEmpty {
          root.readObjectList(
            "volunteer_requests",
            "volunteerRequests",
            "requests",
            "help_requests",
            "helpRequests"
          )
        }.ifEmpty { genericItems }

        ApiCallResult.Success(
          ApiMyRequestsResponse(
            role = role,
            userRequests = userRequestItems.mapNotNull { parseVolunteerRequest(it) },
            volunteerRequests = if (role == ApiUserRole.Volunteer) {
              volunteerRequestItems.mapNotNull { parseAssistanceRequest(it) }
            } else {
              emptyList()
            }
          )
        )
      }
      is ApiCallResult.Failure -> response
    }
  }

  suspend fun getIncomingVolunteerRequests(accessToken: String): ApiCallResult<List<ApiAssistanceRequest>> {
    val path = "/volunteer/incoming?lat=$DEFAULT_LAT&lng=$DEFAULT_LNG&page=1&per_page=50"
    return when (val response = get(path, token = accessToken)) {
      is ApiCallResult.Success -> ApiCallResult.Success(
        extractItems(response.data).mapNotNull { parseAssistanceRequest(it) }
      )
      is ApiCallResult.Failure -> response
    }
  }

  suspend fun getVolunteerIncoming(
    accessToken: String,
    lat: Double? = DEFAULT_LAT,
    lng: Double? = DEFAULT_LNG,
    perPage: Int = 50
  ): ApiCallResult<ApiVolunteerIncomingResponse> {
    val queryParts = mutableListOf(
      "page=1",
      "per_page=${perPage.coerceIn(1, 100)}"
    )
    if (lat != null && lng != null) {
      queryParts += "lat=$lat"
      queryParts += "lng=$lng"
    }
    val path = "/volunteer/incoming?${queryParts.joinToString("&")}"
    return when (val response = get(path, token = accessToken)) {
      is ApiCallResult.Success -> {
        logApiPayload("GET $path", response.data)
        val parsed = parseVolunteerIncomingResponse(response.data)
        ApiCallResult.Success(parsed)
      }
      is ApiCallResult.Failure -> response
    }
  }

  suspend fun getVolunteerActive(
    accessToken: String,
    perPage: Int = 50
  ): ApiCallResult<ApiVolunteerActiveResponse> {
    val path = "/volunteer/active?page=1&per_page=${perPage.coerceIn(1, 100)}"
    return when (val response = get(path, token = accessToken)) {
      is ApiCallResult.Success -> {
        val parsed = parseVolunteerActiveResponse(response.data)
        ApiCallResult.Success(parsed)
      }
      is ApiCallResult.Failure -> response
    }
  }

  suspend fun getVolunteerHistory(
    accessToken: String,
    perPage: Int = 50
  ): ApiCallResult<ApiVolunteerHistoryResponse> {
    val path = "/volunteer/history?page=1&per_page=${perPage.coerceIn(1, 100)}"
    return when (val response = get(path, token = accessToken)) {
      is ApiCallResult.Success -> {
        val parsed = parseVolunteerHistoryResponse(response.data)
        ApiCallResult.Success(parsed)
      }
      is ApiCallResult.Failure -> response
    }
  }

  suspend fun getVolunteerImpact(accessToken: String? = null): ApiCallResult<ApiVolunteerImpactResponse> {
    return when (val response = get("/volunteer/impact", token = accessToken)) {
      is ApiCallResult.Success -> {
        val parsed = parseVolunteerImpactResponse(response.data)
        ApiCallResult.Success(parsed)
      }
      is ApiCallResult.Failure -> response
    }
  }

  suspend fun getVolunteerAnalyticsEarnings(accessToken: String? = null): ApiCallResult<ApiVolunteerAnalyticsEarningsResponse> {
    return when (val response = get("/volunteer/analytics/earnings", token = accessToken)) {
      is ApiCallResult.Success -> {
        Log.d("VolunteerAnalyticsApi", "baseUrl=$baseUrl earningsRaw=${response.data}")
        val parsed = parseVolunteerAnalyticsEarningsResponse(response.data)
          ?: decodeApiModel<ApiVolunteerAnalyticsEarningsResponse>(response.data)
        Log.d("VolunteerAnalyticsApi", "earningsParsed=$parsed")
        parsed?.let {
          ApiCallResult.Success(parsed)
        } ?: ApiCallResult.Failure("Invalid volunteer analytics earnings response.")
      }
      is ApiCallResult.Failure -> response
    }
  }

  suspend fun getVolunteerAnalyticsPerformance(accessToken: String? = null): ApiCallResult<ApiVolunteerAnalyticsPerformanceResponse> {
    return when (val response = get("/volunteer/analytics/performance", token = accessToken)) {
      is ApiCallResult.Success -> {
        Log.d("VolunteerAnalyticsApi", "baseUrl=$baseUrl performanceRaw=${response.data}")
        val parsed = parseVolunteerAnalyticsPerformanceResponse(response.data)
          ?: decodeApiModel<ApiVolunteerAnalyticsPerformanceResponse>(response.data)
        Log.d("VolunteerAnalyticsApi", "performanceParsed=$parsed")
        parsed?.let {
          ApiCallResult.Success(parsed)
        } ?: ApiCallResult.Failure("Invalid volunteer analytics performance response.")
      }
      is ApiCallResult.Failure -> response
    }
  }

  suspend fun getVolunteerAnalyticsReviews(
    accessToken: String? = null,
    page: Int = 1,
    perPage: Int = 100,
    rating: Int? = null
  ): ApiCallResult<ApiVolunteerAnalyticsReviewsResponse> {
    val queryParts = mutableListOf(
      "page=${page.coerceAtLeast(1)}",
      "per_page=${perPage.coerceIn(1, 100)}"
    )
    if (rating != null) {
      queryParts += "rating=$rating"
    }
    val path = "/volunteer/analytics/reviews?${queryParts.joinToString("&")}"
    return when (val response = get(path, token = accessToken)) {
      is ApiCallResult.Success -> {
        val parsed = parseVolunteerAnalyticsReviewsResponse(response.data)
          ?: decodeApiModel<ApiVolunteerAnalyticsReviewsResponse>(response.data)
        parsed?.let {
          ApiCallResult.Success(parsed)
        } ?: ApiCallResult.Failure("Invalid volunteer analytics reviews response.")
      }
      is ApiCallResult.Failure -> response
    }
  }

  suspend fun submitVolunteerAnalyticsWithdrawal(
    accessToken: String? = null,
    amountEgp: Int,
    method: String
  ): ApiCallResult<ApiActionResult> {
    val body = JsonObject(
      mapOf(
        "amount_egp" to JsonPrimitive(amountEgp.coerceAtLeast(1)),
        "method" to JsonPrimitive(method)
      )
    )
    return if (accessToken.isNullOrBlank()) {
      when (val response = post("/volunteer/analytics/withdrawals", body = body, token = null)) {
        is ApiCallResult.Success -> {
          val message = response.data?.asObjectOrNull()?.readString("message")
            ?: "Withdrawal submitted successfully."
          ApiCallResult.Success(ApiActionResult(success = true, message = message))
        }
        is ApiCallResult.Failure -> response
      }
    } else {
      actionPost(
        "/volunteer/analytics/withdrawals",
        accessToken,
        "Withdrawal submitted successfully.",
        body
      )
    }
  }

  suspend fun rateVolunteer(
    accessToken: String,
    requestId: String,
    rating: Int,
    comment: String?,
    issues: List<String> = emptyList()
  ): ApiCallResult<ApiActionResult> {
    val bodyEntries = linkedMapOf<String, JsonElement>(
      "rating" to JsonPrimitive(rating)
    )
    comment?.trim()?.takeIf { it.isNotBlank() }?.let { trimmed ->
      bodyEntries["comment"] = JsonPrimitive(trimmed)
    }
    if (issues.isNotEmpty()) {
      bodyEntries["issues"] = JsonArray(issues.map { JsonPrimitive(it) })
    }
    return actionPost("/help-requests/$requestId/rate", accessToken, "Volunteer rated successfully.", JsonObject(bodyEntries))
  }

  suspend fun createRequest(accessToken: String, request: ApiCreateRequest): ApiCallResult<ApiVolunteerRequest> {
    val fromLat = request.fromLat ?: DEFAULT_LAT
    val fromLng = request.fromLng ?: DEFAULT_LNG
    val toLat = request.toLat ?: fromLat
    val toLng = request.toLng ?: fromLng
    val body = JsonObject(
      mapOf(
        "from_label" to JsonPrimitive(request.location.trim()),
        "from_lat" to JsonPrimitive(fromLat),
        "from_lng" to JsonPrimitive(fromLng),
        "to_label" to JsonPrimitive(request.destination.trim()),
        "to_lat" to JsonPrimitive(toLat),
        "to_lng" to JsonPrimitive(toLng),
        "assistance_type" to JsonPrimitive(request.helpType.ifBlank { "general" }),
        "urgency" to JsonPrimitive(request.urgency.ifBlank { "medium" }),
        "details" to JsonPrimitive(request.description.ifBlank { request.helpType }),
        "payment_method" to JsonPrimitive(request.paymentMethod.lowercase(Locale.getDefault())),
        "service_fee" to JsonPrimitive(request.serviceFee),
        "hours" to JsonPrimitive(request.hours),
        "price_per_hour" to JsonPrimitive(request.pricePerHour)
      )
    )

    return when (val response = post("/help-requests", body = body, token = accessToken)) {
      is ApiCallResult.Success -> {
        logApiPayload("POST /help-requests", response.data)
        val parsed = response.data?.asObjectOrNull()?.let { parseVolunteerRequest(it) }
        if (parsed != null) {
          ApiCallResult.Success(parsed)
        } else {
          ApiCallResult.Failure("Failed to parse created request.")
        }
      }
      is ApiCallResult.Failure -> response
    }
  }

  suspend fun acceptRequest(accessToken: String, requestId: String): ApiCallResult<ApiActionResult> {
    return actionPost("/help-requests/$requestId/accept", accessToken, "Request accepted.")
  }

  suspend fun declineRequest(accessToken: String, requestId: String): ApiCallResult<ApiActionResult> {
    return actionPost("/help-requests/$requestId/decline", accessToken, "Request declined.")
  }

  suspend fun cancelRequest(accessToken: String, requestId: String): ApiCallResult<ApiActionResult> {
    return actionPost("/help-requests/$requestId/cancel", accessToken, "Request cancelled.")
  }

  suspend fun completeRequest(accessToken: String, requestId: String): ApiCallResult<ApiActionResult> {
    return actionPost("/help-requests/$requestId/complete", accessToken, "Request marked as completed.")
  }

  suspend fun payRequest(
    accessToken: String,
    requestId: String,
    paymentMethod: String,
    amountEgp: Int,
    customer: ApiPaymentCustomerDetails? = null
  ): ApiCallResult<ApiPayRequestResponse> {
    val normalizedAmount = amountEgp.coerceAtLeast(1)
    val bodyContent = linkedMapOf<String, JsonElement>(
      "payment_method" to JsonPrimitive(paymentMethod.lowercase(Locale.getDefault())),
      "amount_egp" to JsonPrimitive(normalizedAmount),
      "currency" to JsonPrimitive("EGP")
    )
    customer?.let {
      bodyContent["first_name"] = JsonPrimitive(it.firstName)
      bodyContent["last_name"] = JsonPrimitive(it.lastName)
      bodyContent["email"] = JsonPrimitive(it.email)
      bodyContent["phone_number"] = JsonPrimitive(it.phoneNumber)
    }
    val body = JsonObject(bodyContent)
    return when (val response = post("/help-requests/$requestId/pay", body = body, token = accessToken)) {
      is ApiCallResult.Success -> {
        val parsed = parseLegacyPayRequestResponse(response.data, fallbackMethod = paymentMethod)
        if (parsed != null) {
          ApiCallResult.Success(parsed)
        } else {
          ApiCallResult.Failure("Invalid payment response.")
        }
      }
      is ApiCallResult.Failure -> response
    }
  }

  suspend fun getRequestMessages(accessToken: String, requestId: String, perPage: Int = 20): ApiCallResult<List<ApiHelpRequestMessage>> {
    return when (val response = get("/help-requests/$requestId/messages?per_page=$perPage", token = accessToken)) {
      is ApiCallResult.Success -> {
        val list = response.data?.asArrayOrNull()?.mapNotNull { el ->
          el.asObjectOrNull()?.let { obj ->
            ApiHelpRequestMessage(
              id = obj.readString("id") ?: "",
              senderId = obj.readString("senderId") ?: "",
              senderName = obj.readString("senderName") ?: "",
              message = obj.readString("message") ?: "",
              createdAtEpochSeconds = obj.readLong("createdAtEpochSeconds") ?: 0L
            )
          }
        } ?: emptyList()
        ApiCallResult.Success(list)
      }
      is ApiCallResult.Failure -> response
    }
  }

  suspend fun sendRequestMessage(accessToken: String, requestId: String, message: String): ApiCallResult<ApiActionResult> {
    val body = JsonObject(mapOf("message" to JsonPrimitive(message)))
    return when (val response = post("/help-requests/$requestId/messages", body = body, token = accessToken)) {
      is ApiCallResult.Success -> ApiCallResult.Success(ApiActionResult(true, "Message sent."))
      is ApiCallResult.Failure -> response
    }
  }

  suspend fun checkoutCard(
    accessToken: String,
    requestId: String,
    amountEgp: Int,
    customer: ApiPaymentCustomerDetails? = null
  ): ApiCallResult<ApiCheckoutResponse> {
    val body = buildCheckoutBody(requestId = requestId, amountEgp = amountEgp, customer = customer)
    return when (val response = post("/payments/card/checkout", body = body, token = accessToken)) {
      is ApiCallResult.Success -> {
        val parsed = parseCheckoutResponse(response.data)
        if (parsed != null) {
          ApiCallResult.Success(parsed)
        } else {
          ApiCallResult.Failure("Invalid card checkout response.")
        }
      }
      is ApiCallResult.Failure -> response
    }
  }

  suspend fun checkoutWallet(
    accessToken: String,
    requestId: String,
    amountEgp: Int,
    customer: ApiPaymentCustomerDetails? = null
  ): ApiCallResult<ApiCheckoutResponse> {
    val body = buildCheckoutBody(requestId = requestId, amountEgp = amountEgp, customer = customer)
    return when (val response = post("/payments/wallet/checkout", body = body, token = accessToken)) {
      is ApiCallResult.Success -> {
        val parsed = parseCheckoutResponse(response.data)
        if (parsed != null) {
          ApiCallResult.Success(parsed)
        } else {
          ApiCallResult.Failure("Invalid wallet checkout response.")
        }
      }
      is ApiCallResult.Failure -> response
    }
  }

  suspend fun getPaymentStatus(accessToken: String, paymentId: String): ApiCallResult<ApiPaymentStatus> {
    return when (val response = get("/payments/$paymentId", token = accessToken)) {
      is ApiCallResult.Success -> {
        val parsed = parsePaymentStatusResponse(response.data, fallbackPaymentId = paymentId)
        if (parsed != null) {
          ApiCallResult.Success(parsed)
        } else {
          ApiCallResult.Failure("Invalid payment status response.")
        }
      }
      is ApiCallResult.Failure -> response
    }
  }

  suspend fun refreshPayment(accessToken: String, paymentId: String): ApiCallResult<ApiPaymentStatus> {
    return when (val response = post("/payments/$paymentId/refresh", body = null, token = accessToken)) {
      is ApiCallResult.Success -> {
        val parsed = parsePaymentStatusResponse(response.data, fallbackPaymentId = paymentId)
        if (parsed != null) {
          ApiCallResult.Success(parsed)
        } else {
          ApiCallResult.Failure("Invalid refresh response.")
        }
      }
      is ApiCallResult.Failure -> response
    }
  }

  private fun buildCheckoutBody(
    requestId: String,
    amountEgp: Int,
    customer: ApiPaymentCustomerDetails?
  ): JsonObject {
    val normalizedAmount = amountEgp.coerceAtLeast(1)
    val body = linkedMapOf<String, JsonElement>(
      "request_id" to JsonPrimitive(requestId),
      "help_request_id" to JsonPrimitive(requestId),
      "amount_egp" to JsonPrimitive(normalizedAmount),
      "currency" to JsonPrimitive("EGP")
    )
    customer?.let {
      body["first_name"] = JsonPrimitive(it.firstName)
      body["last_name"] = JsonPrimitive(it.lastName)
      body["email"] = JsonPrimitive(it.email)
      body["phone_number"] = JsonPrimitive(it.phoneNumber)
      body["phone"] = JsonPrimitive(it.phoneNumber)
    }
    return JsonObject(body)
  }

  private fun parseCheckoutResponse(data: JsonElement?): ApiCheckoutResponse? {
    val root = data?.asObjectOrNull() ?: return null
    val payload = root["data"]?.asObjectOrNull() ?: root
    val payment = payload["payment"]?.asObjectOrNull()
      ?: payload["checkout"]?.asObjectOrNull()
      ?: payload["gateway"]?.asObjectOrNull()
      ?: payload["transaction"]?.asObjectOrNull()
      ?: payload

    val checkoutUrl = payment.readString(
      "checkout_url",
      "checkoutUrl",
      "url",
      "redirect_url",
      "redirectUrl",
      "payment_url",
      "paymentUrl",
      "iframe_url",
      "iframeUrl"
    ) ?: payload.readString(
      "checkout_url",
      "checkoutUrl",
      "url",
      "redirect_url",
      "redirectUrl",
      "payment_url",
      "paymentUrl",
      "iframe_url",
      "iframeUrl"
    )

    val paymentId = payment.readString(
      "payment_id",
      "paymentId",
      "id",
      "payment_reference",
      "paymentReference",
      "transaction_id",
      "transactionId",
      "reference"
    ) ?: payload.readString(
      "payment_id",
      "paymentId",
      "id",
      "payment_reference",
      "paymentReference",
      "transaction_id",
      "transactionId",
      "reference"
    )

    if (paymentId.isNullOrBlank()) {
      return null
    }

    return ApiCheckoutResponse(
      checkout_url = checkoutUrl.orEmpty(),
      payment_id = paymentId
    )
  }

  private fun parseLegacyPayRequestResponse(
    data: JsonElement?,
    fallbackMethod: String
  ): ApiPayRequestResponse? {
    val root = data?.asObjectOrNull() ?: return null
    val payload = root["data"]?.asObjectOrNull() ?: root
    val payment = payload["payment"]?.asObjectOrNull()
      ?: payload["checkout"]?.asObjectOrNull()
      ?: payload["gateway"]?.asObjectOrNull()
      ?: payload["transaction"]?.asObjectOrNull()
      ?: payload

    val checkoutUrl = payment.readString(
      "checkout_url",
      "checkoutUrl",
      "url",
      "redirect_url",
      "redirectUrl",
      "payment_url",
      "paymentUrl",
      "iframe_url",
      "iframeUrl"
    ) ?: payload.readString(
      "checkout_url",
      "checkoutUrl",
      "url",
      "redirect_url",
      "redirectUrl",
      "payment_url",
      "paymentUrl",
      "iframe_url",
      "iframeUrl"
    )

    val paymentId = payment.readString(
      "payment_id",
      "paymentId",
      "id",
      "payment_reference",
      "paymentReference",
      "transaction_id",
      "transactionId",
      "reference"
    ) ?: payload.readString(
      "payment_id",
      "paymentId",
      "id",
      "payment_reference",
      "paymentReference",
      "transaction_id",
      "transactionId",
      "reference"
    )

    val paymentMethod = payload.readString("payment_method", "paymentMethod", "method")
      ?: root.readString("payment_method", "paymentMethod", "method")
      ?: fallbackMethod

    val status = payload.readString("status", "payment_status", "paymentStatus", "state")
      ?: root.readString("status", "payment_status", "paymentStatus", "state")
      ?: if (!checkoutUrl.isNullOrBlank()) "pending_payment" else ""

    val message = payload.readString("message")
      ?: root.readString("message")
      ?: if (!checkoutUrl.isNullOrBlank()) "Checkout created successfully." else ""

    if (paymentId.isNullOrBlank() && checkoutUrl.isNullOrBlank()) {
      return null
    }

    return ApiPayRequestResponse(
      paymentMethod = paymentMethod,
      status = status,
      message = message,
      checkoutUrl = checkoutUrl,
      paymentId = paymentId
    )
  }

  private fun parsePaymentStatusResponse(
    data: JsonElement?,
    fallbackPaymentId: String
  ): ApiPaymentStatus? {
    val root = data?.asObjectOrNull() ?: return null
    val payload = root["data"]?.asObjectOrNull() ?: root
    val payment = payload["payment"]?.asObjectOrNull()
      ?: payload["transaction"]?.asObjectOrNull()
      ?: payload["status"]?.asObjectOrNull()
      ?: payload

    val status = payment.readString(
      "status",
      "payment_status",
      "paymentStatus",
      "state"
    ).orEmpty()

    val rawPaymentAmount = payment.readDouble(
      "amount_egp",
      "amountEgp",
      "payable_amount_egp",
      "payableAmountEgp",
      "amount",
      "value",
      "total"
    )
    val paymentAmountCents = payment.readDouble("amount_cents", "amountCents")
    val amount = when {
      paymentAmountCents != null && paymentAmountCents > 0.0 -> paymentAmountCents / 100.0
      rawPaymentAmount != null && rawPaymentAmount > MAX_VALID_TOTAL_EGP && rawPaymentAmount % 100.0 == 0.0 ->
        rawPaymentAmount / 100.0
      rawPaymentAmount != null -> rawPaymentAmount
      else -> 0.0
    }

    val currency = payment.readString("currency", "currency_code", "currencyCode").orEmpty()

    val success = payment.readBoolean(
      "success",
      "is_success",
      "isSuccess",
      "paid",
      "is_paid",
      "isPaid",
      "captured"
    ) ?: inferPaymentSuccess(status)

    return ApiPaymentStatus(
      id = payment.readString("id", "payment_id", "paymentId").orEmpty().ifBlank { fallbackPaymentId },
      status = status,
      amount = amount,
      currency = currency,
      success = success
    )
  }

  private fun inferPaymentSuccess(status: String): Boolean {
    return when (status.trim().lowercase(Locale.getDefault())) {
      "success",
      "succeeded",
      "approved",
      "paid",
      "captured",
      "completed",
      "settled",
      "authorized" -> true
      else -> false
    }
  }

  suspend fun confirmPaymobPayment(paymentId: String, success: Boolean = true): ApiCallResult<ApiActionResult> {
    val body = JsonObject(
      mapOf(
        "id" to JsonPrimitive(paymentId),
        "success" to JsonPrimitive(success)
      )
    )
    return when (val response = post("/payments/paymob/callback", body = body)) {
      is ApiCallResult.Success -> {
        val message = response.data?.asObjectOrNull()?.readString("message")
          ?: "Payment updated."
        ApiCallResult.Success(ApiActionResult(success = true, message = message))
      }
      is ApiCallResult.Failure -> response
    }
  }

  suspend fun submitRating(accessToken: String, locationId: String, score: Int, comment: String?): ApiCallResult<ApiLocation> {
    val content = linkedMapOf<String, JsonElement>(
      "rating" to JsonPrimitive(score)
    )
    comment?.takeIf { it.isNotBlank() }?.let {
      content["comment"] = JsonPrimitive(it)
    }

    return when (
      val response = post(
        "/locations/$locationId/ratings",
        body = JsonObject(content),
        token = accessToken
      )
    ) {
      is ApiCallResult.Success -> {
        val parsedLocation = response.data?.asObjectOrNull()?.let { root ->
          parseLocation(root) ?: root["location"]?.asObjectOrNull()?.let { parseLocation(it) }
        }
        if (parsedLocation != null) {
          return ApiCallResult.Success(parsedLocation)
        }

        val fallbackLocation = ApiLocation(
          id = locationId,
          name = "Location",
          category = "Unknown",
          lat = 0.0,
          lng = 0.0,
          rating = score.toDouble(),
          totalRatings = 1,
          features = ApiLocationFeatures(
            ramp = false,
            elevator = false,
            accessibleToilet = false,
            accessibleParking = false,
            wideEntrance = false,
            brailleSignage = false
          ),
          recentReports = emptyList(),
          distance = ""
        )
        ApiCallResult.Success(fallbackLocation)
      }
      is ApiCallResult.Failure -> response
    }
  }

  suspend fun submitLocationReport(
    accessToken: String,
    request: ApiLocationReportRequest
  ): ApiCallResult<ApiLocationReportResult> {
    val fields = linkedMapOf(
      "name" to request.name.trim(),
      "address" to request.address.trim(),
      "government_id" to request.governmentId.toString(),
      "latitude" to request.latitude.toString(),
      "longitude" to request.longitude.toString(),
      "rating" to request.rating.toString(),
      "ramp_available" to request.rampAvailable.toFormBoolean(),
      "elevator_available" to request.elevatorAvailable.toFormBoolean(),
      "parking" to request.parking.toFormBoolean(),
      "wheelchair_accessible" to request.wheelchairAccessible.toFormBoolean(),
      "wide_entrance" to request.wideEntrance.toFormBoolean(),
      "accessible_toilet" to request.accessibleToilet.toFormBoolean()
    )
    request.categoryId?.let {
      fields["category_id"] = it.toString()
    }
    request.comment?.takeIf { it.isNotBlank() }?.let {
      fields["comment"] = it.trim()
    }
    request.notes?.takeIf { it.isNotBlank() }?.let {
      fields["notes"] = it.trim()
    }

    return when (
      val response = postMultipart(
        "/locations/report",
        fields = fields,
        filePart = null,
        token = accessToken
      )
    ) {
      is ApiCallResult.Success -> {
        val root = response.data?.asObjectOrNull()
        val message = root?.readString("message")
          ?: "Place report submitted successfully."
        val locationObj = root?.get("location")?.asObjectOrNull()
        val locationId = locationObj?.readString("id")
          ?: locationObj?.get("id")?.jsonPrimitive?.longOrNull?.toString()
          ?: locationObj?.get("id")?.jsonPrimitive?.intOrNull?.toString()
        val contributionObj = root?.get("accessibility_contribution")?.asObjectOrNull()
        val contribution = contributionObj?.let { parseAccessibilityContribution(it) }
        Log.d("LocationReportApi", "parsed locationId=$locationId contribution=$contribution")
        ApiCallResult.Success(
          ApiLocationReportResult(
            action = ApiActionResult(success = true, message = message),
            contribution = contribution,
            locationId = locationId ?: contribution?.locationId
          )
        )
      }
      is ApiCallResult.Failure -> {
        Log.w(
          "LocationReportApi",
          "Location report failed: status=${response.statusCode}, field=${response.validationField}, " +
            "errors=${response.validationErrors}, message=${response.message}"
        )
        response
      }
    }
  }

  private fun parseAccessibilityContribution(obj: JsonObject): ApiAccessibilityContribution {
    val locationIdString = obj.readString("location_id")
      ?: obj["location_id"]?.jsonPrimitive?.longOrNull?.toString()
      ?: obj["location_id"]?.jsonPrimitive?.intOrNull?.toString()
    val idInt = obj["id"]?.jsonPrimitive?.intOrNull
      ?: obj["id"]?.jsonPrimitive?.longOrNull?.toInt()
    return ApiAccessibilityContribution(
      id = idInt,
      locationId = locationIdString,
      wheelchairAccessible = obj.readBoolean("wheelchair_accessible") ?: false,
      rampAvailable = obj.readBoolean("ramp_available") ?: false,
      elevatorAvailable = obj.readBoolean("elevator_available") ?: false,
      parking = obj.readBoolean("parking") ?: false,
      accessibleToilet = obj.readBoolean("accessible_toilet") ?: false,
      wideEntrance = obj.readBoolean("wide_entrance") ?: false,
      status = obj.readString("status"),
      pendingVerification = obj.readBoolean("pending_verification") ?: true
    )
  }

  private fun Boolean.toFormBoolean(): String = if (this) "1" else "0"

  suspend fun sendSupportMessage(accessToken: String, subject: String, message: String): ApiCallResult<ApiActionResult> {
    val body = JsonObject(
      mapOf(
        "subject" to JsonPrimitive(subject.trim()),
        "message" to JsonPrimitive(message.trim()),
        "category" to JsonPrimitive("technical"),
        "priority" to JsonPrimitive("medium")
      )
    )
    return when (val response = post("/support/tickets", body = body, token = accessToken)) {
      is ApiCallResult.Success -> {
        val messageText = response.data?.asObjectOrNull()?.readString("message")
          ?: "Support ticket submitted successfully."
        ApiCallResult.Success(ApiActionResult(success = true, message = messageText))
      }
      is ApiCallResult.Failure -> response
    }
  }

  private suspend fun getNearbyLocations(
    search: String?,
    lat: Double = DEFAULT_LAT,
    lng: Double = DEFAULT_LNG,
    radiusKm: Int = 20
  ): ApiCallResult<List<ApiLocation>> {
    val path = buildString {
      append("/locations/nearby")
      append("?lat=$lat&lng=$lng&radius_km=$radiusKm")
      append("&search=${encodeQuery(search.orEmpty())}")
      append("&category_id=&government_id=")
    }

    return when (val nearby = get(path)) {
      is ApiCallResult.Success -> {
        val items = extractItems(nearby.data)
        ApiCallResult.Success(items.mapNotNull { parseLocation(it) })
      }
      is ApiCallResult.Failure -> {
        when (val fallback = get("/locations?page=1&per_page=200")) {
          is ApiCallResult.Success -> {
            val items = extractItems(fallback.data)
            ApiCallResult.Success(items.mapNotNull { parseLocation(it) })
          }
          is ApiCallResult.Failure -> nearby
        }
      }
    }
  }

  private suspend fun actionPost(
    path: String,
    token: String,
    defaultMessage: String,
    body: JsonElement? = null
  ): ApiCallResult<ApiActionResult> {
    return when (val response = post(path, body = body, token = token)) {
      is ApiCallResult.Success -> {
        val message = response.data?.asObjectOrNull()?.readString("message")
          ?: defaultMessage
        ApiCallResult.Success(ApiActionResult(success = true, message = message))
      }
      is ApiCallResult.Failure -> response
    }
  }

  private inline fun <reified T> decodeApiModel(data: JsonElement?): T? {
    val element = data ?: return null
    return runCatching { json.decodeFromJsonElement<T>(element) }.getOrNull()
  }

  private fun parseRegistrationStartResponse(data: JsonElement?): ApiCallResult<ApiRegistrationStartResponse> {
    val root = data?.asObjectOrNull()
    val verificationRequired = root.readBoolean(
      "verification_required",
      "verificationRequired",
      "requires_verification",
      "requiresVerification"
    ) == true
    val challenge = parseEmailVerificationChallenge(data)
    if (challenge != null) {
      return ApiCallResult.Success(ApiRegistrationStartResponse.VerificationRequired(challenge))
    }
    if (verificationRequired) {
      return ApiCallResult.Failure("Server requested email verification, but the challenge details were incomplete.")
    }

    return when (val auth = parseAuthResponse(data)) {
      is ApiCallResult.Success -> ApiCallResult.Success(ApiRegistrationStartResponse.Authenticated(auth.data))
      is ApiCallResult.Failure -> auth
    }
  }

  private fun parseAuthResponse(data: JsonElement?): ApiCallResult<ApiAuthResponse> {
    val dataObject = data?.asObjectOrNull()
      ?: return ApiCallResult.Failure("Failed to parse authentication response.")
    val tokenObject = dataObject["tokens"]?.asObjectOrNull()
    val accessToken = dataObject.readString("token", "access_token", "accessToken")
      ?: tokenObject.readString("access_token", "accessToken", "token")
      ?: return ApiCallResult.Failure("Server did not return an auth token.")
    val refreshToken = dataObject.readString("refresh_token", "refreshToken")
      ?: tokenObject.readString("refresh_token", "refreshToken")
      ?: accessToken
    val rawExpiry = tokenObject.readLong("expires_at_epoch_seconds", "expiresAtEpochSeconds", "expires_at", "expiresIn")
      ?: dataObject.readLong("expires_at_epoch_seconds", "expiresAtEpochSeconds", "expires_at", "expiresIn")
      ?: Long.MAX_VALUE
    val expiresAt = normalizeExpiryEpochSeconds(rawExpiry)

    val user = parseUser(dataObject["user"] ?: dataObject)
      ?: return ApiCallResult.Failure("Server did not return user details.")

    return ApiCallResult.Success(
      ApiAuthResponse(
        user = user,
        tokens = ApiTokenPair(
          accessToken = accessToken,
          refreshToken = refreshToken,
          expiresAtEpochSeconds = expiresAt
        )
      )
    )
  }

  private fun parseEmailVerificationChallenge(data: JsonElement?): ApiEmailVerificationChallenge? {
    val root = data?.asObjectOrNull() ?: return null
    val hasChallengeFlag = root.readBoolean(
      "verification_required",
      "verificationRequired",
      "requires_verification",
      "requiresVerification"
    ) ?: false
    val candidate = root["challenge"]?.asObjectOrNull()
      ?: root["verification"]?.asObjectOrNull()
      ?: root["email_verification"]?.asObjectOrNull()
      ?: root.takeIf {
        hasChallengeFlag || it.readString("challenge_id", "challengeId") != null
      }
      ?: return null

    val challengeId = candidate.readString("challenge_id", "challengeId", "id")
      ?: root.readString("challenge_id", "challengeId", "id")
      ?: return null
    val email = candidate.readString("email")
      ?: root.readString("email")
      ?: return null
    val role = when ((candidate.readString("role") ?: root.readString("role"))?.lowercase(Locale.getDefault())) {
      "volunteer" -> ApiUserRole.Volunteer
      else -> ApiUserRole.User
    }
    val expiresAt = candidate.readLong("expires_at_epoch_seconds", "expiresAtEpochSeconds")
      ?: root.readLong("expires_at_epoch_seconds", "expiresAtEpochSeconds")
      ?: return null
    val resendAvailableAt = candidate.readLong(
      "resend_available_at_epoch_seconds",
      "resendAvailableAtEpochSeconds"
    )
      ?: root.readLong("resend_available_at_epoch_seconds", "resendAvailableAtEpochSeconds")
      ?: expiresAt
    val codeLength = candidate.readInt("code_length", "codeLength")
      ?: root.readInt("code_length", "codeLength")
      ?: 6
    val message = candidate.readString("message")
      ?: root.readString("message")
      ?: "We sent a verification code to your email."

    return ApiEmailVerificationChallenge(
      challengeId = challengeId,
      email = email,
      role = role,
      expiresAtEpochSeconds = expiresAt,
      resendAvailableAtEpochSeconds = resendAvailableAt,
      codeLength = codeLength,
      message = message
    )
  }

  private fun parseUser(element: JsonElement?): ApiAuthUser? {
    val root = element?.asObjectOrNull() ?: return null
    val obj = root["user"]?.asObjectOrNull()
      ?: root["data"]?.asObjectOrNull()
      ?: root["profile"]?.asObjectOrNull()
      ?: root
    val id = obj.readString("id") ?: return null
    val role = when (obj.readString("role")?.lowercase(Locale.getDefault())) {
      "volunteer" -> ApiUserRole.Volunteer
      else -> ApiUserRole.User
    }
    val fullName = obj.readString("full_name", "name").orEmpty()
    val email = obj.readString("email").orEmpty()
    val phone = obj.readString("phone").orEmpty()
    val disabilityType = obj.readString("disability_type")
    val memberSince = obj.readString("member_since")
      ?: parseMemberSince(obj.readString("created_at"))
    val volunteerLive = obj.readBoolean("is_live") ?: false
    val roleVerifiedAt = obj.readString("role_verified_at", "roleVerifiedAt")
    val volunteerStatus = obj.readString("volunteer_status", "volunteerStatus")
    val isActive = obj.readBoolean("is_active", "isActive") ?: true

    return ApiAuthUser(
      id = id,
      role = role,
      fullName = fullName,
      email = email,
      phone = phone,
      location = obj.readString("location", "city").orEmpty(),
      profilePhotoPath = resolveProfilePhotoPath(
        obj.readString(
          "profile_photo_path",
          "profilePhotoPath",
          "profile_photo_url",
          "profilePhotoUrl",
          "avatar_url",
          "avatarUrl"
        )
      ),
      disabilityType = disabilityType,
      memberSince = memberSince,
      passwordChangedAt = obj.readString("password_changed_at", "passwordChangedAt"),
      volunteerLive = volunteerLive,
      roleVerifiedAt = roleVerifiedAt,
      volunteerStatus = volunteerStatus,
      isActive = isActive,
      contributionStats = ApiContributionStats(0, 0, 0),
      notificationSettings = ApiNotificationSettings(),
      privacySettings = ApiPrivacySettings()
    )
  }

  private fun parseSession(obj: JsonObject, nowEpochSeconds: Long): ApiSessionDto? {
    val id = obj.readString("id", "session_id") ?: return null
    val createdAt = obj.readEpochSeconds(
      "created_at_epoch_seconds",
      "createdAtEpochSeconds",
      "created_at",
      "created_at_epoch",
      "created_at_unix"
    ) ?: nowEpochSeconds
    val lastSeenAt = obj.readEpochSeconds(
      "last_seen_at_epoch_seconds",
      "lastSeenAtEpochSeconds",
      "last_seen_at",
      "last_seen",
      "last_activity_at",
      "updated_at"
    ) ?: createdAt

    return ApiSessionDto(
      id = id,
      deviceName = obj.readString("device_name", "deviceName", "device", "user_agent")
        ?: "Unknown device",
      createdAtEpochSeconds = createdAt,
      lastSeenAtEpochSeconds = lastSeenAt,
      isCurrent = obj.readBoolean("is_current", "isCurrent", "current") ?: false
    )
  }

  private fun parseNotificationSettings(element: JsonElement?): ApiNotificationSettings? {
    val root = element?.asObjectOrNull() ?: return null
    val obj = root["notification_preferences"]?.asObjectOrNull()
      ?: root["notification_settings"]?.asObjectOrNull()
      ?: root["preferences"]?.asObjectOrNull()
      ?: root
    return ApiNotificationSettings(
      pushEnabled = obj.readBoolean("push_enabled", "pushEnabled") ?: true,
      emailEnabled = obj.readBoolean("email_enabled", "emailEnabled") ?: true,
      smsEnabled = obj.readBoolean("sms_enabled", "smsEnabled") ?: false,
      volunteerRequests = obj.readBoolean("volunteer_requests", "volunteerRequests") ?: true,
      volunteerAccepted = obj.readBoolean("volunteer_accepted", "volunteerAccepted") ?: true,
      locationUpdates = obj.readBoolean("location_updates", "locationUpdates") ?: true,
      newRatings = obj.readBoolean("new_ratings", "newRatings") ?: true,
      communityUpdates = obj.readBoolean("community_updates", "communityUpdates") ?: false,
      marketingEmails = obj.readBoolean("marketing_emails", "marketingEmails") ?: false,
      soundEnabled = obj.readBoolean("sound_enabled", "soundEnabled") ?: true,
      vibrationEnabled = obj.readBoolean("vibration_enabled", "vibrationEnabled") ?: true
    )
  }

  private fun parsePrivacySettings(element: JsonElement?): ApiPrivacySettings? {
    val root = element?.asObjectOrNull() ?: return null
    val obj = root["privacy_settings"]?.asObjectOrNull()
      ?: root["settings"]?.asObjectOrNull()
      ?: root
    return ApiPrivacySettings(
      locationSharing = obj.readBoolean("location_sharing", "locationSharing") ?: true,
      profileVisibility = obj.readBoolean("profile_visibility", "profileVisibility") ?: true,
      showRatings = obj.readBoolean("show_ratings", "showRatings") ?: true,
      activityStatus = obj.readBoolean("activity_status", "activityStatus") ?: true,
      twoFactorAuth = obj.readBoolean("two_factor_auth", "twoFactorAuth") ?: false
    )
  }

  private fun parseLocation(obj: JsonObject): ApiLocation? {
    val id = obj.readString("id") ?: return null
    val name = obj.readString("name", "title") ?: return null
    val lat = obj.readDouble("latitude", "lat") ?: return null
    val lng = obj.readDouble("longitude", "lng") ?: return null

    val categoryName = obj["category"]
      ?.asObjectOrNull()
      ?.readString("name")
      ?: obj.readString("category")
      ?: "Uncategorized"

    val accessibility = obj["accessibility"]?.asObjectOrNull()
    val verified = accessibility?.get("verified_report")?.asObjectOrNull()
    val contributions = accessibility?.get("contributions_summary")?.asObjectOrNull()
    val contributionsCount = contributions?.readInt("count") ?: 0

    val fallbackContribution: JsonObject? = if (verified == null && contributionsCount == 0) {
      obj["accessibility_contribution"]?.asObjectOrNull()
        ?: obj["latest_contribution"]?.asObjectOrNull()
        ?: accessibility?.get("latest_contribution")?.asObjectOrNull()
        ?: accessibility?.get("accessibility_contribution")?.asObjectOrNull()
    } else null

    val pendingCached = PendingContributionsCache.get(id)
    Log.d("parseLocation", "id=$id verified=${verified!=null} count=$contributionsCount pendingCached=${pendingCached!=null}")

    val features = ApiLocationFeatures(
      ramp = verified.readBoolean("ramp_available")
        ?: contributions.readBoolean("ramp_available")
        ?: fallbackContribution.readBoolean("ramp_available")
        ?: pendingCached?.rampAvailable
        ?: false,
      elevator = verified.readBoolean("elevator_available")
        ?: contributions.readBoolean("elevator_available")
        ?: fallbackContribution.readBoolean("elevator_available")
        ?: pendingCached?.elevatorAvailable
        ?: false,
      accessibleToilet = verified.readBoolean("accessible_toilet")
        ?: contributions.readBoolean("accessible_toilet")
        ?: fallbackContribution.readBoolean("accessible_toilet")
        ?: pendingCached?.accessibleToilet
        ?: false,
      accessibleParking = verified.readBoolean("parking")
        ?: contributions.readBoolean("parking")
        ?: fallbackContribution.readBoolean("parking")
        ?: pendingCached?.parking
        ?: false,
      wideEntrance = verified.readBoolean("wide_entrance")
        ?: contributions.readBoolean("wide_entrance")
        ?: fallbackContribution.readBoolean("wide_entrance")
        ?: pendingCached?.wideEntrance
        ?: false,
      brailleSignage = false
    ).let { base ->
      if (pendingCached != null && verified == null) {
        base.copy(
          ramp = base.ramp || pendingCached.rampAvailable,
          elevator = base.elevator || pendingCached.elevatorAvailable,
          accessibleToilet = base.accessibleToilet || pendingCached.accessibleToilet,
          accessibleParking = base.accessibleParking || pendingCached.parking,
          wideEntrance = base.wideEntrance || pendingCached.wideEntrance
        )
      } else base
    }

    val distanceKm = obj.readDouble("distance_km")
    val distanceLabel = if (distanceKm != null) {
      if (distanceKm >= 1.0) {
        String.format(Locale.US, "%.1f km", distanceKm)
      } else {
        "${(distanceKm * 1000).toInt()} m"
      }
    } else {
      obj.readString("distance") ?: ""
    }

    return ApiLocation(
      id = id,
      name = name,
      category = categoryName,
      lat = lat,
      lng = lng,
      rating = obj.readDouble("rating_avg", "rating") ?: 0.0,
      totalRatings = obj.readInt("ratings_count", "total_ratings") ?: 0,
      features = features,
      recentReports = emptyList(),
      distance = distanceLabel
    )
  }

  private fun parseGovernment(obj: JsonObject): ApiGovernment? {
    val id = obj.readInt("id", "government_id", "governmentId") ?: return null
    val englishName = obj.readString(
      "name",
      "english_name",
      "englishName",
      "name_en",
      "nameEn",
      "title",
      "label"
    )?.trim()
    val arabicName = obj.readString(
      "arabic_name",
      "arabicName",
      "name_ar",
      "nameAr",
      "title_ar",
      "titleAr",
      "label_ar",
      "labelAr"
    )?.trim()
    val fallbackName = obj.readString("accessible_locations")?.trim()
    val displayName = englishName
      ?.takeIf { it.isNotBlank() }
      ?: arabicName?.takeIf { it.isNotBlank() }
      ?: fallbackName?.takeIf { it.isNotBlank() }
      ?: return null

    return ApiGovernment(
      id = id,
      name = displayName,
      arabicName = arabicName?.takeIf { it.isNotBlank() && !it.equals(displayName, ignoreCase = false) }
    )
  }

  private fun parseCategory(obj: JsonObject): ApiCategory? {
    val id = obj.readInt("id", "category_id", "categoryId") ?: return null
    val name = obj.readString("name", "title", "label")?.trim()
      ?.takeIf { it.isNotBlank() }
      ?: return null
    val icon = obj.readString("icon", "icon_name", "iconName")?.trim()?.takeIf { it.isNotBlank() }

    return ApiCategory(
      id = id,
      name = name,
      icon = icon
    )
  }

  private fun parseVolunteerRequest(obj: JsonObject): ApiVolunteerRequest? {
    val id = obj.readString("id") ?: return null
    android.util.Log.d("AtharParse", "parseVolunteerRequest id=$id payment_status=${obj["payment_status"]} payment=${obj["payment"]} isPaid=${obj["is_paid"] ?: obj["isPaid"] ?: obj["paid"]}")
    val requester = obj["requester"]?.asObjectOrNull() ?: obj["user"]?.asObjectOrNull()
    val volunteer = obj["volunteer"]?.asObjectOrNull()
    val payment = obj["payment"]?.asObjectOrNull()
      ?: obj["payment_status"]?.asObjectOrNull()
      ?: obj["paymentStatus"]?.asObjectOrNull()
    val pricing = parseRequestPricing(obj)
    val paymentStatus = payment?.readString("status", "payment_status", "paymentStatus", "state")
      ?: obj.readString("payment_status", "paymentStatus", "payment_state", "paymentState")
    val isPaid = payment?.readBoolean(
      "success",
      "is_success",
      "isSuccess",
      "paid",
      "is_paid",
      "isPaid",
      "captured"
    ) ?: obj.readBoolean(
      "paid",
      "is_paid",
      "isPaid",
      "payment_success",
      "paymentSuccess"
    ) ?: paymentStatus?.let(::inferPaymentSuccess)
      ?: false

    val userId = obj.readString("requester_id", "user_id", "userId")
      ?: requester.readString("id")
      ?: ""
    val userName = requester.readString("full_name", "name")
      ?: obj.readString("requester_name", "user_name", "userName")
      ?: "User"
    val userType = requester.readString("disability_type", "user_type", "type")
      ?: obj.readString("user_type", "userType")
      ?: "User"

    return ApiVolunteerRequest(
      id = id,
      userId = userId,
      userName = userName,
      userType = userType,
      location = obj.readString("from_label", "fromLabel", "location") ?: "",
      requestTime = formatRelativeTime(obj.readString("created_at", "request_time", "requestTime")),
      status = obj.readString("status") ?: "pending",
      volunteerName = volunteer.readString("full_name", "name")
        ?: obj.readString("volunteer_name", "volunteerName"),
      description = obj.readString(
        "details",
        "description",
        "assistance_type",
        "help_type",
        "helpType"
      ) ?: "",
      hours = pricing.hours,
      pricePerHour = pricing.pricePerHour,
      totalAmountEgp = pricing.totalAmountEgp,
      paymentMethod = payment?.readString("method", "payment_method", "paymentMethod")
        ?: obj.readString("payment_method", "paymentMethod", "pay_method")
        ?: "cash",
      paymentStatus = paymentStatus,
      isPaid = isPaid
    )
  }

  private fun parseAssistanceRequest(obj: JsonObject): ApiAssistanceRequest? {
    val id = obj.readString("id") ?: return null
    val requester = obj["requester"]?.asObjectOrNull() ?: obj["user"]?.asObjectOrNull()
    val pricing = parseRequestPricing(obj)
    val payment = obj["payment"]?.asObjectOrNull()
      ?: obj["payment_status"]?.asObjectOrNull()
      ?: obj["paymentStatus"]?.asObjectOrNull()
    val paymentStatus = payment?.readString("status", "payment_status", "paymentStatus", "state")
      ?: obj.readString("payment_status", "paymentStatus", "payment_state", "paymentState")
    val isPaid = payment?.readBoolean(
      "success", "is_success", "isSuccess", "paid", "is_paid", "isPaid", "captured"
    ) ?: obj.readBoolean(
      "paid", "is_paid", "isPaid", "payment_success", "paymentSuccess"
    ) ?: paymentStatus?.let(::inferPaymentSuccess)
      ?: false

    val userName = requester.readString("full_name", "name")
      ?: obj.readString("requester_name", "user_name", "userName")
      ?: "User"
    val userType = requester.readString("disability_type", "user_type", "type")
      ?: obj.readString("user_type", "userType")
      ?: "User"

    val distance = obj.readDouble("distance_km")?.let {
      if (it >= 1.0) String.format(Locale.US, "%.1f km", it) else "${(it * 1000).toInt()} m"
    } ?: obj.readString("distance") ?: ""

    return ApiAssistanceRequest(
      id = id,
      userName = userName,
      userType = userType,
      location = obj.readString("from_label", "fromLabel", "location") ?: "",
      destination = obj.readString("to_label", "toLabel", "destination") ?: "",
      distance = distance,
      urgency = obj.readString("urgency", "priority") ?: "medium",
      helpType = obj.readString("assistance_type", "help_type", "helpType") ?: "General",
      requestTime = formatRelativeTime(obj.readString("created_at", "request_time", "requestTime")),
      status = obj.readString("status") ?: "pending",
      hours = pricing.hours,
      pricePerHour = pricing.pricePerHour,
      totalAmountEgp = pricing.totalAmountEgp,
      paymentMethod = payment?.readString("method", "payment_method", "paymentMethod")
        ?: obj.readString("payment_method", "paymentMethod", "pay_method")
        ?: "cash",
      paymentStatus = paymentStatus,
      isPaid = isPaid
    )
  }

  private data class RequestPricing(
    val hours: Int,
    val pricePerHour: Int,
    val totalAmountEgp: Int
  )

  private fun parseRequestPricing(obj: JsonObject): RequestPricing {
    val candidates = buildList {
      add(obj)
      obj["pricing"]?.asObjectOrNull()?.let { add(it) }
      obj["payment"]?.asObjectOrNull()?.let { add(it) }
      obj["fare"]?.asObjectOrNull()?.let { add(it) }
      obj["quote"]?.asObjectOrNull()?.let { add(it) }
      obj["amounts"]?.asObjectOrNull()?.let { add(it) }
    }

    fun readIntFromCandidates(vararg keys: String): Int? {
      candidates.forEach { candidate ->
        candidate.readInt(*keys)?.let { return it }
      }
      return null
    }

    fun readDoubleFromCandidates(vararg keys: String): Double? {
      candidates.forEach { candidate ->
        candidate.readDouble(*keys)?.let { return it }
      }
      return null
    }

    val hours = readIntFromCandidates(
      "hours",
      "duration_hours",
      "durationHours",
      "service_hours",
      "serviceHours",
      "requested_hours",
      "requestedHours"
    )?.coerceAtLeast(1) ?: 1

    val rawTotalAmount = readDoubleFromCandidates(
      "amount_egp",
      "total_amount_egp",
      "totalAmountEgp",
      "payable_amount_egp",
      "payableAmountEgp",
      "total_amount",
      "totalAmount",
      "total_price",
      "totalPrice",
      "grand_total",
      "grandTotal",
      "final_total",
      "finalTotal"
    )
    val rawServiceFee = readDoubleFromCandidates("service_fee", "serviceFee")
    val amountCents = readDoubleFromCandidates("amount_cents", "amountCents")

    val rawExplicitPricePerHour = readDoubleFromCandidates(
      "price_per_hour",
      "pricePerHour",
      "hourly_rate",
      "hourlyRate",
      "rate_per_hour",
      "ratePerHour"
    )

    // --- Resolve raw values, converting piasters to EGP when detected ---
    // Valid EGP range: pricePerHour 50-200, max total 200*8=1600.
    // Backend may return values in piasters (EGP * 100). Any value above
    // the valid EGP ceiling that is divisible by 100 is treated as piasters.

    val rawPrice = amountCents?.let { cents ->
      // If amount_cents is present and matches raw price * hours, raw price is in piasters
      val rp = rawExplicitPricePerHour?.takeIf { it > 0.0 }
      if (rp != null && cents > 0.0 && isApproximatelySameAmount(cents, rp * hours.toDouble())) {
        rp / 100.0
      } else {
        rp
      }
    } ?: rawExplicitPricePerHour?.takeIf { it > 0.0 }

    val pricePerHourEgp = when {
      rawPrice != null && rawPrice > 200.0 && rawPrice % 100.0 == 0.0 -> rawPrice / 100.0
      rawPrice != null -> rawPrice
      else -> null
    }

    val rawTotal = amountCents?.takeIf { it > 0.0 }?.let { it / 100.0 }
      ?: rawTotalAmount?.takeIf { it > 0.0 }
      ?: rawServiceFee?.takeIf { it > 0.0 }

    val maxValidTotalEgp = 200.0 * 8.0  // 1600
    val totalEgp = when {
      rawTotal != null && rawTotal > maxValidTotalEgp && rawTotal % 100.0 == 0.0 -> rawTotal / 100.0
      rawTotal != null -> rawTotal
      else -> null
    }

    val resolvedPricePerHour = when {
      pricePerHourEgp != null && pricePerHourEgp > 0.0 -> pricePerHourEgp.roundToInt()
      totalEgp != null && totalEgp > 0.0 -> (totalEgp / hours.toDouble()).roundToInt()
      else -> 50
    }.coerceAtLeast(1)

    val resolvedTotalAmount = when {
      totalEgp != null && totalEgp > 0.0 -> totalEgp.roundToInt()
      else -> (hours * resolvedPricePerHour).coerceAtLeast(1)
    }

    return RequestPricing(
      hours = hours,
      pricePerHour = resolvedPricePerHour,
      totalAmountEgp = resolvedTotalAmount
    )
  }

  private fun isApproximatelySameAmount(left: Double, right: Double): Boolean {
    return kotlin.math.abs(left - right) <= 0.99
  }

  private fun parseVolunteerIncomingResponse(data: JsonElement?): ApiVolunteerIncomingResponse {
    val root = data?.asObjectOrNull()
    val requests = extractVolunteerEndpointItems(data).mapNotNull { parseAssistanceRequest(it) }
    val counts = parseVolunteerCounts(
      root = root,
      fallbackIncoming = requests.size
    )
    val incomingAlert = parseIncomingAlert(
      root = root,
      fallbackCount = counts.incoming
    )
    return ApiVolunteerIncomingResponse(
      counts = counts,
      incomingAlert = incomingAlert,
      requests = requests
    )
  }

  private fun parseVolunteerActiveResponse(data: JsonElement?): ApiVolunteerActiveResponse {
    val root = data?.asObjectOrNull()
    val requests = extractVolunteerEndpointItems(data).mapNotNull { parseAssistanceRequest(it) }
    val counts = parseVolunteerCounts(
      root = root,
      fallbackActive = requests.size
    )
    val statusBanner = parseStatusBanner(root)
    return ApiVolunteerActiveResponse(
      counts = counts,
      statusBanner = statusBanner,
      requests = requests
    )
  }

  private fun parseVolunteerHistoryResponse(data: JsonElement?): ApiVolunteerHistoryResponse {
    val root = data?.asObjectOrNull()
    val requests = extractVolunteerEndpointItems(data).mapNotNull { parseAssistanceRequest(it) }
    val counts = parseVolunteerCounts(
      root = root,
      fallbackHistory = requests.size
    )
    val impact = parseVolunteerImpact(root)
    return ApiVolunteerHistoryResponse(
      counts = counts,
      impact = impact,
      requests = requests
    )
  }

  private fun parseVolunteerImpactResponse(data: JsonElement?): ApiVolunteerImpactResponse {
    val envelope = data?.asObjectOrNull()
    val root = envelope?.get("data")?.asObjectOrNull() ?: envelope
    return ApiVolunteerImpactResponse(
      counts = parseVolunteerCounts(root = root),
      impact = parseVolunteerImpact(root)
    )
  }

  private fun parseVolunteerAnalyticsEarningsResponse(data: JsonElement?): ApiVolunteerAnalyticsEarningsResponse? {
    val envelope = data?.asObjectOrNull() ?: return null
    val payload = envelope["data"]?.asObjectOrNull() ?: envelope
    val earningsObject = payload["earnings"]?.asObjectOrNull()
      ?: payload["analytics"]?.asObjectOrNull()
      ?: payload["summary"]?.asObjectOrNull()
      ?: payload["stats"]?.asObjectOrNull()
      ?: payload["totals"]?.asObjectOrNull()
      ?: payload["metrics"]?.asObjectOrNull()
      ?: payload

    val monthlyEarnings = earningsObject.readObjectList(
      "monthly_earnings",
      "monthlyEarnings",
      "earnings_by_month",
      "monthly_breakdown",
      "monthly",
      "monthly_stats",
      "monthlyStats",
      "months"
    ).ifEmpty {
      payload.readObjectList(
        "monthly_earnings",
        "monthlyEarnings",
        "earnings_by_month",
        "monthly_breakdown",
        "monthly",
        "monthly_stats",
        "monthlyStats",
        "months"
      )
    }.map { item ->
      ApiVolunteerAnalyticsMonthlyEarning(
        month = item.readString("month", "label", "name").orEmpty(),
        gross = item.readDouble("gross", "total_gross", "gross_amount", "amount") ?: 0.0,
        net = item.readDouble("net", "total_net", "net_amount", "amount_after_fee") ?: 0.0,
        fee = item.readDouble("fee", "fees", "total_fees", "platform_fee", "service_fee") ?: 0.0
      )
    }

    val withdrawalHistory = earningsObject.readObjectList(
      "withdrawal_history",
      "withdrawalHistory",
      "withdrawals",
      "payouts"
    ).ifEmpty {
      payload.readObjectList(
        "withdrawal_history",
        "withdrawalHistory",
        "withdrawals",
        "payouts"
      )
    }.map { item ->
      ApiVolunteerAnalyticsWithdrawalRecord(
        id = item.readString("id", "withdrawal_id", "payout_id").orEmpty(),
        date = item.readString("date", "created_at", "createdAt").orEmpty(),
        amount = item.readDouble("amount", "net", "value", "total") ?: 0.0,
        method = item.readString("method", "channel", "type").orEmpty(),
        status = item.readString("status", "state").orEmpty().ifBlank { "pending" }
      )
    }

    val paymentHistory = earningsObject.readObjectList(
      "payment_history",
      "paymentHistory",
      "payments",
      "records",
      "transactions"
    ).ifEmpty {
      payload.readObjectList(
        "payment_history",
        "paymentHistory",
        "payments",
        "records",
        "transactions"
      )
    }.map { item ->
      ApiVolunteerAnalyticsPaymentRecord(
        id = item.readString("id", "request_id", "payment_id").orEmpty(),
        date = item.readString("date", "created_at", "createdAt").orEmpty(),
        user = item.readString("user", "user_name", "customer_name", "name").orEmpty(),
        hours = item.readInt("hours", "duration_hours", "duration") ?: 0,
        gross = item.readDouble("gross", "gross_amount", "amount", "total") ?: 0.0,
        net = item.readDouble("net", "net_amount", "amount_after_fee", "take_home") ?: 0.0,
        status = item.readString("status", "payment_status", "state").orEmpty().ifBlank { "pending" }
      )
    }

    return ApiVolunteerAnalyticsEarningsResponse(
      availableBalance = earningsObject.readDouble(
        "available_balance",
        "availableBalance",
        "available",
        "available_earnings",
        "availableEarnings",
        "balance_available"
      ) ?: 0.0,
      pendingBalance = earningsObject.readDouble(
        "pending_balance",
        "pendingBalance",
        "pending",
        "pending_earnings",
        "pendingEarnings",
        "balance_pending"
      ) ?: 0.0,
      totalGross = earningsObject.readDouble(
        "total_gross",
        "totalGross",
        "gross_total",
        "gross",
        "gross_earnings",
        "total_earnings",
        "totalEarnings",
        "earnings_total"
      ) ?: 0.0,
      totalFees = earningsObject.readDouble(
        "total_fees",
        "totalFees",
        "fees_total",
        "fees",
        "platform_fees",
        "platformFees",
        "service_fees",
        "serviceFees"
      ) ?: 0.0,
      totalNet = earningsObject.readDouble(
        "total_net",
        "totalNet",
        "net_total",
        "net",
        "net_earnings",
        "take_home_total",
        "takeHomeTotal"
      ) ?: 0.0,
      thisWeekNet = earningsObject.readDouble("this_week_net", "thisWeekNet", "week_net", "weekly_net_earnings", "this_week_earnings") ?: 0.0,
      currentMonthLabel = earningsObject.readString("current_month_label", "currentMonthLabel", "month_label", "current_month").orEmpty(),
      currentMonthNet = earningsObject.readDouble("current_month_net", "currentMonthNet", "this_month_net", "thisMonthNet", "current_month_earnings", "this_month_earnings") ?: 0.0,
      lastMonthNet = earningsObject.readDouble("last_month_net", "lastMonthNet", "previous_month_net", "previousMonthNet", "last_month_earnings", "previous_month_earnings") ?: 0.0,
      monthlyChangePercent = earningsObject.readDouble("monthly_change_percent", "monthlyChangePercent", "monthly_change") ?: 0.0,
      monthlyEarnings = monthlyEarnings,
      withdrawalHistory = withdrawalHistory,
      paymentHistory = paymentHistory
    )
  }

  private fun parseVolunteerAnalyticsPerformanceResponse(data: JsonElement?): ApiVolunteerAnalyticsPerformanceResponse? {
    val envelope = data?.asObjectOrNull() ?: return null
    val payload = envelope["data"]?.asObjectOrNull() ?: envelope
    val performanceObject = payload["performance"]?.asObjectOrNull()
      ?: payload["analytics"]?.asObjectOrNull()
      ?: payload["summary"]?.asObjectOrNull()
      ?: payload["stats"]?.asObjectOrNull()
      ?: payload["metrics"]?.asObjectOrNull()
      ?: payload["totals"]?.asObjectOrNull()
      ?: payload

    val weeklyActivity = performanceObject.readObjectList(
      "weekly_activity",
      "weeklyActivity",
      "weekly",
      "activity",
      "activity_by_day",
      "activityByDay"
    ).ifEmpty {
      payload.readObjectList(
        "weekly_activity",
        "weeklyActivity",
        "weekly",
        "activity",
        "activity_by_day",
        "activityByDay"
      )
    }.map { item ->
      ApiVolunteerAnalyticsWeeklyActivity(
        day = item.readString("day", "label", "name").orEmpty(),
        completed = item.readInt("completed", "completed_requests", "count", "requests", "value")
          ?: (item.readDouble("completed", "completed_requests", "count", "requests", "value")?.toInt() ?: 0)
      )
    }

    val requestTypes = performanceObject.readObjectList(
      "request_types",
      "requestTypes",
      "request_type_shares",
      "types",
      "assistance_types",
      "assistanceTypes"
    ).ifEmpty {
      payload.readObjectList(
        "request_types",
        "requestTypes",
        "request_type_shares",
        "types",
        "assistance_types",
        "assistanceTypes"
      )
    }.map { item ->
      ApiVolunteerAnalyticsRequestTypeShare(
        name = item.readString("name", "type", "label", "assistance_type").orEmpty(),
        value = item.readInt("value", "count", "percentage", "share", "percent")
          ?: (item.readDouble("value", "count", "percentage", "share", "percent")?.toInt() ?: 0)
      )
    }

    val badges = when (val badgesElement = performanceObject["badges"]) {
      is JsonArray -> badgesElement.mapNotNull { entry ->
        when (entry) {
          is JsonPrimitive -> entry.contentOrNull?.trim()?.takeIf { it.isNotBlank() }
          is JsonObject -> entry.readString("name", "label", "title", "badge")
          else -> null
        }
      }
      else -> emptyList()
    }

    return ApiVolunteerAnalyticsPerformanceResponse(
      grade = performanceObject.readString("grade", "rank").orEmpty().ifBlank { "C" },
      headline = performanceObject.readString("headline", "summary", "title").orEmpty(),
      percentile = performanceObject.readInt("percentile", "rank_percentile")
        ?: (performanceObject.readDouble("percentile", "rank_percentile")?.toInt() ?: 50),
      responseRate = (performanceObject.readDouble("response_rate", "responseRate", "response") ?: 0.0).toFloat(),
      completionRate = (performanceObject.readDouble("completion_rate", "completionRate", "completion") ?: 0.0).toFloat(),
      averageRating = (performanceObject.readDouble("average_rating", "averageRating", "avg_rating", "avgRating") ?: 0.0).toFloat(),
      onTimeRate = (performanceObject.readDouble("on_time_rate", "onTimeRate", "on_time") ?: 0.0).toFloat(),
      completed = performanceObject.readInt("completed", "completed_count", "completed_requests", "completedRequests") ?: 0,
      pending = performanceObject.readInt("pending", "pending_count", "pending_requests", "pendingRequests") ?: 0,
      usersHelped = performanceObject.readInt("users_helped", "usersHelped", "helped_users") ?: 0,
      positiveReviews = performanceObject.readInt("positive_reviews", "positiveReviews") ?: 0,
      fiveStarRatings = performanceObject.readInt("five_star_ratings", "fiveStarRatings") ?: 0,
      totalReviews = performanceObject.readInt("total_reviews", "totalReviews", "reviews_count") ?: 0,
      badges = badges,
      weeklyActivity = weeklyActivity,
      requestTypes = requestTypes
    )
  }

  private fun parseVolunteerAnalyticsReviewsResponse(data: JsonElement?): ApiVolunteerAnalyticsReviewsResponse? {
    val envelope = data?.asObjectOrNull() ?: return null
    val root = envelope["data"]?.asObjectOrNull() ?: envelope
    val reviewsElement = root["reviews"]
    val reviewObjects = when (reviewsElement) {
      is JsonArray -> reviewsElement.mapNotNull { it.asObjectOrNull() }
      is JsonObject -> reviewsElement.readObjectList("reviews", "data", "items", "results")
      else -> emptyList()
    }.ifEmpty {
      root.readObjectList("reviews", "data", "items", "results")
    }

    val reviews = reviewObjects.map { item ->
      val issueElement = item["issues"] ?: item["flags"] ?: item["issue_flags"]
      val issues = when (issueElement) {
        is JsonArray -> issueElement.mapNotNull { entry ->
          (entry as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotBlank() }
        }
        is JsonPrimitive -> issueElement.contentOrNull
          ?.split(',')
          ?.map { part -> part.trim() }
          ?.filter { part -> part.isNotBlank() }
          ?: emptyList()
        else -> emptyList()
      }

      ApiVolunteerAnalyticsReview(
        id = item.readString("id", "review_id").orEmpty(),
        userName = item.readString("user_name", "userName", "author", "name").orEmpty(),
        rating = item.readInt("rating", "stars", "score")
          ?: (item.readDouble("rating", "stars", "score")?.toInt() ?: 0),
        comment = item.readString("comment", "review", "text").orEmpty(),
        date = item.readString("date", "created_at", "createdAt").orEmpty(),
        issues = issues
      )
    }

    val meta = root["meta"]?.asObjectOrNull()
      ?: root["pagination"]?.asObjectOrNull()
      ?: root

    val averageRating = meta.readDouble("average_rating", "averageRating", "avg_rating")
      ?: if (reviews.isEmpty()) 0.0 else reviews.map { it.rating }.average()

    return ApiVolunteerAnalyticsReviewsResponse(
      reviews = reviews,
      page = meta.readInt("page", "current_page", "currentPage") ?: 1,
      perPage = meta.readInt("per_page", "perPage", "page_size", "pageSize") ?: 10,
      total = meta.readInt("total", "total_count", "totalCount") ?: reviews.size,
      averageRating = averageRating
    )
  }

  private fun parseVolunteerCounts(
    root: JsonObject?,
    fallbackIncoming: Int = 0,
    fallbackActive: Int = 0,
    fallbackHistory: Int = 0
  ): ApiVolunteerCounts {
    val countObject = root?.get("counts")?.asObjectOrNull()
      ?: root?.get("badges")?.asObjectOrNull()
      ?: root?.get("tabs")?.asObjectOrNull()
      ?: root?.get("counters")?.asObjectOrNull()
      ?: root

    return ApiVolunteerCounts(
      incoming = (countObject.readInt(
        "incoming",
        "incoming_count",
        "incomingCount",
        "pending",
        "pending_count"
      ) ?: fallbackIncoming).coerceAtLeast(0),
      active = (countObject.readInt(
        "active",
        "active_count",
        "activeCount",
        "accepted",
        "accepted_count",
        "in_progress"
      ) ?: fallbackActive).coerceAtLeast(0),
      history = (countObject.readInt(
        "history",
        "history_count",
        "historyCount",
        "completed",
        "completed_count"
      ) ?: fallbackHistory).coerceAtLeast(0)
    )
  }

  private fun parseIncomingAlert(root: JsonObject?, fallbackCount: Int): ApiIncomingAlert {
    val alertObject = root?.get("incoming_alert")?.asObjectOrNull()
    val incomingAlertElement = root?.get("incoming_alert")
    val count = alertObject.readInt("count", "incoming_count", "incomingCount")
      ?: root.readInt("incoming_alert_count", "alert_count")
      ?: fallbackCount

    val elementMessage = (incomingAlertElement as? JsonPrimitive)?.contentOrNull
    val message = alertObject.readString("message", "text", "label")
      ?: root.readString("incoming_alert_message", "alert_message")
      ?: elementMessage
      ?: if (count > 0) {
        "$count ${if (count == 1) "person needs" else "people need"} your help nearby"
      } else {
        "No incoming requests nearby."
      }

    return ApiIncomingAlert(count = count.coerceAtLeast(0), message = message)
  }

  private fun parseStatusBanner(root: JsonObject?): String {
    val bannerElement = root?.get("status_banner")
    val objectMessage = bannerElement?.asObjectOrNull()
      .readString("message", "text", "title", "label")
    val primitiveMessage = (bannerElement as? JsonPrimitive)?.contentOrNull
    return objectMessage
      ?: primitiveMessage
      ?: root.readString("status_banner", "statusBanner")
      ?: "Assistance in Progress"
  }

  private fun parseVolunteerImpact(root: JsonObject?): ApiVolunteerImpact {
    val impactObject = root?.get("impact")?.asObjectOrNull()
      ?: root?.get("summary")?.asObjectOrNull()
      ?: root?.get("stats")?.asObjectOrNull()
      ?: root

    return ApiVolunteerImpact(
      totalAssists = impactObject.readInt(
        "total_assists",
        "totalAssists",
        "assists_total",
        "total_completed",
        "completed_requests",
        "completed_count"
      ) ?: 0,
      avgRating = (impactObject.readDouble(
        "avg_rating",
        "average_rating",
        "avgRating",
        "rating_avg",
        "rating"
      ) ?: 0.0).toFloat(),
      thisWeek = impactObject.readInt(
        "this_week",
        "thisWeek",
        "assists_this_week",
        "week_count",
        "weekly_assists"
      ) ?: 0
    )
  }

  private fun extractVolunteerEndpointItems(data: JsonElement?): List<JsonObject> {
    if (data == null) return emptyList()
    if (data is JsonArray) {
      return data.mapNotNull { it.asObjectOrNull() }
    }
    val root = data.asObjectOrNull() ?: return emptyList()

    val directItems = root.readObjectList(
      "requests",
      "help_requests",
      "volunteer_requests",
      "items",
      "results",
      "data"
    )
    if (directItems.isNotEmpty()) {
      return directItems
    }

    val nestedData = root["data"]?.asObjectOrNull()
    if (nestedData != null) {
      val nestedItems = nestedData.readObjectList(
        "requests",
        "help_requests",
        "volunteer_requests",
        "items",
        "results",
        "data"
      )
      if (nestedItems.isNotEmpty()) {
        return nestedItems
      }
    }

    return extractItems(data)
  }

  private fun extractItems(data: JsonElement?): List<JsonObject> {
    val element = data ?: return emptyList()
    return when (element) {
      is JsonArray -> element.mapNotNull { it.asObjectOrNull() }
      is JsonObject -> {
        val keyCandidates = listOf(
          "items",
          "results",
          "data",
          "sessions",
          "requests",
          "help_requests",
          "locations",
          "notifications"
        )
        keyCandidates.forEach { key ->
          val entry = element[key]
          val directValues = entry?.asArrayOrNull()?.mapNotNull { it.asObjectOrNull() }
          if (directValues != null) {
            return directValues
          }
          val nestedObject = entry?.asObjectOrNull()
          if (nestedObject != null) {
            keyCandidates.forEach { nestedKey ->
              val nestedValues = nestedObject[nestedKey]?.asArrayOrNull()?.mapNotNull { it.asObjectOrNull() }
              if (nestedValues != null) {
                return nestedValues
              }
            }
          }
        }
        if (element.readString("id") != null) listOf(element) else emptyList()
      }
      else -> emptyList()
    }
  }

  private fun JsonObject?.readObjectList(vararg keys: String): List<JsonObject> {
    if (this == null) return emptyList()
    keys.forEach { key ->
      val values = extractObjectListFromElement(this[key])
      if (values.isNotEmpty()) {
        return values
      }
    }
    return emptyList()
  }

  private suspend fun get(path: String, token: String? = null): ApiCallResult<JsonElement?> {
    return executeRequest {
      client.get(urlFor(path)) {
        if (!token.isNullOrBlank()) {
          bearerAuth(token)
        }
      }
    }
  }

  private suspend fun post(path: String, body: JsonElement?, token: String? = null): ApiCallResult<JsonElement?> {
    return executeRequest {
      client.post(urlFor(path)) {
        if (!token.isNullOrBlank()) {
          bearerAuth(token)
        }
        contentType(ContentType.Application.Json)
        if (body != null) {
          setBody(body)
        }
      }
    }
  }

  private suspend fun delete(path: String, token: String): ApiCallResult<JsonElement?> {
    return executeRequest {
      client.delete(urlFor(path)) {
        bearerAuth(token)
      }
    }
  }

  private suspend fun postMultipart(
    path: String,
    fields: Map<String, String>,
    filePart: UploadFilePart?,
    token: String? = null
  ): ApiCallResult<JsonElement?> {
    return executeRequest {
      client.post(urlFor(path)) {
        if (!token.isNullOrBlank()) {
          bearerAuth(token)
        }
        setBody(
          MultiPartFormDataContent(
            formData {
              fields.forEach { (key, value) ->
                append(key, value)
              }
              if (filePart != null) {
                append(
                  key = filePart.fieldName,
                  value = filePart.bytes,
                  headers = Headers.build {
                    append(HttpHeaders.ContentType, filePart.contentType)
                    append(
                      HttpHeaders.ContentDisposition,
                      "filename=\"${filePart.fileName}\""
                    )
                  }
                )
              }
            }
          )
        )
      }
    }
  }

  private suspend fun put(path: String, body: JsonObject, token: String): ApiCallResult<JsonElement?> {
    return executeRequest {
      client.put(urlFor(path)) {
        bearerAuth(token)
        contentType(ContentType.Application.Json)
        setBody(body)
      }
    }
  }

  private suspend inline fun executeRequest(
    crossinline block: suspend () -> HttpResponse
  ): ApiCallResult<JsonElement?> {
    return try {
      val response = block()
      parseResponse(response.status, response.bodyAsText())
    } catch (cancellation: CancellationException) {
      throw cancellation
    } catch (exception: Exception) {
      ApiCallResult.Failure(message = toNetworkErrorMessage(exception))
    }
  }

  private fun parseResponse(status: HttpStatusCode, rawBody: String): ApiCallResult<JsonElement?> {
    val parsed = runCatching { json.parseToJsonElement(rawBody) }.getOrNull()
    val validationErrors = extractValidationErrors(parsed?.asObjectOrNull()?.get("errors"))
    val validationIssue = validationErrors.entries.firstOrNull()?.let { ValidationIssue(it.key, it.value) }

    if (status.value in 200..299) {
      val payload = parsed?.asObjectOrNull()
      if (payload != null && payload.containsKey("success")) {
        val isSuccess = payload["success"]?.jsonPrimitive?.booleanOrNull == true
        if (!isSuccess) {
          return ApiCallResult.Failure(
            message = extractMessage(payload, rawBody, status.value, validationIssue),
            statusCode = status.value,
            validationField = validationIssue?.field,
            validationErrors = validationErrors
          )
        }
        val normalizedPayload = payload["data"]?.takeUnless { it is JsonNull }
          ?: payload["result"]?.takeUnless { it is JsonNull }
          ?: payload["payload"]?.takeUnless { it is JsonNull }
          ?: payload
        return ApiCallResult.Success(normalizedPayload)
      }
      return ApiCallResult.Success(parsed)
    }

    return ApiCallResult.Failure(
      message = extractMessage(parsed?.asObjectOrNull(), rawBody, status.value, validationIssue),
      statusCode = status.value,
      validationField = validationIssue?.field,
      validationErrors = validationErrors
    )
  }

  private fun extractMessage(
    payload: JsonObject?,
    rawBody: String,
    statusCode: Int,
    validationIssue: ValidationIssue?
  ): String {
    if (payload == null) {
      return rawBody.ifBlank { "Request failed with status $statusCode." }
    }

    val message = payload.readString("message")?.takeIf { it.isNotBlank() }
    val firstError = validationIssue?.message

    return when {
      !firstError.isNullOrBlank() && message != null -> "$message $firstError"
      !firstError.isNullOrBlank() -> firstError
      !message.isNullOrBlank() -> message
      else -> "Request failed with status $statusCode."
    }
  }

  private fun extractValidationErrors(errors: JsonElement?): Map<String, String> {
    val obj = errors?.asObjectOrNull() ?: return emptyMap()
    val extracted = linkedMapOf<String, String>()
    obj.entries.forEach { (field, value) ->
      when (value) {
        is JsonPrimitive -> {
          val message = value.contentOrNull?.takeIf { it.isNotBlank() }
          if (message != null) {
            extracted[field] = message
          }
        }
        is JsonArray -> {
          val first = value.firstOrNull()?.jsonPrimitive?.contentOrNull
          if (!first.isNullOrBlank()) {
            extracted[field] = first
          }
        }
        else -> Unit
      }
    }
    return extracted
  }

  private data class ValidationIssue(
    val field: String,
    val message: String
  )

  private fun urlFor(path: String): String {
    val normalizedPath = if (path.startsWith('/')) path else "/$path"
    val prefixedPath = if (baseUrl.endsWith("/api") || normalizedPath.startsWith("/admin")) {
      normalizedPath
    } else {
      "/api$normalizedPath"
    }
    return "$baseUrl$prefixedPath"
  }

  private fun encodeQuery(value: String): String {
    return URLEncoder.encode(value, Charsets.UTF_8.name())
  }

  private fun normalizeExpiryEpochSeconds(rawValue: Long): Long {
    if (rawValue <= 0L || rawValue == Long.MAX_VALUE) return Long.MAX_VALUE
    return when {
      rawValue > 10_000_000_000L -> rawValue / 1000L
      rawValue < 1_500_000_000L -> Instant.now().epochSecond + rawValue
      else -> rawValue
    }
  }

  private fun readIdDocumentPart(documentUri: String?): UploadFilePart? {
    val context = appContext ?: return null
    val rawUri = documentUri?.trim().orEmpty()
    if (rawUri.isBlank()) return null
    val uri = runCatching { Uri.parse(rawUri) }.getOrNull() ?: return null
    val resolver = context.contentResolver
    val bytes = runCatching {
      resolver.openInputStream(uri)?.use { input -> input.readBytes() }
    }.getOrNull() ?: return null
    if (bytes.isEmpty()) return null

    val mimeType = resolver.getType(uri)?.takeIf { it.isNotBlank() }
      ?: "application/octet-stream"
    val fileName = resolveDocumentFileName(context, uri)

    return UploadFilePart(
      fieldName = "id_document",
      fileName = fileName,
      contentType = mimeType,
      bytes = bytes
    )
  }

  private fun readProfilePhotoPart(photoUri: String?): UploadFilePart? {
    val rawPhoto = photoUri?.trim().orEmpty()
    if (rawPhoto.isBlank()) return null

    val parsedUri = runCatching { Uri.parse(rawPhoto) }.getOrNull()
    if (parsedUri?.scheme.equals("content", ignoreCase = true)) {
      val contentUri = parsedUri ?: return null
      val context = appContext ?: return null
      val resolver = context.contentResolver
      val bytes = runCatching {
        resolver.openInputStream(contentUri)?.use { input -> input.readBytes() }
      }.getOrNull() ?: return null
      if (bytes.isEmpty()) return null

      val fileName = resolveDocumentFileName(context, contentUri).ifBlank { "profile_photo.jpg" }
      val mimeType = resolver.getType(contentUri)?.takeIf { it.isNotBlank() }
        ?: resolveMimeType(fileName)

      return UploadFilePart(
        fieldName = "photo",
        fileName = fileName,
        contentType = mimeType,
        bytes = bytes
      )
    }

    val photoFile = when {
      parsedUri?.scheme.equals("file", ignoreCase = true) -> parsedUri?.path?.let(::File)
      else -> File(rawPhoto)
    }?.takeIf { it.exists() && it.isFile } ?: return null

    val bytes = runCatching { photoFile.readBytes() }.getOrNull() ?: return null
    if (bytes.isEmpty()) return null

    return UploadFilePart(
      fieldName = "photo",
      fileName = photoFile.name.ifBlank { "profile_photo.jpg" },
      contentType = resolveMimeType(photoFile.name),
      bytes = bytes
    )
  }

  private fun logApiPayload(label: String, payload: JsonElement?) {
    if (!BuildConfig.DEBUG) return
    val line = "$label => ${payload?.toString().orEmpty()}"
    Log.d("AtharApiDebug", line)
    runCatching {
      appContext?.openFileOutput("api_debug_payloads.txt", Context.MODE_APPEND)?.bufferedWriter()?.use { writer ->
        writer.appendLine(line)
      }
    }
  }

  private fun resolveDocumentFileName(context: Context, uri: Uri): String {
    val fallback = uri.lastPathSegment?.substringAfterLast('/') ?: "id_document"
    return runCatching {
      context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null
      )?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) {
          cursor.getString(index)
        } else {
          null
        }
      }
    }.getOrNull()
      ?.takeIf { it.isNotBlank() }
      ?: fallback
  }

  private fun resolveMimeType(fileName: String): String {
    val extension = fileName.substringAfterLast('.', "").lowercase(Locale.getDefault())
    return MimeTypeMap.getSingleton()
      .getMimeTypeFromExtension(extension)
      ?.takeIf { it.isNotBlank() }
      ?: "application/octet-stream"
  }

  private fun resolveProfilePhotoPath(rawPath: String?): String? {
    val normalized = rawPath?.trim().orEmpty()
    if (normalized.isBlank()) return null
    if (
      normalized.startsWith("http://", ignoreCase = true) ||
      normalized.startsWith("https://", ignoreCase = true) ||
      normalized.startsWith("content://", ignoreCase = true) ||
      normalized.startsWith("file://", ignoreCase = true) ||
      Regex("^[A-Za-z]:[\\\\/].*").matches(normalized)
    ) {
      return normalized
    }

    val base = baseUrl.removeSuffix("/api")
    return if (normalized.startsWith('/')) {
      "$base$normalized"
    } else {
      "$base/${normalized.trimStart('/')}"
    }
  }

  private fun parseMemberSince(rawCreatedAt: String?): String {
    if (rawCreatedAt.isNullOrBlank()) return ""
    val month = runCatching { YearMonth.from(OffsetDateTime.parse(rawCreatedAt)) }
      .getOrNull()
      ?: return ""
    return month.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault()))
  }

  private fun formatRelativeTime(rawTimestamp: String?): String {
    if (rawTimestamp.isNullOrBlank()) return ""
    val instant = runCatching { OffsetDateTime.parse(rawTimestamp).toInstant() }
      .getOrElse { runCatching { Instant.parse(rawTimestamp) }.getOrNull() }
      ?: return rawTimestamp

    val seconds = Duration.between(instant, Instant.now()).seconds.coerceAtLeast(0)
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
      minutes < 1 -> "Just now"
      minutes < 60 -> "${minutes}m ago"
      hours < 24 -> "${hours}h ago"
      else -> "${days}d ago"
    }
  }

  private fun toNetworkErrorMessage(exception: Exception): String {
    val name = exception.javaClass.simpleName
    return when {
      name.contains("ConnectException", ignoreCase = true) ->
        "Cannot reach the server. Check BACKEND_BASE_URL and internet connection."
      name.contains("UnresolvedAddressException", ignoreCase = true) ||
        name.contains("UnknownHostException", ignoreCase = true) ->
        "Server address is invalid. Check BACKEND_BASE_URL."
      name.contains("SocketTimeoutException", ignoreCase = true) ->
        "Server timed out. Please try again."
      name.contains("SSL", ignoreCase = true) ->
        "Secure connection failed. Please try again."
      else -> "Network error. Please try again."
    }
  }
}

private fun JsonElement.asObjectOrNull(): JsonObject? = this as? JsonObject
private fun JsonElement.asArrayOrNull(): JsonArray? = this as? JsonArray

private fun JsonObject?.readString(vararg keys: String): String? {
  if (this == null) return null
  keys.forEach { key ->
    val element = this[key] ?: return@forEach
    if (element is JsonNull) return@forEach
    val primitive = element as? JsonPrimitive ?: return@forEach
    val value = primitive.contentOrNull ?: primitive.content
    if (value.isNotBlank()) {
      return value
    }
  }
  return null
}

private fun JsonObject?.readInt(vararg keys: String): Int? {
  if (this == null) return null
  keys.forEach { key ->
    parseNumberFromElement(this[key])?.toInt()?.let { return it }
  }
  return null
}

private fun JsonObject?.readLong(vararg keys: String): Long? {
  if (this == null) return null
  keys.forEach { key ->
    parseNumberFromElement(this[key])?.toLong()?.let { return it }
  }
  return null
}

private fun JsonObject?.readEpochSeconds(vararg keys: String): Long? {
  if (this == null) return null
  keys.forEach { key ->
    parseEpochSecondsFromElement(this[key])?.let { return it }
  }
  return null
}

private fun JsonObject?.readDouble(vararg keys: String): Double? {
  if (this == null) return null
  keys.forEach { key ->
    parseNumberFromElement(this[key])?.let { return it }
  }
  return null
}

private fun extractObjectListFromElement(element: JsonElement?): List<JsonObject> {
  return when (element) {
    is JsonArray -> element.mapNotNull { it.asObjectOrNull() }
    is JsonObject -> {
      val nestedKeys = listOf(
        "data",
        "items",
        "results",
        "records",
        "list",
        "entries",
        "history",
        "rows"
      )
      nestedKeys.forEach { key ->
        val nested = extractObjectListFromElement(element[key])
        if (nested.isNotEmpty()) {
          return nested
        }
      }
      if (element.readString("id") != null) listOf(element) else emptyList()
    }
    else -> emptyList()
  }
}

private fun parseNumberFromElement(element: JsonElement?): Double? {
  return when (element) {
    null, JsonNull -> null
    is JsonPrimitive -> {
      element.doubleOrNull
        ?: element.longOrNull?.toDouble()
        ?: element.intOrNull?.toDouble()
        ?: parseLooseNumber(element.contentOrNull)
    }
    is JsonObject -> {
      val preferredKeys = listOf(
        "value",
        "amount",
        "count",
        "total",
        "net",
        "gross",
        "fee",
        "hours",
        "number",
        "percent",
        "percentage"
      )
      preferredKeys.forEach { key ->
        parseNumberFromElement(element[key])?.let { return it }
      }
      element.values.forEach { value ->
        parseNumberFromElement(value)?.let { return it }
      }
      null
    }
    is JsonArray -> {
      element.forEach { value ->
        parseNumberFromElement(value)?.let { return it }
      }
      null
    }
  }
}

private fun parseEpochSecondsFromElement(element: JsonElement?): Long? {
  return when (element) {
    null, JsonNull -> null
    is JsonPrimitive -> {
      element.longOrNull?.let { return normalizeEpochSeconds(it) }
      element.intOrNull?.toLong()?.let { return normalizeEpochSeconds(it) }
      element.doubleOrNull?.toLong()?.let { return normalizeEpochSeconds(it) }
      parseEpochSeconds(element.contentOrNull)
    }
    is JsonObject -> {
      val preferredKeys = listOf(
        "epoch",
        "epoch_seconds",
        "epochSeconds",
        "timestamp",
        "time",
        "date",
        "created_at",
        "createdAt",
        "updated_at",
        "updatedAt",
        "value"
      )
      preferredKeys.forEach { key ->
        parseEpochSecondsFromElement(element[key])?.let { return it }
      }
      element.values.forEach { value ->
        parseEpochSecondsFromElement(value)?.let { return it }
      }
      null
    }
    is JsonArray -> {
      element.forEach { value ->
        parseEpochSecondsFromElement(value)?.let { return it }
      }
      null
    }
  }
}

private fun parseLooseNumber(raw: String?): Double? {
  val text = raw?.trim().orEmpty()
  if (text.isBlank()) return null

  val cleaned = text
    .replace(",", "")
    .replace("%", "")
    .replace("$", "")
    .replace("£", "")
    .replace("€", "")
    .replace("ج.م", "", ignoreCase = true)
    .replace("EGP", "", ignoreCase = true)
    .replace("USD", "", ignoreCase = true)
    .replace("SAR", "", ignoreCase = true)
    .replace("AED", "", ignoreCase = true)
    .trim()

  if (cleaned.isBlank()) return null
  return cleaned.toDoubleOrNull()
}

private fun JsonObject?.readBoolean(vararg keys: String): Boolean? {
  if (this == null) return null
  keys.forEach { key ->
    val element = this[key] ?: return@forEach
    val primitive = element as? JsonPrimitive ?: return@forEach
    primitive.booleanOrNull?.let { return it }
    primitive.contentOrNull?.toBooleanStrictOrNull()?.let { return it }
  }
  return null
}

private fun parseEpochSeconds(rawValue: String?): Long? {
  if (rawValue.isNullOrBlank()) return null
  rawValue.toLongOrNull()?.let { return normalizeEpochSeconds(it) }
  rawValue.toDoubleOrNull()?.let { return normalizeEpochSeconds(it.toLong()) }
  val instant = runCatching { OffsetDateTime.parse(rawValue).toInstant() }
    .getOrElse { runCatching { Instant.parse(rawValue) }.getOrNull() }
    ?: return null
  return instant.epochSecond
}

private fun normalizeEpochSeconds(rawValue: Long): Long {
  return if (rawValue > 10_000_000_000L) rawValue / 1000L else rawValue
}

fun ApiAuthResponse.toAuthSession(): AuthSession {
  val role = if (user.role == ApiUserRole.Volunteer) UserRole.Volunteer else UserRole.User
  return AuthSession(
    userId = user.id,
    role = role,
    fullName = user.fullName,
    email = user.email,
    phone = user.phone,
    disabilityType = user.disabilityType,
    volunteerLive = user.volunteerLive
  )
}

fun ApiLocation.toDomainLocation(): Location {
  return Location(
    id = id,
    name = name,
    category = category,
    lat = lat,
    lng = lng,
    rating = rating,
    totalRatings = totalRatings,
    features = LocationFeatures(
      ramp = features.ramp,
      elevator = features.elevator,
      accessibleToilet = features.accessibleToilet,
      accessibleParking = features.accessibleParking,
      wideEntrance = features.wideEntrance,
      brailleSignage = features.brailleSignage
    ),
    recentReports = recentReports,
    distance = distance
  )
}

fun ApiVolunteerRequest.toDomainVolunteerRequest(): VolunteerRequest {
  val normalizedMoney = normalizeDomainRequestMoney(
    hours = hours,
    pricePerHour = pricePerHour,
    totalAmountEgp = totalAmountEgp
  )
  return VolunteerRequest(
    id = id,
    userId = userId,
    userName = userName,
    userType = userType,
    location = location,
    requestTime = requestTime,
    status = normalizeVolunteerRequestStatus(status),
    volunteerName = volunteerName,
    description = description,
    hours = hours,
    pricePerHour = normalizedMoney.pricePerHour,
    totalAmountEgp = normalizedMoney.totalAmountEgp,
    paymentMethod = paymentMethod,
    paymentStatus = paymentStatus,
    isPaid = isPaid
  )
}

fun ApiAssistanceRequest.toDomainAssistanceRequest(): AssistanceRequest {
  val normalizedMoney = normalizeDomainRequestMoney(
    hours = hours,
    pricePerHour = pricePerHour,
    totalAmountEgp = totalAmountEgp
  )
  return AssistanceRequest(
    id = id,
    userName = userName,
    userType = userType,
    location = location,
    destination = destination,
    distance = distance,
    urgency = urgency,
    helpType = helpType,
    requestTime = requestTime,
    status = when (status.lowercase()) {
      "created", "broadcasted", "pending" -> RequestStatus.Broadcasted
      "accepted", "pending_payment" -> RequestStatus.Accepted
      "active", "confirmed", "inprogress", "in_progress" -> RequestStatus.InProgress
      "completed" -> RequestStatus.Completed
      "rated" -> RequestStatus.Rated
      "archived" -> RequestStatus.Archived
      "cancelled" -> RequestStatus.Cancelled
      "novolunteer", "no_volunteer" -> RequestStatus.NoVolunteer
      else -> RequestStatus.Broadcasted
    },
    hours = hours,
    pricePerHour = normalizedMoney.pricePerHour,
    totalAmountEgp = normalizedMoney.totalAmountEgp,
    paymentMethod = paymentMethod,
    paymentStatus = paymentStatus,
    isPaid = isPaid
  )
}

private data class NormalizedDomainRequestMoney(
  val pricePerHour: Int,
  val totalAmountEgp: Int?
)

private fun normalizeDomainRequestMoney(
  hours: Int,
  pricePerHour: Int,
  totalAmountEgp: Int?
): NormalizedDomainRequestMoney {
  // Valid EGP range: pricePerHour 50-200, max total 200*8=1600.
  // Values above ceiling that are divisible by 100 are piasters.
  val normalizedPricePerHour = when {
    pricePerHour > 200 && pricePerHour % 100 == 0 -> (pricePerHour / 100).coerceAtLeast(1)
    else -> pricePerHour.coerceAtLeast(1)
  }
  val normalizedTotalAmount = totalAmountEgp?.let { total ->
    when {
      total > 1600 && total % 100 == 0 -> (total / 100).coerceAtLeast(1)
      else -> total.coerceAtLeast(1)
    }
  }
  return NormalizedDomainRequestMoney(
    pricePerHour = normalizedPricePerHour,
    totalAmountEgp = normalizedTotalAmount
  )
}

fun ApiVolunteerCounts.toDomainCounts(): VolunteerDashboardCounts {
  return VolunteerDashboardCounts(
    incoming = incoming,
    active = active,
    history = history
  )
}

fun ApiIncomingAlert.toDomainIncomingAlert(): VolunteerIncomingAlert {
  return VolunteerIncomingAlert(
    count = count,
    message = message
  )
}

fun ApiVolunteerImpact.toDomainVolunteerImpact(): VolunteerImpact {
  return VolunteerImpact(
    totalAssists = totalAssists,
    avgRating = avgRating,
    thisWeek = thisWeek
  )
}

fun ApiVolunteerAnalyticsEarningsResponse.toDomainVolunteerAnalyticsEarnings(): VolunteerAnalyticsEarnings {
  return VolunteerAnalyticsEarnings(
    availableBalance = availableBalance,
    pendingBalance = pendingBalance,
    totalGross = totalGross,
    totalFees = totalFees,
    totalNet = totalNet,
    thisWeekNet = thisWeekNet,
    currentMonthLabel = currentMonthLabel,
    currentMonthNet = currentMonthNet,
    lastMonthNet = lastMonthNet,
    monthlyChangePercent = monthlyChangePercent,
    monthlyEarnings = monthlyEarnings.map { entry ->
      VolunteerAnalyticsMonthlyEarning(
        month = entry.month,
        gross = entry.gross,
        net = entry.net,
        fee = entry.fee
      )
    },
    withdrawalHistory = withdrawalHistory.map { entry ->
      VolunteerAnalyticsWithdrawalRecord(
        id = entry.id,
        date = entry.date,
        amount = entry.amount,
        method = entry.method,
        status = if (entry.status.equals("completed", ignoreCase = true)) {
          AnalyticsRecordStatus.Completed
        } else {
          AnalyticsRecordStatus.Pending
        }
      )
    },
    paymentHistory = paymentHistory.map { entry ->
      VolunteerAnalyticsPaymentRecord(
        id = entry.id,
        date = entry.date,
        user = entry.user,
        hours = entry.hours,
        gross = entry.gross,
        net = entry.net,
        status = if (entry.status.equals("completed", ignoreCase = true)) {
          AnalyticsRecordStatus.Completed
        } else {
          AnalyticsRecordStatus.Pending
        }
      )
    }
  )
}

fun ApiVolunteerAnalyticsPerformanceResponse.toDomainVolunteerAnalyticsPerformance(): VolunteerAnalyticsPerformance {
  return VolunteerAnalyticsPerformance(
    grade = grade,
    headline = headline,
    percentile = percentile,
    responseRate = responseRate,
    completionRate = completionRate,
    averageRating = averageRating,
    onTimeRate = onTimeRate,
    completed = completed,
    pending = pending,
    usersHelped = usersHelped,
    positiveReviews = positiveReviews,
    fiveStarRatings = fiveStarRatings,
    totalReviews = totalReviews,
    badges = badges,
    weeklyActivity = weeklyActivity.map { entry ->
      VolunteerAnalyticsWeeklyActivity(
        day = entry.effectiveDay,
        completed = entry.effectiveCompleted
      )
    },
    requestTypes = requestTypes.map { entry ->
      VolunteerAnalyticsRequestTypeShare(
        name = entry.name,
        value = entry.value
      )
    }
  )
}

fun ApiVolunteerAnalyticsReviewsResponse.toDomainVolunteerAnalyticsReviews(): VolunteerAnalyticsReviews {
  return VolunteerAnalyticsReviews(
    reviews = reviews.map { entry ->
      VolunteerAnalyticsReview(
        id = entry.id,
        userName = entry.userName,
        rating = entry.rating,
        comment = entry.comment,
        date = entry.date,
        issues = entry.issues
      )
    },
    page = page,
    perPage = perPage,
    total = total,
    averageRating = averageRating
  )
}

private fun normalizeVolunteerRequestStatus(raw: String): String {
  return when (raw.lowercase()) {
    "created", "broadcasted", "pending" -> "pending"
    "active", "accepted", "inprogress", "in_progress" -> "accepted"
    "completed", "rated", "archived" -> "completed"
    "cancelled", "novolunteer", "no_volunteer" -> "cancelled"
    else -> raw.lowercase()
  }
}

fun ApiAuthUser.toDomainUserProfile(): UserProfile {
  return UserProfile(
    name = fullName,
    email = email,
    phone = phone,
    disabilityType = disabilityType.orEmpty(),
    memberSince = memberSince,
    contributionStats = ContributionStats(
      ratingsSubmitted = contributionStats.ratingsSubmitted,
      reportsSubmitted = contributionStats.reportsSubmitted,
      helpfulVotes = contributionStats.helpfulVotes
    )
  )
}

fun ApiAuthUser.toDomainVolunteerAnalyticsVolunteerSummary(): VolunteerAnalyticsVolunteerSummary {
  val status = volunteerStatus?.trim()?.lowercase(Locale.getDefault())
  val approvalStatus = when {
    role != ApiUserRole.Volunteer -> VolunteerApprovalStatus.Unknown
    !isActive -> VolunteerApprovalStatus.Inactive
    !roleVerifiedAt.isNullOrBlank() || status == "approved" || status == "active" ->
      VolunteerApprovalStatus.Approved
    status == "pending" || status == "pending_approval" || status == "awaiting_approval" ->
      VolunteerApprovalStatus.PendingApproval
    status == "rejected" || status == "denied" || status == "declined" ->
      VolunteerApprovalStatus.Rejected
    else -> VolunteerApprovalStatus.Unknown
  }

  return VolunteerAnalyticsVolunteerSummary(
    id = id,
    fullName = fullName,
    email = email,
    phone = phone,
    location = location,
    memberSince = memberSince,
    volunteerLive = volunteerLive,
    volunteerStatus = volunteerStatus,
    roleVerifiedAt = roleVerifiedAt,
    approvalStatus = approvalStatus,
    isActive = isActive
  )
}
