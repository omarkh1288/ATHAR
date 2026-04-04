package com.athar.accessibilitymapping.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.athar.accessibilitymapping.data.ApiCallResult
import com.athar.accessibilitymapping.data.AssistanceRequest
import com.athar.accessibilitymapping.data.AtharRepository
import com.athar.accessibilitymapping.data.RequestStatus
import com.athar.accessibilitymapping.data.VolunteerDashboardCounts
import com.athar.accessibilitymapping.data.volunteer.AtharVolunteerDashboardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class VolunteerDashboardTab(val label: String) {
  INCOMING("Incoming"),
  ACCEPTED("Active"),
  HISTORY("History")
}

data class VolunteerDashboardStatsState(
  val totalAssists: Int = 0,
  val thisWeek: Int = 0,
  val avgRating: Float = 0f,
  val streak: Int = 3
)

data class VolunteerDashboardLocalHistoryEntry(
  val id: String,
  val userName: String,
  val userType: String,
  val location: String,
  val helpType: String,
  val completedTime: String,
  val rating: Int = 0
)

data class VolunteerDashboardWeeklyPoint(
  val day: String,
  val completed: Int
)

data class VolunteerDashboardState(
  val activeTab: VolunteerDashboardTab = VolunteerDashboardTab.INCOMING,
  val isRefreshing: Boolean = false,
  val actionError: String? = null,
  val acceptBlockMessage: String? = null,
  val paymentPendingMessage: String? = null,
  val tabCounts: VolunteerDashboardCounts = VolunteerDashboardCounts(),
  val incomingAlertMessage: String = "No incoming requests nearby.",
  val activeStatusBanner: String = "Assistance in Progress",
  val stats: VolunteerDashboardStatsState = VolunteerDashboardStatsState(),
  val weeklyActivity: List<VolunteerDashboardWeeklyPoint> = emptyList(),
  val incomingRequests: List<AssistanceRequest> = emptyList(),
  val activeRequest: AssistanceRequest? = null,
  val historyRequests: List<AssistanceRequest> = emptyList(),
  val localCompletedHistory: List<VolunteerDashboardLocalHistoryEntry> = emptyList()
)

class VolunteerDashboardViewModel(application: Application) : AndroidViewModel(application) {
  private val repository = AtharRepository(application)

  private val _uiState = MutableStateFlow(VolunteerDashboardState())
  val uiState: StateFlow<VolunteerDashboardState> = _uiState.asStateFlow()

  private var impactLoaded = false
  private var incomingLoaded = false
  private var activeLoaded = false
  private var historyLoaded = false
  private var lastKnownActiveRequest: AssistanceRequest? = null

  fun loadIfNeeded(isVolunteerLive: Boolean) {
    viewModelScope.launch {
      ensureImpactLoaded(forceRefresh = true)
      if (isVolunteerLive) {
        ensureTabLoaded(_uiState.value.activeTab, forceRefresh = true)
      } else if (!historyLoaded) {
        // Offline mode still shows prior stats/history from cached or persisted data.
        refreshHistoryData(forceRefresh = true)
      }
    }
  }

  fun selectTab(tab: VolunteerDashboardTab, isVolunteerLive: Boolean) {
    _uiState.update { it.copy(activeTab = tab, actionError = null, acceptBlockMessage = null) }
    if (isVolunteerLive) {
      viewModelScope.launch {
        ensureTabLoaded(tab, forceRefresh = false)
      }
    }
  }

  fun clearActionError() {
    _uiState.update { it.copy(actionError = null) }
  }

  fun clearAcceptBlockMessage() {
    _uiState.update { it.copy(acceptBlockMessage = null) }
  }

  fun refresh(isVolunteerLive: Boolean) {
    viewModelScope.launch {
      _uiState.update { it.copy(isRefreshing = true, actionError = null, acceptBlockMessage = null) }
      try {
        ensureImpactLoaded(forceRefresh = true)
        if (isVolunteerLive) {
          ensureTabLoaded(_uiState.value.activeTab, forceRefresh = true)
        } else {
          refreshHistoryData(forceRefresh = true)
        }
      } finally {
        _uiState.update { it.copy(isRefreshing = false) }
      }
    }
  }

