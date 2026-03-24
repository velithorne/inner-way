package com.aura.shell.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aura.shell.data.LauncherRepository
import com.aura.shell.data.RecentAppsStore
import com.aura.shell.command.DrawerRequest
import com.aura.shell.model.LauncherAppInfo
import com.aura.shell.model.RecentAppEntry
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
)

class HomeViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val repository = LauncherRepository(application.applicationContext)
    private val recentStore = RecentAppsStore(application.applicationContext)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refreshApps()
        refreshRecents()
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

}
