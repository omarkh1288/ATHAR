package com.athar.accessibilitymapping.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
  primary = NavyPrimary,
  onPrimary = Color.White,
  secondary = AccentGold,
  onSecondary = Color.White,
  tertiary = AccentGoldLight,
  onTertiary = NavyPrimary,
  background = BluePrimary,
  onBackground = NavyPrimary,
  surface = Color.White,
  onSurface = NavyPrimary,
  surfaceVariant = BlueSecondary,
  onSurfaceVariant = TextLight,
  outline = Gray200,
  outlineVariant = Gray200,
  surfaceTint = Color.Transparent,
  error = ErrorRed
)

private val DarkColors = darkColorScheme(
  primary = NavyPrimary,
  onPrimary = Color.White,
  secondary = AccentGold,
  onSecondary = Color.White,
  tertiary = AccentGoldLight,
  onTertiary = NavyPrimary,
  background = NavyPrimary,
  onBackground = Color.White,
  surface = NavyDark,
  onSurface = Color.White,
  surfaceVariant = NavyDark,
  onSurfaceVariant = Color(0xFFCBD5E1),
  outline = Color(0xFF64748B),
  outlineVariant = Color(0xFF475569),
  surfaceTint = Color.Transparent,
  error = ErrorRed
)

private val AtharShapes = Shapes(
  extraSmall = RoundedCornerShape(12.dp),
  small = RoundedCornerShape(12.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(20.dp),
  extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun AtharTheme(
  darkTheme: Boolean = false,
  content: @Composable () -> Unit
) {
  val colors = if (darkTheme) DarkColors else LightColors
  MaterialTheme(
    colorScheme = colors,
    typography = Typography,
    shapes = AtharShapes,
    content = content
  )
}
