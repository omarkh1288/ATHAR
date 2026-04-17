package com.athar.accessibilitymapping.data.signlanguage

import android.content.Context
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * A single entry in the Egyptian Sign Language lexicon.
 *
 * NOTE: The current app recognizes 7 MediaPipe hand gestures, not full
 * Egyptian Sign Language (ESL). This lexicon maps those 7 gestures plus
 * placeholder entries from the legacy sign database. True full-coverage
 * ESL translation requires a dedicated trained model and a verified
 * linguistic dataset — see [EXPANSION_NEEDED] markers below.
 */
@Serializable
data class LexiconEntry(
  val id: String,
  val recognitionLabel: String,
  val arabicText: String,
  val englishGloss: String = "",
  val category: String = "general",
  val aliases: List<String> = emptyList(),
  val phraseTags: List<String> = emptyList(),
  val confidenceThresholdOverride: Float? = null,
  val canMergeIntoPhrase: Boolean = true
)

/**
 * A phrase rule that maps a sequence of recognized sign labels to a
 * single Arabic/English phrase output.
 */
@Serializable
data class PhraseRule(
  val id: String,
  val signSequence: List<String>,
  val arabicPhrase: String,
  val englishPhrase: String,
  val category: String = "general",
  val priority: Int = 0
)

/**
 * Repository that loads and indexes the Egyptian Sign Language lexicon.
 * Entries can come from a JSON asset file or from the built-in seed data.
 */
class EgyptianSignLexiconRepository(context: Context? = null) {

  companion object {
    private const val TAG = "EgyptianSignLexicon"
    private const val LEXICON_ASSET = "esl_lexicon.json"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
  }

  private val entriesByNormalizedLabel = linkedMapOf<String, LexiconEntry>()
  private val entriesByAlias = linkedMapOf<String, LexiconEntry>()
  private val entriesById = linkedMapOf<String, LexiconEntry>()
  private val phraseRules = mutableListOf<PhraseRule>()

  val allEntries: List<LexiconEntry> get() = entriesByNormalizedLabel.values.toList()
  val allPhraseRules: List<PhraseRule> get() = phraseRules.toList()
  val unknownLabelsEncountered = mutableSetOf<String>()

  init {
    var loaded = false
    if (context != null) {
      loaded = loadFromAsset(context)
    }
    if (!loaded) {
      loadSeedData()
    }
    loadSeedPhraseRules()
  }

  private fun loadFromAsset(context: Context): Boolean {
    return try {
      val raw = context.assets.open(LEXICON_ASSET).bufferedReader().readText()
      val entries = json.decodeFromString(ListSerializer(LexiconEntry.serializer()), raw)
      entries.forEach(::indexEntry)
      Log.d(TAG, "Loaded ${entries.size} lexicon entries from asset.")
      true
    } catch (e: Exception) {
      Log.d(TAG, "No asset lexicon found ($LEXICON_ASSET), using seed data: ${e.message}")
      false
    }
  }

  private fun indexEntry(entry: LexiconEntry) {
    val normalized = SignPrediction.normalizeLabel(entry.recognitionLabel)
    entriesByNormalizedLabel[normalized] = entry
    entriesById[entry.id] = entry
    entry.aliases.forEach { alias ->
      entriesByAlias[SignPrediction.normalizeLabel(alias)] = entry
    }
  }

  /** Look up a lexicon entry by recognition label (normalized). */
  fun lookup(label: String): LexiconEntry? {
    val normalized = SignPrediction.normalizeLabel(label)
    return entriesByNormalizedLabel[normalized]
      ?: entriesByAlias[normalized]
  }

  /** Look up by ID. */
  fun lookupById(id: String): LexiconEntry? = entriesById[id]

  /** Check if a label is known. */
  fun isKnown(label: String): Boolean = lookup(label) != null

  /** Record an unknown label for future expansion logging. */
  fun recordUnknown(label: String) {
    val normalized = SignPrediction.normalizeLabel(label)
    if (unknownLabelsEncountered.add(normalized)) {
      Log.w(TAG, "[EXPANSION_NEEDED] Unknown sign label encountered: '$label' (normalized: '$normalized'). " +
        "Add this to the lexicon when a trained model for this sign is available.")
    }
  }

