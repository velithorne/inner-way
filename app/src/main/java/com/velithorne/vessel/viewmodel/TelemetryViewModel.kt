package com.velithorne.vessel.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velithorne.vessel.telemetry.TelemetryRepository
import com.velithorne.vessel.telemetry.TelemetrySnapshot
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * UI-facing telemetry. Survives configuration changes via [ViewModel].
 *
 * Future: merge in [com.velithorne.vessel.domain.phase2.SpeciesState] / physiology deltas for HUD overlays.
 */
class TelemetryViewModel(
    private val repository: TelemetryRepository,
) : ViewModel() {

    val telemetry: StateFlow<TelemetrySnapshot> = repository.snapshot
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = repository.snapshot.value,
        )

    /**
     * Repository is process-scoped and driven by [ProcessLifecycleOwner].
     * ViewModel does not duplicate start/stop for sensors — avoids duplicate listeners.
     */
}
