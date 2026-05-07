package com.athar.accessibilitymapping.ui.screens

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.athar.accessibilitymapping.data.ApiCallResult
import com.athar.accessibilitymapping.data.ApiUpdateProfileRequest
import com.athar.accessibilitymapping.data.AtharRepository
import com.athar.accessibilitymapping.data.UserRole
import com.athar.accessibilitymapping.ui.components.AtharPullToRefresh
import com.athar.accessibilitymapping.ui.components.ProfilePhoto
import com.athar.accessibilitymapping.ui.components.ScreenHeader
import com.athar.accessibilitymapping.ui.localization.AppLanguage
import com.athar.accessibilitymapping.ui.localization.localized
import com.athar.accessibilitymapping.ui.theme.AccentGold
import com.athar.accessibilitymapping.ui.theme.AccentGoldDark
import com.athar.accessibilitymapping.ui.theme.BluePrimary
import com.athar.accessibilitymapping.ui.theme.BlueSecondary
import com.athar.accessibilitymapping.ui.theme.Gray200
import com.athar.accessibilitymapping.ui.theme.NavyDark
import com.athar.accessibilitymapping.ui.theme.NavyPrimary
import com.athar.accessibilitymapping.ui.theme.sdp
import com.athar.accessibilitymapping.ui.theme.ssp
import java.io.File
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun AccountSettingsScreen(
  onBack: () -> Unit,
  userId: String,
  userRole: UserRole,
  userDisabilityType: String?,
  currentLanguage: AppLanguage,
  profilePhotoPath: String?,
  onLanguageChange: (AppLanguage) -> Unit,
  onProfilePhotoChanged: (String?) -> Unit,
  onAccountRefreshRequested: () -> Unit = {},
  onOpenChangePassword: () -> Unit = {}
) {
  val context = LocalContext.current
  val repository = remember(context) { AtharRepository(context) }
  val coroutineScope = rememberCoroutineScope()
  val viewModel: AccountSettingsViewModel = viewModel()

  val name by viewModel.name.collectAsState()
  val email by viewModel.email.collectAsState()
  val phone by viewModel.phone.collectAsState()
  val address by viewModel.address.collectAsState()
  val disabilityType by viewModel.disabilityType.collectAsState()
  val passwordChangedAt by viewModel.passwordChangedAt.collectAsState()
  val isLoading by viewModel.isLoading.collectAsState()
  val loadError by viewModel.loadError.collectAsState()
  val isSaving by viewModel.isSaving.collectAsState()
  val saveError by viewModel.saveError.collectAsState()
  val saveMessage by viewModel.saveMessage.collectAsState()
  var photoError by remember { mutableStateOf<String?>(null) }
  var photoMessage by remember { mutableStateOf<String?>(null) }
  var isUploadingPhoto by remember { mutableStateOf(false) }
  var disabilityExpanded by remember { mutableStateOf(false) }

  val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
    if (uri == null) return@rememberLauncherForActivityResult
    val previousPhotoPath = profilePhotoPath
    val savedPhotoPath = runCatching {
      copySelectedProfilePhoto(context, uri, userId)
    }.getOrNull()
    if (savedPhotoPath == null) {
      photoMessage = null
      photoError = "Couldn't update your profile photo. Please try again."
    } else {
      photoError = null
      photoMessage = null
      onProfilePhotoChanged(savedPhotoPath)
      isUploadingPhoto = true
      coroutineScope.launch {
        when (val result = repository.uploadProfilePhoto(savedPhotoPath)) {
          is ApiCallResult.Success -> {
            photoError = null
            photoMessage = "Photo uploaded successfully"
            onAccountRefreshRequested()
          }
          is ApiCallResult.Failure -> {
            onProfilePhotoChanged(previousPhotoPath)
            photoMessage = null
            photoError = formatProfilePhotoUploadError(result)
          }
        }
        isUploadingPhoto = false
      }
    }
  }

  val disabilityOptions = listOf(
    "Wheelchair user",
    "Visually impaired",
    "Hearing impaired",
    "Mobility challenges",
    "Cognitive disability",
    "Other",
    "Prefer not to say"
  )

  viewModel.loadIfNeeded(userDisabilityType, userRole)

  AtharPullToRefresh(
    isRefreshing = isLoading,
    onRefresh = { viewModel.refresh(userDisabilityType, userRole) }
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.White)
    ) {
      ScreenHeader(title = "Account Settings", onBack = onBack, background = NavyPrimary)

      Column(
        modifier = Modifier
          .fillMaxSize()
          .background(BluePrimary)
          .verticalScroll(rememberScrollState())
          .padding(16.dp)
      ) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Box {
          ProfilePhoto(
            photoPath = profilePhotoPath,
            modifier = Modifier.size(96.dp),
            backgroundColor = NavyPrimary,
            borderColor = NavyDark,
            placeholderTint = Color.White,
            placeholderIconSize = 48.dp
          )

          Box(
            modifier = Modifier
              .align(Alignment.BottomEnd)
              .offset(x = 2.dp, y = 2.dp)
              .size(32.dp)
              .background(AccentGold, CircleShape)
              .border(2.dp, AccentGoldDark, CircleShape)
              .clickable(enabled = !isUploadingPhoto) { photoPicker.launch("image/*") },
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Outlined.CameraAlt,
              contentDescription = localized("Change profile photo"),
              tint = Color.White,
              modifier = Modifier.size(16.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
          onClick = { photoPicker.launch("image/*") },
          enabled = !isUploadingPhoto,
          colors = ButtonDefaults.buttonColors(
            containerColor = AccentGold,
            contentColor = Color.White
          ),
          border = BorderStroke(2.dp, AccentGoldDark),
          shape = RoundedCornerShape(999.dp),
          modifier = Modifier.height(42.dp)
        ) {
          Text(
            text = if (isUploadingPhoto) "Uploading..." else "Change Photo",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
          )
        }

        if (photoError != null) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = photoError ?: "",
            color = Color(0xFFB91C1C),
            fontSize = 13.sp
          )
        }
        if (photoMessage != null) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = photoMessage ?: "",
            color = Color(0xFF166534),
            fontSize = 13.sp
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      if (isLoading) {
        Text("Loading account...", color = NavyPrimary, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(12.dp))
      }
      if (loadError != null) {
        Text(loadError ?: "", color = Color(0xFFB91C1C), fontSize = 14.sp)
        Spacer(modifier = Modifier.height(12.dp))
      }

      SettingsSection(title = "Personal Information") {
        ProfileField(
          label = "Full Name",
          value = name,
          onValueChange = { viewModel.updateName(it) },
          icon = Icons.Outlined.Person
        )
        ProfileField(
          label = "Email Address",
          value = email,
          onValueChange = {},
          icon = Icons.Outlined.Email,
          readOnly = true
        )
        ProfileField(
          label = "Phone Number",
          value = phone,
          onValueChange = { viewModel.updatePhone(it) },
          icon = Icons.Outlined.Phone
        )
        ProfileField(
          label = "Location",
          value = address,
          onValueChange = { viewModel.updateAddress(it) },
          icon = Icons.Outlined.LocationOn
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      SettingsSection(title = "Language") {
        Text(
          text = "Choose your app language",
          color = NavyPrimary.copy(alpha = 0.8f),
          fontSize = 14.sp,
          lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          LanguageOptionButton(
            modifier = Modifier.weight(1f),
            label = "English",
            selected = currentLanguage == AppLanguage.English,
            onClick = { onLanguageChange(AppLanguage.English) }
          )
          LanguageOptionButton(
            modifier = Modifier.weight(1f),
            label = "Arabic",
            selected = currentLanguage == AppLanguage.Arabic,
            onClick = { onLanguageChange(AppLanguage.Arabic) }
          )
        }
      }

      if (userRole == UserRole.User) {
        Spacer(modifier = Modifier.height(16.dp))
        SettingsSection(title = "Accessibility Information") {
          Text(
            text = "This helps us provide better assistance and relevant accessibility information",
            color = NavyPrimary.copy(alpha = 0.8f),
            fontSize = 14.sp,
            lineHeight = 20.sp
          )
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "Accessibility Needs",
            color = NavyPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
          )
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(Color.White, RoundedCornerShape(8.dp))
              .border(2.dp, NavyPrimary, RoundedCornerShape(8.dp))
              .clickable { disabilityExpanded = !disabilityExpanded }
              .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = if (disabilityType.isBlank()) "Select your accessibility needs..." else disabilityType,
              color = if (disabilityType.isBlank()) NavyPrimary.copy(alpha = 0.6f) else NavyPrimary,
              fontSize = 16.sp
            )
            Icon(
              imageVector = if (disabilityExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
              contentDescription = null,
              tint = NavyPrimary
            )
          }
          AnimatedVisibility(visible = disabilityExpanded) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .background(Color.White, RoundedCornerShape(8.dp))
                .border(2.dp, NavyPrimary, RoundedCornerShape(8.dp))
            ) {
              disabilityOptions.forEachIndexed { index, option ->
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                      viewModel.updateDisabilityType(option)
                      disabilityExpanded = false
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                  Text(
                    text = option,
                    color = NavyPrimary,
                    fontSize = 16.sp
                  )
                }
                if (index != disabilityOptions.lastIndex) {
                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(1.dp)
                      .background(Gray200)
                  )
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      SettingsSection(title = "Security") {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(BlueSecondary, RoundedCornerShape(8.dp))
            .border(2.dp, NavyPrimary, RoundedCornerShape(8.dp))
            .clickable { onOpenChangePassword() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Change Password",
              color = NavyPrimary,
              fontSize = 16.sp,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              text = formatPasswordChangedLabel(passwordChangedAt),
              color = NavyPrimary.copy(alpha = 0.8f),
              fontSize = 14.sp
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Button(
        onClick = {
          viewModel.save(userRole)
        },
        enabled = !isSaving,
        colors = ButtonDefaults.buttonColors(
          containerColor = NavyPrimary,
          contentColor = Color.White,
          disabledContainerColor = Color(0xFF9CA3AF)
        ),
        border = BorderStroke(2.dp, NavyDark),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
          .fillMaxWidth()
          .shadow(10.dp, RoundedCornerShape(8.dp))
          .height(56.dp)
      ) {
        Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (isSaving) "Saving..." else "Save Changes",
          fontSize = 18.sp,
          fontWeight = FontWeight.SemiBold
        )
      }

      if (saveError != null) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(saveError ?: "", color = Color(0xFFB91C1C), fontSize = 14.sp)
      }
      if (saveMessage != null) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(saveMessage ?: "", color = Color(0xFF166534), fontSize = 14.sp)
      }

        Spacer(modifier = Modifier.height(12.dp))
      }
    }
  }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color.White, RoundedCornerShape(16.dp))
      .border(2.dp, NavyPrimary, RoundedCornerShape(16.dp))
      .padding(16.dp)
  ) {
    Text(
      text = title,
      color = NavyPrimary,
      fontSize = 20.sp,
      fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(16.dp))
    content()
  }
}

