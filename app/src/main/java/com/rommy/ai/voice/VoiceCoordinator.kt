package com.rommy.ai.voice

import android.content.Context
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Keeps speech serialized and makes switching to a neural engine later transparent to the UI. */
class VoiceCoordinator(context: Context) {
    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private val mutex = Mutex()
    private val engine: SpeechEngine = AndroidTtsEngine(context)
    private var job: Job? = null

    fun speak(text: String, locale: Locale = Locale.getDefault(), style: SpeechStyle = SpeechStyle.DEFAULT) {
        if (text.isBlank()) return
        job?.cancel()
        job = scope.launch {
            mutex.withLock {
                engine.speak(normalize(text), locale, style)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        engine.stop()
    }

    fun release() {
        stop()
        engine.release()
    }

    private fun normalize(text: String): String = text
        .replace(Regex("\\s+"), " ")
        .replace("…", ".")
        .trim()
}
