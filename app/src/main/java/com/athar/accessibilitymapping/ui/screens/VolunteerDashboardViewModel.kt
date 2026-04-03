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

  fun loadIfNeeded(isVolunteerLive: Boolean) {
    viewModelScope.launch {
      ensureImpactLoaded(forceRefresh = false)
      if (isVolunteerLive) {
        ensureTabLoaded(_uiState.value.activeTab, forceRefresh = false)
      } else if (!historyLoaded) {
        // Offline mode still shows prior stats/history from cached or persisted data.
        refreshHistoryData(forceRefresh = false)
      }
    }
  }

  fun selectTab(tab: VolunteerDashboardTab, isVolunteerLive: Boolean) {
    _uiState.update { it.copy(activeTab = tab, actionError = null) }
    if (isVolunteerLive) {
      viewModelScope.launch {
        ensureTabLoaded(tab, forceRefresh = false)
      }
    }
  }

  fun refresh(isVolunteerLive: Boolean) {
    viewModelScope.launch {
      _uiState.update { it.copy(isRefreshing = true, actionError = null) }
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
      _uiState.update { it.copy(actionError = null, isRefreshing = true) }
      try {
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
      _uiState.update { it.copy(actionError = null, isRefreshing = true) }
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

  fun completeAcceptedRequest() {
    val completedRequest = _uiState.value.activeRequest ?: return
    viewModelScope.launch {
      _uiState.update { it.copy(actionError = null, isRefreshing = true) }
      try {
        when (val result = repository.completeRequest(completedRequest.id)) {
          is ApiCallResult.Success -> {
            // Completing a request changes earnings, payment history, and weekly analytics.
            AtharVolunteerDashboardRepository.clearCachedDashboards()
            _uiState.update {
              it.copy(
                activeTab = VolunteerDashboardTab.HISTORY,
                activeRequest = null,
                localCompletedHistory = listOf(completedRequest.toLocalHistoryEntry()) +
                  it.localCompletedHistory.filterNot { existing -> existing.id == completedRequest.id }
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
            _uiState.update { it.copy(actionError = result.message) }
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
    val incoming = repository.getVolunteerIncomingDashboard(perPage = 50)
    val currentAcceptedId = _uiState.value.activeRequest?.id
    val alertCount = if (incoming.incomingAlert.count > 0) {
      incoming.incomingAlert.count
    } else {
      incoming.requests.size
    }
    _uiState.update {
      it.copy(
        tabCounts = incoming.counts,
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
    _uiState.update {
      it.copy(
        tabCounts = active.counts,
        activeStatusBanner = active.statusBanner.ifBlank { "Assistance in Progress" },
        activeRequest = active.requests.firstOrNull { request -> request.id == currentAcceptedId }
          ?: _uiState.value.activeRequest
          ?: active.requests.firstOrNull()
      )
    }
    activeLoaded = true
  }

  private suspend fun refreshHistoryData(forceRefresh: Boolean) {
    val history = repository.getVolunteerHistoryDashboard(perPage = 50)
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
