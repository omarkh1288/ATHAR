package com.athar.accessibilitymapping.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.athar.accessibilitymapping.data.AtharRepository
import com.athar.accessibilitymapping.data.VolunteerRequest
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

  // Track locally-confirmed request updates so polling does not revert them.
  private val locallyPaidRequestIds = mutableSetOf<String>()
  private val locallyRatedRequestIds = mutableSetOf<String>()

  fun refreshNow() {
    refresh(showRefreshing = false)
  }

  fun markRequestPaid(requestId: String) {
    locallyPaidRequestIds.add(requestId)
    _requests.value = _requests.value.map { req ->
      if (req.id == requestId) req.copy(isPaid = true, paymentStatus = "success") else req
    }
  }

  fun markRequestRated(requestId: String) {
    locallyRatedRequestIds.add(requestId)
    _requests.value = _requests.value.map { req ->
      if (req.id == requestId) req.copy(status = "rated") else req
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
      _requests.value = serverRequests.map { req ->
        val paidMerged = if (req.id in locallyPaidRequestIds) {
          if (req.isPaid || req.paymentStatus?.lowercase() in setOf("success", "succeeded", "paid", "completed", "captured")) {
            locallyPaidRequestIds.remove(req.id)
            req
          } else {
            req.copy(isPaid = true, paymentStatus = "success")
          }
        } else {
          req
        }

        if (paidMerged.id in locallyRatedRequestIds) {
          if (paidMerged.status.trim().lowercase() == "rated") {
            locallyRatedRequestIds.remove(paidMerged.id)
            paidMerged
          } else {
            paidMerged.copy(status = "rated")
          }
        } else {
          paidMerged
        }
      }
    } finally {
      if (showRefreshing) {
        _isRefreshing.value = false
      }
    }
  }
}
