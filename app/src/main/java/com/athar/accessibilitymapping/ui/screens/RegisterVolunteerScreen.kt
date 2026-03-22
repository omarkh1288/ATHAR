package com.athar.accessibilitymapping.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.UploadFile
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.athar.accessibilitymapping.data.VolunteerRegistrationPayload
import com.athar.accessibilitymapping.ui.components.PrimaryButton
import java.text.Normalizer
import kotlinx.coroutines.launch

@Composable
fun RegisterVolunteerScreen(
  onBack: () -> Unit,
  onComplete: suspend (VolunteerRegistrationPayload) -> String?
) {
  val volunteerGreen = Color(0xFF1F3C5B)
  val volunteerGreenMuted = Color(0xFF2C4F73)
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  var step by remember { mutableStateOf(1) }
  var fullName by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var phone by remember { mutableStateOf("") }
  var location by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var confirmPassword by remember { mutableStateOf("") }
  var idNumber by remember { mutableStateOf("") }
  var dateOfBirth by remember { mutableStateOf("") }
  var motivation by remember { mutableStateOf("") }
  var showPassword by remember { mutableStateOf(false) }
  var showConfirmPassword by remember { mutableStateOf(false) }
  var idDocumentUri by remember { mutableStateOf<Uri?>(null) }
  var idDocumentName by remember { mutableStateOf<String?>(null) }
  var submissionError by remember { mutableStateOf<String?>(null) }
  var isSubmitting by remember { mutableStateOf(false) }

  val languageOptions = listOf("Arabic", "English", "French", "Urdu", "Hindi", "Other")
  val availabilityOptions = listOf(
    "Weekday mornings",
    "Weekday afternoons",
    "Weekday evenings",
    "Weekend mornings",
    "Weekend afternoons",
    "Weekend evenings",
    "Flexible/On-demand"
  )
  var selectedLanguages by remember { mutableStateOf(setOf<String>()) }
  var selectedAvailability by remember { mutableStateOf(setOf<String>()) }
  val idDocumentPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri != null) {
      runCatching {
        context.contentResolver.takePersistableUriPermission(
          uri,
          Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
      }
      idDocumentUri = uri
      idDocumentName = resolveDocumentName(context, uri)
      submissionError = null
    }
  }

  val colorScheme = MaterialTheme.colorScheme

  Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(volunteerGreen)
        .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onBack) {
          Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = Color.White)
          Spacer(modifier = Modifier.width(6.dp))
          Text("Back", color = Color.White)
        }
      }
      Text("Become a Volunteer", color = Color.White, style = MaterialTheme.typography.headlineSmall)
      Text("Help others in your community", color = Color(0xFFD6E4F5))

      Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (1..5).forEach { index ->
          val barColor = if (index <= step) Color.White else volunteerGreenMuted
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
        Text("Tell us about yourself", color = colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(12.dp))

        VolunteerTextField("Full Name *", fullName, { fullName = it }, Icons.Outlined.Person, "Enter your full name")
        VolunteerTextField("Email Address *", email, { email = it }, Icons.Outlined.Email, "your.email@example.com")
        VolunteerTextField("Phone Number *", phone, { phone = it }, Icons.Outlined.Phone, "+966 50 123 4567")
        VolunteerTextField("City/Location *", location, { location = it }, Icons.Outlined.LocationOn, "Riyadh, Saudi Arabia")

        Spacer(modifier = Modifier.height(12.dp))

        PrimaryButton(
          text = "Next Step",
          onClick = { step = 2 },
          background = volunteerGreen
        )
      }

      if (step == 2) {
        Text("Create Password", style = MaterialTheme.typography.titleLarge)
        Text("Secure your account", color = colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(12.dp))

        Text("Password *", color = colorScheme.onSurface)
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
            focusedBorderColor = volunteerGreen,
            unfocusedBorderColor = colorScheme.outline,
            focusedLeadingIconColor = volunteerGreen,
            focusedTrailingIconColor = volunteerGreen,
            unfocusedLeadingIconColor = colorScheme.onSurfaceVariant,
            unfocusedTrailingIconColor = colorScheme.onSurfaceVariant,
            cursorColor = volunteerGreen
          )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text("Confirm Password *", color = colorScheme.onSurface)
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
            focusedBorderColor = volunteerGreen,
            unfocusedBorderColor = colorScheme.outline,
            focusedLeadingIconColor = volunteerGreen,
            focusedTrailingIconColor = volunteerGreen,
            unfocusedLeadingIconColor = colorScheme.onSurfaceVariant,
            unfocusedTrailingIconColor = colorScheme.onSurfaceVariant,
            cursorColor = volunteerGreen
          )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          PrimaryButton(
            text = "Back",
            onClick = { step = 1 },
            background = colorScheme.surfaceVariant,
            contentColor = colorScheme.onSurface,
            modifier = Modifier.weight(1f)
          )
          PrimaryButton(
            text = "Next Step",
            onClick = { step = 3 },
            background = volunteerGreen,
            modifier = Modifier.weight(1f)
          )
        }
      }

      if (step == 3) {
        Text("Identity Verification", style = MaterialTheme.typography.titleLarge)
        Text("Required for volunteer approval", color = colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(12.dp))

        VolunteerTextField("National ID / Iqama Number *", idNumber, { idNumber = it }, Icons.Outlined.Person, "Enter your ID number")
        VolunteerTextField("Date of Birth *", dateOfBirth, { dateOfBirth = it }, Icons.Outlined.Event, "YYYY-MM-DD")

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(colorScheme.surfaceVariant, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .clickable { idDocumentPicker.launch(arrayOf("*/*")) }
            .padding(16.dp)
        ) {
          Icon(Icons.Outlined.UploadFile, contentDescription = null, tint = colorScheme.onSurfaceVariant)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = idDocumentName ?: "Tap to upload ID document",
            color = colorScheme.onSurfaceVariant
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          PrimaryButton(
            text = "Back",
            onClick = { step = 2 },
            background = colorScheme.surfaceVariant,
            contentColor = colorScheme.onSurface,
            modifier = Modifier.weight(1f)
          )
          PrimaryButton(
            text = "Next Step",
            onClick = { step = 4 },
            background = volunteerGreen,
            modifier = Modifier.weight(1f)
          )
        }
      }

      if (step == 4) {
        Text("Skills & Availability", style = MaterialTheme.typography.titleLarge)
        Text("Help us match you with requests", color = colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(12.dp))

        Text("Languages You Speak *", color = colorScheme.onSurface)
        languageOptions.forEach { lang ->
          val selected = selectedLanguages.contains(lang)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 6.dp)
              .background(
                if (selected) Color(0xFFD6E4F5) else colorScheme.surfaceVariant,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
              )
              .padding(12.dp)
              .clickable {
                selectedLanguages = if (selected) selectedLanguages - lang else selectedLanguages + lang
              },
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              Icons.Outlined.CheckCircle,
              contentDescription = null,
              tint = if (selected) volunteerGreen else colorScheme.outlineVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(lang)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("Availability *", color = colorScheme.onSurface)
        availabilityOptions.forEach { option ->
          val selected = selectedAvailability.contains(option)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 6.dp)
              .background(
                if (selected) Color(0xFFD6E4F5) else colorScheme.surfaceVariant,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
              )
              .padding(12.dp)
              .clickable {
                selectedAvailability = if (selected) selectedAvailability - option else selectedAvailability + option
              },
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              Icons.Outlined.CheckCircle,
              contentDescription = null,
              tint = if (selected) volunteerGreen else colorScheme.outlineVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(option)
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          PrimaryButton(
            text = "Back",
            onClick = { step = 3 },
            background = colorScheme.surfaceVariant,
            contentColor = colorScheme.onSurface,
            modifier = Modifier.weight(1f)
          )
          PrimaryButton(
            text = "Next Step",
            onClick = { step = 5 },
            background = volunteerGreen,
            modifier = Modifier.weight(1f)
          )
        }
      }

      if (step == 5) {
        Text("Almost There!", style = MaterialTheme.typography.titleLarge)
        Text("One last question", color = colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(12.dp))

        Text("Why do you want to volunteer with Athar? *", color = colorScheme.onSurface)
        OutlinedTextField(
          value = motivation,
          onValueChange = { motivation = it },
          placeholder = { Text("Share your motivation for helping others...") },
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = volunteerGreen,
            unfocusedBorderColor = colorScheme.outline,
            cursorColor = volunteerGreen
          )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          PrimaryButton(
            text = "Back",
            onClick = { step = 4 },
            background = colorScheme.surfaceVariant,
            contentColor = colorScheme.onSurface,
            modifier = Modifier.weight(1f)
          )
          PrimaryButton(
            text = if (isSubmitting) "Submitting..." else "Submit Application",
            onClick = {
              val normalizedPassword = normalizePasswordInput(password)
              val normalizedConfirmPassword = normalizePasswordInput(confirmPassword)
              submissionError = validateVolunteerRegistration(
                fullName = fullName,
                email = email,
                phone = phone,
                location = location,
                password = normalizedPassword,
                confirmPassword = normalizedConfirmPassword,
                idNumber = idNumber,
                dateOfBirth = dateOfBirth,
                motivation = motivation,
                selectedLanguages = selectedLanguages,
                selectedAvailability = selectedAvailability
              )
              if (submissionError != null) return@PrimaryButton

              isSubmitting = true
              coroutineScope.launch {
                submissionError = onComplete(
                  VolunteerRegistrationPayload(
                    fullName = fullName,
                    email = email,
                    phone = phone,
                    location = location,
                    password = normalizedPassword,
                    idNumber = idNumber,
                    dateOfBirth = dateOfBirth,
                    motivation = motivation,
                    languages = selectedLanguages,
                    availability = selectedAvailability,
                    idDocumentUri = idDocumentUri?.toString()
                  )
                )
                isSubmitting = false
              }
            },
            enabled = !isSubmitting,
            background = volunteerGreen,
            modifier = Modifier.weight(1f)
          )
        }

        if (submissionError != null) {
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = submissionError ?: "",
            color = colorScheme.error
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}

