package com.velithorne.vessel.progression

import com.velithorne.vessel.growth_seedpod.SeedPodGrowthStage

/**
 * Persisted irreversible development — never reduced on bad telemetry.
 */
data class StructuralGrowthState(
    /** Monotonic permanent stage (ordinal only increases in normal play). */
    val permanentStage: SeedPodGrowthStage,
    /** Bit flags for [DevelopmentMilestone]. */
    val milestoneFlags: Long,
    val stageEnteredAtMs: Long,
    /** 0..1 readiness toward the next stage. */
    val nextStageAccum: Float,
    /** High-water maturity snapshot for gating. */
    val confirmedMaturityHigh: Float,
    /** 0..1 lineage path lean from adaptation + physiology EMA. */
    val leanThermal: Float,
    val leanNeural: Float,
    val leanSignal: Float,
    val leanReserve: Float,
    /** Smoothed progress bar 0..1 (monotonic display). */
    val smoothedStructuralProgress: Float,
) {
    companion object {
        fun initial(nowMs: Long) = StructuralGrowthState(
            permanentStage = SeedPodGrowthStage.DORMANT_POD,
            milestoneFlags = 0L,
            stageEnteredAtMs = nowMs,
            nextStageAccum = 0f,
            confirmedMaturityHigh = 0f,
            leanThermal = 0.25f,
            leanNeural = 0.25f,
            leanSignal = 0.25f,
            leanReserve = 0.25f,
            smoothedStructuralProgress = 0f,
        )
    }
}
