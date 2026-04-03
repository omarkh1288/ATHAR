package com.athar.accessibilitymapping.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bell
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.athar.accessibilitymapping.ui.theme.NavyDark
import com.athar.accessibilitymapping.ui.theme.NavyPrimary
import com.athar.accessibilitymapping.ui.theme.sdp
import com.athar.accessibilitymapping.ui.theme.ssp

@Composable
fun NotificationsScreen(onBack: () -> Unit) {
  val viewModel: NotificationsViewModel = viewModel()
  viewModel.loadIfNeeded()

  val pushEnabled by viewModel.pushEnabled.collectAsState()
  val emailEnabled by viewModel.emailEnabled.collectAsState()
  val smsEnabled by viewModel.smsEnabled.collectAsState()
  val volunteerRequests by viewModel.volunteerRequests.collectAsState()
  val volunteerAccepted by viewModel.volunteerAccepted.collectAsState()
  val locationUpdates by viewModel.locationUpdates.collectAsState()
  val newRatings by viewModel.newRatings.collectAsState()
  val communityUpdates by viewModel.communityUpdates.collectAsState()
  val marketingEmails by viewModel.marketingEmails.collectAsState()
  val soundEnabled by viewModel.soundEnabled.collectAsState()
  val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
  val isLoading by viewModel.isLoading.collectAsState()
  val errorMessage by viewModel.errorMessage.collectAsState()

  AtharPullToRefresh(
    isRefreshing = isLoading,
    onRefresh = viewModel::refresh
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.White)
    ) {
      ScreenHeader(title = "Notifications", onBack = onBack, background = NavyPrimary)

      Column(
        modifier = Modifier
          .fillMaxSize()
          .background(BluePrimary)
          .verticalScroll(rememberScrollState())
          .padding(16.dp)
      ) {
        if (isLoading) {
          Text("Loading notification settings...", color = NavyPrimary, fontSize = 14.sp)
          Spacer(modifier = Modifier.height(12.dp))
        }
        if (errorMessage != null) {
          Text(errorMessage ?: "", color = Color(0xFFB91C1C), fontSize = 14.sp)
          Spacer(modifier = Modifier.height(12.dp))
        }

        NotificationSection(
          title = "Notification Channels",
          subtitle = "Choose how you want to receive notifications"
        ) {
          ChannelRow(
            icon = Icons.Outlined.Bell,
            iconBackground = NavyPrimary,
            iconBorder = NavyDark,
            title = "Push Notifications",
            subtitle = "On this device",
            enabled = pushEnabled,
            onToggle = { viewModel.togglePush() }
          )
          ChannelRow(
            icon = Icons.AutoMirrored.Outlined.Chat,
            iconBackground = Color(0xFF10B981),
            iconBorder = Color(0xFF059669),
            title = "Email Notifications",
            subtitle = "Updates via email",
            enabled = emailEnabled,
            onToggle = { viewModel.toggleEmail() }
          )
          ChannelRow(
            icon = Icons.AutoMirrored.Outlined.Chat,
            iconBackground = AccentGold,
            iconBorder = AccentGoldDark,
            title = "SMS Notifications",
            subtitle = "Text messages",
            enabled = smsEnabled,
            onToggle = { viewModel.toggleSms() },
            showDivider = false
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        NotificationSection(title = "What to Notify") {
          ToggleOnlyRow("Volunteer Requests", "New assistance requests nearby", volunteerRequests) {
            viewModel.toggleVolunteerRequests()
          }
          ToggleOnlyRow("Request Accepted", "Volunteer accepted your request", volunteerAccepted) {
            viewModel.toggleVolunteerAccepted()
          }
          ToggleOnlyRow("Location Updates", "New accessible places added", locationUpdates) {
            viewModel.toggleLocationUpdates()
          }
          ToggleOnlyRow("Ratings & Reviews", "Someone rated your contribution", newRatings) {
            viewModel.toggleNewRatings()
          }
          ToggleOnlyRow("Community Updates", "News and feature updates", communityUpdates) {
            viewModel.toggleCommunityUpdates()
          }
          ToggleOnlyRow("Marketing Emails", "Tips and promotional content", marketingEmails, showDivider = false) {
            viewModel.toggleMarketingEmails()
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        NotificationSection(title = "Sound & Vibration") {
          ChannelRow(
            icon = Icons.AutoMirrored.Outlined.VolumeUp,
            iconBackground = AccentGold,
            iconBorder = AccentGoldDark,
            title = "Notification Sound",
            subtitle = "Play sound on notifications",
            enabled = soundEnabled,
            onToggle = { viewModel.toggleSound() }
          )
          ChannelRow(
            icon = Icons.Outlined.Bell,
            iconBackground = NavyPrimary,
            iconBorder = NavyDark,
            title = "Vibration",
            subtitle = "Vibrate on notifications",
            enabled = vibrationEnabled,
            onToggle = { viewModel.toggleVibration() },
            showDivider = false
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        NotificationSection(title = "Quiet Hours") {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(Color(0xFFF7FAFF), RoundedCornerShape(12.dp))
              .border(1.dp, Color(0xFFD6E0EC), RoundedCornerShape(12.dp))
              .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "Set Quiet Hours",
                color = NavyPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
              )
              Text(
                text = "Mute notifications during specific times",
                color = NavyPrimary.copy(alpha = 0.8f),
                fontSize = 14.sp
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }
}

@Composable
private fun NotificationSection(
  title: String,
  subtitle: String? = null,
  content: @Composable () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(6.dp, RoundedCornerShape(16.dp), clip = false)
      .background(Color.White, RoundedCornerShape(16.dp))
      .border(1.dp, Color(0xFFD6E0EC), RoundedCornerShape(16.dp))
      .padding(16.dp)
  ) {
    Text(
      text = title,
      color = NavyPrimary,
      fontSize = 19.sp,
      fontWeight = FontWeight.SemiBold
    )
    if (subtitle != null) {
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = subtitle,
        color = NavyPrimary.copy(alpha = 0.8f),
        fontSize = 14.sp,
        lineHeight = 20.sp
      )
    }
    Spacer(modifier = Modifier.height(16.dp))
    content()
  }
}

@Composable
private fun ChannelRow(
  icon: ImageVector,
  iconBackground: Color,
  iconBorder: Color,
  title: String,
  subtitle: String,
  enabled: Boolean,
  onToggle: () -> Unit,
  showDivider: Boolean = true
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color(0xFFF7FAFF), RoundedCornerShape(12.dp))
      .border(1.dp, Color(0xFFD6E0EC), RoundedCornerShape(12.dp))
      .padding(horizontal = 12.dp, vertical = 10.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(38.dp)
            .background(iconBackground, CircleShape)
            .border(1.dp, iconBorder, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(text = title, color = NavyPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
          Text(text = subtitle, color = NavyPrimary.copy(alpha = 0.8f), fontSize = 13.sp)
        }
      }
      Spacer(modifier = Modifier.width(12.dp))
      ToggleSwitch(
        enabled = enabled,
        onChange = onToggle,
        activeColor = AccentGold,
        inactiveColor = Color(0xFFE2E8F0),
        activeBorderColor = AccentGoldDark,
        inactiveBorderColor = Color(0xFFC6D2E2)
      )
    }
  }
  if (showDivider) {
    Spacer(modifier = Modifier.height(10.dp))
  }
}

@Composable
private fun ToggleOnlyRow(
  title: String,
  subtitle: String,
  enabled: Boolean,
  showDivider: Boolean = true,
  onToggle: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color(0xFFF7FAFF), RoundedCornerShape(12.dp))
      .border(1.dp, Color(0xFFD6E0EC), RoundedCornerShape(12.dp))
      .padding(horizontal = 12.dp, vertical = 10.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(text = title, color = NavyPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text(text = subtitle, color = NavyPrimary.copy(alpha = 0.8f), fontSize = 13.sp)
      }
      Spacer(modifier = Modifier.width(12.dp))
      ToggleSwitch(
        enabled = enabled,
        onChange = onToggle,
        activeColor = AccentGold,
        inactiveColor = Color(0xFFE2E8F0),
        activeBorderColor = AccentGoldDark,
        inactiveBorderColor = Color(0xFFC6D2E2)
      )
    }
  }
  if (showDivider) {
    Spacer(modifier = Modifier.height(10.dp))
  }
}

