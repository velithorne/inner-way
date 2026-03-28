package com.velithorne.vessel.renderer

/**
 * Fixed design anchor for the seed specimen — **not** derived from contour centroid or graph mass.
 * Normalized to chamber viewport (0..1), origin top-left.
 */
data class SeedPlacement(
    /** Horizontal center of the suspended seed (0.5 = centered). */
    val anchorXNormalized: Float,
    /**
     * Vertical position of seed nucleus — **lower value = higher on screen**.
     * 0.40–0.45 reads as upper-mid suspension with room below for growth.
     */
    val anchorYNormalized: Float,
    val anchorBiasMode: SeedAnchorBiasMode,
    /**
     * Scales specimen width/height around the anchor (1 = layout default bodyWidth/Height).
     * Early stages use slightly smaller viewport scale so the seed feels focused, not filling the old body box.
     */
    val seedViewportScale: Float,
) {
    enum class SeedAnchorBiasMode {
        /** Explicit fixed anchor — default for seed-first phases. */
        FIXED_UPPER_MID,
        /** Legacy: blend toward contour/graph (not used for early modes). */
        BLEND_CONTOUR,
    }

    companion object {
        /** Default suspended seed — centered, upper-mid chamber. */
        val Default = SeedPlacement(
            anchorXNormalized = 0.5f,
            anchorYNormalized = 0.415f,
            anchorBiasMode = SeedAnchorBiasMode.FIXED_UPPER_MID,
            seedViewportScale = 0.88f,
        )
    }
}
