package com.velithorne.vessel.model

/**
 * Renderer-facing juvenile body parameters.
 */
data class JuvenileVisualState(
    val crownElevationMul: Float,
    val lateralSpanMul: Float,
    val reserveDropMul: Float,
    val shellForwardMul: Float,
    val supportCrossMul: Float,
    val bodySilhouetteStretchX: Float,
    val bodySilhouetteStretchY: Float,
    val chamberOrganBias: Float,
    val active: Boolean,
) {
    companion object {
        fun inactive() = JuvenileVisualState(
            crownElevationMul = 1f,
            lateralSpanMul = 1f,
            reserveDropMul = 1f,
            shellForwardMul = 1f,
            supportCrossMul = 1f,
            bodySilhouetteStretchX = 1f,
            bodySilhouetteStretchY = 1f,
            chamberOrganBias = 0f,
            active = false,
        )
    }
}
