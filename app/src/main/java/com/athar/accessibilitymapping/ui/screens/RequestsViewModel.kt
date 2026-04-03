package com.athar.accessibilitymapping.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.athar.accessibilitymapping.data.AtharRepository
import com.athar.accessibilitymapping.data.VolunteerRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RequestsViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = AtharRepository(application)

  private val _requests = MutableStateFlow<List<VolunteerRequest>>(emptyList())
  val requests: StateFlow<List<VolunteerRequest>> = _requests.asStateFlow()

  private val _isRefreshing = MutableStateFlow(false)
  val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

  private var pollingStarted = false

  fun startPollingIfNeeded() {
    if (pollingStarted) return
    pollingStarted = true
    viewModelScope.launch {
      while (true) {
        fetchRequests(showRefreshing = false)
        delay(30_000)
      }
    }
  }

  fun refreshNow() {
    refresh(showRefreshing = false)
  }

  fun refresh(showRefreshing: Boolean = true) {
    viewModelScope.launch {
      fetchRequests(showRefreshing = showRefreshing)
    }
  }

  private suspend fun fetchRequests(showRefreshing: Boolean) {
    if (showRefreshing) {
      _isRefreshing.value = true
    }
    try {
      _requests.value = repository.getRequests()
    } finally {
      if (showRefreshing) {
        _isRefreshing.value = false
      }
    }
  }
}