  fun acceptRequest(request: AssistanceRequest) {
    viewModelScope.launch {
      _uiState.update { it.copy(actionError = null, acceptBlockMessage = null, isRefreshing = true) }
      try {
        val existingActiveRequest = resolveCurrentActiveRequest()
        if (existingActiveRequest != null) {
          _uiState.update {
            it.copy(
              activeTab = VolunteerDashboardTab.ACCEPTED,
              activeRequest = existingActiveRequest,
              acceptBlockMessage = "Finish your current request first, then you can accept a new one."
            )
          }
          activeLoaded = true
          incomingLoaded = false
          refreshIncomingData(forceRefresh = true)
          return@launch
        }

        when (val result = repository.acceptRequest(request.id)) {
          is ApiCallResult.Success -> {
            _uiState.update {
              it.copy(
                activeTab = VolunteerDashboardTab.ACCEPTED,
                activeRequest = request
              )
            }
            incomingLoaded = false
            activeLoaded = false
            refreshActiveData(forceRefresh = true)
            refreshIncomingData(forceRefresh = true)
          }
          is ApiCallResult.Failure -> {
            _uiState.update { it.copy(actionError = result.message) }
          }
        }
      } finally {
        _uiState.update { it.copy(isRefreshing = false) }
      }
    }
  }

