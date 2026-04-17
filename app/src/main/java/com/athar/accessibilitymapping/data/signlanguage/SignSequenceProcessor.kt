package com.athar.accessibilitymapping.data.signlanguage

import android.util.Log

/**
 * Builds a sequence of committed signs, merging repeated consecutive labels
 * and detecting sign boundaries (pauses between signs).
 *
 * This sits between [TemporalPredictionBuffer] (which commits individual signs)
 * and [SignToTextTranslator] (which turns sign sequences into sentences).
 *
 * Responsibilities:
 * - Suppress repeated consecutive same-label commits (debounce)
 * - Detect pauses / end-of-utterance when a gap exceeds a threshold
 * - Maintain the ordered sequence of distinct committed signs
 * - Signal when a new sign is added or when an utterance ends
 */
class SignSequenceProcessor(
  private val minRepeatIntervalMs: Long = 1500L,
  private val pauseGapFrames: Int = 8,
  private val maxSequenceLength: Int = 50
) {
  companion object {
    private const val TAG = "SignSequenceProc"
  }

  private val sequence = mutableListOf<CommittedSign>()
  private var lastCommittedLabel: String? = null
  private var lastCommittedTimestamp: Long = 0L
  private var utteranceEndPending = false

  /** The current sequence of distinct committed signs. */
  val currentSequence: List<CommittedSign> get() = sequence.toList()

  /** Just the normalized labels in order. */
  val currentLabels: List<String> get() = sequence.map { it.normalizedLabel }

  /** Whether the processor detected a pause (end-of-utterance). */
  val isUtteranceComplete: Boolean get() = utteranceEndPending

  /**
   * Process a newly committed sign from the temporal buffer.
   * Returns true if this sign was accepted (not suppressed as a repeat).
   */
  fun onSignCommitted(sign: CommittedSign): Boolean {
    // Suppress immediate repeat of the same label
    if (sign.normalizedLabel == lastCommittedLabel &&
      sign.startTimestampMs - lastCommittedTimestamp < minRepeatIntervalMs
    ) {
      Log.d(TAG, "Suppressed repeat: '${sign.normalizedLabel}' " +
        "(${sign.startTimestampMs - lastCommittedTimestamp}ms since last)")
      return false
    }

    // Accept the sign
    utteranceEndPending = false
    if (sequence.size >= maxSequenceLength) sequence.removeAt(0)
    sequence.add(sign)
    lastCommittedLabel = sign.normalizedLabel
    lastCommittedTimestamp = sign.endTimestampMs
    Log.d(TAG, "Accepted sign: '${sign.normalizedLabel}' → sequence size=${sequence.size}")
    return true
  }

  /**
   * Called by the pipeline when the temporal buffer reports a gap.
   * If the gap exceeds [pauseGapFrames], signals end-of-utterance.
   */
  fun onGapUpdate(gapFrameCount: Int) {
    if (gapFrameCount >= pauseGapFrames && sequence.isNotEmpty() && !utteranceEndPending) {
      utteranceEndPending = true
      Log.d(TAG, "End-of-utterance detected after $gapFrameCount gap frames.")
    }
  }

  /** Mark the utterance as consumed (after translation reads it). */
  fun consumeUtterance() {
    utteranceEndPending = false
  }

  /** Start a new sentence (clear the sequence). */
  fun startNewUtterance() {
    sequence.clear()
    lastCommittedLabel = null
    lastCommittedTimestamp = 0L
    utteranceEndPending = false
  }

  fun clear() {
    startNewUtterance()
  }
}
