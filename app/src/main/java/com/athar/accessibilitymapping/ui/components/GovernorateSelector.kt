package com.athar.accessibilitymapping.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.athar.accessibilitymapping.ui.theme.sdp
import com.athar.accessibilitymapping.ui.theme.ssp

private object AtharColors {
  val Secondary = Color(0xFF1F3C5B)
  val SecondaryDark = Color(0xFF2C4F73)
  val Accent = Color(0xFFC9A24D)
  val BgPrimary = Color(0xFFEAF2FB)
  val BgSecondary = Color(0xFFD6E4F5)
  val Gray200 = Color(0xFFE2E8F0)
  val White = Color(0xFFFFFFFF)
  val Error = Color(0xFFB91C1C)
}

data class Governorate(
  val id: Int,
  val name: String,
  val arabicName: String? = null
)

@Composable
fun GovernorateSelector(
  governorates: List<Governorate>,
  selectedGovernorate: Governorate?,
  isLoading: Boolean = false,
  errorMessage: String? = null,
  onGovernorateSelected: (Governorate) -> Unit
) {
  var showSheet by remember { mutableStateOf(false) }
  val canOpenSelector = governorates.isNotEmpty() && !isLoading

  Box(
    modifier = Modifier
      .padding(horizontal = 16.sdp)
      .padding(bottom = 16.sdp)
  ) {
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.sdp),
      color = AtharColors.White,
      shadowElevation = 8.sdp,
      border = BorderStroke(2.sdp, AtharColors.Gray200)
    ) {
      Column(modifier = Modifier.padding(20.sdp)) {
        Text(
          text = "Governorate",
          fontSize = 20.ssp,
          fontWeight = FontWeight.SemiBold,
          color = AtharColors.Secondary,
          modifier = Modifier.padding(bottom = 4.sdp)
        )

        Text(
          text = "Select the governorate where this place is located.",
          fontSize = 14.ssp,
          color = AtharColors.Secondary.copy(alpha = 0.7f),
          modifier = Modifier.padding(bottom = 12.sdp)
        )

        GovernorateSelectorButton(
          selected = selectedGovernorate,
          enabled = canOpenSelector,
          placeholder = when {
            isLoading -> "Loading governorates..."
            governorates.isEmpty() -> "No governorates available"
            else -> "Select Governorate"
          },
          onClick = {
            if (canOpenSelector) {
              showSheet = !showSheet
            }
          }
        )

        if (!errorMessage.isNullOrBlank()) {
          Text(
            text = errorMessage,
            fontSize = 12.ssp,
            color = AtharColors.Error,
            modifier = Modifier.padding(top = 10.sdp)
          )
        }

        if (!isLoading && governorates.isEmpty() && errorMessage.isNullOrBlank()) {
          Text(
            text = "The server did not return any governorates yet.",
            fontSize = 12.ssp,
            color = AtharColors.Secondary.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 10.sdp)
          )
        }

        AnimatedVisibility(
          visible = showSheet && governorates.isNotEmpty(),
          enter = fadeIn(),
          exit = fadeOut()
        ) {
          GovernorateDropdownPanel(
            governorates = governorates,
            selectedGovernorate = selectedGovernorate,
            onGovernorateSelected = { governorate ->
              onGovernorateSelected(governorate)
              showSheet = false
            },
            onDismiss = { showSheet = false }
          )
        }
      }
    }
  }
}

@Composable
private fun GovernorateSelectorButton(
  selected: Governorate?,
  enabled: Boolean,
  placeholder: String,
  onClick: () -> Unit
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val hasSelection = selected != null
  val bgColor = when {
    !enabled -> AtharColors.Gray200
    hasSelection && isPressed -> AtharColors.SecondaryDark
    hasSelection -> AtharColors.Secondary
    else -> AtharColors.BgSecondary
  }
  val borderColor = if (hasSelection && enabled) AtharColors.SecondaryDark else AtharColors.Secondary
  val iconTint = if (hasSelection && enabled) AtharColors.White else AtharColors.Secondary

  Surface(
    onClick = onClick,
    enabled = enabled,
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.sdp),
    color = bgColor,
    border = BorderStroke(2.sdp, borderColor),
    interactionSource = interactionSource
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.sdp, vertical = 12.sdp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.sdp),
        modifier = Modifier.weight(1f)
      ) {
        Icon(
          imageVector = Icons.Default.Business,
          contentDescription = null,
          tint = iconTint,
          modifier = Modifier.size(20.sdp)
        )

        if (selected != null) {
          Column {
            Text(
              text = selected.name,
              fontSize = 14.ssp,
              fontWeight = FontWeight.SemiBold,
              color = AtharColors.White
            )
            selected.arabicName?.takeIf { it.isNotBlank() }?.let { arabicName ->
              Text(
                text = arabicName,
                fontSize = 12.ssp,
                color = AtharColors.White.copy(alpha = 0.8f)
              )
            }
          }
        } else {
          Text(
            text = placeholder,
            fontSize = 14.ssp,
            fontWeight = FontWeight.SemiBold,
            color = AtharColors.Secondary
          )
        }
      }

      Spacer(Modifier.width(8.sdp))

      Icon(
        imageVector = Icons.Default.KeyboardArrowDown,
        contentDescription = null,
        tint = iconTint,
        modifier = Modifier.size(20.sdp)
      )
    }
  }
}

