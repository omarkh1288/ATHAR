package com.athar.accessibilitymapping.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.DoorFront
import androidx.compose.material.icons.outlined.Wc
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.athar.accessibilitymapping.ui.theme.sdp
import com.athar.accessibilitymapping.ui.theme.ssp
import androidx.core.app.ActivityCompat
import com.athar.accessibilitymapping.data.ApiCallResult
import com.athar.accessibilitymapping.data.ApiLocationReportRequest
import com.athar.accessibilitymapping.data.AtharRepository
import com.athar.accessibilitymapping.ui.components.Governorate
import com.athar.accessibilitymapping.ui.components.GovernorateSelector
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import android.location.Geocoder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

object AtharColors {
  val Secondary = Color(0xFF1F3C5B)
  val SecondaryDark = Color(0xFF2C4F73)
  val Accent = Color(0xFFC9A24D)
  val AccentDark = Color(0xFFB38F3D)
  val BgPrimary = Color(0xFFEAF2FB)
  val BgSecondary = Color(0xFFD6E4F5)
  val Gray200 = Color(0xFFE2E8F0)
  val White = Color(0xFFFFFFFF)
  val ErrorRed = Color(0xFFDC2626)
}

data class AccessibilityFeature(val id: String, val label: String, val icon: ImageVector)

val accessibilityFeatures = listOf(
  AccessibilityFeature("ramp", "Ramps", Icons.Default.TrendingUp),
  AccessibilityFeature("elevator", "Elevators", Icons.Default.SwapVert),
  AccessibilityFeature("parking", "Accessible Parking", Icons.Default.DirectionsCar),
  AccessibilityFeature("braille", "Braille", Icons.Default.Fingerprint),
  AccessibilityFeature("toilet", "Accessible Toilet", Icons.Outlined.Wc),
  AccessibilityFeature("wideEntrance", "Wide Entrance", Icons.Outlined.DoorFront)
)

data class LocationSuggestion(val name: String, val lat: Double, val lng: Double)

val mockSuggestions = listOf(
  LocationSuggestion("City Stars Mall", 30.0726, 31.3498),
  LocationSuggestion("Giza Public Library", 30.0131, 31.2189),
  LocationSuggestion("Orman Garden", 30.0350, 31.2136),
  LocationSuggestion("Giza Medical Center", 30.0089, 31.2050),
  LocationSuggestion("Nile View Café", 30.0195, 31.2234),
  LocationSuggestion("Egyptian Museum", 30.0478, 31.2336),
  LocationSuggestion("Al-Azhar Park", 30.0444, 31.2672),
  LocationSuggestion("Cairo Tower", 30.0459, 31.2243),
  LocationSuggestion("Tahrir Square", 30.0444, 31.2357),
  LocationSuggestion("Ramses Station", 30.0644, 31.2490)
)

val ratingLabels = listOf("", "Poor", "Fair", "Good", "Very Good", "Excellent")

@Composable
fun AddPlaceReportButton(onClick: () -> Unit) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val bgColor = if (isPressed) AtharColors.SecondaryDark else AtharColors.Secondary

  Box(modifier = Modifier.padding(horizontal = 16.dp)) {
    Button(
      onClick = onClick,
      modifier = Modifier.fillMaxWidth().height(64.dp),
      shape = RoundedCornerShape(16.dp),
      colors = ButtonDefaults.buttonColors(containerColor = bgColor),
      border = BorderStroke(2.dp, AtharColors.SecondaryDark),
      elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
      interactionSource = interactionSource,
      contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .padding(start = 28.dp)
            .size(34.dp)
            .clip(CircleShape)
            .background(AtharColors.Accent)
            .border(2.dp, AtharColors.AccentDark, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = AtharColors.White,
            modifier = Modifier.size(20.dp)
          )
        }
        Spacer(Modifier.width(12.dp))
        Text(
          text = "Add a Place Report",
          color = AtharColors.White,
          fontSize = 16.sp,
          fontWeight = FontWeight.SemiBold
        )
      }
    }
  }
}

