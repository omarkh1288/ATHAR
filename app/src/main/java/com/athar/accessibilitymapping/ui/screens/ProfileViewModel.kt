package com.athar.accessibilitymapping.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.athar.accessibilitymapping.data.AtharRepository
import com.athar.accessibilitymapping.data.ContributionStats
import com.athar.accessibilitymapping.data.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = AtharRepository(application)

  private val _profile = MutableStateFlow(
    UserProfile(
      name = "",
      email = "",
      phone = "",
      disabilityType = "",
      memberSince = "",
      contributionStats = ContributionStats(0, 0, 0)
    )
  )
  val profile: StateFlow<UserProfile> = _profile

  private val _isLoaded = MutableStateFlow(false)
  val isLoaded: StateFlow<Boolean> = _isLoaded

  init {
    loadProfile()
  }

  private fun loadProfile() {
    viewModelScope.launch {
      val loaded = repository.getUserProfile()
      _profile.value = loaded
      _isLoaded.value = true
    }
  }

  fun refresh() {
    _isLoaded.value = false
    loadProfile()
  }
}
