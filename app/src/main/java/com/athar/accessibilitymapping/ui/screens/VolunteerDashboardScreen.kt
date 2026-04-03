package com.athar.accessibilitymapping.ui.screens

import androidx.compose.runtime.Composable

@Composable
fun VolunteerDashboardScreen(
  userId: String,
  isVolunteerLive: Boolean,
  userName: String
) {
  AtharVolunteerDashboard(
    userId = userId,
    isVolunteerLive = isVolunteerLive,
    userName = userName
  )
}
