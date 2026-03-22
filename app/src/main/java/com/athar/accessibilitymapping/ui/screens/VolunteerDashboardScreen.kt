package com.athar.accessibilitymapping.ui.screens

import androidx.compose.runtime.Composable

@Composable
fun VolunteerDashboardScreen(
  isVolunteerLive: Boolean,
  userName: String
) {
  AtharVolunteerDashboard(
    isVolunteerLive = isVolunteerLive,
    userName = userName
  )
}
