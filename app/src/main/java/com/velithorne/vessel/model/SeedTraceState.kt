package com.velithorne.vessel.model

/**
 * Original seed kernel / seam — visible but subordinate once [VisibleMorphologyState.generatedTopologyInfluence] rises.
 */
data class SeedTraceState(
    /** 0..1 visibility of embedded core knot + seam. */
    val traceAlpha: Float,
    /** Radians — slight tilt of latent seam. */
    val seamAngleRad: Float,
    /** Scale of trace vs current body. */
    val coreKnotScaleMul: Float,
    /** Normalized offset of trace center from pod anchor. */
    val traceOffsetNx: Float,
    val traceOffsetNy: Float,
) {
    companion object {
        fun default() = SeedTraceState(
            traceAlpha = 0.55f,
            seamAngleRad = 0f,
            coreKnotScaleMul = 0.62f,
            traceOffsetNx = 0f,
            traceOffsetNy = 0f,
        )
    }
}
