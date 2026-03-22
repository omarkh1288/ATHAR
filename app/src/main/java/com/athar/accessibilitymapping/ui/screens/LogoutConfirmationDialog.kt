package com.athar.accessibilitymapping.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun LogoutConfirmationDialog(
  onConfirm: () -> Unit,
  onCancel: () -> Unit
) {
  Dialog(onDismissRequest = onCancel) {
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(20.dp),
      color = Color.White
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Box(
          modifier = Modifier
            .size(76.dp)
            .background(Color(0xFFFEF3C7), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Outlined.WarningAmber,
            contentDescription = null,
            tint = Color(0xFFD97706),
            modifier = Modifier.size(40.dp)
          )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
          text = "Log Out?",
          color = Color(0xFF001A45),
          fontSize = 24.sp,
          lineHeight = 30.sp,
          fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = "Are you sure you want to log out of your account?",
          color = Color(0xFF334155),
          fontSize = 16.sp,
          lineHeight = 24.sp,
          textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(22.dp))
        Button(
          onClick = onConfirm,
          modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFEF000F),
            contentColor = Color.White
          ),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
        ) {
          Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, modifier = Modifier.size(22.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Yes, Log Out",
            fontSize = 16.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold
          )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
          onClick = onCancel,
          modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFE2E8F0),
            contentColor = Color(0xFF0F172A)
          )
        ) {
          Text(
            text = "Cancel",
            fontSize = 16.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium
          )
        }
      }
    }
  }
}

