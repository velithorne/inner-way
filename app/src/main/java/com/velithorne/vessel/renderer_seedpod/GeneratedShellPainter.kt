package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.velithorne.vessel.model.VisibleMorphologyState
import com.velithorne.vessel.model.VesselPaletteState
/** Regional shell plating — not uniform thickness. */
object GeneratedShellPainter {

    fun draw(
        scope: DrawScope,
        pod: Offset,
        radii: SeedPodContourBuilder.PodRadii,
        palette: VesselPaletteState,
        visible: VisibleMorphologyState,
        phaseSec: Float,
    ) {
        val g = visible.generatedTopologyInfluence.coerceIn(0f, 1f)
        if (g < 0.15f) return
        val rx = radii.shellRx
        val ry = radii.shellRy
        val c = pod
        fun band(arcStart: Float, sweep: Float, thick: Float, alpha: Float) {
            scope.drawArc(
                color = palette.shellBase.copy(alpha = alpha * g),
                startAngle = arcStart,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(c.x - rx * 1.05f, c.y - ry * 1.05f),
                style = Stroke(width = thick),
                size = Size(rx * 2.1f, ry * 2.1f),
            )
        }
        val u = visible.shellUpperPlate
        val low = visible.shellLowerPlate
        val le = visible.shellLeftWing
        val ri = visible.shellRightWing
        band(-140f, 80f, 2f + u * 4f, 0.08f + u * 0.12f)
        band(40f, 100f, 2f + low * 4f, 0.08f + low * 0.11f)
        band(120f, 70f, 2f + le * 3.5f, 0.06f + le * 0.1f)
        band(-60f, 70f, 2f + ri * 3.5f, 0.06f + ri * 0.1f)
    }
}