  /** Get the confidence threshold for a label, or the default. */
  fun confidenceThreshold(label: String, default: Float = 0.70f): Float {
    return lookup(label)?.confidenceThresholdOverride ?: default
  }

  /** Search entries by Arabic text, English gloss, label, alias, or category. */
  fun search(query: String): List<LexiconEntry> {
    val q = query.trim().lowercase()
    if (q.isBlank()) return allEntries
    return allEntries.filter { entry ->
      entry.arabicText.contains(q, ignoreCase = true) ||
        entry.englishGloss.contains(q, ignoreCase = true) ||
        entry.recognitionLabel.contains(q, ignoreCase = true) ||
        entry.category.contains(q, ignoreCase = true) ||
        entry.aliases.any { it.contains(q, ignoreCase = true) }
    }
  }

  /**
   * Try to match a phrase rule against the tail of the given label sequence.
   * Returns the longest matching rule, or null.
   */
  fun matchPhrase(normalizedLabels: List<String>): PhraseRule? {
    if (normalizedLabels.isEmpty()) return null
    return phraseRules
      .filter { rule -> rule.signSequence.size <= normalizedLabels.size }
      .filter { rule ->
        val tail = normalizedLabels.takeLast(rule.signSequence.size)
        tail == rule.signSequence.map { SignPrediction.normalizeLabel(it) }
      }
      .maxByOrNull { it.signSequence.size * 1000 + it.priority }
  }

  // ── Seed data ─────────────────────────────────────────────────────────
  // These map the 7 MediaPipe gestures recognized by the current model
  // plus entries from the legacy signLanguageDatabase for future expansion.

