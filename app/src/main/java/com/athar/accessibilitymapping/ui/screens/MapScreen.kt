package com.athar.accessibilitymapping.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import androidx.activity.compose.BackHandler
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location as AndroidLocation
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.IconButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import com.athar.accessibilitymapping.data.SavedLocationsStore
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.PlaceTypes
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.coroutines.resume
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Accessible
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Accessible
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ArrowDropUp
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.DoorFront
import androidx.compose.material.icons.outlined.Elevator
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.LocalParking
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Wc
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import com.athar.accessibilitymapping.data.AtharRepository
import com.athar.accessibilitymapping.data.ApiCallResult
import com.athar.accessibilitymapping.data.ApiCreateRequest
import com.athar.accessibilitymapping.data.Location
import com.athar.accessibilitymapping.data.UserRole
import com.athar.accessibilitymapping.ui.request.AtharHelpRequestForm
import com.athar.accessibilitymapping.ui.theme.AccentGold
import com.athar.accessibilitymapping.ui.theme.AccentGoldDark
import com.athar.accessibilitymapping.ui.theme.AccentGoldLight
import com.athar.accessibilitymapping.ui.theme.BluePrimary
import com.athar.accessibilitymapping.ui.theme.BlueSecondary
import com.athar.accessibilitymapping.ui.theme.Gray200
import com.athar.accessibilitymapping.ui.theme.NavyDark
import com.athar.accessibilitymapping.ui.theme.NavyPrimary
import com.athar.accessibilitymapping.ui.theme.sdp
import com.athar.accessibilitymapping.ui.theme.ssp
import com.athar.accessibilitymapping.ui.payment.AtharPaymentFlow
import com.athar.accessibilitymapping.util.resolveMapsApiKey

private val MapHeaderStyle @Composable get() = TextStyle(
  fontFamily = FontFamily.SansSerif,
  fontWeight = FontWeight.Bold,
  fontSize = 30.ssp,
  lineHeight = 36.ssp
)

private val MapTitleLargeStyle @Composable get() = TextStyle(
  fontFamily = FontFamily.SansSerif,
  fontWeight = FontWeight.SemiBold,
  fontSize = 20.ssp,
  lineHeight = 28.ssp
)

private val MapTitleMediumStyle @Composable get() = TextStyle(
  fontFamily = FontFamily.SansSerif,
  fontWeight = FontWeight.SemiBold,
  fontSize = 20.ssp,
  lineHeight = 28.ssp
)

private val MapTitleSmallStyle @Composable get() = TextStyle(
  fontFamily = FontFamily.SansSerif,
  fontWeight = FontWeight.SemiBold,
  fontSize = 16.ssp,
  lineHeight = 24.ssp
)

private val MapBodyMediumStyle @Composable get() = TextStyle(
  fontFamily = FontFamily.SansSerif,
  fontWeight = FontWeight.Normal,
  fontSize = 16.ssp,
  lineHeight = 24.ssp
)

private val MapBodySmallStyle @Composable get() = TextStyle(
  fontFamily = FontFamily.SansSerif,
  fontWeight = FontWeight.Normal,
  fontSize = 14.ssp,
  lineHeight = 20.ssp
)

private val MapLabelMediumStyle @Composable get() = TextStyle(
  fontFamily = FontFamily.SansSerif,
  fontWeight = FontWeight.Medium,
  fontSize = 14.ssp,
  lineHeight = 20.ssp
)

