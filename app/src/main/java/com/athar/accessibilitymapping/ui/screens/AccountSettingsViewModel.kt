package com.athar.accessibilitymapping.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.athar.accessibilitymapping.data.ApiCallResult
import com.athar.accessibilitymapping.data.ApiUpdateProfileRequest
import com.athar.accessibilitymapping.data.AtharRepository
import com.athar.accessibilitymapping.data.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AccountSettingsViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = AtharRepository(application)

  private val _name = MutableStateFlow("")
  val name: StateFlow<String> = _name

  private val _email = MutableStateFlow("")
  val email: StateFlow<String> = _email

  private val _phone = MutableStateFlow("")
  val phone: StateFlow<String> = _phone

  private val _address = MutableStateFlow("")
  val address: StateFlow<String> = _address

  private val _disabilityType = MutableStateFlow("")
  val disabilityType: StateFlow<String> = _disabilityType

  private val _passwordChangedAt = MutableStateFlow<String?>(null)
  val passwordChangedAt: StateFlow<String?> = _passwordChangedAt

  private val _isLoading = MutableStateFlow(true)
  val isLoading: StateFlow<Boolean> = _isLoading

  private val _loadError = MutableStateFlow<String?>(null)
  val loadError: StateFlow<String?> = _loadError

  private val _isSaving = MutableStateFlow(false)
  val isSaving: StateFlow<Boolean> = _isSaving

  private val _saveError = MutableStateFlow<String?>(null)
  val saveError: StateFlow<String?> = _saveError

  private val _saveMessage = MutableStateFlow<String?>(null)
  val saveMessage: StateFlow<String?> = _saveMessage

  private var loaded = false

  fun loadIfNeeded(userDisabilityType: String?, userRole: UserRole) {
    if (loaded) return
    loaded = true
    _disabilityType.value = userDisabilityType ?: ""
    viewModelScope.launch {
      when (val result = repository.getCurrentAccount()) {
        is ApiCallResult.Success -> {
          _name.value = result.data.fullName
          _email.value = result.data.email
          _phone.value = result.data.phone
          _address.value = result.data.location
          _passwordChangedAt.value = result.data.passwordChangedAt
          if (userRole == UserRole.User) {
            _disabilityType.value = result.data.disabilityType.orEmpty()
          }
        }
        is ApiCallResult.Failure -> {
          _loadError.value = result.message
        }
      }
      _isLoading.value = false
    }
  }

  fun updateName(value: String) { _name.value = value }
  fun updatePhone(value: String) { _phone.value = value }
  fun updateAddress(value: String) { _address.value = value }
  fun updateDisabilityType(value: String) { _disabilityType.value = value }

  fun save(userRole: UserRole) {
    _saveError.value = null
    _saveMessage.value = null
    _isSaving.value = true
    viewModelScope.launch {
      when (
        val result = repository.updateProfile(
          ApiUpdateProfileRequest(
            fullName = _name.value.trim(),
            phone = _phone.value.trim(),
            location = _address.value.trim(),
            disabilityType = if (userRole == UserRole.User) _disabilityType.value.trim() else null
          )
        )
      ) {
        is ApiCallResult.Success -> _saveMessage.value = "Account settings updated successfully!"
        is ApiCallResult.Failure -> _saveError.value = result.message
      }
      _isSaving.value = false
    }
  }
}
