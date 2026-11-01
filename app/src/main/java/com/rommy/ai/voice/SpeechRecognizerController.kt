package com.rommy.ai.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/** Thin lifecycle-safe wrapper around Android's on-device/installed speech recognizer. */
class SpeechRecognizerController(
    context: Context,
    private val onResult: (String) -> Unit,
    private val onState: (ListeningState) -> Unit = {},
) {
    private val appContext = context.applicationContext
    private var recognizer: SpeechRecognizer? = null

    fun start() {
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            onState(ListeningState.UNAVAILABLE)
            return
        }
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(appContext).also { it.setRecognitionListener(listener) }
        }
        onState(ListeningState.LISTENING)
        recognizer?.startListening(intent())
    }

    fun stop() {
        recognizer?.stopListening()
        onState(ListeningState.IDLE)
    }

    fun release() {
        recognizer?.destroy()
        recognizer = null
    }

    private fun intent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale.getDefault().toLanguageTag())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) { onState(ListeningState.LISTENING) }
        override fun onBeginningOfSpeech() { onState(ListeningState.SPEAKING) }
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() { onState(ListeningState.PROCESSING) }
        override fun onError(error: Int) { onState(ListeningState.ERROR) }
        override fun onResults(results: Bundle?) {
            val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            if (text.isNotBlank()) onResult(text)
            onState(ListeningState.IDLE)
        }
        override fun onPartialResults(partialResults: Bundle?) = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }
}

enum class ListeningState { IDLE, LISTENING, SPEAKING, PROCESSING, ERROR, UNAVAILABLE }
