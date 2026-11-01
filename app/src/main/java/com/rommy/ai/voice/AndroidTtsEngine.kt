package com.rommy.ai.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

/** Immediate fallback while the neural voice packs are being integrated. */
class AndroidTtsEngine(context: Context) : SpeechEngine, TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    private var ready = false

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
    }

    override suspend fun speak(text: String, locale: Locale, style: SpeechStyle) {
        if (!ready || text.isBlank()) return
        tts.language = locale
        tts.setSpeechRate(style.rate)
        tts.setPitch(style.pitch)
        suspendCancellableCoroutine { continuation ->
            val utteranceId = "rommy-${System.nanoTime()}"
            val listener = object : TextToSpeech.OnUtteranceProgressListener() {
                override fun onStart(id: String?) = Unit
                override fun onDone(id: String?) {
                    if (id == utteranceId && continuation.isActive) continuation.resume(Unit)
                }
                override fun onError(id: String?) {
                    if (id == utteranceId && continuation.isActive) continuation.resume(Unit)
                }
            }
            tts.setOnUtteranceProgressListener(listener)
            tts.speak(text, TextToSpeech.QUEUE_ADD, null, utteranceId)
            continuation.invokeOnCancellation { tts.stop() }
        }
    }

    override fun stop() = tts.stop()

    override fun release() = tts.shutdown()
}
