package com.velithorne.vessel.progression

import com.velithorne.vessel.growth_seedpod.SeedPodGrowthState

/** Combined structural + live growth snapshot for systems that need both. */
data class DevelopmentState(
    val growth: SeedPodGrowthState,
    val liveExpression: LiveExpressionState,
)
