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

    /**
     * Controls timeouts and error recovery. Hands-free follow-up needs longer silence budgets
     * than a one-shot wake word; otherwise [SpeechRecognizer] often returns NO_MATCH and the UI
     * jumps back to "Listening for Aura" before the user finishes speaking.
     */
    enum class ListenProfile {
        /** Single utterance / command bar mic (shorter end-of-speech). */
        Standard,

        /** Passive: user should only say "Aura" (or very short wake). */
        PassiveWake,

        /** Passive: second pass after wake — full phrases like "open settings". */
        PassiveFollowUp,
    }

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(appContext)

    fun startListening(
        onReady: () -> Unit,
        onPartialResult: (String) -> Unit,
        onFinalResult: (String) -> Unit,
        onError: (userMessage: String) -> Unit,
        profile: ListenProfile = ListenProfile.Standard,
    ) {
        stopListening()

        if (!isAvailable()) {
            onError("Voice input isn’t available on this device. Try typing.")
            return
        }

        val (completeSilenceMs, possiblyCompleteMs, usePartialFallback) = when (profile) {
            ListenProfile.Standard -> Triple(1_200L, 1_500L, false)
            ListenProfile.PassiveWake -> Triple(1_600L, 2_000L, true)
            ListenProfile.PassiveFollowUp -> Triple(3_800L, 5_000L, true)
        }

        val sr = SpeechRecognizer.createSpeechRecognizer(appContext)
        recognizer = sr

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                completeSilenceMs,
            )
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                possiblyCompleteMs,
            )
        }

        var lastPartial = ""

        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                onReady()
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                if (usePartialFallback &&
                    lastPartial.isNotBlank() &&
                    error in partialRecoverableErrors
                ) {
                    val toUse = lastPartial.trim()
                    stopListening()
                    if (toUse.isNotEmpty()) {
                        onFinalResult(toUse)
                    } else {
                        onError(errorToMessage(error))
                    }
                    return
                }
                stopListening()
                onError(errorToMessage(error))
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()?.trim().orEmpty()
                stopListening()
                val toUse = text.ifBlank { lastPartial.trim() }
                if (toUse.isEmpty()) {
                    onError("No speech detected. Try again or type.")
                } else {
                    onFinalResult(toUse)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()?.trim().orEmpty()
                if (text.isNotEmpty()) {
                    lastPartial = text
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

    companion object {
        private val partialRecoverableErrors = setOf(
            SpeechRecognizer.ERROR_NO_MATCH,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
        )
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
