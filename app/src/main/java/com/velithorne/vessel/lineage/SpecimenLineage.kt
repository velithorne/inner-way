package com.velithorne.vessel.lineage

/**
 * Aggregate view for lineage UI: identity + current stage label + last major event text.
 */
data class SpecimenLineage(
    val identity: SpecimenIdentity,
    val age: SpecimenAge,
    val currentStageLabel: String,
    val nextStageTargetLabel: String,
    val timeInCurrentStageFormatted: String,
    val lineageTendencyLine: String,
    val unlockedMilestonesSummary: String,
    val lastMajorChangeLabel: String,
    val lastStageTransitionMillis: Long?,
    val lastGrowthEventMillis: Long?,
    /** Lineage morphology branching — persisted affinities / lead. */
    val branchReadinessPercent: Int,
    val leadingBranchLabel: String,
    val branchSummaryLine: String,
    val branchReasonLine: String,
)