private val MapLabelSmallStyle @Composable get() = TextStyle(
  fontFamily = FontFamily.SansSerif,
  fontWeight = FontWeight.Medium,
  fontSize = 12.ssp,
  lineHeight = 16.ssp
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
  userRole: UserRole,
  userDisabilityType: String?,
  isVolunteerLive: Boolean,
  isVolunteerVerified: Boolean,
  onVolunteerLiveToggle: (Boolean) -> Unit
) {
  val context = LocalContext.current
  val repository = remember(context) { AtharRepository(context) }
  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current
  val savedLocationsStore = remember { SavedLocationsStore(context) }
  val savedIds by savedLocationsStore.savedIds.collectAsState(initial = emptySet())
  val coroutineScope = rememberCoroutineScope()
  val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
  val mapsApiKey = remember { resolveMapsApiKey(context) }
  val mapsApiKeyMissing = mapsApiKey.isBlank()
  var placesClient by remember { mutableStateOf<PlacesClient?>(null) }
  var autocompletePredictions by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }
  var fallbackSuggestions by remember { mutableStateOf<List<FallbackSuggestion>>(emptyList()) }
  var isAutocompleteLoading by remember { mutableStateOf(false) }
  var autocompleteError by remember { mutableStateOf<String?>(null) }
  var showAutocomplete by remember { mutableStateOf(false) }
  var activeSuggestionQuery by remember { mutableStateOf("") }
  var sessionToken by remember { mutableStateOf(AutocompleteSessionToken.newInstance()) }
  val playServicesStatus = remember {
    GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
  }
  val playServicesOk = playServicesStatus == ConnectionResult.SUCCESS
  val playServicesMessage = remember(playServicesStatus) {
    GoogleApiAvailability.getInstance().getErrorString(playServicesStatus)
  }
  var savedCameraLat by rememberSaveable { mutableStateOf(30.0131) }
  var savedCameraLng by rememberSaveable { mutableStateOf(31.2189) }
  var savedCameraZoom by rememberSaveable { mutableStateOf(12f) }
  val cameraPositionState = rememberCameraPositionState {
    position = CameraPosition.fromLatLngZoom(LatLng(savedCameraLat, savedCameraLng), savedCameraZoom)
  }
  DisposableEffect(Unit) {
    onDispose {
      savedCameraLat = cameraPositionState.position.target.latitude
      savedCameraLng = cameraPositionState.position.target.longitude
      savedCameraZoom = cameraPositionState.position.zoom
    }
  }
  var locationPermissionGranted by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    )
  }
  val locationPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    locationPermissionGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
  }
  val mapViewModel: MapViewModel = viewModel()
  val locations by mapViewModel.locations.collectAsState()
  var searchQuery by remember { mutableStateOf("") }
  var searchResult by remember { mutableStateOf<LatLng?>(null) }
  var searchError by remember { mutableStateOf<String?>(null) }
  var isSearching by remember { mutableStateOf(false) }
  var isSearchFieldFocused by remember { mutableStateOf(false) }
  var hasLoadedBefore by rememberSaveable { mutableStateOf(false) }
  var placesExpanded by remember { mutableStateOf(true) }
  var mapLoaded by remember { mutableStateOf(false) }
  var mapLoadTimedOut by remember { mutableStateOf(false) }
  var activeFilters by remember { mutableStateOf(setOf<String>()) }
  var selectedLocation by remember { mutableStateOf<Location?>(null) }
  var showVolunteerRequest by remember { mutableStateOf(false) }
  var volunteerRequestSubmitted by remember { mutableStateOf(false) }
  var showRatingModal by remember { mutableStateOf(false) }
  var volunteerLiveError by remember { mutableStateOf<String?>(null) }
  var volunteerRequestError by remember { mutableStateOf<String?>(null) }
  var ratingError by remember { mutableStateOf<String?>(null) }
  var isSubmittingVolunteerRequest by remember { mutableStateOf(false) }
  var isSubmittingRating by remember { mutableStateOf(false) }
  var isUpdatingVolunteerLive by remember { mutableStateOf(false) }


  LaunchedEffect(mapsApiKey) {
    // Disable Places API client to use free Geocoder fallback instead
    // To enable Google Places: set up billing at https://console.cloud.google.com
    placesClient = null

    /* Uncomment this block after enabling billing on Google Cloud Console:
    if (mapsApiKey.isNotBlank() && !Places.isInitialized()) {
      Log.d("MapScreen", "Initializing Places API with key: ${mapsApiKey.take(10)}...")
      try {
        Places.initialize(context.applicationContext, mapsApiKey)
        Log.d("MapScreen", "Places API initialized successfully")
      } catch (e: Exception) {
        Log.e("MapScreen", "Failed to initialize Places API", e)
      }
    }
    if (mapsApiKey.isNotBlank() && Places.isInitialized()) {
      placesClient = Places.createClient(context)
      Log.d("MapScreen", "Places client created: ${placesClient != null}")
    } else if (mapsApiKey.isBlank()) {
      Log.e("MapScreen", "Maps API key is blank!")
    }
    */
  }
  LaunchedEffect(Unit) {
    if (!mapLoaded) {
      delay(8000)
      if (!mapLoaded) {
        mapLoadTimedOut = true
      }
    }
  }
  // Move camera to user's location on first load
  var hasCenteredOnUser by rememberSaveable { mutableStateOf(false) }
  LaunchedEffect(locationPermissionGranted, mapLoaded) {
    if (locationPermissionGranted && mapLoaded && !hasCenteredOnUser) {
      hasCenteredOnUser = true
      try {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
          if (location != null) {
            coroutineScope.launch {
              cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                  LatLng(location.latitude, location.longitude),
                  15f
                )
              )
            }
          }
        }
      } catch (_: SecurityException) { }
    }
  }
  LaunchedEffect(searchQuery, locations) {
    val query = searchQuery.trim()
    val normalizedQuery = query.lowercase(Locale.getDefault())
    if (query.isBlank()) {
      autocompletePredictions = emptyList()
      fallbackSuggestions = emptyList()
      autocompleteError = null
      showAutocomplete = false
      activeSuggestionQuery = ""
      isAutocompleteLoading = false
      return@LaunchedEffect
    }
    isAutocompleteLoading = true
    autocompleteError = null
    delay(300)
    if (query != searchQuery.trim()) return@LaunchedEffect

    val origin = cameraPositionState.position.target

    // 1. Search loaded locations (case-insensitive)
    val localSuggestions = buildLocalFallbackSuggestions(query, locations, origin)

    // 2. Search backend DB
    val backendSuggestions = try {
      withContext(Dispatchers.IO) {
        val backendResults = mapViewModel.repository.searchLocations(query)
        backendResults.map { location ->
          val target = LatLng(location.lat, location.lng)
          val distance = formatDistance(distanceMeters(origin, target))
          FallbackSuggestion(
            id = "backend:${location.id}",
            title = location.name,
            subtitle = "${location.category} • $distance away",
            latLng = target
          )
        }
      }
    } catch (e: Exception) {
      emptyList<FallbackSuggestion>()
    }
    if (query != searchQuery.trim()) return@LaunchedEffect

    // 3. Merge local + backend results (local first)
    val dbSuggestions = mergeFallbackSuggestions(localSuggestions, backendSuggestions)

    // 4. Try Nominatim/Geocoder as supplement (non-blocking)
    val geocoderSuggestions = try {
      fetchGeocoderFallbackSuggestions(context, query, origin)
    } catch (e: Exception) {
      emptyList<FallbackSuggestion>()
    }
    if (query != searchQuery.trim()) return@LaunchedEffect

    val allSuggestions = mergeFallbackSuggestions(dbSuggestions, geocoderSuggestions)

    autocompletePredictions = emptyList()
    fallbackSuggestions = allSuggestions
    showAutocomplete = allSuggestions.isNotEmpty()
    autocompleteError = if (allSuggestions.isEmpty()) "No places found for \"$query\"" else null
    activeSuggestionQuery = normalizedQuery
    isAutocompleteLoading = false
  }

  val toggleSaved: (String) -> Unit = { id ->
    coroutineScope.launch { savedLocationsStore.toggleSaved(id) }
  }

  val filterOptions = listOf(
    "ramp" to "Ramps",
    "elevator" to "Elevators",
    "parking" to "Accessible Parking",
    "braille" to "Braille",
    "toilet" to "Accessible Toilet",
    "wideEntrance" to "Wide Entrance"
  )

  val filteredLocations by remember {
    derivedStateOf {
      locations.filter { location ->
        val matchesFilters = activeFilters.isEmpty() || activeFilters.all { filter ->
          when (filter) {
            "saved" -> savedIds.contains(location.id)
            "ramp" -> location.features.ramp
            "elevator" -> location.features.elevator
            "parking" -> location.features.accessibleParking
            "braille" -> location.features.brailleSignage
            "toilet" -> location.features.accessibleToilet
            "wideEntrance" -> location.features.wideEntrance
            else -> false
          }
        }
        matchesFilters
      }
    }
  }

  val runSearch = runSearch@{
    val query = searchQuery.trim()
    val normalizedQuery = query.lowercase(Locale.getDefault())
    if (query.isBlank()) {
      searchError = null
      searchResult = null
      return@runSearch
    }
    isSearching = true
    searchError = null
    showAutocomplete = false
    coroutineScope.launch {
      val client = placesClient
      val hasFreshSuggestions = activeSuggestionQuery == normalizedQuery
      if (hasFreshSuggestions && autocompletePredictions.isNotEmpty() && client != null) {
        val bestPrediction = withContext(Dispatchers.IO) {
          resolveBestPrediction(
            client = client,
            predictions = autocompletePredictions
          )
        }
        if (bestPrediction != null) {
          val (prediction, latLng) = bestPrediction
          searchResult = latLng
          searchError = null
          searchQuery = prediction.getFullText(null).toString()
          cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 14f))
          autocompletePredictions = emptyList()
          fallbackSuggestions = emptyList()
          sessionToken = AutocompleteSessionToken.newInstance()
          isSearching = false
          return@launch
        }
      }
      if (hasFreshSuggestions && fallbackSuggestions.isNotEmpty()) {
        val bestFallback = pickBestFallbackSuggestion(query, fallbackSuggestions)
        if (bestFallback != null) {
          searchResult = bestFallback.latLng
          searchError = null
          searchQuery = bestFallback.title
          cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(bestFallback.latLng, 14f))
          autocompletePredictions = emptyList()
          fallbackSuggestions = emptyList()
          sessionToken = AutocompleteSessionToken.newInstance()
          isSearching = false
          return@launch
        }
      }
      val bestLocalMatch = findBestLocalLocationMatch(query, locations)
      if (bestLocalMatch != null) {
        val latLng = LatLng(bestLocalMatch.lat, bestLocalMatch.lng)
        searchResult = latLng
        searchError = null
        searchQuery = bestLocalMatch.name
        autocompletePredictions = emptyList()
        fallbackSuggestions = emptyList()
        sessionToken = AutocompleteSessionToken.newInstance()
        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 14f))
        isSearching = false
        return@launch
      }
      if (!Geocoder.isPresent()) {
        searchError = "Search is not available on this device."
        searchResult = null
        isSearching = false
        return@launch
      }
      val result = withContext(Dispatchers.IO) {
        try {
          @Suppress("DEPRECATION")
          Geocoder(context, Locale.getDefault()).getFromLocationName(query, 1)
        } catch (_: IOException) {
          null
        } catch (_: Exception) {
          null
        }
      }
      if (!result.isNullOrEmpty()) {
        val address = result.first()
        val latLng = LatLng(address.latitude, address.longitude)
        searchResult = latLng
        searchQuery = address.featureName ?: query
        autocompletePredictions = emptyList()
        fallbackSuggestions = emptyList()
        sessionToken = AutocompleteSessionToken.newInstance()
        coroutineScope.launch {
          cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 14f))
        }
      } else {
        searchResult = null
        searchError = "No results found for \"$query\"."
      }
      isSearching = false
    }
  }

  val selectSearchTarget: (String, LatLng) -> Unit = { label, latLng ->
    searchResult = latLng
    searchError = null
    searchQuery = label
    autocompletePredictions = emptyList()
    fallbackSuggestions = emptyList()
    showAutocomplete = false
    activeSuggestionQuery = ""
    sessionToken = AutocompleteSessionToken.newInstance()
    coroutineScope.launch {
      cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 14f))
    }
  }

  val onPredictionSelected: (AutocompletePrediction) -> Unit = { prediction ->
    keyboardController?.hide()
    focusManager.clearFocus(force = true)
    showAutocomplete = false
    searchError = null
    isSearching = true
    coroutineScope.launch {
      val client = placesClient
      if (client == null) {
        isSearching = false
        searchError = "Google Places is not ready yet. Please try again."
        return@launch
      }
      val place = withContext(Dispatchers.IO) {
        fetchPlaceDetails(
          client,
          FetchPlaceRequest.newInstance(
            prediction.placeId,
            listOf(Place.Field.LAT_LNG, Place.Field.NAME, Place.Field.ADDRESS)
          )
        )
      }
      val latLng = place?.latLng
      if (latLng != null) {
        selectSearchTarget(place.name ?: prediction.getFullText(null).toString(), latLng)
      } else {
        searchQuery = prediction.getFullText(null).toString()
        searchResult = null
        searchError = "Unable to load the selected place."
        autocompletePredictions = emptyList()
        fallbackSuggestions = emptyList()
        sessionToken = AutocompleteSessionToken.newInstance()
      }
      isSearching = false
    }
  }

  val onFallbackSuggestionSelected: (FallbackSuggestion) -> Unit = { suggestion ->
    keyboardController?.hide()
    focusManager.clearFocus(force = true)
    selectSearchTarget(suggestion.title, suggestion.latLng)
  }

  val listLocations = filteredLocations.sortedBy { parseDistanceMeters(it.distance) }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(BluePrimary)
  ) {
    // ── MAP (full background) ──
    GoogleMap(
      modifier = Modifier.fillMaxSize(),
      cameraPositionState = cameraPositionState,
      properties = MapProperties(isMyLocationEnabled = locationPermissionGranted),
      uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false),
      onMapLoaded = { mapLoaded = true; hasLoadedBefore = true }
    ) {
      searchResult?.let { target ->
        Marker(
          state = MarkerState(position = target),
          title = searchQuery.ifBlank { "Search result" },
          snippet = "Lat: ${target.latitude}, Lng: ${target.longitude}"
        )
      }

      filteredLocations.forEach { location ->
        val position = LatLng(location.lat, location.lng)
        Marker(
          state = MarkerState(position = position),
          title = location.name,
          snippet = "${location.category} • Rating: ${location.rating}",
          icon = if (activeFilters.isNotEmpty()) {
            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
          } else {
            BitmapDescriptorFactory.defaultMarker()
          },
          onClick = {
            selectedLocation = location
            true
          }
        )
      }
    }

    // ── TOP OVERLAY ──
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.TopCenter)
        .statusBarsPadding()
        .padding(top = 12.sdp),
      verticalArrangement = Arrangement.spacedBy(6.sdp)
    ) {
      // ── Athar title + action button row (no card, floating) ──
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 18.sdp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Box(
          modifier = Modifier
            .background(
              brush = Brush.linearGradient(
                colors = listOf(
                  Color.White.copy(alpha = 0.28f),
                  Color.White.copy(alpha = 0.08f),
                  Color.White.copy(alpha = 0.18f)
                )
              ),
              shape = RoundedCornerShape(24.sdp)
            )
            .border(
              width = 1.dp,
              color = Color.White.copy(alpha = 0.72f),
              shape = RoundedCornerShape(24.sdp)
            )
            .padding(horizontal = 20.sdp, vertical = 10.sdp)
        ) {
          Text(
            "Athar",
            color = NavyPrimary,
            style = MapHeaderStyle.copy(
              fontSize = 30.sp,
              lineHeight = 36.sp,
              fontWeight = FontWeight.SemiBold
            )
          )
        }

        if (userRole == UserRole.User) {
          Card(
            shape = RoundedCornerShape(28.sdp),
            colors = CardDefaults.cardColors(containerColor = NavyPrimary),
            elevation = CardDefaults.cardElevation(6.dp),
            modifier = Modifier.clickable {
              volunteerRequestError = null
              volunteerRequestSubmitted = false
              showVolunteerRequest = true
            }
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 16.sdp, vertical = 10.sdp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.sdp)
            ) {
              Icon(
                Icons.Outlined.Add,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.sdp)
              )
              Text(
                "Get Help",
                color = Color.White,
                style = MapLabelMediumStyle.copy(fontWeight = FontWeight.Bold)
              )
            }
          }
        } else {
          if (isVolunteerVerified) {
            Card(
              shape = RoundedCornerShape(28.sdp),
              colors = CardDefaults.cardColors(
                containerColor = if (isVolunteerLive) Color(0xFF2E9E57) else Color(0xFF0F2747)
              ),
              elevation = CardDefaults.cardElevation(6.dp),
              modifier = Modifier
                .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(28.sdp))
                .clickable(enabled = !isUpdatingVolunteerLive) {
                volunteerLiveError = null
                isUpdatingVolunteerLive = true
                val nextState = !isVolunteerLive
                coroutineScope.launch {
                  when (val result = repository.setVolunteerLive(nextState)) {
                    is ApiCallResult.Success -> {
                      onVolunteerLiveToggle(result.data.volunteerLive)
                    }
                    is ApiCallResult.Failure -> {
                      volunteerLiveError = result.message
                    }
                  }
                  isUpdatingVolunteerLive = false
                }
              }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 16.sdp, vertical = 10.sdp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.sdp)
              ) {
                Icon(
                  Icons.Outlined.PowerSettingsNew,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(18.sdp)
                )
                Text(
                  if (isUpdatingVolunteerLive) "Saving..."
                  else if (isVolunteerLive) "Live"
                  else "Go Live",
                  color = Color.White,
                  style = MapLabelMediumStyle.copy(fontWeight = FontWeight.Bold)
                )
              }
            }
          } else {
            Card(
              shape = RoundedCornerShape(28.sdp),
              colors = CardDefaults.cardColors(containerColor = AccentGold),
              elevation = CardDefaults.cardElevation(6.dp)
            ) {
              Text(
                "Pending Verification",
                color = Color.White,
                style = MapLabelMediumStyle.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(horizontal = 16.sdp, vertical = 10.sdp)
              )
            }
          }
        }
      }

      if (volunteerLiveError != null && userRole == UserRole.Volunteer) {
        Text(
          volunteerLiveError ?: "",
          color = Color(0xFFDC2626),
          style = MapBodySmallStyle,
          modifier = Modifier.padding(horizontal = 18.sdp)
        )
      }

      // ── Search bar (separate card) ──
      Card(
        shape = RoundedCornerShape(28.sdp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(8.dp),
        modifier = Modifier.padding(horizontal = 16.sdp)
      ) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { value ->
            searchQuery = value
            if (value.isBlank()) {
              autocompletePredictions = emptyList()
              fallbackSuggestions = emptyList()
              showAutocomplete = false
              autocompleteError = null
              sessionToken = AutocompleteSessionToken.newInstance()
            }
          },
          leadingIcon = {
            Icon(
              Icons.Outlined.Search,
              contentDescription = null,
              tint = NavyPrimary,
              modifier = Modifier.size(22.sdp)
            )
          },
          trailingIcon = {
            if (searchQuery.isNotEmpty() || isSearchFieldFocused) {
              Icon(
                Icons.Outlined.Close,
                contentDescription = "Clear search",
                tint = NavyPrimary,
                modifier = Modifier
                  .size(20.sdp)
                  .clickable {
                    searchQuery = ""
                    autocompletePredictions = emptyList()
                    fallbackSuggestions = emptyList()
                    showAutocomplete = false
                    autocompleteError = null
                    searchResult = null
                    sessionToken = AutocompleteSessionToken.newInstance()
                    keyboardController?.hide()
                    focusManager.clearFocus(force = true)
                  }
              )
            }
          },
          placeholder = {
            Text(
              "Search for accessible places...",
              color = NavyPrimary,
              style = MapBodySmallStyle
            )
          },
          textStyle = MapBodySmallStyle.copy(
            color = NavyPrimary,
            fontWeight = FontWeight.Medium
          ),
          modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
              isSearchFieldFocused = focusState.isFocused
              if (!focusState.isFocused) {
                showAutocomplete = false
              }
            },
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
          ),
          shape = RoundedCornerShape(28.sdp),
          singleLine = true,
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
          keyboardActions = KeyboardActions(
            onSearch = {
              keyboardController?.hide()
              focusManager.clearFocus(force = true)
              runSearch()
            }
          )
        )
      }

      // ── Autocomplete dropdown ──
      if (isSearchFieldFocused && showAutocomplete) {
        Card(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.sdp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          shape = RoundedCornerShape(16.sdp),
          elevation = CardDefaults.cardElevation(6.dp)
        ) {
          LazyColumn(modifier = Modifier.heightIn(max = 220.sdp)) {
            if (isAutocompleteLoading) {
              item {
                Text(
                  "Loading suggestions...",
                  color = NavyPrimary,
                  style = MapBodySmallStyle,
                  modifier = Modifier.padding(horizontal = 12.sdp, vertical = 10.sdp)
                )
              }
            }

            items(
              items = autocompletePredictions,
              key = { prediction -> prediction.placeId }
            ) { prediction ->
              val title = prediction.getPrimaryText(null).toString()
              val subtitle = prediction.getSecondaryText(null).toString()
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { onPredictionSelected(prediction) }
                  .padding(horizontal = 12.sdp, vertical = 10.sdp)
              ) {
                Text(
                  title,
                  color = NavyPrimary,
                  style = MapBodySmallStyle.copy(fontWeight = FontWeight.SemiBold)
                )
                if (subtitle.isNotBlank()) {
                  Spacer(modifier = Modifier.height(2.sdp))
                  Text(
                    subtitle,
                    color = NavyPrimary.copy(alpha = 0.7f),
                    style = MapLabelSmallStyle
                  )
                }
              }
              Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Gray200))
            }

            items(
              items = fallbackSuggestions,
              key = { suggestion -> suggestion.id }
            ) { suggestion ->
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { onFallbackSuggestionSelected(suggestion) }
                  .padding(horizontal = 12.sdp, vertical = 10.sdp)
              ) {
                Text(
                  suggestion.title,
                  color = NavyPrimary,
                  style = MapBodySmallStyle.copy(fontWeight = FontWeight.SemiBold)
                )
                if (suggestion.subtitle.isNotBlank()) {
                  Spacer(modifier = Modifier.height(2.sdp))
                  Text(
                    suggestion.subtitle,
                    color = NavyPrimary.copy(alpha = 0.7f),
                    style = MapLabelSmallStyle
                  )
                }
              }
              Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Gray200))
            }

            if (!isAutocompleteLoading && autocompletePredictions.isEmpty() && fallbackSuggestions.isEmpty()) {
              item {
                Text(
                  autocompleteError ?: "No suggestions found.",
                  color = NavyPrimary.copy(alpha = 0.8f),
                  style = MapBodySmallStyle,
                  modifier = Modifier.padding(horizontal = 12.sdp, vertical = 10.sdp)
                )
              }
            } else if (autocompleteError != null) {
              item {
                Text(
                  autocompleteError ?: "",
                  color = NavyPrimary.copy(alpha = 0.8f),
                  style = MapBodySmallStyle,
                  modifier = Modifier.padding(horizontal = 12.sdp, vertical = 10.sdp)
                )
              }
            }
          }
        }
      }

      if (isSearching) {
        Text(
          "Searching...",
          color = NavyPrimary.copy(alpha = 0.6f),
          style = MapBodySmallStyle,
          modifier = Modifier.padding(horizontal = 18.sdp)
        )
      } else if (searchError != null) {
        Text(
          searchError ?: "",
          color = Color(0xFFDC2626),
          style = MapBodySmallStyle,
          modifier = Modifier.padding(horizontal = 18.sdp)
        )
      }

      if (searchResult != null && !isSearching) {
        Card(
          shape = RoundedCornerShape(28.sdp),
          colors = CardDefaults.cardColors(containerColor = NavyPrimary),
          elevation = CardDefaults.cardElevation(4.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.sdp)
            .clickable {
              searchResult?.let { destination ->
                openDirections(context, destination)
              }
            }
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.sdp, vertical = 12.sdp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Icon(
              Icons.Outlined.Navigation,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(18.sdp)
            )
            Spacer(modifier = Modifier.width(6.sdp))
            Text(
              "Directions to search result",
              color = Color.White,
              style = MapLabelMediumStyle.copy(fontWeight = FontWeight.SemiBold)
            )
          }
        }
      }

      // ── Accessibility feature chips (Google Maps style) ──
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 16.sdp),
        horizontalArrangement = Arrangement.spacedBy(8.sdp)
      ) {
        filterOptions.forEach { (id, label) ->
          val selected = activeFilters.contains(id)
          val chipIcon: ImageVector = when (id) {
            "ramp" -> Icons.AutoMirrored.Outlined.Accessible
            "elevator" -> Icons.Outlined.Elevator
            "parking" -> Icons.Outlined.LocalParking
            "braille" -> Icons.Outlined.TouchApp
            "toilet" -> Icons.Outlined.Wc
            "wideEntrance" -> Icons.Outlined.DoorFront
            else -> Icons.Outlined.CheckCircle
          }
          Card(
            shape = RoundedCornerShape(24.sdp),
            colors = CardDefaults.cardColors(
              containerColor = if (selected) NavyPrimary else Color.White
            ),
            elevation = CardDefaults.cardElevation(4.dp),
            modifier = Modifier.clickable {
              activeFilters = if (selected) activeFilters - id else activeFilters + id
            }
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.sdp, vertical = 8.sdp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.sdp)
            ) {
              Icon(
                chipIcon,
                contentDescription = null,
                tint = if (selected) Color.White else NavyPrimary,
                modifier = Modifier.size(16.sdp)
              )
              Text(
                label,
                color = if (selected) Color.White else NavyPrimary,
                style = MapLabelSmallStyle.copy(fontWeight = FontWeight.SemiBold)
              )
            }
          }
        }
      }
    } // end top overlay Column

    // ── MAP OVERLAY NOTICES ──
    Box(
      modifier = Modifier
        .fillMaxSize()
    ) {
      if (!playServicesOk) {
        Card(
          modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(16.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
          shape = RoundedCornerShape(12.dp)
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text("Google Play services not available", color = NavyPrimary, fontWeight = FontWeight.SemiBold)
            Text(playServicesMessage, color = NavyPrimary.copy(alpha = 0.8f))
          }
        }
      } else if (!mapLoaded && !hasLoadedBefore) {
        Card(
          modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(16.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
          shape = RoundedCornerShape(12.dp)
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text("Loading map...", color = NavyPrimary, fontWeight = FontWeight.SemiBold)
            if (mapLoadTimedOut) {
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                "Map is taking too long to load. Check API key restrictions and billing, or try a device with Google Play.",
                color = NavyPrimary.copy(alpha = 0.8f)
              )
            }
          }
        }
      }

      if (mapsApiKeyMissing) {
        Card(
          modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(16.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
          shape = RoundedCornerShape(12.dp)
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text("Google Maps API key is missing", color = NavyPrimary, fontWeight = FontWeight.SemiBold)
            Text(
              "Add MAPS_API_KEY to local.properties or set it as an environment variable.",
              color = NavyPrimary.copy(alpha = 0.8f)
            )
          }
        }
      }

      // (zoom and location controls removed – geolocation is now part of bottom panel)

      if (filteredLocations.isEmpty()) {
        Card(
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(16.dp),
          colors = CardDefaults.cardColors(containerColor = BluePrimary),
          shape = RoundedCornerShape(12.dp)
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text("No places match your filters", color = NavyPrimary, fontWeight = FontWeight.SemiBold)
            Text("Try removing a filter to see more locations.", color = NavyPrimary.copy(alpha = 0.7f))
          }
        }
      }
    }

    // ── BOTTOM PLACES PANEL + GEO BUTTON ──
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.BottomCenter),
      horizontalAlignment = Alignment.End
    ) {
      // Geolocation button above the panel
      Card(
        shape = CircleShape,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(6.dp),
        modifier = Modifier
          .padding(end = 14.dp, bottom = 10.dp)
          .size(48.dp)
          .clickable {
            if (locationPermissionGranted) {
              fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                  coroutineScope.launch {
                    cameraPositionState.animate(
                      CameraUpdateFactory.newLatLngZoom(
                        LatLng(location.latitude, location.longitude),
                        15f
                      )
                    )
                  }
                }
              }
            } else {
              locationPermissionLauncher.launch(
                arrayOf(
                  Manifest.permission.ACCESS_FINE_LOCATION,
                  Manifest.permission.ACCESS_COARSE_LOCATION
                )
              )
            }
          }
      ) {
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            Icons.Filled.MyLocation,
            contentDescription = "My location",
            tint = NavyPrimary,
            modifier = Modifier.size(24.dp)
          )
        }
      }

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
          .background(Color.White)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(BlueSecondary)
            .clickable { placesExpanded = !placesExpanded }
            .padding(horizontal = 18.dp, vertical = 14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "${listLocations.size} Accessible ${if (listLocations.size == 1) "Place" else "Places"} Nearby",
            color = NavyPrimary,
            style = MapTitleSmallStyle.copy(
              fontWeight = FontWeight.SemiBold,
              fontSize = 16.sp
            )
          )
          Icon(
            imageVector = if (placesExpanded) Icons.Outlined.ArrowDropDown else Icons.Outlined.ArrowDropUp,
            contentDescription = if (placesExpanded) "Collapse" else "Expand",
            tint = NavyPrimary,
            modifier = Modifier.size(28.dp)
          )
        }

        AnimatedVisibility(visible = placesExpanded) {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .height(220.dp)
          ) {
            itemsIndexed(listLocations, key = { _, location -> location.id }) { index, location ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { selectedLocation = location }
                  .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Top
              ) {
                Column(
                  modifier = Modifier.weight(1f),
                  verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Text(
                    location.name,
                    color = NavyPrimary,
                    style = MapTitleSmallStyle.copy(fontWeight = FontWeight.SemiBold)
                  )
                  Text(
                    "${location.category} • ${location.distance}",
                    color = NavyPrimary.copy(alpha = 0.75f),
                    style = MapBodySmallStyle
                  )
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                  ) {
                    Icon(
                      Icons.Outlined.Star,
                      contentDescription = null,
                      tint = AccentGold,
                      modifier = Modifier.size(14.dp)
                    )
                    Text(
                      location.rating.toString(),
                      color = NavyPrimary.copy(alpha = 0.9f),
                      style = MapLabelSmallStyle
                    )
                    Text(
                      "(${location.totalRatings})",
                      color = NavyPrimary.copy(alpha = 0.65f),
                      style = MapLabelSmallStyle
                    )
                  }
                }
                Column(
                  horizontalAlignment = Alignment.Start,
                  verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  if (location.features.ramp) MapFeatureChip("Ramp")
                  if (location.features.elevator) MapFeatureChip("Elevator")
                }
              }

              if (index < listLocations.lastIndex) {
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Gray200)
                )
              }
            }
          }
        }
      } // end bottom panel
    } // end bottom Column wrapper

  } // end parent Box

  if (selectedLocation != null) {
    val location = selectedLocation!!
    BackHandler { selectedLocation = null }
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.4f))
        .clickable(
          indication = null,
          interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
        ) { selectedLocation = null },
      contentAlignment = Alignment.BottomCenter
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .fillMaxHeight(0.85f)
          .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
          .clickable(
            indication = null,
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
          ) { /* consume clicks so they don't dismiss */ }
          .background(Color.White)
      ) {
        LocationDetailSheet(
          location = location,
          onNavigate = { openDirections(context, LatLng(location.lat, location.lng)) },
          onClose = { selectedLocation = null },
          onRate = {
            ratingError = null
            showRatingModal = true
          }
        )
      }
    }
  }

  if (showVolunteerRequest) {
    AtharHelpRequestForm(
      userDisabilityType = userDisabilityType ?: "Wheelchair user",
      isSubmitting = isSubmittingVolunteerRequest,
      submitted = volunteerRequestSubmitted,
      errorMessage = volunteerRequestError,
      onClose = { showVolunteerRequest = false },
      onSubmit = { data ->
        if (isSubmittingVolunteerRequest) {
          return@AtharHelpRequestForm
        }
        volunteerRequestError = null
        volunteerRequestSubmitted = false
        isSubmittingVolunteerRequest = true
        coroutineScope.launch {
          when (
            val result = repository.createAssistanceRequest(
            ApiCreateRequest(
              userType = userDisabilityType ?: "Wheelchair user",
              location = data.location,
              destination = data.destination,
              distance = "Unknown",
              urgency = data.urgency,
              helpType = data.helpType,
              description = data.description,
              paymentMethod = data.paymentMethod,
              hours = data.hours,
              pricePerHour = data.pricePerHour,
              serviceFee = data.total.toDouble()
            )
          )
          ) {
            is ApiCallResult.Success -> {
              volunteerRequestSubmitted = true
            }
            is ApiCallResult.Failure -> {
              volunteerRequestError = result.message
            }
          }
          isSubmittingVolunteerRequest = false
        }
      }
    )
  }

  if (showRatingModal && selectedLocation != null) {
    val location = selectedLocation!!
    BackHandler {
      showRatingModal = false
      ratingError = null
    }
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.4f))
        .clickable(
          indication = null,
          interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
        ) {
          showRatingModal = false
          ratingError = null
        },
      contentAlignment = Alignment.BottomCenter
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
          .clickable(
            indication = null,
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
          ) { /* consume clicks */ }
          .background(Color.White)
      ) {
        RatingSheet(
          locationName = location.name,
          isSubmitting = isSubmittingRating,
          errorMessage = ratingError,
          onSubmit = { score, comment ->
            ratingError = null
            isSubmittingRating = true
            coroutineScope.launch {
              when (val result = repository.submitLocationRating(location.id, score, comment.ifBlank { null })) {
                is ApiCallResult.Success -> {
                  mapViewModel.refresh()
                  selectedLocation = null
                  showRatingModal = false
                }
                is ApiCallResult.Failure -> {
                  ratingError = result.message
                }
              }
              isSubmittingRating = false
            }
          }
        )
      }
    }
  }
}

