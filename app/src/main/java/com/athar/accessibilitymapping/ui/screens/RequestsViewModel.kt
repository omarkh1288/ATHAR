package com.athar.accessibilitymapping.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.athar.accessibilitymapping.data.AtharRepository
import com.athar.accessibilitymapping.data.VolunteerRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RequestsViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = AtharRepository(application)

  private val _requests = MutableStateFlow<List<VolunteerRequest>>(emptyList())
  val requests: StateFlow<List<VolunteerRequest>> = _requests

  private var pollingStarted = false

  fun startPollingIfNeeded() {
    if (pollingStarted) return
    pollingStarted = true
    viewModelScope.launch {
      while (true) {
        _requests.value = repository.getRequests()
        delay(30_000)
      }
    }
  }

  fun refreshNow() {
    viewModelScope.launch {
      _requests.value = repository.getRequests()
    }
  }
}
