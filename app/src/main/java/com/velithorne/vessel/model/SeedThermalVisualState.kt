package com.velithorne.vessel.model

import androidx.compose.ui.graphics.Color

/**
 * Thermal readout for edge shimmer and warm hotspots (fever / stress).
 */
data class SeedThermalVisualState(
    val edgeShimmer: Float,
    val hotspotAlpha: Float,
    val warmTint: Color,
    val coolRim: Color,
)
