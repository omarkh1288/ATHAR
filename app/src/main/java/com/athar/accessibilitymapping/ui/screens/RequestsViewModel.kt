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

  // Track locally-confirmed paid request IDs so polling doesn't revert them
  private val locallyPaidRequestIds = mutableSetOf<String>()

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

  fun markRequestPaid(requestId: String) {
    locallyPaidRequestIds.add(requestId)
    _requests.value = _requests.value.map { req ->
      if (req.id == requestId) req.copy(isPaid = true, paymentStatus = "success")
      else req
    }
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
      val serverRequests = repository.getRequests()
      // Merge: preserve local paid status for requests the server hasn't caught up on yet
      _requests.value = serverRequests.map { req ->
        if (req.id in locallyPaidRequestIds) {
          if (req.isPaid || req.paymentStatus?.lowercase() in setOf("success", "succeeded", "paid", "completed", "captured")) {
            // Server caught up — remove from local override
            locallyPaidRequestIds.remove(req.id)
            req
          } else {
            // Server hasn't caught up yet — keep optimistic state
            req.copy(isPaid = true, paymentStatus = "success")
          }
        } else {
          req
        }
      }
    } finally {
      if (showRefreshing) {
        _isRefreshing.value = false
      }
    }
  }
}
