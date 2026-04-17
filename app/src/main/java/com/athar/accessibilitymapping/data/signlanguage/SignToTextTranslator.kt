package com.athar.accessibilitymapping.data.signlanguage

import android.util.Log

/**
 * Translates a sequence of committed signs into readable Arabic and English text.
 *
 * Pipeline: committed signs -> phrase matching -> grammar-aware templates -> word lookup -> text cleanup.
 *
 * This is the final stage that produces user-facing [TranslationResult] output.
 *
 * NOTE: The current implementation uses the 7-gesture MediaPipe model plus
 * phrase rules defined in the lexicon. Full grammatical Egyptian Sign Language
 * translation still requires:
 * - A much larger vocabulary model [EXPANSION_NEEDED]
 * - Proper ESL grammar rules beyond simple templates
 * - Contextual disambiguation
 */
class SignToTextTranslator(
  private val lexicon: EgyptianSignLexiconRepository,
  private val debugMode: Boolean = false
) {

  private data class TemplatePhraseMatch(
    val id: String,
    val signCount: Int,
    val arabicPhrase: String,
    val englishPhrase: String
  )

  /**
   * Translate the current committed sign sequence into text.
   *
   * @param signs The ordered sequence of committed signs.
   * @param isPartial True if the user is still signing (live partial output).
   * @param isUtteranceComplete True if a pause was detected (sentence end).
   */
  fun translate(
    signs: List<CommittedSign>,
    isPartial: Boolean = false,
    isUtteranceComplete: Boolean = false
  ): TranslationResult {
    if (signs.isEmpty()) {
      return TranslationResult("", "", emptyList(), isPartial = true)
    }

    val arabicParts = mutableListOf<String>()
    val englishParts = mutableListOf<String>()
    val debugPhraseMatches = mutableListOf<String>()
    val debugUnknownLabels = mutableListOf<String>()

    var i = 0
    while (i < signs.size) {
      val remaining = signs.subList(i, signs.size).map { it.normalizedLabel }
      val phraseMatch = findBestPhraseMatch(remaining)

      if (phraseMatch != null) {
        arabicParts.add(phraseMatch.arabicPhrase)
        englishParts.add(phraseMatch.englishPhrase)
        debugPhraseMatches += "${phraseMatch.id}: ${phraseMatch.signSequence.joinToString("+")} -> ${phraseMatch.arabicPhrase}"
        i += phraseMatch.signSequence.size
        continue
      }

      val templateMatch = findBestTemplateMatch(signs, i)
      if (templateMatch != null) {
        arabicParts.add(templateMatch.arabicPhrase)
        englishParts.add(templateMatch.englishPhrase)
        debugPhraseMatches += "${templateMatch.id}: ${signs.subList(i, i + templateMatch.signCount).joinToString("+") { it.normalizedLabel }} -> ${templateMatch.arabicPhrase}"
        i += templateMatch.signCount
        continue
      }

      val sign = signs[i]
      val entry = lookupEntry(sign)
      if (entry != null) {
        arabicParts.add(entry.arabicText)
        englishParts.add(entry.englishGloss.ifBlank { entry.recognitionLabel })
      } else {
        lexicon.recordUnknown(sign.label)
        debugUnknownLabels.add(sign.normalizedLabel)
        if (sign.confidence >= 0.85f) {
          arabicParts.add("[${sign.label}]")
          englishParts.add("[${sign.label}]")
        }
        Log.d(
          TAG,
          "Unknown sign '${sign.label}' (confidence=${sign.confidence}) -> " +
            if (sign.confidence >= 0.85f) "placeholder inserted" else "skipped"
        )
      }
      i++
    }

    val cleanedArabic = cleanupArabicText(arabicParts.joinToString(" "), isUtteranceComplete)
    val cleanedEnglish = cleanupEnglishText(englishParts.joinToString(" "), isUtteranceComplete)

    val debugInfo = if (debugMode) {
      TranslationDebugInfo(
        rawPredictions = signs.map { "${it.normalizedLabel}@${(it.confidence * 100).toInt()}%" },
        stabilizedLabels = signs.map { it.normalizedLabel },
        phraseMatches = debugPhraseMatches,
        unknownLabels = debugUnknownLabels
      )
    } else null

    return TranslationResult(
      arabicText = cleanedArabic,
      englishText = cleanedEnglish,
      signSequence = signs,
      isPartial = isPartial && !isUtteranceComplete,
      debugInfo = debugInfo
    )
  }

  /**
   * Try to find the longest exact phrase match starting from the beginning
   * of the given label sequence.
   */
  private fun findBestPhraseMatch(labels: List<String>): PhraseRule? {
    if (labels.isEmpty()) return null
    return lexicon.allPhraseRules
      .filter { rule ->
        rule.signSequence.size <= labels.size &&
          labels.take(rule.signSequence.size) == rule.signSequence.map { SignPrediction.normalizeLabel(it) }
      }
      .maxByOrNull { it.signSequence.size * 1000 + it.priority }
  }

  /**
   * Grammar-aware fallback for common Egyptian sign patterns when there is no
   * exact phrase rule.
   */
  private fun findBestTemplateMatch(signs: List<CommittedSign>, startIndex: Int): TemplatePhraseMatch? {
    if (startIndex + 1 >= signs.size) return null
    val firstSign = signs[startIndex]
    val secondSign = signs[startIndex + 1]
    val firstEntry = lookupEntry(firstSign) ?: return null
    val secondEntry = lookupEntry(secondSign) ?: return null

    return buildQuestionTemplate(firstSign.normalizedLabel, secondEntry)
      ?: buildPredicateTemplate(firstEntry, secondEntry)
  }

  private fun buildQuestionTemplate(
    normalizedQuestionLabel: String,
    targetEntry: LexiconEntry
  ): TemplatePhraseMatch? {
    val englishTarget = englishQuestionTarget(targetEntry)
    return when (normalizedQuestionLabel) {
      "where", "فين" -> TemplatePhraseMatch(
        id = "template-where",
        signCount = 2,
        arabicPhrase = "${targetEntry.arabicText} فين؟",
        englishPhrase = "Where is $englishTarget?"
      )

      "who", "مين" -> TemplatePhraseMatch(
        id = "template-who",
        signCount = 2,
        arabicPhrase = "مين ${targetEntry.arabicText}؟",
        englishPhrase = "Who is $englishTarget?"
      )

      "when", "امتى" -> TemplatePhraseMatch(
        id = "template-when",
        signCount = 2,
        arabicPhrase = "${targetEntry.arabicText} امتى؟",
        englishPhrase = "When is $englishTarget?"
      )

      "what", "ايه" -> TemplatePhraseMatch(
        id = "template-what",
        signCount = 2,
        arabicPhrase = "${targetEntry.arabicText} ايه؟",
        englishPhrase = "What is $englishTarget?"
      )

      "how", "ازاى" -> TemplatePhraseMatch(
        id = "template-how",
        signCount = 2,
        arabicPhrase = "ازاي ${targetEntry.arabicText}؟",
        englishPhrase = "How is $englishTarget?"
      )

      "why", "ليه" -> TemplatePhraseMatch(
        id = "template-why",
        signCount = 2,
        arabicPhrase = "ليه ${targetEntry.arabicText}؟",
        englishPhrase = "Why $englishTarget?"
      )

      "كام_كمية", "كام_للعدد", "how_much", "how_many" -> TemplatePhraseMatch(
        id = "template-how-many",
        signCount = 2,
        arabicPhrase = "كام ${targetEntry.arabicText}؟",
        englishPhrase = "How much ${englishNounPhrase(targetEntry, includeArticle = false)}?"
      )

      else -> null
    }
  }

  private fun buildPredicateTemplate(
    subjectEntry: LexiconEntry,
    predicateEntry: LexiconEntry
  ): TemplatePhraseMatch? {
    if (!subjectEntry.canMergeIntoPhrase || !predicateEntry.canMergeIntoPhrase) return null

    val predicateLabel = SignPrediction.normalizeLabel(predicateEntry.recognitionLabel)
    val isPredicateLike =
      predicateEntry.category in setOf("description", "color", "emotion") ||
        predicateLabel in setOf("جيد", "كويس", "جيد_-_كويس", "ممكن")
    if (!isPredicateLike) return null

    val isSubjectLike = subjectEntry.category in setOf(
      "common",
      "job",
      "family",
      "needs",
      "accessibility",
      "day"
    )
    if (!isSubjectLike) return null

    return TemplatePhraseMatch(
      id = "template-predicate",
      signCount = 2,
      arabicPhrase = "${subjectEntry.arabicText} ${predicateEntry.arabicText}",
      englishPhrase = "${englishNounPhrase(subjectEntry)} is ${englishPredicateWord(predicateEntry)}"
    )
  }

  private fun lookupEntry(sign: CommittedSign): LexiconEntry? {
    return lexicon.lookup(sign.label) ?: lexicon.lookup(sign.normalizedLabel)
  }

  private fun englishQuestionTarget(entry: LexiconEntry): String {
    return englishNounPhrase(entry)
      .replaceFirstChar { it.lowercaseChar() }
  }

  private fun englishNounPhrase(entry: LexiconEntry, includeArticle: Boolean = true): String {
    val base = englishBase(entry)
    if (!includeArticle) return base
    val needsArticle = entry.category in setOf("job", "family", "accessibility", "day") ||
      base in setOf("doctor", "teacher", "engineer", "pilot", "worker", "college", "problem", "name", "number")
    return if (needsArticle) "the $base" else base
  }

  private fun englishPredicateWord(entry: LexiconEntry): String {
    return when (SignPrediction.normalizeLabel(entry.recognitionLabel)) {
      "جيد", "كويس", "جيد_-_كويس" -> "good"
      "ممكن" -> "possible"
      else -> englishBase(entry)
    }
  }

  private fun englishBase(entry: LexiconEntry): String {
    val gloss = entry.englishGloss.ifBlank { entry.recognitionLabel }.trim()
    return when {
      gloss.contains("/") -> gloss.substringBefore("/").trim().lowercase()
      else -> gloss.lowercase()
    }
  }

  // -- Text cleanup ---------------------------------------------------------

  internal fun cleanupArabicText(raw: String, addPunctuation: Boolean): String {
    var text = raw.trim()
    text = removeDuplicateWords(text)
    text = text.replace(Regex("\\s{2,}"), " ").trim()
    if (addPunctuation && text.isNotEmpty() && !text.endsWith(".") &&
      !text.endsWith("؟") && !text.endsWith("!") && !text.endsWith("،")
    ) {
      text += "."
    }
    return text
  }

  internal fun cleanupEnglishText(raw: String, addPunctuation: Boolean): String {
    var text = raw.trim()
    text = removeDuplicateWords(text)
    text = text.replace(Regex("\\s{2,}"), " ").trim()
    if (text.isNotEmpty()) {
      text = text.replaceFirstChar { it.uppercaseChar() }
    }
    if (addPunctuation && text.isNotEmpty() && !text.endsWith(".") &&
      !text.endsWith("?") && !text.endsWith("!") && !text.endsWith(",")
    ) {
      text += "."
    }
    return text
  }

  companion object {
    private const val TAG = "SignToTextTranslator"

    /**
     * Remove consecutive duplicate words.
     * "نعم نعم شكرا" -> "نعم شكرا"
     */
    internal fun removeDuplicateWords(text: String): String {
      if (text.isBlank()) return ""
      val words = text.trim().split(Regex("\\s+"))
      if (words.size <= 1) return words.first()
      val result = mutableListOf(words.first())
      for (i in 1 until words.size) {
        if (words[i] != words[i - 1]) {
          result.add(words[i])
        }
      }
      return result.joinToString(" ")
    }
  }
}
