package com.athar.accessibilitymapping.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.athar.accessibilitymapping.data.UserRegistrationPayload
import com.athar.accessibilitymapping.ui.components.PrimaryButton
import com.athar.accessibilitymapping.ui.theme.BlueSecondary
import com.athar.accessibilitymapping.ui.theme.Gray200
import com.athar.accessibilitymapping.ui.theme.Gray600
import com.athar.accessibilitymapping.ui.theme.Gray700
import java.text.Normalizer
import kotlinx.coroutines.launch

@Composable
fun RegisterUserScreen(
  onBack: () -> Unit,
  onComplete: suspend (UserRegistrationPayload) -> String?
) {
  val headerBlue = Color(0xFF1F3C5B)
  val headerBlueMuted = Color(0xFF2C4F73)
  val coroutineScope = rememberCoroutineScope()
  var step by remember { mutableStateOf(1) }
  var fullName by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var phone by remember { mutableStateOf("") }
  var location by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var confirmPassword by remember { mutableStateOf("") }
  var disabilityType by remember { mutableStateOf("") }
  var emergencyContact by remember { mutableStateOf("") }
  var emergencyPhone by remember { mutableStateOf("") }
  var showPassword by remember { mutableStateOf(false) }
  var showConfirmPassword by remember { mutableStateOf(false) }
  var submissionError by remember { mutableStateOf<String?>(null) }
  var isSubmitting by remember { mutableStateOf(false) }

  val disabilityOptions = listOf(
    "Wheelchair user",
    "Visually impaired",
    "Hearing impaired",
    "Mobility challenges",
    "Cognitive disability",
    "Multiple disabilities",
    "Other"
  )

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.White)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(headerBlue)
        .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onBack) {
          Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = Color.White)
          Spacer(modifier = Modifier.width(6.dp))
          Text("Back", color = Color.White)
        }
      }
      Text("Create User Account", color = Color.White, style = MaterialTheme.typography.headlineSmall)
      Text("For people who need assistance", color = BlueSecondary)

      Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (1..4).forEach { index ->
          val barColor = if (index <= step) Color.White else headerBlueMuted
          Spacer(
            modifier = Modifier
              .height(6.dp)
              .weight(1f)
              .background(barColor)
          )
        }
      }
    }

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(16.dp)
    ) {
      if (step == 1) {
        Text("Personal Information", style = MaterialTheme.typography.titleLarge)
        Text("Tell us about yourself", color = Gray600)

        Spacer(modifier = Modifier.height(12.dp))

        LabeledTextField("Full Name *", fullName, { fullName = it }, Icons.Outlined.Person, "Enter your full name")
        LabeledTextField("Email Address *", email, { email = it }, Icons.Outlined.Email, "your.email@example.com")
        LabeledTextField("Phone Number *", phone, { phone = it }, Icons.Outlined.Phone, "+966 50 123 4567")
        LabeledTextField("City/Location *", location, { location = it }, Icons.Outlined.LocationOn, "Riyadh, Saudi Arabia")

        Spacer(modifier = Modifier.height(12.dp))

        PrimaryButton(
          text = "Next Step",
          onClick = { step = 2 },
          background = headerBlue
        )
      }

      if (step == 2) {
        Text("Create Password", style = MaterialTheme.typography.titleLarge)
        Text("Secure your account", color = Gray600)

        Spacer(modifier = Modifier.height(12.dp))

        Text("Password *", color = Gray700)
        OutlinedTextField(
          value = password,
          onValueChange = {
            password = it
            submissionError = null
          },
          leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
          trailingIcon = {
            IconButton(onClick = { showPassword = !showPassword }) {
              Icon(if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, contentDescription = null)
            }
          },
          visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
          placeholder = { Text("At least 8 characters") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Next,
            autoCorrectEnabled = false
          ),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = headerBlue,
            unfocusedBorderColor = Gray200
          )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text("Confirm Password *", color = Gray700)
        OutlinedTextField(
          value = confirmPassword,
          onValueChange = {
            confirmPassword = it
            submissionError = null
          },
          leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
          trailingIcon = {
            IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
              Icon(if (showConfirmPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, contentDescription = null)
            }
          },
          visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
          placeholder = { Text("Re-enter your password") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            autoCorrectEnabled = false
          ),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = headerBlue,
            unfocusedBorderColor = Gray200
          )
        )

        Spacer(modifier = Modifier.height(16.dp))
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFEFF6FF), RoundedCornerShape(12.dp))
            .padding(12.dp)
        ) {
          Text("Password must contain:", color = Gray700, style = MaterialTheme.typography.bodySmall)
          Spacer(modifier = Modifier.height(6.dp))
          Text("- At least 8 characters", color = Gray600, style = MaterialTheme.typography.bodySmall)
          Text("- A mix of letters and numbers", color = Gray600, style = MaterialTheme.typography.bodySmall)
          Text("- At least one special character", color = Gray600, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          PrimaryButton(
            text = "Back",
            onClick = { step = 1 },
            background = Gray200,
            contentColor = Gray700,
            modifier = Modifier.weight(1f)
          )
          PrimaryButton(
            text = "Next Step",
            onClick = { step = 3 },
            background = headerBlue,
            modifier = Modifier.weight(1f)
          )
        }
      }

      if (step == 3) {
        Text("Accessibility Needs", style = MaterialTheme.typography.titleLarge)
        Text("Help us serve you better", color = Gray600)

        Spacer(modifier = Modifier.height(12.dp))

        Text("What best describes your accessibility needs? *", color = Gray700)
        disabilityOptions.forEach { option ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 6.dp)
              .background(
                if (disabilityType == option) BlueSecondary else Color.White,
                shape = RoundedCornerShape(12.dp)
              )
              .padding(12.dp)
              .clickable { disabilityType = option },
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              Icons.Outlined.CheckCircle,
              contentDescription = null,
              tint = if (disabilityType == option) headerBlue else Gray200
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(option)
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          PrimaryButton(
            text = "Back",
            onClick = { step = 2 },
            background = Gray200,
            contentColor = Gray700,
            modifier = Modifier.weight(1f)
          )
          PrimaryButton(
            text = "Next Step",
            onClick = { step = 4 },
            background = headerBlue,
            modifier = Modifier.weight(1f)
          )
        }
      }

      if (step == 4) {
        Text("Emergency Contact", style = MaterialTheme.typography.titleLarge)
        Text("Someone we can reach in case of emergency", color = Gray600)

        Spacer(modifier = Modifier.height(12.dp))

        LabeledTextField("Emergency Contact Name *", emergencyContact, { emergencyContact = it }, Icons.Outlined.Person, "Full name")
        LabeledTextField("Emergency Contact Phone *", emergencyPhone, { emergencyPhone = it }, Icons.Outlined.Phone, "+966 50 123 4567")

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          PrimaryButton(
            text = "Back",
            onClick = { step = 3 },
            background = Gray200,
            contentColor = Gray700,
            modifier = Modifier.weight(1f)
          )
          PrimaryButton(
            text = if (isSubmitting) "Creating..." else "Create Account",
            onClick = {
              val normalizedPassword = normalizePasswordInput(password)
              val normalizedConfirmPassword = normalizePasswordInput(confirmPassword)
              submissionError = validateUserRegistration(
                fullName = fullName,
                email = email,
                phone = phone,
                location = location,
                password = normalizedPassword,
                confirmPassword = normalizedConfirmPassword,
                disabilityType = disabilityType,
                emergencyContact = emergencyContact,
                emergencyPhone = emergencyPhone
              )
              if (submissionError != null) return@PrimaryButton

              isSubmitting = true
              coroutineScope.launch {
                submissionError = onComplete(
                  UserRegistrationPayload(
                    fullName = fullName,
                    email = email,
                    phone = phone,
                    location = location,
                    password = normalizedPassword,
                    disabilityType = disabilityType,
                    emergencyContactName = emergencyContact,
                    emergencyContactPhone = emergencyPhone
                  )
                )
                isSubmitting = false
              }
            },
            enabled = !isSubmitting,
            background = headerBlue,
            modifier = Modifier.weight(1f)
          )
        }

        if (submissionError != null) {
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = submissionError ?: "",
            color = Color(0xFFB91C1C)
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}

