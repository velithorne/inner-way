package com.velithorne.vessel.renderer_genesis

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.velithorne.vessel.model.BiographyVisualState
import com.velithorne.vessel.model.VesselPaletteState
import kotlin.math.cos
import kotlin.math.sin

object GenesisScarPainter {
    fun draw(
        scope: DrawScope,
        pod: Offset,
        minDim: Float,
        bio: BiographyVisualState,
        palette: VesselPaletteState,
        phaseSec: Float,
    ) {
        if (bio.scars.isEmpty()) return
        for ((i, s) in bio.scars.withIndex()) {
            val ang = i * 0.9f + phaseSec * 0.15f
            val len = minDim * 0.06f * s.strength.coerceIn(0.2f, 1f)
            val x0 = pod.x + (s.nx - 0.5f) * minDim * 0.45f
            val y0 = pod.y + (s.ny - 0.5f) * minDim * 0.45f
            val x1 = x0 + cos(ang) * len
            val y1 = y0 + sin(ang) * len
            scope.drawLine(
                color = palette.accentSignal.copy(alpha = 0.18f),
                start = Offset(x0, y0),
                end = Offset(x1, y1),
                strokeWidth = 1.2f,
            )
        }
    }
}
