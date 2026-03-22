package com.athar.accessibilitymapping.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.athar.accessibilitymapping.data.ApiCallResult
import com.athar.accessibilitymapping.data.ApiSessionDto
import com.athar.accessibilitymapping.data.AtharRepository
import com.athar.accessibilitymapping.ui.components.ScreenHeader
import com.athar.accessibilitymapping.ui.theme.BluePrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun ActiveSessionsScreen(onBack: () -> Unit) {
  val context = LocalContext.current
  val repository = remember(context) { AtharRepository(context) }
  val coroutineScope = rememberCoroutineScope()

  var sessions by remember { mutableStateOf<List<ApiSessionDto>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var actionMessage by remember { mutableStateOf<String?>(null) }

  suspend fun refreshSessions() {
    when (val result = repository.getSessions()) {
      is ApiCallResult.Success -> {
        sessions = result.data
        errorMessage = null
      }
      is ApiCallResult.Failure -> {
        errorMessage = result.message
      }
    }
    isLoading = false
  }

  LaunchedEffect(Unit) {
    refreshSessions()
  }

  Column(modifier = Modifier.fillMaxSize().background(BluePrimary)) {
    ScreenHeader(title = "Active Sessions", onBack = onBack, background = Color(0xFF1F3C5B))

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFFEFF6FF), RoundedCornerShape(12.dp))
          .padding(12.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Outlined.Shield, contentDescription = null, tint = Color(0xFF1F3C5B))
          Spacer(modifier = Modifier.width(8.dp))
          Text("These are the devices currently logged into your account.")
        }
      }

      if (isLoading) {
        Spacer(modifier = Modifier.height(12.dp))
        Text("Loading sessions...", color = Color(0xFF334155))
      }
      if (errorMessage != null) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(errorMessage ?: "", color = Color(0xFFB91C1C))
      }
      if (actionMessage != null) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(actionMessage ?: "", color = Color(0xFF166534))
      }

      Spacer(modifier = Modifier.height(12.dp))

      sessions.forEach { session ->
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(12.dp)
        ) {
          if (session.isCurrent) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Outlined.Shield, contentDescription = null, tint = Color(0xFF1F3C5B))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Current Device", color = Color(0xFF1F3C5B))
            }
            Spacer(modifier = Modifier.height(6.dp))
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Devices, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(session.deviceName)
          }
          Spacer(modifier = Modifier.height(6.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Schedule, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Created: ${formatSessionTime(session.createdAtEpochSeconds)}")
          }
          Spacer(modifier = Modifier.height(6.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Schedule, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Last seen: ${formatSessionTime(session.lastSeenAtEpochSeconds)}")
          }

          if (!session.isCurrent) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
              onClick = {
                actionMessage = null
                errorMessage = null
                coroutineScope.launch {
                  when (val result = repository.revokeSession(session.id)) {
                    is ApiCallResult.Success -> {
                      actionMessage = result.data.message
                      refreshSessions()
                    }
                    is ApiCallResult.Failure -> {
                      errorMessage = result.message
                    }
                  }
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(Icons.Outlined.Logout, contentDescription = null)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Log Out")
            }
          }
        }
        Spacer(modifier = Modifier.height(12.dp))
      }

      if (sessions.any { !it.isCurrent }) {
        Button(
          onClick = {
            actionMessage = null
            errorMessage = null
            coroutineScope.launch {
              sessions
                .filterNot { it.isCurrent }
                .forEach { session ->
                  when (val result = repository.revokeSession(session.id)) {
                    is ApiCallResult.Success -> actionMessage = result.data.message
                    is ApiCallResult.Failure -> errorMessage = result.message
                  }
                }
              refreshSessions()
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Outlined.Logout, contentDescription = null)
          Spacer(modifier = Modifier.width(6.dp))
          Text("Log Out All Other Sessions")
        }
      }
    }
  }
}

private fun formatSessionTime(epochSeconds: Long): String {
  val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
  return formatter.format(Date(epochSeconds * 1000))
}


