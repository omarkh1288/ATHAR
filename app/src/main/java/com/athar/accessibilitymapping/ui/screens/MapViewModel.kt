package com.athar.accessibilitymapping.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.athar.accessibilitymapping.data.AtharRepository
import com.athar.accessibilitymapping.data.Location
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MapViewModel(application: Application) : AndroidViewModel(application) {
  companion object {
    private const val DEFAULT_NEARBY_RADIUS_KM = 20
  }

  val repository = AtharRepository(application)

  private val _locations = MutableStateFlow<List<Location>>(emptyList())
  val locations: StateFlow<List<Location>> = _locations

  private val _isLoaded = MutableStateFlow(false)
  val isLoaded: StateFlow<Boolean> = _isLoaded

  init {
    loadLocations()
  }

  private fun loadLocations(
    lat: Double? = null,
    lng: Double? = null,
    radiusKm: Int = DEFAULT_NEARBY_RADIUS_KM
  ) {
    viewModelScope.launch {
      _locations.value = repository.getLocations(lat = lat, lng = lng, radiusKm = radiusKm)
      _isLoaded.value = true
    }
  }

  fun refresh(
    lat: Double? = null,
    lng: Double? = null,
    radiusKm: Int = DEFAULT_NEARBY_RADIUS_KM
  ) {
    _isLoaded.value = false
    loadLocations(lat = lat, lng = lng, radiusKm = radiusKm)
  }
}
