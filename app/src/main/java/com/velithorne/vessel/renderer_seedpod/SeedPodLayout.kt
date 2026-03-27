package com.velithorne.vessel.renderer_seedpod

/**
 * Fixed chamber anchor for the seed pod (normalized 0..1, origin top-left).
 * **Not** derived from legacy contour or graph centroid.
 */
object SeedPodLayout {
    const val anchorXNormalized: Float = 0.5f
    /** Upper-mid suspension — reads higher in the chamber viewport. */
    const val anchorYNormalized: Float = 0.38f
}
