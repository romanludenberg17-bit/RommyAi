package com.rommy.ai.voice

import java.util.Locale

/** A replaceable speech backend. System TTS is the safe fallback; neural engines can implement this interface. */
interface SpeechEngine {
    suspend fun speak(text: String, locale: Locale = Locale.getDefault(), style: SpeechStyle = SpeechStyle.DEFAULT)
    fun stop()
    fun release()
}

enum class SpeechStyle(
    val rate: Float,
    val pitch: Float,
    val pauseAfterMs: Long,
) {
    DEFAULT(1.0f, 1.0f, 80),
    CALM(0.94f, 1.02f, 130),
    CHEERFUL(1.04f, 1.08f, 65),
    SOFT(0.90f, 1.10f, 150),
    SERIOUS(0.96f, 0.94f, 105),
    SURPRISED(1.08f, 1.14f, 45),
}
