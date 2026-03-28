package com.athar.accessibilitymapping.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val AtharSans = FontFamily.SansSerif

// Static fallback used before CompositionLocal is available
val Typography = Typography(
  headlineLarge = TextStyle(fontFamily = AtharSans, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp),
  headlineMedium = TextStyle(fontFamily = AtharSans, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp),
  headlineSmall = TextStyle(fontFamily = AtharSans, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp),
  titleLarge = TextStyle(fontFamily = AtharSans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
  titleMedium = TextStyle(fontFamily = AtharSans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
  titleSmall = TextStyle(fontFamily = AtharSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
  bodyLarge = TextStyle(fontFamily = AtharSans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
  bodyMedium = TextStyle(fontFamily = AtharSans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
  bodySmall = TextStyle(fontFamily = AtharSans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
  labelLarge = TextStyle(fontFamily = AtharSans, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 28.sp),
  labelMedium = TextStyle(fontFamily = AtharSans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
  labelSmall = TextStyle(fontFamily = AtharSans, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp)
)

@Composable
fun responsiveTypography(): Typography = Typography(
  headlineLarge = TextStyle(fontFamily = AtharSans, fontWeight = FontWeight.Bold, fontSize = 30.ssp, lineHeight = 36.ssp),
  headlineMedium = TextStyle(fontFamily = AtharSans, fontWeight = FontWeight.Bold, fontSize = 30.ssp, lineHeight = 36.ssp),
  headlineSmall = TextStyle(fontFamily = AtharSans, fontWeight = FontWeight.Bold, fontSize = 30.ssp, lineHeight = 36.ssp),
  titleLarge = TextStyle(fontFamily = AtharSans, fontWeight = FontWeight.SemiBold, fontSize = 20.ssp, lineHeight = 28.ssp),
  titleMedium = TextStyle(fontFamily = AtharSans, fontWeight = FontWeight.SemiBold, fontSize = 20.ssp, lineHeight = 28.ssp),
  titleSmall = TextStyle(fontFamily = AtharSans, fontWeight = FontWeight.SemiBold, fontSize = 16.ssp, lineHeight = 24.ssp),
  bodyLarge = TextStyle(fontFamily = AtharSans, fontWeight = FontWeight.Normal, fontSize = 16.ssp, lineHeight = 24.ssp),
  bodyMedium = TextStyle(fontFamily = AtharSans, fontWeight = FontWeight.Normal, fontSize = 16.ssp, lineHeight = 24.ssp),
  bodySmall = TextStyle(fontFamily = AtharSans, fontWeight = FontWeight.Normal, fontSize = 14.ssp, lineHeight = 20.ssp),
  labelLarge = TextStyle(fontFamily = AtharSans, fontWeight = FontWeight.Medium, fontSize = 18.ssp, lineHeight = 28.ssp),
  labelMedium = TextStyle(fontFamily = AtharSans, fontWeight = FontWeight.Medium, fontSize = 14.ssp, lineHeight = 20.ssp),
  labelSmall = TextStyle(fontFamily = AtharSans, fontWeight = FontWeight.Medium, fontSize = 12.ssp, lineHeight = 16.ssp)
)
