package com.athar.accessibilitymapping.data.signlanguage

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Optional text-to-speech component that reads translated Arabic (or English)
 * text aloud using Android's built-in TTS engine.
 */
class ArabicTextSpeaker(context: Context) : AutoCloseable {
  companion object {
    private const val TAG = "ArabicTextSpeaker"
    private val ARABIC_LOCALE = Locale("ar", "EG")
  }

  private var tts: TextToSpeech? = null
  private var isReady = false
  private var arabicAvailable = false
  private var englishAvailable = false

  init {
    tts = TextToSpeech(context.applicationContext) { status ->
      if (status == TextToSpeech.SUCCESS) {
        isReady = true
        arabicAvailable = tts?.isLanguageAvailable(ARABIC_LOCALE).let {
          it == TextToSpeech.LANG_AVAILABLE || it == TextToSpeech.LANG_COUNTRY_AVAILABLE
        }
        englishAvailable = tts?.isLanguageAvailable(Locale.ENGLISH).let {
          it == TextToSpeech.LANG_AVAILABLE || it == TextToSpeech.LANG_COUNTRY_AVAILABLE
        }
        Log.d(TAG, "TTS ready. Arabic=$arabicAvailable, English=$englishAvailable")
      } else {
        Log.w(TAG, "TTS initialization failed with status=$status")
      }
    }
  }

  /** Speak Arabic text. Falls back to English if Arabic TTS is unavailable. */
  fun speakArabic(text: String) {
    if (!isReady || text.isBlank()) return
    val engine = tts ?: return
    if (arabicAvailable) {
      engine.language = ARABIC_LOCALE
      engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "arabic-${System.currentTimeMillis()}")
    } else {
      Log.w(TAG, "Arabic TTS not available on this device.")
    }
  }

  /** Speak English text. */
  fun speakEnglish(text: String) {
    if (!isReady || text.isBlank()) return
    val engine = tts ?: return
    if (englishAvailable) {
      engine.language = Locale.ENGLISH
      engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "english-${System.currentTimeMillis()}")
    }
  }

  /** Speak the translation result — prefers Arabic, falls back to English. */
  fun speakResult(result: TranslationResult) {
    if (result.arabicText.isNotBlank() && arabicAvailable) {
      speakArabic(result.arabicText)
    } else if (result.englishText.isNotBlank() && englishAvailable) {
      speakEnglish(result.englishText)
    }
  }

  fun stop() {
    tts?.stop()
  }

  val isAvailable: Boolean get() = isReady && (arabicAvailable || englishAvailable)

  override fun close() {
    tts?.stop()
    tts?.shutdown()
    tts = null
    isReady = false
  }
}
