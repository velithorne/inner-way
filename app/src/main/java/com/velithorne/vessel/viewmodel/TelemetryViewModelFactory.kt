package com.velithorne.vessel.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.velithorne.vessel.background.AmbientEventIngestor
import com.velithorne.vessel.data.LineageRepository
import com.velithorne.vessel.growth_seedpod.SeedPodGrowthCoordinator
import com.velithorne.vessel.growthtime.GrowthTimeCoordinator
import com.velithorne.vessel.morphogenesis.MorphogenesisEngine
import com.velithorne.vessel.physiology.PhysiologyEngine
import com.velithorne.vessel.renderer_seedpod.SeedPodRenderer
import com.velithorne.vessel.telemetry.TelemetryRepository

class TelemetryViewModelFactory(
    private val repository: TelemetryRepository,
    private val physiologyEngine: PhysiologyEngine,
    private val seedPodRenderer: SeedPodRenderer,
    private val seedPodGrowthCoordinator: SeedPodGrowthCoordinator,
    private val morphogenesisEngine: MorphogenesisEngine,
    private val growthTimeCoordinator: GrowthTimeCoordinator,
    private val lineageRepository: LineageRepository,
    private val ambientEventIngestor: AmbientEventIngestor,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TelemetryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TelemetryViewModel(
                repository,
                physiologyEngine,
                seedPodRenderer,
                seedPodGrowthCoordinator,
                morphogenesisEngine,
                growthTimeCoordinator,
                lineageRepository,
                ambientEventIngestor,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
