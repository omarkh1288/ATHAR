package com.athar.accessibilitymapping.data.signlanguage

/**
 * A single per-frame prediction produced by the gesture recognizer.
 * This is the raw input into the translation pipeline.
 */
data class SignPrediction(
  val label: String,
  val confidence: Float,
  val timestampMs: Long,
  val handCount: Int = 1
) {
  /** Normalized label: trimmed, lowercased, underscores replaced with underscores (consistent). */
  val normalizedLabel: String get() = normalizeLabel(label)

  companion object {
    fun normalizeLabel(raw: String): String {
      return raw.trim().lowercase().replace(' ', '_')
    }
  }
}

/**
 * A stabilized sign that survived temporal smoothing and was committed
 * as a real detection (not noise).
 */
data class CommittedSign(
  val label: String,
  val normalizedLabel: String,
  val confidence: Float,
  val startTimestampMs: Long,
  val endTimestampMs: Long,
  val frameCount: Int
)

/**
 * Final translation output for the UI.
 */
data class TranslationResult(
  val arabicText: String,
  val englishText: String,
  val signSequence: List<CommittedSign>,
  val isPartial: Boolean,
  val debugInfo: TranslationDebugInfo? = null
)

/**
 * Debug info for development and troubleshooting.
 */
data class TranslationDebugInfo(
  val rawPredictions: List<String>,
  val stabilizedLabels: List<String>,
  val phraseMatches: List<String>,
  val unknownLabels: List<String>
)