  private fun loadSeedData() {
    val seedEntries = buildList {
      // ── Currently recognized by the MediaPipe gesture_recognizer.task model ──
      add(LexiconEntry("mp-open-palm", "Open_Palm", "مرحبا", "Hello",
        "greeting", listOf("open_palm", "palm"), listOf("greeting"), canMergeIntoPhrase = true))
      add(LexiconEntry("mp-closed-fist", "Closed_Fist", "قبضة", "Fist/Stop",
        "gesture", listOf("closed_fist", "fist"), listOf("stop", "emphasis")))
      add(LexiconEntry("mp-pointing-up", "Pointing_Up", "انتبه", "Attention",
        "gesture", listOf("pointing_up", "point_up"), listOf("attention", "one")))
      add(LexiconEntry("mp-thumb-up", "Thumb_Up", "نعم", "Yes/Good",
        "response", listOf("thumbs_up", "thumb_up"), listOf("agreement", "positive")))
      add(LexiconEntry("mp-thumb-down", "Thumb_Down", "لا", "No/Bad",
        "response", listOf("thumbs_down", "thumb_down"), listOf("disagreement", "negative")))
      add(LexiconEntry("mp-victory", "Victory", "سلام", "Peace",
        "gesture", listOf("victory", "peace", "v_sign"), listOf("greeting", "number_two")))
      add(LexiconEntry("mp-iloveyou", "ILoveYou", "أحبك", "I love you",
        "emotion", listOf("i_love_you", "ily"), listOf("emotion", "affection")))

      // ── Legacy sign database entries (not yet recognized by the model) ──
      // [EXPANSION_NEEDED] These require a custom-trained model to recognize.
      // They are included so the lexicon is ready when the model is expanded.
      add(LexiconEntry("esl-hello", "hello", "مرحبا", "Hello", "greeting",
        listOf("hi", "greet"), listOf("greeting")))
      add(LexiconEntry("esl-good-morning", "good_morning", "صباح الخير", "Good morning",
        "greeting", listOf("morning"), listOf("greeting", "time")))
      add(LexiconEntry("esl-good-evening", "good_evening", "مساء الخير", "Good evening",
        "greeting", listOf("evening"), listOf("greeting", "time")))
      add(LexiconEntry("esl-welcome", "welcome", "أهلا وسهلا", "Welcome",
        "greeting", listOf("ahlan"), listOf("greeting")))
      add(LexiconEntry("esl-goodbye", "goodbye", "وداعا", "Goodbye",
        "greeting", listOf("bye", "farewell"), listOf("greeting")))
      add(LexiconEntry("esl-thank-you", "thank_you", "شكرا", "Thank you",
        "common", listOf("thanks", "shukran"), listOf("courtesy")))
      add(LexiconEntry("esl-please", "please", "من فضلك", "Please",
        "common", listOf("min_fadlak"), listOf("courtesy")))
      add(LexiconEntry("esl-sorry", "sorry", "آسف", "Sorry",
        "common", listOf("apology"), listOf("courtesy")))
      add(LexiconEntry("esl-excuse-me", "excuse_me", "عفوا", "Excuse me",
        "common", listOf("pardon"), listOf("courtesy")))
      add(LexiconEntry("esl-how-are-you", "how_are_you", "كيف حالك؟", "How are you?",
        "common", listOf("how_are_you"), listOf("question", "greeting")))
      add(LexiconEntry("esl-fine", "i_am_fine", "أنا بخير", "I'm fine",
        "common", listOf("fine", "good"), listOf("response")))
      add(LexiconEntry("esl-help", "help", "مساعدة", "Help",
        "needs", listOf("assist", "aid"), listOf("emergency", "request")))
      add(LexiconEntry("esl-water", "water", "ماء", "Water",
        "needs", listOf("drink"), listOf("request", "basic_need")))
      add(LexiconEntry("esl-food", "food", "طعام", "Food",
        "needs", listOf("eat", "meal"), listOf("request", "basic_need")))
      add(LexiconEntry("esl-bathroom", "bathroom", "حمام", "Bathroom",
        "needs", listOf("toilet", "restroom"), listOf("request", "basic_need")))
      add(LexiconEntry("esl-medicine", "medicine", "دواء", "Medicine",
        "needs", listOf("drug", "pill"), listOf("health")))
      add(LexiconEntry("esl-doctor", "doctor", "طبيب", "Doctor",
        "needs", listOf("physician"), listOf("health")))
      add(LexiconEntry("esl-hospital", "hospital", "مستشفى", "Hospital",
        "needs", listOf("clinic"), listOf("health", "emergency")))
      add(LexiconEntry("esl-yes", "yes", "نعم", "Yes",
        "response", listOf("okay", "affirmative"), listOf("response")))
      add(LexiconEntry("esl-no", "no", "لا", "No",
        "response", listOf("nope", "negative"), listOf("response")))
      add(LexiconEntry("esl-ok", "ok", "حسنا", "OK",
        "response", listOf("okay", "fine"), listOf("response")))
      add(LexiconEntry("esl-where", "where", "أين", "Where",
        "direction", listOf("location"), listOf("question", "direction")))
      add(LexiconEntry("esl-here", "here", "هنا", "Here",
        "direction", listOf("this_place"), listOf("direction")))
      add(LexiconEntry("esl-there", "there", "هناك", "There",
        "direction", listOf("that_place"), listOf("direction")))
      add(LexiconEntry("esl-left", "left", "يسار", "Left",
        "direction", listOf("turn_left"), listOf("direction")))
      add(LexiconEntry("esl-right", "right", "يمين", "Right",
        "direction", listOf("turn_right"), listOf("direction")))
      add(LexiconEntry("esl-emergency", "emergency", "طوارئ", "Emergency",
        "emergency", listOf("urgent", "sos"), listOf("emergency"),
        confidenceThresholdOverride = 0.60f))
      add(LexiconEntry("esl-police", "police", "شرطة", "Police",
        "emergency", listOf("officer"), listOf("emergency")))
      add(LexiconEntry("esl-wheelchair", "wheelchair", "كرسي متحرك", "Wheelchair",
        "accessibility", listOf("chair"), listOf("accessibility")))
      add(LexiconEntry("esl-elevator", "elevator", "مصعد", "Elevator",
        "accessibility", listOf("lift"), listOf("accessibility")))

      // ── ESL LSTM model signs (66 Egyptian Sign Language signs) ────────
      add(LexiconEntry("esl-lstm-ابيض", "ابيض", "أبيض", "White", "color"))
      add(LexiconEntry("esl-lstm-احمر", "احمر", "أحمر", "Red", "color"))
      add(LexiconEntry("esl-lstm-احنا", "احنا", "احنا", "We", "pronoun"))
      add(LexiconEntry("esl-lstm-ازاى", "ازاى", "ازاى", "How", "question"))
      add(LexiconEntry("esl-lstm-ازرق", "ازرق", "أزرق", "Blue", "color"))
      add(LexiconEntry("esl-lstm-اسف", "اسف", "آسف", "Sorry", "common"))
      add(LexiconEntry("esl-lstm-اسم", "اسم", "اسم", "Name", "common"))
      add(LexiconEntry("esl-lstm-اسود", "اسود", "أسود", "Black", "color"))
      add(LexiconEntry("esl-lstm-اكيد", "اكيد", "أكيد", "Sure", "response"))
      add(LexiconEntry("esl-lstm-الاحد", "الاحد", "الأحد", "Sunday", "day"))
      add(LexiconEntry("esl-lstm-الاربعاء", "الاربعاء", "الأربعاء", "Wednesday", "day"))
      add(LexiconEntry("esl-lstm-الاسرة", "الاسرة", "الأسرة", "Family", "family"))
      add(LexiconEntry("esl-lstm-الثلاثاء", "الثلاثاء", "الثلاثاء", "Tuesday", "day"))
      add(LexiconEntry("esl-lstm-الجمعة", "الجمعة", "الجمعة", "Friday", "day"))
      add(LexiconEntry("esl-lstm-الجو", "الجو", "الجو", "Weather", "common"))
      add(LexiconEntry("esl-lstm-الخميس", "الخميس", "الخميس", "Thursday", "day"))
      add(LexiconEntry("esl-lstm-السبت", "السبت", "السبت", "Saturday", "day"))
      add(LexiconEntry("esl-lstm-الوان", "الوان", "ألوان", "Colors", "color"))
      add(LexiconEntry("esl-lstm-امتى", "امتى", "امتى", "When", "question"))
      add(LexiconEntry("esl-lstm-انترنت", "انترنت", "انترنت", "Internet", "common"))
      add(LexiconEntry("esl-lstm-ايه", "ايه", "ايه", "What", "question"))
      add(LexiconEntry("esl-lstm-بتاعه", "بتاعه", "بتاعه", "His/Hers", "pronoun"))
      add(LexiconEntry("esl-lstm-بتاعى", "بتاعى", "بتاعى", "Mine", "pronoun"))
      add(LexiconEntry("esl-lstm-بحبك", "بحبك", "بحبك", "I love you", "emotion"))
      add(LexiconEntry("esl-lstm-برتقالي", "برتقالي", "برتقالي", "Orange", "color"))
      add(LexiconEntry("esl-lstm-بعد", "بعد", "بعد", "After", "common"))
      add(LexiconEntry("esl-lstm-بنفسجى", "بنفسجى", "بنفسجي", "Purple", "color"))
      add(LexiconEntry("esl-lstm-جد", "جد", "جد", "Grandfather", "family"))
      add(LexiconEntry("esl-lstm-جمب", "جمب", "جنب", "Next to", "direction"))
      add(LexiconEntry("esl-lstm-جميل", "جميل", "جميل", "Beautiful", "description"))
      add(LexiconEntry("esl-lstm-جيد", "جيد - كويس", "كويس", "Good", "response",
        aliases = listOf("كويس", "جيد")))
      add(LexiconEntry("esl-lstm-خطوبة", "خطوبة", "خطوبة", "Engagement", "social"))
      add(LexiconEntry("esl-lstm-خفيف", "خفيف", "خفيف", "Light/Easy", "description"))
      add(LexiconEntry("esl-lstm-دكتور", "دكتور", "دكتور", "Doctor", "job"))
      add(LexiconEntry("esl-lstm-رقم", "رقم", "رقم", "Number", "common"))
      add(LexiconEntry("esl-lstm-سماعة", "سماعة", "سماعة", "Headphones", "common"))
      add(LexiconEntry("esl-lstm-شغل", "شغل", "شغل", "Work", "common"))
      add(LexiconEntry("esl-lstm-شكرا", "شكرا", "شكرا", "Thank you", "common"))
      add(LexiconEntry("esl-lstm-طويل", "طويل", "طويل", "Tall/Long", "description"))
      add(LexiconEntry("esl-lstm-طيار", "طيار", "طيار", "Pilot", "job"))
      add(LexiconEntry("esl-lstm-عامل", "عامل", "عامل", "Worker", "job"))
      add(LexiconEntry("esl-lstm-غير", "غير", "غير", "Other/Different", "common"))
      add(LexiconEntry("esl-lstm-فاضي", "فاضي", "فاضي", "Free/Available", "description"))
      add(LexiconEntry("esl-lstm-فقير", "فقير", "فقير", "Poor", "description"))
      add(LexiconEntry("esl-lstm-فلوس", "فلوس", "فلوس", "Money", "common"))
      add(LexiconEntry("esl-lstm-فوق", "فوق", "فوق", "Above/Up", "direction"))
      add(LexiconEntry("esl-lstm-فى", "فى", "في", "In", "common"))
      add(LexiconEntry("esl-lstm-فين", "فين", "فين", "Where", "question"))
      add(LexiconEntry("esl-lstm-قبيح", "قبيح", "قبيح", "Ugly", "description"))
      add(LexiconEntry("esl-lstm-قصير", "قصير", "قصير", "Short", "description"))
      add(LexiconEntry("esl-lstm-كام-كمية", "كام كمية", "كام", "How much", "question",
        aliases = listOf("كمية")))
      add(LexiconEntry("esl-lstm-كام-عدد", "كام للعدد", "كام", "How many", "question"))
      add(LexiconEntry("esl-lstm-كلية", "كلية", "كلية", "College", "common"))
      add(LexiconEntry("esl-lstm-لغة", "لغة", "لغة", "Language", "common"))
      add(LexiconEntry("esl-lstm-ليه", "ليه", "ليه", "Why", "question"))
      add(LexiconEntry("esl-lstm-متفائل", "متفائل", "متفائل", "Optimistic", "emotion"))
      add(LexiconEntry("esl-lstm-مشغول", "مشغول", "مشغول", "Busy", "description"))
      add(LexiconEntry("esl-lstm-مشكلة", "مشكلة", "مشكلة", "Problem", "common"))
      add(LexiconEntry("esl-lstm-مطلق", "مطلق", "مطلق", "Divorced", "social"))
      add(LexiconEntry("esl-lstm-مع-السلامة", "مع السلامة", "مع السلامة", "Goodbye", "greeting"))
      add(LexiconEntry("esl-lstm-معلم", "معلم", "معلم", "Teacher", "job"))
      add(LexiconEntry("esl-lstm-ممكن", "ممكن", "ممكن", "Maybe/Possible", "response"))
      add(LexiconEntry("esl-lstm-مهندس", "مهندس", "مهندس", "Engineer", "job"))
      add(LexiconEntry("esl-lstm-مين", "مين", "مين", "Who", "question"))
      add(LexiconEntry("esl-lstm-نشيط", "نشيط", "نشيط", "Active/Energetic", "description"))
      add(LexiconEntry("esl-lstm-و", "و", "و", "And", "common"))
    }
    seedEntries.forEach(::indexEntry)
    Log.d(TAG, "Loaded ${seedEntries.size} seed lexicon entries.")
  }

