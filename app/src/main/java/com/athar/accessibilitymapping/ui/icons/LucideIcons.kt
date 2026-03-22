package com.athar.accessibilitymapping.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private const val LUCIDE_STROKE_WIDTH = 2.5f

object LucideIcons {
  val Map: ImageVector by lazy {
    ImageVector.Builder(
      name = "LucideMap",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f
    ).apply {
      path(
        fill = SolidColor(Color.Transparent),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = LUCIDE_STROKE_WIDTH,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathFillType = PathFillType.NonZero
      ) {
        moveTo(3f, 6f)
        lineTo(9f, 3f)
        lineTo(15f, 6f)
        lineTo(21f, 3f)
        verticalLineTo(18f)
        lineTo(15f, 21f)
        lineTo(9f, 18f)
        lineTo(3f, 21f)
        close()
      }
      path(
        fill = SolidColor(Color.Transparent),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = LUCIDE_STROKE_WIDTH,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathFillType = PathFillType.NonZero
      ) {
        moveTo(9f, 3f)
        verticalLineTo(18f)
      }
      path(
        fill = SolidColor(Color.Transparent),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = LUCIDE_STROKE_WIDTH,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathFillType = PathFillType.NonZero
      ) {
        moveTo(15f, 6f)
        verticalLineTo(21f)
      }
    }.build()
  }

  val MessageCircle: ImageVector by lazy {
    ImageVector.Builder(
      name = "LucideMessageCircle",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f
    ).apply {
      path(
        fill = SolidColor(Color.Transparent),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = LUCIDE_STROKE_WIDTH,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathFillType = PathFillType.NonZero
      ) {
        moveTo(7.9f, 20f)
        arcToRelative(9f, 9f, 0f, true, false, -3.9f, -3.9f)
        lineTo(2f, 22f)
        close()
      }
    }.build()
  }

  val User: ImageVector by lazy {
    ImageVector.Builder(
      name = "LucideUser",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f
    ).apply {
      path(
        fill = SolidColor(Color.Transparent),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = LUCIDE_STROKE_WIDTH,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathFillType = PathFillType.NonZero
      ) {
        moveTo(19f, 21f)
        verticalLineTo(19f)
        arcToRelative(4f, 4f, 0f, false, false, -4f, -4f)
        horizontalLineTo(9f)
        arcToRelative(4f, 4f, 0f, false, false, -4f, 4f)
        verticalLineTo(21f)
      }
      path(
        fill = SolidColor(Color.Transparent),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = LUCIDE_STROKE_WIDTH,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathFillType = PathFillType.NonZero
      ) {
        moveTo(12f, 11f)
        arcToRelative(4f, 4f, 0f, true, false, 0f, -8f)
        arcToRelative(4f, 4f, 0f, false, false, 0f, 8f)
        close()
      }
    }.build()
  }

  val AnalyticsDashboard: ImageVector by lazy {
    ImageVector.Builder(
      name = "AnalyticsDashboard",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f
    ).apply {
      path(
        fill = SolidColor(Color.Transparent),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2.2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathFillType = PathFillType.NonZero
      ) {
        moveTo(5f, 18f)
        horizontalLineTo(19f)
      }
      path(
        fill = SolidColor(Color.Transparent),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2.2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathFillType = PathFillType.NonZero
      ) {
        moveTo(8f, 18f)
        verticalLineTo(11f)
      }
      path(
        fill = SolidColor(Color.Transparent),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2.2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathFillType = PathFillType.NonZero
      ) {
        moveTo(12f, 18f)
        verticalLineTo(8f)
      }
      path(
        fill = SolidColor(Color.Transparent),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2.2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathFillType = PathFillType.NonZero
      ) {
        moveTo(16f, 18f)
        verticalLineTo(13f)
      }
    }.build()
  }

  val CheckCircle: ImageVector by lazy {
    ImageVector.Builder(
      name = "LucideCheckCircle",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f
    ).apply {
      path(
        fill = SolidColor(Color.Transparent),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathFillType = PathFillType.NonZero
      ) {
        moveTo(22f, 12f)
        arcToRelative(10f, 10f, 0f, true, true, -20f, 0f)
        arcToRelative(10f, 10f, 0f, true, true, 20f, 0f)
      }
      path(
        fill = SolidColor(Color.Transparent),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathFillType = PathFillType.NonZero
      ) {
        moveTo(9f, 12f)
        lineTo(11f, 14f)
        lineTo(15f, 10f)
      }
    }.build()
  }
}
