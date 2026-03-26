package com.velithorne.vessel.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velithorne.vessel.physiology.PhysiologyEngine
import com.velithorne.vessel.physiology.PhysiologySnapshot
import com.velithorne.vessel.telemetry.TelemetryRepository
import com.velithorne.vessel.telemetry.TelemetrySnapshot
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Exposes raw [TelemetrySnapshot] and derived [PhysiologySnapshot].
 *
 * Phase 3: [com.velithorne.vessel.domain.phase2.VesselRenderer] observes [physiology] only.
 * Phase 4+: evolution engine may sample long-horizon tails of the same flow.
 */
class TelemetryViewModel(
    private val repository: TelemetryRepository,
    private val physiologyEngine: PhysiologyEngine,
) : ViewModel() {

    val telemetry: StateFlow<TelemetrySnapshot> = repository.snapshot
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = repository.snapshot.value,
        )

    private val initialPhysiology = physiologyEngine.update(repository.snapshot.value)

    val physiology: StateFlow<PhysiologySnapshot> = repository.snapshot
        .map { physiologyEngine.update(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = initialPhysiology,
        )
}
