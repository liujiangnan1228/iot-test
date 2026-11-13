package com.example.iot_test

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * 单词发音（系统 TTS）
 */
class TtsHelper(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var ready = false
    private var speaking = false
    private val mainHandler = Handler(Looper.getMainLooper())

    var onSpeakingChanged: ((Boolean) -> Unit)? = null

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (!ready) return
        val result = tts?.setLanguage(Locale.US)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts?.setLanguage(Locale.ENGLISH)
        }
        tts?.setSpeechRate(0.85f)
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                speaking = true
                notifyChange()
            }

            override fun onDone(utteranceId: String?) {
                speaking = false
                notifyChange()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                speaking = false
                notifyChange()
            }
        })
    }

    private fun notifyChange() {
        val callback = onSpeakingChanged ?: return
        mainHandler.post { callback(speaking) }
    }

    fun speak(text: String) {
        if (!ready || text.isEmpty()) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "word_${System.currentTimeMillis()}")
    }

    fun isReady(): Boolean = ready

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}
