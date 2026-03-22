package com.athar.backend

import io.ktor.http.HttpStatusCode
import kotlin.math.roundToInt

internal class EgyptianSignInterpreter {
  private data class PhraseRule(
    val labels: List<String>,
    val arabic: String,
    val english: String
  )

  private val sequenceRules = listOf(
    PhraseRule(
      labels = listOf("Pointing_Up", "Open_Palm"),
      arabic = "أحتاج مساعدة من فضلك.",
      english = "I need help, please."
    ),
    PhraseRule(
      labels = listOf("Closed_Fist", "Open_Palm"),
      arabic = "توقف من فضلك.",
      english = "Please stop."
    ),
    PhraseRule(
      labels = listOf("Thumb_Up", "Victory"),
      arabic = "نعم، شكرا.",
      english = "Yes, thank you."
    ),
    PhraseRule(
      labels = listOf("Thumb_Down", "Open_Palm"),
      arabic = "لا، من فضلك انتظر.",
      english = "No, please wait."
    ),
    PhraseRule(
      labels = listOf("Pointing_Up", "Thumb_Up"),
      arabic = "نعم، أحتاج مساعدة.",
      english = "Yes, I need help."
    ),
    PhraseRule(
      labels = listOf("Pointing_Up", "Victory"),
      arabic = "أحتاج المساعدة، شكرا.",
      english = "I need help, thank you."
    )
  )

  fun interpret(request: InterpretEgyptianSignRequest): ServiceResult<InterpretEgyptianSignResponse> {
    val observations = request.observations
      .filter { it.confidencePercent > 0 }
      .takeLast(12)
    if (observations.isEmpty()) {
      return ServiceResult.Failure(
        HttpStatusCode.BadRequest,
        "At least one live sign observation is required."
      )
    }

    val labels = observations.mapNotNull { observation ->
      observation.gestureLabel
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
    }
    val matchedRule = sequenceRules.firstOrNull { labels.endsWithSequence(it.labels) }
    val latest = observations.last()
    val confidencePercent = observations
      .map { it.confidencePercent.coerceIn(0, 100) }
      .average()
      .roundToInt()
      .coerceIn(0, 100)

    val fallbackArabic = latest.localArabic
      ?.trim()
      ?.takeIf { it.isNotEmpty() }
      ?: defaultArabicFor(latest.gestureLabel)
      ?: "تعذر فهم الإشارة الحالية."
    val fallbackEnglish = latest.localEnglish
      ?.trim()
      ?.takeIf { it.isNotEmpty() }
      ?: defaultEnglishFor(latest.gestureLabel)
      ?: "The current sign could not be interpreted."

    val notes = mutableListOf(
      "Backend interpretation is currently rule-based scaffolding. Replace it with a trained Egyptian Sign Language sequence model for free-form sentences."
    )
    if (latest.landmarks.isNotEmpty()) {
      notes += "Received ${latest.landmarks.size} landmarks from the latest observation."
    }
    if (matchedRule == null) {
      notes += "No multi-sign phrase match yet, so the response falls back to the latest supported sign meaning."
    }

    return ServiceResult.Success(
      InterpretEgyptianSignResponse(
        sessionId = request.sessionId,
        mode = if (matchedRule != null) "rule_sequence_v1" else "rule_fallback_v1",
        arabicSentence = matchedRule?.arabic ?: fallbackArabic,
        englishSentence = matchedRule?.english ?: fallbackEnglish,
        confidencePercent = confidencePercent,
        dominantGestureLabel = latest.gestureLabel,
        notes = notes
      )
    )
  }

  private fun defaultArabicFor(label: String?): String? {
    return when (label) {
      "Open_Palm" -> "من فضلك انتظر."
      "Closed_Fist" -> "توقف."
      "Pointing_Up" -> "أحتاج مساعدة."
      "Thumb_Up" -> "نعم."
      "Thumb_Down" -> "لا."
      "Victory" -> "شكرا."
      "ILoveYou" -> "أحبك."
      else -> null
    }
  }

  private fun defaultEnglishFor(label: String?): String? {
    return when (label) {
      "Open_Palm" -> "Please wait."
      "Closed_Fist" -> "Stop."
      "Pointing_Up" -> "I need help."
      "Thumb_Up" -> "Yes."
      "Thumb_Down" -> "No."
      "Victory" -> "Thank you."
      "ILoveYou" -> "I love you."
      else -> null
    }
  }
}

private fun List<String>.endsWithSequence(other: List<String>): Boolean {
  if (other.isEmpty() || size < other.size) {
    return false
  }
  return takeLast(other.size) == other
}
