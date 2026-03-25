package com.aura.shell.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aura.shell.command.CommandInputSource
import com.aura.shell.command.CommandRouter
import com.aura.shell.command.CommandHistoryStore
import com.aura.shell.command.DrawerRequest
import com.aura.shell.command.PipelineResult
import com.aura.shell.command.executeCommandPipeline
import com.aura.shell.data.AuraSettingsStore
import com.aura.shell.data.LauncherRepository
import com.aura.shell.data.RecentAppsStore
import com.aura.shell.model.LauncherAppInfo
import com.aura.shell.model.RecentAppEntry
import com.aura.shell.voice.ForegroundPassiveVoiceCoordinator
import com.aura.shell.voice.HandsFreeUiState
import com.aura.shell.voice.SpeechInputManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val installedApps: List<LauncherAppInfo> = emptyList(),
    val recentApps: List<RecentAppEntry> = emptyList(),
    val isLoadingApps: Boolean = true,
    val loadError: String? = null,
    val drawerRequest: DrawerRequest? = null,
    val passiveHandsFreeEnabled: Boolean = false,
    val handsFree: HandsFreeUiState = HandsFreeUiState.Disabled,
)

class HomeViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val repository = LauncherRepository(application.applicationContext)
    private val recentStore = RecentAppsStore(application.applicationContext)
    private val historyStore = CommandHistoryStore(application.applicationContext)
    private val settingsStore = AuraSettingsStore(application.applicationContext)
    private val router = CommandRouter()

    private val speech = SpeechInputManager(application.applicationContext)

    private val _uiState = MutableStateFlow(
        HomeUiState(passiveHandsFreeEnabled = settingsStore.passiveHandsFreeEnabled),
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var passiveCoordinator: ForegroundPassiveVoiceCoordinator? = null
    private var isForeground = false

    init {
        refreshApps()
        refreshRecents()
    }

    fun setPassiveHandsFreeEnabled(enabled: Boolean) {
        settingsStore.passiveHandsFreeEnabled = enabled
        _uiState.update { it.copy(passiveHandsFreeEnabled = enabled) }
        passiveCoordinator?.onPassiveSettingChanged(enabled)
        if (!enabled) {
            _uiState.update { it.copy(handsFree = HandsFreeUiState.Disabled) }
        } else if (isForeground) {
            ensureCoordinator()
            passiveCoordinator?.setForegroundVisible(true)
        }
    }

    fun onForegroundChanged(visible: Boolean) {
        isForeground = visible
        if (!visible) {
            passiveCoordinator?.setForegroundVisible(false)
            _uiState.update { it.copy(handsFree = HandsFreeUiState.Disabled) }
            return
        }
        if (settingsStore.passiveHandsFreeEnabled) {
            ensureCoordinator()
            passiveCoordinator?.setForegroundVisible(true)
        } else {
            _uiState.update { it.copy(handsFree = HandsFreeUiState.Disabled) }
        }
    }

    private fun ensureCoordinator() {
        if (passiveCoordinator != null) return
        passiveCoordinator = ForegroundPassiveVoiceCoordinator(
            scope = viewModelScope,
            speech = speech,
            isPassiveEnabled = { settingsStore.passiveHandsFreeEnabled },
            submitCommand = { text, source ->
                val result = executeCommandPipeline(
                    text = text,
                    source = source,
                    repository = repository,
                    recentStore = recentStore,
                    historyStore = historyStore,
                    router = router,
                )
                applyPassivePipelineResult(result)
                result
            },
            onState = { state ->
                _uiState.update { it.copy(handsFree = state) }
            },
        )
    }

    private suspend fun applyPassivePipelineResult(result: PipelineResult) {
        when (result) {
            is PipelineResult.Launched -> {
                refreshRecents()
                _uiState.update {
                    it.copy(recentApps = recentStore.loadRecentEntries(repository))
                }
            }
            is PipelineResult.OpenDrawer -> {
                _uiState.update {
                    it.copy(drawerRequest = DrawerRequest(nonce = System.nanoTime(), expand = true))
                }
            }
            is PipelineResult.CloseDrawer -> {
                _uiState.update {
                    it.copy(drawerRequest = DrawerRequest(nonce = System.nanoTime(), expand = false))
                }
            }
            is PipelineResult.SurfaceOnly -> { }
            is PipelineResult.Error -> { }
        }
    }

    fun refreshApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingApps = true, loadError = null) }
            try {
                val apps = repository.loadLaunchableApps()
                _uiState.update {
                    it.copy(installedApps = apps, isLoadingApps = false, loadError = null)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingApps = false,
                        loadError = e.message ?: "Could not load apps",
                    )
                }
            }
        }
    }

    fun refreshRecents() {
        viewModelScope.launch {
            val recent = recentStore.loadRecentEntries(repository)
            _uiState.update { it.copy(recentApps = recent) }
        }
    }

    fun onAppLaunch(packageName: String) {
        viewModelScope.launch {
            val ok = repository.launchApp(packageName)
            if (ok) {
                recentStore.recordLaunch(packageName)
                refreshRecents()
            }
        }
    }

    fun requestDrawer(expand: Boolean) {
        _uiState.update {
            it.copy(drawerRequest = DrawerRequest(nonce = System.nanoTime(), expand = expand))
        }
    }

    fun consumeDrawerRequest() {
        _uiState.update { it.copy(drawerRequest = null) }
    }

    override fun onCleared() {
        passiveCoordinator?.stop()
        super.onCleared()
    }
}
