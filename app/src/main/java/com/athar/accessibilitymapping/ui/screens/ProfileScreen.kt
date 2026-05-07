package com.athar.accessibilitymapping.ui.screens

import android.view.MotionEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.focusable
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.athar.accessibilitymapping.data.AtharRepository
import com.athar.accessibilitymapping.data.ContributionStats
import com.athar.accessibilitymapping.data.UserProfile
import com.athar.accessibilitymapping.data.UserRole
import com.athar.accessibilitymapping.ui.components.AtharPullToRefresh
import com.athar.accessibilitymapping.ui.components.ProfilePhoto
import com.athar.accessibilitymapping.ui.icons.LucideIcons as AtharIcons
import com.athar.accessibilitymapping.ui.localization.AppLanguage
import com.composables.icons.lucide.*
import com.composables.icons.lucide.Lucide
import com.athar.accessibilitymapping.ui.theme.AccentGold
import com.athar.accessibilitymapping.ui.theme.AccentGoldDark
import com.athar.accessibilitymapping.ui.theme.BluePrimary
import com.athar.accessibilitymapping.ui.theme.BlueSecondary
import com.athar.accessibilitymapping.ui.theme.Gray200
import com.athar.accessibilitymapping.ui.theme.NavyDark
import com.athar.accessibilitymapping.ui.theme.NavyPrimary
import com.athar.accessibilitymapping.ui.theme.sdp
import com.athar.accessibilitymapping.ui.theme.ssp

private data class ProfileMenuItem(
  val icon: ImageVector,
  val label: String,
  val description: String,
  val screen: String
)

private val ProfileHeaderTextStyle = TextStyle(
  fontFamily = FontFamily.SansSerif,
  fontWeight = FontWeight.Bold,
  fontSize = 30.sp,
  lineHeight = 36.sp
)

private val ProfileTitleLargeStyle = TextStyle(
  fontFamily = FontFamily.SansSerif,
  fontWeight = FontWeight.SemiBold,
  fontSize = 20.sp,
  lineHeight = 28.sp
)

private val ProfileTitleSmallStyle = TextStyle(
  fontFamily = FontFamily.SansSerif,
  fontWeight = FontWeight.SemiBold,
  fontSize = 16.sp,
  lineHeight = 24.sp
)

private val ProfileBodyStyle = TextStyle(
  fontFamily = FontFamily.SansSerif,
  fontWeight = FontWeight.Normal,
  fontSize = 16.sp,
  lineHeight = 24.sp
)

private val ProfileBodySmallStyle = TextStyle(
  fontFamily = FontFamily.SansSerif,
  fontWeight = FontWeight.Normal,
  fontSize = 14.sp,
  lineHeight = 20.sp
)

private val ProfileFeatureButtonTextStyle = TextStyle(
  fontFamily = FontFamily.SansSerif,
  fontWeight = FontWeight.SemiBold,
  fontSize = 16.sp,
  lineHeight = 22.sp
)

private val ProfileStatValueStyle = TextStyle(
  fontFamily = FontFamily.SansSerif,
  fontWeight = FontWeight.Bold,
  fontSize = 24.sp,
  lineHeight = 32.sp
)

private val ProfileStatLabelStyle = TextStyle(
  fontFamily = FontFamily.SansSerif,
  fontWeight = FontWeight.SemiBold,
  fontSize = 14.sp,
  lineHeight = 20.sp
)

private val RoleLockBackground = Color(0xFFFFF8DC)
private val LogoutBorder = Color(0xFFB91C1C)
private val LogoutText = Color(0xFFB91C1C)
private val LogoutHoverBackground = Color(0xFFFEE2E2)
private val HelpfulGreen = Color(0xFF16A34A)
private val HelpfulGreenBorder = Color(0xFF15803D)

