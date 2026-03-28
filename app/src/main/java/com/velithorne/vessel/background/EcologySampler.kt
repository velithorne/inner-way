package com.velithorne.vessel.background

import android.content.Context
import com.velithorne.vessel.data.LineageRepository
import com.velithorne.vessel.growth_seedpod.SeedPodGrowthState
import com.velithorne.vessel.telemetry.TelemetrySnapshot

/**
 * Records one ecology row and updates meta — called from worker / lifecycle hooks.
 */
class EcologySampler(
    private val appContext: Context,
    private val lineageRepository: LineageRepository,
) {

    suspend fun record(
        telemetry: TelemetrySnapshot,
        growthState: SeedPodGrowthState,
        sourceType: EcologySnapshotSourceType,
        notes: String = "",
    ) {
        val snap = EcologySnapshot.fromTelemetryAndState(telemetry, growthState, sourceType, notes)
        lineageRepository.insertEcologySnapshot(snap)
    }
}
