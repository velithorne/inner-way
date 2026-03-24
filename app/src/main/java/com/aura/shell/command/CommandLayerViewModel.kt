package com.aura.shell.command

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aura.shell.data.LauncherRepository
import com.aura.shell.data.RecentAppsStore
import com.aura.shell.model.LauncherAppInfo
import com.aura.shell.model.RecentAppEntry
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
    val history: List<String> = emptyList(),
    val installedApps: List<LauncherAppInfo> = emptyList(),
    val recentApps: List<RecentAppEntry> = emptyList(),
    val isLoading: Boolean = true,
    val surface: CommandSurfaceState = CommandSurfaceState.Empty,
)

sealed class CommandSurfaceState {
    data object Empty : CommandSurfaceState()
    data class Success(val message: String) : CommandSurfaceState()
    data class Suggestions(val title: String, val apps: List<LauncherAppInfo>) : CommandSurfaceState()
    data class SearchResults(val query: String, val apps: List<LauncherAppInfo>) : CommandSurfaceState()
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
    private val router = CommandRouter()

    private val _uiState = MutableStateFlow(CommandLayerUiState())
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
                        isLoading = false,
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

    fun applyHistoryLine(line: String) {
        _uiState.update { it.copy(inputText = line) }
    }

    fun launchApp(packageName: String) {
        viewModelScope.launch {
            repository.launchApp(packageName)
            recentStore.recordLaunch(packageName)
            reloadData()
        }
    }

    fun submitCommand() {
        val text = _uiState.value.inputText
        val apps = _uiState.value.installedApps
        val recent = _uiState.value.recentApps
        if (_uiState.value.isLoading) return

        historyStore.recordCommand(text)
        _uiState.update {
            it.copy(
                history = historyStore.loadHistory(),
            )
        }

        val dispatch = router.route(text, apps, recent)
        when (dispatch) {
            is CommandDispatch.LaunchApp -> {
                viewModelScope.launch {
                    repository.launchApp(dispatch.packageName)
                    recentStore.recordLaunch(dispatch.packageName)
                    _uiState.update {
                        it.copy(
                            surface = CommandSurfaceState.Success("Opened ${dispatch.displayLabel}"),
                            recentApps = recentStore.loadRecentEntries(repository),
                        )
                    }
                }
            }
            is CommandDispatch.OpenAppDrawer -> {
                _uiState.update { it.copy(surface = CommandSurfaceState.Success("Opening app drawer…")) }
                _sideEffects.tryEmit(CommandSideEffect.OpenAppDrawerAndFinish)
            }
            is CommandDispatch.CloseAppDrawer -> {
                _uiState.update { it.copy(surface = CommandSurfaceState.Success("Closing app drawer…")) }
                _sideEffects.tryEmit(CommandSideEffect.CloseAppDrawerAndFinish)
            }
            else -> applySurfaceOnly(dispatch)
        }
    }

    private fun applySurfaceOnly(dispatch: CommandDispatch) {
        when (dispatch) {
            is CommandDispatch.LaunchApp,
            is CommandDispatch.OpenAppDrawer,
            is CommandDispatch.CloseAppDrawer,
            -> { }
            is CommandDispatch.PickFromSuggestions -> {
                _uiState.update {
                    it.copy(
                        surface = CommandSurfaceState.Suggestions(
                            title = dispatch.message,
                            apps = dispatch.candidates,
                        ),
                    )
                }
            }
            is CommandDispatch.SearchMatches -> {
                _uiState.update {
                    it.copy(
                        surface = CommandSurfaceState.SearchResults(
                            query = dispatch.query,
                            apps = dispatch.matches,
                        ),
                    )
                }
            }
            is CommandDispatch.RecentMatches -> {
                _uiState.update {
                    it.copy(
                        surface = CommandSurfaceState.RecentsList(
                            title = dispatch.title,
                            entries = dispatch.entries,
                        ),
                    )
                }
            }
            is CommandDispatch.ShowHelp -> {
                _uiState.update {
                    it.copy(surface = CommandSurfaceState.Help(dispatch.lines))
                }
            }
            is CommandDispatch.Unknown -> {
                _uiState.update {
                    it.copy(surface = CommandSurfaceState.Unknown(dispatch.message))
                }
            }
        }
    }
}