@Composable
fun AddPlaceReportScreen(onBack: () -> Unit) {
  var mapCenter by remember { mutableStateOf(Pair(30.0131, 31.2089)) }
  var rating by remember { mutableIntStateOf(0) }
  var hoverRating by remember { mutableIntStateOf(0) }
  var selectedFeatures by remember { mutableStateOf(setOf<String>()) }
  var searchQuery by remember { mutableStateOf("") }
  var showSuggestions by remember { mutableStateOf(false) }
  var selectedLocation by remember { mutableStateOf("") }
  var isLocating by remember { mutableStateOf(false) }
  var isPinning by remember { mutableStateOf(false) }
  var showFullScreenMap by remember { mutableStateOf(false) }
  var locationError by remember { mutableStateOf("") }
  var submitted by remember { mutableStateOf(false) }
  var selectedGovernorate by remember { mutableStateOf<Governorate?>(null) }
  var submitError by remember { mutableStateOf("") }
  var isSubmitting by remember { mutableStateOf(false) }

  val context = LocalContext.current
  val repository = remember(context) { AtharRepository(context) }
  val coroutineScope = rememberCoroutineScope()
  var suggestions by remember { mutableStateOf(listOf<LocationSuggestion>()) }
  var isSearching by remember { mutableStateOf(false) }
  val canSubmit = selectedLocation.isNotEmpty() && rating > 0 && selectedGovernorate != null && !isSubmitting
  val mapLatLng = remember(mapCenter) { LatLng(mapCenter.first, mapCenter.second) }
  val markerState = remember { MarkerState(position = mapLatLng) }
  val cameraPositionState = rememberCameraPositionState {
    position = CameraPosition.fromLatLngZoom(mapLatLng, 14f)
  }
  val hasFineLocationPermission = ActivityCompat.checkSelfPermission(
    context,
    Manifest.permission.ACCESS_FINE_LOCATION
  ) == PackageManager.PERMISSION_GRANTED

  LaunchedEffect(mapLatLng) {
    markerState.position = mapLatLng
    cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(mapLatLng, 14f))
  }

  LaunchedEffect(searchQuery) {
    if (searchQuery.length < 2 || searchQuery == selectedLocation) {
      if (searchQuery.isEmpty()) suggestions = emptyList()
      return@LaunchedEffect
    }
    delay(350)
    isSearching = true
    val results = fetchPlaceSuggestions(searchQuery, mapLatLng)
    suggestions = results
    isSearching = false
    showSuggestions = results.isNotEmpty()
  }

  val locationCallback = remember {
    object : LocationCallback() {
      override fun onLocationResult(result: LocationResult) {
        val loc = result.lastLocation ?: return
        mapCenter = Pair(loc.latitude, loc.longitude)
        selectedLocation = "Your Current Location"
        searchQuery = "Your Current Location"
        isLocating = false
        showSuggestions = false
      }
    }
  }

  fun requestGpsLocation() {
    isLocating = true
    locationError = ""
    isPinning = false
    if (
      ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
      ) != PackageManager.PERMISSION_GRANTED
    ) {
      locationError = "Location permission not granted."
      isLocating = false
      return
    }

    val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    fusedClient.lastLocation
      .addOnSuccessListener { loc ->
        if (loc != null) {
          mapCenter = Pair(loc.latitude, loc.longitude)
          selectedLocation = "Your Current Location"
          searchQuery = "Your Current Location"
        } else {
          locationError = "Unable to fetch location. Please enable GPS."
        }
        isLocating = false
      }
      .addOnFailureListener {
        locationError = "Unable to fetch location. Please try again."
        isLocating = false
      }
  }

  remember(locationCallback) { locationCallback }

  if (submitted) {
    LaunchedEffect(Unit) {
      delay(1800)
      onBack()
    }

    Box(
      modifier = Modifier.fillMaxSize().background(AtharColors.BgPrimary),
      contentAlignment = Alignment.Center
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 24.dp)
      ) {
        Box(
          modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(AtharColors.Secondary)
            .border(2.dp, AtharColors.SecondaryDark, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = AtharColors.White,
            modifier = Modifier.size(48.dp)
          )
        }
        Spacer(Modifier.height(20.dp))
        Text(
          text = "Report Submitted!",
          fontSize = 20.sp,
          fontWeight = FontWeight.SemiBold,
          color = AtharColors.Secondary
        )
        Spacer(Modifier.height(8.dp))
        Text(
          text = "Thank you for helping improve accessibility for everyone.",
          fontSize = 16.sp,
          color = AtharColors.Secondary.copy(alpha = 0.75f),
          textAlign = TextAlign.Center,
          modifier = Modifier.widthIn(max = 280.dp)
        )
      }
    }
    return
  }

  Column(modifier = Modifier.fillMaxSize().background(AtharColors.BgPrimary)) {
    Surface(color = AtharColors.Secondary, shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .statusBarsPadding()
          .padding(horizontal = 16.dp)
          .padding(top = 20.dp, bottom = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AtharColors.SecondaryDark)
            .border(2.dp, AtharColors.SecondaryDark, RoundedCornerShape(12.dp))
        ) {
          Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = "Go back",
            tint = AtharColors.White,
            modifier = Modifier.size(20.dp)
          )
        }
        Text(
          text = "Add Place Report",
          fontSize = 30.sp,
          fontWeight = FontWeight.Bold,
          color = AtharColors.White
        )
      }
    }

    // Fullscreen map dialog for pin location
    if (showFullScreenMap) {
      Dialog(
        onDismissRequest = {
          showFullScreenMap = false
          isPinning = false
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
      ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
          val fullScreenCameraState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(
              LatLng(mapCenter.first, mapCenter.second), 14f
            )
          }

          GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = fullScreenCameraState,
            properties = MapProperties(
              isMyLocationEnabled = hasFineLocationPermission
            ),
            uiSettings = MapUiSettings(
              zoomControlsEnabled = true,
              myLocationButtonEnabled = hasFineLocationPermission
            ),
            onMapClick = { latLng ->
              mapCenter = Pair(latLng.latitude, latLng.longitude)
              showSuggestions = false
              isPinning = false
              showFullScreenMap = false
              coroutineScope.launch {
                val address = reverseGeocode(context, latLng.latitude, latLng.longitude)
                selectedLocation = address
                searchQuery = address
              }
            }
          ) {
            Marker(
              state = MarkerState(position = LatLng(mapCenter.first, mapCenter.second)),
              title = if (selectedLocation.isNotBlank()) selectedLocation else "Selected Location"
            )
          }

          // Top bar with close button and instructions
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .statusBarsPadding()
              .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = AtharColors.Secondary.copy(alpha = 0.85f),
              shadowElevation = 4.dp
            ) {
              Text(
                text = "Tap on the map to pin a location",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
              )
            }

            Surface(
              shape = CircleShape,
              color = AtharColors.Secondary.copy(alpha = 0.85f),
              shadowElevation = 4.dp,
              modifier = Modifier
                .size(40.dp)
                .clickable {
                  showFullScreenMap = false
                  isPinning = false
                }
            ) {
              Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = "Close",
                  tint = Color.White,
                  modifier = Modifier.size(22.dp)
                )
              }
            }
          }
        }
      }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().background(AtharColors.BgPrimary)) {
      item {
        Column(
          modifier = Modifier.padding(horizontal = 16.dp).padding(top = 20.dp, bottom = 8.dp)
        ) {
          Text(
            text = "Select Location",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = AtharColors.Secondary,
            modifier = Modifier.padding(bottom = 12.dp)
          )

          Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            // Use my location button
            Button(
              onClick = { requestGpsLocation() },
              enabled = !isLocating,
              modifier = Modifier.weight(1f).height(54.dp),
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = AtharColors.Secondary,
                disabledContainerColor = AtharColors.SecondaryDark
              ),
              border = BorderStroke(1.5.dp, AtharColors.SecondaryDark),
              elevation = ButtonDefaults.buttonElevation(4.dp),
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.MyLocation,
                  contentDescription = null,
                  tint = AtharColors.White,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                  text = if (isLocating) "Locating…" else "Use my location",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = AtharColors.White,
                  maxLines = 1
                )
              }
            }

            // Pin Location button
            Button(
              onClick = {
                isPinning = true
                showFullScreenMap = true
                locationError = ""
              },
              modifier = Modifier.weight(1f).height(54.dp),
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(containerColor = AtharColors.White),
              border = BorderStroke(1.5.dp, AtharColors.Secondary),
              elevation = ButtonDefaults.buttonElevation(4.dp),
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.PushPin,
                  contentDescription = null,
                  tint = AtharColors.Secondary,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                  text = "Pin Location",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = AtharColors.Secondary,
                  maxLines = 1
                )
              }
            }
          }

          if (locationError.isNotEmpty()) {
            Text(
              text = locationError,
              fontSize = 14.sp,
              color = AtharColors.ErrorRed,
              modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
            )
          }

          Box {
            OutlinedTextField(
              value = searchQuery,
              onValueChange = {
                searchQuery = it
                showSuggestions = true
              },
              modifier = Modifier.fillMaxWidth(),
              placeholder = {
                Text(
                  "Search for a location…",
                  fontSize = 16.sp,
                  color = AtharColors.Secondary.copy(alpha = 0.5f)
                )
              },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Default.Search,
                  contentDescription = null,
                  tint = AtharColors.Secondary,
                  modifier = Modifier.size(20.dp)
                )
              },
              trailingIcon = {
                if (isSearching) {
                  androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = AtharColors.Secondary
                  )
                } else if (searchQuery.isNotEmpty()) {
                  IconButton(
                    onClick = {
                      searchQuery = ""
                      selectedLocation = ""
                      showSuggestions = false
                      suggestions = emptyList()
                    }
                  ) {
                    Icon(
                      imageVector = Icons.Default.Close,
                      contentDescription = "Clear",
                      tint = AtharColors.Secondary,
                      modifier = Modifier.size(16.dp)
                    )
                  }
                }
              },
              singleLine = true,
              shape = RoundedCornerShape(16.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AtharColors.Secondary,
                unfocusedBorderColor = AtharColors.Secondary,
                focusedContainerColor = AtharColors.White,
                unfocusedContainerColor = AtharColors.White,
                focusedTextColor = AtharColors.Secondary,
                unfocusedTextColor = AtharColors.Secondary,
                cursorColor = AtharColors.Secondary
              )
            )

            if (showSuggestions && suggestions.isNotEmpty()) {
              Surface(
                modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 8.dp,
                border = BorderStroke(2.dp, AtharColors.Secondary),
                color = AtharColors.White
              ) {
                Column {
                  suggestions.forEachIndexed { index, loc ->
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                          mapCenter = Pair(loc.lat, loc.lng)
                          selectedLocation = loc.name
                          searchQuery = loc.name
                          showSuggestions = false
                          isPinning = false
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                      Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = AtharColors.Accent,
                        modifier = Modifier.size(16.dp)
                      )
                      Text(
                        text = loc.name,
                        fontSize = 14.sp,
                        color = AtharColors.Secondary
                      )
                    }
                    if (index < suggestions.lastIndex) {
                      HorizontalDivider(color = AtharColors.Gray200, thickness = 1.dp)
                    }
                  }
                }
              }
            }
          }

          if (selectedLocation.isNotEmpty()) {
            Row(
              modifier = Modifier.padding(top = 8.dp, start = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = AtharColors.Accent,
                modifier = Modifier.size(16.dp)
              )
              Text(
                text = selectedLocation,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = AtharColors.Secondary
              )
            }
          }
        }
      }

      item {
        Box(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 20.dp)) {
          Card(
            modifier = Modifier.fillMaxWidth().height(220.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(2.dp, AtharColors.Secondary),
            elevation = CardDefaults.cardElevation(8.dp)
          ) {
            GoogleMap(
              modifier = Modifier.fillMaxSize(),
              cameraPositionState = cameraPositionState,
              properties = MapProperties(
                isMyLocationEnabled = hasFineLocationPermission
              ),
              uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = hasFineLocationPermission
              ),
              onMapClick = { latLng ->
                if (isPinning) {
                  mapCenter = Pair(latLng.latitude, latLng.longitude)
                  showSuggestions = false
                  isPinning = false
                  coroutineScope.launch {
                    val address = reverseGeocode(context, latLng.latitude, latLng.longitude)
                    selectedLocation = address
                    searchQuery = address
                  }
                }
              }
            ) {
              Marker(
                state = markerState,
                title = if (selectedLocation.isNotBlank()) selectedLocation else "Selected Location"
              )
            }
          }

          if (isPinning) {
            Box(
              modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(16.dp))
                .background(AtharColors.Secondary.copy(alpha = 0.18f)),
              contentAlignment = Alignment.Center
            ) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(2.dp, AtharColors.Secondary),
                color = AtharColors.White
              ) {
                Text(
                  text = "Tap anywhere to drop pin",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = AtharColors.Secondary,
                  modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
              }
            }
          }
        }
      }
      item {
        GovernorateSelector(
          selectedGovernorate = selectedGovernorate,
          onGovernorateSelected = { selectedGovernorate = it }
        )
      }
      item {
        Card(
          modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
            .fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(2.dp, AtharColors.Gray200),
          colors = CardDefaults.cardColors(containerColor = AtharColors.White),
          elevation = CardDefaults.cardElevation(8.dp)
        ) {
          Column(modifier = Modifier.padding(20.dp)) {
            Text(
              text = "Rate This Place",
              fontSize = 20.sp,
              fontWeight = FontWeight.SemiBold,
              color = AtharColors.Secondary,
              modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
              text = "How accessible is this location?",
              fontSize = 14.sp,
              color = AtharColors.Secondary.copy(alpha = 0.7f),
              modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
              for (star in 1..5) {
                val filled = star <= (hoverRating.takeIf { it > 0 } ?: rating)
                IconButton(onClick = { rating = star }, modifier = Modifier.size(40.dp)) {
                  Icon(
                    imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = "Rate $star star",
                    tint = if (filled) AtharColors.Accent else AtharColors.Gray200,
                    modifier = Modifier.size(40.dp)
                  )
                }
              }
            }

            AnimatedVisibility(visible = rating > 0) {
              Text(
                text = ratingLabels[rating],
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = AtharColors.AccentDark,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
              )
            }
          }
        }
      }


      item {
        Card(
          modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
            .fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(2.dp, AtharColors.Gray200),
          colors = CardDefaults.cardColors(containerColor = AtharColors.White),
          elevation = CardDefaults.cardElevation(8.dp)
        ) {
          Column(modifier = Modifier.padding(20.dp)) {
            Text(
              text = "Accessibility Features",
              fontSize = 20.sp,
              fontWeight = FontWeight.SemiBold,
              color = AtharColors.Secondary,
              modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
              text = "Select all features available at this location.",
              fontSize = 14.sp,
              color = AtharColors.Secondary.copy(alpha = 0.7f),
              modifier = Modifier.padding(bottom = 16.dp)
            )

            val chunkedFeatures = accessibilityFeatures.chunked(2)
            chunkedFeatures.forEach { rowFeatures ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(IntrinsicSize.Min)
                  .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                rowFeatures.forEach { feature ->
                  val isSelected = selectedFeatures.contains(feature.id)
                  val chipBg = if (isSelected) AtharColors.Secondary else AtharColors.BgSecondary
                  val chipBorder = if (isSelected) AtharColors.SecondaryDark else AtharColors.Secondary
                  val chipText = if (isSelected) AtharColors.White else AtharColors.Secondary

                  Surface(
                    modifier = Modifier
                      .weight(1f)
                      .fillMaxHeight()
                      .clickable {
                        selectedFeatures = if (isSelected) {
                          selectedFeatures - feature.id
                        } else {
                          selectedFeatures + feature.id
                        }
                      },
                    shape = RoundedCornerShape(12.dp),
                    color = chipBg,
                    border = BorderStroke(2.dp, chipBorder)
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                      Icon(
                        imageVector = feature.icon,
                        contentDescription = null,
                        tint = chipText,
                        modifier = Modifier.size(20.dp)
                      )
                      Text(
                        text = feature.label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = chipText
                      )
                    }
                  }
                }

                if (rowFeatures.size < 2) {
                  Spacer(modifier = Modifier.weight(1f))
                }
              }
            }
          }
        }
      }

      item {
        Column(
          modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 32.dp)
        ) {
          Button(
            onClick = {
              val governorate = selectedGovernorate ?: return@Button
              submitError = ""
              isSubmitting = true
              val selectedLabels = accessibilityFeatures
                .filter { selectedFeatures.contains(it.id) }
                .joinToString { it.label }
              val request = ApiLocationReportRequest(
                name = selectedLocation.ifBlank { "New Place From Mobile" },
                address = selectedLocation.ifBlank { "Unknown address" },
                governmentId = governorate.id,
                latitude = mapCenter.first,
                longitude = mapCenter.second,
                categoryId = null,
                rating = rating,
                comment = ratingLabels.getOrNull(rating)
                  ?.takeIf { it.isNotBlank() }
                  ?.let { "$it accessibility" },
                rampAvailable = selectedFeatures.contains("ramp"),
                elevatorAvailable = selectedFeatures.contains("elevator"),
                parking = selectedFeatures.contains("parking"),
                wheelchairAccessible = selectedFeatures.contains("ramp") || selectedFeatures.contains("elevator"),
                wideEntrance = selectedFeatures.contains("wideEntrance"),
                accessibleToilet = selectedFeatures.contains("toilet"),
                notes = buildString {
                  append("Submitted from Add Place Report screen")
                  if (selectedLabels.isNotBlank()) {
                    append(" | Selected features: ")
                    append(selectedLabels)
                  }
                }
              )
              coroutineScope.launch {
                when (val result = repository.submitLocationReport(request)) {
                  is ApiCallResult.Success -> {
                    submitted = true
                    submitError = ""
                  }
                  is ApiCallResult.Failure -> {
                    submitError = result.message.ifBlank { "Failed to submit report." }
                  }
                }
                isSubmitting = false
              }
            },
            enabled = canSubmit,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = AtharColors.Secondary,
              disabledContainerColor = AtharColors.Gray200
            ),
            border = BorderStroke(
              2.dp,
              if (canSubmit) AtharColors.SecondaryDark else AtharColors.Gray200
            ),
            elevation = ButtonDefaults.buttonElevation(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
          ) {
            Text(
              text = if (isSubmitting) "Submitting..." else "Submit Report",
              fontSize = 16.sp,
              fontWeight = FontWeight.SemiBold,
              color = if (canSubmit) AtharColors.White else AtharColors.Secondary.copy(alpha = 0.6f)
            )
          }

          if (submitError.isNotBlank()) {
            Text(
              text = submitError,
              fontSize = 14.sp,
              color = AtharColors.ErrorRed,
              textAlign = TextAlign.Center,
              modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
          }

          if (!canSubmit) {
            Text(
              text = "Select a location, governorate, and rating to continue",
              fontSize = 14.sp,
              color = AtharColors.Secondary.copy(alpha = 0.6f),
              textAlign = TextAlign.Center,
              modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
          }
        }
      }
    }
  }
}

