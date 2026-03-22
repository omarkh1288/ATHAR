package com.athar.backend

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class EgyptianSignInterpreterTest {
  private val interpreter = EgyptianSignInterpreter()

  @Test
  fun interpretUsesSequenceRuleWhenRecentSignsMatch() {
    val result = interpreter.interpret(
      InterpretEgyptianSignRequest(
        sessionId = "session-1",
        observations = listOf(
          SignObservationDto(timestampMs = 1_000, gestureLabel = "Pointing_Up", confidencePercent = 82),
          SignObservationDto(timestampMs = 2_000, gestureLabel = "Open_Palm", confidencePercent = 88)
        )
      )
    )

    val response = assertIs<ServiceResult.Success<InterpretEgyptianSignResponse>>(result).value
    assertEquals("rule_sequence_v1", response.mode)
    assertEquals("أحتاج مساعدة من فضلك.", response.arabicSentence)
    assertEquals("I need help, please.", response.englishSentence)
  }

  @Test
  fun interpretFallsBackToLatestSupportedSignMeaning() {
    val result = interpreter.interpret(
      InterpretEgyptianSignRequest(
        sessionId = "session-2",
        observations = listOf(
          SignObservationDto(
            timestampMs = 3_000,
            gestureLabel = "Victory",
            localEnglish = "Victory",
            localArabic = "علامة النصر",
            confidencePercent = 91,
            landmarks = List(21) { index ->
              SignLandmarkDto(index = index, x = 0.1f, y = 0.2f, z = 0.3f)
            }
          )
        )
      )
    )

    val response = assertIs<ServiceResult.Success<InterpretEgyptianSignResponse>>(result).value
    assertEquals("rule_fallback_v1", response.mode)
    assertEquals("علامة النصر", response.arabicSentence)
    assertEquals("Victory", response.englishSentence)
    assertTrue(response.notes.any { it.contains("landmarks") })
  }
}
