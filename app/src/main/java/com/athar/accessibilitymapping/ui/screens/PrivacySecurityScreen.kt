package com.athar.accessibilitymapping.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.UserCheck
import androidx.compose.material.icons.outlined.Visibility
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.athar.accessibilitymapping.ui.components.AtharPullToRefresh
import com.athar.accessibilitymapping.ui.components.ScreenHeader
import com.athar.accessibilitymapping.ui.components.ToggleSwitch
import com.athar.accessibilitymapping.ui.theme.AccentGold
import com.athar.accessibilitymapping.ui.theme.AccentGoldDark
import com.athar.accessibilitymapping.ui.theme.BluePrimary
import com.athar.accessibilitymapping.ui.theme.BlueSecondary
import com.athar.accessibilitymapping.ui.theme.Gray200
import com.athar.accessibilitymapping.ui.theme.NavyDark
import com.athar.accessibilitymapping.ui.theme.NavyPrimary
import com.athar.accessibilitymapping.ui.theme.sdp
import com.athar.accessibilitymapping.ui.theme.ssp

@Composable
fun PrivacySecurityScreen(onBack: () -> Unit) {
  val viewModel: PrivacySecurityViewModel = viewModel()
  viewModel.loadIfNeeded()

  val locationSharing by viewModel.locationSharing.collectAsState()
  val profileVisibility by viewModel.profileVisibility.collectAsState()
  val showRatings by viewModel.showRatings.collectAsState()
  val activityStatus by viewModel.activityStatus.collectAsState()
  val twoFactorAuth by viewModel.twoFactorAuth.collectAsState()
  val isLoading by viewModel.isLoading.collectAsState()
  val errorMessage by viewModel.errorMessage.collectAsState()

  var showChangePassword by remember { mutableStateOf(false) }
  var showActiveSessions by remember { mutableStateOf(false) }
  var showDownloadData by remember { mutableStateOf(false) }
  var showPrivacyPolicy by remember { mutableStateOf(false) }
  var showTermsOfService by remember { mutableStateOf(false) }

  if (showChangePassword) {
    ChangePasswordScreen(onBack = { showChangePassword = false })
    return
  }
  if (showActiveSessions) {
    ActiveSessionsScreen(onBack = { showActiveSessions = false })
    return
  }
  if (showDownloadData) {
    DownloadDataScreen(onBack = { showDownloadData = false })
    return
  }
  if (showPrivacyPolicy) {
    PrivacyPolicyScreen(onBack = { showPrivacyPolicy = false })
    return
  }
  if (showTermsOfService) {
    TermsOfServiceScreen(onBack = { showTermsOfService = false })
    return
  }

  AtharPullToRefresh(
    isRefreshing = isLoading,
    onRefresh = viewModel::refresh
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.White)
    ) {
      ScreenHeader(title = "Privacy & Security", onBack = onBack, background = NavyPrimary)

      Column(
        modifier = Modifier
          .fillMaxSize()
          .background(BluePrimary)
          .verticalScroll(rememberScrollState())
          .padding(16.dp)
      ) {
        if (isLoading) {
          Text("Loading privacy settings...", color = NavyPrimary, fontSize = 14.sp)
          Spacer(modifier = Modifier.height(12.dp))
        }
        if (errorMessage != null) {
          Text(errorMessage ?: "", color = Color(0xFFB91C1C), fontSize = 14.sp)
          Spacer(modifier = Modifier.height(12.dp))
        }

        PrivacySection(title = "Privacy Settings") {
          PrivacyToggleRow(
            icon = Icons.Outlined.Place,
            iconBackground = NavyPrimary,
            iconBorder = NavyDark,
            title = "Location Sharing",
            subtitle = "Share your location with volunteers",
            enabled = locationSharing
          ) { viewModel.toggleLocationSharing() }
          PrivacyToggleRow(
            icon = Icons.Outlined.Visibility,
            iconBackground = Color(0xFF10B981),
            iconBorder = Color(0xFF059669),
            title = "Profile Visibility",
            subtitle = "Allow others to see your profile",
            enabled = profileVisibility
          ) { viewModel.toggleProfileVisibility() }
          PrivacyToggleRow(
            icon = Icons.Outlined.UserCheck,
            iconBackground = AccentGold,
            iconBorder = AccentGoldDark,
            title = "Show My Ratings",
            subtitle = "Display ratings you've given",
            enabled = showRatings
          ) { viewModel.toggleShowRatings() }
          PrivacyToggleRow(
            icon = Icons.Outlined.Visibility,
            iconBackground = NavyPrimary,
            iconBorder = NavyDark,
            title = "Activity Status",
            subtitle = "Show when you're active",
            enabled = activityStatus,
            showDivider = false
          ) { viewModel.toggleActivityStatus() }
        }

        Spacer(modifier = Modifier.height(16.dp))

        PrivacySection(title = "Security") {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(Color(0xFFFFF8DC), RoundedCornerShape(8.dp))
              .border(2.dp, AccentGold, RoundedCornerShape(8.dp))
              .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              modifier = Modifier.weight(1f),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .background(AccentGold, CircleShape)
                  .border(2.dp, AccentGoldDark, CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Outlined.Shield, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text("Two-Factor Authentication", color = NavyPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text("Add extra security to your account", color = NavyPrimary.copy(alpha = 0.8f), fontSize = 14.sp)
              }
            }
            Spacer(modifier = Modifier.width(12.dp))
            ToggleSwitch(
              enabled = twoFactorAuth,
              onChange = { viewModel.toggleTwoFactorAuth() },
              activeColor = AccentGold,
              inactiveColor = Color(0xFFE5E7EB),
              activeBorderColor = AccentGoldDark,
              inactiveBorderColor = Color(0xFFD1D5DB)
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          SecurityActionRow(
            icon = Icons.Outlined.Lock,
            iconBackground = NavyPrimary,
            iconBorder = NavyDark,
            title = "Change Password",
            subtitle = "Update your password"
          ) { showChangePassword = true }

          SecurityActionRow(
            icon = Icons.Outlined.UserCheck,
            iconBackground = NavyPrimary,
            iconBorder = NavyDark,
            title = "Active Sessions",
            subtitle = "Manage logged in devices"
          ) { showActiveSessions = true }
        }

        Spacer(modifier = Modifier.height(16.dp))

        PrivacySection(title = "Data & Privacy") {
          SecurityActionRow(
            icon = Icons.Outlined.Download,
            iconBackground = NavyPrimary,
            iconBorder = NavyDark,
            title = "Download Your Data",
            subtitle = "Get a copy of your information"
          ) { showDownloadData = true }

          SecurityActionRow(
            icon = Icons.Outlined.FileCopy,
            iconBackground = Color(0xFF10B981),
            iconBorder = Color(0xFF059669),
            title = "Privacy Policy",
            subtitle = "Read our privacy policy"
          ) { showPrivacyPolicy = true }

          SecurityActionRow(
            icon = Icons.Outlined.FileCopy,
            iconBackground = Color(0xFF10B981),
            iconBorder = Color(0xFF059669),
            title = "Terms of Service",
            subtitle = "Read terms and conditions",
            showDivider = false
          ) { showTermsOfService = true }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFEE2E2), RoundedCornerShape(16.dp))
            .border(2.dp, Color(0xFFDC2626), RoundedCornerShape(16.dp))
            .padding(16.dp)
        ) {
          Text("Danger Zone", color = Color(0xFF991B1B), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
          Spacer(modifier = Modifier.height(12.dp))
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(Color.White, RoundedCornerShape(8.dp))
              .border(2.dp, Color(0xFFDC2626), RoundedCornerShape(8.dp))
              .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .background(Color(0xFFFEE2E2), CircleShape)
                .border(2.dp, Color(0xFFDC2626), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text("Delete Account", color = Color(0xFF991B1B), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
              Text("Permanently delete your account and data", color = Color(0xFFDC2626), fontSize = 14.sp)
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "We take your privacy seriously. Your data is encrypted and never shared without consent.",
          color = NavyPrimary.copy(alpha = 0.6f),
          fontSize = 12.sp,
          lineHeight = 18.sp,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))
      }
    }
  }
}

