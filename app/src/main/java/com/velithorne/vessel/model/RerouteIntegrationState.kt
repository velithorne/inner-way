package com.velithorne.vessel.model

/**
 * How strongly reroute conduits are embedded vs overlaid.
 */
data class RerouteIntegrationState(
    /** 0..1 — follows contour tangent, occluded under shell. */
    val embeddedStrength: Float,
    /** 0..1 — alpha through translucent surface. */
    val subsurfaceAlpha: Float,
)
