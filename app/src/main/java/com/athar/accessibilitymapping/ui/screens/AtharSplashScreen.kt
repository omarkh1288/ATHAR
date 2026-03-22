package com.athar.accessibilitymapping.ui.screens

import androidx.core.graphics.PathParser as AndroidPathParser
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.athar.accessibilitymapping.R
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

private val Gold = Color(0xFFC9A24D)
private val Navy = Color(0xFF1F3C5B)
private val SkyBlue = Color(0xFFEAF2FB)
private val NavyLight = Color(0xFF2C4F73)
private val White = Color(0xFFFFFFFF)

private enum class SplashPhase {
    INIT,
    ICONS,
    CONNECT,
    CONVERGE,
    LOGO,
    TEXT,
    EXIT,
}

private data class AccessibilityIcon(
    val label: String,
    val path: String,
    val viewBoxWidth: Float = 24f,
    val viewBoxHeight: Float = 24f,
)

private val accessibilityIcons = listOf(
    AccessibilityIcon(
        label = "Mobility",
        path = "M12 2a2 2 0 1 1 0 4 2 2 0 0 1 0-4zm-1 6h2l1 5h3l1.5 4h-2l-1-3h-3l-1.5 3.5a3.5 3.5 0 1 1-1.5-.7L10.5 13 10 8z",
    ),
    AccessibilityIcon(
        label = "Vision",
        path = "M12 5C5.6 5 1 12 1 12s4.6 7 11 7 11-7 11-7-4.6-7-11-7zm0 12a5 5 0 1 1 0-10 5 5 0 0 1 0 10zm0-8a3 3 0 1 0 0 6 3 3 0 0 0 0-6z",
    ),
    AccessibilityIcon(
        label = "Hearing",
        path = "M12 2C8.14 2 5 5.14 5 9c0 2.38 1.19 4.47 3 5.74V17c0 2.76 2.24 5 5 5h1v-2h-1c-1.66 0-3-1.34-3-3v-3.1A7 7 0 0 0 12 2zm0 2a5 5 0 0 1 5 5c0 1.3-.5 2.5-1.4 3.4l-1.4-1.4A3 3 0 0 0 12 6V4zm0 4a3 3 0 0 1 2.12 5.12L12 11V8z",
    ),
    AccessibilityIcon(
        label = "Communication",
        path = "M20 7c0-1.1-.9-2-2-2h-1c0-1.1-.9-2-2-2s-2 .9-2 2h-1c-1.1 0-2 .9-2 2v5l-2.6-2.6c-.8-.8-2-.8-2.8 0-.8.8-.8 2 0 2.8L11 17.6V22h8v-4.4l2-4V7z",
    ),
    AccessibilityIcon(
        label = "Cognitive",
        path = "M12 2C9.8 2 8 3.8 8 6c0 .7.2 1.4.5 2H7c-1.7 0-3 1.3-3 3 0 1.2.7 2.2 1.7 2.7C4.7 14.2 4 15.3 4 16.5 4 18.4 5.6 20 7.5 20H9v2h6v-2h1.5c1.9 0 3.5-1.6 3.5-3.5 0-1.2-.7-2.3-1.7-2.8 1-.5 1.7-1.5 1.7-2.7 0-1.7-1.3-3-3-3h-1.5c.3-.6.5-1.3.5-2 0-2.2-1.8-4-4-4z",
    ),
)

private data class Particle(
    val x: Float,
    val y: Float,
    val size: Float,
    val delay: Int,
    val duration: Int,
    val opacity: Float,
    val isGold: Boolean,
)

private val particles = (0 until 20).map { i ->
    Particle(
        x = Random.nextFloat(),
        y = Random.nextFloat(),
        size = 3f + Random.nextFloat() * 5f,
        delay = (Random.nextFloat() * 6000).toInt(),
        duration = (4000 + Random.nextFloat() * 4000).toInt(),
        opacity = 0.06f + Random.nextFloat() * 0.12f,
        isGold = i % 3 == 0,
    )
}

