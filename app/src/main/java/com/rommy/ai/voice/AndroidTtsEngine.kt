package com.rommy.ai.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

/** Immediate fallback while the neural voice packs are being integrated. */
class AndroidTtsEngine(context: Context) : SpeechEngine, TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    private val initialization = CompletableDeferred<Boolean>()

    override fun onInit(status: Int) {
        initialization.complete(status == TextToSpeech.SUCCESS)
    }

    override suspend fun speak(text: String, locale: Locale, style: SpeechStyle) {
        if (text.isBlank()) return
        val ready = withTimeoutOrNull(15_000) { initialization.await() } ?: false
        if (!ready) return
        val requestedLocale = if (tts.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE) locale else Locale.getDefault()
        tts.language = requestedLocale
        tts.setSpeechRate(style.rate)
        tts.setPitch(style.pitch)
        suspendCancellableCoroutine { continuation ->
            val utteranceId = "rommy-${System.nanoTime()}"
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) = Unit
                override fun onDone(id: String?) {
                    if (id == utteranceId && continuation.isActive) continuation.resume(Unit)
                }
                override fun onError(id: String?) {
                    if (id == utteranceId && continuation.isActive) continuation.resume(Unit)
                }
            })
            tts.speak(text, TextToSpeech.QUEUE_ADD, null, utteranceId)
            continuation.invokeOnCancellation { tts.stop() }
        }
    }

    override fun stop() {
        tts.stop()
    }

    override fun release() {
        initialization.complete(false)
        tts.shutdown()
    }
}
