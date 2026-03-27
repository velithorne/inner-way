package com.velithorne.vessel.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velithorne.vessel.growthtime.GrowthSessionSummary
import com.velithorne.vessel.growthtime.GrowthTimeCoordinator
import com.velithorne.vessel.growth_seedpod.SeedPodExplainer
import com.velithorne.vessel.growth_seedpod.SeedPodGrowthCoordinator
import com.velithorne.vessel.morphogenesis.MorphogenesisEngine
import com.velithorne.vessel.morphogenesis.MorphogenesisSnapshot
import com.velithorne.vessel.model.OrganInspectionState
import com.velithorne.vessel.model.SeedPodVesselUiState
import com.velithorne.vessel.physiology.OrganType
import com.velithorne.vessel.physiology.PhysiologyEngine
import com.velithorne.vessel.physiology.PhysiologySnapshot
import com.velithorne.vessel.renderer_seedpod.SeedPodRenderer
import com.velithorne.vessel.renderer_seedpod.SeedPodSceneState
import com.velithorne.vessel.telemetry.TelemetryRepository
import com.velithorne.vessel.telemetry.TelemetrySnapshot
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Telemetry → physiology → **SeedPod** scene for the Vessel tab.
 * Legacy [com.velithorne.vessel.renderer.VesselRenderer] is **not** in this pipeline.
 */
class TelemetryViewModel(
    private val repository: TelemetryRepository,
    private val physiologyEngine: PhysiologyEngine,
    private val seedPodRenderer: SeedPodRenderer,
    private val seedPodGrowthCoordinator: SeedPodGrowthCoordinator,
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

    /** Morphogenesis still runs for inspection/anatomy text — not for Vessel canvas. */
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

    private val initialPodGrowth = seedPodGrowthCoordinator.process(initialPhysiology)
    private val initialSeedPodScene = seedPodRenderer.map(initialPhysiology, initialPodGrowth.display)

    val seedPodScene: StateFlow<SeedPodSceneState> = physiology
        .map { phys ->
            val growth = seedPodGrowthCoordinator.process(phys)
            seedPodRenderer.map(phys, growth.display)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = initialSeedPodScene,
        )

    val seedPodVesselUi: StateFlow<SeedPodVesselUiState> = combine(
        seedPodScene,
        growthReturnSummary,
    ) { scene, ret ->
        val gs = seedPodGrowthCoordinator.current()
        SeedPodVesselUiState(
            stageLabel = SeedPodExplainer.stageLabel(scene.stage),
            statusLine = SeedPodExplainer.statusLine(scene.physiology, gs),
            growthProgressFraction = seedPodGrowthCoordinator.progressFraction(),
            activeBudgetChannelLabel = growthTimeCoordinator.activeBudgetChannelLabel(),
            recentAwayLine = growthTimeCoordinator.recentAwayLine(),
            returnSummary = ret,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SeedPodVesselUiState(
            stageLabel = SeedPodExplainer.stageLabel(initialPodGrowth.display.stage),
            statusLine = SeedPodExplainer.statusLine(initialPhysiology, initialPodGrowth),
            growthProgressFraction = seedPodGrowthCoordinator.progressFraction(),
            activeBudgetChannelLabel = growthTimeCoordinator.activeBudgetChannelLabel(),
            recentAwayLine = growthTimeCoordinator.recentAwayLine(),
            returnSummary = growthTimeCoordinator.returnSummary.value,
        ),
    )

    fun dismissReturnGrowthSummary() {
        growthTimeCoordinator.dismissReturnSummary()
    }

    private val _selectedVesselOrgan = MutableStateFlow<OrganType?>(null)
    val selectedVesselOrgan: StateFlow<OrganType?> = _selectedVesselOrgan

    /** Seed pod tap target: "pod" | "thermal" — legacy organ selection uses [selectedVesselOrgan]. */
    private val _selectedSeedPodTarget = MutableStateFlow<String?>(null)
    val selectedSeedPodTarget: StateFlow<String?> = _selectedSeedPodTarget

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
        if (type != null) _selectedSeedPodTarget.value = null
    }

    fun selectSeedPodTarget(target: String?) {
        _selectedSeedPodTarget.value = target
        if (target == null) {
            _selectedVesselOrgan.value = null
            _vesselSheetVisible.value = false
            return
        }
        val organ = when (target) {
            "pod" -> OrganType.METABOLIC_HEART
            "thermal" -> OrganType.THERMAL_MEMBRANE
            else -> null
        }
        _selectedVesselOrgan.value = organ
        _vesselSheetVisible.value = organ != null
    }

    fun showVesselSheet(visible: Boolean) {
        _vesselSheetVisible.update { visible && _selectedVesselOrgan.value != null }
    }

    fun dismissVesselSheet() {
        _vesselSheetVisible.value = false
    }
}