@Composable
private fun ProfileField(
  label: String,
  value: String,
  onValueChange: (String) -> Unit,
  icon: ImageVector,
  readOnly: Boolean = false
) {
  Text(
    text = label,
    color = NavyPrimary,
    fontSize = 14.sp,
    fontWeight = FontWeight.SemiBold
  )
  Spacer(modifier = Modifier.height(8.dp))
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    modifier = Modifier.fillMaxWidth(),
    readOnly = readOnly,
    singleLine = true,
    leadingIcon = { Icon(icon, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(20.dp)) },
    shape = RoundedCornerShape(8.dp),
    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = NavyPrimary),
    colors = OutlinedTextFieldDefaults.colors(
      focusedBorderColor = NavyPrimary,
      unfocusedBorderColor = NavyPrimary,
      focusedContainerColor = Color.White,
      unfocusedContainerColor = Color.White,
      cursorColor = NavyPrimary
    )
  )
  Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun LanguageOptionButton(
  modifier: Modifier = Modifier,
  label: String,
  selected: Boolean,
  onClick: () -> Unit
) {
  Button(
    onClick = onClick,
    colors = ButtonDefaults.buttonColors(
      containerColor = if (selected) NavyPrimary else BlueSecondary,
      contentColor = if (selected) Color.White else NavyPrimary
    ),
    border = BorderStroke(2.dp, NavyPrimary),
    shape = RoundedCornerShape(12.dp),
    modifier = modifier
  ) {
    Text(
      text = label,
      fontSize = 15.sp,
      fontWeight = FontWeight.SemiBold
    )
  }
}

