package com.velithorne.vessel.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velithorne.vessel.branching.BranchExplainer
import com.velithorne.vessel.branching.BranchInfluenceModel
import com.velithorne.vessel.branching.BranchingTuning
import com.velithorne.vessel.renderer_seedpod.SeedPodBranchMapper
import com.velithorne.vessel.data.LineageRepository
import com.velithorne.vessel.growthtime.GrowthTimeCoordinator
import com.velithorne.vessel.growth_seedpod.SeedPodExplainer
import com.velithorne.vessel.growth_seedpod.SeedPodGrowthCoordinator
import com.velithorne.vessel.lineage.LineageSummary
import com.velithorne.vessel.lineage.SeedPodReturnSummary
import com.velithorne.vessel.lineage.SpecimenIdentity
import com.velithorne.vessel.morphogenesis.MorphogenesisEngine
import com.velithorne.vessel.morphogenesis.MorphogenesisSnapshot
import com.velithorne.vessel.model.OrganInspectionState
import com.velithorne.vessel.model.SeedPodVesselUiState
import com.velithorne.vessel.physiology.OrganType
import com.velithorne.vessel.physiology.PhysiologyEngine
import com.velithorne.vessel.physiology.PhysiologySnapshot
import com.velithorne.vessel.progression.ProgressBarModelFactory
import com.velithorne.vessel.renderer_seedpod.SeedPodRenderer
import com.velithorne.vessel.renderer_seedpod.SeedPodSceneState
import com.velithorne.vessel.telemetry.TelemetryRepository
import com.velithorne.vessel.telemetry.TelemetrySnapshot
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Telemetry → physiology → **SeedPod** scene for the Vessel tab.
 */
