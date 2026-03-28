package com.velithorne.vessel.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velithorne.vessel.background.AmbientEcologyPresentation
import com.velithorne.vessel.background.AmbientEventIngestor
import com.velithorne.vessel.background.EcologySnapshot
import com.velithorne.vessel.background.EcologySnapshotSourceType
import com.velithorne.vessel.BuildConfig
import com.velithorne.vessel.background.ReturnSummaryComposer
import com.velithorne.vessel.config.DevSettingsStore
import com.velithorne.vessel.config.GrowthProfile
import com.velithorne.vessel.config.GrowthProfileProvider
import com.velithorne.vessel.config.GrowthProfileScaler
import com.velithorne.vessel.config.SimulationMode
import com.velithorne.vessel.branching.BranchExplainer
import com.velithorne.vessel.branching.BranchInfluenceModel
import com.velithorne.vessel.renderer_seedpod.SeedPodBranchMapper
import com.velithorne.vessel.data.LineageRepository
import com.velithorne.vessel.growthtime.GrowthTimeCoordinator
import com.velithorne.vessel.growth_seedpod.SeedPodExplainer
import com.velithorne.vessel.growth_seedpod.SeedPodGrowthCoordinator
import com.velithorne.vessel.growth_seedpod.SeedPodGrowthState
import com.velithorne.vessel.lineage.LineageSummary
import com.velithorne.vessel.lineage.SeedPodReturnSummary
import com.velithorne.vessel.lineage.SpecimenIdentity
import com.velithorne.vessel.data.prefs.GrowthStateStore
import com.velithorne.vessel.morphogenesis.MorphogenesisEngine
import com.velithorne.vessel.morphogenesis.MorphogenesisSnapshot
import com.velithorne.vessel.model.BiographyVisualState
import com.velithorne.vessel.model.OrganInspectionState
import com.velithorne.vessel.model.SeedPodVesselUiState
import com.velithorne.vessel.physiology.OrganType
import com.velithorne.vessel.physiology.PhysiologyEngine
import com.velithorne.vessel.physiology.PhysiologySnapshot
import com.velithorne.vessel.progression.ProgressBarModelFactory
import com.velithorne.vessel.juvenile_form.JuvenileExplainer
import com.velithorne.vessel.renderer_seedpod.SeedPodRenderer
import com.velithorne.vessel.renderer_seedpod.SeedPodSceneState
import com.velithorne.vessel.renderer_seedpod.SeedPodTuning
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
    private val application: Application,
    private val repository: TelemetryRepository,
    private val physiologyEngine: PhysiologyEngine,
    private val seedPodRenderer: SeedPodRenderer,
    private val seedPodGrowthCoordinator: SeedPodGrowthCoordinator,
    private val morphogenesisEngine: MorphogenesisEngine,
    private val growthTimeCoordinator: GrowthTimeCoordinator,
    private val lineageRepository: LineageRepository,
    private val ambientEventIngestor: AmbientEventIngestor,
    private val growthProfileProvider: GrowthProfileProvider,
) : ViewModel() {

    private val profile get() = growthProfileProvider.profile
    private val devSettings = DevSettingsStore(application)

    private val _devEvolutionSpeedMultiplier = MutableStateFlow(devSettings.devEvolutionSpeedMultiplier)
    /** Debug dev mode: persisted evolution speed (1 = default dev profile). */
    val devEvolutionSpeedMultiplier: StateFlow<Float> = _devEvolutionSpeedMultiplier.asStateFlow()

    private fun scaledGrowthProfile(): GrowthProfile =
        if (BuildConfig.DEBUG && profile.mode == SimulationMode.DEV_SIMULATION) {
            GrowthProfileScaler.scale(profile, devSettings.devEvolutionSpeedMultiplier)
        } else {
            profile
        }

    private fun syncDevEvolutionEnginesFromPrefs() {
        if (!BuildConfig.DEBUG || profile.mode != SimulationMode.DEV_SIMULATION) return
        val scaled = scaledGrowthProfile()
        growthTimeCoordinator.updateTimeTuning(scaled.time)
        seedPodGrowthCoordinator.updateTuningFromProfile(scaled)
    }

    /**
     * Debug dev mode only: scales structural progression, morphogenesis time tuning, and seed-pod display rates.
     */
    fun setDevEvolutionSpeedMultiplier(value: Float) {
        if (!BuildConfig.DEBUG || profile.mode != SimulationMode.DEV_SIMULATION) return
        val v = GrowthProfileScaler.clampMultiplier(value)
        devSettings.devEvolutionSpeedMultiplier = v
        _devEvolutionSpeedMultiplier.value = v
        val scaled = GrowthProfileScaler.scale(profile, v)
        growthTimeCoordinator.updateTimeTuning(scaled.time)
        seedPodGrowthCoordinator.updateTuningFromProfile(scaled)
    }

    private val _seedPodReturnSummary = MutableStateFlow<SeedPodReturnSummary?>(null)
    val seedPodReturnSummary: StateFlow<SeedPodReturnSummary?> = _seedPodReturnSummary.asStateFlow()

    private val _specimenIdentity = MutableStateFlow<SpecimenIdentity?>(null)
    val specimenIdentity: StateFlow<SpecimenIdentity?> = _specimenIdentity.asStateFlow()

    private val _lineageSummary = MutableStateFlow<LineageSummary?>(null)
    val lineageSummary: StateFlow<LineageSummary?> = _lineageSummary.asStateFlow()

    private val _ambientEcologyHintLine = MutableStateFlow("")

    init {
        viewModelScope.launch {
            if (devSettings.pendingSpecimenReset && BuildConfig.DEBUG) {
                performDevSpecimenReset()
                devSettings.clearPendingReset()
            }
        }
        loadIdentity()
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                growthTimeCoordinator.markBackground()
                seedPodGrowthCoordinator.markBackground()
                viewModelScope.launch {
                    ambientEventIngestor.onAppBackground()
                }
            }

            override fun onStart(owner: LifecycleOwner) {
                viewModelScope.launch {
                    val phys = physiologyEngine.update(repository.snapshot.value)
                    val folded = seedPodGrowthCoordinator.foldPendingAmbientEcology(phys)
                    val summary = seedPodGrowthCoordinator.catchUpOffline(phys)
                    val merged = ReturnSummaryComposer.mergeWithExisting(
                        folded?.lines.orEmpty(),
                        summary,
                        awaySeconds = summary?.awaySeconds ?: folded?.awaySeconds ?: 0L,
                    )
                    ambientEventIngestor.onAppForeground()
                    val gs = seedPodGrowthCoordinator.current()
                    lineageRepository.tryRecordEcologySnapshot(
                        EcologySnapshot.fromTelemetryAndState(
                            repository.snapshot.value,
                            gs,
                            EcologySnapshotSourceType.APP_RESUME,
                        ),
                    )
                    val sid = seedPodGrowthCoordinator.specimenId()
                    val recent = withContext(Dispatchers.IO) {
                        lineageRepository.recentEcologySnapshots(sid, 24)
                    }
                    val meta = withContext(Dispatchers.IO) { lineageRepository.getAmbientEcologyMeta(sid) }
                    val lastEvt = withContext(Dispatchers.IO) {
                        lineageRepository.recentAmbientEvents(sid, 1).firstOrNull()
                    }
                    val workOk = AmbientEcologyPresentation.workScheduled(application)
                    _ambientEcologyHintLine.value = AmbientEcologyPresentation.build(
                        recent,
                        meta?.samplesSinceLastOpen ?: 0,
                        meta = meta,
                        lastEvent = lastEvt,
                        workScheduled = workOk,
                        tuning = profile.background,
                    ).vesselHintLine
                    if (merged != null && merged.lines.isNotEmpty()) {
                        val key = merged.lines.joinToString("|") + merged.awaySeconds
                        val ambientHash = ReturnSummaryComposer.ambientSummaryHash(merged.lines)
                        val show = withContext(Dispatchers.IO) {
                            lineageRepository.shouldShowSeedPodReturnCombined(key, ambientHash)
                        }
                        if (show) {
                            _seedPodReturnSummary.value = merged
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

    private val initialPhysiology = run {
        syncDevEvolutionEnginesFromPrefs()
        physiologyEngine.update(repository.snapshot.value)
    }

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

    private val initialPodGrowth = seedPodGrowthCoordinator.process(initialPhysiology)
    private val initialBranchVisual = SeedPodBranchMapper.map(
        initialPodGrowth.structural.morphologyBranch,
        initialPodGrowth.structural.permanentStage,
        scaledGrowthProfile().branching,
    )
    private val initialSeedPodScene = seedPodRenderer.map(
        initialPhysiology,
        initialPodGrowth.display.copy(stage = initialPodGrowth.structural.permanentStage),
        initialBranchVisual,
        generatedAnatomy = initialPodGrowth.lastSelfAssembly?.anatomy,
        biographyVisual = BiographyVisualState.fromNullable(initialPodGrowth.lastSelfAssembly?.biography),
        animTimeSec = System.nanoTime() / 1_000_000_000f,
        simulationMode = profile.mode,
        structuralProgress = initialPodGrowth.structural.smoothedStructuralProgress,
        growthPressure = initialPodGrowth.lastSelfAssembly?.pressure,
    )

    /**
     * One structural step per physiology tick — do not combine with dev speed here or the slider
     * would double-advance growth.
     */
    private val seedPodAfterPhysiology: StateFlow<Pair<PhysiologySnapshot, SeedPodGrowthState>> =
        physiology
            .map { phys ->
                val growth = seedPodGrowthCoordinator.process(phys)
                phys to growth
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = initialPhysiology to initialPodGrowth,
            )

    val seedPodScene: StateFlow<SeedPodSceneState> = combine(
        seedPodAfterPhysiology,
        devEvolutionSpeedMultiplier,
    ) { (phys, growth), _ ->
        val display = growth.display.copy(stage = growth.structural.permanentStage)
        val branchVisual = SeedPodBranchMapper.map(
            growth.structural.morphologyBranch,
            growth.structural.permanentStage,
            scaledGrowthProfile().branching,
        )
        val snap = growth.lastSelfAssembly
        seedPodRenderer.map(
            phys,
            display,
            branchVisual,
            generatedAnatomy = snap?.anatomy,
            biographyVisual = BiographyVisualState.fromNullable(snap?.biography),
            animTimeSec = System.nanoTime() / 1_000_000_000f,
            simulationMode = profile.mode,
            structuralProgress = growth.structural.smoothedStructuralProgress,
            growthPressure = snap?.pressure,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = initialSeedPodScene,
    )

    val seedPodVesselUi: StateFlow<SeedPodVesselUiState> = combine(
        seedPodScene,
        growthReturnSummary,
        seedPodReturnSummary,
        _ambientEcologyHintLine,
        devEvolutionSpeedMultiplier,
    ) { scene, ret, seedRet, ambientHint, devSpeed ->
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
        val vm = scene.visibleMorphology
        val driver = vm.dominantContourDriver
        val topoLines = vm.topologySummaryLines
        val jf = scene.juvenileForm
        val traitChips = if (jf.active) {
            buildList {
                if (jf.bodyPlan.crownMass > 0.5f) add("Crown region")
                if (jf.bodyPlan.lateralMass > 0.5f) add("Lateral spread")
                if (jf.bodyPlan.reserveMass > 0.5f) add("Reserve basin")
                if (jf.bodyPlan.shellMass > 0.5f) add("Shell plates")
                if (jf.bodyPlan.supportMass > 0.5f) add("Support braces")
                if (jf.bodyPlan.archiveMass > 0.5f) add("Archive core")
            }
        } else emptyList()
        val topoStageLine = when {
            !jf.active -> null
            jf.transition.topologyExpandedBeyondSeed -> "Topology: expanded beyond seed phase"
            else -> "Topology: consolidating juvenile regions"
        }
        val juvenileLines = buildList {
            val h = JuvenileExplainer.headline(jf)
            if (h.isNotEmpty()) add(h)
            val r = JuvenileExplainer.regionLine(jf)
            if (r.isNotEmpty()) add(r)
            val t = JuvenileExplainer.transitionLine(jf)
            if (t.isNotEmpty()) add(t)
            val b = JuvenileExplainer.branchFamilyLine(gs.structural.morphologyBranch.leadingBranch())
            if (jf.active) add(b)
        }
        val debugVis = if (BuildConfig.DEBUG && SeedPodTuning().showSeedPodDebug) {
            val cg = scene.contourGeometry
            val sb = scene.seedBurial
            "topology ${(vm.generatedTopologyInfluence * 100f).toInt()}% · seed ${(vm.fallbackSeedInfluence * 100f).toInt()}% · asym ${(vm.visibleAsymmetryScore * 100f).toInt()}% · n=${cg.sampleCount} · smooth=${cg.smoothingPasses} · relax=${cg.relaxationIterations} · spline=${cg.splineEnabled} · ghostα=${(sb.outerShellGhostAlphaMul * 100f).toInt()}%"
        } else null
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
                val lead = gs.structural.morphologyBranch.leadingBranch()
                buildString {
                    append(BranchExplainer.reasonLine(dev, eco, lead))
                    append("\n")
                    append(BranchExplainer.vignetteLine(lead, eco))
                }
            },
            ambientEcologyHintLine = ambientHint,
            devSimulationHintLine = if (BuildConfig.DEBUG && profile.mode == SimulationMode.DEV_SIMULATION) {
                "Accelerated growth profile (dev)"
            } else null,
            devEvolutionSpeedMultiplier = if (BuildConfig.DEBUG && profile.mode == SimulationMode.DEV_SIMULATION) {
                devSpeed
            } else null,
            visibleTopologyLines = topoLines,
            morphologyDriverLine = "Contour driver: $driver",
            visibilityDebugLine = debugVis,
            juvenileFormLines = juvenileLines,
            juvenileTraitChips = traitChips,
            juvenileTopologyStageLine = topoStageLine,
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
                    val lead = gs.structural.morphologyBranch.leadingBranch()
                    buildString {
                        append(BranchExplainer.reasonLine(dev, eco, lead))
                        append("\n")
                        append(BranchExplainer.vignetteLine(lead, eco))
                    }
                },
                ambientEcologyHintLine = "",
                devSimulationHintLine = if (BuildConfig.DEBUG && profile.mode == SimulationMode.DEV_SIMULATION) {
                    "Accelerated growth profile (dev)"
                } else null,
                devEvolutionSpeedMultiplier = if (BuildConfig.DEBUG && profile.mode == SimulationMode.DEV_SIMULATION) {
                    devSettings.devEvolutionSpeedMultiplier
                } else null,
                visibleTopologyLines = initialSeedPodScene.visibleMorphology.topologySummaryLines,
                morphologyDriverLine = "Contour driver: ${initialSeedPodScene.visibleMorphology.dominantContourDriver}",
                juvenileFormLines = run {
                    val jf = initialSeedPodScene.juvenileForm
                    buildList {
                        val h = JuvenileExplainer.headline(jf)
                        if (h.isNotEmpty()) add(h)
                        val r = JuvenileExplainer.regionLine(jf)
                        if (r.isNotEmpty()) add(r)
                        val t = JuvenileExplainer.transitionLine(jf)
                        if (t.isNotEmpty()) add(t)
                        val b = JuvenileExplainer.branchFamilyLine(gs.structural.morphologyBranch.leadingBranch())
                        if (jf.active) add(b)
                    }
                },
                juvenileTraitChips = run {
                    val jf = initialSeedPodScene.juvenileForm
                    if (!jf.active) emptyList()
                    else buildList {
                        if (jf.bodyPlan.crownMass > 0.5f) add("Crown region")
                        if (jf.bodyPlan.lateralMass > 0.5f) add("Lateral spread")
                        if (jf.bodyPlan.reserveMass > 0.5f) add("Reserve basin")
                        if (jf.bodyPlan.shellMass > 0.5f) add("Shell plates")
                        if (jf.bodyPlan.supportMass > 0.5f) add("Support braces")
                        if (jf.bodyPlan.archiveMass > 0.5f) add("Archive core")
                    }
                },
                juvenileTopologyStageLine = run {
                    val jf = initialSeedPodScene.juvenileForm
                    when {
                        !jf.active -> null
                        jf.transition.topologyExpandedBeyondSeed -> "Topology: expanded beyond seed phase"
                        else -> "Topology: consolidating juvenile regions"
                    }
                },
                visibilityDebugLine = if (BuildConfig.DEBUG && SeedPodTuning().showSeedPodDebug) {
                    val vm = initialSeedPodScene.visibleMorphology
                    val cg = initialSeedPodScene.contourGeometry
                    val sb = initialSeedPodScene.seedBurial
                    "topology ${(vm.generatedTopologyInfluence * 100f).toInt()}% · seed ${(vm.fallbackSeedInfluence * 100f).toInt()}% · asym ${(vm.visibleAsymmetryScore * 100f).toInt()}% · n=${cg.sampleCount} · smooth=${cg.smoothingPasses} · relax=${cg.relaxationIterations} · spline=${cg.splineEnabled} · ghostα=${(sb.outerShellGhostAlphaMul * 100f).toInt()}%"
                } else null,
            )
        },
    )

    fun requestDevSpecimenReset() {
        if (!BuildConfig.DEBUG || profile.mode != SimulationMode.DEV_SIMULATION) return
        devSettings.pendingSpecimenReset = true
        viewModelScope.launch { performDevSpecimenReset() }
    }

    private suspend fun performDevSpecimenReset() {
        withContext(Dispatchers.IO) {
            lineageRepository.clearAllLineage()
            GrowthStateStore(application).clear()
        }
        morphogenesisEngine.resetForNewBuild()
        growthTimeCoordinator.resetSessionState()
        seedPodGrowthCoordinator.reloadFromRepository()
        _seedPodReturnSummary.value = null
        loadIdentity()
    }

    fun dismissReturnGrowthSummary() {
        growthTimeCoordinator.dismissReturnSummary()
    }

    fun dismissSeedPodReturnSummary() {
        val s = _seedPodReturnSummary.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val key = s.lines.joinToString("|") + s.awaySeconds
            val ambientHash = ReturnSummaryComposer.ambientSummaryHash(s.lines)
            lineageRepository.markSeedPodReturnShown(key, ambientHash)
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