private fun formatPasswordChangedLabel(rawValue: String?): String {
  if (rawValue.isNullOrBlank()) return "Password change date unavailable"
  val parsedInstant = parseTimestamp(rawValue) ?: return "Last changed $rawValue"
  val localDate = parsedInstant.atZone(ZoneId.systemDefault()).toLocalDate()
  val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
  return "Last changed ${localDate.format(formatter)}"
}

private fun formatProfilePhotoUploadError(result: ApiCallResult.Failure): String {
  val normalizedMessages = buildList {
    add(result.message)
    addAll(result.validationErrors.values)
  }.joinToString(" ").lowercase(Locale.getDefault())

  return when {
    result.statusCode == 401 -> "Session expired. Please sign in again."
    result.statusCode == 422 && (
      normalizedMessages.contains("jpg") ||
        normalizedMessages.contains("jpeg") ||
        normalizedMessages.contains("png") ||
        normalizedMessages.contains("webp") ||
        normalizedMessages.contains("mime") ||
        normalizedMessages.contains("format")
      ) -> "Invalid image format. Use JPG/PNG/WEBP"
    result.statusCode == 422 -> result.validationErrors["photo"]
      ?: result.message.ifBlank { "Upload failed, please try again" }
    result.statusCode == null -> "Upload failed, please try again"
    else -> result.message.ifBlank { "Upload failed, please try again" }
  }
}

