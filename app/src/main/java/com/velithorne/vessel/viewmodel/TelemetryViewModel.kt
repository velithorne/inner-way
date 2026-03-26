package com.velithorne.vessel.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velithorne.vessel.model.OrganInspectionState
import com.velithorne.vessel.model.VesselVisualState
import com.velithorne.vessel.physiology.OrganType
import com.velithorne.vessel.physiology.PhysiologyEngine
import com.velithorne.vessel.physiology.PhysiologySnapshot
import com.velithorne.vessel.renderer.VesselRenderer
import com.velithorne.vessel.renderer.VesselSceneState
import com.velithorne.vessel.telemetry.TelemetryRepository
import com.velithorne.vessel.telemetry.TelemetrySnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Telemetry → physiology → vessel scene (Phase 3–4).
 * Phase 4: vessel organ selection + inspection content (camera stays in [VesselGestureController]).
 */
class TelemetryViewModel(
    private val repository: TelemetryRepository,
    private val physiologyEngine: PhysiologyEngine,
    private val vesselRenderer: VesselRenderer,
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

    private val initialScene = vesselRenderer.map(initialPhysiology)

    val vesselScene: StateFlow<VesselSceneState> = physiology
        .map { vesselRenderer.map(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = initialScene,
        )

    val vesselVisual: StateFlow<VesselVisualState> = vesselScene
        .map { VesselVisualState(scene = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = VesselVisualState(initialScene),
        )

    private val _selectedVesselOrgan = MutableStateFlow<OrganType?>(null)
    val selectedVesselOrgan: StateFlow<OrganType?> = _selectedVesselOrgan

    private val _vesselSheetVisible = MutableStateFlow(false)
    val vesselSheetVisible: StateFlow<Boolean> = _vesselSheetVisible

    val organInspection: StateFlow<OrganInspectionState?> = combine(
        physiology,
        _selectedVesselOrgan,
    ) { snap, organ ->
        if (organ == null) null else OrganInspectionState.build(organ, snap.organs)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    fun selectVesselOrgan(type: OrganType?) {
        _selectedVesselOrgan.value = type
        _vesselSheetVisible.value = type != null
    }

    fun showVesselSheet(visible: Boolean) {
        _vesselSheetVisible.update { visible && _selectedVesselOrgan.value != null }
    }

    fun dismissVesselSheet() {
        _vesselSheetVisible.value = false
    }
}