private fun calculateIconPositions(): List<Offset> {
    val centerX = 0.5f
    val centerY = 0.5f
    val radius = 0.28f

    return (0 until 5).map { i ->
        val angle = ((i.toFloat() / 5f) * 360f - 90f) * (PI.toFloat() / 180f)
        Offset(
            centerX + radius * cos(angle),
            centerY + radius * sin(angle),
        )
    }
}

private fun insetLine(
    start: Offset,
    end: Offset,
    startInset: Float,
    endInset: Float = startInset,
): Pair<Offset, Offset> {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val distance = hypot(dx.toDouble(), dy.toDouble()).toFloat()
    if (distance <= 0f || startInset + endInset >= distance) {
        return start to end
    }

    val unitX = dx / distance
    val unitY = dy / distance
    val insetStart = Offset(
        x = start.x + unitX * startInset,
        y = start.y + unitY * startInset,
    )
    val insetEnd = Offset(
        x = end.x - unitX * endInset,
        y = end.y - unitY * endInset,
    )
    return insetStart to insetEnd
}

private fun parseSvgPath(pathData: String, size: Float, viewBox: Float = 24f): Path {
    return try {
        val androidPath = AndroidPathParser.createPathFromPathData(pathData)
        val composePath = androidPath.asComposePath()
        val scale = size / viewBox
        val matrix = android.graphics.Matrix()
        matrix.setScale(scale, scale)
        composePath.asAndroidPath().transform(matrix)
        composePath
    } catch (_: Exception) {
        Path().apply {
            addOval(Rect(0f, 0f, size, size))
        }
    }
}