@Composable
private fun FeatureTag(label: String) {
  Box(
    modifier = Modifier
      .padding(vertical = 2.dp)
      .clip(RoundedCornerShape(10.dp))
      .background(AccentGoldLight)
      .border(1.dp, AccentGold, RoundedCornerShape(10.dp))
      .padding(horizontal = 8.dp, vertical = 2.dp)
  ) {
    Text(label, color = AccentGoldDark, style = MapLabelSmallStyle)
  }
}

@Composable
private fun MapFeatureChip(label: String) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(6.dp))
      .background(AccentGoldLight)
      .border(1.dp, AccentGoldDark, RoundedCornerShape(6.dp))
      .width(72.dp)
      .padding(horizontal = 10.dp, vertical = 3.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      label,
      color = AccentGoldDark,
      textAlign = TextAlign.Center,
      style = MapLabelSmallStyle.copy(
        fontSize = 12.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Medium
      )
    )
  }
}

@Composable
private fun LocationDetailSheet(
  location: Location,
  onNavigate: () -> Unit,
  onClose: () -> Unit,
  onRate: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color.White)
  ) {
    // Header - kept outside LazyColumn for better UX
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = location.name,
        color = NavyPrimary,
        style = MapTitleMediumStyle.copy(fontWeight = FontWeight.SemiBold),
        modifier = Modifier.weight(1f)
      )
      IconButton(
        onClick = onClose,
        modifier = Modifier.size(30.dp)
      ) {
        Icon(
          Icons.Outlined.Close,
          contentDescription = "Close location details",
          tint = NavyPrimary,
          modifier = Modifier.size(24.dp)
        )
      }
    }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(2.dp)
        .background(Color(0xFFE2E8F0))
    )

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 12.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // Rating section
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(BlueSecondary, RoundedCornerShape(10.dp))
          .border(1.dp, NavyPrimary, RoundedCornerShape(10.dp))
          .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          Icons.Outlined.Star,
          contentDescription = null,
          tint = AccentGold,
          modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = location.rating.toString(),
            color = NavyPrimary,
            style = MapTitleMediumStyle.copy(fontWeight = FontWeight.SemiBold)
          )
          Text(
            text = "${location.totalRatings} ratings",
            color = NavyPrimary.copy(alpha = 0.75f),
            style = MapBodySmallStyle
          )
        }
        Box(
          modifier = Modifier
            .background(AccentGold, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
          Text(
            text = location.category,
            color = Color.White,
            style = MapBodySmallStyle.copy(fontWeight = FontWeight.SemiBold)
          )
        }
      }

      // Distance
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          Icons.Outlined.Place,
          contentDescription = null,
          tint = NavyPrimary.copy(alpha = 0.75f),
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "${location.distance} away from your location",
          color = NavyPrimary,
          style = MapBodyMediumStyle
        )
      }

      // Accessibility Features Title
      Text(
        "Accessibility Features",
        color = NavyPrimary,
        style = MapTitleSmallStyle.copy(fontWeight = FontWeight.SemiBold)
      )

      // Accessibility Features List
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FeatureRow("Wheelchair Ramp", location.features.ramp, Icons.AutoMirrored.Outlined.Accessible)
        FeatureRow("Elevator", location.features.elevator, Icons.Outlined.Elevator)
        FeatureRow("Accessible Toilet", location.features.accessibleToilet, Icons.Outlined.Wc)
        FeatureRow("Accessible Parking", location.features.accessibleParking, Icons.Outlined.LocalParking)
        FeatureRow("Wide Entrance", location.features.wideEntrance, Icons.Outlined.DoorFront)
        FeatureRow("Braille Signage", location.features.brailleSignage, Icons.Outlined.TouchApp)
      }

      // Recent Updates Title
      Text(
        "Recent Updates",
        color = NavyPrimary,
        style = MapTitleSmallStyle.copy(fontWeight = FontWeight.SemiBold)
      )

      // Recent Updates List
      if (location.recentReports.isEmpty()) {
        RecentUpdateRow("No updates yet")
      } else {
        location.recentReports.forEach { report ->
          RecentUpdateRow(report)
        }
      }

      // Navigate Button
      Button(
        onClick = onNavigate,
        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary, contentColor = Color.White),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
      ) {
        Icon(
          Icons.Outlined.Navigation,
          contentDescription = null,
          modifier = Modifier.size(19.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          "Navigate Here",
          style = MapTitleSmallStyle.copy(fontWeight = FontWeight.SemiBold)
        )
      }

      // Rate Button
      Button(
        onClick = onRate,
        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = NavyPrimary),
        border = androidx.compose.foundation.BorderStroke(1.dp, NavyPrimary),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
      ) {
        Icon(
          Icons.Outlined.Star,
          contentDescription = null,
          modifier = Modifier.size(19.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          "Rate & Report",
          style = MapTitleSmallStyle.copy(fontWeight = FontWeight.SemiBold)
        )
      }
    }
  }
}