private fun validateVolunteerRegistration(
  fullName: String,
  email: String,
  phone: String,
  location: String,
  password: String,
  confirmPassword: String,
  idNumber: String,
  dateOfBirth: String,
  motivation: String,
  selectedLanguages: Set<String>,
  selectedAvailability: Set<String>
): String? {
  if (fullName.isBlank()) return "Full name is required."
  if (email.isBlank()) return "Email is required."
  if (phone.isBlank()) return "Phone number is required."
  if (location.isBlank()) return "Location is required."
  if (password.length < 8) return "Password must be at least 8 characters."
  if (password != confirmPassword) return "Passwords do not match."
  if (idNumber.isBlank()) return "National ID / Iqama number is required."
  if (dateOfBirth.isBlank()) return "Date of birth is required."
  if (selectedLanguages.isEmpty()) return "Please select at least one language."
  if (selectedAvailability.isEmpty()) return "Please select at least one availability option."
  if (motivation.isBlank()) return "Please write your volunteering motivation."
  return null
}

private fun normalizePasswordInput(value: String): String {
  val nfkc = Normalizer.normalize(value, Normalizer.Form.NFKC)
  return nfkc
    .replace(Regex("[\\p{Cf}\\r\\n\\t]"), "")
    .trim()
}

private fun resolveDocumentName(context: Context, uri: Uri): String {
  val fallback = uri.lastPathSegment?.substringAfterLast('/') ?: "id_document"
  val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
  return runCatching {
    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
      val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
      if (index >= 0 && cursor.moveToFirst()) {
        cursor.getString(index)
      } else {
        null
      }
    }
  }.getOrNull()
    ?.takeIf { it.isNotBlank() }
    ?: fallback
}

@Composable
private fun VolunteerTextField(
  label: String,
  value: String,
  onValueChange: (String) -> Unit,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  placeholder: String
) {
  val colorScheme = MaterialTheme.colorScheme
  Text(label, color = colorScheme.onSurface)
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    leadingIcon = { Icon(icon, contentDescription = null) },
    placeholder = { Text(placeholder) },
    modifier = Modifier.fillMaxWidth(),
    colors = OutlinedTextFieldDefaults.colors(
      focusedBorderColor = colorScheme.primary,
      unfocusedBorderColor = colorScheme.outline,
      focusedLeadingIconColor = colorScheme.primary,
      unfocusedLeadingIconColor = colorScheme.onSurfaceVariant,
      cursorColor = colorScheme.primary
    )
  )
  Spacer(modifier = Modifier.height(12.dp))
}





