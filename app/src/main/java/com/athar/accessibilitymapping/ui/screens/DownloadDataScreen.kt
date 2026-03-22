package com.athar.accessibilitymapping.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.athar.accessibilitymapping.ui.components.PrimaryButton
import com.athar.accessibilitymapping.ui.components.ScreenHeader
import com.athar.accessibilitymapping.ui.theme.BluePrimary

@Composable
fun DownloadDataScreen(onBack: () -> Unit) {
  var selectedFormat by remember { mutableStateOf("json") }
  var profile by remember { mutableStateOf(true) }
  var assistanceHistory by remember { mutableStateOf(true) }
  var locationRatings by remember { mutableStateOf(true) }
  var accountSettings by remember { mutableStateOf(true) }
  var requestSent by remember { mutableStateOf(false) }

  if (requestSent) {
    DownloadDataSuccessScreen(onBack = onBack)
    return
  }

  Column(modifier = Modifier.fillMaxSize().background(BluePrimary)) {
    ScreenHeader(title = "Download Your Data", onBack = onBack, background = Color(0xFF1F3C5B))

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp)
    ) {
      InfoBanner("Request a copy of your personal data stored in Athar.")

      Spacer(modifier = Modifier.height(12.dp))

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color.White, RoundedCornerShape(16.dp))
          .padding(12.dp)
      ) {
        Text("Select Data to Download")
        DataToggleRow("Profile Information", profile) { profile = !profile }
        DataToggleRow("Assistance History", assistanceHistory) { assistanceHistory = !assistanceHistory }
        DataToggleRow("Location Ratings & Reviews", locationRatings) { locationRatings = !locationRatings }
        DataToggleRow("Account Settings", accountSettings) { accountSettings = !accountSettings }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color.White, RoundedCornerShape(16.dp))
          .padding(12.dp)
      ) {
        Text("Choose File Format")
        FormatOption("JSON Format", selectedFormat == "json") { selectedFormat = "json" }
        FormatOption("CSV Format", selectedFormat == "csv") { selectedFormat = "csv" }
      }

      Spacer(modifier = Modifier.height(12.dp))

      PrimaryButton(
        text = "Request Data Download",
        onClick = { requestSent = true },
        background = Color(0xFF1F3C5B)
      )
    }
  }
}

@Composable
private fun DownloadDataSuccessScreen(onBack: () -> Unit) {
  Column(modifier = Modifier.fillMaxSize().background(BluePrimary)) {
    ScreenHeader(title = "Download Your Data", onBack = onBack, background = Color(0xFF1F3C5B))

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Spacer(modifier = Modifier.height(40.dp))
      Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Color(0xFF65A30D), modifier = Modifier.size(72.dp))
      Spacer(modifier = Modifier.height(12.dp))
      Text("Request Received!", color = Color(0xFF0F172A))
      Text("We're preparing your data download.", color = Color(0xFF475569))
      Spacer(modifier = Modifier.height(20.dp))
      InfoBanner("You'll receive an email with a secure download link when your data is ready.")
      Spacer(modifier = Modifier.height(20.dp))
      PrimaryButton(text = "Done", onClick = onBack, background = Color(0xFF1F3C5B))
    }
  }
}

@Composable
private fun InfoBanner(text: String) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color(0xFFE0F2FE), RoundedCornerShape(12.dp))
      .padding(12.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(Icons.Outlined.Download, contentDescription = null, tint = Color(0xFF0284C7))
      Spacer(modifier = Modifier.width(8.dp))
      Text(text, color = Color(0xFF1E3A8A))
    }
  }
}

@Composable
private fun DataToggleRow(title: String, selected: Boolean, onToggle: () -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onToggle() }
      .padding(vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      if (selected) Icons.Outlined.CheckCircle else Icons.Outlined.Settings,
      contentDescription = null,
      tint = if (selected) Color(0xFF1F3C5B) else Color(0xFFCBD5E1)
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(title)
  }
}

@Composable
private fun FormatOption(title: String, selected: Boolean, onSelect: () -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onSelect() }
      .padding(vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      if (selected) Icons.Outlined.Star else Icons.Outlined.Settings,
      contentDescription = null,
      tint = if (selected) Color(0xFF1F3C5B) else Color(0xFFCBD5E1)
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(title)
  }
}


