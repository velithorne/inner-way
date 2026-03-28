package com.velithorne.vessel.renderer

import androidx.compose.ui.geometry.Offset

/**
 * Maps device tilt to parallax offsets in px. Heavy smoothing avoids motion sickness.
 * Renderer applies different multipliers per [VesselLayer] depth.
 */
class ParallaxController(
    private val tuning: RenderTuning = RenderTuning(),
) {
    private var sx = 0f
    private var sy = 0f

    fun step(
        pitchDeg: Float?,
        rollDeg: Float?,
        mobility: Float,
    ): Offset {
        val rx = (rollDeg ?: 0f) / 55f
        val ry = (pitchDeg ?: 0f) / 55f
        val tx = (-rx * tuning.parallaxMaxPx * (0.65f + mobility * 0.35f)).coerceIn(-tuning.parallaxMaxPx, tuning.parallaxMaxPx)
        val ty = (ry * tuning.parallaxMaxPx * 0.55f * (0.65f + mobility * 0.35f)).coerceIn(-tuning.parallaxMaxPx, tuning.parallaxMaxPx)
        val a = tuning.parallaxSmoothing.coerceIn(0.02f, 1f)
        sx += (tx - sx) * a
        sy += (ty - sy) * a
        return Offset(sx, sy)
    }

    fun reset() {
        sx = 0f
        sy = 0f
    }
}
