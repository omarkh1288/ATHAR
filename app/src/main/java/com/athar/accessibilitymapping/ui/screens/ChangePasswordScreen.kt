package com.athar.accessibilitymapping.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.athar.accessibilitymapping.data.ApiCallResult
import com.athar.accessibilitymapping.data.AtharRepository
import com.athar.accessibilitymapping.ui.components.PrimaryButton
import com.athar.accessibilitymapping.ui.components.ScreenHeader
import com.athar.accessibilitymapping.ui.theme.BluePrimary
import com.athar.accessibilitymapping.ui.theme.sdp
import com.athar.accessibilitymapping.ui.theme.ssp
import kotlinx.coroutines.launch

@Composable
fun ChangePasswordScreen(onBack: () -> Unit) {
  val context = LocalContext.current
  val repository = remember(context) { AtharRepository(context) }
  val coroutineScope = rememberCoroutineScope()

  var currentPassword by remember { mutableStateOf("") }
  var newPassword by remember { mutableStateOf("") }
  var confirmPassword by remember { mutableStateOf("") }
  var showCurrent by remember { mutableStateOf(false) }
  var showNew by remember { mutableStateOf(false) }
  var showConfirm by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var successMessage by remember { mutableStateOf<String?>(null) }
  var isSubmitting by remember { mutableStateOf(false) }

  val requirements = listOf(
    "At least 8 characters" to (newPassword.length >= 8),
    "One uppercase letter" to newPassword.any { it.isUpperCase() },
    "One lowercase letter" to newPassword.any { it.isLowerCase() },
    "One number" to newPassword.any { it.isDigit() },
    "One special character" to newPassword.any { !it.isLetterOrDigit() }
  )

  Column(modifier = Modifier.fillMaxSize().background(BluePrimary)) {
    ScreenHeader(title = "Change Password", onBack = onBack, background = Color(0xFF1F3C5B))

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp)
    ) {
      Text("Choose a strong password to keep your account secure.", color = Color(0xFF475569))
      Spacer(modifier = Modifier.height(12.dp))

      PasswordField("Current Password *", currentPassword, {
        currentPassword = it
        errorMessage = null
      }, showCurrent, { showCurrent = !showCurrent })
      PasswordField("New Password *", newPassword, {
        newPassword = it
        errorMessage = null
      }, showNew, { showNew = !showNew })
      PasswordField("Confirm New Password *", confirmPassword, {
        confirmPassword = it
        errorMessage = null
      }, showConfirm, { showConfirm = !showConfirm })

      Spacer(modifier = Modifier.height(12.dp))

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color.White, RoundedCornerShape(16.dp))
          .padding(12.dp)
      ) {
        Text("Password Requirements", color = Color(0xFF0F172A))
        Spacer(modifier = Modifier.height(8.dp))
        requirements.forEach { (label, met) ->
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Check, contentDescription = null, tint = if (met) Color(0xFF65A30D) else Color(0xFFCBD5E1))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, color = if (met) Color(0xFF0F172A) else Color(0xFF94A3B8))
          }
          Spacer(modifier = Modifier.height(4.dp))
        }
      }

      if (errorMessage != null) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(errorMessage ?: "", color = Color(0xFFB91C1C))
      }
      if (successMessage != null) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(successMessage ?: "", color = Color(0xFF166534))
      }

      Spacer(modifier = Modifier.height(12.dp))

      PrimaryButton(
        text = if (isSubmitting) "Updating..." else "Change Password",
        onClick = {
          errorMessage = null
          successMessage = null
          if (currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
            errorMessage = "All password fields are required."
            return@PrimaryButton
          }
          if (newPassword != confirmPassword) {
            errorMessage = "New password and confirmation do not match."
            return@PrimaryButton
          }
          if (!requirements.all { it.second }) {
            errorMessage = "New password does not meet all requirements."
            return@PrimaryButton
          }
          isSubmitting = true
          coroutineScope.launch {
            when (val result = repository.changePassword(currentPassword, newPassword)) {
              is ApiCallResult.Success -> {
                successMessage = result.data.message
                currentPassword = ""
                newPassword = ""
                confirmPassword = ""
              }
              is ApiCallResult.Failure -> {
                errorMessage = result.message
              }
            }
            isSubmitting = false
          }
        },
        enabled = !isSubmitting,
        background = Color(0xFF1F3C5B)
      )
    }
  }
}

@Composable
private fun PasswordField(
  label: String,
  value: String,
  onValueChange: (String) -> Unit,
  show: Boolean,
  onToggle: () -> Unit
) {
  Text(label, color = Color(0xFF0F172A))
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
    trailingIcon = {
      IconButton(onClick = onToggle) {
        Icon(if (show) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, contentDescription = null)
      }
    },
    visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
    modifier = Modifier.fillMaxWidth(),
    colors = OutlinedTextFieldDefaults.colors(
      focusedBorderColor = Color(0xFF1F3C5B)
    )
  )
  Spacer(modifier = Modifier.height(10.dp))
}


