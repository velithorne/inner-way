package com.aura.shell.command

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aura.shell.data.LauncherRepository
import com.aura.shell.data.RecentAppsStore
import com.aura.shell.model.LauncherAppInfo
import com.aura.shell.model.RecentAppEntry
import com.aura.shell.data.AuraSettingsStore
import com.aura.shell.personalization.LearnedPreferenceRow
import com.aura.shell.personalization.LearningSignal
import com.aura.shell.personalization.PersonalAliasEntry
import com.aura.shell.personalization.PersonalAliasStore
import com.aura.shell.personalization.PreferenceLearningStore
import com.aura.shell.personalization.PersonalResolutionContext
import com.aura.shell.voice.ForegroundPassiveVoiceCoordinator
import com.aura.shell.voice.HandsFreeUiState
import com.aura.shell.voice.SpeechInputManager
import com.aura.shell.voice.VoiceSurfaceState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CommandLayerUiState(
    val inputText: String = "",
    val history: List<CommandHistoryEntry> = emptyList(),
    val installedApps: List<LauncherAppInfo> = emptyList(),
    val recentApps: List<RecentAppEntry> = emptyList(),
    val personalAliases: List<PersonalAliasEntry> = emptyList(),
    val learnedPreferences: List<LearnedPreferenceRow> = emptyList(),
    val isLoading: Boolean = true,
    val surface: CommandSurfaceState = CommandSurfaceState.Empty,
    val voice: VoiceSurfaceState = VoiceSurfaceState.Idle,
    val passiveHandsFreeEnabled: Boolean = false,
    val handsFree: HandsFreeUiState = HandsFreeUiState.Disabled,
    val showPersonalizationSheet: Boolean = false,
)

sealed class CommandSurfaceState {
    data object Empty : CommandSurfaceState()
    data class Success(val message: String, val hint: String? = null) : CommandSurfaceState()
    data class Suggestions(
        val title: String,
        val subtitle: String?,
        val apps: List<LauncherAppInfo>,
        val kind: SuggestionKind,
        val resolutionTargetKey: String? = null,
    ) : CommandSurfaceState()
    data class SearchResults(
        val query: String,
        val apps: List<LauncherAppInfo>,
        val resolutionTargetKey: String? = null,
    ) : CommandSurfaceState()
    data class RecentsList(val title: String, val entries: List<RecentAppEntry>) : CommandSurfaceState()
    data class Help(val lines: List<String>) : CommandSurfaceState()
    data class Unknown(val message: String) : CommandSurfaceState()
}

class CommandLayerViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val repository = LauncherRepository(application.applicationContext)
    private val recentStore = RecentAppsStore(application.applicationContext)
    private val historyStore = CommandHistoryStore(application.applicationContext)
    private val settingsStore = AuraSettingsStore(application.applicationContext)
    private val aliasStore = PersonalAliasStore(application.applicationContext)
    private val learningStore = PreferenceLearningStore(application.applicationContext)
    private val passiveSpeech = SpeechInputManager(application.applicationContext)
    private val router = CommandRouter()
    private val personal: PersonalResolutionContext
        get() = PersonalResolutionContext(aliasStore, learningStore)

    private var passiveCoordinator: ForegroundPassiveVoiceCoordinator? = null
    private var commandForeground = false

    private val _uiState = MutableStateFlow(
        CommandLayerUiState(passiveHandsFreeEnabled = settingsStore.passiveHandsFreeEnabled),
    )
    val uiState: StateFlow<CommandLayerUiState> = _uiState.asStateFlow()

    private val _sideEffects = MutableSharedFlow<CommandSideEffect>(extraBufferCapacity = 1)
    val sideEffects: SharedFlow<CommandSideEffect> = _sideEffects.asSharedFlow()

    init {
        reloadData()
    }

    fun reloadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val apps = repository.loadLaunchableApps()
                val recent = recentStore.loadRecentEntries(repository)
                val history = historyStore.loadHistory()
                _uiState.update {
                    it.copy(
                        installedApps = apps,
                        recentApps = recent,
                        history = history,
                        personalAliases = aliasStore.getAllAliases(),
                        learnedPreferences = learningStore.getAllLearnedRows(),
                        isLoading = false,
                        passiveHandsFreeEnabled = settingsStore.passiveHandsFreeEnabled,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        surface = CommandSurfaceState.Unknown(
                            e.message ?: "Could not load apps.",
                        ),
                    )
                }
            }
        }
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun applyHistoryEntry(entry: CommandHistoryEntry) {
        val text = entry.normalized ?: entry.original
        _uiState.update { it.copy(inputText = text) }
    }

    fun setVoiceState(state: VoiceSurfaceState) {
        _uiState.update { it.copy(voice = state) }
    }

    fun launchApp(
        packageName: String,
        resolutionTargetKey: String? = null,
        learningSignal: LearningSignal? = null,
    ) {
        viewModelScope.launch {
            repository.launchApp(packageName)
            recentStore.recordLaunch(packageName)
            if (resolutionTargetKey != null && learningSignal != null) {
                learningStore.record(resolutionTargetKey, packageName, learningSignal)
            }
            reloadData()
        }
    }

    fun dismissPersonalizationSheet() {
        _uiState.update { it.copy(showPersonalizationSheet = false) }
    }

    fun openPersonalizationSheet() {
        _uiState.update {
            it.copy(
                showPersonalizationSheet = true,
                personalAliases = aliasStore.getAllAliases(),
                learnedPreferences = learningStore.getAllLearnedRows(),
            )
        }
    }

    fun addPersonalAlias(alias: String, packageName: String) {
        viewModelScope.launch {
            aliasStore.setAlias(alias, packageName)
            reloadData()
        }
    }

    fun removePersonalAlias(alias: String) {
        viewModelScope.launch {
            aliasStore.removeAlias(alias)
            reloadData()
        }
    }

    fun clearLearnedPreferencesOnly() {
        viewModelScope.launch {
            learningStore.clearAll()
            reloadData()
        }
    }

    fun clearAllPersonalization() {
        viewModelScope.launch {
            learningStore.clearAll()
            aliasStore.clearAll()
            reloadData()
        }
    }

    fun submitCommand() {
        submitFromText(_uiState.value.inputText, CommandInputSource.Typed)
    }

    /**
     * Same pipeline as typed submit; used for voice transcripts.
     */
    fun submitCommand(source: CommandInputSource) {
        submitFromText(_uiState.value.inputText, source)
    }

    suspend fun submitPipeline(text: String, source: CommandInputSource): PipelineResult {
        if (_uiState.value.isLoading) {
            return PipelineResult.Error("Loading…")
        }
        return executeCommandPipeline(
            text = text,
            source = source,
            repository = repository,
            recentStore = recentStore,
            historyStore = historyStore,
            router = router,
            learningStore = learningStore,
            personal = personal,
        )
    }

    private fun submitFromText(text: String, source: CommandInputSource) {
        viewModelScope.launch {
            val result = submitPipeline(text, source)
            applyPipelineResult(result, commandTextForField = text)
        }
    }

    fun setPassiveHandsFreeEnabled(enabled: Boolean) {
        settingsStore.passiveHandsFreeEnabled = enabled
        _uiState.update { it.copy(passiveHandsFreeEnabled = enabled) }
        passiveCoordinator?.onPassiveSettingChanged(enabled)
        if (!enabled) {
            _uiState.update { it.copy(handsFree = HandsFreeUiState.Disabled) }
        } else if (commandForeground) {
            ensurePassiveCoordinator()
            passiveCoordinator?.setForegroundVisible(true)
        }
    }

    fun onForegroundChanged(visible: Boolean) {
        commandForeground = visible
        if (!visible) {
            passiveCoordinator?.setForegroundVisible(false)
            _uiState.update { it.copy(handsFree = HandsFreeUiState.Disabled) }
            return
        }
        if (settingsStore.passiveHandsFreeEnabled) {
            ensurePassiveCoordinator()
            passiveCoordinator?.setForegroundVisible(true)
        } else {
            _uiState.update { it.copy(handsFree = HandsFreeUiState.Disabled) }
        }
    }

    fun onTapMicStarted() {
        passiveCoordinator?.stop()
        _uiState.update { it.copy(handsFree = HandsFreeUiState.Disabled) }
    }

    private fun ensurePassiveCoordinator() {
        if (passiveCoordinator != null) return
        passiveCoordinator = ForegroundPassiveVoiceCoordinator(
            scope = viewModelScope,
            speech = passiveSpeech,
            isPassiveEnabled = { settingsStore.passiveHandsFreeEnabled },
            submitCommand = { text, source ->
                val result = submitPipeline(text, source)
                applyPipelineResult(result, commandTextForField = text)
                result
            },
            onState = { state ->
                _uiState.update { it.copy(handsFree = state) }
            },
        )
    }

    fun applyPipelineResult(result: PipelineResult, commandTextForField: String? = null) {
        _uiState.update {
            it.copy(
                voice = VoiceSurfaceState.Idle,
            )
        }
        when (result) {
            is PipelineResult.Launched -> {
                _uiState.update {
                    it.copy(
                        inputText = commandTextForField ?: it.inputText,
                        surface = CommandSurfaceState.Success(
                            message = "Opened ${result.displayLabel}",
                            hint = result.hint,
                        ),
                        history = result.history,
                        recentApps = result.recentApps,
                        learnedPreferences = learningStore.getAllLearnedRows(),
                    )
                }
            }
            is PipelineResult.OpenDrawer -> {
                _uiState.update {
                    it.copy(
                        surface = CommandSurfaceState.Success("Opening app drawer…"),
                        history = result.history,
                    )
                }
                _sideEffects.tryEmit(CommandSideEffect.OpenAppDrawerAndFinish)
            }
            is PipelineResult.CloseDrawer -> {
                _uiState.update {
                    it.copy(
                        surface = CommandSurfaceState.Success("Closing app drawer…"),
                        history = result.history,
                    )
                }
                _sideEffects.tryEmit(CommandSideEffect.CloseAppDrawerAndFinish)
            }
            is PipelineResult.GoHome -> {
                _uiState.update {
                    it.copy(
                        surface = CommandSurfaceState.Success("Returned home"),
                        history = result.history,
                    )
                }
                _sideEffects.tryEmit(CommandSideEffect.FinishAfterGoHome)
            }
            is PipelineResult.OpenPersonalization -> {
                _uiState.update {
                    it.copy(
                        history = result.history,
                        showPersonalizationSheet = true,
                        personalAliases = aliasStore.getAllAliases(),
                        learnedPreferences = learningStore.getAllLearnedRows(),
                    )
                }
            }
            is PipelineResult.SurfaceOnly -> {
                _uiState.update {
                    it.copy(
                        surface = result.surface,
                        history = result.history,
                        personalAliases = aliasStore.getAllAliases(),
                        learnedPreferences = learningStore.getAllLearnedRows(),
                    )
                }
            }
            is PipelineResult.Error -> {
                _uiState.update {
                    it.copy(surface = CommandSurfaceState.Unknown(result.message))
                }
            }
        }
    }

    /**
     * Inserts transcript and runs the same pipeline as typing.
     */
    fun applySpeechTranscriptAndSubmit(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            setVoiceState(VoiceSurfaceState.Error("Nothing recognized. Try again or type."))
            return
        }
        _uiState.update { it.copy(inputText = trimmed, voice = VoiceSurfaceState.Processing) }
        submitFromText(trimmed, CommandInputSource.Voice)
    }

    fun updateHandsFreeState(state: HandsFreeUiState) {
        _uiState.update { it.copy(handsFree = state) }
    }

    override fun onCleared() {
        passiveCoordinator?.stop()
        super.onCleared()
    }
}
