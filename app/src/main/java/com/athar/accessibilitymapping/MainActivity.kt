package com.athar.accessibilitymapping

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.athar.accessibilitymapping.data.AuthOperationResult
import com.athar.accessibilitymapping.data.AppPreferencesStore
import com.athar.accessibilitymapping.data.AuthRepository
import com.athar.accessibilitymapping.data.AtharRepository
import com.athar.accessibilitymapping.data.ApiCallResult
import com.athar.accessibilitymapping.data.UserRole
import com.athar.accessibilitymapping.data.volunteer.AtharVolunteerDashboardRepository
import com.athar.accessibilitymapping.ui.screens.LoginScreen
import com.athar.accessibilitymapping.ui.screens.MapScreen
import com.athar.accessibilitymapping.ui.screens.ProfileScreen
import com.athar.accessibilitymapping.ui.screens.RequestsScreen
import com.athar.accessibilitymapping.ui.screens.RoleSelectionScreen
import com.athar.accessibilitymapping.ui.screens.AtharSplashScreen
import com.athar.accessibilitymapping.ui.icons.LucideIcons
import com.athar.accessibilitymapping.ui.localization.AppLanguage
import com.athar.accessibilitymapping.ui.localization.ProvideAppLocalization
import com.athar.accessibilitymapping.ui.theme.AtharTheme
import com.athar.accessibilitymapping.ui.theme.Gray200
import com.athar.accessibilitymapping.ui.theme.NavyPrimary
import com.athar.accessibilitymapping.util.resolveMapsApiKey
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.libraries.places.api.Places
import kotlinx.coroutines.launch

open class AtharActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)

    initializePlaces()

    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.auto(
        lightScrim = android.graphics.Color.TRANSPARENT,
        darkScrim = android.graphics.Color.TRANSPARENT
      ),
      navigationBarStyle = SystemBarStyle.auto(
        lightScrim = android.graphics.Color.TRANSPARENT,
        darkScrim = android.graphics.Color.TRANSPARENT
      )
    )
    setContent {
      AtharTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          AppRoot()
        }
      }
    }
  }

  private fun initializePlaces() {
    val mapsApiKey = resolveMapsApiKey(applicationContext)
    if (mapsApiKey.isBlank() || Places.isInitialized()) return
    runCatching {
      Places.initialize(applicationContext, mapsApiKey)
    }
  }
}

private enum class MainTab {
  Map,
  Requests,
  Profile
}

private object SplashSessionTracker {
  var hasShownSplashInProcess: Boolean = false
}

