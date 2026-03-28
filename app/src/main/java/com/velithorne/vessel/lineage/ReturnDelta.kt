package com.velithorne.vessel.lineage

import com.velithorne.vessel.growth_seedpod.SeedPodGrowthStage

/**
 * Differences between persisted snapshot before away period and state after catch-up.
 */
data class ReturnDelta(
    val timeAwayMillis: Long,
    val stagesCrossed: List<Pair<SeedPodGrowthStage, SeedPodGrowthStage>>,
    val growthEventIds: List<Long>,
    val adaptationKindsTouched: Set<AdaptationKind>,
    val crownDelta: Float,
    val lateralDelta: Float,
    val reserveDelta: Float,
    val shellDelta: Float,
    val chamberingProxyDelta: Float,
    val summaryLines: List<String>,
)
