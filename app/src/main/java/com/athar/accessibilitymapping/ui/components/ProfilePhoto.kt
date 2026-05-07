package com.athar.accessibilitymapping.ui.components
import com.athar.accessibilitymapping.ui.theme.ssp

import com.athar.accessibilitymapping.ui.theme.sdp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File

@Composable
fun ProfilePhoto(
  photoPath: String?,
  modifier: Modifier = Modifier,
  backgroundColor: Color,
  borderColor: Color,
  placeholderTint: Color,
  contentDescription: String? = null,
  placeholderIcon: ImageVector = Icons.Outlined.Person,
  placeholderIconSize: Dp = 40.sdp
) {
  val imageModel = remember(photoPath) {
    val normalizedPath = photoPath?.trim().orEmpty()
    when {
      normalizedPath.isBlank() -> null
      normalizedPath.startsWith("http://", ignoreCase = true) ||
        normalizedPath.startsWith("https://", ignoreCase = true) ||
        normalizedPath.startsWith("content://", ignoreCase = true) ||
        normalizedPath.startsWith("file://", ignoreCase = true) -> normalizedPath
      else -> File(normalizedPath).takeIf(File::exists)
    }
  }

  Box(
    modifier = modifier
      .clip(CircleShape)
      .background(backgroundColor, CircleShape)
      .border(2.sdp, borderColor, CircleShape),
    contentAlignment = Alignment.Center
  ) {
    if (imageModel != null) {
      AsyncImage(
        model = imageModel,
        contentDescription = contentDescription,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
      )
    } else {
      Icon(
        imageVector = placeholderIcon,
        contentDescription = contentDescription,
        tint = placeholderTint,
        modifier = Modifier.size(placeholderIconSize)
      )
    }
  }
}