class TelemetryViewModel(
    private val repository: TelemetryRepository,
    private val physiologyEngine: PhysiologyEngine,
    private val seedPodRenderer: SeedPodRenderer,
    private val seedPodGrowthCoordinator: SeedPodGrowthCoordinator,
    private val morphogenesisEngine: MorphogenesisEngine,
    private val growthTimeCoordinator: GrowthTimeCoordinator,
    private val lineageRepository: LineageRepository,
) : ViewModel() {

    private val _seedPodReturnSummary = MutableStateFlow<SeedPodReturnSummary?>(null)
    val seedPodReturnSummary: StateFlow<SeedPodReturnSummary?> = _seedPodReturnSummary.asStateFlow()

    private val _specimenIdentity = MutableStateFlow<SpecimenIdentity?>(null)
    val specimenIdentity: StateFlow<SpecimenIdentity?> = _specimenIdentity.asStateFlow()

    private val _lineageSummary = MutableStateFlow<LineageSummary?>(null)
    val lineageSummary: StateFlow<LineageSummary?> = _lineageSummary.asStateFlow()

    init {
        loadIdentity()
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                growthTimeCoordinator.markBackground()
                seedPodGrowthCoordinator.markBackground()
            }

            override fun onStart(owner: LifecycleOwner) {
                viewModelScope.launch {
                    val phys = physiologyEngine.update(repository.snapshot.value)
                    val summary = seedPodGrowthCoordinator.catchUpOffline(phys)
                    if (summary != null && summary.lines.isNotEmpty()) {
                        val key = summary.lines.joinToString("|") + summary.awaySeconds
                        val show = withContext(Dispatchers.IO) {
                            lineageRepository.shouldShowSeedPodReturn(key)
                        }
                        if (show) {
                            _seedPodReturnSummary.value = summary
                        }
                    }
                }
            }
        })
    }

    private fun loadIdentity() {
        viewModelScope.launch {
            val id = withContext(Dispatchers.IO) { lineageRepository.getActiveIdentity() }
            _specimenIdentity.value = id
            if (id != null) {
                _lineageSummary.value = lineageRepository.lineageSummaryFromIdentity(id)
            }
        }
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

    val morphogenesis: StateFlow<MorphogenesisSnapshot> = physiology
        .map { raw ->
            growthTimeCoordinator.process(morphogenesisEngine.update(raw))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = growthTimeCoordinator.process(initialMorph),
        )

    val growthReturnSummary = growthTimeCoordinator.returnSummary

    private val branchingTuning = BranchingTuning()

    private val initialPodGrowth = seedPodGrowthCoordinator.process(initialPhysiology)
    private val initialBranchVisual = SeedPodBranchMapper.map(
        initialPodGrowth.structural.morphologyBranch,
        initialPodGrowth.structural.permanentStage,
        branchingTuning,
    )
    private val initialSeedPodScene = seedPodRenderer.map(
        initialPhysiology,
        initialPodGrowth.display.copy(stage = initialPodGrowth.structural.permanentStage),
        initialBranchVisual,
    )

    val seedPodScene: StateFlow<SeedPodSceneState> = physiology
        .map { phys ->
            val growth = seedPodGrowthCoordinator.process(phys)
            val display = growth.display.copy(stage = growth.structural.permanentStage)
            val branchVisual = SeedPodBranchMapper.map(
                growth.structural.morphologyBranch,
                growth.structural.permanentStage,
                branchingTuning,
            )
            seedPodRenderer.map(phys, display, branchVisual)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = initialSeedPodScene,
        )

    val seedPodVesselUi: StateFlow<SeedPodVesselUiState> = combine(
        seedPodScene,
        growthReturnSummary,
        seedPodReturnSummary,
    ) { scene, ret, seedRet ->
        val gs = seedPodGrowthCoordinator.current()
        val strain = scene.physiology.species.let {
            (it.stress * 0.5f + it.fever * 0.35f + (1f - it.vitality) * 0.15f).coerceIn(0f, 1f)
        }
        val pb = ProgressBarModelFactory.build(
            stage = gs.structural.permanentStage,
            smoothedProgress = gs.structural.smoothedStructuralProgress,
            nextAccum = gs.structural.nextStageAccum,
            strain = strain,
        )
        SeedPodVesselUiState(
            structuralStageLabel = SeedPodExplainer.stageLabel(gs.structural.permanentStage),
            liveConditionLabel = scene.liveExpression.conditionLabel,
            statusLine = SeedPodExplainer.statusLine(scene.physiology, gs),
            growthProgressFraction = pb.structuralProgress,
            progressCaption = pb.progressCaption,
            nextStageLabel = pb.nextStageLabel,
            liveStrainIndicator = pb.liveStrainIndicator,
            activeBudgetChannelLabel = growthTimeCoordinator.activeBudgetChannelLabel(),
            recentAwayLine = growthTimeCoordinator.recentAwayLine(),
            returnSummary = ret,
            seedPodReturnSummary = seedRet,
            branchStatusLine = run {
                val lead = gs.structural.morphologyBranch.leadingBranch()
                BranchExplainer.leadingLine(lead, gs.structural.morphologyBranch.branchReadiness, gs.structural.permanentStage).ifEmpty {
                    if (gs.structural.permanentStage.ordinal >= com.velithorne.vessel.growth_seedpod.SeedPodGrowthStage.LINEAGE_DIFFERENTIATING.ordinal) {
                        "Branch readiness ${(gs.structural.morphologyBranch.branchReadiness * 100f).toInt()}% · lead ${lead.displayName}"
                    } else ""
                }
            },
            branchReasonLine = run {
                val markers = lineageRepository.getAdaptationMarkersSync(seedPodGrowthCoordinator.specimenId())
                val (dev, eco) = BranchInfluenceModel.fromTelemetryForStep(scene.physiology.telemetry, markers)
                BranchExplainer.reasonLine(dev, eco, gs.structural.morphologyBranch.leadingBranch())
            },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = run {
            val gs = initialPodGrowth
            val strain = initialPhysiology.species.let {
                (it.stress * 0.5f + it.fever * 0.35f + (1f - it.vitality) * 0.15f).coerceIn(0f, 1f)
            }
            val pb = ProgressBarModelFactory.build(
                gs.structural.permanentStage,
                gs.structural.smoothedStructuralProgress,
                gs.structural.nextStageAccum,
                strain,
            )
            SeedPodVesselUiState(
                structuralStageLabel = SeedPodExplainer.stageLabel(gs.structural.permanentStage),
                liveConditionLabel = com.velithorne.vessel.progression.LiveExpressionMapper.map(initialPhysiology).conditionLabel,
                statusLine = SeedPodExplainer.statusLine(initialPhysiology, gs),
                growthProgressFraction = pb.structuralProgress,
                progressCaption = pb.progressCaption,
                nextStageLabel = pb.nextStageLabel,
                liveStrainIndicator = pb.liveStrainIndicator,
                activeBudgetChannelLabel = growthTimeCoordinator.activeBudgetChannelLabel(),
                recentAwayLine = growthTimeCoordinator.recentAwayLine(),
                returnSummary = growthTimeCoordinator.returnSummary.value,
                seedPodReturnSummary = null,
                branchStatusLine = run {
                    val lead = gs.structural.morphologyBranch.leadingBranch()
                    BranchExplainer.leadingLine(lead, gs.structural.morphologyBranch.branchReadiness, gs.structural.permanentStage).ifEmpty {
                        if (gs.structural.permanentStage.ordinal >= com.velithorne.vessel.growth_seedpod.SeedPodGrowthStage.LINEAGE_DIFFERENTIATING.ordinal) {
                            "Branch readiness ${(gs.structural.morphologyBranch.branchReadiness * 100f).toInt()}% · lead ${lead.displayName}"
                        } else ""
                    }
                },
                branchReasonLine = run {
                    val markers = lineageRepository.getAdaptationMarkersSync(seedPodGrowthCoordinator.specimenId())
                    val (dev, eco) = BranchInfluenceModel.fromTelemetryForStep(initialPhysiology.telemetry, markers)
                    BranchExplainer.reasonLine(dev, eco, gs.structural.morphologyBranch.leadingBranch())
                },
            )
        },
    )

    fun dismissReturnGrowthSummary() {
        growthTimeCoordinator.dismissReturnSummary()
    }

    fun dismissSeedPodReturnSummary() {
        val s = _seedPodReturnSummary.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val key = s.lines.joinToString("|") + s.awaySeconds
            lineageRepository.markSeedPodReturnShown(key)
        }
        _seedPodReturnSummary.value = null
    }

    private val _selectedVesselOrgan = MutableStateFlow<OrganType?>(null)
    val selectedVesselOrgan: StateFlow<OrganType?> = _selectedVesselOrgan

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