private suspend fun fetchPlaceSuggestions(
  query: String,
  reference: LatLng
): List<LocationSuggestion> = withContext(Dispatchers.IO) {
  try {
    val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
    val url = "https://nominatim.openstreetmap.org/search?" +
      "q=$encodedQuery&" +
      "format=json&" +
      "limit=8&" +
      "countrycodes=eg&" +
      "addressdetails=1&" +
      "accept-language=en"

    val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
    connection.setRequestProperty("User-Agent", "Athar-Accessibility-App/1.0")
    connection.connectTimeout = 5000
    connection.readTimeout = 5000

    if (connection.responseCode != 200) {
      Log.e("AddPlaceReport", "Nominatim API returned ${connection.responseCode}")
      return@withContext emptyList()
    }

    val response = connection.inputStream.bufferedReader().use { it.readText() }
    parseNominatimToSuggestions(response, reference)
  } catch (e: Exception) {
    Log.e("AddPlaceReport", "Nominatim API error: ${e.message}", e)
    emptyList()
  }
}

private fun parseNominatimToSuggestions(json: String, reference: LatLng): List<LocationSuggestion> {
  try {
    val results = mutableListOf<LocationSuggestion>()
    val items = json.trim().removeSurrounding("[", "]").split("},{")

    for (item in items) {
      try {
        val lat = extractJsonField(item, "lat")?.toDoubleOrNull() ?: continue
        val lon = extractJsonField(item, "lon")?.toDoubleOrNull() ?: continue
        val displayName = extractJsonField(item, "display_name") ?: continue
        val name = extractJsonField(item, "name")
          ?: displayName.split(",").firstOrNull()?.trim()
          ?: continue

        val parts = displayName.split(", ")
        val subtitle = if (parts.size > 1) parts.drop(1).take(2).joinToString(", ") else ""
        val label = if (subtitle.isNotBlank()) "$name, $subtitle" else name

        results.add(LocationSuggestion(label, lat, lon))
      } catch (_: Exception) {
        continue
      }
    }

    return results
      .sortedBy { placeDistanceMeters(reference, LatLng(it.lat, it.lng)) }
      .distinctBy { it.name }
  } catch (e: Exception) {
    Log.e("AddPlaceReport", "Failed to parse Nominatim response: ${e.message}")
    return emptyList()
  }
}

