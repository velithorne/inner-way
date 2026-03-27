package com.velithorne.vessel.progression

/**
 * Grouped thresholds — no magic numbers in engines.
 */
data class ProgressionTuning(
    /** Minimum ms in a stage before another advance can complete (dwell floor). */
    /** Index = [SeedPodGrowthStage.ordinal] of the stage you must dwell in before advancing. */
    val minDwellMsByStageOrdinal: LongArray = longArrayOf(
        0L, // DORMANT_POD
        45_000L, // ACTIVATING_POD
        60_000L, // GERMINATING_POD
        90_000L, // EARLY_BUDDING
        120_000L, // EARLY_CHAMBERING
        180_000L, // CHAMBER_MATURED
        240_000L, // LINEAGE_DIFFERENTIATING
        300_000L, // FIRST_BRANCH_FORMING
        360_000L, // BRANCH_STABILIZING
        400_000L, // SPECIALIZATION_EMERGING
        420_000L, // SPECIALIZATION_ESTABLISHED
    ),
    /** Accumulation 0..1 per second toward next stage under good conditions. */
    val baseAccumPerSec: Float = 0.00012f,
    /** Multiplier when stress + fever are high — slows advance. */
    val strainSlowdownMax: Float = 0.55f,
    /** Below this strain product, no slowdown. */
    val strainSlowdownStart: Float = 0.35f,
    /** Hysteresis: must exceed threshold + this to advance (reduces flip-flop). */
    val stageAdvanceHysteresis: Float = 0.04f,
    /** Maturity score needed to confirm CHAMBER_MATURED from EARLY_CHAMBERING. */
    val chamberMaturedMaturity: Float = 0.76f,
    /** Readiness for post-mature stages scales with nextStageAccum crossing. */
    val lineageDiffAccum: Float = 0.85f,
    val firstBranchAccum: Float = 0.88f,
    val branchStabilizingAccum: Float = 0.9f,
    val specializationEmergingAccum: Float = 0.91f,
    val specializationEstablishedAccum: Float = 0.93f,
    /** EMA for lineage tendency from physiology. */
    val tendencyLerp: Float = 0.02f,
    /** Progress bar smoothing toward target (higher = smoother, slower). */
    val progressBarSmoothing: Float = 0.08f,
)
