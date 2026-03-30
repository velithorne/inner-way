package com.collide.app.ui.collider

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.collide.app.data.repository.EventRepository
import com.collide.app.data.settings.CollideSettings
import com.collide.app.data.settings.CollideSettingsStore
import com.collide.app.domain.engine.CompressionColliderEngine
import com.collide.app.domain.engine.RunProgress
import com.collide.app.domain.engine.RunStatus
import com.collide.app.domain.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class ColliderUiState(
    val selectedFileName: String? = null,
    val selectedFileSize: Long = 0L,
    val selectedMimeType: String? = null,
    val fileLoadError: String? = null,
    val selectedBaseline: BaselineStrategy = BaselineStrategy.RAW_DEFLATE,
    val selectedRunMode: RunMode = RunMode.BALANCED,
    val maxCandidates: Int = RunMode.BALANCED.maxCandidates,
    val maxChainLength: Int = RunMode.BALANCED.maxChainLength,
    val progress: RunProgress = RunProgress(),
    val lastRunMessage: String? = null
)

class ColliderViewModel(
    private val repository: EventRepository,
    private val settingsStore: CollideSettingsStore
) : ViewModel() {

    private val engine = CompressionColliderEngine()
    private var runJob: Job? = null
    private var loadedBytes: ByteArray? = null

    private val _uiState = MutableStateFlow(ColliderUiState())
    val uiState: StateFlow<ColliderUiState> = _uiState

    val runProgress: StateFlow<RunProgress> = engine.progress

    private var settings = CollideSettings()

    init {
        viewModelScope.launch {
            settingsStore.settings.collect { s ->
                settings = s
                _uiState.update {
                    it.copy(
                        selectedBaseline = s.defaultBaseline,
                        selectedRunMode = s.defaultRunMode,
                        maxCandidates = s.defaultMaxCandidates,
                        maxChainLength = s.defaultMaxChainLength
                    )
                }
            }
        }
    }

    fun loadFile(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                    ?: throw IllegalStateException("Cannot open file stream")

                val maxSize = settings.maxFileSizeBytes
                if (bytes.size > maxSize) {
                    _uiState.update {
                        it.copy(
                            fileLoadError = "File too large: ${bytes.size} bytes (limit: $maxSize bytes)",
                            selectedFileName = null
                        )
                    }
                    return@launch
                }

                val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "unknown"
                val mimeType = context.contentResolver.getType(uri)
                loadedBytes = bytes

                _uiState.update {
                    it.copy(
                        selectedFileName = fileName,
                        selectedFileSize = bytes.size.toLong(),
                        selectedMimeType = mimeType,
                        fileLoadError = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(fileLoadError = "Failed to load file: ${e.message}")
                }
            }
        }
    }

    fun loadBuiltInFixture(context: Context, assetName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bytes = context.assets.open(assetName).readBytes()
                loadedBytes = bytes
                _uiState.update {
                    it.copy(
                        selectedFileName = assetName,
                        selectedFileSize = bytes.size.toLong(),
                        selectedMimeType = guessMimeFromName(assetName),
                        fileLoadError = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(fileLoadError = "Failed to load fixture: ${e.message}")
                }
            }
        }
    }

    fun setBaseline(strategy: BaselineStrategy) {
        _uiState.update { it.copy(selectedBaseline = strategy) }
    }

    fun setRunMode(mode: RunMode) {
        _uiState.update {
            it.copy(
                selectedRunMode = mode,
                maxCandidates = mode.maxCandidates,
                maxChainLength = mode.maxChainLength
            )
        }
    }

    fun setMaxCandidates(value: Int) {
        _uiState.update { it.copy(maxCandidates = value) }
    }

    fun setMaxChainLength(value: Int) {
        _uiState.update { it.copy(maxChainLength = value.coerceIn(1, 3)) }
    }

    fun startRun() {
        val bytes = loadedBytes ?: return
        val state = _uiState.value

        runJob?.cancel()
        runJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                val input = InputSample(
                    fileName = state.selectedFileName ?: "unknown",
                    filePath = null,
                    bytes = bytes,
                    mimeType = state.selectedMimeType
                )

                val result = engine.run(
                    input = input,
                    runMode = state.selectedRunMode,
                    baselineStrategy = state.selectedBaseline,
                    maxCandidatesOverride = state.maxCandidates,
                    maxChainLengthOverride = state.maxChainLength
                )

                // Save winners
                result.winners.forEach { event ->
                    repository.saveEvent(event)
                }

                // Save run summary
                repository.saveRunSummary(
                    fileName = input.fileName,
                    inputSize = input.size.toLong(),
                    runMode = state.selectedRunMode,
                    stats = result.stats
                )

                val msg = when {
                    result.stats.strictWinnerCount > 0 ->
                        "Run complete. ${result.stats.strictWinnerCount} winner(s) archived."
                    else ->
                        "Run complete. No strict improvements found. ${result.stats.candidatesEvaluated} candidates evaluated."
                }
                _uiState.update { it.copy(lastRunMessage = msg) }
            } catch (e: CancellationException) {
                _uiState.update { it.copy(lastRunMessage = "Run cancelled.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(lastRunMessage = "Run error: ${e.message}") }
            }
        }
    }

    fun cancelRun() {
        runJob?.cancel()
        runJob = null
    }

    fun clearLastRunMessage() {
        _uiState.update { it.copy(lastRunMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        runJob?.cancel()
    }

    private fun guessMimeFromName(name: String): String? = when {
        name.endsWith(".json") -> "application/json"
        name.endsWith(".csv") -> "text/csv"
        name.endsWith(".txt") -> "text/plain"
        name.endsWith(".bin") -> "application/octet-stream"
        else -> null
    }

    class Factory(
        private val repository: EventRepository,
        private val settingsStore: CollideSettingsStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ColliderViewModel(repository, settingsStore) as T
        }
    }
}