private fun validateUserRegistration(
  fullName: String,
  email: String,
  phone: String,
  location: String,
  password: String,
  confirmPassword: String,
  disabilityType: String,
  emergencyContact: String,
  emergencyPhone: String
): String? {
  if (fullName.isBlank()) return "Full name is required."
  if (email.isBlank()) return "Email is required."
  if (phone.isBlank()) return "Phone number is required."
  if (location.isBlank()) return "Location is required."
  if (password.length < 8) return "Password must be at least 8 characters."
  if (password != confirmPassword) return "Passwords do not match."
  if (disabilityType.isBlank()) return "Please select or enter your accessibility needs."
  if (emergencyContact.isBlank()) return "Emergency contact name is required."
  if (emergencyPhone.isBlank()) return "Emergency contact phone is required."
  return null
}

private fun normalizePasswordInput(value: String): String {
  val nfkc = Normalizer.normalize(value, Normalizer.Form.NFKC)
  return nfkc
    .replace(Regex("[\\p{Cf}\\r\\n\\t]"), "")
    .trim()
}

@Composable
private fun LabeledTextField(
  label: String,
  value: String,
  onValueChange: (String) -> Unit,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  placeholder: String
) {
  Text(label, color = Gray700)
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    leadingIcon = { Icon(icon, contentDescription = null) },
    placeholder = { Text(placeholder) },
    modifier = Modifier.fillMaxWidth(),
    colors = OutlinedTextFieldDefaults.colors(
      focusedBorderColor = Color(0xFF2563EB),
      unfocusedBorderColor = Gray200
    )
  )
  Spacer(modifier = Modifier.height(12.dp))
}