@Composable
fun ProfileScreen(
  userRole: UserRole,
  userId: String,
  userName: String,
  userEmail: String,
  userDisabilityType: String?,
  profilePhotoPath: String?,
  currentLanguage: AppLanguage,
  onLanguageChange: (AppLanguage) -> Unit,
  onProfilePhotoChanged: (String?) -> Unit,
  onAccountRefreshRequested: () -> Unit,
  onLogout: () -> Unit
) {
  val profileViewModel: ProfileViewModel = viewModel(key = "profile-$userId")
  val profile by profileViewModel.profile.collectAsState()
  val isProfileLoaded by profileViewModel.isLoaded.collectAsState()
  var activeScreen by remember(userId) { mutableStateOf("profile") }
  var showLogoutDialog by remember(userId) { mutableStateOf(false) }

  BackHandler(enabled = activeScreen != "profile") {
    activeScreen = if (activeScreen == "changePassword") "account" else "profile"
  }

  if (activeScreen == "account") {
    AccountSettingsScreen(
      onBack = { activeScreen = "profile" },
      userId = userId,
      userRole = userRole,
      userDisabilityType = userDisabilityType,
      currentLanguage = currentLanguage,
      profilePhotoPath = profilePhotoPath,
      onLanguageChange = onLanguageChange,
      onProfilePhotoChanged = onProfilePhotoChanged,
      onAccountRefreshRequested = onAccountRefreshRequested,
      onOpenChangePassword = { activeScreen = "changePassword" }
    )
    return
  }
  if (activeScreen == "changePassword") {
    ChangePasswordScreen(onBack = { activeScreen = "account" })
    return
  }
  if (activeScreen == "notifications") {
    NotificationsScreen(onBack = { activeScreen = "profile" })
    return
  }
  if (activeScreen == "privacy") {
    PrivacySecurityScreen(onBack = { activeScreen = "profile" })
    return
  }
  if (activeScreen == "help") {
    HelpSupportScreen(onBack = { activeScreen = "profile" })
    return
  }
  if (activeScreen == "analytics") {
    VolunteerAnalyticsScreen(userId = userId, onBack = { activeScreen = "profile" })
    return
  }
  if (activeScreen == "signTranslator") {
    SignLanguageTranslatorScreen(onBack = { activeScreen = "profile" })
    return
  }
  if (activeScreen == "addPlaceReport") {
    AddPlaceReportScreen(onBack = { activeScreen = "profile" })
    return
  }

  val displayName = profile.name.ifBlank { userName }
  val displayEmail = profile.email.ifBlank { userEmail }
  val displayPhone = profile.phone.ifBlank { "+966 50 123 4567" }
  val displayMemberSince = profile.memberSince.ifBlank { "March 2024" }

  val menuItems = listOf(
    ProfileMenuItem(Lucide.Settings, "Account Settings", "Update your personal information", "account"),
    ProfileMenuItem(Lucide.Bell, "Notifications", "Manage notification preferences", "notifications"),
    ProfileMenuItem(Lucide.Shield, "Privacy & Security", "Control your data and privacy", "privacy"),
    ProfileMenuItem(Lucide.CircleQuestionMark, "Help & Support", "Get help or send feedback", "help")
  )

  var logoutInteractionSourceKey by remember(userId) { mutableStateOf(0) }
  val logoutInteractionSource = remember(logoutInteractionSourceKey) { MutableInteractionSource() }
  val isLogoutHovered by logoutInteractionSource.collectIsHoveredAsState()
  val isLogoutPressed by logoutInteractionSource.collectIsPressedAsState()
  val isLogoutFocused by logoutInteractionSource.collectIsFocusedAsState()
  var isLogoutHoverInterop by remember { mutableStateOf(false) }

  AtharPullToRefresh(
    isRefreshing = !isProfileLoaded,
    onRefresh = profileViewModel::refresh
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .background(BluePrimary)
    ) {
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(NavyPrimary)
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 34.dp)
        ) {
          Text(
            text = "Prof\u200Cile",
            color = Color.White,
            style = ProfileHeaderTextStyle
          )
          Spacer(modifier = Modifier.height(16.dp))
          Row(
            modifier = Modifier
              .background(NavyDark, RoundedCornerShape(999.dp))
              .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = if (userRole == UserRole.User) Lucide.User else Lucide.Award,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (userRole == UserRole.User) "User Account" else "Volunteer Account",
              color = Color.White,
              style = ProfileBodySmallStyle
            )
          }
        }
      }

      item {
        Column(
          modifier = Modifier
            .padding(horizontal = 16.dp)
            .offset(y = (-16).dp)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .shadow(10.dp, RoundedCornerShape(16.dp))
              .background(Color.White, RoundedCornerShape(16.dp))
              .padding(24.dp)
          ) {
          Row(verticalAlignment = Alignment.Top) {
            ProfilePhoto(
              photoPath = profilePhotoPath,
              modifier = Modifier
                .size(80.dp),
              backgroundColor = NavyPrimary,
              borderColor = NavyDark,
              placeholderTint = Color.White,
              placeholderIcon = Lucide.User,
              placeholderIconSize = 40.dp
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = displayName,
                  color = NavyPrimary,
                  style = ProfileTitleLargeStyle,
                  modifier = Modifier.weight(1f)
                )
                Box(
                  modifier = Modifier
                    .background(BlueSecondary, RoundedCornerShape(8.dp))
                    .border(1.dp, Gray200, RoundedCornerShape(8.dp))
                    .padding(4.dp)
                ) {
                  Icon(Lucide.Pencil, contentDescription = "Edit profile", tint = NavyPrimary, modifier = Modifier.size(16.dp))
                }
              }

              Spacer(modifier = Modifier.height(4.dp))

              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Lucide.Mail, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(displayEmail, color = NavyPrimary, style = ProfileBodySmallStyle)
              }

              Spacer(modifier = Modifier.height(2.dp))

              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Lucide.Phone, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(displayPhone, color = NavyPrimary, style = ProfileBodySmallStyle)
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(BlueSecondary, RoundedCornerShape(8.dp))
              .border(1.5.dp, NavyPrimary, RoundedCornerShape(8.dp))
              .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Lucide.Calendar, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Member since $displayMemberSince", color = NavyPrimary, style = ProfileBodySmallStyle)
          }

          if (!userDisabilityType.isNullOrBlank() && userRole == UserRole.User) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(AccentGold, RoundedCornerShape(8.dp))
                .border(1.5.dp, AccentGoldDark, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
              Text(
                text = "Accessibility needs: $userDisabilityType",
                color = Color.White,
                style = ProfileBodySmallStyle
              )
            }
          }
          }
        }
      }

    item {
      Column(
        modifier = Modifier
          .padding(horizontal = 16.dp)
          .offset(y = (2).dp)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .background(RoleLockBackground, RoundedCornerShape(16.dp))
            .border(2.dp, AccentGoldDark, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
          Icon(
            Lucide.CircleAlert,
            contentDescription = null,
            tint = AccentGoldDark,
            modifier = Modifier
              .size(24.dp)
              .padding(top = 2.dp)
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              "Role Locked",
              color = NavyPrimary,
              style = ProfileBodyStyle.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              "Your account role is permanent and can only be changed by admin verification. Contact support if you need to switch roles.",
              color = NavyPrimary,
              style = ProfileBodySmallStyle
            )
          }
        }
      }
      Spacer(modifier = Modifier.height(20.dp))
    }

    item {
      ProfileFeatureButton(
        text = "Sign Language Translator",
        icon = Lucide.Hand,
        brush = Brush.linearGradient(listOf(AccentGold, AccentGoldDark)),
        borderColor = AccentGoldDark,
        iconBackground = NavyPrimary,
        iconBorderColor = NavyDark,
        onClick = { activeScreen = "signTranslator" }
      )
      Spacer(modifier = Modifier.height(12.dp))
    }

    if (userRole == UserRole.Volunteer) {
      item {
        ProfileFeatureButton(
          text = "Analytics Dashboard",
          icon = AtharIcons.AnalyticsDashboard,
          brush = Brush.linearGradient(listOf(NavyPrimary, NavyPrimary)),
          borderColor = NavyDark,
          iconBackground = AccentGold,
          iconBorderColor = AccentGoldDark,
          onClick = { activeScreen = "analytics" }
        )
        Spacer(modifier = Modifier.height(16.dp))
      }
    }

    if (userRole == UserRole.User) {
      item {
        AddPlaceReportButton(onClick = { activeScreen = "addPlaceReport" })
        Spacer(modifier = Modifier.height(16.dp))
      }
    }

    item {
      Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(16.dp))
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(2.dp, Gray200, RoundedCornerShape(16.dp))
        ) {
          menuItems.forEachIndexed { index, item ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { activeScreen = item.screen }
                .padding(horizontal = 16.dp, vertical = 16.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(48.dp)
                  .clip(CircleShape)
                  .background(NavyPrimary, CircleShape)
                  .border(2.dp, NavyDark, CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Icon(item.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
              }

              Spacer(modifier = Modifier.width(16.dp))

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = item.label,
                  color = NavyPrimary,
                  style = ProfileTitleSmallStyle
                )
                Text(
                  text = item.description,
                  color = NavyPrimary,
                  style = ProfileBodySmallStyle
                )
              }

              Icon(Lucide.ChevronRight, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(24.dp))
            }

            if (index != menuItems.lastIndex) {
              Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Gray200))
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(16.dp))
    }

    item {
      Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .shadow(10.dp, RoundedCornerShape(16.dp))
            .background(
              if (isLogoutHovered || isLogoutHoverInterop || isLogoutPressed || isLogoutFocused) LogoutHoverBackground else Color.White,
              RoundedCornerShape(16.dp)
            )
            .border(3.dp, LogoutBorder, RoundedCornerShape(16.dp))
            .hoverable(interactionSource = logoutInteractionSource)
            .focusable(interactionSource = logoutInteractionSource)
            .pointerInteropFilter { event ->
              when (event.actionMasked) {
                MotionEvent.ACTION_HOVER_ENTER, MotionEvent.ACTION_HOVER_MOVE -> isLogoutHoverInterop = true
                MotionEvent.ACTION_DOWN -> isLogoutHoverInterop = true
                MotionEvent.ACTION_UP -> isLogoutHoverInterop = false
                MotionEvent.ACTION_HOVER_EXIT, MotionEvent.ACTION_CANCEL -> isLogoutHoverInterop = false
              }
              false
            }
            .clickable(
              interactionSource = logoutInteractionSource,
              indication = null
            ) {
              isLogoutHoverInterop = false
              showLogoutDialog = true
            }
            .padding(vertical = 16.dp),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Lucide.LogOut, contentDescription = null, tint = LogoutText, modifier = Modifier.size(24.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            "Log Out",
            color = LogoutText,
            style = ProfileTitleSmallStyle
          )
        }
      }
      Spacer(modifier = Modifier.height(24.dp))
    }
    }
  }

  if (showLogoutDialog) {
    LogoutConfirmationDialog(
      onConfirm = {
        isLogoutHoverInterop = false
        logoutInteractionSourceKey += 1
        showLogoutDialog = false
        onLogout()
      },
      onCancel = {
        isLogoutHoverInterop = false
        logoutInteractionSourceKey += 1
        showLogoutDialog = false
      }
    )
  }
}

@Composable
private fun ProfileFeatureButton(
  text: String,
  icon: ImageVector,
  brush: Brush,
  borderColor: Color,
  iconBackground: Color,
  iconBorderColor: Color,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .padding(horizontal = 16.dp)
      .fillMaxWidth()
      .height(64.dp)
      .shadow(8.dp, RoundedCornerShape(16.dp))
      .clip(RoundedCornerShape(16.dp))
      .background(brush)
      .border(2.dp, borderColor, RoundedCornerShape(16.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 24.dp),
    horizontalArrangement = Arrangement.Start,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .padding(start = 28.dp)
        .size(34.dp)
        .clip(CircleShape)
        .background(iconBackground, CircleShape)
        .border(2.dp, iconBorderColor, CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
    }
    Spacer(modifier = Modifier.width(12.dp))
    Text(
      text = text,
      color = Color.White,
      style = ProfileFeatureButtonTextStyle
    )
  }
}
