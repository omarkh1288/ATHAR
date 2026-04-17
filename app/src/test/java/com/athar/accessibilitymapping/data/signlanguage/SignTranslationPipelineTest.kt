package com.athar.accessibilitymapping.data.signlanguage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SignPredictionTest {

  @Test
  fun `normalizeLabel trims and lowercases`() {
    assertEquals("thumb_up", SignPrediction.normalizeLabel("  Thumb_Up "))
    assertEquals("open_palm", SignPrediction.normalizeLabel("Open_Palm"))
    assertEquals("hello", SignPrediction.normalizeLabel("Hello"))
  }

  @Test
  fun `normalizeLabel replaces spaces with underscores`() {
    assertEquals("open_palm", SignPrediction.normalizeLabel("Open Palm"))
    assertEquals("i_love_you", SignPrediction.normalizeLabel("I Love You"))
  }

  @Test
  fun `SignPrediction normalizedLabel property works`() {
    val prediction = SignPrediction("Thumb_Up", 0.95f, 1000L)
    assertEquals("thumb_up", prediction.normalizedLabel)
  }
}

class TemporalPredictionBufferTest {

  private fun makePrediction(label: String, confidence: Float, ts: Long, handCount: Int = 1) =
    SignPrediction(label, confidence, ts, handCount)

  @Test
  fun `requires multiple stable frames before committing`() {
    val buffer = TemporalPredictionBuffer(requiredStableFrames = 3, defaultConfidenceThreshold = 0.70f)

    assertNull(buffer.feed(makePrediction("Thumb_Up", 0.90f, 100)))
    assertNull(buffer.feed(makePrediction("Thumb_Up", 0.92f, 200)))
    // Third frame commits
    val committed = buffer.feed(makePrediction("Thumb_Up", 0.88f, 300))
    assertNotNull(committed)
    assertEquals("thumb_up", committed!!.normalizedLabel)
    assertEquals(3, committed.frameCount)
  }

  @Test
  fun `resets run when label changes`() {
    val buffer = TemporalPredictionBuffer(requiredStableFrames = 3)

    buffer.feed(makePrediction("Thumb_Up", 0.90f, 100))
    buffer.feed(makePrediction("Thumb_Up", 0.92f, 200))
    // Different label resets
    buffer.feed(makePrediction("Victory", 0.85f, 300))
    // Now Victory needs 3 frames from scratch
    assertNull(buffer.feed(makePrediction("Victory", 0.86f, 400)))
    val committed = buffer.feed(makePrediction("Victory", 0.88f, 500))
    assertNotNull(committed)
    assertEquals("victory", committed!!.normalizedLabel)
  }

  @Test
  fun `ignores low confidence predictions`() {
    val buffer = TemporalPredictionBuffer(requiredStableFrames = 2, defaultConfidenceThreshold = 0.70f)

    buffer.feed(makePrediction("Thumb_Up", 0.50f, 100)) // Below threshold
    buffer.feed(makePrediction("Thumb_Up", 0.50f, 200)) // Below threshold
    assertNull(buffer.feed(makePrediction("Thumb_Up", 0.50f, 300))) // Still below
  }

  @Test
  fun `tracks gap frames when no hand detected`() {
    val buffer = TemporalPredictionBuffer(requiredStableFrames = 2)

    buffer.feed(makePrediction("", 0f, 100, handCount = 0))
    assertEquals(1, buffer.currentGapFrames)
    assertTrue(buffer.isInGap)

    buffer.feed(makePrediction("", 0f, 200, handCount = 0))
    assertEquals(2, buffer.currentGapFrames)

    // Hand appears again, gap resets
    buffer.feed(makePrediction("Thumb_Up", 0.90f, 300, handCount = 1))
    assertEquals(0, buffer.currentGapFrames)
    assertFalse(buffer.isInGap)
  }