@Composable
private fun RatingSheet(
  locationName: String,
  isSubmitting: Boolean,
  errorMessage: String?,
  onSubmit: (Int, String) -> Unit
) {
  var rating by remember { mutableStateOf(0) }
  var comment by remember { mutableStateOf("") }
  var localError by remember { mutableStateOf<String?>(null) }

  Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
    Text("Rate & Report", style = MapTitleLargeStyle, color = NavyPrimary)
    Spacer(modifier = Modifier.height(8.dp))
    Text("Rating for: $locationName", color = NavyPrimary.copy(alpha = 0.7f))
    Spacer(modifier = Modifier.height(12.dp))

    Row(
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      (1..5).forEach { star ->
        Icon(
          imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.Star,
          contentDescription = "Star $star",
          tint = if (star <= rating) AccentGold else Color(0xFFD1D5DB),
          modifier = Modifier
            .size(44.dp)
            .clickable {
              rating = star
              localError = null
            }
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
      value = comment,
      onValueChange = { comment = it },
      placeholder = { Text("Additional Comments (Optional)") },
      modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(12.dp))

    Button(
      onClick = {
        if (rating == 0) {
          localError = "Please choose a rating before submitting."
          return@Button
        }
        localError = null
        onSubmit(rating, comment.trim())
      },
      enabled = !isSubmitting,
      colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
      modifier = Modifier.fillMaxWidth()
    ) {
      Text(if (isSubmitting) "Submitting..." else "Submit Rating")
    }

    if (localError != null || errorMessage != null) {
      Spacer(modifier = Modifier.height(8.dp))
      Text(localError ?: errorMessage.orEmpty(), color = Color(0xFFB91C1C))
    }
  }
}

@Composable
private fun FeatureRow(label: String, available: Boolean, icon: ImageVector) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(
        if (available) Color(0xFFD6B768) else BlueSecondary,
        RoundedCornerShape(10.dp)
      )
      .border(
        1.dp,
        if (available) Color(0xFFD6B768) else Color(0xFFD0D8E4),
        RoundedCornerShape(10.dp)
      )
      .padding(horizontal = 12.dp, vertical = 11.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(24.dp)
        .background(Color(0xFF5EA2F0), RoundedCornerShape(6.dp)),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier.size(15.dp)
      )
    }
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      label,
      color = NavyPrimary,
      style = MapBodyMediumStyle.copy(fontWeight = FontWeight.Medium),
      modifier = Modifier.weight(1f)
    )
    Icon(
      imageVector = if (available) Icons.Outlined.CheckCircle else Icons.Outlined.Cancel,
      contentDescription = null,
      tint = if (available) Color(0xFFBC943D) else Color(0xFF9AAEC8),
      modifier = Modifier.size(18.dp)
    )
  }
}

