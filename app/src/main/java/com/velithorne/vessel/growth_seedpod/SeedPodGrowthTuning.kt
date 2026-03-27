package com.velithorne.vessel.growth_seedpod

/**
 * Visual / local growth rates for [SeedPodGrowthEngine] — separate from structural [ProgressionTuning].
 */
data class SeedPodGrowthTuning(
    val budgetGainMultiplier: Float = 1f,
    val budgetDecayMultiplier: Float = 1f,
    /** Scales display lerp toward target (higher = faster visible bud/chamber motion). */
    val displayLerpMultiplier: Float = 1f,
    /** Scales maturity contribution from display (dev can bump slightly for faster stage gates). */
    val maturityScoreMultiplier: Float = 1f,
) {
    companion object {
        fun default() = SeedPodGrowthTuning()
    }
}
