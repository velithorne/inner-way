package com.velithorne.vessel.renderer_seedpod

/**
 * How much legacy pod scaffold vs generated layers participate this frame.
 */
enum class SeedPodFallbackMode {
    /** Seed era — pod silhouette is the main read. */
    SEED_DOMINANT,
    /** Veil — generated emerging, pod still recognizable. */
    BLEND_EMERGING,
    /** Core establishment onward — generated contour and chambers lead. */
    GENERATED_PRIMARY,
    /** Branching threshold+ — topology clearly dominates; seed is trace only. */
    GENERATED_OVERRIDE,
}