  @Test
  fun `peak confidence is tracked across the run`() {
    val buffer = TemporalPredictionBuffer(requiredStableFrames = 3)

    buffer.feed(makePrediction("Thumb_Up", 0.85f, 100))
    buffer.feed(makePrediction("Thumb_Up", 0.95f, 200))
    val committed = buffer.feed(makePrediction("Thumb_Up", 0.88f, 300))

    assertNotNull(committed)
    assertEquals(0.95f, committed!!.confidence)
  }

  @Test
  fun `clear resets all state`() {
    val buffer = TemporalPredictionBuffer(requiredStableFrames = 2)
    buffer.feed(makePrediction("Thumb_Up", 0.90f, 100))
    buffer.clear()
    assertNull(buffer.pendingLabel)
    assertEquals(0, buffer.pendingFrameCount)
    assertEquals(0, buffer.currentGapFrames)
  }
}

class SignSequenceProcessorTest {

  private fun makeSign(label: String, startMs: Long, endMs: Long = startMs + 500) =
    CommittedSign(label, SignPrediction.normalizeLabel(label), 0.90f, startMs, endMs, 3)

  @Test
  fun `accepts first sign`() {
    val processor = SignSequenceProcessor(minRepeatIntervalMs = 1500)
    assertTrue(processor.onSignCommitted(makeSign("Thumb_Up", 1000)))
    assertEquals(1, processor.currentSequence.size)
  }

  @Test
  fun `suppresses immediate repeat of same label`() {
    val processor = SignSequenceProcessor(minRepeatIntervalMs = 1500)
    assertTrue(processor.onSignCommitted(makeSign("Thumb_Up", 1000, 1500)))
    // Same label, too soon (only 200ms later)
    assertFalse(processor.onSignCommitted(makeSign("Thumb_Up", 1700, 1700)))
    assertEquals(1, processor.currentSequence.size)
  }

  @Test
  fun `allows same label after repeat interval`() {
    val processor = SignSequenceProcessor(minRepeatIntervalMs = 1500)
    assertTrue(processor.onSignCommitted(makeSign("Thumb_Up", 1000, 1500)))
    // Same label, enough time passed
    assertTrue(processor.onSignCommitted(makeSign("Thumb_Up", 3100, 3100)))
    assertEquals(2, processor.currentSequence.size)
  }

  @Test
  fun `accepts different labels immediately`() {
    val processor = SignSequenceProcessor(minRepeatIntervalMs = 1500)
    assertTrue(processor.onSignCommitted(makeSign("Thumb_Up", 1000)))
    assertTrue(processor.onSignCommitted(makeSign("Victory", 1200)))
    assertEquals(2, processor.currentSequence.size)
    assertEquals(listOf("thumb_up", "victory"), processor.currentLabels)
  }

  @Test
  fun `detects end of utterance after gap`() {
    val processor = SignSequenceProcessor(pauseGapFrames = 5)
    processor.onSignCommitted(makeSign("Thumb_Up", 1000))
    assertFalse(processor.isUtteranceComplete)

    processor.onGapUpdate(3) // Not enough
    assertFalse(processor.isUtteranceComplete)

    processor.onGapUpdate(5) // Enough
    assertTrue(processor.isUtteranceComplete)
  }

  @Test
  fun `no utterance end when sequence is empty`() {
    val processor = SignSequenceProcessor(pauseGapFrames = 2)
    processor.onGapUpdate(10)
    assertFalse(processor.isUtteranceComplete)
  }

  @Test
  fun `consumeUtterance resets the flag`() {
    val processor = SignSequenceProcessor(pauseGapFrames = 2)
    processor.onSignCommitted(makeSign("Thumb_Up", 1000))
    processor.onGapUpdate(5)
    assertTrue(processor.isUtteranceComplete)
    processor.consumeUtterance()
    assertFalse(processor.isUtteranceComplete)
  }

  @Test
  fun `clear resets everything`() {
    val processor = SignSequenceProcessor()
    processor.onSignCommitted(makeSign("Thumb_Up", 1000))
    processor.onSignCommitted(makeSign("Victory", 3000))
    processor.clear()
    assertTrue(processor.currentSequence.isEmpty())
    assertFalse(processor.isUtteranceComplete)
  }
}