private fun extractJsonField(json: String, key: String): String? {
  val regex = Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"")
  return regex.find(json)?.groupValues?.get(1)
}

private fun placeDistanceMeters(from: LatLng, to: LatLng): Float {
  val result = FloatArray(1)
  android.location.Location.distanceBetween(
    from.latitude, from.longitude,
    to.latitude, to.longitude,
    result
  )
  return result[0]
}

private suspend fun reverseGeocode(
  context: android.content.Context,
  lat: Double,
  lng: Double
): String = withContext(Dispatchers.IO) {
  try {
    // Try Nominatim reverse geocoding first
    val url = "https://nominatim.openstreetmap.org/reverse?" +
      "lat=$lat&lon=$lng&format=json&accept-language=en"
    val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
    connection.setRequestProperty("User-Agent", "Athar-Accessibility-App/1.0")
    connection.connectTimeout = 5000
    connection.readTimeout = 5000
    if (connection.responseCode == 200) {
      val response = connection.inputStream.bufferedReader().use { it.readText() }
      val name = extractJsonField(response, "name")
      val displayName = extractJsonField(response, "display_name")
      if (!name.isNullOrBlank()) return@withContext name
      if (!displayName.isNullOrBlank()) {
        return@withContext displayName.split(",").take(2).joinToString(",").trim()
      }
    }
  } catch (e: Exception) {
    Log.e("AddPlaceReport", "Nominatim reverse geocode failed: ${e.message}")
  }
  // Fallback to Android Geocoder
  try {
    if (Geocoder.isPresent()) {
      @Suppress("DEPRECATION")
      val addresses = Geocoder(context, java.util.Locale.getDefault()).getFromLocation(lat, lng, 1)
      val address = addresses?.firstOrNull()
      if (address != null) {
        val featureName = address.featureName?.takeIf { it.isNotBlank() }
        val locality = address.locality?.takeIf { it.isNotBlank() }
        val line = address.getAddressLine(0)?.takeIf { it.isNotBlank() }
        return@withContext featureName ?: locality ?: line ?: "Pinned Location"
      }
    }
  } catch (e: Exception) {
    Log.e("AddPlaceReport", "Geocoder reverse failed: ${e.message}")
  }
  "Pinned Location (${String.format("%.4f", lat)}, ${String.format("%.4f", lng)})"
}
