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
import com.collide.app.domain.engine.RunResult
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
    val lastRunMessage: String? = null,
    val lastRunResult: RunResult? = null
)

class ColliderViewModel(
    private val repository: EventRepository,
    private val settingsStore: CollideSettingsStore
) : ViewModel() {

    private var engine = CompressionColliderEngine()
    private var runJob: Job? = null
    private var loadedBytes: ByteArray? = null

    private val _uiState = MutableStateFlow(ColliderUiState())
    val uiState: StateFlow<ColliderUiState> = _uiState

    val runProgress: StateFlow<RunProgress> get() = engine.progress

    private var settings = CollideSettings()

    init {
        viewModelScope.launch {
            settingsStore.settings.collect { s ->
                settings = s
                // Rebuild engine if enabled transforms change
                engine = CompressionColliderEngine(s.enabledTransformIds)
                _uiState.update {
                    it.copy(
                        selectedBaseline = s.defaultBaseline,
                        selectedRunMode = s.defaultRunMode,
                        maxCandidates = s.defaultMaxCandidates,
                        maxChainLength = if (s.allowChainLength4) s.defaultMaxChainLength.coerceAtMost(4)
                                        else s.defaultMaxChainLength.coerceAtMost(3)
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

                if (bytes.size > settings.maxFileSizeBytes) {
                    _uiState.update {
                        it.copy(
                            fileLoadError = "File too large: ${bytes.size} bytes (limit: ${settings.maxFileSizeBytes} bytes)",
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
                _uiState.update { it.copy(fileLoadError = "Failed to load file: ${e.message}") }
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
                        selectedFileName = assetName.substringAfterLast('/'),
                        selectedFileSize = bytes.size.toLong(),
                        selectedMimeType = guessMime(assetName),
                        fileLoadError = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(fileLoadError = "Failed to load fixture: ${e.message}") }
            }
        }
    }

    fun setBaseline(s: BaselineStrategy) = _uiState.update { it.copy(selectedBaseline = s) }
    fun setRunMode(m: RunMode) = _uiState.update {
        it.copy(
            selectedRunMode = m,
            maxCandidates = m.maxCandidates,
            maxChainLength = if (settings.allowChainLength4) m.maxChainLength.coerceAtMost(4)
                            else m.maxChainLength.coerceAtMost(3)
        )
    }
    fun setMaxCandidates(v: Int) = _uiState.update { it.copy(maxCandidates = v) }
    fun setMaxChainLength(v: Int) {
        val max = if (settings.allowChainLength4) 4 else 3
        _uiState.update { it.copy(maxChainLength = v.coerceIn(1, max)) }
    }

    fun startRun() {
        val bytes = loadedBytes ?: return
        val state = _uiState.value
        runJob?.cancel()

        // Rebuild engine with current settings
        engine = CompressionColliderEngine(settings.enabledTransformIds)

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
                result.winners.forEach { repository.saveEvent(it) }

                var bestWinner = 0L
                result.winners.minByOrNull { it.winningSize }?.let { bestWinner = it.winningSize }

                repository.saveRunSummary(
                    fileName = input.fileName,
                    inputSize = input.size.toLong(),
                    runMode = state.selectedRunMode,
                    stats = result.stats,
                    baselineSize = result.baselineResult.encodedSize,
                    bestWinnerSize = bestWinner
                )

                val msg = if (result.stats.strictWinnerCount > 0)
                    "Run complete. ${result.stats.strictWinnerCount} verified winner(s) archived."
                else
                    "Run complete. No strict improvements found. ${result.stats.candidatesEvaluated} candidates evaluated."

                _uiState.update { it.copy(lastRunMessage = msg, lastRunResult = result) }
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

    fun clearLastRunMessage() = _uiState.update { it.copy(lastRunMessage = null) }
    fun clearLastRunResult() = _uiState.update { it.copy(lastRunResult = null) }

    override fun onCleared() {
        super.onCleared()
        runJob?.cancel()
    }

    private fun guessMime(name: String) = when {
        name.endsWith(".json") -> "application/json"
        name.endsWith(".csv") -> "text/csv"
        name.endsWith(".txt") -> "text/plain"
        name.endsWith(".kt") -> "text/x-kotlin"
        name.endsWith(".bin") -> "application/octet-stream"
        else -> null
    }

    class Factory(
        private val repository: EventRepository,
        private val settingsStore: CollideSettingsStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            ColliderViewModel(repository, settingsStore) as T
    }
}