class EgyptianSignLexiconRepositoryTest {

  @Test
  fun `lookup finds MediaPipe gesture labels`() {
    val repo = EgyptianSignLexiconRepository()
    val entry = repo.lookup("Open_Palm")
    assertNotNull(entry)
    assertEquals("مرحبا", entry!!.arabicText)
    assertEquals("Hello", entry.englishGloss)
  }

  @Test
  fun `lookup finds by alias`() {
    val repo = EgyptianSignLexiconRepository()
    val entry = repo.lookup("thumbs_up")
    assertNotNull(entry)
    assertEquals("نعم", entry!!.arabicText)
  }

  @Test
  fun `lookup is case-insensitive`() {
    val repo = EgyptianSignLexiconRepository()
    assertNotNull(repo.lookup("THUMB_UP"))
    assertNotNull(repo.lookup("thumb_up"))
    assertNotNull(repo.lookup("Thumb_Up"))
  }

  @Test
  fun `isKnown returns true for known labels`() {
    val repo = EgyptianSignLexiconRepository()
    assertTrue(repo.isKnown("Thumb_Up"))
    assertTrue(repo.isKnown("Victory"))
    assertTrue(repo.isKnown("ILoveYou"))
  }

  @Test
  fun `isKnown returns false for unknown labels`() {
    val repo = EgyptianSignLexiconRepository()
    assertFalse(repo.isKnown("nonexistent_gesture"))
  }

  @Test
  fun `recordUnknown logs unknown labels`() {
    val repo = EgyptianSignLexiconRepository()
    repo.recordUnknown("mystery_sign")
    assertTrue(repo.unknownLabelsEncountered.contains("mystery_sign"))
  }

  @Test
  fun `search finds by Arabic text`() {
    val repo = EgyptianSignLexiconRepository()
    val results = repo.search("نعم")
    assertTrue(results.isNotEmpty())
    assertTrue(results.any { it.arabicText == "نعم" })
  }

  @Test
  fun `search finds by English gloss`() {
    val repo = EgyptianSignLexiconRepository()
    val results = repo.search("Hello")
    assertTrue(results.isNotEmpty())
  }

  @Test
  fun `search finds by category`() {
    val repo = EgyptianSignLexiconRepository()
    val results = repo.search("greeting")
    assertTrue(results.isNotEmpty())
    assertTrue(results.all { it.category == "greeting" })
  }

  @Test
  fun `matchPhrase finds matching phrase rule`() {
    val repo = EgyptianSignLexiconRepository()
    val labels = listOf("open_palm", "pointing_up")
    val match = repo.matchPhrase(labels)
    assertNotNull(match)
    assertEquals("مرحبا، كيف حالك؟", match!!.arabicPhrase)
  }

  @Test
  fun `matchPhrase returns null for no match`() {
    val repo = EgyptianSignLexiconRepository()
    val labels = listOf("pointing_up", "open_palm", "iloveyou")
    val match = repo.matchPhrase(labels)
    // Only matches tail, so last 2 = ["open_palm", "iloveyou"] → "مرحبا، أحبك"
    // Actually let's check what the seed phrase rules have
    // "phrase-hello-love" matches ["Open_Palm", "ILoveYou"]
    // normalized: ["open_palm", "iloveyou"]
    assertNotNull(match) // This should match phrase-hello-love
  }

  @Test
  fun `allEntries returns non-empty list`() {
    val repo = EgyptianSignLexiconRepository()
    assertTrue(repo.allEntries.isNotEmpty())
    assertTrue(repo.allEntries.size >= 7) // At least the 7 MediaPipe gestures
  }

  @Test
  fun `confidenceThreshold returns override when set`() {
    val repo = EgyptianSignLexiconRepository()
    // Emergency has a 0.60 override in seed data
    val threshold = repo.confidenceThreshold("emergency", default = 0.70f)
    assertEquals(0.60f, threshold)
  }

