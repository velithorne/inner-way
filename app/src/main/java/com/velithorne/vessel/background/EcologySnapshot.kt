package com.velithorne.vessel.background

import com.velithorne.vessel.growth_seedpod.SeedPodGrowthStage
import com.velithorne.vessel.growth_seedpod.SeedPodGrowthState
import com.velithorne.vessel.telemetry.TelemetrySnapshot

/**
 * Lightweight ecology row — derived from [TelemetrySnapshot] + specimen pointers at sample time.
 */
data class EcologySnapshot(
    val timestampMillis: Long,
    val telemetry: TelemetrySnapshot,
    val specimenStageOrdinal: Int,
    val branchLeadingOrdinal: Int,
    val sourceType: EcologySnapshotSourceType,
    val notes: String = "",
) {
    companion object {
        fun fromTelemetryAndState(
            telemetry: TelemetrySnapshot,
            state: SeedPodGrowthState,
            sourceType: EcologySnapshotSourceType,
            notes: String = "",
        ) = EcologySnapshot(
            timestampMillis = telemetry.timestampMillis,
            telemetry = telemetry,
            specimenStageOrdinal = state.structural.permanentStage.ordinal,
            branchLeadingOrdinal = state.structural.morphologyBranch.leadingBranchOrdinal,
            sourceType = sourceType,
            notes = notes,
        )
    }

    fun stageAtSample(): SeedPodGrowthStage =
        SeedPodGrowthStage.entries.getOrNull(specimenStageOrdinal) ?: SeedPodGrowthStage.DORMANT_POD
}