@Composable
private fun RecentUpdateRow(text: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(BlueSecondary, RoundedCornerShape(10.dp))
      .border(1.dp, NavyPrimary, RoundedCornerShape(10.dp))
      .padding(horizontal = 12.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      Icons.Outlined.Flag,
      contentDescription = null,
      tint = NavyPrimary,
      modifier = Modifier.size(16.dp)
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = text,
      color = NavyPrimary,
      style = MapBodySmallStyle
    )
  }
}

@Composable
private fun VolunteerRequestSheet(
  userRole: UserRole,
  userDisabilityType: String?,
  submitted: Boolean,
  onClose: () -> Unit,
  onSubmit: (location: String, destination: String, helpType: String, urgency: String, description: String) -> Unit,
  isSubmitting: Boolean,
  errorMessage: String?
) {
  var location by remember { mutableStateOf("") }
  var destination by remember { mutableStateOf("") }
  var helpType by remember { mutableStateOf("") }
  var showHelpTypeMenu by remember { mutableStateOf(false) }
  var urgency by remember { mutableStateOf("medium") }
  var description by remember { mutableStateOf("") }
  var localError by remember { mutableStateOf<String?>(null) }
  val helpTypes = listOf(
    "Navigation assistance",
    "Finding accessible entrance",
    "Help with accessible transportation",
    "Guide to specific location",
    "Reading assistance",
    "Other assistance"
  )

  if (userRole != UserRole.User) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(
        modifier = Modifier
          .size(64.dp)
          .background(Color(0xFFFEE2E2), RoundedCornerShape(999.dp)),
        contentAlignment = Alignment.Center
      ) {
        Icon(Icons.Outlined.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(36.dp))
      }
      Spacer(modifier = Modifier.height(16.dp))
      Text("Access Denied", color = NavyPrimary, style = MapTitleLargeStyle)
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        "Only users with accessibility needs can request volunteer assistance.",
        color = Color(0xFF4B5563),
        style = MapBodySmallStyle
      )
      Spacer(modifier = Modifier.height(16.dp))
      Button(
        onClick = onClose,
        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text("Close")
      }
    }
    return
  }

  if (submitted) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(
        modifier = Modifier
          .size(64.dp)
          .background(Color(0xFFDCFCE7), RoundedCornerShape(999.dp)),
        contentAlignment = Alignment.Center
      ) {
        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(36.dp))
      }
      Spacer(modifier = Modifier.height(16.dp))
      Text("Request Broadcasted!", color = NavyPrimary, style = MapTitleLargeStyle)
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        "Searching for nearby volunteers. Average response time is under 2 minutes.",
        color = Color(0xFF4B5563),
        style = MapBodySmallStyle
      )
    }
    return
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color.White)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        "Request Volunteer Help",
        color = NavyPrimary,
        style = MapTitleMediumStyle.copy(fontWeight = FontWeight.SemiBold),
        modifier = Modifier.weight(1f)
      )
      IconButton(
        onClick = onClose,
        modifier = Modifier.size(30.dp)
      ) {
        Icon(
          Icons.Outlined.Close,
          contentDescription = "Close request form",
          tint = NavyPrimary,
          modifier = Modifier.size(22.dp)
        )
      }
    }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(2.dp)
        .background(Color(0xFFE2E8F0))
    )

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 16.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
      if (!userDisabilityType.isNullOrBlank()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp))
            .padding(16.dp)
        ) {
          Text(
            "Request as: $userDisabilityType",
            color = NavyPrimary,
            style = MapBodySmallStyle
          )
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          "Your Current Location *",
          color = NavyPrimary,
          style = MapBodyMediumStyle
        )
        OutlinedTextField(
          value = location,
          onValueChange = {
            location = it
            localError = null
          },
          leadingIcon = {
            Icon(
              Icons.Outlined.Place,
              contentDescription = null,
              tint = Color(0xFF9AA9BC),
              modifier = Modifier.size(19.dp)
            )
          },
          placeholder = {
            Text(
              "e.g., Central Mall entrance",
              color = Color(0xFFA9B6C7),
              style = MapBodyMediumStyle
            )
          },
          textStyle = MapBodyMediumStyle.copy(color = NavyPrimary),
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          shape = RoundedCornerShape(8.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFFD1D5DB),
            unfocusedBorderColor = Color(0xFFD1D5DB),
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
          )
        )
        TextButton(
          onClick = { location = "Central Mall, Main Entrance" },
          contentPadding = PaddingValues(0.dp)
        ) {
          Text(
            "Use my current location",
            color = NavyPrimary,
            style = MapBodySmallStyle
          )
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          "Where do you need to go? *",
          color = NavyPrimary,
          style = MapBodyMediumStyle
        )
        OutlinedTextField(
          value = destination,
          onValueChange = {
            destination = it
            localError = null
          },
          leadingIcon = {
            Icon(
              Icons.Outlined.Place,
              contentDescription = null,
              tint = Color(0xFF9AA9BC),
              modifier = Modifier.size(19.dp)
            )
          },
          placeholder = {
            Text(
              "e.g., Central Mall - Level 2, Store 45",
              color = Color(0xFFA9B6C7),
              style = MapBodyMediumStyle
            )
          },
          textStyle = MapBodyMediumStyle.copy(color = NavyPrimary),
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          shape = RoundedCornerShape(8.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFFD1D5DB),
            unfocusedBorderColor = Color(0xFFD1D5DB),
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
          )
        )
      }

      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          "Type of Assistance Needed *",
          color = NavyPrimary,
          style = MapBodyMediumStyle
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .height(56.dp)
              .background(Color.White, RoundedCornerShape(8.dp))
              .border(2.dp, Color(0xFFD1D5DB), RoundedCornerShape(8.dp))
              .clickable { showHelpTypeMenu = !showHelpTypeMenu }
              .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = helpType.ifBlank { "Select help type..." },
              color = if (helpType.isBlank()) NavyPrimary.copy(alpha = 0.75f) else NavyPrimary,
              style = MapBodyMediumStyle,
              modifier = Modifier.weight(1f)
            )
            Icon(
              if (showHelpTypeMenu) Icons.Outlined.ArrowDropUp else Icons.Outlined.ArrowDropDown,
              contentDescription = null,
              tint = NavyPrimary,
              modifier = Modifier.size(22.dp)
            )
          }
          if (showHelpTypeMenu) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(8.dp))
                .border(2.dp, Color(0xFFD1D5DB), RoundedCornerShape(8.dp))
            ) {
              helpTypes.forEachIndexed { index, type ->
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                      helpType = type
                      showHelpTypeMenu = false
                      localError = null
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = type,
                    color = NavyPrimary,
                    style = MapBodyMediumStyle
                  )
                }
                if (index < helpTypes.lastIndex) {
                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(1.dp)
                      .background(Gray200)
                  )
                }
              }
            }
          }
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          "Urgency Level *",
          color = NavyPrimary,
          style = MapBodyMediumStyle
        )
        Row(
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          listOf("low", "medium", "high").forEach { level ->
            val selected = urgency == level
            val interactionSource = remember { MutableInteractionSource() }
            val hovered by interactionSource.collectIsHoveredAsState()
            val palette = when (level) {
              "medium" -> Triple(Color(0xFFFFF8F0), Color(0xFFF08E37), Color(0xFFB66B29))
              "high" -> Triple(Color(0xFFFFF1F1), Color(0xFFE97D7D), Color(0xFFB34747))
              else -> Triple(Color(0xFFEEF5FF), Color(0xFF8DB5E5), Color(0xFF37608E))
            }
            val background = when {
              selected -> palette.first
              hovered -> palette.first.copy(alpha = 0.65f)
              else -> Color(0xFFF1F4F9)
            }
            val border = when {
              selected -> palette.second
              hovered -> palette.second.copy(alpha = 0.7f)
              else -> Color.Transparent
            }
            val textColor = when {
              selected -> palette.third
              hovered -> palette.third.copy(alpha = 0.9f)
              else -> Color(0xFF64748B)
            }
            Box(
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .background(
                  background,
                  RoundedCornerShape(8.dp)
                )
                .border(
                  2.dp,
                  border,
                  RoundedCornerShape(8.dp)
                )
                .hoverable(interactionSource)
                .clickable(
                  interactionSource = interactionSource,
                  indication = null
                ) { urgency = level },
              contentAlignment = Alignment.Center
            ) {
              Text(
                level.replaceFirstChar { it.uppercase() },
                color = textColor,
                style = MapBodySmallStyle.copy(fontWeight = FontWeight.Medium)
              )
            }
          }
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          "Additional Details (Optional)",
          color = NavyPrimary,
          style = MapBodyMediumStyle
        )
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          placeholder = {
            Text(
              "Any additional information that might help the volunteer...",
              color = Color(0xFFB0BBCB),
              style = MapBodyMediumStyle
            )
          },
          textStyle = MapBodyMediumStyle.copy(color = NavyPrimary),
          modifier = Modifier.fillMaxWidth(),
          minLines = 3,
          maxLines = 4,
          shape = RoundedCornerShape(8.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFFD1D5DB),
            unfocusedBorderColor = Color(0xFFD1D5DB),
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
          )
        )
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFFFFFBEB), RoundedCornerShape(8.dp))
          .padding(16.dp),
        verticalAlignment = Alignment.Top
      ) {
        Icon(
          Icons.AutoMirrored.Outlined.Chat,
          contentDescription = null,
          tint = Color(0xFFF08E37),
          modifier = Modifier
            .size(18.dp)
            .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          "Your request will be broadcast to verified volunteers within 5km. You'll be matched with the nearest available volunteer.",
          color = NavyPrimary,
          style = MapBodyMediumStyle
        )
      }

      val canSubmit = !isSubmitting && location.isNotBlank() && destination.isNotBlank() && helpType.isNotBlank()
      Button(
        onClick = {
          if (location.isBlank() || destination.isBlank() || helpType.isBlank()) {
            localError = "Please fill in all required fields."
            return@Button
          }
          localError = null
          onSubmit(
            location.trim(),
            destination.trim(),
            helpType.trim(),
            urgency,
            description.trim()
          )
        },
        enabled = canSubmit,
        colors = ButtonDefaults.buttonColors(
          containerColor = NavyPrimary,
          disabledContainerColor = Color(0xFFD1D5DB),
          contentColor = Color.White,
          disabledContentColor = Color.White
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp)
      ) {
        Text(
          if (isSubmitting) "Broadcasting Request..." else "Send Request",
          style = MapBodyMediumStyle.copy(fontWeight = FontWeight.SemiBold)
        )
      }

      if (localError != null || errorMessage != null) {
        Text(
          localError ?: errorMessage.orEmpty(),
          color = Color(0xFFB91C1C),
          style = MapBodySmallStyle
        )
      }
    }
  }
}

