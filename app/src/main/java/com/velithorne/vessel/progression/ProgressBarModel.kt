package com.velithorne.vessel.progression

import com.velithorne.vessel.growth_seedpod.SeedPodExplainer
import com.velithorne.vessel.growth_seedpod.SeedPodGrowthStage

/**
 * Semantics for the developmental progress UI — structural only, smoothed.
 */
data class ProgressBarModel(
    /** Monotonic 0..1 permanent advancement. */
    val structuralProgress: Float,
    val currentStageLabel: String,
    val nextStageLabel: String,
    /** Subtitle for the card. */
    val progressCaption: String,
    /** Optional 0..1 live strain for a thin secondary indicator (not structural). */
    val liveStrainIndicator: Float,
)

object ProgressBarModelFactory {
    fun build(
        stage: SeedPodGrowthStage,
        smoothedProgress: Float,
        nextAccum: Float,
        strain: Float,
    ): ProgressBarModel {
        val next = when (stage) {
            SeedPodGrowthStage.DORMANT_POD -> SeedPodGrowthStage.ACTIVATING_POD
            SeedPodGrowthStage.ACTIVATING_POD -> SeedPodGrowthStage.GERMINATING_POD
            SeedPodGrowthStage.GERMINATING_POD -> SeedPodGrowthStage.EARLY_BUDDING
            SeedPodGrowthStage.EARLY_BUDDING -> SeedPodGrowthStage.EARLY_CHAMBERING
            SeedPodGrowthStage.EARLY_CHAMBERING -> SeedPodGrowthStage.CHAMBER_MATURED
            SeedPodGrowthStage.CHAMBER_MATURED -> SeedPodGrowthStage.LINEAGE_DIFFERENTIATING
            SeedPodGrowthStage.LINEAGE_DIFFERENTIATING -> SeedPodGrowthStage.FIRST_BRANCH_FORMING
            SeedPodGrowthStage.FIRST_BRANCH_FORMING -> SeedPodGrowthStage.ADAPTIVE_SHELL_VARIANT
            SeedPodGrowthStage.ADAPTIVE_SHELL_VARIANT -> SeedPodGrowthStage.SPECIALIZATION_READY
            SeedPodGrowthStage.SPECIALIZATION_READY -> SeedPodGrowthStage.SPECIALIZATION_READY
        }
        val nextLabel = SeedPodExplainer.stageLabel(next)
        val cap = "Permanent growth toward $nextLabel · readiness ${(nextAccum * 100f).toInt()}%"
        return ProgressBarModel(
            structuralProgress = smoothedProgress,
            currentStageLabel = SeedPodExplainer.stageLabel(stage),
            nextStageLabel = nextLabel,
            progressCaption = cap,
            liveStrainIndicator = strain.coerceIn(0f, 1f),
        )
    }
}
