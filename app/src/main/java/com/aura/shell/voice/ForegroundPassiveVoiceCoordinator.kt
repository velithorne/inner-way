package com.aura.shell.voice

import com.aura.shell.command.CommandInputSource
import com.aura.shell.command.CommandSurfaceState
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
                onState(afterSubmitState(result))
                delay(900)
                if (foregroundVisible && isPassiveEnabled()) scheduleArm()
                else onState(HandsFreeUiState.Disabled)
            }
            WakeProcessResult.NotWake -> {
                if (followUpMode) {
                    onState(HandsFreeUiState.Processing)
                    val r = submitCommand(raw, CommandInputSource.PassiveVoice)
                    onState(afterSubmitState(r))
                    delay(900)
                    if (foregroundVisible && isPassiveEnabled()) scheduleArm()
                } else {
                    // Single utterance without "Aura" prefix — still try as a command (e.g. "open camera").
                    onState(HandsFreeUiState.Processing)
                    val r = submitCommand(raw, CommandInputSource.PassiveVoice)
                    onState(afterSubmitState(r))
                    delay(900)
                    if (foregroundVisible && isPassiveEnabled()) scheduleArm()
                }
            }
        }
    }

    fun stop() {
        loopJob?.cancel()
        loopJob = null
        speech.stopListening()
    }

    private fun afterSubmitState(result: PipelineResult): HandsFreeUiState {
        return when (result) {
            is PipelineResult.Launched -> HandsFreeUiState.Success("Opened ${result.displayLabel}")
            is PipelineResult.OpenDrawer,
            is PipelineResult.CloseDrawer,
            -> HandsFreeUiState.Success("Done")
            is PipelineResult.SurfaceOnly ->
                HandsFreeUiState.Success(surfaceOnlySummary(result.surface))
            is PipelineResult.Error -> HandsFreeUiState.Error(result.message)
        }
    }

    /**
     * Hands-free cannot tap suggestions; make clear when the router needs the command screen.
     */
    private fun surfaceOnlySummary(surface: CommandSurfaceState): String {
        return when (surface) {
            is CommandSurfaceState.Empty -> "No matching command."
            is CommandSurfaceState.Success -> surface.message
            is CommandSurfaceState.Suggestions ->
                "${surface.title} — open the command bar to choose an app."
            is CommandSurfaceState.SearchResults ->
                "Found apps for “${surface.query}” — open the command bar to pick one."
            is CommandSurfaceState.RecentsList ->
                "${surface.title} — open the command bar to pick one."
            is CommandSurfaceState.Help -> "Say e.g. “Aura, open camera”."
            is CommandSurfaceState.Unknown -> surface.message
        }
    }
}