private data class AutocompleteFetchResult(
  val predictions: List<AutocompletePrediction>,
  val errorMessage: String? = null
)

private data class FallbackSuggestion(
  val id: String,
  val title: String,
  val subtitle: String,
  val latLng: LatLng
)

private fun findBestLocalLocationMatch(
  query: String,
  locations: List<Location>
): Location? {
  if (locations.isEmpty()) return null
  val normalizedQuery = query.trim().lowercase(Locale.getDefault())
  if (normalizedQuery.isBlank()) return null
  val requirePrefixOnly = isPrefixOnlyQuery(normalizedQuery)

  return locations
    .map { location ->
      val rank = rankTextMatch(
        normalizedQuery = normalizedQuery,
        candidates = listOf(location.name, location.category),
        requirePrefixOnly = requirePrefixOnly
      )
      location to rank
    }
    .filter { (_, rank) -> rank <= if (requirePrefixOnly) 2 else 5 }
    .sortedWith(
      compareBy<Pair<Location, Int>> { it.second }
        .thenBy { it.first.name.length }
    )
    .firstOrNull()
    ?.first
}

private fun pickBestFallbackSuggestion(
  query: String,
  suggestions: List<FallbackSuggestion>
): FallbackSuggestion? {
  if (suggestions.isEmpty()) return null
  val normalizedQuery = query.trim().lowercase(Locale.getDefault())
  if (normalizedQuery.isBlank()) return suggestions.first()
  val requirePrefixOnly = isPrefixOnlyQuery(normalizedQuery)

  return suggestions
    .map { suggestion ->
      val rank = rankTextMatch(
        normalizedQuery = normalizedQuery,
        candidates = listOf(suggestion.title, suggestion.subtitle),
        requirePrefixOnly = requirePrefixOnly
      )
      suggestion to rank
    }
    .filter { (_, rank) -> rank <= if (requirePrefixOnly) 2 else 5 }
    .sortedWith(
      compareBy<Pair<FallbackSuggestion, Int>> { it.second }
        .thenBy { it.first.title.length }
    )
    .firstOrNull()
    ?.first
}

