package com.velithorne.vessel.branching

/**
 * Affinity accumulation, readiness, commitment — grouped constants.
 */
data class BranchingTuning(
    /** Per-second affinity drift toward targets (scaled by dt). */
    val affinityLerpPerSec: Float = 0.012f,
    /** Max single-step affinity change from one influence pass. */
    val affinityDeltaCap: Float = 0.04f,
    /** Branch readiness 0..1 — unlocks visible leaning after chamber mature + time. */
    val branchReadinessUnlockStageOrdinal: Int = com.velithorne.vessel.growth_seedpod.SeedPodGrowthStage.CHAMBER_MATURED.ordinal,
    /** Minimum structural stage ordinal before affinities affect visuals strongly. */
    val visualInfluenceStartOrdinal: Int =
        com.velithorne.vessel.growth_seedpod.SeedPodGrowthStage.LINEAGE_DIFFERENTIATING.ordinal,
    /** Stronger visuals after first branch forming. */
    val visualInfluenceStrongOrdinal: Int =
        com.velithorne.vessel.growth_seedpod.SeedPodGrowthStage.FIRST_BRANCH_FORMING.ordinal,
    /** Gap between first and second place to flip leading branch (hysteresis). */
    val leadSwitchMargin: Float = 0.06f,
    /** Resistance to changing committed lead (extra margin). */
    val commitmentResistance: Float = 0.12f,
    /** Stage ordinal at which soft commit (leading branch locked for display) tightens. */
    val softCommitMinStageOrdinal: Int =
        com.velithorne.vessel.growth_seedpod.SeedPodGrowthStage.FIRST_BRANCH_FORMING.ordinal,
    /** Device profile weight vs adaptation history (0..1). */
    val deviceVsAdaptationWeight: Float = 0.35f,
    /** Max visual influence strength 0..1 at full specialization. */
    val maxVisualInfluence: Float = 0.55f,
) {
    /** 0..1 strength of branch-driven silhouette/material tweaks at this structural stage. */
    fun visualInfluenceAt(stage: com.velithorne.vessel.growth_seedpod.SeedPodGrowthStage): Float {
        if (stage.ordinal < visualInfluenceStartOrdinal) return 0f
        val span = (visualInfluenceStrongOrdinal - visualInfluenceStartOrdinal).coerceAtLeast(1)
        val t = ((stage.ordinal - visualInfluenceStartOrdinal).toFloat() / span).coerceIn(0f, 1f)
        return (t * maxVisualInfluence).coerceIn(0f, maxVisualInfluence)
    }
}
