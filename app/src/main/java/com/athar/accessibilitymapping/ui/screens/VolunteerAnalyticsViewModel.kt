package com.athar.accessibilitymapping.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.athar.accessibilitymapping.data.volunteer.AtharDataResult
import com.athar.accessibilitymapping.data.volunteer.AtharVolunteerDashboardRepository
import com.athar.accessibilitymapping.data.volunteer.VolunteerAnalyticsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VolunteerAnalyticsViewModel(application: Application) : AndroidViewModel(application) {
  private val repository = AtharVolunteerDashboardRepository(application)

  private val _uiState = MutableStateFlow<VolunteerAnalyticsUiState>(VolunteerAnalyticsUiState.Loading)
  val uiState: StateFlow<VolunteerAnalyticsUiState> = _uiState.asStateFlow()

  init {
    refresh()
  }

  fun refresh() {
    viewModelScope.launch {
      _uiState.value = VolunteerAnalyticsUiState.Loading
      _uiState.value = when (val result = repository.loadDashboard()) {
        is AtharDataResult.Loading -> VolunteerAnalyticsUiState.Loading
        is AtharDataResult.Error -> VolunteerAnalyticsUiState.Error(result.message)
        is AtharDataResult.Success -> {
          if (result.data.isMeaningfullyEmpty()) {
            VolunteerAnalyticsUiState.Empty(
              model = result.data,
              message = "No volunteer analytics available yet.",
              warnings = result.warnings
            )
          } else {
            VolunteerAnalyticsUiState.Content(
              model = result.data,
              warnings = result.warnings
            )
          }
        }
      }
    }
  }
}
