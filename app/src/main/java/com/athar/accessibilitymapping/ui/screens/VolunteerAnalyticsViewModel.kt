package com.athar.accessibilitymapping.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.athar.accessibilitymapping.data.volunteer.AtharDataResult
import com.athar.accessibilitymapping.data.volunteer.AtharVolunteerDashboardRepository
import com.athar.accessibilitymapping.data.volunteer.VolunteerAnalyticsRequest
import com.athar.accessibilitymapping.data.volunteer.VolunteerAnalyticsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VolunteerAnalyticsViewModel(application: Application) : AndroidViewModel(application) {
  private val repository = AtharVolunteerDashboardRepository(application)
  private var currentRequest = VolunteerAnalyticsRequest()

  private val _uiState = MutableStateFlow<VolunteerAnalyticsUiState>(
    repository.peekCachedDashboard()?.toUiState() ?: VolunteerAnalyticsUiState.Loading
  )
  val uiState: StateFlow<VolunteerAnalyticsUiState> = _uiState.asStateFlow()

  init {
    loadIfNeeded()
  }

  fun loadIfNeeded(request: VolunteerAnalyticsRequest = currentRequest) {
    currentRequest = request
    val cached = repository.peekCachedDashboard(request)
    if (cached != null) {
      _uiState.value = cached.toUiState()
      return
    }
    refresh(forceRefresh = false, request = request)
  }

  fun refresh(
    forceRefresh: Boolean = true,
    request: VolunteerAnalyticsRequest = currentRequest
  ) {
    currentRequest = request
    viewModelScope.launch {
      val previousState = _uiState.value
      _uiState.value = when (previousState) {
        is VolunteerAnalyticsUiState.Content -> previousState.copy(isRefreshing = true)
        is VolunteerAnalyticsUiState.Empty -> previousState.copy(isRefreshing = true)
        else -> VolunteerAnalyticsUiState.Loading
      }
      _uiState.value = when (val result = repository.loadDashboard(request = request, forceRefresh = forceRefresh)) {
        is AtharDataResult.Loading -> VolunteerAnalyticsUiState.Loading
        is AtharDataResult.Error -> when (previousState) {
          is VolunteerAnalyticsUiState.Content -> previousState.copy(isRefreshing = false)
          is VolunteerAnalyticsUiState.Empty -> previousState.copy(isRefreshing = false)
          else -> VolunteerAnalyticsUiState.Error(result.message)
        }
        is AtharDataResult.Success -> {
          result.toUiState()
        }
      }
    }
  }

  fun invalidateCache() {
    repository.invalidateDashboardCache(currentRequest)
  }

  private fun AtharDataResult.Success<com.athar.accessibilitymapping.data.volunteer.VolunteerDashboardUiModel>.toUiState(): VolunteerAnalyticsUiState {
    return if (data.isMeaningfullyEmpty()) {
      VolunteerAnalyticsUiState.Empty(
        model = data,
        message = "No volunteer analytics available yet.",
        warnings = warnings,
        isRefreshing = false
      )
    } else {
      VolunteerAnalyticsUiState.Content(
        model = data,
        warnings = warnings,
        isRefreshing = false
      )
    }
  }
}