private fun rankTextMatch(
  normalizedQuery: String,
  candidates: List<String>,
  requirePrefixOnly: Boolean = false
): Int {
  val tokens = normalizedQuery
    .split("\\s+".toRegex())
    .map { it.trim() }
    .filter { it.isNotBlank() }

  return candidates.minOfOrNull { rawCandidate ->
    val candidate = rawCandidate.lowercase(Locale.getDefault())
    val hasWordPrefixMatch = hasWordPrefix(candidate, normalizedQuery)
    when {
      candidate == normalizedQuery -> 0
      candidate.startsWith(normalizedQuery) -> 1
      hasWordPrefixMatch -> 2
      requirePrefixOnly -> 6
      candidate.contains(normalizedQuery) -> 3
      tokens.isNotEmpty() && tokens.all { candidate.contains(it) } -> 4
      tokens.isNotEmpty() && tokens.any { candidate.contains(it) } -> 5
      else -> 6
    }
  } ?: 6
}

private fun buildLocalFallbackSuggestions(
  query: String,
  locations: List<Location>,
  reference: LatLng
): List<FallbackSuggestion> {
  if (locations.isEmpty()) return emptyList()
  val normalizedQuery = query.trim().lowercase(Locale.getDefault())
  val requirePrefixOnly = isPrefixOnlyQuery(normalizedQuery)
  val ranked = locations.map { location ->
    val target = LatLng(location.lat, location.lng)
    val matchRank = rankTextMatch(
      normalizedQuery = normalizedQuery,
      candidates = listOf(location.name, location.category),
      requirePrefixOnly = requirePrefixOnly
    )
    Triple(location, target, matchRank)
  }
  return ranked
    .filter { it.third <= if (requirePrefixOnly) 2 else 5 }
    .sortedWith(
      compareBy<Triple<Location, LatLng, Int>> { it.third }
        .thenBy { distanceMeters(reference, it.second) }
    )
    .take(6)
    .map { (location, target, _) ->
      val distance = formatDistance(distanceMeters(reference, target))
      FallbackSuggestion(
        id = "local:${location.id}",
        title = location.name,
        subtitle = "${location.category} • $distance away",
        latLng = target
      )
    }
}

private fun buildNearestFallbackSuggestions(
  locations: List<Location>,
  reference: LatLng
): List<FallbackSuggestion> {
  return locations
    .map { location ->
      val target = LatLng(location.lat, location.lng)
      val distance = formatDistance(distanceMeters(reference, target))
      FallbackSuggestion(
        id = "near:${location.id}",
        title = location.name,
        subtitle = "${location.category} • $distance away",
        latLng = target
      )
    }
    .sortedBy { distanceMeters(reference, it.latLng) }
    .take(6)
}

private suspend fun fetchGeocoderFallbackSuggestions(
  context: Context,
  query: String,
  reference: LatLng
): List<FallbackSuggestion> {
  val normalizedQuery = query.trim()
  if (normalizedQuery.length < 2) return emptyList()

  // Use Nominatim API (OpenStreetMap) for real-time search - completely FREE
  val nominatimResults = try {
    fetchNominatimSuggestions(normalizedQuery, reference)
  } catch (e: Exception) {
    Log.e("MapScreen", "Nominatim API failed: ${e.message}")
    emptyList()
  }

  // Fallback to Android Geocoder if Nominatim fails
  if (nominatimResults.isNotEmpty()) {
    return nominatimResults.take(8)
  }

  // Android Geocoder as final fallback
  if (!Geocoder.isPresent()) return emptyList()
  val requirePrefixOnly = isPrefixOnlyQuery(normalizedQuery.lowercase(Locale.getDefault()))
  val addresses = try {
    @Suppress("DEPRECATION")
    Geocoder(context, Locale.getDefault()).getFromLocationName(query, 10)
  } catch (_: IOException) {
    null
  } catch (_: Exception) {
    null
  }

  return addresses.orEmpty()
    .mapNotNull { address ->
      val lat = address.latitude
      val lng = address.longitude
      if (lat == 0.0 && lng == 0.0) return@mapNotNull null
      val target = LatLng(lat, lng)
      val title = address.featureName?.takeIf { it.isNotBlank() }
        ?: address.locality?.takeIf { it.isNotBlank() }
        ?: address.getAddressLine(0)?.takeIf { it.isNotBlank() }
        ?: return@mapNotNull null
      val rank = rankTextMatch(
        normalizedQuery = normalizedQuery.lowercase(Locale.getDefault()),
        candidates = listOf(title, address.getAddressLine(0).orEmpty()),
        requirePrefixOnly = requirePrefixOnly
      )
      if (rank > if (requirePrefixOnly) 2 else 5) return@mapNotNull null
      val subtitle = buildAddressSubtitle(address, reference, target)
      FallbackSuggestion(
        id = "geo:${title.lowercase(Locale.getDefault())}:${lat}:${lng}",
        title = title,
        subtitle = subtitle,
        latLng = target
      )
    }
    .sortedBy { distanceMeters(reference, it.latLng) }
    .take(8)
}