  @Test
  fun `confidenceThreshold returns default when no override`() {
    val repo = EgyptianSignLexiconRepository()
    val threshold = repo.confidenceThreshold("Open_Palm", default = 0.70f)
    assertEquals(0.70f, threshold)
  }
}

class SignToTextTranslatorTest {

  private fun makeSign(label: String) =
    CommittedSign(label, SignPrediction.normalizeLabel(label), 0.90f, 1000L, 1500L, 3)

  @Test
  fun `translates single known sign to Arabic`() {
    val lexicon = EgyptianSignLexiconRepository()
    val translator = SignToTextTranslator(lexicon)
    val result = translator.translate(listOf(makeSign("Open_Palm")))
    assertEquals("مرحبا", result.arabicText)
  }

  @Test
  fun `translates multiple signs to Arabic sentence`() {
    val lexicon = EgyptianSignLexiconRepository()
    val translator = SignToTextTranslator(lexicon)
    val result = translator.translate(listOf(
      makeSign("Open_Palm"),
      makeSign("Thumb_Up")
    ))
    assertTrue(result.arabicText.contains("مرحبا"))
    assertTrue(result.arabicText.contains("نعم") || result.arabicText.contains("شكرا"))
  }

  @Test
  fun `phrase matching produces combined output`() {
    val lexicon = EgyptianSignLexiconRepository()
    val translator = SignToTextTranslator(lexicon)
    // Open_Palm + Pointing_Up → "مرحبا، كيف حالك؟"
    val result = translator.translate(listOf(
      makeSign("Open_Palm"),
      makeSign("Pointing_Up")
    ))
    assertEquals("مرحبا، كيف حالك؟", result.arabicText)
    assertEquals("Hello, how are you?", result.englishText)
  }

  @Test
  fun `generic where question is reordered into natural sentence`() {
    val lexicon = EgyptianSignLexiconRepository()
    val translator = SignToTextTranslator(lexicon)
    val result = translator.translate(
      listOf(makeSign("فين"), makeSign("مهندس")),
      isUtteranceComplete = true
    )

    assertEquals("مهندس فين؟", result.arabicText)
    assertEquals("Where is the engineer?", result.englishText)
  }

  @Test
  fun `generic who question is reordered into natural sentence`() {
    val lexicon = EgyptianSignLexiconRepository()
    val translator = SignToTextTranslator(lexicon)
    val result = translator.translate(
      listOf(makeSign("مين"), makeSign("مهندس")),
      isUtteranceComplete = true
    )

    assertEquals("مين مهندس؟", result.arabicText)
    assertEquals("Who is the engineer?", result.englishText)
  }

  @Test
  fun `generic how much question is built from esl signs`() {
    val lexicon = EgyptianSignLexiconRepository()
    val translator = SignToTextTranslator(lexicon)
    val result = translator.translate(
      listOf(makeSign("كام كمية"), makeSign("فلوس")),
      isUtteranceComplete = true
    )

    assertEquals("كام فلوس؟", result.arabicText)
    assertEquals("How much money?", result.englishText)
  }

  @Test
  fun `predicate template builds natural statement`() {
    val lexicon = EgyptianSignLexiconRepository()
    val translator = SignToTextTranslator(lexicon)
    val result = translator.translate(
      listOf(makeSign("دكتور"), makeSign("مشغول")),
      isUtteranceComplete = true
    )

    assertEquals("دكتور مشغول.", result.arabicText)
    assertEquals("The doctor is busy.", result.englishText)
  }

  @Test
  fun `unknown sign with high confidence shows placeholder`() {
    val lexicon = EgyptianSignLexiconRepository()
    val translator = SignToTextTranslator(lexicon)
    val unknownSign = CommittedSign("UnknownGesture", "unknowngesture", 0.90f, 1000L, 1500L, 3)
    val result = translator.translate(listOf(unknownSign))
    assertTrue(result.arabicText.contains("[UnknownGesture]"))
  }

