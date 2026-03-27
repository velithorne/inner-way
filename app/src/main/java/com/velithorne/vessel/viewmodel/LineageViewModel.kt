package com.velithorne.vessel.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velithorne.vessel.data.LineageRepository
import com.velithorne.vessel.lineage.AdaptationMarker
import com.velithorne.vessel.lineage.GrowthEvent
import com.velithorne.vessel.lineage.GrowthHistory
import com.velithorne.vessel.lineage.LineageSummary
import com.velithorne.vessel.lineage.SpecimenIdentity
import com.velithorne.vessel.lineage.SpecimenLineage
import com.velithorne.vessel.lineage.StageTransition
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
)

class LineageViewModel(
    private val lineageRepository: LineageRepository,
) : ViewModel() {

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
            _ui.value = LineageUiState(
                identity = id,
                lineage = lineage,
                summary = lineageRepository.lineageSummaryFromIdentity(id),
                growthEvents = hist.growthEvents,
                stageTransitions = hist.stageTransitions,
                adaptations = adapt,
            )
        }
    }
}
