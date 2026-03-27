package com.velithorne.vessel.model

import com.velithorne.vessel.growthtime.GrowthSessionSummary

/** Vessel tab UI state for the seed-pod renderer (no legacy morphogenesis labels). */
data class SeedPodVesselUiState(
    val stageLabel: String,
    val statusLine: String,
    val growthProgressFraction: Float,
    val activeBudgetChannelLabel: String,
    val recentAwayLine: String,
    val returnSummary: GrowthSessionSummary?,
)