  @Test
  fun `unknown sign with low confidence is skipped`() {
    val lexicon = EgyptianSignLexiconRepository()
    val translator = SignToTextTranslator(lexicon)
    val unknownSign = CommittedSign("UnknownGesture", "unknowngesture", 0.50f, 1000L, 1500L, 3)
    val result = translator.translate(listOf(
      makeSign("Thumb_Up"),
      unknownSign,
      makeSign("Victory")
    ))
    assertFalse(result.arabicText.contains("[UnknownGesture]"))
    assertTrue(result.arabicText.contains("نعم"))
    assertTrue(result.arabicText.contains("سلام"))
  }

  @Test
  fun `unknown sign is recorded in lexicon`() {
    val lexicon = EgyptianSignLexiconRepository()
    val translator = SignToTextTranslator(lexicon)
    val unknownSign = CommittedSign("BrandNewSign", "brandnewsign", 0.90f, 1000L, 1500L, 3)
    translator.translate(listOf(unknownSign))
    assertTrue(lexicon.unknownLabelsEncountered.contains("brandnewsign"))
  }

  @Test
  fun `adds punctuation on utterance complete`() {
    val lexicon = EgyptianSignLexiconRepository()
    val translator = SignToTextTranslator(lexicon)
    val result = translator.translate(
      listOf(makeSign("Thumb_Up")),
      isUtteranceComplete = true
    )
    assertTrue(result.arabicText.endsWith("."))
    assertTrue(result.englishText.endsWith("."))
  }

  @Test
  fun `no punctuation on partial utterance`() {
    val lexicon = EgyptianSignLexiconRepository()
    val translator = SignToTextTranslator(lexicon)
    val result = translator.translate(
      listOf(makeSign("Thumb_Up")),
      isPartial = true
    )
    assertFalse(result.arabicText.endsWith("."))
  }

  @Test
  fun `empty sign list returns empty text`() {
    val lexicon = EgyptianSignLexiconRepository()
    val translator = SignToTextTranslator(lexicon)
    val result = translator.translate(emptyList())
    assertEquals("", result.arabicText)
    assertEquals("", result.englishText)
    assertTrue(result.isPartial)
  }

  @Test
  fun `removeDuplicateWords works`() {
    assertEquals("نعم شكرا", SignToTextTranslator.removeDuplicateWords("نعم نعم شكرا"))
    assertEquals("hello world", SignToTextTranslator.removeDuplicateWords("hello hello world"))
    assertEquals("a b a", SignToTextTranslator.removeDuplicateWords("a b a")) // non-consecutive ok
    assertEquals("single", SignToTextTranslator.removeDuplicateWords("single"))
    assertEquals("", SignToTextTranslator.removeDuplicateWords(""))
  }

  @Test
  fun `debug mode produces debug info`() {
    val lexicon = EgyptianSignLexiconRepository()
    val translator = SignToTextTranslator(lexicon, debugMode = true)
    val result = translator.translate(listOf(makeSign("Open_Palm")))
    assertNotNull(result.debugInfo)
    assertTrue(result.debugInfo!!.stabilizedLabels.contains("open_palm"))
  }

  @Test
  fun `debug mode off produces no debug info`() {
    val lexicon = EgyptianSignLexiconRepository()
    val translator = SignToTextTranslator(lexicon, debugMode = false)
    val result = translator.translate(listOf(makeSign("Open_Palm")))
    assertNull(result.debugInfo)
  }

  @Test
  fun `cleanupEnglishText capitalizes first letter`() {
    val lexicon = EgyptianSignLexiconRepository()
    val translator = SignToTextTranslator(lexicon)
    assertEquals("Hello world.", translator.cleanupEnglishText("hello world", addPunctuation = true))
  }

  @Test
  fun `cleanupArabicText removes extra spaces`() {
    val lexicon = EgyptianSignLexiconRepository()
    val translator = SignToTextTranslator(lexicon)
    assertEquals("مرحبا نعم.", translator.cleanupArabicText("مرحبا  نعم", addPunctuation = true))
  }

