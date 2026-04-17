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
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
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
  /** Raw right-hand landmark coordinates for ESL LSTM model: List of [x, y, z]. */
  val rightHandCoords: List<FloatArray>? = null,
  /** Raw left-hand landmark coordinates for ESL LSTM model: List of [x, y, z]. */
  val leftHandCoords: List<FloatArray>? = null,
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

// ── HandLandmarker for ESL LSTM (tracks 2 hands) ─────────────────────

private const val HandLandmarkerAsset = "hand_landmarker.task"

/**
 * Extracts hand landmarks (up to 2 hands) from camera frames.
 * Used alongside [MediaPipeGestureRecognizer] to feed the ESL LSTM model.
 */
internal class MediaPipeHandLandmarker(context: Context) : AutoCloseable {
  private val appContext = context.applicationContext
  private var initError: String? = null

  private val handLandmarker: HandLandmarker? = runCatching {
    val baseOptions = BaseOptions.builder()
      .setModelAssetPath(HandLandmarkerAsset)
      .build()
    val options = HandLandmarker.HandLandmarkerOptions.builder()
      .setBaseOptions(baseOptions)
      .setRunningMode(RunningMode.VIDEO)
      .setNumHands(2)
      .setMinHandDetectionConfidence(0.5f)
      .setMinHandPresenceConfidence(0.5f)
      .setMinTrackingConfidence(0.5f)
      .build()
    HandLandmarker.createFromOptions(appContext, options)
  }.onFailure { e ->
    initError = e.message
  }.getOrNull()

  val isReady: Boolean get() = handLandmarker != null

  /**
   * Extract hand landmarks from an image frame.
   *
   * @return Pair of (rightHandCoords, leftHandCoords), each a list of 21 [x,y,z] arrays.
   *         Null if that hand is not detected.
   */
  fun extractLandmarks(
    imageProxy: ImageProxy,
    frameTimestampMs: Long
  ): HandLandmarkResult {
    val landmarker = handLandmarker ?: return HandLandmarkResult()
    val bitmap = try {
      imageProxy.toRgbaBitmap()
    } catch (e: Exception) {
      return HandLandmarkResult()
    }

    return try {
      val image = BitmapImageBuilder(bitmap).build()
      val options = ImageProcessingOptions.builder()
        .setRotationDegrees(imageProxy.imageInfo.rotationDegrees)
        .build()
      val result = landmarker.detectForVideo(image, options, frameTimestampMs)
      result.toHandLandmarkResult()
    } catch (_: Exception) {
      HandLandmarkResult()
    } finally {
      bitmap.recycle()
    }
  }

  override fun close() {
    handLandmarker?.close()
  }
}

internal data class HandLandmarkResult(
  val rightHandCoords: List<FloatArray>? = null,
  val leftHandCoords: List<FloatArray>? = null,
  val handCount: Int = 0
)

private fun HandLandmarkerResult.toHandLandmarkResult(): HandLandmarkResult {
  val handLandmarksList = landmarks()
  if (handLandmarksList.isEmpty()) return HandLandmarkResult()

  // MediaPipe returns hands in detection order; use handedness to determine left/right
  val handednessList = handednesses()
  var rightHand: List<FloatArray>? = null
  var leftHand: List<FloatArray>? = null

  for (i in handLandmarksList.indices) {
    val coords = handLandmarksList[i].map { lm ->
      floatArrayOf(lm.x(), lm.y(), lm.z())
    }
    val label = handednessList.getOrNull(i)?.firstOrNull()?.categoryName() ?: ""
    // Note: "Left" in MediaPipe means the camera sees it on the left side,
    // which is actually the signer's RIGHT hand (mirror). So we swap.
    if (label.equals("Left", ignoreCase = true)) {
      rightHand = coords
    } else {
      leftHand = coords
    }
  }

  // If only one hand and we couldn't determine handedness, use as right
  if (rightHand == null && leftHand == null && handLandmarksList.isNotEmpty()) {
    rightHand = handLandmarksList[0].map { lm ->
      floatArrayOf(lm.x(), lm.y(), lm.z())
    }
  }

  return HandLandmarkResult(
    rightHandCoords = rightHand,
    leftHandCoords = leftHand,
    handCount = handLandmarksList.size
  )
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
