package com.velithorne.vessel.lineage

/**
 * Aggregate view for lineage UI: identity + current stage label + last major event text.
 */
data class SpecimenLineage(
    val identity: SpecimenIdentity,
    val age: SpecimenAge,
    val currentStageLabel: String,
    val lastMajorChangeLabel: String,
    val lastStageTransitionMillis: Long?,
    val lastGrowthEventMillis: Long?,
)
