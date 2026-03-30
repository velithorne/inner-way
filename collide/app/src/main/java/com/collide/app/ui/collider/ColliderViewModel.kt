package com.collide.app.ui.collider

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.collide.app.data.repository.EventRepository
import com.collide.app.data.settings.AppSettings
import com.collide.app.domain.engine.CodeColliderEngine
import com.collide.app.domain.engine.collision.RecipeSerializer
import com.collide.app.domain.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class ColliderUiState(
    val inputTextA: String = "",
    val inputLabelA: String = "Input A",
    val inputTextB: String = "",
    val inputLabelB: String = "Input B",
    val profileA: InputProfile? = null,
    val profileB: InputProfile? = null,
    val twoInputMode: Boolean = false,
    val selectedRunMode: RunMode = RunMode.BALANCED,
    val selectedSensitivity: DetectorSensitivity = DetectorSensitivity.MEDIUM,
    val maxCandidates: Int = RunMode.BALANCED.maxCandidates,
    val searchDepth: Int = RunMode.BALANCED.searchDepth,
    val runStatus: RunStatus = RunStatus.IDLE,
    val progress: RunProgress? = null,
    val errorMessage: String? = null,
    val availableFixtures: List<String> = emptyList()
)

class ColliderViewModel(
    private val engine: CodeColliderEngine,
    private val repo: EventRepository,
    private val settings: AppSettings,
    private val recipeSerializer: RecipeSerializer
) : ViewModel() {

    private val _uiState = MutableStateFlow(ColliderUiState())
    val uiState: StateFlow<ColliderUiState> = _uiState.asStateFlow()

    private var runJob: Job? = null
    private var currentRunId: String = ""

    init {
        viewModelScope.launch {
            settings.configFlow.collect { config ->
                _uiState.update { it.copy(
                    selectedRunMode = config.runMode,
                    maxCandidates = config.maxCandidates,
                    searchDepth = config.searchDepth,
                    selectedSensitivity = config.detectorSensitivity,
                    twoInputMode = config.twoInputMode
                ) }
            }
        }
    }

    fun updateInputA(text: String) {
        _uiState.update { it.copy(inputTextA = text) }
        if (text.isNotBlank()) profileInputA()
    }

    fun updateInputB(text: String) {
        _uiState.update { it.copy(inputTextB = text) }
        if (text.isNotBlank()) profileInputB()
    }

    fun updateLabelA(label: String) = _uiState.update { it.copy(inputLabelA = label) }
    fun updateLabelB(label: String) = _uiState.update { it.copy(inputLabelB = label) }

    fun setTwoInputMode(enabled: Boolean) = _uiState.update { it.copy(twoInputMode = enabled) }

    fun setRunMode(mode: RunMode) {
        _uiState.update { it.copy(
            selectedRunMode = mode,
            maxCandidates = mode.maxCandidates,
            searchDepth = mode.searchDepth
        ) }
    }

    fun setSensitivity(sensitivity: DetectorSensitivity) =
        _uiState.update { it.copy(selectedSensitivity = sensitivity) }

    fun setMaxCandidates(count: Int) = _uiState.update { it.copy(maxCandidates = count) }
    fun setSearchDepth(depth: Int) = _uiState.update { it.copy(searchDepth = depth) }

    fun loadFileA(context: Context, uri: Uri) {
        val text = readTextFromUri(context, uri) ?: return
        val name = uri.lastPathSegment ?: "file_a"
        _uiState.update { it.copy(inputTextA = text, inputLabelA = name) }
        profileInputA()
    }

    fun loadFileB(context: Context, uri: Uri) {
        val text = readTextFromUri(context, uri) ?: return
        val name = uri.lastPathSegment ?: "file_b"
        _uiState.update { it.copy(inputTextB = text, inputLabelB = name) }
        profileInputB()
    }

    fun loadFixture(context: Context, fixtureName: String, slot: String = "A") {
        val text = try {
            context.assets.open("fixtures/$fixtureName").bufferedReader().readText()
        } catch (e: Exception) { return }

        if (slot == "A") {
            _uiState.update { it.copy(inputTextA = text, inputLabelA = fixtureName) }
            profileInputA()
        } else {
            _uiState.update { it.copy(inputTextB = text, inputLabelB = fixtureName) }
            profileInputB()
        }
    }

    fun getAvailableFixtures(context: Context): List<String> {
        return try {
            context.assets.list("fixtures")?.toList() ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    fun startRun() {
        val state = _uiState.value
        if (state.inputTextA.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Input A is required") }
            return
        }
        if (state.runStatus == RunStatus.RUNNING) return

        currentRunId = UUID.randomUUID().toString()

        val sampleA = InputSample(
            id = "run_a_${currentRunId}",
            label = state.inputLabelA,
            rawText = state.inputTextA
        )
        val sampleB = if (state.twoInputMode && state.inputTextB.isNotBlank()) {
            InputSample(id = "run_b_${currentRunId}", label = state.inputLabelB, rawText = state.inputTextB)
        } else null

        val config = ColliderConfig(
            runMode = state.selectedRunMode,
            maxCandidates = state.maxCandidates,
            searchDepth = state.searchDepth,
            detectorSensitivity = state.selectedSensitivity,
            twoInputMode = sampleB != null
        )

        _uiState.update { it.copy(runStatus = RunStatus.RUNNING, errorMessage = null) }

        runJob = viewModelScope.launch {
            try {
                val savedEventIndices = mutableSetOf<Int>()

                engine.runCollision(sampleA, sampleB, config).collect { progress ->
                    _uiState.update { it.copy(progress = progress, runStatus = progress.stats.status) }

                    // Save newly discovered events (track by index to avoid re-saving)
                    for (event in progress.latestEvents) {
                        if (event.index !in savedEventIndices &&
                            event.classification == CandidateClassification.SAVED_EVENT &&
                            event.replayable) {
                            savedEventIndices.add(event.index)
                            val recipeJson = recipeSerializer.serialize(
                                com.collide.app.domain.model.CollisionRecipe(
                                    id = event.recipeId,
                                    name = event.recipeName,
                                    description = "",
                                    mode = if (sampleB != null)
                                        com.collide.app.domain.model.CollisionMode.DUAL_INPUT_RECOMBINATION
                                    else
                                        com.collide.app.domain.model.CollisionMode.SINGLE_INPUT_MUTATION,
                                    operations = emptyList()
                                )
                            )
                            repo.saveEvent(
                                candidate = event,
                                config = config,
                                inputLabelA = sampleA.label,
                                inputLabelB = sampleB?.label,
                                inputProfileA = state.profileA?.estimatedLanguage?.displayName() ?: "unknown",
                                inputProfileB = state.profileB?.estimatedLanguage?.displayName(),
                                collisionRecipeJson = recipeJson
                            )
                        }
                    }
                }

                // Save run summary
                val finalStats = _uiState.value.progress?.stats
                if (finalStats != null) {
                    repo.saveRunSummary(
                        currentRunId,
                        finalStats,
                        if (sampleB != null) "dual_input" else "single_input"
                    )
                }

                _uiState.update { it.copy(runStatus = RunStatus.COMPLETED) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    runStatus = RunStatus.ERROR,
                    errorMessage = "Run error: ${e.message}"
                ) }
            }
        }
    }

    fun cancelRun() {
        runJob?.cancel()
        _uiState.update { it.copy(runStatus = RunStatus.CANCELLED) }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
    fun resetRun() = _uiState.update { it.copy(runStatus = RunStatus.IDLE, progress = null) }

    private fun profileInputA() {
        val text = _uiState.value.inputTextA
        if (text.isBlank()) return
        viewModelScope.launch {
            val sample = InputSample("prof_a", "Input A", text)
            val profile = engine.profileInput(sample)
            _uiState.update { it.copy(profileA = profile) }
        }
    }

    private fun profileInputB() {
        val text = _uiState.value.inputTextB
        if (text.isBlank()) return
        viewModelScope.launch {
            val sample = InputSample("prof_b", "Input B", text)
            val profile = engine.profileInput(sample)
            _uiState.update { it.copy(profileB = profile) }
        }
    }

    private fun readTextFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                ?.take(InputSample.MAX_INPUT_SIZE_BYTES)
        } catch (e: Exception) { null }
    }

    class Factory(
        private val engine: CodeColliderEngine,
        private val repo: EventRepository,
        private val settings: AppSettings,
        private val recipeSerializer: RecipeSerializer
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            ColliderViewModel(engine, repo, settings, recipeSerializer) as T
    }
}
