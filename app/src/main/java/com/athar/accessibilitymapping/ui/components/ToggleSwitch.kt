package com.athar.accessibilitymapping.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp

@Composable
fun ToggleSwitch(
  enabled: Boolean,
  onChange: () -> Unit,
  activeColor: Color,
  inactiveColor: Color,
  activeBorderColor: Color = activeColor.copy(alpha = 0.9f),
  inactiveBorderColor: Color = Color(0xFFD1D5DB)
) {
  val trackColor = animateColorAsState(
    targetValue = if (enabled) activeColor else inactiveColor,
    animationSpec = tween(durationMillis = 180),
    label = "trackColor"
  )
  val borderColor = animateColorAsState(
    targetValue = if (enabled) activeBorderColor else inactiveBorderColor,
    animationSpec = tween(durationMillis = 180),
    label = "borderColor"
  )
  val thumbOffset = animateDpAsState(
    targetValue = if (enabled) 24.dp else 2.dp,
    animationSpec = spring(dampingRatio = 0.8f, stiffness = 500f),
    label = "thumbOffset"
  )

  Box(
    modifier = Modifier
      .size(width = 52.dp, height = 30.dp)
      .background(trackColor.value, RoundedCornerShape(15.dp))
      .border(
        width = 1.5.dp,
        color = borderColor.value,
        shape = RoundedCornerShape(15.dp)
      )
      .clickable { onChange() }
  ) {
    Box(
      modifier = Modifier
        .offset(x = thumbOffset.value, y = 2.dp)
        .size(26.dp)
        .shadow(2.dp, CircleShape)
        .background(Color.White, CircleShape)
        .border(
          width = 1.dp,
          color = if (enabled) activeBorderColor.copy(alpha = 0.25f) else Color(0xFFD5DDE8),
          shape = CircleShape
        )
    )
  }
}
