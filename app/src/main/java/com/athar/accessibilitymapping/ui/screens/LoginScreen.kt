package com.athar.accessibilitymapping.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.athar.accessibilitymapping.R
import com.athar.accessibilitymapping.ui.components.PrimaryButton
import com.athar.accessibilitymapping.ui.theme.BluePrimary
import com.athar.accessibilitymapping.ui.theme.NavyDark
import com.athar.accessibilitymapping.ui.theme.NavyPrimary
import com.athar.accessibilitymapping.ui.theme.TextLight
import com.athar.accessibilitymapping.ui.theme.sdp
import com.athar.accessibilitymapping.ui.theme.ssp
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
  onLogin: suspend (String, String) -> String?,
  onRegister: () -> Unit
) {
  val coroutineScope = rememberCoroutineScope()
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var showPassword by remember { mutableStateOf(false) }
  var rememberMe by remember { mutableStateOf(false) }
  var authError by remember { mutableStateOf<String?>(null) }
  var isSubmitting by remember { mutableStateOf(false) }
  val compactTouchTarget = 18.dp

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.linearGradient(
          colors = listOf(NavyPrimary, NavyDark)
        )
      )
      .padding(16.sdp)
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
          modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(Color.White),
          contentAlignment = Alignment.Center
        ) {
          Image(
            painter = painterResource(id = R.drawable.athar_logo),
            contentDescription = "Athar logo",
            modifier = Modifier.size(72.dp)
          )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
          text = "Welcome to Athar",
          style = MaterialTheme.typography.headlineSmall,
          color = Color.White,
          textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Making accessibility easier for everyone",
          style = MaterialTheme.typography.bodySmall,
          color = BluePrimary,
          textAlign = TextAlign.Center
        )
      }

      Spacer(modifier = Modifier.height(28.dp))

      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
      ) {
        Column(modifier = Modifier.padding(horizontal = 28.dp, vertical = 26.dp)) {
          Text(
            text = "Sign In",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Enter your credentials to continue",
            style = MaterialTheme.typography.bodySmall,
            color = TextLight,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(20.dp))

          Text(text = "Email Address", style = MaterialTheme.typography.labelSmall, color = TextLight)
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = email,
            onValueChange = {
              email = it
              authError = null
            },
            leadingIcon = {
              Icon(
                Icons.Outlined.Email,
                contentDescription = null,
                tint = TextLight,
                modifier = Modifier.size(18.dp)
              )
            },
            placeholder = {
              Text(
                "your.email@example.com",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9CA9BC)
              )
            },
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall.copy(color = NavyPrimary),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = NavyPrimary,
              unfocusedBorderColor = Color(0xFFE5E7EB),
              focusedLeadingIconColor = TextLight,
              unfocusedLeadingIconColor = TextLight,
              cursorColor = NavyPrimary
            )
          )

          Spacer(modifier = Modifier.height(18.dp))

          Text(text = "Password", style = MaterialTheme.typography.labelSmall, color = TextLight)
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = password,
            onValueChange = {
              password = it
              authError = null
            },
            leadingIcon = {
              Icon(
                Icons.Outlined.Lock,
                contentDescription = null,
                tint = TextLight,
                modifier = Modifier.size(18.dp)
              )
            },
            trailingIcon = {
              Icon(
                imageVector = if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                contentDescription = if (showPassword) "Hide password" else "Show password",
                tint = TextLight,
                modifier = Modifier
                  .size(18.dp)
                  .clickable { showPassword = !showPassword }
              )
            },
            placeholder = {
              Text(
                "Enter your password",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9CA9BC)
              )
            },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall.copy(color = NavyPrimary),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = NavyPrimary,
              unfocusedBorderColor = Color(0xFFE5E7EB),
              focusedLeadingIconColor = TextLight,
              unfocusedLeadingIconColor = TextLight,
              focusedTrailingIconColor = TextLight,
              unfocusedTrailingIconColor = TextLight,
              cursorColor = NavyPrimary
            )
          )

          Spacer(modifier = Modifier.height(16.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              modifier = Modifier.weight(1f),
              verticalAlignment = Alignment.CenterVertically
            ) {
              androidx.compose.runtime.CompositionLocalProvider(
                LocalMinimumInteractiveComponentSize provides compactTouchTarget
              ) {
                Checkbox(
                  checked = rememberMe,
                  onCheckedChange = { rememberMe = it },
                  modifier = Modifier.size(18.dp),
                  colors = CheckboxDefaults.colors(
                    checkedColor = NavyPrimary,
                    uncheckedColor = Color(0xFFD1D5DB),
                    checkmarkColor = Color.White
                  )
                )
              }
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Remember me",
                style = MaterialTheme.typography.bodySmall,
                color = TextLight
              )
            }
            TextButton(
              onClick = { authError = "Password reset is not enabled yet. Please contact support." },
              enabled = !isSubmitting,
              contentPadding = PaddingValues(0.dp),
              modifier = Modifier
                .heightIn(min = 0.dp)
                .wrapContentWidth(Alignment.End)
            ) {
              Text(
                text = "Forgot password?",
                color = TextLight,
                style = MaterialTheme.typography.bodySmall
              )
            }
          }

          Spacer(modifier = Modifier.height(18.dp))

          if (authError != null) {
            Text(authError ?: "", color = Color(0xFFB91C1C))
            Spacer(modifier = Modifier.height(8.dp))
          }

          PrimaryButton(
            text = if (isSubmitting) "Signing In..." else "Sign In",
            onClick = {
              val normalizedEmail = email.trim()
              if (normalizedEmail.isBlank() || password.isBlank()) {
                authError = "Please enter your email and password."
                return@PrimaryButton
              }
              isSubmitting = true
              coroutineScope.launch {
                authError = onLogin(normalizedEmail, password)
                isSubmitting = false
              }
            },
            enabled = !isSubmitting,
            background = NavyPrimary,
            leadingIcon = if (isSubmitting) null else Icons.AutoMirrored.Outlined.Login
          )

          Spacer(modifier = Modifier.height(20.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Don't have an account? ",
              color = Color(0xFF6B7280),
              style = MaterialTheme.typography.bodySmall
            )
            TextButton(
              onClick = onRegister,
              enabled = !isSubmitting,
              contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
              modifier = Modifier.heightIn(min = 0.dp)
            ) {
              Text(
                text = "Register here",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodySmall
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "By signing in, you agree to our\nTerms of Service and Privacy Policy",
        color = BluePrimary,
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 28.dp)
      )
    }
  }
}
