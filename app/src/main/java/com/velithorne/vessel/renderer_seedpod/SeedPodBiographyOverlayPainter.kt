package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.velithorne.vessel.model.BiographyVisualState
import kotlin.math.sin

/** Structural memory marks on the shell — from biography scar plates. */
object SeedPodBiographyOverlayPainter {

    fun draw(
        scope: DrawScope,
        pod: Offset,
        radii: SeedPodContourBuilder.PodRadii,
        bio: BiographyVisualState,
        phaseSec: Float,
    ) {
        if (bio.scars.isEmpty()) return
        val rx = radii.shellRx
        val ry = radii.shellRy
        for (sc in bio.scars) {
            val nx = (sc.nx - 0.5f) * 2f
            val ny = (sc.ny - 0.5f) * 2f
            val len = kotlin.math.hypot(nx.toDouble(), ny.toDouble()).toFloat().coerceAtLeast(0.15f)
            val ux = nx / len
            val uy = ny / len
            val px = pod.x + ux * rx * 0.94f
            val py = pod.y + uy * ry * 0.94f
            val a = (sc.strength * 0.5f).coerceIn(0.06f, 0.42f)
            val pulse = 0.85f + sin(phaseSec * 0.7f + sc.nx * 6.2f) * 0.15f
            scope.drawCircle(
                color = Color(0xFF9AACBC).copy(alpha = a * pulse),
                radius = 2f + sc.strength * 3.5f,
                center = Offset(px, py),
                style = Stroke(1.1f + sc.strength * 1.2f),
            )
        }
    }
}