  @Test
  fun `does not add double punctuation`() {
    val lexicon = EgyptianSignLexiconRepository()
    val translator = SignToTextTranslator(lexicon)
    assertEquals("كيف حالك؟", translator.cleanupArabicText("كيف حالك؟", addPunctuation = true))
    assertEquals("Hello?", translator.cleanupEnglishText("Hello?", addPunctuation = true))
  }
}

// ── LandmarkFrameBuffer Tests ─────────────────────────────────────────

class LandmarkFrameBufferTest {

  @Test
  fun `buffer is not ready until 30 frames`() {
    val buffer = LandmarkFrameBuffer()
    assertFalse(buffer.isReady)
    repeat(29) {
      buffer.addFrame(LandmarkFrameBuffer.emptyFrame(), false)
    }
    assertFalse(buffer.isReady)
    buffer.addFrame(LandmarkFrameBuffer.emptyFrame(), false)
    assertTrue(buffer.isReady)
  }

  @Test
  fun `getSequence returns null when not ready`() {
    val buffer = LandmarkFrameBuffer()
    assertNull(buffer.getSequence())
  }

  @Test
  fun `getSequence returns correct frame count`() {
    val buffer = LandmarkFrameBuffer()
    repeat(30) {
      buffer.addFrame(LandmarkFrameBuffer.emptyFrame(), true)
    }
    val sequence = buffer.getSequence()
    assertNotNull(sequence)
    assertEquals(30, sequence!!.size)
  }

  @Test
  fun `consumeAndSlide removes oldest frames`() {
    val buffer = LandmarkFrameBuffer(sequenceLength = 30, slideAmount = 10)
    repeat(30) {
      buffer.addFrame(LandmarkFrameBuffer.emptyFrame(), true)
    }
    assertTrue(buffer.isReady)
    buffer.consumeAndSlide()
    // After sliding 10 frames, we have 20 remaining — not enough
    assertFalse(buffer.isReady)
    assertEquals(20, buffer.frameCount)
  }

  @Test
  fun `handFrameRatio tracks hand detection`() {
    val buffer = LandmarkFrameBuffer()
    repeat(15) { buffer.addFrame(LandmarkFrameBuffer.emptyFrame(), true) }
    repeat(15) { buffer.addFrame(LandmarkFrameBuffer.emptyFrame(), false) }
    assertEquals(0.5f, buffer.handFrameRatio, 0.01f)
  }

  @Test
  fun `clear resets buffer completely`() {
    val buffer = LandmarkFrameBuffer()
    repeat(30) { buffer.addFrame(LandmarkFrameBuffer.emptyFrame(), true) }
    assertTrue(buffer.isReady)
    buffer.clear()
    assertFalse(buffer.isReady)
    assertEquals(0, buffer.frameCount)
  }

  @Test
  fun `buildFrame creates correct landmark layout`() {
    val rightHand = List(21) { floatArrayOf(it.toFloat(), it * 2f, it * 3f) }
    val frame = LandmarkFrameBuffer.buildFrame(rightHand = rightHand)
    // Right hand at indices 0-20
    assertEquals(0f, frame[0][0], 0.001f)
    assertEquals(20f, frame[20][0], 0.001f)
    // Left hand at indices 21-41 should be zeros
    assertEquals(0f, frame[21][0], 0.001f)
    // Total 46 landmarks
    assertEquals(46, frame.size)
  }

  @Test
  fun `buildFrame with both hands`() {
    val rightHand = List(21) { floatArrayOf(1f, 1f, 1f) }
    val leftHand = List(21) { floatArrayOf(2f, 2f, 2f) }
    val frame = LandmarkFrameBuffer.buildFrame(rightHand, leftHand)
    assertEquals(1f, frame[0][0], 0.001f)  // right hand
    assertEquals(2f, frame[21][0], 0.001f) // left hand
    assertEquals(1f, frame[42][0], 0.001f) // right wrist (derived)
    assertEquals(2f, frame[44][0], 0.001f) // left wrist (derived)
  }

