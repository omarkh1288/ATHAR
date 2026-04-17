package com.athar.accessibilitymapping.data.signlanguage

import android.util.Log

/**
 * Smooths noisy per-frame predictions across time.
 *
 * Each frame produces a [SignPrediction]. This buffer collects them, applies
 * confidence filtering, counts consecutive stable frames for the same label,
 * and emits a [CommittedSign] only when a label has been stable for
 * [requiredStableFrames] consecutive frames above the confidence threshold.
 *
 * It also detects when no hand is present (gap frames) to signal potential
 * sign boundaries.
 */
class TemporalPredictionBuffer(
  private val requiredStableFrames: Int = 3,
  private val defaultConfidenceThreshold: Float = 0.70f,
  private val maxBufferSize: Int = 60,
  private val lexicon: EgyptianSignLexiconRepository? = null
) {
  companion object {
    private const val TAG = "TemporalBuffer"
  }

  private val rawBuffer = ArrayDeque<SignPrediction>(maxBufferSize)
  private var currentLabel: String? = null
  private var currentRunStart: Long = 0L
  private var consecutiveCount: Int = 0
  private var peakConfidence: Float = 0f
  private var gapFrameCount: Int = 0

  /** Number of consecutive frames with no hand / no prediction. */
  val currentGapFrames: Int get() = gapFrameCount

  /** Whether we are currently in a gap (no hand visible). */
  val isInGap: Boolean get() = gapFrameCount > 0

  /** The label currently being tracked for stability. */
  val pendingLabel: String? get() = currentLabel

  /** How many consecutive stable frames for the current label. */
  val pendingFrameCount: Int get() = consecutiveCount

  /** All raw predictions in the buffer (for debug display). */
  val rawPredictions: List<SignPrediction> get() = rawBuffer.toList()

  /**
   * Feed a new frame prediction. Returns a [CommittedSign] if the prediction
   * has been stable long enough, or null otherwise.
   */
  fun feed(prediction: SignPrediction): CommittedSign? {
    // Maintain buffer size
    if (rawBuffer.size >= maxBufferSize) rawBuffer.removeFirst()
    rawBuffer.addLast(prediction)

    // No hand detected → increment gap counter, reset run
    if (prediction.handCount == 0 || prediction.label.isBlank()) {
      gapFrameCount++
      resetRun()
      return null
    }

    gapFrameCount = 0
    val normalized = prediction.normalizedLabel
    val threshold = lexicon?.confidenceThreshold(prediction.label, defaultConfidenceThreshold)
      ?: defaultConfidenceThreshold

    // Below confidence threshold → noise, reset run
    if (prediction.confidence < threshold) {
      resetRun()
      return null
    }

    // Same label continues the run
    if (normalized == currentLabel) {
      consecutiveCount++
      if (prediction.confidence > peakConfidence) peakConfidence = prediction.confidence
    } else {
      // New label → start fresh run
      currentLabel = normalized
      currentRunStart = prediction.timestampMs
      consecutiveCount = 1
      peakConfidence = prediction.confidence
    }

    // Commit if stable for enough frames
    if (consecutiveCount >= requiredStableFrames) {
      val committed = CommittedSign(
        label = prediction.label,
        normalizedLabel = normalized,
        confidence = peakConfidence,
        startTimestampMs = currentRunStart,
        endTimestampMs = prediction.timestampMs,
        frameCount = consecutiveCount
      )
      Log.d(TAG, "Committed sign: '$normalized' (${consecutiveCount} frames, ${(peakConfidence * 100).toInt()}%)")
      resetRun()
      return committed
    }

    return null
  }

  /**
   * Feed a gap frame (no prediction / no hand). Returns null.
   */
  fun feedGap(timestampMs: Long) {
    gapFrameCount++
    resetRun()
  }

  fun clear() {
    rawBuffer.clear()
    resetRun()
    gapFrameCount = 0
  }

  private fun resetRun() {
    currentLabel = null
    consecutiveCount = 0
    peakConfidence = 0f
    currentRunStart = 0L
  }
}
