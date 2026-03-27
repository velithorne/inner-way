package com.velithorne.vessel.model

import com.velithorne.vessel.growthtime.GrowthSessionSummary
import com.velithorne.vessel.lineage.SeedPodReturnSummary

/** Vessel tab UI state for the seed-pod renderer (no legacy morphogenesis labels). */
data class SeedPodVesselUiState(
    val stageLabel: String,
    val statusLine: String,
    val growthProgressFraction: Float,
    val activeBudgetChannelLabel: String,
    val recentAwayLine: String,
    /** Morphogenesis temporal return (legacy morph pipeline). */
    val returnSummary: GrowthSessionSummary?,
    /** Saved lineage / offline catch-up for seed pod. */
    val seedPodReturnSummary: SeedPodReturnSummary?,
)