@Composable
private fun PrivacySection(title: String, content: @Composable () -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color.White, RoundedCornerShape(16.dp))
      .border(2.dp, NavyPrimary, RoundedCornerShape(16.dp))
      .padding(16.dp)
  ) {
    Text(text = title, color = NavyPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(16.dp))
    content()
  }
}

@Composable
private fun PrivacyToggleRow(
  icon: ImageVector,
  iconBackground: Color,
  iconBorder: Color,
  title: String,
  subtitle: String,
  enabled: Boolean,
  showDivider: Boolean = true,
  onToggle: () -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.Top
  ) {
    Row(
      modifier = Modifier.weight(1f),
      verticalAlignment = Alignment.Top
    ) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .background(iconBackground, CircleShape)
          .border(2.dp, iconBorder, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column {
        Text(text = title, color = NavyPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text(text = subtitle, color = NavyPrimary.copy(alpha = 0.8f), fontSize = 14.sp)
      }
    }
    Spacer(modifier = Modifier.width(12.dp))
    ToggleSwitch(
      enabled = enabled,
      onChange = onToggle,
      activeColor = AccentGold,
      inactiveColor = Color(0xFFE5E7EB),
      activeBorderColor = AccentGoldDark,
      inactiveBorderColor = Color(0xFFD1D5DB)
    )
  }
  if (showDivider) {
    Spacer(modifier = Modifier.height(14.dp))
  }
}

@Composable
private fun SecurityActionRow(
  icon: ImageVector,
  iconBackground: Color,
  iconBorder: Color,
  title: String,
  subtitle: String,
  showDivider: Boolean = true,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(BlueSecondary, RoundedCornerShape(8.dp))
      .border(2.dp, NavyPrimary, RoundedCornerShape(8.dp))
      .clickable { onClick() }
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(40.dp)
        .background(iconBackground, CircleShape)
        .border(2.dp, iconBorder, CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
    Spacer(modifier = Modifier.width(12.dp))
    Column {
      Text(text = title, color = NavyPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
      Text(text = subtitle, color = NavyPrimary.copy(alpha = 0.8f), fontSize = 14.sp)
    }
  }
  if (showDivider) {
    Spacer(modifier = Modifier.height(12.dp))
  }
}
