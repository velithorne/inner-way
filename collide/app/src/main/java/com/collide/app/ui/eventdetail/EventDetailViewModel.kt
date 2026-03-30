package com.collide.app.ui.eventdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.collide.app.data.repository.EventRepository
import com.collide.app.data.settings.AppSettings
import com.collide.app.domain.engine.collision.RecipeSerializer
import com.collide.app.domain.engine.collision.TransformationEngine
import com.collide.app.domain.engine.normalize.CodeNormalizer
import com.collide.app.domain.engine.replay.EventReplayer
import com.collide.app.domain.model.ReplayStatus
import com.collide.app.domain.model.SavedEvent
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class EventDetailUiState(
    val event: SavedEvent? = null,
    val isLoading: Boolean = true,
    val replayResult: EventReplayer.ReplayResult? = null,
    val isReplaying: Boolean = false,
    val shareSummary: String? = null
)

class EventDetailViewModel(
    private val repo: EventRepository,
    private val settings: AppSettings,
    private val replayer: EventReplayer
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventDetailUiState())
    val uiState: StateFlow<EventDetailUiState> = _uiState.asStateFlow()

    fun loadEvent(eventId: String) {
        viewModelScope.launch {
            val event = repo.getEventById(eventId)
            _uiState.update { it.copy(event = event, isLoading = false) }
        }
    }

    fun replayEvent(originalTextA: String, originalTextB: String?) {
        val event = _uiState.value.event ?: return
        _uiState.update { it.copy(isReplaying = true) }

        viewModelScope.launch {
            val config = settings.configFlow.first()
            val result = replayer.replay(event, originalTextA, originalTextB, config)
            repo.updateReplayStatus(event.id, result.status)
            _uiState.update { state ->
                state.copy(
                    isReplaying = false,
                    replayResult = result,
                    event = state.event?.copy(replayStatus = result.status)
                )
            }
        }
    }

    fun buildShareSummary() {
        val event = _uiState.value.event ?: return
        val summary = buildString {
            appendLine("=== COLLIDE Event Export ===")
            appendLine("ID: ${event.id}")
            appendLine("Timestamp: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date(event.timestamp))}")
            appendLine("Type: ${event.eventType.displayName}")
            appendLine("Mode: ${event.mode.name}")
            appendLine("Input A: ${event.inputLabelA} (${event.inputProfileA})")
            event.inputLabelB?.let { appendLine("Input B: $it (${event.inputProfileB ?: "??"})") }
            appendLine()
            appendLine("Candidate Preview:")
            appendLine(event.candidatePreview.take(400))
            appendLine()
            appendLine("Notes: ${event.notes}")
            appendLine("Replayable: ${event.replayable}")
            appendLine("Replay Status: ${event.replayStatus.name}")
            appendLine()
            appendLine("Detector Summary:")
            appendLine(event.detectorSummaryJson.take(300))
            appendLine()
            appendLine("--- Exported from COLLIDE Phase 1 ---")
        }
        _uiState.update { it.copy(shareSummary = summary) }
    }

    fun clearShareSummary() = _uiState.update { it.copy(shareSummary = null) }

    class Factory(
        private val repo: EventRepository,
        private val settings: AppSettings,
        private val replayer: EventReplayer
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            EventDetailViewModel(repo, settings, replayer) as T
    }
}
