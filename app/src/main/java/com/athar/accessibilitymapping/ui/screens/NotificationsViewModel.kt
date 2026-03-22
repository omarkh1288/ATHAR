package com.athar.accessibilitymapping.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.athar.accessibilitymapping.data.ApiCallResult
import com.athar.accessibilitymapping.data.AtharRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NotificationsViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = AtharRepository(application)

  private val _pushEnabled = MutableStateFlow(true)
  val pushEnabled: StateFlow<Boolean> = _pushEnabled

  private val _emailEnabled = MutableStateFlow(true)
  val emailEnabled: StateFlow<Boolean> = _emailEnabled

  private val _smsEnabled = MutableStateFlow(false)
  val smsEnabled: StateFlow<Boolean> = _smsEnabled

  private val _volunteerRequests = MutableStateFlow(true)
  val volunteerRequests: StateFlow<Boolean> = _volunteerRequests

  private val _volunteerAccepted = MutableStateFlow(true)
  val volunteerAccepted: StateFlow<Boolean> = _volunteerAccepted

  private val _locationUpdates = MutableStateFlow(true)
  val locationUpdates: StateFlow<Boolean> = _locationUpdates

  private val _newRatings = MutableStateFlow(true)
  val newRatings: StateFlow<Boolean> = _newRatings

  private val _communityUpdates = MutableStateFlow(false)
  val communityUpdates: StateFlow<Boolean> = _communityUpdates

  private val _marketingEmails = MutableStateFlow(false)
  val marketingEmails: StateFlow<Boolean> = _marketingEmails

  private val _soundEnabled = MutableStateFlow(true)
  val soundEnabled: StateFlow<Boolean> = _soundEnabled

  private val _vibrationEnabled = MutableStateFlow(true)
  val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled

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
          val settings = result.data.notificationSettings
          _pushEnabled.value = settings.pushEnabled
          _emailEnabled.value = settings.emailEnabled
          _smsEnabled.value = settings.smsEnabled
          _volunteerRequests.value = settings.volunteerRequests
          _volunteerAccepted.value = settings.volunteerAccepted
          _locationUpdates.value = settings.locationUpdates
          _newRatings.value = settings.newRatings
          _communityUpdates.value = settings.communityUpdates
          _marketingEmails.value = settings.marketingEmails
          _soundEnabled.value = settings.soundEnabled
          _vibrationEnabled.value = settings.vibrationEnabled
        }
        is ApiCallResult.Failure -> {
          _errorMessage.value = result.message
        }
      }
      _isLoading.value = false
    }
  }

  fun togglePush() { _pushEnabled.value = !_pushEnabled.value }
  fun toggleEmail() { _emailEnabled.value = !_emailEnabled.value }
  fun toggleSms() { _smsEnabled.value = !_smsEnabled.value }
  fun toggleVolunteerRequests() { _volunteerRequests.value = !_volunteerRequests.value }
  fun toggleVolunteerAccepted() { _volunteerAccepted.value = !_volunteerAccepted.value }
  fun toggleLocationUpdates() { _locationUpdates.value = !_locationUpdates.value }
  fun toggleNewRatings() { _newRatings.value = !_newRatings.value }
  fun toggleCommunityUpdates() { _communityUpdates.value = !_communityUpdates.value }
  fun toggleMarketingEmails() { _marketingEmails.value = !_marketingEmails.value }
  fun toggleSound() { _soundEnabled.value = !_soundEnabled.value }
  fun toggleVibration() { _vibrationEnabled.value = !_vibrationEnabled.value }
}
