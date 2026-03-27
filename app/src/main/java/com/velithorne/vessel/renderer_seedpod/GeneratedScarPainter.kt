package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.velithorne.vessel.model.BiographyVisualState
import com.velithorne.vessel.model.VisibleMorphologyState
import kotlin.math.hypot
import kotlin.math.sin

/** Healed growth scars — scaled by [VisibleMorphologyState.generatedTopologyInfluence]. */
object GeneratedScarPainter {

    fun draw(
        scope: DrawScope,
        pod: Offset,
        radii: SeedPodContourBuilder.PodRadii,
        bio: BiographyVisualState,
        visible: VisibleMorphologyState,
        phaseSec: Float,
    ) {
        if (bio.scars.isEmpty()) return
        val g = visible.generatedTopologyInfluence.coerceIn(0f, 1f)
        val rx = radii.shellRx
        val ry = radii.shellRy
        for (sc in bio.scars) {
            val nx = (sc.nx - 0.5f) * 2f
            val ny = (sc.ny - 0.5f) * 2f
            val len = hypot(nx.toDouble(), ny.toDouble()).toFloat().coerceAtLeast(0.15f)
            val ux = nx / len
            val uy = ny / len
            val px = pod.x + ux * rx * 0.94f
            val py = pod.y + uy * ry * 0.94f
            val a = (sc.strength * 0.55f * (0.35f + g * 0.65f)).coerceIn(0.04f, 0.5f)
            val pulse = 0.85f + sin(phaseSec * 0.7f + sc.nx * 6.2f) * 0.15f
            scope.drawCircle(
                color = Color(0xFF9AACBC).copy(alpha = a * pulse),
                radius = (2f + sc.strength * 4f) * (0.7f + g * 0.35f),
                center = Offset(px, py),
                style = Stroke(1.1f + sc.strength * 1.4f),
            )
        }
    }
}
