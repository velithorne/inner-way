package com.velithorne.vessel.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velithorne.vessel.background.AmbientEcologyPresentation
import com.velithorne.vessel.background.EcologySnapshotStore
import com.velithorne.vessel.config.GrowthProfileProvider
import com.velithorne.vessel.data.LineageRepository
import com.velithorne.vessel.growth_seedpod.SeedPodGrowthCoordinator
import com.velithorne.vessel.model.BiographyVisualState
import com.velithorne.vessel.renderer_seedpod.SeedPodBranchMapper
import com.velithorne.vessel.renderer_seedpod.VisibilityOverrideMapper
import com.velithorne.vessel.lineage.AdaptationMarker
import com.velithorne.vessel.lineage.GrowthEvent
import com.velithorne.vessel.lineage.GrowthHistory
import com.velithorne.vessel.lineage.LineageSummary
import com.velithorne.vessel.lineage.SpecimenIdentity
import com.velithorne.vessel.lineage.SpecimenLineage
import com.velithorne.vessel.lineage.StageTransition
import com.velithorne.vessel.model.AmbientEcologyUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LineageUiState(
    val identity: SpecimenIdentity? = null,
    val lineage: SpecimenLineage? = null,
    val summary: LineageSummary? = null,
    val growthEvents: List<GrowthEvent> = emptyList(),
    val stageTransitions: List<StageTransition> = emptyList(),
    val adaptations: List<AdaptationMarker> = emptyList(),
    val ambientEcology: AmbientEcologyUiState? = null,
    val visibleTopologyLines: List<String> = emptyList(),
    val morphologyDriverLine: String? = null,
)

class LineageViewModel(
    private val application: Application,
    private val lineageRepository: LineageRepository,
    private val ecologySnapshotStore: EcologySnapshotStore,
    private val growthProfileProvider: GrowthProfileProvider,
    private val seedPodGrowthCoordinator: SeedPodGrowthCoordinator,
) : ViewModel() {

    private val profile get() = growthProfileProvider.profile

    private val _ui = MutableStateFlow(LineageUiState())
    val ui: StateFlow<LineageUiState> = _ui.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val id = withContext(Dispatchers.IO) { lineageRepository.getActiveIdentity() }
            if (id == null) {
                _ui.value = LineageUiState()
                return@launch
            }
            val lineage = withContext(Dispatchers.IO) { lineageRepository.buildLineageView(id.specimenId) }
            val hist: GrowthHistory = withContext(Dispatchers.IO) {
                lineageRepository.getGrowthHistory(id.specimenId, 40)
            }
            val adapt = withContext(Dispatchers.IO) {
                lineageRepository.getAdaptationMarkers(id.specimenId)
            }
            val recent = withContext(Dispatchers.IO) {
                ecologySnapshotStore.recent(id.specimenId, 24)
            }
            val meta = withContext(Dispatchers.IO) {
                lineageRepository.getAmbientEcologyMeta(id.specimenId)
            }
            val lastEvt = withContext(Dispatchers.IO) {
                lineageRepository.recentAmbientEvents(id.specimenId, 1).firstOrNull()
            }
            val workOk = AmbientEcologyPresentation.workScheduled(application)
            val ambient = AmbientEcologyPresentation.build(
                snapshots = recent,
                samplesSinceOpen = meta?.samplesSinceLastOpen ?: 0,
                meta = meta,
                lastEvent = lastEvt,
                workScheduled = workOk,
                tuning = growthProfileProvider.profile.background,
            )
            val gs = seedPodGrowthCoordinator.current()
            val snap = gs.lastSelfAssembly
            val branch = SeedPodBranchMapper.map(
                gs.structural.morphologyBranch,
                gs.structural.permanentStage,
                profile.branching,
            )
            val bio = BiographyVisualState.fromNullable(snap?.biography)
            val vis = VisibilityOverrideMapper.map(
                anatomy = snap?.anatomy,
                biography = bio,
                branch = branch,
                stage = gs.structural.permanentStage,
                structuralProgress = gs.structural.smoothedStructuralProgress,
                mode = profile.mode,
                pressure = snap?.pressure,
            )
            _ui.value = LineageUiState(
                identity = id,
                lineage = lineage,
                summary = lineageRepository.lineageSummaryFromIdentity(id),
                growthEvents = hist.growthEvents,
                stageTransitions = hist.stageTransitions,
                adaptations = adapt,
                ambientEcology = ambient,
                visibleTopologyLines = vis.visible.topologySummaryLines,
                morphologyDriverLine = "Contour driver: ${vis.visible.dominantContourDriver}",
            )
        }
    }
}
