package com.athar.accessibilitymapping.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val REFERENCE_WIDTH_DP = 360f
private const val MIN_SCALE = 0.78f
private const val MAX_SCALE = 1.25f

data class ScreenDimensions(
    val widthDp: Float,
    val heightDp: Float,
    val scaleFactor: Float
)

val LocalScreenDimensions = compositionLocalOf {
    ScreenDimensions(
        widthDp = REFERENCE_WIDTH_DP,
        heightDp = 800f,
        scaleFactor = 1f
    )
}

@Composable
fun rememberScreenDimensions(): ScreenDimensions {
    val configuration = LocalConfiguration.current
    val widthDp = configuration.screenWidthDp.toFloat()
    val heightDp = configuration.screenHeightDp.toFloat()
    return remember(widthDp, heightDp) {
        val rawScale = widthDp / REFERENCE_WIDTH_DP
        val scale = rawScale.coerceIn(MIN_SCALE, MAX_SCALE)
        ScreenDimensions(widthDp = widthDp, heightDp = heightDp, scaleFactor = scale)
    }
}

// Extension functions that read from the CompositionLocal
// Use these anywhere inside a @Composable function:  16.sdp  or  14.ssp

val Int.sdp: Dp
    @Composable get() = (this * LocalScreenDimensions.current.scaleFactor).dp

val Float.sdp: Dp
    @Composable get() = (this * LocalScreenDimensions.current.scaleFactor).dp

val Int.ssp: TextUnit
    @Composable get() = (this * LocalScreenDimensions.current.scaleFactor).sp

val Float.ssp: TextUnit
    @Composable get() = (this * LocalScreenDimensions.current.scaleFactor).sp

// Helper to scale a Dp value
@Composable
fun Dp.scaled(): Dp {
    val scale = LocalScreenDimensions.current.scaleFactor
    return (this.value * scale).dp
}

// Helper to scale a TextUnit (sp) value
@Composable
fun TextUnit.scaled(): TextUnit {
    val scale = LocalScreenDimensions.current.scaleFactor
    return (this.value * scale).sp
}