private fun parseTimestamp(rawValue: String): Instant? {
  rawValue.toLongOrNull()?.let { numeric ->
    val seconds = if (numeric > 10_000_000_000L) numeric / 1000L else numeric
    return Instant.ofEpochSecond(seconds)
  }
  return try {
    OffsetDateTime.parse(rawValue).toInstant()
  } catch (_: DateTimeParseException) {
    try {
      Instant.parse(rawValue)
    } catch (_: DateTimeParseException) {
      null
    }
  }
}

private fun copySelectedProfilePhoto(
  context: Context,
  uri: Uri,
  userId: String
): String {
  val photoDirectory = File(context.filesDir, "profile_photos").apply { mkdirs() }
  val safeUserId = userId.ifBlank { "default_profile" }.replace(Regex("[^A-Za-z0-9._-]"), "_")
  photoDirectory.listFiles()
    ?.filter { it.name.startsWith("$safeUserId.") }
    ?.forEach(File::delete)

  val mimeType = context.contentResolver.getType(uri)
  val extension = MimeTypeMap.getSingleton()
    .getExtensionFromMimeType(mimeType)
    ?.ifBlank { null }
    ?: "jpg"
  val targetFile = File(photoDirectory, "$safeUserId.$extension")

  context.contentResolver.openInputStream(uri)?.use { inputStream ->
    targetFile.outputStream().use { outputStream ->
      inputStream.copyTo(outputStream)
    }
  } ?: error("Unable to open selected image")

  return targetFile.absolutePath
}
