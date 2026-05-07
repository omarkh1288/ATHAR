package com.athar.accessibilitymapping.data

import android.content.Context
import android.util.Log
import com.athar.accessibilitymapping.util.SearchScore
import com.athar.accessibilitymapping.util.buildLocationSearchCandidates
import com.athar.accessibilitymapping.util.normalizePlaceSearchText
import com.athar.accessibilitymapping.util.scorePlaceSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class AtharRepository(
  context: Context,
  private val api: BackendApiClient = BackendApiClient(context.applicationContext),
  private val sessionStore: AuthSessionStore = AuthSessionStore(context.applicationContext),
  private val localRequestStore: LocalRequestStore = LocalRequestStore(context.applicationContext),
  private val appPreferences: AppPreferencesStore = AppPreferencesStore(context.applicationContext)
) {
  @Volatile
  private var cachedSearchableLocations: List<Location>? = null

  suspend fun getLocations(
    lat: Double? = null,
    lng: Double? = null,
    radiusKm: Int = 20
  ): List<Location> = withContext(Dispatchers.IO) {
    when (
      val result = api.getLocations(
        lat = lat ?: BackendApiClient.DEFAULT_LAT,
        lng = lng ?: BackendApiClient.DEFAULT_LNG,
        radiusKm = radiusKm
      )
    ) {
      is ApiCallResult.Success -> result.data.map { it.toDomainLocation() }
      is ApiCallResult.Failure -> mockLocations
    }
  }

  suspend fun searchLocations(
    query: String,
    lat: Double? = null,
    lng: Double? = null,
    radiusKm: Int = 20
  ): List<Location> = withContext(Dispatchers.IO) {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isBlank()) return@withContext emptyList()

    val searchableLocations = getSearchableLocations(lat = lat, lng = lng, radiusKm = radiusKm)
    val localMatches = searchLocalLocations(
      normalizedQuery,
      searchableLocations,
      lat = lat,
      lng = lng
    )

    when (val result = api.searchLocations(normalizedQuery)) {
      is ApiCallResult.Success -> {
        val remoteMatches = result.data.results.map { it.toDomainLocation() }
        mergeSearchLocations(
          remoteMatches = remoteMatches,
          localMatches = localMatches,
          query = normalizedQuery,
          lat = lat,
          lng = lng
        )
      }
      is ApiCallResult.Failure -> localMatches
    }
  }

  private suspend fun getSearchableLocations(
    lat: Double? = null,
    lng: Double? = null,
    radiusKm: Int = 20
  ): List<Location> = withContext(Dispatchers.IO) {
    cachedSearchableLocations?.takeIf { it.isNotEmpty() }?.let { return@withContext it }

    when (val result = api.getAllLocations()) {
      is ApiCallResult.Success -> {
        val locations = result.data.map { it.toDomainLocation() }
        if (locations.isNotEmpty()) {
          cachedSearchableLocations = locations
        }
        locations
      }
      is ApiCallResult.Failure -> {
        cachedSearchableLocations
          ?: getLocations(lat = lat, lng = lng, radiusKm = radiusKm)
      }
    }
  }

  suspend fun getRequests(): List<VolunteerRequest> = withContext(Dispatchers.IO) {
    when (val result = callAuthorized { token -> api.getMyRequests(token) }) {
      is ApiCallResult.Success -> result.data.userRequests
        .map { it.toDomainVolunteerRequest() }
        .map { it.normalizeMoney() }
      is ApiCallResult.Failure -> {
        if (result.statusCode == null && canUseLocalRequestFallback()) {
          val session = sessionStore.readAuthSession()
          if (session?.role == UserRole.User) {
            return@withContext localRequestStore.getUserRequests(session.userId)
              .map { it.toDomainVolunteerRequest() }
              .map { it.normalizeMoney() }
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
          requests = result.data.requests
            .map { it.toDomainAssistanceRequest() }
            .map { it.normalizeMoney() }
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
              requests = requests.map { it.normalizeMoney() }
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
          requests = result.data.requests
            .map { it.toDomainAssistanceRequest() }
            .map { it.normalizeMoney() }
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
            requests = activeRequests.map { it.normalizeMoney() }
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
          requests = result.data.requests
            .map { it.toDomainAssistanceRequest() }
            .map { it.normalizeMoney() }
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
            requests = historyRequests.map { it.normalizeMoney() }
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

  suspend fun submitVolunteerAnalyticsWithdrawal(
    amountEgp: Int,
    method: String
  ): ApiCallResult<ApiActionResult> = withContext(Dispatchers.IO) {
    when (val result = callAuthorized { token -> api.submitVolunteerAnalyticsWithdrawal(token, amountEgp = amountEgp, method = method) }) {
      is ApiCallResult.Success -> result
      is ApiCallResult.Failure -> result
    }
  }

  suspend fun getVolunteerAnalyticsSnapshot(
    page: Int = 1,
    perPage: Int = 100,
    rating: Int? = null
  ): VolunteerAnalyticsSnapshot = withContext(Dispatchers.IO) {
    val warnings = linkedSetOf<String>()
    val isLocalSession = canUseLocalRequestFallback()

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

    val shouldFetchRemoteAnalytics = true

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
        is ApiCallResult.Failure -> {
          Log.w("VolunteerAnalytics", "Impact endpoint failed: ${result.message} (status=${result.statusCode})")
          warnings += result.message
        }
      }

      when (val result = callAuthorized { token -> api.getVolunteerAnalyticsEarnings(token) }) {
        is ApiCallResult.Success -> earnings = result.data.toDomainVolunteerAnalyticsEarnings()
        is ApiCallResult.Failure -> {
          Log.w("VolunteerAnalytics", "Earnings endpoint failed: ${result.message} (status=${result.statusCode})")
          warnings += result.message
        }
      }

      when (val result = callAuthorized { token -> api.getVolunteerAnalyticsPerformance(token) }) {
        is ApiCallResult.Success -> performance = result.data.toDomainVolunteerAnalyticsPerformance()
        is ApiCallResult.Failure -> {
          Log.w("VolunteerAnalytics", "Performance endpoint failed: ${result.message} (status=${result.statusCode})")
          warnings += result.message
        }
      }

      when (val result = callAuthorized { token -> api.getVolunteerAnalyticsReviews(token, page = page, perPage = perPage, rating = rating) }) {
        is ApiCallResult.Success -> reviews = result.data.toDomainVolunteerAnalyticsReviews()
        is ApiCallResult.Failure -> {
          Log.w("VolunteerAnalytics", "Reviews endpoint failed: ${result.message} (status=${result.statusCode})")
          warnings += result.message
        }
      }
    }

    val needsFallbackAnalytics = earnings.needsFallbackData() || performance.needsFallbackData()
    if (needsFallbackAnalytics) {
      Log.w("VolunteerAnalytics", "Remote analytics data is partial or empty, computing fallback from request history")
      val fallback = computeAnalyticsFallbackFromHistory()
      if (fallback != null) {
        if (impact == VolunteerImpactDashboard()) impact = fallback.impact
        val mergedEarnings = earnings.mergeMissingFrom(fallback.earnings)
        val mergedPerformance = performance.mergeMissingFrom(fallback.performance)
        val mergedAnything = mergedEarnings != earnings || mergedPerformance != performance
        earnings = mergedEarnings
        performance = mergedPerformance
        if (mergedAnything) {
          warnings += "Some analytics were rebuilt from request history."
        }
      }
    }

    VolunteerAnalyticsSnapshot(
      volunteer = volunteer,
      impact = impact,
      earnings = earnings,
      performance = performance,
      reviews = reviews,
      isRemoteConnected = shouldFetchRemoteAnalytics && !earnings.needsFallbackData() && !performance.needsFallbackData(),
      warningMessage = warnings
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString(" ")
        .ifBlank { null }
    )
  }

  private data class AnalyticsFallback(
    val impact: VolunteerImpactDashboard,
    val earnings: VolunteerAnalyticsEarnings,
    val performance: VolunteerAnalyticsPerformance
  )

  private fun VolunteerAnalyticsEarnings.needsFallbackData(): Boolean {
    return totalGross <= 0.0 &&
      totalNet <= 0.0 &&
      availableBalance <= 0.0 &&
      pendingBalance <= 0.0 &&
      monthlyEarnings.isEmpty() &&
      paymentHistory.isEmpty()
  }

  private fun VolunteerAnalyticsPerformance.needsFallbackData(): Boolean {
    return completed <= 0 &&
      pending <= 0 &&
      weeklyActivity.isEmpty() &&
      requestTypes.isEmpty()
  }

  private fun VolunteerAnalyticsEarnings.mergeMissingFrom(
    fallback: VolunteerAnalyticsEarnings
  ): VolunteerAnalyticsEarnings {
    return copy(
      availableBalance = availableBalance.takeUnless { it <= 0.0 } ?: fallback.availableBalance,
      pendingBalance = pendingBalance.takeUnless { it <= 0.0 } ?: fallback.pendingBalance,
      totalGross = totalGross.takeUnless { it <= 0.0 } ?: fallback.totalGross,
      totalFees = totalFees.takeUnless { it <= 0.0 } ?: fallback.totalFees,
      totalNet = totalNet.takeUnless { it <= 0.0 } ?: fallback.totalNet,
      thisWeekNet = thisWeekNet.takeUnless { it <= 0.0 } ?: fallback.thisWeekNet,
      currentMonthLabel = currentMonthLabel.ifBlank { fallback.currentMonthLabel },
      currentMonthNet = currentMonthNet.takeUnless { it <= 0.0 } ?: fallback.currentMonthNet,
      lastMonthNet = lastMonthNet.takeUnless { it <= 0.0 } ?: fallback.lastMonthNet,
      monthlyChangePercent = monthlyChangePercent.takeUnless { it == 0.0 } ?: fallback.monthlyChangePercent,
      monthlyEarnings = monthlyEarnings.ifEmpty { fallback.monthlyEarnings },
      withdrawalHistory = withdrawalHistory.ifEmpty { fallback.withdrawalHistory },
      paymentHistory = paymentHistory.ifEmpty { fallback.paymentHistory }
    )
  }

  private fun VolunteerAnalyticsPerformance.mergeMissingFrom(
    fallback: VolunteerAnalyticsPerformance
  ): VolunteerAnalyticsPerformance {
    return copy(
      grade = grade.ifBlank { fallback.grade },
      headline = headline.ifBlank { fallback.headline },
      percentile = percentile.takeUnless { it == 50 } ?: fallback.percentile,
      responseRate = responseRate.takeUnless { it <= 0f } ?: fallback.responseRate,
      completionRate = completionRate.takeUnless { it <= 0f } ?: fallback.completionRate,
      averageRating = averageRating.takeUnless { it <= 0f } ?: fallback.averageRating,
      onTimeRate = onTimeRate.takeUnless { it <= 0f } ?: fallback.onTimeRate,
      completed = completed.takeUnless { it <= 0 } ?: fallback.completed,
      pending = pending.takeUnless { it <= 0 } ?: fallback.pending,
      usersHelped = usersHelped.takeUnless { it <= 0 } ?: fallback.usersHelped,
      positiveReviews = positiveReviews.takeUnless { it <= 0 } ?: fallback.positiveReviews,
      fiveStarRatings = fiveStarRatings.takeUnless { it <= 0 } ?: fallback.fiveStarRatings,
      totalReviews = totalReviews.takeUnless { it <= 0 } ?: fallback.totalReviews,
      badges = badges.ifEmpty { fallback.badges },
      weeklyActivity = weeklyActivity.ifEmpty { fallback.weeklyActivity },
      requestTypes = requestTypes.ifEmpty { fallback.requestTypes }
    )
  }

  private suspend fun computeAnalyticsFallbackFromHistory(): AnalyticsFallback? {
    return try {
      val history = getVolunteerHistoryDashboard(perPage = 100)
      val active = getVolunteerActiveDashboard(perPage = 100)
      val allRequests = (history.requests + active.requests).distinctBy { it.id }
      if (allRequests.isEmpty()) return null

      val completedRequests = allRequests.filter {
        it.status == RequestStatus.Completed ||
          it.status == RequestStatus.Rated ||
          it.status == RequestStatus.Archived
      }
      val pendingRequests = allRequests.filter {
        it.status == RequestStatus.Accepted ||
          it.status == RequestStatus.InProgress ||
          it.status == RequestStatus.Broadcasted ||
          it.status == RequestStatus.Created
      }
      val cancelledRequests = allRequests.filter {
        it.status == RequestStatus.Cancelled ||
          it.status == RequestStatus.NoVolunteer
      }

      val totalGross = completedRequests.sumOf { req ->
        (req.totalAmountEgp ?: (req.hours * req.pricePerHour)).toDouble()
      }
      val totalFees = totalGross * 0.30
      val totalNet = totalGross - totalFees
      val pendingGross = pendingRequests.sumOf { req ->
        (req.totalAmountEgp ?: (req.hours * req.pricePerHour)).toDouble()
      }

      val paymentHistory = completedRequests.map { req ->
        val gross = (req.totalAmountEgp ?: (req.hours * req.pricePerHour)).toDouble()
        val net = gross * 0.70
        VolunteerAnalyticsPaymentRecord(
          id = req.id,
          date = req.requestTime,
          user = req.userName,
          hours = req.hours,
          gross = gross,
          net = net,
          status = AnalyticsRecordStatus.Completed
        )
      }

      val completedCount = completedRequests.size
      val pendingCount = pendingRequests.size
      val totalCount = completedCount + pendingCount + cancelledRequests.size
      val completionRate = if (totalCount > 0) {
        (completedCount.toFloat() / totalCount * 100f)
      } else 0f

      val requestTypeCounts = completedRequests
        .groupBy { it.helpType.ifBlank { "General" } }
        .map { (type, reqs) -> VolunteerAnalyticsRequestTypeShare(name = type, value = reqs.size) }

      val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
      val weeklyCounts = completedRequests
        .groupingBy { request ->
          request.requestTime.take(3).replaceFirstChar { char -> char.uppercase() }
        }
        .eachCount()
      val weeklyActivity = dayNames.map { day ->
        VolunteerAnalyticsWeeklyActivity(day = day, completed = weeklyCounts[day] ?: 0)
      }
      val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
      val monthlyEarnings = completedRequests
        .groupBy { request ->
          request.requestTime.take(3).replaceFirstChar { char -> char.uppercase() }
        }
        .map { (month, requestsForMonth) ->
          val gross = requestsForMonth.sumOf { req ->
            (req.totalAmountEgp ?: (req.hours * req.pricePerHour)).toDouble()
          }
          val fee = gross * 0.30
          VolunteerAnalyticsMonthlyEarning(
            month = month,
            gross = gross,
            net = gross - fee,
            fee = fee
          )
        }
        .sortedBy { entry -> monthNames.indexOf(entry.month).takeIf { it >= 0 } ?: Int.MAX_VALUE }

      val grade = when {
        completionRate >= 90f -> "A"
        completionRate >= 75f -> "B"
        completionRate >= 50f -> "C"
        completionRate >= 25f -> "D"
        else -> "F"
      }

      val earnings = VolunteerAnalyticsEarnings(
        availableBalance = totalNet,
        pendingBalance = pendingGross * 0.70,
        totalGross = totalGross,
        totalFees = totalFees,
        totalNet = totalNet,
        thisWeekNet = weeklyActivity.sumOf { activity ->
          if (activity.completed > 0 && completedCount > 0) totalNet / completedCount * activity.completed else 0.0
        },
        currentMonthLabel = monthlyEarnings.lastOrNull()?.month.orEmpty(),
        currentMonthNet = monthlyEarnings.lastOrNull()?.net ?: totalNet,
        monthlyEarnings = monthlyEarnings,
        paymentHistory = paymentHistory
      )

      val performance = VolunteerAnalyticsPerformance(
        grade = grade,
        headline = when (grade) {
          "A" -> "Outstanding volunteer!"
          "B" -> "Great performance!"
          "C" -> "Good progress, keep going!"
          else -> "Complete more requests to improve"
        },
        percentile = (completionRate * 0.9f).toInt().coerceIn(1, 99),
        completionRate = completionRate,
        averageRating = history.impact.avgRating,
        completed = completedCount,
        pending = pendingCount,
        usersHelped = completedRequests.map { it.userName }.distinct().size,
        totalReviews = 0,
        weeklyActivity = weeklyActivity,
        requestTypes = requestTypeCounts
      )

      AnalyticsFallback(
        impact = VolunteerImpactDashboard(
          counts = VolunteerDashboardCounts(
            incoming = 0,
            active = pendingCount,
            history = completedCount
          ),
          impact = history.impact
        ),
        earnings = earnings,
        performance = performance
      )
    } catch (e: Exception) {
      Log.w("VolunteerAnalytics", "Fallback computation failed: ${e.message}")
      null
    }
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
    val normalizedAmount = normalizeMoneyAmount(amountEgp).coerceAtLeast(1)
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
      is ApiCallResult.Success -> ApiCallResult.Success(result.data.normalizeMoney())
      is ApiCallResult.Failure -> {
        if (result.statusCode != null) return@withContext result
        ApiCallResult.Failure("Payments require a live backend connection.")
      }
    }
  }

  suspend fun refreshPayment(paymentId: String): ApiCallResult<ApiPaymentStatus> = withContext(Dispatchers.IO) {
    when (val result = callAuthorized { token -> api.refreshPayment(token, paymentId) }) {
      is ApiCallResult.Success -> ApiCallResult.Success(result.data.normalizeMoney())
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

  suspend fun getGovernments(): ApiCallResult<List<ApiGovernment>> = withContext(Dispatchers.IO) {
    val accessToken = ensureValidAccessToken()
      ?.takeUnless { it.startsWith("local-") }
    api.getGovernments(token = accessToken)
  }

  suspend fun getCategories(): ApiCallResult<List<ApiCategory>> = withContext(Dispatchers.IO) {
    val accessToken = ensureValidAccessToken()
      ?.takeUnless { it.startsWith("local-") }
    api.getCategories(token = accessToken)
  }

  suspend fun submitLocationReport(request: ApiLocationReportRequest): ApiCallResult<ApiLocationReportResult> = withContext(Dispatchers.IO) {
    val result = callAuthorized { token -> api.submitLocationReport(token, request) }
    if (result is ApiCallResult.Success) {
      val locId = result.data.locationId
      val contribution = result.data.contribution
      if (locId != null && contribution != null) {
        PendingContributionsCache.put(locId, contribution.copy(locationId = locId))
      }
    }
    result
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

  suspend fun uploadProfilePhoto(photoUri: String): ApiCallResult<ApiAuthUser> = withContext(Dispatchers.IO) {
    callAuthorized { token -> api.uploadProfilePhoto(token, photoUri) }
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
    val accessToken = ensureValidAccessToken()
      ?: return ApiCallResult.Failure("You are not logged in.")
    val firstAttempt = apiCall(accessToken)
    if (firstAttempt is ApiCallResult.Failure && firstAttempt.statusCode == 401) {
      val refreshedAccessToken = refreshAuthorizedSession()
      if (!refreshedAccessToken.isNullOrBlank()) {
        return apiCall(refreshedAccessToken)
      }
      sessionStore.clearSession()
      return ApiCallResult.Failure("Session expired. Please sign in again.", 401)
    }
    return firstAttempt
  }

  private suspend fun ensureValidAccessToken(): String? {
    val accessToken = sessionStore.getAccessToken() ?: return null
    if (accessToken.startsWith("local-")) return accessToken
    val expiresAt = sessionStore.getExpiresAtEpochSeconds()
    val now = Instant.now().epochSecond
    return if (expiresAt != null && expiresAt <= now + 30) {
      refreshAuthorizedSession() ?: accessToken
    } else {
      accessToken
    }
  }

  private suspend fun refreshAuthorizedSession(): String? {
    val currentAccessToken = sessionStore.getAccessToken() ?: return null
    if (currentAccessToken.startsWith("local-")) return currentAccessToken
    val refreshToken = sessionStore.getRefreshToken() ?: return null
    return when (val refreshResult = api.refresh(refreshToken)) {
      is ApiCallResult.Success -> {
        sessionStore.updateTokens(
          accessToken = refreshResult.data.tokens.accessToken,
          refreshToken = refreshResult.data.tokens.refreshToken,
          expiresAtEpochSeconds = refreshResult.data.tokens.expiresAtEpochSeconds
        )
        sessionStore.updateVolunteerLive(refreshResult.data.user.volunteerLive)
        refreshResult.data.tokens.accessToken
      }
      is ApiCallResult.Failure -> {
        Log.w("AtharRepository", "Token refresh failed: ${refreshResult.message}")
        sessionStore.clearSession()
        null
      }
    }
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
      profilePhotoPath = null,
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

  private fun searchLocalLocations(
    query: String,
    locations: List<Location>,
    lat: Double? = null,
    lng: Double? = null
  ): List<Location> {
    return rankLocations(locations, query, lat, lng).map { it.location }
  }

  private fun mergeSearchLocations(
    remoteMatches: List<Location>,
    localMatches: List<Location>,
    query: String,
    lat: Double? = null,
    lng: Double? = null
  ): List<Location> {
    return rankLocations(
      locations = (remoteMatches + localMatches)
      .distinctBy { location ->
        listOf(location.id, normalizePlaceSearchText(location.name))
          .firstOrNull { it.isNotBlank() }
          .orEmpty()
      },
      query = query,
      lat = lat,
      lng = lng
    ).map { it.location }
  }

  private fun rankLocations(
    locations: List<Location>,
    query: String,
    lat: Double? = null,
    lng: Double? = null
  ): List<RankedLocation> {
    return locations
      .mapNotNull { location ->
        val score = scorePlaceSearch(query, buildLocationSearchCandidates(location))
          ?: return@mapNotNull null
        RankedLocation(
          location = location,
          score = score,
          distanceMeters = if (lat != null && lng != null) {
            haversineDistanceMeters(lat, lng, location.lat, location.lng)
          } else {
            null
          }
        )
      }
      .sortedWith(
        compareBy<RankedLocation> { it.score }
          .thenBy { it.distanceMeters ?: Double.MAX_VALUE }
          .thenByDescending { it.location.rating }
          .thenBy { it.location.name.length }
      )
  }

  private fun haversineDistanceMeters(
    fromLat: Double,
    fromLng: Double,
    toLat: Double,
    toLng: Double
  ): Double {
    val earthRadiusMeters = 6371000.0
    val latDistance = Math.toRadians(toLat - fromLat)
    val lngDistance = Math.toRadians(toLng - fromLng)
    val startLat = Math.toRadians(fromLat)
    val endLat = Math.toRadians(toLat)
    val a = sin(latDistance / 2).pow(2.0) +
      sin(lngDistance / 2).pow(2.0) * cos(startLat) * cos(endLat)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadiusMeters * c
  }

  private data class RankedLocation(
    val location: Location,
    val score: SearchScore,
    val distanceMeters: Double?
  )

  private fun VolunteerRequest.normalizeMoney(): VolunteerRequest {
    return copy(
      pricePerHour = normalizeMoneyAmount(pricePerHour),
      totalAmountEgp = totalAmountEgp?.let { normalizeMoneyAmount(it) }
    )
  }

  private fun AssistanceRequest.normalizeMoney(): AssistanceRequest {
    return copy(
      pricePerHour = normalizeMoneyAmount(pricePerHour),
      totalAmountEgp = totalAmountEgp?.let { normalizeMoneyAmount(it) }
    )
  }

  private fun ApiPaymentStatus.normalizeMoney(): ApiPaymentStatus {
    return copy(amount = normalizeMoneyAmount(amount.toInt()).toDouble())
  }

  private fun normalizeMoneyAmount(amount: Int): Int {
    // Valid EGP range: pricePerHour 50-200, max total 200*8=1600.
    // Piaster values start at 5000 (50 EGP * 100), so > 1600 safely catches them.
    return when {
      amount > 1600 && amount % 100 == 0 -> (amount / 100).coerceAtLeast(1)
      else -> amount.coerceAtLeast(1)
    }
  }
}
