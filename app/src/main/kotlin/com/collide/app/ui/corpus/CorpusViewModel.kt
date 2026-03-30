package com.collide.app.ui.corpus

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.collide.app.domain.engine.CorpusRunner
import com.collide.app.domain.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class CorpusUiState(
    val isRunning: Boolean = false,
    val currentFixture: String = "",
    val currentFixtureIndex: Int = 0,
    val totalFixtures: Int = 0,
    val summary: CorpusSummary? = null,
    val error: String? = null
)

class CorpusViewModel : ViewModel() {

    private val runner = CorpusRunner()
    private var runJob: Job? = null

    private val _uiState = MutableStateFlow(CorpusUiState())
    val uiState: StateFlow<CorpusUiState> = _uiState

    private val fixtureAssets = listOf(
        "fixtures/sample_text.txt",
        "fixtures/sample_data.json",
        "fixtures/sample_table.csv",
        "fixtures/sample_mixed.txt",
        "fixtures/sample_binary.bin",
        "fixtures/sample_source.kt",
        "fixtures/sample_dense.bin"
    )

    fun startCorpusRun(
        context: Context,
        runMode: RunMode,
        baselineStrategy: BaselineStrategy,
        maxCandidatesOverride: Int? = null
    ) {
        runJob?.cancel()
        _uiState.value = CorpusUiState(
            isRunning = true,
            totalFixtures = fixtureAssets.size
        )

        runJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                val fixtures = fixtureAssets.mapNotNull { assetPath ->
                    try {
                        val bytes = context.assets.open(assetPath).readBytes()
                        InputSample(
                            fileName = assetPath.substringAfterLast('/'),
                            filePath = assetPath,
                            bytes = bytes
                        )
                    } catch (_: Exception) { null }
                }

                val config = CorpusRunner.CorpusRunConfig(
                    runMode = runMode,
                    baselineStrategy = baselineStrategy,
                    maxCandidatesOverride = maxCandidatesOverride
                )

                val summary = runner.run(
                    fixtures = fixtures,
                    config = config,
                    onFixtureProgress = { name, idx ->
                        _uiState.value = _uiState.value.copy(
                            currentFixture = name,
                            currentFixtureIndex = idx
                        )
                    }
                )

                _uiState.value = _uiState.value.copy(
                    isRunning = false,
                    summary = summary,
                    currentFixture = ""
                )
            } catch (e: CancellationException) {
                _uiState.value = _uiState.value.copy(isRunning = false, error = "Corpus run cancelled.")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isRunning = false, error = e.message)
            }
        }
    }

    fun cancelRun() { runJob?.cancel() }
    fun clearError() = _uiState.run { value = value.copy(error = null) }

    override fun onCleared() { super.onCleared(); runJob?.cancel() }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) = CorpusViewModel() as T
    }
}