  fun declineRequest(requestId: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(actionError = null, acceptBlockMessage = null, isRefreshing = true) }
      try {
        when (val result = repository.declineRequest(requestId)) {
          is ApiCallResult.Success -> {
            incomingLoaded = false
            refreshIncomingData(forceRefresh = true)
          }
          is ApiCallResult.Failure -> {
            _uiState.update { it.copy(actionError = result.message) }
          }
        }
      } finally {
        _uiState.update { it.copy(isRefreshing = false) }
      }
    }
  }

  private var pendingCompleteRequest: AssistanceRequest? = null
  private var recentlyCompletedId: String? = null

  fun clearPaymentPendingMessage() {
    _uiState.update { it.copy(paymentPendingMessage = null) }
  }

  private fun isRequestPaidByUser(request: AssistanceRequest): Boolean {
    if (request.paymentMethod.lowercase() == "cash") return true
    if (request.isPaid) return true
    return when (request.paymentStatus?.trim()?.lowercase()) {
      "success", "succeeded", "approved", "paid", "captured", "completed", "settled", "authorized" -> true
      else -> false
    }
  }

  fun completeAcceptedRequest() {
    viewModelScope.launch {
      _uiState.update { it.copy(actionError = null, acceptBlockMessage = null, paymentPendingMessage = null, isRefreshing = true) }
      try {
        val currentRequest = pendingCompleteRequest ?: _uiState.value.activeRequest
        val latestActiveDashboard = repository.getVolunteerActiveDashboard(perPage = 50)
        val refreshedActiveRequest = when {
          currentRequest != null -> latestActiveDashboard.requests.firstOrNull { it.id == currentRequest.id }
            ?: currentRequest
          else -> latestActiveDashboard.requests.firstOrNull()
        }

        if (refreshedActiveRequest == null) {
          _uiState.update {
            it.copy(
              activeTab = VolunteerDashboardTab.HISTORY,
              activeRequest = null,
              actionError = "No active request is available to complete."
            )
          }
          activeLoaded = true
          refreshHistoryData(forceRefresh = true)
          return@launch
        }

        pendingCompleteRequest = refreshedActiveRequest
        _uiState.update {
          it.copy(
            tabCounts = latestActiveDashboard.counts,
            activeStatusBanner = latestActiveDashboard.statusBanner.ifBlank { "Assistance in Progress" },
            activeRequest = refreshedActiveRequest
          )
        }

        when (val result = repository.completeRequest(refreshedActiveRequest.id)) {
          is ApiCallResult.Success -> {
            pendingCompleteRequest = null
            recentlyCompletedId = refreshedActiveRequest.id
            android.util.Log.i("VolunteerDashboard", "completeRequest SUCCESS for ${refreshedActiveRequest.id}")
            // Completing a request changes earnings, payment history, and weekly analytics.
            AtharVolunteerDashboardRepository.clearCachedDashboards()
            _uiState.update {
              it.copy(
                activeTab = VolunteerDashboardTab.HISTORY,
                activeRequest = null,
                acceptBlockMessage = null,
                localCompletedHistory = listOf(refreshedActiveRequest.toLocalHistoryEntry()) +
                  it.localCompletedHistory.filterNot { existing -> existing.id == refreshedActiveRequest.id }
              )
            }
            impactLoaded = false
            activeLoaded = false
            historyLoaded = false
            refreshHistoryData(forceRefresh = true)
            refreshActiveData(forceRefresh = true)
            ensureImpactLoaded(forceRefresh = true)
          }
          is ApiCallResult.Failure -> {
            pendingCompleteRequest = null
            android.util.Log.e("VolunteerDashboard", "completeRequest FAILED for ${refreshedActiveRequest.id}: ${result.message}")
            val msg = result.message.lowercase()
            val isPaymentIssue = msg.contains("payment") ||
              msg.contains("not paid") ||
              msg.contains("pending") ||
              msg.contains("confirmed") ||
              msg.contains("assigned active")
            if (isPaymentIssue) {
              _uiState.update { it.copy(paymentPendingMessage = "The user has not paid for this request yet. Please wait for payment before marking as completed.") }
            } else {
              _uiState.update { it.copy(actionError = result.message) }
            }
            activeLoaded = false
            historyLoaded = false
            refreshActiveData(forceRefresh = true)
            refreshHistoryData(forceRefresh = true)
          }
        }
      } finally {
        _uiState.update { it.copy(isRefreshing = false) }
      }
    }
  }

  private suspend fun ensureImpactLoaded(forceRefresh: Boolean) {
    if (forceRefresh || !impactLoaded) {
      refreshImpactData(forceRefresh)
    }
  }

  private suspend fun ensureTabLoaded(tab: VolunteerDashboardTab, forceRefresh: Boolean) {
    when (tab) {
      VolunteerDashboardTab.INCOMING -> if (forceRefresh || !incomingLoaded) refreshIncomingData(forceRefresh)
      VolunteerDashboardTab.ACCEPTED -> if (forceRefresh || !activeLoaded) refreshActiveData(forceRefresh)
      VolunteerDashboardTab.HISTORY -> if (forceRefresh || !historyLoaded) refreshHistoryData(forceRefresh)
    }
  }

  private suspend fun refreshIncomingData(forceRefresh: Boolean) {
    // Don't resolve an active request if we just completed one
    val resolvedActiveRequest = if (recentlyCompletedId != null) {
      _uiState.value.activeRequest
    } else {
      _uiState.value.activeRequest ?: resolveCurrentActiveRequest()
    }
    val incoming = repository.getVolunteerIncomingDashboard(perPage = 50)
    val currentAcceptedId = resolvedActiveRequest?.id
    val alertCount = if (incoming.incomingAlert.count > 0) {
      incoming.incomingAlert.count
    } else {
      incoming.requests.size
    }
    _uiState.update {
      it.copy(
        tabCounts = incoming.counts,
        activeRequest = resolvedActiveRequest ?: it.activeRequest,
        incomingAlertMessage = incoming.incomingAlert.message.ifBlank {
          if (alertCount > 0) {
            "$alertCount ${if (alertCount == 1) "person needs" else "people need"} your help nearby"
          } else {
            "No incoming requests nearby."
          }
        },
        incomingRequests = incoming.requests.filter { request -> request.id != currentAcceptedId }
      )
    }
    incomingLoaded = true
  }

  private suspend fun refreshActiveData(forceRefresh: Boolean) {
    val active = repository.getVolunteerActiveDashboard(perPage = 50)
    val currentAcceptedId = _uiState.value.activeRequest?.id
    // Filter out the recently completed request so server lag doesn't bring it back
    val filteredRequests = active.requests.filter { it.id != recentlyCompletedId }
    _uiState.update {
      val knownHistoryIds = it.historyRequests.map { request -> request.id }.toSet() +
        it.localCompletedHistory.map { entry -> entry.id }.toSet()
      val resolvedActive = when {
        pendingCompleteRequest != null -> it.activeRequest
        currentAcceptedId != null -> filteredRequests.firstOrNull { request -> request.id == currentAcceptedId }
          ?: if (currentAcceptedId !in knownHistoryIds) it.activeRequest ?: lastKnownActiveRequest?.takeIf { request -> request.id == currentAcceptedId } else null
        filteredRequests.isNotEmpty() -> filteredRequests.firstOrNull()
        else -> lastKnownActiveRequest?.takeIf { request -> request.id !in knownHistoryIds }
      }
      if (resolvedActive != null) {
        lastKnownActiveRequest = resolvedActive
      }
      it.copy(
        tabCounts = active.counts,
        activeStatusBanner = if (resolvedActive != null) {
          "Assistance in Progress"
        } else {
          active.statusBanner.ifBlank { "No active assistance right now." }
        },
        activeRequest = resolvedActive
      )
    }
    // If server no longer returns the completed request, clear the guard
    if (recentlyCompletedId != null && active.requests.none { it.id == recentlyCompletedId }) {
      recentlyCompletedId = null
    }
    activeLoaded = true
  }

  private suspend fun resolveCurrentActiveRequest(): AssistanceRequest? {
    val localActiveRequest = _uiState.value.activeRequest
    if (localActiveRequest != null) return localActiveRequest

    val active = repository.getVolunteerActiveDashboard(perPage = 1)
    // Filter out recently completed request
    val resolvedActiveRequest = active.requests.firstOrNull { it.id != recentlyCompletedId }
    if (resolvedActiveRequest != null) {
      lastKnownActiveRequest = resolvedActiveRequest
      _uiState.update {
        it.copy(
          tabCounts = active.counts,
          activeStatusBanner = "Assistance in Progress",
          activeRequest = resolvedActiveRequest
        )
      }
    }
    return resolvedActiveRequest
  }

  private suspend fun refreshHistoryData(forceRefresh: Boolean) {
    val history = repository.getVolunteerHistoryDashboard(perPage = 50)
    val historyIds = history.requests.map { request -> request.id }.toSet()
    if (lastKnownActiveRequest?.id in historyIds) {
      lastKnownActiveRequest = null
    }
    _uiState.update {
      it.copy(
        tabCounts = history.counts,
        stats = it.stats.copy(
          totalAssists = history.impact.totalAssists,
          avgRating = history.impact.avgRating,
          thisWeek = history.impact.thisWeek
        ),
        historyRequests = history.requests
      )
    }
    historyLoaded = true
  }

  private suspend fun refreshImpactData(forceRefresh: Boolean) {
    val impact = repository.getVolunteerImpactDashboard()
    val performance = repository.getVolunteerAnalyticsPerformance()
    val weeklyPoints = performance.weeklyActivity.map { entry ->
      VolunteerDashboardWeeklyPoint(day = entry.day, completed = entry.completed)
    }
    _uiState.update {
      it.copy(
        tabCounts = impact.counts,
        stats = it.stats.copy(
          totalAssists = impact.impact.totalAssists,
          avgRating = impact.impact.avgRating,
          thisWeek = impact.impact.thisWeek
        ),
        weeklyActivity = weeklyPoints
      )
    }
    impactLoaded = true
  }

  private fun AssistanceRequest.toLocalHistoryEntry(): VolunteerDashboardLocalHistoryEntry {
    return VolunteerDashboardLocalHistoryEntry(
      id = id,
      userName = userName,
      userType = userType,
      location = location,
      helpType = helpType,
      completedTime = "Just now",
      rating = if (
        status == RequestStatus.Completed ||
        status == RequestStatus.Rated ||
        status == RequestStatus.Archived
      ) 0 else 0
    )
  }
}
