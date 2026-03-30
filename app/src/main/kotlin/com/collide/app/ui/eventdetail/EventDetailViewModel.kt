package com.collide.app.ui.eventdetail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.collide.app.data.repository.EventRepository
import com.collide.app.domain.engine.ReplayEngine
import com.collide.app.domain.model.ReplayResult
import com.collide.app.domain.model.ReplayStatus
import com.collide.app.domain.model.SavedEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class EventDetailUiState(
    val event: SavedEvent? = null,
    val replayResult: ReplayResult? = null,
    val isReplaying: Boolean = false,
    val replayError: String? = null
)

class EventDetailViewModel(
    private val repository: EventRepository,
    private val eventId: Long
) : ViewModel() {

    private val replayEngine = ReplayEngine()

    private val _uiState = MutableStateFlow(EventDetailUiState())
    val uiState: StateFlow<EventDetailUiState> = _uiState

    init {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(event = repository.getEventById(eventId))
        }
    }

    fun startReplay(context: Context) {
        val event = _uiState.value.event ?: return
        _uiState.value = _uiState.value.copy(isReplaying = true, replayError = null, replayResult = null)

        viewModelScope.launch(Dispatchers.Default) {
            try {
                // Try to reload the original file from assets if it's a fixture
                val bytes = tryLoadFixtureBytes(context, event.inputFileName)
                    ?: run {
                        _uiState.value = _uiState.value.copy(
                            isReplaying = false,
                            replayError = "Cannot replay: original file bytes not available. " +
                                         "Load the same file in the Collider and run replay from there."
                        )
                        return@launch
                    }

                val result = replayEngine.replay(bytes, event)
                repository.updateReplayStatus(event.id, result.status)
                _uiState.value = _uiState.value.copy(
                    isReplaying = false,
                    replayResult = result,
                    event = repository.getEventById(event.id)
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isReplaying = false,
                    replayError = e.message ?: "Replay failed"
                )
            }
        }
    }

    private suspend fun tryLoadFixtureBytes(context: Context, fileName: String): ByteArray? {
        return try {
            // Try direct asset name match (fixtures/sample_xxx.txt → sample_xxx.txt)
            val assetName = "fixtures/$fileName"
            context.assets.open(assetName).readBytes()
        } catch (_: Exception) {
            try {
                context.assets.open("fixtures/${fileName.substringAfterLast('/')}").readBytes()
            } catch (_: Exception) {
                null
            }
        }
    }

    class Factory(
        private val repository: EventRepository,
        private val eventId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            EventDetailViewModel(repository, eventId) as T
    }
}
