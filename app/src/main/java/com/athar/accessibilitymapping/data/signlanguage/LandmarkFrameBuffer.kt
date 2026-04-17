package com.athar.accessibilitymapping.data.signlanguage

/**
 * Collects 30 consecutive frames of hand landmarks for the ESL LSTM model.
 *
 * Each frame consists of 46 landmarks × 3 coordinates (x, y, z):
 *   - Indices  0–20: right-hand landmarks (21 points from MediaPipe)
 *   - Indices 21–41: left-hand landmarks  (zeros if only one hand)
 *   - Indices 42–43: right arm (wrist, elbow) — zeros if unavailable
 *   - Indices 44–45: left arm (wrist, elbow)  — zeros if unavailable
 *
 * Call [addFrame] for every camera frame. When 30 frames have accumulated,
 * [isReady] becomes true and [getSequence] returns the buffered data.
 *
 * After consuming the sequence, call [consumeAndSlide] to remove the oldest
 * frames and keep a sliding window (overlap) for smoother transitions.
 */
class LandmarkFrameBuffer(
  private val sequenceLength: Int = EslLstmClassifier.FRAMES_PER_SEQUENCE,
  private val slideAmount: Int = 5
) {
  private val buffer = ArrayDeque<Array<FloatArray>>(sequenceLength + slideAmount)
  private var handDetectedCount = 0

  /** Whether we have enough frames to run inference. */
  val isReady: Boolean get() = buffer.size >= sequenceLength

  /** Number of frames currently buffered. */
  val frameCount: Int get() = buffer.size

  /** How many of the buffered frames had a hand detected. */
  val handFrameRatio: Float
    get() = if (buffer.isEmpty()) 0f else handDetectedCount.toFloat() / buffer.size

  /**
   * Add one frame of landmarks.
   *
   * @param landmarks Array of 46 entries, each being [x, y, z]. If hand is not
   *                  detected, pass an array of 46 zero-vectors.
   * @param hasHand Whether a hand was detected in this frame.
   */
  fun addFrame(landmarks: Array<FloatArray>, hasHand: Boolean) {
    if (landmarks.size != EslLstmClassifier.LANDMARKS_PER_FRAME) return

    buffer.addLast(landmarks)
    if (hasHand) handDetectedCount++

    // Keep at most sequenceLength + slideAmount to avoid unbounded growth
    while (buffer.size > sequenceLength + slideAmount) {
      buffer.removeFirst()
      // handDetectedCount is approximate after trimming — acceptable
    }
  }

  /**
   * Get the current sequence of [sequenceLength] frames for inference.
   * Returns null if not enough frames are buffered.
   */
  fun getSequence(): Array<Array<FloatArray>>? {
    if (!isReady) return null
    val start = buffer.size - sequenceLength
    return Array(sequenceLength) { i -> buffer.elementAt(start + i) }
  }

  /**
   * After consuming a sequence, slide the window forward by removing
   * the oldest [slideAmount] frames. This allows overlapping sequences
   * for smoother continuous recognition.
   */
  fun consumeAndSlide() {
    val toRemove = slideAmount.coerceAtMost(buffer.size)
    repeat(toRemove) {
      buffer.removeFirst()
    }
    // Recalculate hand count (approximate is fine)
    handDetectedCount = buffer.count { frame ->
      frame.any { lm -> lm[0] != 0f || lm[1] != 0f || lm[2] != 0f }
    }
  }

  /** Clear all buffered frames. */
  fun clear() {
    buffer.clear()
    handDetectedCount = 0
  }

  companion object {
    /**
     * Create a zero-filled landmark frame (no hand detected).
     */
    fun emptyFrame(): Array<FloatArray> =
      Array(EslLstmClassifier.LANDMARKS_PER_FRAME) { FloatArray(EslLstmClassifier.COORDS_PER_LANDMARK) }

    /**
     * Build a landmark frame from MediaPipe hand landmarks.
     *
     * @param rightHand 21 landmarks of the right hand, each [x, y, z]. Null if no right hand.
     * @param leftHand 21 landmarks of the left hand, each [x, y, z]. Null if no left hand.
     */
    fun buildFrame(
      rightHand: List<FloatArray>? = null,
      leftHand: List<FloatArray>? = null
    ): Array<FloatArray> {
      val frame = emptyFrame()
      // Right hand: indices 0–20
      rightHand?.forEachIndexed { i, coords ->
        if (i < 21 && coords.size >= 3) {
          frame[i] = floatArrayOf(coords[0], coords[1], coords[2])
        }
      }
      // Left hand: indices 21–41
      leftHand?.forEachIndexed { i, coords ->
        if (i < 21 && coords.size >= 3) {
          frame[21 + i] = floatArrayOf(coords[0], coords[1], coords[2])
        }
      }
      // Arm pose points (42–45): derive from hand landmarks if available
      // Index 42 = right wrist (same as right hand landmark 0)
      if (rightHand != null && rightHand.isNotEmpty()) {
        frame[42] = frame[0].copyOf()
      }
      // Index 44 = left wrist (same as left hand landmark 0)
      if (leftHand != null && leftHand.isNotEmpty()) {
        frame[44] = frame[21].copyOf()
      }
      // Elbow points (43, 45) are not available from hand landmarks — left as zeros
      return frame
    }
  }
}
