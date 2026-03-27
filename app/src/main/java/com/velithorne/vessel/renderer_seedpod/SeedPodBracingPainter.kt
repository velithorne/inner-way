package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.velithorne.vessel.model.BranchVisualState
import com.velithorne.vessel.model.VesselPaletteState
import kotlin.math.cos
import kotlin.math.sin

/**
 * Subtle tension arcs for [BranchVisualState.bracingLineAlpha] — motion-braced / stability morphology.
 */
object SeedPodBracingPainter {

    fun draw(
        scope: DrawScope,
        pod: Offset,
        radii: SeedPodContourBuilder.PodRadii,
        palette: VesselPaletteState,
        branch: BranchVisualState,
        phaseSec: Float,
    ) {
        val a = branch.bracingLineAlpha.coerceIn(0f, 0.5f)
        if (a < 0.02f) return
        val rx = radii.shellRx * 1.02f
        val ry = radii.shellRy * 1.02f
        val col = palette.shellRimCool.copy(alpha = a * 0.55f)
        val w = 1.1f + a * 0.8f
        val phase = phaseSec * 0.35f

        fun arc(side: Float, startDeg: Float, sweep: Float) {
            val path = Path()
            val steps = 24
            for (i in 0..steps) {
                val t = i / steps.toFloat()
                val ang = Math.toRadians((startDeg + sweep * t).toDouble())
                val x = pod.x + side * rx * cos(ang).toFloat()
                val y = pod.y + ry * sin(ang).toFloat() * 0.92f
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            scope.drawPath(path, color = col, style = Stroke(width = w))
        }

        arc(-1f, 195f + sin(phase) * 4f, 55f)
        arc(1f, -10f + sin(phase + 1.2f) * 4f, 55f)
    }
}
