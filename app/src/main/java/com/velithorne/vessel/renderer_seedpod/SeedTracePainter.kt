package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.velithorne.vessel.model.SeedTraceState
import com.velithorne.vessel.model.VesselPaletteState
import kotlin.math.cos
import kotlin.math.sin

/** Original seed kernel + seam — subordinate when topology influence is high. */
object SeedTracePainter {

    fun draw(
        scope: DrawScope,
        pod: Offset,
        minDim: Float,
        radii: SeedPodContourBuilder.PodRadii,
        palette: VesselPaletteState,
        trace: SeedTraceState,
    ) {
        val a = trace.traceAlpha.coerceIn(0f, 1f)
        if (a < 0.04f) return
        val ox = trace.traceOffsetNx * minDim * 0.08f
        val oy = trace.traceOffsetNy * minDim * 0.08f + trace.burialDepthPx
        val c = pod + Offset(ox, oy)
        val r = minDim * 0.022f * trace.coreKnotScaleMul
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.shellBase.copy(alpha = 0.18f * a),
                    palette.neuralPathway.copy(alpha = 0.06f * a),
                    Color(0xFF000000).copy(alpha = 0f),
                ),
                center = c,
                radius = r * 3f,
            ),
            radius = r,
            center = c,
        )
        val rx = radii.shellRx * 0.38f * trace.coreKnotScaleMul
        val ry = radii.shellRy * 0.38f * trace.coreKnotScaleMul
        val ca = cos(trace.seamAngleRad)
        val sa = sin(trace.seamAngleRad)
        scope.drawLine(
            color = palette.innerChamberShadow.copy(alpha = 0.18f * a),
            start = Offset(c.x - rx * ca, c.y - ry * sa),
            end = Offset(c.x + rx * ca, c.y + ry * sa),
            strokeWidth = 1.2f,
        )
    }
}
