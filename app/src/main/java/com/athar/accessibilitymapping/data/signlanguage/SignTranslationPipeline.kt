package com.athar.accessibilitymapping.data.signlanguage

import android.content.Context
import android.util.Log

/**
 * Top-level orchestrator that connects the full translation pipeline:
 *
 *   Camera frame → [SignPrediction]
 *     → [TemporalPredictionBuffer] (smoothing, stability)
 *       → [SignSequenceProcessor] (dedup, boundary detection)
 *         → [SignToTextTranslator] (phrase matching, Arabic output)
 *           → [TranslationResult]
 *
 *   Camera frame landmarks → [LandmarkFrameBuffer] (collect 30 frames)
 *     → [EslLstmClassifier] (66 Egyptian Sign Language signs)
 *       → merged into the same pipeline above
 *
 * Usage from the UI:
 * 1. Call [feedFrame] for each recognition result.
 * 2. Call [feedLandmarks] for each frame's hand landmarks.
 * 3. Read [currentTranslation] for the latest translated text.
 * 4. Optionally call [speakCurrentTranslation] to read it aloud.
 */
class SignTranslationPipeline(
  context: Context? = null,
  debugMode: Boolean = false
) : AutoCloseable {
  companion object {
    private const val TAG = "SignTranslationPipeline"
  }

  val lexicon = EgyptianSignLexiconRepository(context)

  private val temporalBuffer = TemporalPredictionBuffer(
    requiredStableFrames = 3,
    defaultConfidenceThreshold = 0.70f,
    lexicon = lexicon
  )

  private val sequenceProcessor = SignSequenceProcessor(
    minRepeatIntervalMs = 1500L,
    pauseGapFrames = 8
  )

  private val translator = SignToTextTranslator(
    lexicon = lexicon,
    debugMode = debugMode
  )

  val speaker: ArabicTextSpeaker? = if (context != null) ArabicTextSpeaker(context) else null

  // ── ESL LSTM classifier ─────────────────────────────────────────────
  private val eslClassifier: EslLstmClassifier? =
    if (context != null) {
      try { EslLstmClassifier(context) } catch (e: Exception) {
        Log.e(TAG, "ESL classifier init failed", e)
        null
      }
    } else null

  private val landmarkBuffer = LandmarkFrameBuffer()

  /** Whether the ESL LSTM model is loaded and ready. */
  val eslModelReady: Boolean get() = eslClassifier?.isReady == true
  /** Number of ESL classes in the loaded TFLite model. */
  val eslClassCount: Int get() = eslClassifier?.classCount ?: 0

  private var _currentTranslation = TranslationResult("", "", emptyList(), isPartial = true)

  /** The latest translation result. Read this from the UI. */
  val currentTranslation: TranslationResult get() = _currentTranslation

  /** Whether the pipeline detected a sentence boundary (pause). */
  val isUtteranceComplete: Boolean get() = sequenceProcessor.isUtteranceComplete

  /** The committed sign sequence so far. */
  val currentSequence: List<CommittedSign> get() = sequenceProcessor.currentSequence

  /** Debug: raw predictions in the temporal buffer. */
  val rawPredictions: List<SignPrediction> get() = temporalBuffer.rawPredictions

  /** Debug: current pending label in the temporal buffer. */
  val pendingLabel: String? get() = temporalBuffer.pendingLabel

  /** Debug: frame count for the pending label. */
  val pendingFrameCount: Int get() = temporalBuffer.pendingFrameCount

  /**
   * Feed a single frame's recognition result into the pipeline.
   *
   * @param label The gesture label from MediaPipe (e.g. "Thumb_Up"), or blank if none.
   * @param confidence The confidence score (0.0–1.0).
   * @param timestampMs Frame timestamp in milliseconds.
   * @param handCount Number of hands detected (0 = gap).
   * @return True if a new sign was committed and translation updated.
   */
  fun feedFrame(
    label: String,
    confidence: Float,
    timestampMs: Long,
    handCount: Int
  ): Boolean {
    // Step 1: Feed into temporal buffer
    val prediction = SignPrediction(
      label = label,
      confidence = confidence,
      timestampMs = timestampMs,
      handCount = handCount
    )
    val committedSign = temporalBuffer.feed(prediction)

    // Step 2: Check for gap → pause detection
    sequenceProcessor.onGapUpdate(temporalBuffer.currentGapFrames)

    // Step 3: If a sign was committed, feed into sequence processor
    var signAccepted = false
    if (committedSign != null) {
      signAccepted = sequenceProcessor.onSignCommitted(committedSign)
    }

    // Step 4: Translate whenever sequence changes or utterance completes
    if (signAccepted || sequenceProcessor.isUtteranceComplete) {
      _currentTranslation = translator.translate(
        signs = sequenceProcessor.currentSequence,
        isPartial = !sequenceProcessor.isUtteranceComplete,
        isUtteranceComplete = sequenceProcessor.isUtteranceComplete
      )
      if (sequenceProcessor.isUtteranceComplete) {
        sequenceProcessor.consumeUtterance()
      }
    }

    return signAccepted
  }

  /**
   * Feed hand landmarks from the current frame into the ESL LSTM buffer.
   *
   * When enough frames accumulate (30), the LSTM model runs inference and
   * the result is fed into the same temporal buffer as gesture predictions.
   *
   * @param rightHandLandmarks 21 landmarks for the right hand, each [x, y, z]. Null if no right hand.
   * @param leftHandLandmarks 21 landmarks for the left hand, each [x, y, z]. Null if no left hand.
   * @param timestampMs Frame timestamp in milliseconds.
   * @param handCount Number of hands detected.
   * @return True if the ESL model produced a new prediction that was committed.
   */
  fun feedLandmarks(
    rightHandLandmarks: List<FloatArray>?,
    leftHandLandmarks: List<FloatArray>?,
    timestampMs: Long,
    handCount: Int
  ): Boolean {
    if (eslClassifier == null || !eslClassifier.isReady) return false

    val hasHand = handCount > 0
    val frame = if (hasHand) {
      LandmarkFrameBuffer.buildFrame(rightHandLandmarks, leftHandLandmarks)
    } else {
      LandmarkFrameBuffer.emptyFrame()
    }
    landmarkBuffer.addFrame(frame, hasHand)

    // Only run inference when buffer is ready and enough hand frames
    if (!landmarkBuffer.isReady) return false
    if (landmarkBuffer.handFrameRatio < 0.3f) return false // Too few hand frames

    val sequence = landmarkBuffer.getSequence() ?: return false
    val eslPrediction = eslClassifier.classify(sequence, timestampMs) ?: return false

    // Slide the window for next inference
    landmarkBuffer.consumeAndSlide()

    // Feed the ESL prediction into the same temporal buffer
    val committedSign = temporalBuffer.feed(eslPrediction)

    sequenceProcessor.onGapUpdate(temporalBuffer.currentGapFrames)

    var signAccepted = false
    if (committedSign != null) {
      signAccepted = sequenceProcessor.onSignCommitted(committedSign)
    }

    if (signAccepted || sequenceProcessor.isUtteranceComplete) {
      _currentTranslation = translator.translate(
        signs = sequenceProcessor.currentSequence,
        isPartial = !sequenceProcessor.isUtteranceComplete,
        isUtteranceComplete = sequenceProcessor.isUtteranceComplete
      )
      if (sequenceProcessor.isUtteranceComplete) {
        sequenceProcessor.consumeUtterance()
      }
    }

    return signAccepted
  }

  /** Speak the current translation aloud. */
  fun speakCurrentTranslation() {
    speaker?.speakResult(_currentTranslation)
  }

  /** Clear all state and start fresh. */
  fun clear() {
    temporalBuffer.clear()
    sequenceProcessor.clear()
    landmarkBuffer.clear()
    _currentTranslation = TranslationResult("", "", emptyList(), isPartial = true)
    speaker?.stop()
  }

  /** Start a new utterance but keep the pipeline running. */
  fun startNewSentence() {
    sequenceProcessor.startNewUtterance()
    _currentTranslation = TranslationResult("", "", emptyList(), isPartial = true)
  }

  override fun close() {
    speaker?.close()
    eslClassifier?.close()
  }
}
