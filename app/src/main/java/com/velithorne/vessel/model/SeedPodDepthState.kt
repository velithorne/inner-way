package com.velithorne.vessel.model

import androidx.compose.ui.geometry.Offset

/**
 * Per-frame pseudo-depth scalars for 2.5D seed pod rendering.
 */
data class SeedPodDepthState(
    /** Nucleus center offset from pod anchor (positive Y = visually deeper / recessed). */
    val nucleusRecessOffset: Offset,
    /** Visual shell thickness scale 0..1 (affects rim bands). */
    val shellThicknessVisual: Float,
    /** Rear shell / atmosphere darkening 0..1. */
    val rearDarkening: Float,
    /** Inner chamber volume expansion vs dormant. */
    val innerVolumeExpand: Float,
    /** Bud embedding depth (parallax scale for buds). */
    val budDepthMul: Float,
    /** Stage multiplier for overall depth separation. */
    val stageDepthSeparation: Float,
    /** Core scale vs shell (buried nucleus reads smaller behind front shell). */
    val nucleusBurialScale: Float,
)
