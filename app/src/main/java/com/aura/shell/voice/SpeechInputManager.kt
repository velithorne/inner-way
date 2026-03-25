package com.aura.shell.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Wraps [SpeechRecognizer] for foreground-only capture. No background listening.
 */
class SpeechInputManager(
    context: Context,
) {
    private val appContext = context.applicationContext
    private var recognizer: SpeechRecognizer? = null

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(appContext)

    fun startListening(
        onReady: () -> Unit,
        onPartialResult: (String) -> Unit,
        onFinalResult: (String) -> Unit,
        onError: (userMessage: String) -> Unit,
    ) {
        stopListening()

        if (!isAvailable()) {
            onError("Voice input isn’t available on this device. Try typing.")
            return
        }

        val sr = SpeechRecognizer.createSpeechRecognizer(appContext)
        recognizer = sr

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1_200)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1_500)
        }

        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                onReady()
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                stopListening()
                onError(errorToMessage(error))
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()?.trim().orEmpty()
                stopListening()
                if (text.isEmpty()) {
                    onError("No speech detected. Try again or type.")
                } else {
                    onFinalResult(text)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()?.trim().orEmpty()
                if (text.isNotEmpty()) {
                    onPartialResult(text)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        sr.startListening(intent)
    }

    fun stopListening() {
        recognizer?.let { r ->
            try {
                r.stopListening()
            } catch (_: Exception) { }
            try {
                r.destroy()
            } catch (_: Exception) { }
        }
        recognizer = null
    }

    private fun errorToMessage(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "Couldn’t use the microphone. Check permissions."
        SpeechRecognizer.ERROR_CLIENT -> "Voice input was interrupted."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
        SpeechRecognizer.ERROR_NETWORK -> "Network required for this voice input. Try typing."
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Voice timed out. Try again."
        SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected. Try again or type."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice is busy. Try again."
        SpeechRecognizer.ERROR_SERVER -> "Voice service error. Try typing."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech heard. Try again."
        else -> "Voice didn’t work. Try typing your command."
    }
}
