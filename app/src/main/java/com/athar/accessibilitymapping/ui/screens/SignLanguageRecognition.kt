package com.athar.accessibilitymapping.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.Category
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult
import kotlin.math.roundToInt

internal const val MinimumAcceptedConfidence = 70
private const val GestureModelAsset = "gesture_recognizer.task"

internal data class GestureTranslation(
  val modelLabel: String,
  val english: String,
  val arabic: String,
  val hint: String
)

internal data class GestureFrameResult(
  val translation: GestureTranslation? = null,
  val confidencePercent: Int = 0,
  val handCount: Int = 0,
  val rawLabel: String? = null,
  val landmarks: List<GestureLandmark> = emptyList(),
  val errorMessage: String? = null,
  val analyzedAtMillis: Long = 0L
)

internal data class GestureLandmark(
  val index: Int,
  val x: Float,
  val y: Float,
  val z: Float
)

internal data class GestureDetectionEntry(
  val translation: GestureTranslation,
  val confidencePercent: Int,
  val rawLabel: String,
  val landmarks: List<GestureLandmark> = emptyList(),
  val detectedAtMillis: Long
)

internal val SupportedGestureTranslations = listOf(
  GestureTranslation("Open_Palm", "Open palm", "كف مفتوح", "Palm toward the camera."),
  GestureTranslation("Closed_Fist", "Closed fist", "قبضة مغلقة", "Keep the fist fully visible."),
  GestureTranslation("Pointing_Up", "Pointing up", "إشارة للأعلى", "Raise one finger clearly."),
  GestureTranslation("Thumb_Up", "Thumbs up", "إبهام للأعلى", "Keep the thumb vertical."),
  GestureTranslation("Thumb_Down", "Thumbs down", "إبهام للأسفل", "Rotate the thumb downward."),
  GestureTranslation("Victory", "Victory", "علامة النصر", "Show a clear V shape."),
  GestureTranslation("ILoveYou", "I love you", "أحبك", "Extend thumb, index, and pinky.")
)

private val gestureTranslationsByLabel = SupportedGestureTranslations.associateBy { it.modelLabel }

internal class MediaPipeGestureRecognizer(context: Context) : AutoCloseable {
  private val appContext = context.applicationContext
  private var initializationError: String? = null

  private val gestureRecognizer: GestureRecognizer? = runCatching {
    createRecognizer()
  }.onFailure { exception ->
    initializationError = exception.message ?: "Could not load the gesture recognizer model."
  }.getOrNull()

  private fun createRecognizer(): GestureRecognizer {
    val baseOptions = BaseOptions.builder()
      .setModelAssetPath(GestureModelAsset)
      .build()
    val options = GestureRecognizer.GestureRecognizerOptions.builder()
      .setBaseOptions(baseOptions)
      .setRunningMode(RunningMode.VIDEO)
      .setNumHands(1)
      .setMinHandDetectionConfidence(0.5f)
      .setMinHandPresenceConfidence(0.5f)
      .setMinTrackingConfidence(0.5f)
      .build()
    return GestureRecognizer.createFromOptions(appContext, options)
  }

  fun analyze(imageProxy: ImageProxy, frameTimestampMs: Long): GestureFrameResult {
    val recognizer = gestureRecognizer ?: return GestureFrameResult(
      errorMessage = initializationError ?: "Gesture recognizer is unavailable.",
      analyzedAtMillis = frameTimestampMs
    )
    val bitmap = try {
      imageProxy.toRgbaBitmap()
    } catch (exception: Exception) {
      return GestureFrameResult(
        errorMessage = exception.message ?: "Could not decode the camera frame.",
        analyzedAtMillis = frameTimestampMs
      )
    }

    return try {
      val image = BitmapImageBuilder(bitmap).build()
      val options = ImageProcessingOptions.builder()
        .setRotationDegrees(imageProxy.imageInfo.rotationDegrees)
        .build()
      recognizer.recognizeForVideo(image, options, frameTimestampMs)
        .toGestureFrameResult(frameTimestampMs)
    } catch (exception: Exception) {
      GestureFrameResult(
        errorMessage = exception.message ?: "Gesture recognition failed for the current frame.",
        analyzedAtMillis = frameTimestampMs
      )
    } finally {
      bitmap.recycle()
    }
  }

  override fun close() {
    gestureRecognizer?.close()
  }
}

private fun GestureRecognizerResult.toGestureFrameResult(frameTimestampMs: Long): GestureFrameResult {
  val category = gestures().firstOrNull()?.firstOrNull()
  val rawLabel = category?.resolvedLabel()
    ?.takeIf { it.isNotBlank() && it != "None" }
  val handLandmarks = landmarks()
    .firstOrNull()
    .orEmpty()
    .mapIndexed { index, landmark -> landmark.toGestureLandmark(index) }
  return GestureFrameResult(
    translation = rawLabel?.let(gestureTranslationsByLabel::get),
    confidencePercent = category?.score()?.times(100f)?.roundToInt()?.coerceIn(0, 100) ?: 0,
    handCount = landmarks().size,
    rawLabel = rawLabel,
    landmarks = handLandmarks,
    analyzedAtMillis = frameTimestampMs
  )
}

private fun NormalizedLandmark.toGestureLandmark(index: Int): GestureLandmark {
  return GestureLandmark(
    index = index,
    x = x(),
    y = y(),
    z = z()
  )
}

private fun Category.resolvedLabel(): String {
  val categoryName = categoryName()
  return if (categoryName.isNotBlank()) categoryName else displayName()
}

private fun ImageProxy.toRgbaBitmap(): Bitmap {
  val plane = planes.firstOrNull() ?: error("No image planes are available.")
  val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
  val rowStride = plane.rowStride
  val pixelStride = plane.pixelStride
  val rowBuffer = ByteArray(rowStride)
  val colorBuffer = IntArray(width)
  val byteBuffer = plane.buffer

  byteBuffer.rewind()
  repeat(height) { rowIndex ->
    byteBuffer.position(rowIndex * rowStride)
    byteBuffer.get(rowBuffer, 0, rowStride)
    for (columnIndex in 0 until width) {
      val pixelOffset = columnIndex * pixelStride
      val red = rowBuffer[pixelOffset].toInt() and 0xFF
      val green = rowBuffer[pixelOffset + 1].toInt() and 0xFF
      val blue = rowBuffer[pixelOffset + 2].toInt() and 0xFF
      val alpha = rowBuffer[pixelOffset + 3].toInt() and 0xFF
      colorBuffer[columnIndex] = Color.argb(alpha, red, green, blue)
    }
    bitmap.setPixels(colorBuffer, 0, width, 0, rowIndex, width, 1)
  }

  return bitmap
}
