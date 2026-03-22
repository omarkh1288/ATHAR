package com.athar.accessibilitymapping.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private object AtharColors {
  val Secondary = Color(0xFF1F3C5B)
  val SecondaryDark = Color(0xFF2C4F73)
  val Accent = Color(0xFFC9A24D)
  val BgPrimary = Color(0xFFEAF2FB)
  val BgSecondary = Color(0xFFD6E4F5)
  val Gray200 = Color(0xFFE2E8F0)
  val White = Color(0xFFFFFFFF)
}

data class Governorate(
  val id: Int,
  val name: String,
  val arabicName: String
)

val egyptGovernorates = listOf(
  Governorate(1, "Cairo", "القاهرة"),
  Governorate(2, "Alexandria", "الإسكندرية"),
  Governorate(3, "Giza", "الجيزة"),
  Governorate(4, "Qalyubia", "القليوبية"),
  Governorate(5, "Port Said", "بورسعيد"),
  Governorate(6, "Suez", "السويس"),
  Governorate(7, "Dakahlia", "الدقهلية"),
  Governorate(8, "Sharqia", "الشرقية"),
  Governorate(9, "Kafr El Sheikh", "كفر الشيخ"),
  Governorate(10, "Gharbia", "الغربية"),
  Governorate(11, "Monufia", "المنوفية"),
  Governorate(12, "Beheira", "البحيرة"),
  Governorate(13, "Ismailia", "الإسماعيلية"),
  Governorate(14, "Damietta", "دمياط"),
  Governorate(15, "North Sinai", "شمال سيناء"),
  Governorate(16, "South Sinai", "جنوب سيناء"),
  Governorate(17, "Faiyum", "الفيوم"),
  Governorate(18, "Beni Suef", "بني سويف"),
  Governorate(19, "Minya", "المنيا"),
  Governorate(20, "Assiut", "أسيوط"),
  Governorate(21, "Sohag", "سوهاج"),
  Governorate(22, "Qena", "قنا"),
  Governorate(23, "Luxor", "الأقصر"),
  Governorate(24, "Aswan", "أسوان"),
  Governorate(25, "Red Sea", "البحر الأحمر"),
  Governorate(26, "Matruh", "مطروح"),
  Governorate(27, "New Valley", "الوادي الجديد")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GovernorateSelector(
  selectedGovernorate: Governorate?,
  onGovernorateSelected: (Governorate) -> Unit
) {
  var showSheet by remember { mutableStateOf(false) }

  Box(
    modifier = Modifier
      .padding(horizontal = 16.dp)
      .padding(bottom = 16.dp)
  ) {
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      color = AtharColors.White,
      shadowElevation = 8.dp,
      border = BorderStroke(2.dp, AtharColors.Gray200)
    ) {
      Column(modifier = Modifier.padding(20.dp)) {
        Text(
          text = "Governorate",
          fontSize = 20.sp,
          fontWeight = FontWeight.SemiBold,
          color = AtharColors.Secondary,
          modifier = Modifier.padding(bottom = 4.dp)
        )

        Text(
          text = "Select the governorate where this place is located.",
          fontSize = 14.sp,
          color = AtharColors.Secondary.copy(alpha = 0.7f),
          modifier = Modifier.padding(bottom = 12.dp)
        )

        GovernorateSelectorButton(
          selected = selectedGovernorate,
          onClick = { showSheet = !showSheet }
        )

        AnimatedVisibility(
          visible = showSheet,
          enter = fadeIn(),
          exit = fadeOut()
        ) {
          GovernorateDropdownPanel(
            selectedGovernorate = selectedGovernorate,
            onGovernorateSelected = { gov ->
              onGovernorateSelected(gov)
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
  onClick: () -> Unit
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val hasSelection = selected != null
  val bgColor = when {
    hasSelection && isPressed -> AtharColors.SecondaryDark
    hasSelection -> AtharColors.Secondary
    else -> AtharColors.BgSecondary
  }
  val borderColor = if (hasSelection) AtharColors.SecondaryDark else AtharColors.Secondary
  val iconTint = if (hasSelection) AtharColors.White else AtharColors.Secondary

  Surface(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    color = bgColor,
    border = BorderStroke(2.dp, borderColor),
    interactionSource = interactionSource
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.weight(1f)
      ) {
        Icon(
          imageVector = Icons.Default.Business,
          contentDescription = null,
          tint = iconTint,
          modifier = Modifier.size(20.dp)
        )

        if (selected != null) {
          Column {
            Text(
              text = selected.name,
              fontSize = 14.sp,
              fontWeight = FontWeight.SemiBold,
              color = AtharColors.White
            )
            Text(
              text = selected.arabicName,
              fontSize = 12.sp,
              color = AtharColors.White.copy(alpha = 0.8f)
            )
          }
        } else {
          Text(
            text = "Select Governorate",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = AtharColors.Secondary
          )
        }
      }

      Spacer(Modifier.width(8.dp))

      Icon(
        imageVector = Icons.Default.KeyboardArrowDown,
        contentDescription = null,
        tint = iconTint,
        modifier = Modifier.size(20.dp)
      )
    }
  }
}

@Composable
private fun GovernorateDropdownPanel(
  selectedGovernorate: Governorate?,
  onGovernorateSelected: (Governorate) -> Unit,
  onDismiss: () -> Unit
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 12.dp),
    shape = RoundedCornerShape(14.dp),
    color = AtharColors.White,
    shadowElevation = 6.dp,
    border = BorderStroke(2.dp, AtharColors.Gray200)
  ) {
    Column {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = "Select Governorate",
          fontSize = 15.sp,
          fontWeight = FontWeight.SemiBold,
          color = AtharColors.Secondary
        )

        Surface(
          onClick = onDismiss,
          shape = RoundedCornerShape(8.dp),
          color = AtharColors.BgSecondary,
          border = BorderStroke(2.dp, AtharColors.Secondary)
        ) {
          Box(
            modifier = Modifier.size(28.dp),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close",
              tint = AtharColors.Secondary,
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }

      HorizontalDivider(color = AtharColors.Gray200, thickness = 1.dp)

      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 320.dp)
      ) {
        items(items = egyptGovernorates, key = { it.id }) { gov ->
          GovernorateRow(
            governorate = gov,
            isSelected = selectedGovernorate?.id == gov.id,
            onSelect = { onGovernorateSelected(gov) }
          )
          if (gov.id < egyptGovernorates.size) {
            HorizontalDivider(color = AtharColors.Gray200, thickness = 1.dp)
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GovernorateBottomSheet(
  sheetState: SheetState,
  selectedGovernorate: Governorate?,
  onGovernorateSelected: (Governorate) -> Unit,
  onDismiss: () -> Unit
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    containerColor = AtharColors.BgPrimary,
    scrimColor = Color(0x8C1F3C5B),
    dragHandle = {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 12.dp, bottom = 4.dp)
      ) {
        Box(
          modifier = Modifier
            .width(40.dp)
            .height(4.dp)
            .clip(CircleShape)
            .background(AtharColors.Gray200)
        )
      }
    },
    modifier = Modifier.fillMaxHeight(0.80f)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.weight(1f)
      ) {
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(AtharColors.Secondary)
            .border(2.dp, AtharColors.SecondaryDark, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Business,
            contentDescription = null,
            tint = AtharColors.White,
            modifier = Modifier.size(20.dp)
          )
        }

        Column {
          Text(
            text = "Select Governorate",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = AtharColors.Secondary
          )
          Text(
            text = "27 governorates of Egypt",
            fontSize = 14.sp,
            color = AtharColors.Secondary.copy(alpha = 0.65f)
          )
        }
      }

      val closeInteraction = remember { MutableInteractionSource() }
      val closePressed by closeInteraction.collectIsPressedAsState()

      Surface(
        onClick = onDismiss,
        shape = RoundedCornerShape(12.dp),
        color = if (closePressed) AtharColors.Secondary else AtharColors.BgSecondary,
        border = BorderStroke(2.dp, AtharColors.Secondary),
        interactionSource = closeInteraction,
        modifier = Modifier.size(40.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close",
            tint = if (closePressed) AtharColors.White else AtharColors.Secondary,
            modifier = Modifier.size(20.dp)
          )
        }
      }
    }

    HorizontalDivider(color = AtharColors.Gray200, thickness = 2.dp)

    LazyColumn(modifier = Modifier.fillMaxSize()) {
      items(items = egyptGovernorates, key = { it.id }) { gov ->
        GovernorateRow(
          governorate = gov,
          isSelected = selectedGovernorate?.id == gov.id,
          onSelect = { onGovernorateSelected(gov) }
        )

        if (gov.id < egyptGovernorates.size) {
          HorizontalDivider(color = AtharColors.Gray200, thickness = 2.dp)
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
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier.weight(1f)
    ) {
      val badgeBg = if (isSelected) AtharColors.Secondary else AtharColors.BgSecondary
      val badgeBorder = if (isSelected) AtharColors.SecondaryDark else AtharColors.Secondary
      val badgeText = if (isSelected) AtharColors.White else AtharColors.Secondary

      Box(
        modifier = Modifier
          .size(32.dp)
          .clip(CircleShape)
          .background(badgeBg)
          .border(2.dp, badgeBorder, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = governorate.id.toString(),
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = badgeText,
          textAlign = TextAlign.Center
        )
      }

      Column {
        Text(
          text = governorate.name,
          fontSize = 14.sp,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
          color = AtharColors.Secondary
        )
        Text(
          text = governorate.arabicName,
          fontSize = 12.sp,
          color = AtharColors.Secondary.copy(alpha = 0.6f)
        )
      }
    }

    AnimatedVisibility(
      visible = isSelected,
      enter = fadeIn(),
      exit = fadeOut()
    ) {
      Box(
        modifier = Modifier
          .size(24.dp)
          .clip(CircleShape)
          .background(AtharColors.Accent),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Check,
          contentDescription = "Selected",
          tint = AtharColors.White,
          modifier = Modifier.size(16.dp)
        )
      }
    }
  }
}

