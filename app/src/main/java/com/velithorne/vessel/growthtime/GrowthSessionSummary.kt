package com.velithorne.vessel.growthtime

/**
 * Offline return summary.
 */
data class GrowthSessionSummary(
    val offlineSeconds: Long,
    val deltas: List<GrowthDelta>,
    val simulatedCatchUpMs: Long,
)
