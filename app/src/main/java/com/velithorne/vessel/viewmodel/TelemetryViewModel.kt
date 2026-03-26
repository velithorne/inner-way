package com.velithorne.vessel.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velithorne.vessel.model.GrowthVisualState
import com.velithorne.vessel.model.OrganInspectionState
import com.velithorne.vessel.model.VesselVisualState
import com.velithorne.vessel.morphogenesis.GrowthExplainer
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.velithorne.vessel.growthtime.GrowthSessionSummary
import com.velithorne.vessel.growthtime.GrowthTimeCoordinator
import com.velithorne.vessel.morphogenesis.MorphogenesisEngine
import com.velithorne.vessel.morphogenesis.MorphogenesisSnapshot
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
    private val morphogenesisEngine: MorphogenesisEngine,
    private val growthTimeCoordinator: GrowthTimeCoordinator,
) : ViewModel() {

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                growthTimeCoordinator.markBackground()
            }
        })
    }

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

    private val initialMorphRaw = morphogenesisEngine.update(initialPhysiology)
    private val initialMorph = growthTimeCoordinator.process(initialMorphRaw)
    private val initialScene = vesselRenderer.map(initialPhysiology, initialMorph)

    val morphogenesis: StateFlow<MorphogenesisSnapshot> = physiology
        .map { raw ->
            growthTimeCoordinator.process(morphogenesisEngine.update(raw))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = growthTimeCoordinator.process(initialMorph),
        )

    val growthReturnSummary: StateFlow<GrowthSessionSummary?> = growthTimeCoordinator.returnSummary

    val vesselScene: StateFlow<VesselSceneState> = combine(
        physiology,
        morphogenesis,
    ) { phys, morph ->
        vesselRenderer.map(phys, morph)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = initialScene,
    )

    val growthVisual: StateFlow<GrowthVisualState> = combine(
        morphogenesis,
        growthReturnSummary,
    ) { morph, ret ->
        GrowthVisualState(
            snapshot = morph,
            statusLabel = morph.growthStatusLabel,
            statusLine = morph.growthStatusLine,
            visibleActivity = morph.visibleGrowthActivity,
            germinationStageLabel = GrowthExplainer.stageDisplayName(morph.germinationStage),
            growthProgressFraction = growthTimeCoordinator.displayProgressFraction(),
            activeBudgetChannelLabel = growthTimeCoordinator.activeBudgetChannelLabel(),
            recentAwayLine = growthTimeCoordinator.recentAwayLine(),
            returnSummary = ret,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GrowthVisualState(
            snapshot = initialMorph,
            statusLabel = initialMorph.growthStatusLabel,
            statusLine = initialMorph.growthStatusLine,
            visibleActivity = initialMorph.visibleGrowthActivity,
            germinationStageLabel = GrowthExplainer.stageDisplayName(initialMorph.germinationStage),
            growthProgressFraction = growthTimeCoordinator.displayProgressFraction(),
            activeBudgetChannelLabel = growthTimeCoordinator.activeBudgetChannelLabel(),
            recentAwayLine = growthTimeCoordinator.recentAwayLine(),
            returnSummary = growthTimeCoordinator.returnSummary.value,
        ),
    )

    fun dismissReturnGrowthSummary() {
        growthTimeCoordinator.dismissReturnSummary()
    }

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
        morphogenesis,
        _selectedVesselOrgan,
    ) { snap, morph, organ ->
        if (organ == null) null else OrganInspectionState.build(organ, snap.organs, morph)
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
