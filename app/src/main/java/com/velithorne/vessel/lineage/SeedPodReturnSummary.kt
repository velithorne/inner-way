package com.velithorne.vessel.lineage

/** "While you were away" for seed pod — lines derived from persisted deltas only. */
data class SeedPodReturnSummary(
    val awaySeconds: Long,
    val lines: List<String>,
)
