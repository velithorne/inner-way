package com.collide.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.collide.app.data.settings.AppSettings
import com.collide.app.domain.model.ColliderConfig
import com.collide.app.domain.model.DetectorSensitivity
import com.collide.app.domain.model.InputSample
import com.collide.app.domain.model.RunMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val settings: AppSettings) : ViewModel() {

    val config = settings.configFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ColliderConfig())

    fun setRunMode(mode: RunMode) = viewModelScope.launch { settings.setRunMode(mode) }
    fun setMaxCandidates(count: Int) = viewModelScope.launch { settings.setMaxCandidates(count) }
    fun setSearchDepth(depth: Int) = viewModelScope.launch { settings.setSearchDepth(depth) }
    fun setDetectorSensitivity(s: DetectorSensitivity) = viewModelScope.launch { settings.setDetectorSensitivity(s) }
    fun setSaveNearMisses(save: Boolean) = viewModelScope.launch { settings.setSaveNearMisses(save) }
    fun setMaxInputSize(size: Int) = viewModelScope.launch { settings.setMaxInputSize(size) }
    fun setTwoInputDefault(enabled: Boolean) = viewModelScope.launch { settings.setTwoInputDefault(enabled) }

    class Factory(private val settings: AppSettings) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(settings) as T
    }
}
