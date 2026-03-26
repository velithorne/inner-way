package com.velithorne.vessel.growthtime

/**
 * Wall-clock growth rates, stage dwells, offline caps — single tuning surface.
 */
data class TimeTuning(
    val growthTickIntervalMs: Long = 250L,
    /** Max simulated wall time per offline catch-up step (ms). */
    val offlineCatchUpStepMs: Long = 60_000L,
    /** Cap total offline catch-up simulated time (ms) per resume (~6h). */
    val maxOfflineCatchUpMs: Long = 6 * 60 * 60 * 1000L,
    val returnRevealDurationMs: Long = 4_000L,
    /** Max fraction of target jump applied in one resume frame. */
    val maxOfflineCatchUpVisualJump: Float = 0.18f,
    val buildResetEnabled: Boolean = true,
    // Lerp speeds (per second toward target, approximate)
    val seedFormBlendSpeed: Float = 0.08f,
    val contourMulSpeed: Float = 0.06f,
    val seedCoreSpeed: Float = 0.12f,
    val chamberMassSpeed: Float = 0.05f,
    val growthVisualCueSpeed: Float = 0.07f,
    val buddingSpeed: Float = 0.06f,
    // Rate limits (max change per minute of display value)
    val maxCrownMulDeltaPerMinute: Float = 0.04f,
    val maxFrondVisualDeltaPerMinute: Float = 0.05f,
    val maxReservoirDeltaPerMinute: Float = 0.04f,
    val maxShellDeltaPerHour: Float = 0.08f,
    // Budget gains (per second at full pressure)
    val budgetGainCrown: Float = 0.012f,
    val budgetGainFrond: Float = 0.014f,
    val budgetGainReservoir: Float = 0.011f,
    val budgetGainShell: Float = 0.009f,
    val budgetGainArchive: Float = 0.01f,
    val budgetGainTendon: Float = 0.01f,
    val budgetGainRecovery: Float = 0.012f,
    val budgetDecayPerSecond: Float = 0.002f,
    /** Spend budget to accelerate lerp multipliers. */
    val budgetSpendToLerpBoost: Float = 0.15f,
    // Stage dwell (ms minimum before advancing)
    val dwellDormantMs: Long = 30_000L,
    val dwellActivatedMs: Long = 45_000L,
    val dwellGerminatingMs: Long = 60_000L,
    val dwellCrownMs: Long = 90_000L,
    val dwellLateralMs: Long = 90_000L,
    val dwellChamberMs: Long = 120_000L,
    val dwellShellMs: Long = 120_000L,
)
