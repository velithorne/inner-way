package com.collide.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.collide.app.data.settings.CollideSettings
import com.collide.app.data.settings.CollideSettingsStore
import com.collide.app.domain.model.BaselineStrategy
import com.collide.app.domain.model.RunMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val store: CollideSettingsStore) : ViewModel() {

    val settings = store.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CollideSettings()
    )

    fun setBaseline(s: BaselineStrategy) = viewModelScope.launch { store.updateBaseline(s) }
    fun setRunMode(m: RunMode) = viewModelScope.launch { store.updateRunMode(m) }
    fun setMaxCandidates(max: Int) = viewModelScope.launch { store.updateMaxCandidates(max) }
    fun setMaxChainLength(max: Int) = viewModelScope.launch { store.updateMaxChainLength(max.coerceIn(1, 4)) }
    fun setSaveNearMiss(save: Boolean) = viewModelScope.launch { store.updateSaveNearMiss(save) }
    fun setMaxFileSize(bytes: Long) = viewModelScope.launch { store.updateMaxFileSize(bytes) }
    fun setEnabledTransforms(ids: Set<String>) = viewModelScope.launch { store.updateEnabledTransforms(ids) }
    fun setAllowChainLength4(allow: Boolean) = viewModelScope.launch { store.updateAllowChainLength4(allow) }

    class Factory(private val store: CollideSettingsStore) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) = SettingsViewModel(store) as T
    }
}
