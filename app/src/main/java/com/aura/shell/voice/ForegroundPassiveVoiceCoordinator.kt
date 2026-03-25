package com.aura.shell.voice

import com.aura.shell.command.CommandInputSource
import com.aura.shell.command.PipelineResult
import com.aura.shell.command.WakePhraseProcessor
import com.aura.shell.command.WakeProcessResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground-only passive listening: wake phrase + follow-up command.
 * Stops when [setForegroundVisible] is false.
 */
class ForegroundPassiveVoiceCoordinator(
    private val scope: CoroutineScope,
    private val speech: SpeechInputManager,
    private val isPassiveEnabled: () -> Boolean,
    private val submitCommand: suspend (String, CommandInputSource) -> PipelineResult,
    private val onState: (HandsFreeUiState) -> Unit,
) {
    private var loopJob: Job? = null
    private var foregroundVisible = false

    fun setForegroundVisible(visible: Boolean) {
        foregroundVisible = visible
        if (!visible) {
            loopJob?.cancel()
            loopJob = null
            speech.stopListening()
            onState(if (isPassiveEnabled()) HandsFreeUiState.Armed else HandsFreeUiState.Disabled)
        } else if (isPassiveEnabled()) {
            scheduleArm()
        } else {
            onState(HandsFreeUiState.Disabled)
        }
    }

    fun onPassiveSettingChanged(enabled: Boolean) {
        if (!enabled) {
            loopJob?.cancel()
            loopJob = null
            speech.stopListening()
            onState(HandsFreeUiState.Disabled)
        } else if (foregroundVisible) {
            scheduleArm()
        }
    }

    private fun scheduleArm() {
        loopJob?.cancel()
        loopJob = scope.launch {
            if (!foregroundVisible || !isPassiveEnabled()) return@launch
            onState(HandsFreeUiState.Armed)
            delay(400)
            if (!isActive || !foregroundVisible || !isPassiveEnabled()) return@launch
            listenForWake(followUpMode = false)
        }
    }

    private fun listenForWake(followUpMode: Boolean) {
        if (!foregroundVisible || !isPassiveEnabled()) {
            onState(if (isPassiveEnabled()) HandsFreeUiState.Armed else HandsFreeUiState.Disabled)
            return
        }
        onState(
            if (followUpMode) HandsFreeUiState.ListeningForCommand
            else HandsFreeUiState.ListeningForWake,
        )
        speech.startListening(
            onReady = { },
            onPartialResult = { },
            onFinalResult = { raw ->
                scope.launch {
                    handleTranscript(raw, followUpMode)
                }
            },
            onError = { msg ->
                scope.launch {
                    onState(HandsFreeUiState.Error(msg))
                    delay(1200)
                    if (foregroundVisible && isPassiveEnabled()) {
                        onState(HandsFreeUiState.Timeout)
                        delay(400)
                        scheduleArm()
                    } else {
                        onState(HandsFreeUiState.Disabled)
                    }
                }
            },
        )
    }

    private suspend fun handleTranscript(raw: String, followUpMode: Boolean) {
        if (!foregroundVisible) return
        when (val w = WakePhraseProcessor.classify(raw, followUpMode)) {
            WakeProcessResult.Empty -> {
                onState(HandsFreeUiState.Timeout)
                delay(400)
                scheduleArm()
            }
            WakeProcessResult.WakeOnly -> {
                onState(HandsFreeUiState.WakeDetected)
                delay(350)
                if (!foregroundVisible || !isPassiveEnabled()) return
                listenForWake(followUpMode = true)
            }
            is WakeProcessResult.Command -> {
                onState(HandsFreeUiState.Processing)
                val result = submitCommand(w.text, CommandInputSource.PassiveVoice)
                when (result) {
                    is PipelineResult.Launched -> {
                        onState(HandsFreeUiState.Success("Opened ${result.displayLabel}"))
                    }
                    is PipelineResult.OpenDrawer,
                    is PipelineResult.CloseDrawer,
                    -> {
                        onState(HandsFreeUiState.Success("Done"))
                    }
                    is PipelineResult.SurfaceOnly -> {
                        onState(HandsFreeUiState.Success("Ready"))
                    }
                    is PipelineResult.Error -> {
                        onState(HandsFreeUiState.Error(result.message))
                    }
                }
                delay(900)
                if (foregroundVisible && isPassiveEnabled()) scheduleArm()
                else onState(HandsFreeUiState.Disabled)
            }
            WakeProcessResult.NotWake -> {
                if (followUpMode) {
                    onState(HandsFreeUiState.Processing)
                    submitCommand(raw, CommandInputSource.PassiveVoice)
                    delay(900)
                    if (foregroundVisible && isPassiveEnabled()) scheduleArm()
                } else {
                    onState(HandsFreeUiState.Timeout)
                    delay(500)
                    scheduleArm()
                }
            }
        }
    }

    fun stop() {
        loopJob?.cancel()
        loopJob = null
        speech.stopListening()
    }
}
