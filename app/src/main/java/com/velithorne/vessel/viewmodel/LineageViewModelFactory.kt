package com.velithorne.vessel.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.velithorne.vessel.background.EcologySnapshotStore
import com.velithorne.vessel.config.GrowthProfileProvider
import com.velithorne.vessel.data.LineageRepository
import com.velithorne.vessel.growth_seedpod.SeedPodGrowthCoordinator

class LineageViewModelFactory(
    private val application: Application,
    private val lineageRepository: LineageRepository,
    private val ecologySnapshotStore: EcologySnapshotStore,
    private val growthProfileProvider: GrowthProfileProvider,
    private val seedPodGrowthCoordinator: SeedPodGrowthCoordinator,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LineageViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LineageViewModel(
                application,
                lineageRepository,
                ecologySnapshotStore,
                growthProfileProvider,
                seedPodGrowthCoordinator,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
