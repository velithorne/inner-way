package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.velithorne.vessel.model.BiographyVisualState
import com.velithorne.vessel.model.VisibleMorphologyState
import kotlin.math.cos
import kotlin.math.sin

/** Reroute seams — visible when coherence dropped historically. */
object GeneratedReroutePainter {

    fun draw(
        scope: DrawScope,
        pod: Offset,
        radii: SeedPodContourBuilder.PodRadii,
        bio: BiographyVisualState,
        visible: VisibleMorphologyState,
        phaseSec: Float,
    ) {
        val n = bio.rerouteCount
        if (n <= 0) return
        val g = visible.generatedTopologyInfluence.coerceIn(0f, 1f)
        if (g < 0.2f) return
        val rx = radii.shellRx
        val ry = radii.shellRy
        val alpha = (0.06f + minOf(n, 8) * 0.04f) * g
        for (i in 0 until minOf(n, 5)) {
            val ang = 0.7f + i * 0.95f + sin(phaseSec * 0.3f + i) * 0.08f
            val path = Path()
            val x0 = pod.x + rx * 0.55f * cos(ang)
            val y0 = pod.y + ry * 0.55f * sin(ang)
            val x1 = pod.x + rx * 0.92f * cos(ang + 0.15f)
            val y1 = pod.y + ry * 0.92f * sin(ang + 0.15f)
            path.moveTo(x0, y0)
            path.lineTo(x1, y1)
            scope.drawPath(
                path = path,
                color = Color(0xFF88A0C0).copy(alpha = alpha.coerceIn(0.04f, 0.35f)),
                style = Stroke(width = 1.2f + i * 0.15f),
            )
        }
    }
}
