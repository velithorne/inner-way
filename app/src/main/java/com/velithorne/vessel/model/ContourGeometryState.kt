package com.velithorne.vessel.model

/**
 * Runtime contour resolution for debug / validation.
 */
data class ContourGeometryState(
    val sampleCount: Int,
    val smoothingPasses: Int,
    val relaxationIterations: Int,
    val splineEnabled: Boolean,
)
