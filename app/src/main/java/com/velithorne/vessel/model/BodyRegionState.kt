package com.velithorne.vessel.model

import com.velithorne.vessel.juvenile_form.JuvenileRegion

/**
 * Per-region mass and depth for juvenile architecture.
 */
data class BodyRegionState(
    val masses: Map<JuvenileRegion, Float>,
    val crownDepth: Float,
    val coreDepth: Float,
    val lateralDepth: Float,
    val reserveDepth: Float,
)
