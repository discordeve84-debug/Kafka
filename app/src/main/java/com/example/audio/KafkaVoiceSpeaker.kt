package com.example.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class KafkaVoiceSpeaker(context: Context) {

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var isReady = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    init {
        tts = TextToSpeech(appContext) { status ->
            isReady = status == TextToSpeech.SUCCESS
            if (isReady) {
                val idLocale = Locale.forLanguageTag("id-ID")
                val res = tts?.setLanguage(idLocale)
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.US)
                }
                tts?.setSpeechRate(1.02f)
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        _isSpeaking.value = false
                    }
                })
            }
        }
    }

    fun speak(rawText: String, preferredLanguage: String = "Indonesia") {
        if (!isReady) return
        val cleaned = rawText
            .replace(Regex("\\[([JW]?\\d+)\\]"), "")
            .replace(Regex("[*_`#>]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(1800)
        if (cleaned.isBlank()) return

        val targetLocale = if (preferredLanguage == "English") {
            Locale.US
        } else {
            Locale.forLanguageTag("id-ID")
        }
        val langRes = tts?.setLanguage(targetLocale)
        if (langRes == TextToSpeech.LANG_MISSING_DATA || langRes == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts?.setLanguage(Locale.US)
        }
        tts?.speak(cleaned, TextToSpeech.QUEUE_FLUSH, null, "kafka_tts_${System.currentTimeMillis()}")
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
        _isSpeaking.value = false
    }
}
