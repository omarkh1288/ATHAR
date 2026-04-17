package com.athar.accessibilitymapping.data.signlanguage

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Runs the ESL (Egyptian Sign Language) CNN-LSTM TFLite model.
 *
 * Model input:  [1, 30, 46, 3] — 30 frames × 46 landmarks × (x, y, z)
 * Model output: [1, 66]         — probability distribution over 66 ESL signs
 *
 * The 46 landmarks per frame are:
 *   - 21 right-hand landmarks (from MediaPipe HandLandmarker)
 *   - 21 left-hand landmarks  (zeros if only one hand detected)
 *   - 2 right-arm pose points (wrist + elbow, zeros if unavailable)
 *   - 2 left-arm pose points  (wrist + elbow, zeros if unavailable)
 */
class EslLstmClassifier(context: Context) : AutoCloseable {

  companion object {
    private const val TAG = "EslLstmClassifier"
    private const val MODEL_ASSET = "esl_lstm_model.tflite"
    private const val LABELS_ASSET = "esl_labels.json"
    const val FRAMES_PER_SEQUENCE = 30
    const val LANDMARKS_PER_FRAME = 46
    const val COORDS_PER_LANDMARK = 3
    private const val MIN_CONFIDENCE = 0.40f
  }

  private val labels: List<String>
  private val interpreter: Interpreter?
  private var initError: String? = null

  init {
    // Load labels
    labels = try {
      val json = context.assets.open(LABELS_ASSET).bufferedReader().readText()
      // Simple JSON array parse — labels are plain strings
      json.trim().removePrefix("[").removeSuffix("]")
        .split(",")
        .map { it.trim().removeSurrounding("\"") }
        .filter { it.isNotBlank() }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to load ESL labels", e)
      emptyList()
    }

    // Load TFLite model
    interpreter = try {
      val modelBuffer = loadModelFile(context)
      val options = Interpreter.Options().apply {
        numThreads = 2
      }
      Interpreter(modelBuffer, options)
    } catch (e: Exception) {
      initError = e.message
      Log.e(TAG, "Failed to load ESL TFLite model", e)
      null
    }

    Log.d(TAG, "ESL classifier initialized: ${labels.size} labels, " +
      "model=${if (interpreter != null) "loaded" else "FAILED: $initError"}")
  }

  /** Whether the classifier is ready for inference. */
  val isReady: Boolean get() = interpreter != null && labels.isNotEmpty()

  /** Number of sign classes the model can recognize. */
  val classCount: Int get() = labels.size

  /**
   * Run inference on a sequence of landmark frames.
   *
   * @param frames Array of shape [30][46][3] — 30 frames of landmarks.
   * @return A [SignPrediction] with the top label and confidence, or null if
   *         the model is unavailable or confidence is too low.
   */
  fun classify(frames: Array<Array<FloatArray>>, timestampMs: Long): SignPrediction? {
    val model = interpreter ?: return null
    if (frames.size != FRAMES_PER_SEQUENCE) return null

    // Prepare input: [1, 30, 46, 3]
    val inputBuffer = ByteBuffer.allocateDirect(
      1 * FRAMES_PER_SEQUENCE * LANDMARKS_PER_FRAME * COORDS_PER_LANDMARK * 4
    ).apply {
      order(ByteOrder.nativeOrder())
      rewind()
    }

    for (frame in frames) {
      for (landmark in frame) {
        for (coord in landmark) {
          inputBuffer.putFloat(coord)
        }
      }
    }
    inputBuffer.rewind()

    // Prepare output: [1, 66]
    val outputArray = Array(1) { FloatArray(labels.size) }

    try {
      model.run(inputBuffer, outputArray)
    } catch (e: Exception) {
      Log.e(TAG, "Inference failed", e)
      return null
    }

    // Find top prediction
    val probabilities = outputArray[0]
    var maxIdx = 0
    var maxConf = probabilities[0]
    for (i in 1 until probabilities.size) {
      if (probabilities[i] > maxConf) {
        maxConf = probabilities[i]
        maxIdx = i
      }
    }

    if (maxConf < MIN_CONFIDENCE) {
      Log.d(TAG, "Top prediction '${labels.getOrNull(maxIdx)}' confidence " +
        "${(maxConf * 100).toInt()}% below threshold")
      return null
    }

    val label = labels.getOrElse(maxIdx) { "unknown" }
    Log.d(TAG, "Predicted: '$label' (${(maxConf * 100).toInt()}%)")

    return SignPrediction(
      label = label,
      confidence = maxConf,
      timestampMs = timestampMs,
      handCount = 1
    )
  }

  private fun loadModelFile(context: Context): MappedByteBuffer {
    val fileDescriptor = context.assets.openFd(MODEL_ASSET)
    val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
    val fileChannel = inputStream.channel
    val startOffset = fileDescriptor.startOffset
    val declaredLength = fileDescriptor.declaredLength
    return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
  }

  override fun close() {
    interpreter?.close()
  }
}