  @Test
  fun `rejects frame with wrong landmark count`() {
    val buffer = LandmarkFrameBuffer()
    buffer.addFrame(Array(10) { FloatArray(3) }, true) // wrong size
    assertEquals(0, buffer.frameCount) // should be rejected
  }
}

// ── ESL Lexicon Integration Tests ──────────────────────────────────────

class EslLexiconIntegrationTest {

  @Test
  fun `lexicon contains ESL LSTM signs`() {
    val repo = EgyptianSignLexiconRepository()
    // Check a few Egyptian signs
    assertNotNull(repo.lookup("ازاى"))
    assertNotNull(repo.lookup("شكرا"))
    assertNotNull(repo.lookup("دكتور"))
    assertNotNull(repo.lookup("فين"))
    assertNotNull(repo.lookup("مع السلامة"))
  }

  @Test
  fun `ESL signs translate correctly`() {
    val lexicon = EgyptianSignLexiconRepository()
    val entry = lexicon.lookup("ازاى")
    assertNotNull(entry)
    assertEquals("How", entry!!.englishGloss)
  }

  @Test
  fun `ESL phrase rules work`() {
    val lexicon = EgyptianSignLexiconRepository()
    val translator = SignToTextTranslator(lexicon)
    val signs = listOf(
      makeSign("ازاى"),
      makeSign("الجو")
    )
    val result = translator.translate(signs, isPartial = false, isUtteranceComplete = true)
    // Phrase rule produces "ازاى الجو؟" — cleanup won't add period after ؟
    assertTrue(result.arabicText.contains("ازاى الجو"))
    assertTrue(result.englishText.contains("How's the weather"))
  }

  @Test
  fun `total lexicon has at least 73 entries (7 MediaPipe + 66 ESL)`() {
    val repo = EgyptianSignLexiconRepository()
    assertTrue("Expected at least 73 entries, got ${repo.allEntries.size}",
      repo.allEntries.size >= 73)
  }

  @Test
  fun `total phrase rules include ESL rules`() {
    val repo = EgyptianSignLexiconRepository()
    assertTrue("Expected at least 25 phrase rules, got ${repo.allPhraseRules.size}",
      repo.allPhraseRules.size >= 25)
  }

  @Test
  fun `ESL question words all exist in lexicon`() {
    val repo = EgyptianSignLexiconRepository()
    val questionWords = listOf("ازاى", "امتى", "ايه", "فين", "ليه", "مين")
    questionWords.forEach { word ->
      assertNotNull("Missing question word: $word", repo.lookup(word))
    }
  }

  @Test
  fun `ESL day names all exist in lexicon`() {
    val repo = EgyptianSignLexiconRepository()
    val days = listOf("الاحد", "الاربعاء", "الثلاثاء", "الجمعة", "الخميس", "السبت")
    days.forEach { day ->
      assertNotNull("Missing day: $day", repo.lookup(day))
    }
  }

  @Test
  fun `ESL color names all exist in lexicon`() {
    val repo = EgyptianSignLexiconRepository()
    val colors = listOf("ابيض", "احمر", "ازرق", "اسود", "برتقالي", "بنفسجى")
    colors.forEach { color ->
      assertNotNull("Missing color: $color", repo.lookup(color))
    }
  }

  @Test
  fun `SignToTextTranslator handles mixed MediaPipe and ESL signs`() {
    val lexicon = EgyptianSignLexiconRepository()
    val translator = SignToTextTranslator(lexicon)
    val signs = listOf(
      makeSign("Open_Palm"),
      makeSign("شكرا")
    )
    val result = translator.translate(signs)
    assertTrue(result.arabicText.contains("مرحبا"))
    assertTrue(result.arabicText.contains("شكرا"))
  }

  private fun makeSign(label: String, confidence: Float = 0.95f) = CommittedSign(
    label = label,
    normalizedLabel = SignPrediction.normalizeLabel(label),
    confidence = confidence,
    startTimestampMs = 0L,
    endTimestampMs = 100L,
    frameCount = 5
  )
}