@Composable
private fun AppRoot() {
  val context = LocalContext.current
  val authRepository = remember(context) { AuthRepository(context) }
  val repository = remember(context) { AtharRepository(context) }
  val appPreferences = remember(context) { AppPreferencesStore(context) }
  val coroutineScope = rememberCoroutineScope()
  var userRole by rememberSaveable { mutableStateOf<UserRole?>(null) }
  var isAuthenticated by rememberSaveable { mutableStateOf(false) }
  var authView by rememberSaveable { mutableStateOf("login") }
  var currentTab by rememberSaveable { mutableStateOf(MainTab.Map) }
  var showSplash by rememberSaveable { mutableStateOf(!SplashSessionTracker.hasShownSplashInProcess) }
  var isVolunteerLive by rememberSaveable { mutableStateOf(false) }
  var currentUserId by rememberSaveable { mutableStateOf("") }
  var currentUserName by rememberSaveable { mutableStateOf("Guest") }
  var currentUserEmail by rememberSaveable { mutableStateOf("") }
  var currentUserDisabilityType by rememberSaveable { mutableStateOf<String?>(null) }
  var currentUserPhotoPath by rememberSaveable { mutableStateOf<String?>(null) }
  var appLanguageCode by rememberSaveable { mutableStateOf(AppLanguage.English.code) }
  var preferencesLoaded by rememberSaveable { mutableStateOf(false) }
  var restoredOnce by rememberSaveable { mutableStateOf(false) }
  var authNotice by rememberSaveable { mutableStateOf<String?>(null) }
  var shouldRefreshAccount by rememberSaveable { mutableStateOf(false) }

  val role = userRole
  val userName = if (currentUserName != "Guest") {
    currentUserName
  } else {
    when (role) {
      UserRole.User -> "Layla Abdullah"
      UserRole.Volunteer -> "Sara Mohammed"
      null -> "Guest"
    }
  }
  val userDisabilityType = if (role == UserRole.User) currentUserDisabilityType else null
  val isVolunteerVerified = true
  val appLanguage = AppLanguage.fromCode(appLanguageCode)

  LaunchedEffect(preferencesLoaded) {
    if (preferencesLoaded) return@LaunchedEffect
    appLanguageCode = appPreferences.readLanguage()
    preferencesLoaded = true
  }

  LaunchedEffect(restoredOnce) {
    if (restoredOnce) return@LaunchedEffect
    val restored = authRepository.restoreSession()
    if (restored != null) {
      val backendReachable = authRepository.isBackendReachable()
      val localSession = authRepository.hasLocalSession()
      if (backendReachable && localSession) {
        authRepository.clearStoredSession()
        authNotice = "Signed out of offline mode. Sign in again to load live analytics data."
        restoredOnce = true
        return@LaunchedEffect
      }
      currentUserId = restored.userId
      userRole = restored.role
      currentUserName = restored.fullName
      currentUserEmail = restored.email
      currentUserDisabilityType = restored.disabilityType
      isVolunteerLive = restored.volunteerLive
      currentTab = MainTab.Map
      isAuthenticated = true
      shouldRefreshAccount = false
    } else {
      userRole = null
      currentUserId = ""
      currentUserName = "Guest"
      currentUserEmail = ""
      currentUserDisabilityType = null
      currentUserPhotoPath = null
      isVolunteerLive = false
      isAuthenticated = false
      currentTab = MainTab.Map
      authView = "login"
      shouldRefreshAccount = false
    }
    restoredOnce = true
  }

  LaunchedEffect(authNotice) {
    val message = authNotice?.trim().orEmpty()
    if (message.isNotBlank()) {
      Toast.makeText(context, message, Toast.LENGTH_LONG).show()
      authNotice = null
    }
  }

  LaunchedEffect(currentUserId) {
    currentUserPhotoPath = if (currentUserId.isBlank()) {
      null
    } else {
      appPreferences.readProfilePhotoPath(currentUserId)
    }
  }

  LaunchedEffect(shouldRefreshAccount, isAuthenticated, currentUserId) {
    if (!shouldRefreshAccount || !isAuthenticated || currentUserId.isBlank()) return@LaunchedEffect
    when (val accountResult = repository.getCurrentAccount()) {
      is ApiCallResult.Success -> {
        currentUserId = accountResult.data.id
        currentUserName = accountResult.data.fullName.ifBlank { currentUserName }
        currentUserEmail = accountResult.data.email.ifBlank { currentUserEmail }
        currentUserDisabilityType = accountResult.data.disabilityType ?: currentUserDisabilityType
        isVolunteerLive = accountResult.data.volunteerLive
      }
      is ApiCallResult.Failure -> Unit
    }
    shouldRefreshAccount = false
  }

  Box(modifier = Modifier.fillMaxSize()) {
    if (preferencesLoaded) {
      ProvideAppLocalization(language = appLanguage) {
        if (!isAuthenticated || role == null) {
          if (authView == "login") {
            LoginScreen(
              onLogin = { email, password ->
                when (val result = authRepository.login(email, password)) {
                  is AuthOperationResult.Success -> {
                    AtharVolunteerDashboardRepository.clearCachedDashboards()
                    val session = result.session
                    currentUserId = session.userId
                    userRole = session.role
                    currentUserName = session.fullName
                    currentUserEmail = session.email
                    currentUserDisabilityType = session.disabilityType
                    isVolunteerLive = session.volunteerLive
                    currentTab = MainTab.Map
                    isAuthenticated = true
                    shouldRefreshAccount = true
                    null
                  }
                  is AuthOperationResult.Error -> result.message
                }
              },
              onRegister = { authView = "register" }
            )
          } else if (authView == "register") {
            BackHandler { authView = "login" }
            RoleSelectionScreen(
              onComplete = { session ->
                AtharVolunteerDashboardRepository.clearCachedDashboards()
                currentUserId = session.userId
                userRole = session.role
                currentUserName = session.fullName
                currentUserEmail = session.email
                currentUserDisabilityType = session.disabilityType
                isVolunteerLive = session.volunteerLive
                currentTab = MainTab.Map
                isAuthenticated = true
                authView = "login"
                shouldRefreshAccount = true
              },
              onBack = { authView = "login" },
              onRegisterUser = { payload ->
                authRepository.registerUser(payload)
              },
              onRegisterVolunteer = { payload ->
                authRepository.registerVolunteer(payload)
              }
            )
          }
        } else {
          BackHandler(enabled = currentTab != MainTab.Map) { currentTab = MainTab.Map }
          Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
              // MapScreen stays composed always so Google Map is never destroyed
              Box(
                modifier = if (currentTab == MainTab.Map) Modifier.fillMaxSize()
                else Modifier.size(1.dp)
              ) {
                MapScreen(
                  userRole = role,
                  userDisabilityType = userDisabilityType,
                  isVolunteerLive = isVolunteerLive,
                  isVolunteerVerified = isVolunteerVerified,
                  onVolunteerLiveToggle = { isVolunteerLive = it }
                )
              }

              // Other tabs swap in/out normally
              if (currentTab != MainTab.Map) {
                when (currentTab) {
                  MainTab.Requests -> RequestsScreen(
                    userRole = role,
                    userId = currentUserId,
                    isVolunteerLive = isVolunteerLive,
                    userName = userName
                  )
                  MainTab.Profile -> ProfileScreen(
                    userRole = role,
                    userId = currentUserId,
                    userName = userName,
                    userEmail = currentUserEmail,
                    userDisabilityType = userDisabilityType,
                    profilePhotoPath = currentUserPhotoPath,
                    currentLanguage = appLanguage,
                    onLanguageChange = { language ->
                      appLanguageCode = language.code
                      coroutineScope.launch {
                        appPreferences.saveLanguage(language.code)
                      }
                    },
                    onProfilePhotoChanged = { photoPath ->
                      currentUserPhotoPath = photoPath
                      val activeUserId = currentUserId
                      coroutineScope.launch {
                        if (activeUserId.isNotBlank()) {
                          appPreferences.saveProfilePhotoPath(activeUserId, photoPath)
                        }
                      }
                    },
                    onLogout = {
                      coroutineScope.launch {
                        authRepository.logout()
                        AtharVolunteerDashboardRepository.clearCachedDashboards()
                        userRole = null
                        currentUserId = ""
                        currentUserName = "Guest"
                        currentUserEmail = ""
                        currentUserDisabilityType = null
                        currentUserPhotoPath = null
                        isVolunteerLive = false
                        isAuthenticated = false
                        currentTab = MainTab.Map
                        authView = "login"
                        shouldRefreshAccount = false
                      }
                    }
                  )
                  else -> {}
                }
              }
            }

            Column(
              modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
            ) {
              HorizontalDivider(thickness = 2.dp, color = Gray200)
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(80.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
              ) {
                BottomTabButton(
                  modifier = Modifier.weight(1f),
                  label = "Map",
                  selected = currentTab == MainTab.Map,
                  icon = {
                    Icon(
                      imageVector = LucideIcons.Map,
                      contentDescription = "Map view",
                      modifier = Modifier.size(28.dp)
                    )
                  },
                  onClick = { currentTab = MainTab.Map }
                )
                BottomTabButton(
                  modifier = Modifier.weight(1f),
                  label = if (role == UserRole.Volunteer) "Dashboard" else "Requests",
                  selected = currentTab == MainTab.Requests,
                  icon = {
                    Icon(
                      imageVector = LucideIcons.MessageCircle,
                      contentDescription = "Requests view",
                      modifier = Modifier.size(28.dp)
                    )
                  },
                  onClick = { currentTab = MainTab.Requests }
                )
                BottomTabButton(
                  modifier = Modifier.weight(1f),
                  label = "Prof\u200Cile",
                  selected = currentTab == MainTab.Profile,
                  icon = {
                    Icon(
                      imageVector = LucideIcons.User,
                      contentDescription = "Profile view",
                      modifier = Modifier.size(28.dp)
                    )
                  },
                  onClick = { currentTab = MainTab.Profile }
                )
              }
            }
          }
        }
      }
    }

    if (showSplash) {
      AtharSplashScreen(
        onComplete = {
          showSplash = false
          SplashSessionTracker.hasShownSplashInProcess = true
        }
      )
    }
  }
}

@Composable
private fun BottomTabButton(
  modifier: Modifier = Modifier,
  label: String,
  selected: Boolean,
  icon: @Composable () -> Unit,
  onClick: () -> Unit
) {
  val color = if (selected) NavyPrimary else Color(0xFF9CA3AF)
  Box(
    modifier = modifier
      .height(80.dp)
      .clickable(onClick = onClick),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.material3.LocalContentColor provides color
      ) {
        icon()
      }
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = label,
        color = color,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal
      )
    }
  }
}
