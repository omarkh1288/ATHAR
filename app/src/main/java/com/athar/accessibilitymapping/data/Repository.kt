package com.athar.accessibilitymapping.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AtharRepository(
  context: Context,
  private val api: BackendApiClient = BackendApiClient(),
  private val sessionStore: AuthSessionStore = AuthSessionStore(context.applicationContext),
  private val localRequestStore: LocalRequestStore = LocalRequestStore(context.applicationContext),
  private val appPreferences: AppPreferencesStore = AppPreferencesStore(context.applicationContext)
) {
  suspend fun getLocations(): List<Location> = withContext(Dispatchers.IO) {
    when (val result = api.getLocations()) {
      is ApiCallResult.Success -> result.data.map { it.toDomainLocation() }
      is ApiCallResult.Failure -> mockLocations
    }
  }

  suspend fun searchLocations(query: String): List<Location> = withContext(Dispatchers.IO) {
    when (val result = api.searchLocations(query)) {
      is ApiCallResult.Success -> result.data.results.map { it.toDomainLocation() }
      is ApiCallResult.Failure -> {
        getLocations().filter { location ->
          location.name.contains(query, ignoreCase = true) ||
            location.category.contains(query, ignoreCase = true)
        }
      }
    }
  }

  suspend fun getRequests(): List<VolunteerRequest> = withContext(Dispatchers.IO) {
    when (val result = callAuthorized { token -> api.getMyRequests(token) }) {
      is ApiCallResult.Success -> result.data.userRequests.map { it.toDomainVolunteerRequest() }
      is ApiCallResult.Failure -> {
        if (result.statusCode == null && canUseLocalRequestFallback()) {
          val session = sessionStore.readAuthSession()
          if (session?.role == UserRole.User) {
            return@withContext localRequestStore.getUserRequests(session.userId)
              .map { it.toDomainVolunteerRequest() }
          }
        }
        emptyList()
      }
    }
  }

  suspend fun getVolunteerIncomingDashboard(
    lat: Double? = null,
    lng: Double? = null,
    perPage: Int = 50
  ): VolunteerIncomingDashboard = withContext(Dispatchers.IO) {
    when (
      val result = callAuthorized { token ->
        api.getVolunteerIncoming(token, lat = lat, lng = lng, perPage = perPage)
      }
    ) {
      is ApiCallResult.Success -> {
        VolunteerIncomingDashboard(
          counts = result.data.counts.toDomainCounts(),
          incomingAlert = result.data.incomingAlert.toDomainIncomingAlert(),
          requests = result.data.requests.map { it.toDomainAssistanceRequest() }
        )
      }
      is ApiCallResult.Failure -> {
        val legacyFallback = getLegacyIncomingDashboard(perPage)
        if (legacyFallback != null) {
          return@withContext legacyFallback
        }
        if (result.statusCode == null && canUseLocalRequestFallback()) {
          val session = sessionStore.readAuthSession()
          if (session?.role == UserRole.Volunteer) {
            val requests = localRequestStore.getIncomingRequests(session.userId)
              .map { it.toDomainAssistanceRequest() }
            val count = requests.size
            return@withContext VolunteerIncomingDashboard(
              counts = VolunteerDashboardCounts(incoming = count, active = 0, history = 0),
              incomingAlert = VolunteerIncomingAlert(
                count = count,
                message = incomingAlertMessage(count)
              ),
              requests = requests
            )
          }
        }
        VolunteerIncomingDashboard()
      }
    }
  }

  suspend fun getVolunteerActiveDashboard(perPage: Int = 50): VolunteerActiveDashboard = withContext(Dispatchers.IO) {
    when (val result = callAuthorized { token -> api.getVolunteerActive(token, perPage = perPage) }) {
      is ApiCallResult.Success -> {
        VolunteerActiveDashboard(
          counts = result.data.counts.toDomainCounts(),
          statusBanner = result.data.statusBanner,
          requests = result.data.requests.map { it.toDomainAssistanceRequest() }
        )
      }
      is ApiCallResult.Failure -> {
        val legacyRequests = getLegacyVolunteerRequests()
        if (legacyRequests != null) {
          val activeRequests = legacyRequests.filter { request ->
            request.status == RequestStatus.Accepted || request.status == RequestStatus.InProgress
          }
          val historyCount = legacyRequests.count { request ->
            request.status == RequestStatus.Completed ||
              request.status == RequestStatus.Rated ||
              request.status == RequestStatus.Archived ||
              request.status == RequestStatus.Cancelled ||
              request.status == RequestStatus.NoVolunteer
          }
          return@withContext VolunteerActiveDashboard(
            counts = VolunteerDashboardCounts(
              incoming = 0,
              active = activeRequests.size,
              history = historyCount
            ),
            statusBanner = "Assistance in Progress",
            requests = activeRequests
          )
        }
        VolunteerActiveDashboard()
      }
    }
  }

  suspend fun getVolunteerHistoryDashboard(perPage: Int = 50): VolunteerHistoryDashboard = withContext(Dispatchers.IO) {
    when (val result = callAuthorized { token -> api.getVolunteerHistory(token, perPage = perPage) }) {
      is ApiCallResult.Success -> {
        VolunteerHistoryDashboard(
          counts = result.data.counts.toDomainCounts(),
          impact = result.data.impact.toDomainVolunteerImpact(),
          requests = result.data.requests.map { it.toDomainAssistanceRequest() }
        )
      }
      is ApiCallResult.Failure -> {
        val legacyRequests = getLegacyVolunteerRequests()
        if (legacyRequests != null) {
          val historyRequests = legacyRequests.filter { request ->
            request.status == RequestStatus.Completed ||
              request.status == RequestStatus.Rated ||
              request.status == RequestStatus.Archived ||
              request.status == RequestStatus.Cancelled ||
              request.status == RequestStatus.NoVolunteer
          }
          val completedCount = historyRequests.count { request ->
            request.status == RequestStatus.Completed ||
              request.status == RequestStatus.Rated ||
              request.status == RequestStatus.Archived
          }
          return@withContext VolunteerHistoryDashboard(
            counts = VolunteerDashboardCounts(
              incoming = 0,
              active = legacyRequests.count { it.status == RequestStatus.Accepted || it.status == RequestStatus.InProgress },
              history = historyRequests.size
            ),
            impact = VolunteerImpact(
              totalAssists = completedCount,
              avgRating = 0f,
              thisWeek = completedCount
            ),
            requests = historyRequests
          )
        }
        VolunteerHistoryDashboard()
      }
    }
  }

  suspend fun getVolunteerImpactDashboard(): VolunteerImpactDashboard = withContext(Dispatchers.IO) {
    when (val result = callAuthorized { token -> api.getVolunteerImpact(token) }) {
      is ApiCallResult.Success -> {
        VolunteerImpactDashboard(
          counts = result.data.counts.toDomainCounts(),
          impact = result.data.impact.toDomainVolunteerImpact()
        )
      }
      is ApiCallResult.Failure -> {
        val historyFallback = getVolunteerHistoryDashboard(perPage = 1)
        VolunteerImpactDashboard(
          counts = historyFallback.counts,
          impact = historyFallback.impact
        )
      }
    }
  }

  suspend fun getVolunteerAnalyticsEarnings(): VolunteerAnalyticsEarnings = withContext(Dispatchers.IO) {
    when (val result = callAuthorized { token -> api.getVolunteerAnalyticsEarnings(token) }) {
      is ApiCallResult.Success -> result.data.toDomainVolunteerAnalyticsEarnings()
      is ApiCallResult.Failure -> VolunteerAnalyticsEarnings()
    }
  }

  suspend fun getVolunteerAnalyticsPerformance(): VolunteerAnalyticsPerformance = withContext(Dispatchers.IO) {
    when (val result = callAuthorized { token -> api.getVolunteerAnalyticsPerformance(token) }) {
      is ApiCallResult.Success -> result.data.toDomainVolunteerAnalyticsPerformance()
      is ApiCallResult.Failure -> VolunteerAnalyticsPerformance()
    }
  }

  suspend fun getVolunteerAnalyticsReviews(
    page: Int = 1,
    perPage: Int = 100,
    rating: Int? = null
  ): VolunteerAnalyticsReviews = withContext(Dispatchers.IO) {
    when (val result = callAuthorized { token -> api.getVolunteerAnalyticsReviews(token, page = page, perPage = perPage, rating = rating) }) {
      is ApiCallResult.Success -> result.data.toDomainVolunteerAnalyticsReviews()
      is ApiCallResult.Failure -> VolunteerAnalyticsReviews()
    }
  }

  suspend fun getVolunteerAnalyticsSnapshot(
    page: Int = 1,
    perPage: Int = 100,
    rating: Int? = null
  ): VolunteerAnalyticsSnapshot = withContext(Dispatchers.IO) {
    val warnings = linkedSetOf<String>()
    val isLocalSession = canUseLocalRequestFallback()

    if (isLocalSession) {
      warnings += "Analytics require a live backend session. Sign in with the online volunteer account."
    }

    val accountResult = getCurrentAccount()
    val volunteer = when (accountResult) {
      is ApiCallResult.Success -> accountResult.data.toDomainVolunteerAnalyticsVolunteerSummary()
      is ApiCallResult.Failure -> {
        warnings += accountResult.message
        VolunteerAnalyticsVolunteerSummary()
      }
    }

    val account = (accountResult as? ApiCallResult.Success)?.data
    val role = account?.role
    val volunteerStatus = account?.volunteerStatus?.trim()?.lowercase()

    if (role != null && role != ApiUserRole.Volunteer) {
      warnings += "Analytics are available only for volunteer accounts."
    }

    when {
      account != null && !account.isActive -> warnings += "Volunteer account is inactive."
      volunteerStatus == "pending" ||
        volunteerStatus == "pending_approval" ||
        volunteerStatus == "awaiting_approval" ->
        warnings += "Volunteer account is pending approval. Analytics will appear after approval."
      volunteerStatus == "rejected" ||
        volunteerStatus == "denied" ||
        volunteerStatus == "declined" ->
        warnings += "Volunteer account was not approved for analytics access."
    }

    val shouldFetchRemoteAnalytics = !isLocalSession

    var impact = VolunteerImpactDashboard()
    var earnings = VolunteerAnalyticsEarnings()
    var performance = VolunteerAnalyticsPerformance()
    var reviews = VolunteerAnalyticsReviews()

    if (shouldFetchRemoteAnalytics) {
      when (val result = callAuthorized { token -> api.getVolunteerImpact(token) }) {
        is ApiCallResult.Success -> {
          impact = VolunteerImpactDashboard(
            counts = result.data.counts.toDomainCounts(),
            impact = result.data.impact.toDomainVolunteerImpact()
          )
        }
        is ApiCallResult.Failure -> warnings += result.message
      }

      when (val result = callAuthorized { token -> api.getVolunteerAnalyticsEarnings(token) }) {
        is ApiCallResult.Success -> earnings = result.data.toDomainVolunteerAnalyticsEarnings()
        is ApiCallResult.Failure -> warnings += result.message
      }

      when (val result = callAuthorized { token -> api.getVolunteerAnalyticsPerformance(token) }) {
        is ApiCallResult.Success -> performance = result.data.toDomainVolunteerAnalyticsPerformance()
        is ApiCallResult.Failure -> warnings += result.message
      }

      when (
        val result = callAuthorized {
          token -> api.getVolunteerAnalyticsReviews(token, page = page, perPage = perPage, rating = rating)
        }
      ) {
        is ApiCallResult.Success -> reviews = result.data.toDomainVolunteerAnalyticsReviews()
        is ApiCallResult.Failure -> warnings += result.message
      }
    }

    VolunteerAnalyticsSnapshot(
      volunteer = volunteer,
      impact = impact,
      earnings = earnings,
      performance = performance,
      reviews = reviews,
      isRemoteConnected = !isLocalSession,
      warningMessage = warnings
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString(" ")
        .ifBlank { null }
    )
  }

  suspend fun getIncomingRequests(): List<AssistanceRequest> = withContext(Dispatchers.IO) {
    getVolunteerIncomingDashboard().requests
  }

  suspend fun getVolunteerRequests(): List<AssistanceRequest> = withContext(Dispatchers.IO) {
    val active = getVolunteerActiveDashboard().requests
    val history = getVolunteerHistoryDashboard().requests
    (active + history).distinctBy { it.id }
  }

  suspend fun getUserProfile(): UserProfile = withContext(Dispatchers.IO) {
    when (val result = getCurrentAccount()) {
      is ApiCallResult.Success -> result.data.toDomainUserProfile()
      is ApiCallResult.Failure -> emptyProfileFromSession()
    }
  }

  suspend fun getCurrentAccount(): ApiCallResult<ApiAuthUser> = withContext(Dispatchers.IO) {
    when (val meResult = callAuthorized { token -> api.getMe(token) }) {
      is ApiCallResult.Success -> {
        val contributionStats = when (val stats = callAuthorized { token -> api.getProfileStats(token) }) {
          is ApiCallResult.Success -> stats.data
          is ApiCallResult.Failure -> ApiContributionStats(0, 0, 0)
        }
        val notificationSettings = when (val remoteSettings = callAuthorized { token -> api.getNotificationSettings(token) }) {
          is ApiCallResult.Success -> {
            appPreferences.saveNotificationSettings(remoteSettings.data)
            remoteSettings.data
          }
          is ApiCallResult.Failure -> appPreferences.readNotificationSettings()
        }
        val privacySettings = when (val remoteSettings = callAuthorized { token -> api.getPrivacySettings(token) }) {
          is ApiCallResult.Success -> {
            appPreferences.savePrivacySettings(remoteSettings.data)
            remoteSettings.data
          }
          is ApiCallResult.Failure -> appPreferences.readPrivacySettings()
        }
        val storedLocation = appPreferences.readProfileLocation()
        val memberSince = meResult.data.memberSince.ifBlank { appPreferences.readOrCreateMemberSince() }

        ApiCallResult.Success(
          meResult.data.copy(
            location = if (storedLocation.isBlank()) meResult.data.location else storedLocation,
            memberSince = memberSince,
            contributionStats = contributionStats,
            notificationSettings = notificationSettings,
            privacySettings = privacySettings
          )
        )
      }
      is ApiCallResult.Failure -> {
        if (meResult.statusCode == null) {
          val session = sessionStore.readAuthSession()
          if (session != null) {
            return@withContext ApiCallResult.Success(
              session.toLocalApiAuthUser(
                volunteerLive = session.volunteerLive,
                location = appPreferences.readProfileLocation(),
                memberSince = appPreferences.readOrCreateMemberSince(),
                notificationSettings = appPreferences.readNotificationSettings(),
                privacySettings = appPreferences.readPrivacySettings()
              )
            )
          }
        }
        meResult
      }
    }
  }

  suspend fun setVolunteerLive(isLive: Boolean): ApiCallResult<ApiAuthUser> = withContext(Dispatchers.IO) {
    when (val result = callAuthorized { token -> api.setVolunteerLive(token, isLive) }) {
      is ApiCallResult.Success -> {
        sessionStore.updateVolunteerLive(result.data.volunteerLive)
        if (result.data.id.isNotBlank()) {
          localRequestStore.setVolunteerLive(result.data.id, result.data.volunteerLive)
        }
        result
      }
      is ApiCallResult.Failure -> {
        if (result.statusCode != null || !canUseLocalRequestFallback()) return@withContext result
        val session = sessionStore.readAuthSession()
          ?: return@withContext ApiCallResult.Failure("You are not logged in.")
        if (session.role != UserRole.Volunteer) {
          return@withContext ApiCallResult.Failure("Only volunteers can toggle live status.")
        }
        localRequestStore.setVolunteerLive(session.userId, isLive)
        sessionStore.updateVolunteerLive(isLive)
        ApiCallResult.Success(
          session.toLocalApiAuthUser(
            volunteerLive = isLive,
            location = appPreferences.readProfileLocation(),
            memberSince = appPreferences.readOrCreateMemberSince(),
            notificationSettings = appPreferences.readNotificationSettings(),
            privacySettings = appPreferences.readPrivacySettings()
          )
        )
      }
    }
  }

  suspend fun createAssistanceRequest(request: ApiCreateRequest): ApiCallResult<ApiVolunteerRequest> = withContext(Dispatchers.IO) {
    when (val result = callAuthorized { token -> api.createRequest(token, request) }) {
      is ApiCallResult.Success -> result
      is ApiCallResult.Failure -> {
        if (result.statusCode != null || !canUseLocalRequestFallback()) return@withContext result
        val session = sessionStore.readAuthSession()
          ?: return@withContext ApiCallResult.Failure("You are not logged in.")
        if (session.role != UserRole.User) {
          return@withContext ApiCallResult.Failure("Only users can create assistance requests.")
        }
        ApiCallResult.Success(localRequestStore.createRequest(session, request))
      }
    }
  }

  suspend fun acceptRequest(requestId: String): ApiCallResult<ApiActionResult> = withContext(Dispatchers.IO) {
    when (val result = callAuthorized { token -> api.acceptRequest(token, requestId) }) {
      is ApiCallResult.Success -> result
      is ApiCallResult.Failure -> {
        if (result.statusCode != null || !canUseLocalRequestFallback()) return@withContext result
        val session = sessionStore.readAuthSession()
          ?: return@withContext ApiCallResult.Failure("You are not logged in.")
        if (session.role != UserRole.Volunteer) {
          return@withContext ApiCallResult.Failure("Only volunteers can accept requests.")
        }
        localRequestStore.acceptRequest(session, requestId)
      }
    }
  }

  suspend fun declineRequest(requestId: String): ApiCallResult<ApiActionResult> = withContext(Dispatchers.IO) {
    when (val result = callAuthorized { token -> api.declineRequest(token, requestId) }) {
      is ApiCallResult.Success -> result
      is ApiCallResult.Failure -> {
        if (result.statusCode != null || !canUseLocalRequestFallback()) return@withContext result
        val session = sessionStore.readAuthSession()
          ?: return@withContext ApiCallResult.Failure("You are not logged in.")
        if (session.role != UserRole.Volunteer) {
          return@withContext ApiCallResult.Failure("Only volunteers can decline requests.")
        }
        localRequestStore.declineRequest(session, requestId)
      }
    }
  }

  suspend fun cancelRequest(requestId: String): ApiCallResult<ApiActionResult> = withContext(Dispatchers.IO) {
    when (val result = callAuthorized { token -> api.cancelRequest(token, requestId) }) {
      is ApiCallResult.Success -> result
      is ApiCallResult.Failure -> {
        if (result.statusCode != null || !canUseLocalRequestFallback()) return@withContext result
        val session = sessionStore.readAuthSession()
          ?: return@withContext ApiCallResult.Failure("You are not logged in.")
        if (session.role != UserRole.User) {
          return@withContext ApiCallResult.Failure("Only users can cancel requests.")
        }
        localRequestStore.cancelRequest(session, requestId)
      }
    }
  }

  suspend fun completeRequest(requestId: String): ApiCallResult<ApiActionResult> = withContext(Dispatchers.IO) {
    when (val result = callAuthorized { token -> api.completeRequest(token, requestId) }) {
      is ApiCallResult.Success -> result
      is ApiCallResult.Failure -> {
        if (result.statusCode != null || !canUseLocalRequestFallback()) return@withContext result
        val session = sessionStore.readAuthSession()
          ?: return@withContext ApiCallResult.Failure("You are not logged in.")
        localRequestStore.completeRequest(session, requestId)
      }
    }
  }

  suspend fun payRequest(
    requestId: String,
    paymentMethod: String,
    amountEgp: Int
  ): ApiCallResult<ApiPayRequestResponse> = withContext(Dispatchers.IO) {
    val normalizedMethod = paymentMethod.trim().uppercase()
    val normalizedAmount = amountEgp.coerceAtLeast(1)
    val session = sessionStore.readAuthSession()
    var fullName = session?.fullName.orEmpty()
    var email = session?.email.orEmpty()
    var phone = session?.phone.orEmpty()
    if (fullName.isBlank() || email.isBlank() || phone.isBlank()) {
      when (val meResult = callAuthorized { token -> api.getMe(token) }) {
        is ApiCallResult.Success -> {
          fullName = meResult.data.fullName.ifBlank { fullName }
          email = meResult.data.email.ifBlank { email }
          phone = meResult.data.phone.ifBlank { phone }
        }
        is ApiCallResult.Failure -> {
          if (meResult.statusCode != null) {
            return@withContext ApiCallResult.Failure(meResult.message, meResult.statusCode)
          }
        }
      }
    }
    val customer = buildPaymentCustomerDetails(
      fullName = fullName,
      email = email,
      phone = phone
    )
    if ((normalizedMethod == "CARD" || normalizedMethod == "WALLET") && customer == null) {
      return@withContext ApiCallResult.Failure("Your full name, email, and phone number are required for checkout.")
    }

    suspend fun runLegacyPaymentFlow(): ApiCallResult<ApiPayRequestResponse> {
      return when (val result = callAuthorized { token ->
        api.payRequest(token, requestId, paymentMethod, normalizedAmount, customer)
      }) {
        is ApiCallResult.Success -> result
        is ApiCallResult.Failure -> {
          if (result.statusCode != null) {
            result
          } else {
            ApiCallResult.Failure("Payments require a live backend connection.")
          }
        }
      }
    }

    fun shouldFallbackToLegacyCheckout(result: ApiCallResult.Failure): Boolean {
      val statusCode = result.statusCode ?: return true
      return statusCode != 401 && statusCode != 403
    }

    when (normalizedMethod) {
      "CARD" -> {
        when (val result = callAuthorized { token ->
          api.checkoutCard(token, requestId, normalizedAmount, customer)
        }) {
          is ApiCallResult.Success -> {
            ApiCallResult.Success(
              ApiPayRequestResponse(
                paymentMethod = "card",
                status = "pending_payment",
                message = "Card checkout created successfully.",
                checkoutUrl = result.data.checkout_url.ifBlank { null },
                paymentId = result.data.payment_id.ifBlank { null }
              )
            )
          }
          is ApiCallResult.Failure -> {
            if (shouldFallbackToLegacyCheckout(result)) {
              when (val legacy = runLegacyPaymentFlow()) {
                is ApiCallResult.Success -> legacy
                is ApiCallResult.Failure -> result
              }
            } else {
              result
            }
          }
        }
      }
      "WALLET" -> {
        when (val result = callAuthorized { token ->
          api.checkoutWallet(token, requestId, normalizedAmount, customer)
        }) {
          is ApiCallResult.Success -> {
            ApiCallResult.Success(
              ApiPayRequestResponse(
                paymentMethod = "wallet",
                status = "pending_payment",
                message = "Wallet checkout created successfully.",
                checkoutUrl = result.data.checkout_url.ifBlank { null },
                paymentId = result.data.payment_id.ifBlank { null }
              )
            )
          }
          is ApiCallResult.Failure -> {
            if (shouldFallbackToLegacyCheckout(result)) {
              when (val legacy = runLegacyPaymentFlow()) {
                is ApiCallResult.Success -> legacy
                is ApiCallResult.Failure -> result
              }
            } else {
              result
            }
          }
        }
      }
      else -> runLegacyPaymentFlow()
    }
  }

  suspend fun getPaymentStatus(paymentId: String): ApiCallResult<ApiPaymentStatus> = withContext(Dispatchers.IO) {
    when (val result = callAuthorized { token -> api.getPaymentStatus(token, paymentId) }) {
      is ApiCallResult.Success -> result
      is ApiCallResult.Failure -> {
        if (result.statusCode != null) return@withContext result
        ApiCallResult.Failure("Payments require a live backend connection.")
      }
    }
  }

  suspend fun refreshPayment(paymentId: String): ApiCallResult<ApiPaymentStatus> = withContext(Dispatchers.IO) {
    when (val result = callAuthorized { token -> api.refreshPayment(token, paymentId) }) {
      is ApiCallResult.Success -> result
      is ApiCallResult.Failure -> {
        if (result.statusCode != null) return@withContext result
        ApiCallResult.Failure("Payments require a live backend connection.")
      }
    }
  }

  suspend fun confirmPaymobPayment(
    paymentId: String,
    success: Boolean = true
  ): ApiCallResult<ApiActionResult> = withContext(Dispatchers.IO) {
    when (val result = api.confirmPaymobPayment(paymentId, success)) {
      is ApiCallResult.Success -> result
      is ApiCallResult.Failure -> {
        val shouldFallbackToRefresh = result.statusCode in setOf(401, 403, 404, 405, 422)
        if (!shouldFallbackToRefresh) {
          if (result.statusCode != null) return@withContext result
          return@withContext ApiCallResult.Failure("Payments require a live backend connection.")
        }

        when (val refresh = callAuthorized { token -> api.refreshPayment(token, paymentId) }) {
          is ApiCallResult.Success -> {
            if (refresh.data.success) {
              ApiCallResult.Success(ApiActionResult(success = true, message = "Payment confirmed successfully."))
            } else {
              ApiCallResult.Failure("Payment is still pending. Complete checkout, then refresh.")
            }
          }
          is ApiCallResult.Failure -> {
            if (refresh.statusCode != null) {
              ApiCallResult.Failure(refresh.message, refresh.statusCode)
            } else {
              ApiCallResult.Failure("Payments require a live backend connection.")
            }
          }
        }
      }
    }
  }

  suspend fun rateVolunteerRequest(
    requestId: String,
    rating: Int,
    comment: String?,
    issues: List<String> = emptyList()
  ): ApiCallResult<ApiActionResult> = withContext(Dispatchers.IO) {
    callAuthorized { token ->
      api.rateVolunteer(
        accessToken = token,
        requestId = requestId,
        rating = rating,
        comment = comment,
        issues = issues
      )
    }
  }

  suspend fun submitLocationRating(locationId: String, score: Int, comment: String?): ApiCallResult<ApiLocation> = withContext(Dispatchers.IO) {
    callAuthorized { token -> api.submitRating(token, locationId, score, comment) }
  }

  suspend fun submitLocationReport(request: ApiLocationReportRequest): ApiCallResult<ApiActionResult> = withContext(Dispatchers.IO) {
    callAuthorized { token -> api.submitLocationReport(token, request) }
  }

  suspend fun sendSupportMessage(subject: String, message: String): ApiCallResult<ApiActionResult> = withContext(Dispatchers.IO) {
    callAuthorized { token -> api.sendSupportMessage(token, subject, message) }
  }

  suspend fun updateProfile(request: ApiUpdateProfileRequest): ApiCallResult<ApiAuthUser> = withContext(Dispatchers.IO) {
    when (val result = callAuthorized { token -> api.patchProfile(token, request) }) {
      is ApiCallResult.Success -> {
        request.location?.let { appPreferences.saveProfileLocation(it.trim()) }
        ApiCallResult.Success(
          result.data.copy(
            location = request.location ?: result.data.location,
            memberSince = result.data.memberSince.ifBlank { appPreferences.readOrCreateMemberSince() },
            contributionStats = result.data.contributionStats,
            notificationSettings = appPreferences.readNotificationSettings(),
            privacySettings = appPreferences.readPrivacySettings()
          )
        )
      }
      is ApiCallResult.Failure -> result
    }
  }

  suspend fun changePassword(currentPassword: String, newPassword: String): ApiCallResult<ApiActionResult> = withContext(Dispatchers.IO) {
    when (val result = callAuthorized { token -> api.changePassword(token, ApiChangePasswordRequest(currentPassword, newPassword)) }) {
      is ApiCallResult.Success -> result
      is ApiCallResult.Failure -> {
        if (result.statusCode == 404) {
          return@withContext ApiCallResult.Failure("Password change is not available on the current backend yet.")
        }
        result
      }
    }
  }

  suspend fun getSessions(): ApiCallResult<List<ApiSessionDto>> = withContext(Dispatchers.IO) {
    when (val result = callAuthorized { token -> api.getSessions(token) }) {
      is ApiCallResult.Success -> {
        if (result.data.isEmpty()) ApiCallResult.Success(defaultCurrentSession()) else result
      }
      is ApiCallResult.Failure -> {
        if (result.statusCode == 404 || result.statusCode == null) {
          ApiCallResult.Success(defaultCurrentSession())
        } else {
          result
        }
      }
    }
  }

  suspend fun updateNotificationSettings(
    request: ApiUpdateNotificationSettingsRequest
  ): ApiCallResult<ApiNotificationSettings> = withContext(Dispatchers.IO) {
    val current = appPreferences.readNotificationSettings()
    val localUpdated = current.copy(
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
    when (val result = callAuthorized { token -> api.patchNotificationSettings(token, request) }) {
      is ApiCallResult.Success -> {
        appPreferences.saveNotificationSettings(result.data)
        result
      }
      is ApiCallResult.Failure -> {
        if (result.statusCode == null) {
          appPreferences.saveNotificationSettings(localUpdated)
          ApiCallResult.Success(localUpdated)
        } else {
          result
        }
      }
    }
  }

  suspend fun updatePrivacySettings(
    request: ApiUpdatePrivacySettingsRequest
  ): ApiCallResult<ApiPrivacySettings> = withContext(Dispatchers.IO) {
    val current = appPreferences.readPrivacySettings()
    val localUpdated = current.copy(
      locationSharing = request.locationSharing ?: current.locationSharing,
      profileVisibility = request.profileVisibility ?: current.profileVisibility,
      showRatings = request.showRatings ?: current.showRatings,
      activityStatus = request.activityStatus ?: current.activityStatus,
      twoFactorAuth = request.twoFactorAuth ?: current.twoFactorAuth
    )
    when (val result = callAuthorized { token -> api.patchPrivacySettings(token, request) }) {
      is ApiCallResult.Success -> {
        appPreferences.savePrivacySettings(result.data)
        result
      }
      is ApiCallResult.Failure -> {
        if (result.statusCode == null) {
          appPreferences.savePrivacySettings(localUpdated)
          ApiCallResult.Success(localUpdated)
        } else {
          result
        }
      }
    }
  }

  suspend fun revokeSession(sessionId: String): ApiCallResult<ApiActionResult> = withContext(Dispatchers.IO) {
    if (sessionId == "current-device") {
      return@withContext ApiCallResult.Failure("Current device session cannot be revoked.")
    }
    when (val result = callAuthorized { token -> api.deleteSession(token, sessionId) }) {
      is ApiCallResult.Success -> result
      is ApiCallResult.Failure -> {
        if (result.statusCode == 404 || result.statusCode == null) {
          ApiCallResult.Success(ApiActionResult(success = true, message = "Session revoked locally."))
        } else {
          result
        }
      }
    }
  }

  private suspend fun getLegacyIncomingDashboard(perPage: Int): VolunteerIncomingDashboard? {
    return when (val legacy = callAuthorized { token -> api.getIncomingVolunteerRequests(token) }) {
      is ApiCallResult.Success -> {
        val requests = legacy.data
          .take(perPage.coerceAtLeast(1))
          .map { it.toDomainAssistanceRequest() }
        val count = requests.size
        VolunteerIncomingDashboard(
          counts = VolunteerDashboardCounts(incoming = count, active = 0, history = 0),
          incomingAlert = VolunteerIncomingAlert(count = count, message = incomingAlertMessage(count)),
          requests = requests
        )
      }
      is ApiCallResult.Failure -> null
    }
  }

  private suspend fun getLegacyVolunteerRequests(): List<AssistanceRequest>? {
    return when (val legacy = callAuthorized { token -> api.getMyRequests(token) }) {
      is ApiCallResult.Success -> legacy.data.volunteerRequests.map { it.toDomainAssistanceRequest() }
      is ApiCallResult.Failure -> null
    }
  }

  private fun incomingAlertMessage(count: Int): String {
    return if (count > 0) {
      "$count ${if (count == 1) "person needs" else "people need"} your help nearby"
    } else {
      "No incoming requests nearby."
    }
  }

  private suspend fun <T> callAuthorized(
    apiCall: suspend (accessToken: String) -> ApiCallResult<T>
  ): ApiCallResult<T> {
    val accessToken = sessionStore.getAccessToken()
      ?: return ApiCallResult.Failure("You are not logged in.")
    if (accessToken.startsWith("local-")) {
      return ApiCallResult.Failure("Local session mode.", statusCode = null)
    }
    val firstAttempt = apiCall(accessToken)
    if (firstAttempt is ApiCallResult.Failure && firstAttempt.statusCode == 401) {
      sessionStore.clearSession()
      return ApiCallResult.Failure("Session expired. Please sign in again.", 401)
    }
    return firstAttempt
  }

  private suspend fun canUseLocalRequestFallback(): Boolean {
    return sessionStore.getAccessToken()?.startsWith("local-") == true
  }

  private suspend fun defaultCurrentSession(): List<ApiSessionDto> {
    val now = System.currentTimeMillis() / 1000
    return listOf(
      ApiSessionDto(
        id = "current-device",
        deviceName = "This Android Device",
        createdAtEpochSeconds = now,
        lastSeenAtEpochSeconds = now,
        isCurrent = true
      )
    )
  }

  private suspend fun emptyProfileFromSession(): UserProfile {
    val session = sessionStore.readAuthSession()
    return UserProfile(
      name = session?.fullName.orEmpty(),
      email = session?.email.orEmpty(),
      phone = session?.phone.orEmpty(),
      disabilityType = session?.disabilityType.orEmpty(),
      memberSince = appPreferences.readOrCreateMemberSince(),
      contributionStats = ContributionStats(
        ratingsSubmitted = 0,
        reportsSubmitted = 0,
        helpfulVotes = 0
      )
    )
  }

  private fun AuthSession.toLocalApiAuthUser(
    volunteerLive: Boolean,
    location: String,
    memberSince: String,
    notificationSettings: ApiNotificationSettings,
    privacySettings: ApiPrivacySettings
  ): ApiAuthUser {
    return ApiAuthUser(
      id = userId,
      role = if (role == UserRole.Volunteer) ApiUserRole.Volunteer else ApiUserRole.User,
      fullName = fullName,
      email = email,
      phone = phone,
      location = location,
      disabilityType = disabilityType,
      memberSince = memberSince,
      volunteerLive = volunteerLive,
      contributionStats = ApiContributionStats(
        ratingsSubmitted = 0,
        reportsSubmitted = 0,
        helpfulVotes = 0
      ),
      notificationSettings = notificationSettings,
      privacySettings = privacySettings
    )
  }

  private fun buildPaymentCustomerDetails(
    fullName: String,
    email: String,
    phone: String
  ): ApiPaymentCustomerDetails? {
    val normalizedEmail = email.trim()
    val normalizedPhone = phone.trim()
    val nameParts = fullName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    val firstName = nameParts.firstOrNull().orEmpty()
    val lastName = nameParts.drop(1).joinToString(" ").ifBlank { "User" }
    if (firstName.isBlank() || normalizedEmail.isBlank() || normalizedPhone.isBlank()) {
      return null
    }
    return ApiPaymentCustomerDetails(
      firstName = firstName,
      lastName = lastName,
      email = normalizedEmail,
      phoneNumber = normalizedPhone
    )
  }
}