@Composable
fun AtharSplashScreen(onComplete: () -> Unit) {
    var phase by remember { mutableStateOf(SplashPhase.INIT) }

    LaunchedEffect(Unit) {
        delay(600)
        phase = SplashPhase.ICONS
        delay(1200)
        phase = SplashPhase.CONNECT
        delay(1600)
        phase = SplashPhase.CONVERGE
        delay(800)
        phase = SplashPhase.LOGO
        delay(800)
        phase = SplashPhase.TEXT
        delay(1500)
        phase = SplashPhase.EXIT
        delay(180)
        onComplete()
    }

    fun isPhase(vararg phases: SplashPhase): Boolean = phase in phases

    val iconPositions = remember { calculateIconPositions() }
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp
    val density = LocalDensity.current

    val bgAlpha by animateFloatAsState(
        targetValue = if (isPhase(SplashPhase.INIT)) 0f else 1f,
        animationSpec = tween(1500, easing = EaseInOut),
        label = "bgAlpha",
    )

    val gridOpacity by animateFloatAsState(
        targetValue = if (isPhase(SplashPhase.INIT)) 0f else 0.04f,
        animationSpec = tween(2000, delayMillis = 500, easing = EaseInOut),
        label = "gridOpacity",
    )

    val iconOpacity by animateFloatAsState(
        targetValue = when {
            isPhase(SplashPhase.INIT) -> 0f
            isPhase(
                SplashPhase.CONVERGE,
                SplashPhase.LOGO,
                SplashPhase.TEXT,
                SplashPhase.EXIT,
            ) -> 0f
            else -> 1f
        },
        animationSpec = tween(400, easing = EaseInOut),
        label = "iconOpacity",
    )

    val iconScale by animateFloatAsState(
        targetValue = when {
            isPhase(
                SplashPhase.CONVERGE,
                SplashPhase.LOGO,
                SplashPhase.TEXT,
                SplashPhase.EXIT,
            ) -> 0f
            else -> 1f
        },
        animationSpec = tween(800, easing = EaseOutBack),
        label = "iconScale",
    )

    val convergeProgress by animateFloatAsState(
        targetValue = if (
            isPhase(
                SplashPhase.CONVERGE,
                SplashPhase.LOGO,
                SplashPhase.TEXT,
                SplashPhase.EXIT,
            )
        ) 1f else 0f,
        animationSpec = tween(800, easing = EaseOutBack),
        label = "convergeProgress",
    )

    val linesOpacity by animateFloatAsState(
        targetValue = if (isPhase(SplashPhase.CONNECT)) 1f else 0f,
        animationSpec = tween(500, easing = EaseInOut),
        label = "linesOpacity",
    )

    val lineDashProgress by animateFloatAsState(
        targetValue = if (isPhase(SplashPhase.CONNECT)) 1f else 0f,
        animationSpec = tween(600, easing = EaseInOut),
        label = "lineDashProgress",
    )

    val burstScale by animateFloatAsState(
        targetValue = if (isPhase(SplashPhase.CONVERGE)) 4f else 0.5f,
        animationSpec = tween(800, easing = EaseOut),
        label = "burstScale",
    )

    val burstOpacity by animateFloatAsState(
        targetValue = if (isPhase(SplashPhase.CONVERGE)) 0f else 0f,
        animationSpec = keyframes {
            durationMillis = 800
            0f at 0
            1f at 320
            0f at 800
        },
        label = "burstOpacity",
    )

    val ring0Scale by animateFloatAsState(
        targetValue = if (isPhase(SplashPhase.CONVERGE, SplashPhase.LOGO, SplashPhase.TEXT)) 5f else 1f,
        animationSpec = tween(1000, easing = EaseOut),
        label = "ring0Scale",
    )

    val ring0Opacity by animateFloatAsState(
        targetValue = if (isPhase(SplashPhase.CONVERGE, SplashPhase.LOGO, SplashPhase.TEXT)) 0f else 0.8f,
        animationSpec = tween(1000, easing = EaseOut),
        label = "ring0Opacity",
    )

    val ring1Scale by animateFloatAsState(
        targetValue = if (isPhase(SplashPhase.CONVERGE, SplashPhase.LOGO, SplashPhase.TEXT)) 5f else 1f,
        animationSpec = tween(1000, delayMillis = 150, easing = EaseOut),
        label = "ring1Scale",
    )

    val ring1Opacity by animateFloatAsState(
        targetValue = if (isPhase(SplashPhase.CONVERGE, SplashPhase.LOGO, SplashPhase.TEXT)) 0f else 0.8f,
        animationSpec = tween(1000, delayMillis = 150, easing = EaseOut),
        label = "ring1Opacity",
    )

    val ring2Scale by animateFloatAsState(
        targetValue = if (isPhase(SplashPhase.CONVERGE, SplashPhase.LOGO, SplashPhase.TEXT)) 5f else 1f,
        animationSpec = tween(1000, delayMillis = 300, easing = EaseOut),
        label = "ring2Scale",
    )

    val ring2Opacity by animateFloatAsState(
        targetValue = if (isPhase(SplashPhase.CONVERGE, SplashPhase.LOGO, SplashPhase.TEXT)) 0f else 0.8f,
        animationSpec = tween(1000, delayMillis = 300, easing = EaseOut),
        label = "ring2Opacity",
    )

    val logoOpacity by animateFloatAsState(
        targetValue = when {
            isPhase(SplashPhase.LOGO, SplashPhase.TEXT) -> 1f
            isPhase(SplashPhase.EXIT) -> 0f
            else -> 0f
        },
        animationSpec = tween(700, easing = EaseOutBack),
        label = "logoOpacity",
    )

    val logoScale by animateFloatAsState(
        targetValue = when {
            isPhase(SplashPhase.LOGO, SplashPhase.TEXT) -> 1f
            isPhase(SplashPhase.EXIT) -> 0.9f
            else -> 0.6f
        },
        animationSpec = tween(700, easing = EaseOutBack),
        label = "logoScale",
    )

    val logoTranslateY by animateFloatAsState(
        targetValue = when {
            isPhase(SplashPhase.LOGO, SplashPhase.TEXT) -> 0f
            isPhase(SplashPhase.EXIT) -> -10f
            else -> 20f
        },
        animationSpec = tween(700, easing = EaseOutBack),
        label = "logoTranslateY",
    )

    val logoBlur by animateDpAsState(
        targetValue = if (isPhase(SplashPhase.EXIT)) 6.dp else 0.dp,
        animationSpec = tween(700),
        label = "logoBlur",
    )

    val letterAnimations = (0 until 5).map { i ->
        val opacity by animateFloatAsState(
            targetValue = when {
                isPhase(SplashPhase.EXIT) -> 0f
                isPhase(SplashPhase.TEXT) -> 1f
                else -> 0f
            },
            animationSpec = tween(
                durationMillis = 500,
                delayMillis = 100 + i * 60,
                easing = EaseOutBack,
            ),
            label = "letter${i}Opacity",
        )
        val translateY by animateFloatAsState(
            targetValue = if (isPhase(SplashPhase.TEXT)) 0f else 12f,
            animationSpec = tween(
                durationMillis = 500,
                delayMillis = 100 + i * 60,
                easing = EaseOutBack,
            ),
            label = "letter${i}TranslateY",
        )
        val scale by animateFloatAsState(
            targetValue = if (isPhase(SplashPhase.TEXT)) 1f else 0.85f,
            animationSpec = tween(
                durationMillis = 500,
                delayMillis = 100 + i * 60,
                easing = EaseOutBack,
            ),
            label = "letter${i}Scale",
        )
        Triple(opacity, translateY, scale)
    }

    val underlineWidth by animateDpAsState(
        targetValue = if (isPhase(SplashPhase.TEXT)) 70.dp else 0.dp,
        animationSpec = tween(600, delayMillis = 500, easing = EaseOutBack),
        label = "underlineWidth",
    )

    val underlineOpacity by animateFloatAsState(
        targetValue = if (isPhase(SplashPhase.EXIT)) 0f else 1f,
        animationSpec = tween(300),
        label = "underlineOpacity",
    )

    val taglineOpacity by animateFloatAsState(
        targetValue = when {
            isPhase(SplashPhase.EXIT) -> 0f
            isPhase(SplashPhase.TEXT) -> 1f
            else -> 0f
        },
        animationSpec = tween(600, delayMillis = 700, easing = EaseInOut),
        label = "taglineOpacity",
    )

    val taglineTranslateY by animateFloatAsState(
        targetValue = if (isPhase(SplashPhase.TEXT)) 0f else 8f,
        animationSpec = tween(600, delayMillis = 700, easing = EaseInOut),
        label = "taglineTranslateY",
    )

    val missionOpacity by animateFloatAsState(
        targetValue = when {
            isPhase(SplashPhase.EXIT) -> 0f
            isPhase(SplashPhase.TEXT) -> 1f
            else -> 0f
        },
        animationSpec = tween(600, delayMillis = 900, easing = EaseInOut),
        label = "missionOpacity",
    )

    val dotsOpacity by animateFloatAsState(
        targetValue = when {
            isPhase(SplashPhase.EXIT) -> 0f
            isPhase(
                SplashPhase.ICONS,
                SplashPhase.CONNECT,
                SplashPhase.CONVERGE,
                SplashPhase.LOGO,
                SplashPhase.TEXT,
            ) -> 1f
            else -> 0f
        },
        animationSpec = tween(500, easing = EaseInOut),
        label = "dotsOpacity",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "floatTransition")

    val iconFloatOffsets = (0 until 5).map { i ->
        val offset by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -6f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 2500 + i * 300,
                    easing = EaseInOut,
                ),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "iconFloat$i",
        )
        offset
    }

    val particleOffsets = particles.map { particle ->
        val offsetY by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -15f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = particle.duration,
                    delayMillis = particle.delay,
                    easing = EaseInOut,
                ),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "particleY",
        )
        val offsetX by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 8f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = particle.duration,
                    delayMillis = particle.delay,
                    easing = EaseInOut,
                ),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "particleX",
        )
        Offset(offsetX, offsetY)
    }

    val particleOpacity by animateFloatAsState(
        targetValue = if (isPhase(SplashPhase.INIT)) 0f else 1f,
        animationSpec = tween(1500, easing = EaseInOut),
        label = "particleOpacity",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SkyBlue),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .alpha(bgAlpha),
        ) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(White, Color(0xFFD8E8F6), SkyBlue),
                    center = Offset(size.width / 2, size.height / 2),
                    radius = size.width * 0.7f,
                ),
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .alpha(gridOpacity),
        ) {
            val gridSize = 50.dp.toPx()
            val cols = (size.width / gridSize).toInt() + 1
            val rows = (size.height / gridSize).toInt() + 1

            for (col in 0..cols) {
                for (row in 0..rows) {
                    val x = col * gridSize
                    val y = row * gridSize
                    drawLine(
                        color = Navy.copy(alpha = 0.4f),
                        start = Offset(x, y + gridSize / 2),
                        end = Offset(x + gridSize, y + gridSize / 2),
                        strokeWidth = 0.3.dp.toPx(),
                    )
                    drawLine(
                        color = Navy.copy(alpha = 0.4f),
                        start = Offset(x + gridSize / 2, y),
                        end = Offset(x + gridSize / 2, y + gridSize),
                        strokeWidth = 0.3.dp.toPx(),
                    )
                    drawCircle(
                        color = Navy.copy(alpha = 0.15f),
                        radius = 1.5.dp.toPx(),
                        center = Offset(x + gridSize / 2, y + gridSize / 2),
                    )
                }
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .alpha(particleOpacity),
        ) {
            particles.forEachIndexed { i, particle ->
                val offset = particleOffsets[i]
                val particleX = particle.x * size.width + offset.x.dp.toPx()
                val particleY = particle.y * size.height + offset.y.dp.toPx()
                val particleSize = particle.size.dp.toPx()

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = if (particle.isGold) {
                            listOf(Gold.copy(alpha = 0.38f), Color.Transparent)
                        } else {
                            listOf(Navy.copy(alpha = 0.15f), Color.Transparent)
                        },
                        center = Offset(particleX, particleY),
                        radius = particleSize,
                    ),
                    radius = particleSize,
                    center = Offset(particleX, particleY),
                    alpha = particle.opacity,
                )
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .alpha(linesOpacity),
        ) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val iconInset = 34.dp.toPx()

            iconPositions.forEach { position ->
                val startX = position.x * size.width
                val startY = position.y * size.height
                val iconCenter = Offset(startX, startY)
                val centerPoint = Offset(centerX, centerY)
                val (lineStart, lineEnd) = insetLine(
                    start = iconCenter,
                    end = centerPoint,
                    startInset = iconInset,
                    endInset = 0f,
                )

                drawLine(
                    color = Gold.copy(alpha = 0.5f),
                    start = lineStart,
                    end = Offset(
                        lineStart.x + (lineEnd.x - lineStart.x) * lineDashProgress,
                        lineStart.y + (lineEnd.y - lineStart.y) * lineDashProgress,
                    ),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        intervals = floatArrayOf(6.dp.toPx(), 4.dp.toPx()),
                        phase = 0f,
                    ),
                    cap = StrokeCap.Round,
                )
            }

            iconPositions.forEachIndexed { i, position ->
                val next = iconPositions[(i + 1) % iconPositions.size]
                val startX = position.x * size.width
                val startY = position.y * size.height
                val endX = next.x * size.width
                val endY = next.y * size.height
                val (lineStart, lineEnd) = insetLine(
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    startInset = iconInset,
                    endInset = iconInset,
                )

                drawLine(
                    color = Navy.copy(alpha = 0.2f),
                    start = lineStart,
                    end = Offset(
                        lineStart.x + (lineEnd.x - lineStart.x) * lineDashProgress,
                        lineStart.y + (lineEnd.y - lineStart.y) * lineDashProgress,
                    ),
                    strokeWidth = 0.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        intervals = floatArrayOf(3.dp.toPx(), 6.dp.toPx()),
                        phase = 0f,
                    ),
                    cap = StrokeCap.Round,
                )
            }
        }

        accessibilityIcons.forEachIndexed { i, icon ->
            val position = iconPositions[i]
            val currentX = position.x + (0.5f - position.x) * convergeProgress
            val currentY = position.y + (0.5f - position.y) * convergeProgress
            val floatOffset = if (isPhase(SplashPhase.ICONS, SplashPhase.CONNECT)) {
                iconFloatOffsets[i]
            } else {
                0f
            }

            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .offset(
                            x = (currentX * screenWidthDp.value).dp - 56.dp,
                            y = (currentY * screenHeightDp.value).dp - 28.dp + floatOffset.dp,
                        )
                        .width(112.dp)
                        .alpha(iconOpacity),
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .size(56.dp)
                            .graphicsLayer(scaleX = iconScale, scaleY = iconScale)
                            .shadow(
                                elevation = 10.dp,
                                shape = CircleShape,
                                ambientColor = Navy.copy(alpha = 0.15f),
                                spotColor = Navy.copy(alpha = 0.15f),
                            )
                            .clip(CircleShape)
                            .background(White)
                            .drawBehind {
                                drawCircle(
                                    color = Gold,
                                    radius = size.minDimension / 2,
                                    style = Stroke(width = 2.5.dp.toPx()),
                                )
                                drawCircle(
                                    color = Gold.copy(alpha = 0.06f),
                                    radius = size.minDimension / 2 + 4.dp.toPx(),
                                    style = Stroke(width = 4.dp.toPx()),
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(modifier = Modifier.size(26.dp)) {
                            val iconPath = parseSvgPath(icon.path, size.width, icon.viewBoxWidth)
                            drawPath(path = iconPath, color = Navy)
                        }
                    }

                    Text(
                        text = icon.label,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = 68.dp)
                            .width(112.dp)
                            .alpha(linesOpacity),
                        color = NavyLight,
                        fontSize = 10.sp,
                        letterSpacing = 0.4.sp,
                        fontFamily = FontFamily.SansSerif,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }

        if (isPhase(SplashPhase.CONVERGE)) {
            Canvas(
                modifier = Modifier
                    .size(100.dp)
                    .alpha(burstOpacity)
                    .graphicsLayer(scaleX = burstScale, scaleY = burstScale),
            ) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Gold.copy(alpha = 0.19f), Color.Transparent),
                        center = Offset(size.width / 2, size.height / 2),
                        radius = size.width / 2,
                    ),
                )
            }
        }

        val ringData = listOf(
            Triple(ring0Scale, ring0Opacity, 2f),
            Triple(ring1Scale, ring1Opacity, 1.5f),
            Triple(ring2Scale, ring2Opacity, 1f),
        )

        ringData.forEachIndexed { i, (scale, opacity, strokeWidth) ->
            if (isPhase(SplashPhase.CONVERGE, SplashPhase.LOGO, SplashPhase.TEXT)) {
                Canvas(
                    modifier = Modifier
                        .size(60.dp)
                        .graphicsLayer(scaleX = scale, scaleY = scale)
                        .alpha(opacity),
                ) {
                    drawCircle(
                        color = if (i == 0) Gold else Gold.copy(alpha = 0.38f),
                        radius = size.minDimension / 2,
                        style = Stroke(width = strokeWidth.dp.toPx()),
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .graphicsLayer(
                    scaleX = logoScale,
                    scaleY = logoScale,
                    translationY = with(density) { logoTranslateY.dp.toPx() },
                    alpha = logoOpacity,
                )
                .blur(logoBlur),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .shadow(
                        elevation = 20.dp,
                        shape = CircleShape,
                        ambientColor = Gold.copy(alpha = 0.08f),
                        spotColor = Navy.copy(alpha = 0.07f),
                    )
                    .clip(CircleShape)
                    .background(White)
                    .drawBehind {
                        drawCircle(
                            color = Gold.copy(alpha = 0.31f),
                            radius = size.minDimension / 2,
                            style = Stroke(width = 3.dp.toPx()),
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.athar_logo),
                    contentDescription = "Athar Logo",
                    modifier = Modifier.size(100.dp),
                    contentScale = ContentScale.Fit,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val letters = "ATHAR"
                letters.forEachIndexed { i, letter ->
                    val (letterOpacity, letterTranslateY, letterScale) = letterAnimations[i]
                    Text(
                        text = letter.toString(),
                        modifier = Modifier.graphicsLayer(
                            alpha = letterOpacity,
                            translationY = with(density) { letterTranslateY.dp.toPx() },
                            scaleX = letterScale,
                            scaleY = letterScale,
                        ),
                        color = Navy,
                        fontSize = 26.sp,
                        letterSpacing = if (i == 4) 0.sp else 8.sp,
                        fontFamily = FontFamily.SansSerif,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .width(underlineWidth)
                    .height(2.dp)
                    .alpha(underlineOpacity)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, Gold, Color.Transparent),
                        ),
                    ),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "ACCESSIBILITY FIRST",
                modifier = Modifier
                    .alpha(taglineOpacity)
                    .graphicsLayer(
                        translationY = with(density) { taglineTranslateY.dp.toPx() },
                    ),
                color = Gold,
                fontSize = 9.sp,
                letterSpacing = 3.5.sp,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center,
            )
        }

        Text(
            text = "Mapping accessibility for everyone",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
                .alpha(missionOpacity * 0.7f),
            color = NavyLight,
            fontSize = 12.sp,
            letterSpacing = 0.5.sp,
            fontFamily = FontFamily.SansSerif,
            textAlign = TextAlign.Center,
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 50.dp)
                .alpha(dotsOpacity),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val dotPhases = listOf(
                listOf(
                    SplashPhase.ICONS,
                    SplashPhase.CONNECT,
                    SplashPhase.CONVERGE,
                    SplashPhase.LOGO,
                    SplashPhase.TEXT,
                ),
                listOf(
                    SplashPhase.CONNECT,
                    SplashPhase.CONVERGE,
                    SplashPhase.LOGO,
                    SplashPhase.TEXT,
                ),
                listOf(
                    SplashPhase.CONVERGE,
                    SplashPhase.LOGO,
                    SplashPhase.TEXT,
                ),
                listOf(SplashPhase.LOGO, SplashPhase.TEXT),
                listOf(SplashPhase.TEXT),
            )

            dotPhases.forEach { phases ->
                val active = phase in phases
                val dotWidth by animateDpAsState(
                    targetValue = if (active) 20.dp else 6.dp,
                    animationSpec = tween(400, easing = EaseInOut),
                    label = "dotWidth",
                )

                Box(
                    modifier = Modifier
                        .width(dotWidth)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (active) {
                                Gold
                            } else {
                                Navy.copy(alpha = 0.13f)
                            },
                        )
                        .then(
                            if (active) {
                                Modifier.shadow(
                                    elevation = 3.dp,
                                    shape = RoundedCornerShape(3.dp),
                                    ambientColor = Gold.copy(alpha = 0.25f),
                                    spotColor = Gold.copy(alpha = 0.25f),
                                )
                            } else {
                                Modifier
                            },
                        ),
                )
            }
        }
    }
}

private val EaseOutBack = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