  private fun loadSeedPhraseRules() {
    phraseRules += listOf(
      // Greeting sequences
      PhraseRule("phrase-hello-how", listOf("Open_Palm", "Pointing_Up"),
        "مرحبا، كيف حالك؟", "Hello, how are you?", "greeting", priority = 10),
      PhraseRule("phrase-yes-thanks", listOf("Thumb_Up", "Open_Palm"),
        "نعم، شكرا", "Yes, thank you", "common", priority = 10),
      PhraseRule("phrase-no-sorry", listOf("Thumb_Down", "Closed_Fist"),
        "لا، آسف", "No, sorry", "common", priority = 10),
      PhraseRule("phrase-peace-love", listOf("Victory", "ILoveYou"),
        "سلام ومحبة", "Peace and love", "emotion", priority = 10),
      PhraseRule("phrase-stop-attention", listOf("Closed_Fist", "Pointing_Up"),
        "توقف وانتبه", "Stop and pay attention", "gesture", priority = 10),
      PhraseRule("phrase-hello-love", listOf("Open_Palm", "ILoveYou"),
        "مرحبا، أحبك", "Hello, I love you", "emotion", priority = 10),
      PhraseRule("phrase-yes-good", listOf("Thumb_Up", "Thumb_Up"),
        "نعم، ممتاز", "Yes, excellent", "response", priority = 5),
      PhraseRule("phrase-no-no", listOf("Thumb_Down", "Thumb_Down"),
        "لا بالتأكيد", "Definitely no", "response", priority = 5),
      PhraseRule("phrase-attention-yes", listOf("Pointing_Up", "Thumb_Up"),
        "انتبه، هذا صحيح", "Pay attention, that's correct", "gesture", priority = 8),

      // ── Egyptian Sign Language (ESL LSTM model) phrase rules ──────────
      // Question phrases
      PhraseRule("esl-how-weather", listOf("ازاى", "الجو"),
        "ازاى الجو؟", "How's the weather?", "question", priority = 12),
      PhraseRule("esl-what-name", listOf("ايه", "اسم"),
        "اسمك ايه؟", "What's your name?", "question", priority = 12),
      PhraseRule("esl-where-college", listOf("فين", "كلية"),
        "الكلية فين؟", "Where is the college?", "question", priority = 12),
      PhraseRule("esl-where-doctor", listOf("فين", "دكتور"),
        "الدكتور فين؟", "Where is the doctor?", "question", priority = 12),
      PhraseRule("esl-when-work", listOf("امتى", "شغل"),
        "الشغل امتى؟", "When is work?", "question", priority = 12),
      PhraseRule("esl-how-much-money", listOf("كام كمية", "فلوس"),
        "كام فلوس؟", "How much money?", "question", priority = 12),
      PhraseRule("esl-who-teacher", listOf("مين", "معلم"),
        "مين المعلم؟", "Who is the teacher?", "question", priority = 12),
      PhraseRule("esl-why-busy", listOf("ليه", "مشغول"),
        "ليه مشغول؟", "Why are you busy?", "question", priority = 12),

      // Greeting / farewell
      PhraseRule("esl-goodbye-thanks", listOf("مع السلامة", "شكرا"),
        "مع السلامة وشكرا", "Goodbye and thank you", "greeting", priority = 10),

      // Description phrases
      PhraseRule("esl-weather-beautiful", listOf("الجو", "جميل"),
        "الجو جميل", "The weather is beautiful", "description", priority = 10),
      PhraseRule("esl-good-work", listOf("جيد - كويس", "شغل"),
        "شغل كويس", "Good work", "common", priority = 10),
      PhraseRule("esl-we-family", listOf("احنا", "الاسرة"),
        "احنا أسرة", "We are family", "family", priority = 10),
      PhraseRule("esl-sorry-problem", listOf("اسف", "مشكلة"),
        "آسف، في مشكلة", "Sorry, there's a problem", "common", priority = 10),
      PhraseRule("esl-sure-possible", listOf("اكيد", "ممكن"),
        "أكيد ممكن", "Sure, it's possible", "response", priority = 10),
      PhraseRule("esl-free-after", listOf("فاضي", "بعد"),
        "فاضي بعدين", "Free later", "common", priority = 10),

      // Day phrases
      PhraseRule("esl-friday-free", listOf("الجمعة", "فاضي"),
        "فاضي يوم الجمعة", "Free on Friday", "time", priority = 10),
    )
    Log.d(TAG, "Loaded ${phraseRules.size} phrase rules.")
  }
}