private fun buildAddressSubtitle(
  address: Address,
  reference: LatLng,
  target: LatLng
): String {
  val line = address.getAddressLine(0)
    ?.replaceFirst(address.featureName ?: "", "")
    ?.trim(',', ' ')
    ?.takeIf { it.isNotBlank() }
  val distance = formatDistance(distanceMeters(reference, target))
  return if (line != null) "$line • $distance away" else "$distance away"
}

private fun mergeFallbackSuggestions(
  first: List<FallbackSuggestion>,
  second: List<FallbackSuggestion>
): List<FallbackSuggestion> {
  if (first.isEmpty()) return second
  if (second.isEmpty()) return first
  val merged = linkedMapOf<String, FallbackSuggestion>()
  (first + second).forEach { suggestion ->
    val key = suggestion.title.lowercase(Locale.getDefault())
    if (!merged.containsKey(key)) {
      merged[key] = suggestion
    }
  }
  return merged.values.take(8)
}

private fun isPrefixOnlyQuery(normalizedQuery: String): Boolean {
  return normalizedQuery.length <= 2
}

private fun hasWordPrefix(candidate: String, normalizedQuery: String): Boolean {
  if (normalizedQuery.isBlank()) return false
  return candidate
    .split("[^\\p{L}\\p{N}]+".toRegex())
    .filter { it.isNotBlank() }
    .any { it.startsWith(normalizedQuery) }
}

private fun rankPredictionMatch(
  prediction: AutocompletePrediction,
  normalizedQuery: String,
  requirePrefixOnly: Boolean
): Int {
  return rankTextMatch(
    normalizedQuery = normalizedQuery,
    candidates = listOf(
      prediction.getPrimaryText(null).toString(),
      prediction.getFullText(null).toString()
    ),
    requirePrefixOnly = requirePrefixOnly
  )
}

private fun distanceMeters(from: LatLng, to: LatLng): Float {
  val result = FloatArray(1)
  AndroidLocation.distanceBetween(
    from.latitude,
    from.longitude,
    to.latitude,
    to.longitude,
    result
  )
  return result[0]
}

private fun formatDistance(distanceMeters: Float): String {
  return if (distanceMeters >= 1000f) {
    "${(distanceMeters / 1000f * 10f).roundToInt() / 10f} km"
  } else {
    "${distanceMeters.roundToInt()} m"
  }
}

private fun parseDistanceMeters(distance: String): Float {
  val normalized = distance.trim().lowercase(Locale.getDefault())
  val value = normalized.takeWhile { it.isDigit() || it == '.' }.toFloatOrNull() ?: return Float.MAX_VALUE
  return when {
    "km" in normalized -> value * 1000f
    "m" in normalized -> value
    else -> Float.MAX_VALUE
  }
}

private suspend fun fetchAutocompletePredictions(
  client: PlacesClient,
  query: String,
  token: AutocompleteSessionToken,
  origin: LatLng
): AutocompleteFetchResult = suspendCancellableCoroutine { continuation ->
  val bounds = RectangularBounds.newInstance(
    LatLng(origin.latitude - 1.2, origin.longitude - 1.2),
    LatLng(origin.latitude + 1.2, origin.longitude + 1.2)
  )
  val request = FindAutocompletePredictionsRequest.builder()
    .setQuery(query)
    .setSessionToken(token)
    .setLocationBias(bounds)
    .setOrigin(origin)
    .build()
  client.findAutocompletePredictions(request)
    .addOnSuccessListener { response ->
      continuation.resume(AutocompleteFetchResult(response.autocompletePredictions))
    }
    .addOnFailureListener { exception ->
      Log.e("MapScreen", "Places API Error: ${exception.javaClass.simpleName}: ${exception.message}", exception)
      continuation.resume(
        AutocompleteFetchResult(
          predictions = emptyList(),
          errorMessage = "Google suggestions unavailable: ${exception.message}"
        )
      )
    }
}

private suspend fun fetchPlaceDetails(
  client: PlacesClient,
  request: FetchPlaceRequest
): Place? = suspendCancellableCoroutine { continuation ->
  client.fetchPlace(request)
    .addOnSuccessListener { response ->
      continuation.resume(response.place)
    }
    .addOnFailureListener {
      continuation.resume(null)
    }
}

private suspend fun resolveBestPrediction(
  client: PlacesClient,
  predictions: List<AutocompletePrediction>
): Pair<AutocompletePrediction, LatLng>? {
  predictions.take(5).forEach { prediction ->
    val place = fetchPlaceDetails(
      client,
      FetchPlaceRequest.newInstance(prediction.placeId, listOf(Place.Field.LAT_LNG, Place.Field.NAME, Place.Field.ADDRESS))
    )
    val latLng = place?.latLng ?: return@forEach
    return prediction to latLng
  }

  return null
}

private fun openDirections(context: Context, destination: LatLng) {
  val destinationParam = "${destination.latitude},${destination.longitude}"
  val navigationIntent = Intent(
    Intent.ACTION_VIEW,
    Uri.parse("google.navigation:q=$destinationParam&mode=w")
  ).apply {
    setPackage("com.google.android.apps.maps")
    if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
  }
  val browserIntent = Intent(
    Intent.ACTION_VIEW,
    Uri.parse(
      "https://www.google.com/maps/dir/?api=1&destination=$destinationParam&travelmode=walking"
    )
  ).apply {
    if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
  }

  when {
    navigationIntent.resolveActivity(context.packageManager) != null -> context.startActivity(navigationIntent)
    browserIntent.resolveActivity(context.packageManager) != null -> context.startActivity(browserIntent)
  }
}

// Nominatim API integration for real-time place search (OpenStreetMap)
private suspend fun fetchNominatimSuggestions(
  query: String,
  reference: LatLng
): List<FallbackSuggestion> = withContext(Dispatchers.IO) {
  try {
    val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
    // Prioritize Egypt but allow worldwide search
    val url = "https://nominatim.openstreetmap.org/search?" +
      "q=$encodedQuery&" +
      "format=json&" +
      "limit=10&" +
      "countrycodes=eg&" +  // Prioritize Egypt
      "addressdetails=1&" +
      "accept-language=en"

    val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
    connection.setRequestProperty("User-Agent", "Athar-Accessibility-App/1.0")
    connection.connectTimeout = 5000
    connection.readTimeout = 5000

    if (connection.responseCode != 200) {
      Log.e("MapScreen", "Nominatim API returned ${connection.responseCode}")
      return@withContext emptyList()
    }

    val response = connection.inputStream.bufferedReader().use { it.readText() }
    parseNominatimResponse(response, reference)
  } catch (e: Exception) {
    Log.e("MapScreen", "Nominatim API error: ${e.message}", e)
    emptyList()
  }
}

private fun parseNominatimResponse(json: String, reference: LatLng): List<FallbackSuggestion> {
  try {
    // Simple JSON parsing without external library
    val results = mutableListOf<FallbackSuggestion>()
    val items = json.trim().removeSurrounding("[", "]").split("},{")

    for (item in items) {
      try {
        val lat = extractJsonValue(item, "lat")?.toDoubleOrNull() ?: continue
        val lon = extractJsonValue(item, "lon")?.toDoubleOrNull() ?: continue
        val displayName = extractJsonValue(item, "display_name") ?: continue
        val type = extractJsonValue(item, "type") ?: ""
        val name = extractJsonValue(item, "name") ?: displayName.split(",").firstOrNull() ?: continue

        val latLng = LatLng(lat, lon)
        val distance = distanceMeters(reference, latLng)

        // Create subtitle from display_name
        val parts = displayName.split(", ")
        val subtitle = if (parts.size > 1) {
          val location = parts.drop(1).take(2).joinToString(", ")
          "$location • ${formatDistance(distance)} away"
        } else {
          "${formatDistance(distance)} away"
        }

        results.add(
          FallbackSuggestion(
            id = "osm:$lat:$lon",
            title = name,
            subtitle = subtitle,
            latLng = latLng
          )
        )
      } catch (e: Exception) {
        continue
      }
    }

    return results
      .sortedBy { distanceMeters(reference, it.latLng) }
      .distinctBy { it.title }
  } catch (e: Exception) {
    Log.e("MapScreen", "Failed to parse Nominatim response: ${e.message}")
    return emptyList()
  }
}

private fun extractJsonValue(json: String, key: String): String? {
  val pattern = "\"$key\"\\s*:\\s*\"([^\"]*)\""
  val regex = Regex(pattern)
  return regex.find(json)?.groupValues?.get(1)
}

@Composable
private fun RowScope.FilterChipButton(
  label: String,
  selected: Boolean,
  onClick: () -> Unit
) {
  Button(
    onClick = onClick,
    colors = ButtonDefaults.buttonColors(
      containerColor = if (selected) NavyPrimary else Color(0xFFC6D6E8),
      contentColor = if (selected) Color.White else NavyPrimary
    ),
    shape = RoundedCornerShape(10.dp),
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      if (selected) NavyPrimary else NavyPrimary
    ),
    modifier = Modifier
      .weight(1f)
      .height(40.dp),
    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
  ) {
    Text(
      label,
      style = MapLabelSmallStyle.copy(fontWeight = FontWeight.Medium),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}