@Composable
private fun GovernorateDropdownPanel(
  governorates: List<Governorate>,
  selectedGovernorate: Governorate?,
  onGovernorateSelected: (Governorate) -> Unit,
  onDismiss: () -> Unit
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 12.sdp),
    shape = RoundedCornerShape(14.sdp),
    color = AtharColors.White,
    shadowElevation = 6.sdp,
    border = BorderStroke(2.sdp, AtharColors.Gray200)
  ) {
    Column {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.sdp, vertical = 12.sdp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column {
          Text(
            text = "Select Governorate",
            fontSize = 15.ssp,
            fontWeight = FontWeight.SemiBold,
            color = AtharColors.Secondary
          )
          Text(
            text = "${governorates.size} options from server",
            fontSize = 12.ssp,
            color = AtharColors.Secondary.copy(alpha = 0.65f)
          )
        }

        Surface(
          onClick = onDismiss,
          shape = RoundedCornerShape(8.sdp),
          color = AtharColors.BgSecondary,
          border = BorderStroke(2.sdp, AtharColors.Secondary)
        ) {
          Box(
            modifier = Modifier.size(28.sdp),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close",
              tint = AtharColors.Secondary,
              modifier = Modifier.size(16.sdp)
            )
          }
        }
      }

      HorizontalDivider(color = AtharColors.Gray200, thickness = 1.sdp)

      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 320.sdp)
      ) {
        itemsIndexed(governorates, key = { _, governorate -> governorate.id }) { index, governorate ->
          GovernorateRow(
            governorate = governorate,
            isSelected = selectedGovernorate?.id == governorate.id,
            onSelect = { onGovernorateSelected(governorate) }
          )
          if (index < governorates.lastIndex) {
            HorizontalDivider(color = AtharColors.Gray200, thickness = 1.sdp)
          }
        }
      }
    }
  }
}

@Composable
private fun GovernorateRow(
  governorate: Governorate,
  isSelected: Boolean,
  onSelect: () -> Unit
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val rowBg = when {
    isSelected || isPressed -> AtharColors.BgSecondary
    else -> AtharColors.White
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(rowBg)
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onSelect
      )
      .padding(horizontal = 16.sdp, vertical = 12.sdp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.sdp),
      modifier = Modifier.weight(1f)
    ) {
      val badgeBg = if (isSelected) AtharColors.Secondary else AtharColors.BgSecondary
      val badgeBorder = if (isSelected) AtharColors.SecondaryDark else AtharColors.Secondary
      val badgeText = if (isSelected) AtharColors.White else AtharColors.Secondary

      Box(
        modifier = Modifier
          .size(32.sdp)
          .clip(CircleShape)
          .background(badgeBg)
          .border(2.sdp, badgeBorder, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = governorate.id.toString(),
          fontSize = 12.ssp,
          fontWeight = FontWeight.Bold,
          color = badgeText,
          textAlign = TextAlign.Center
        )
      }

      Column {
        Text(
          text = governorate.name,
          fontSize = 14.ssp,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
          color = AtharColors.Secondary
        )
        governorate.arabicName?.takeIf { it.isNotBlank() }?.let { arabicName ->
          Text(
            text = arabicName,
            fontSize = 12.ssp,
            color = AtharColors.Secondary.copy(alpha = 0.6f)
          )
        }
      }
    }

    AnimatedVisibility(
      visible = isSelected,
      enter = fadeIn(),
      exit = fadeOut()
    ) {
      Box(
        modifier = Modifier
          .size(24.sdp)
          .clip(CircleShape)
          .background(AtharColors.Accent),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Check,
          contentDescription = "Selected",
          tint = AtharColors.White,
          modifier = Modifier.size(16.sdp)
        )
      }
    }
  }
}
