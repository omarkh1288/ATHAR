package com.athar.accessibilitymapping.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.athar.accessibilitymapping.data.ApiCallResult
import com.athar.accessibilitymapping.data.AtharRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PrivacySecurityViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = AtharRepository(application)

  private val _locationSharing = MutableStateFlow(true)
  val locationSharing: StateFlow<Boolean> = _locationSharing

  private val _profileVisibility = MutableStateFlow(true)
  val profileVisibility: StateFlow<Boolean> = _profileVisibility

  private val _showRatings = MutableStateFlow(true)
  val showRatings: StateFlow<Boolean> = _showRatings

  private val _activityStatus = MutableStateFlow(true)
  val activityStatus: StateFlow<Boolean> = _activityStatus

  private val _twoFactorAuth = MutableStateFlow(false)
  val twoFactorAuth: StateFlow<Boolean> = _twoFactorAuth

  private val _isLoading = MutableStateFlow(true)
  val isLoading: StateFlow<Boolean> = _isLoading

  private val _errorMessage = MutableStateFlow<String?>(null)
  val errorMessage: StateFlow<String?> = _errorMessage

  private var loaded = false

  fun loadIfNeeded() {
    if (loaded) return
    loaded = true
    viewModelScope.launch {
      when (val result = repository.getCurrentAccount()) {
        is ApiCallResult.Success -> {
          val settings = result.data.privacySettings
          _locationSharing.value = settings.locationSharing
          _profileVisibility.value = settings.profileVisibility
          _showRatings.value = settings.showRatings
          _activityStatus.value = settings.activityStatus
          _twoFactorAuth.value = settings.twoFactorAuth
        }
        is ApiCallResult.Failure -> {
          _errorMessage.value = result.message
        }
      }
      _isLoading.value = false
    }
  }

  fun toggleLocationSharing() { _locationSharing.value = !_locationSharing.value }
  fun toggleProfileVisibility() { _profileVisibility.value = !_profileVisibility.value }
  fun toggleShowRatings() { _showRatings.value = !_showRatings.value }
  fun toggleActivityStatus() { _activityStatus.value = !_activityStatus.value }
  fun toggleTwoFactorAuth() { _twoFactorAuth.value = !_twoFactorAuth.value }
}
