package com.athar.accessibilitymapping.ui.components
import com.athar.accessibilitymapping.ui.theme.ssp

import com.athar.accessibilitymapping.ui.theme.sdp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.draw.shadow

@Composable
fun ScreenHeader(
  title: String,
  onBack: () -> Unit,
  background: Color,
  subtitle: String? = null
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(elevation = 8.sdp)
      .background(background)
      .statusBarsPadding()
      .padding(start = 16.sdp, end = 16.sdp, top = 20.sdp, bottom = 22.sdp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      TextButton(
        onClick = onBack,
        colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.sdp)
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(24.sdp)
        )
        Spacer(modifier = Modifier.width(6.sdp))
        Text(
          text = "Back",
          color = Color.White,
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.Medium
        )
      }
    }
    Text(
      text = title,
      color = Color.White,
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Bold
    )
    if (subtitle != null) {
      Text(
        text = subtitle,
        color = Color.White.copy(alpha = 0.85f),
        style = MaterialTheme.typography.bodySmall
      )
    }
  }
}

