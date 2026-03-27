package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.velithorne.vessel.model.SeedPodVisualState
import com.velithorne.vessel.model.VesselPaletteState
import kotlin.math.cos
import kotlin.math.sin

/** Translucent wafer shell, membrane rings, conductive seams, mineral fracture hints. */
object SeedPodShellPainter {

    fun draw(
        scope: DrawScope,
        pod: Offset,
        radii: SeedPodContourBuilder.PodRadii,
        palette: VesselPaletteState,
        seedPalette: SeedPodPalette,
        appearance: SeedPodVisualState,
        shellThickening: Float,
        phaseSec: Float,
        tuning: SeedPodTuning,
    ) {
        val st = shellThickening.coerceIn(0f, 1f)
        val op = appearance.shellOpacityMul.coerceIn(0.3f, 1.3f)
        val edge = appearance.shellEdgeBright.coerceIn(0.3f, 1.3f)
        val closed = appearance.shellClosedness.coerceIn(0f, 1f)

        // Outer soft halo (suspension)
        scope.drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    seedPalette.siliconVeil.copy(alpha = 0.12f * op),
                    Color(0xFF000000).copy(alpha = 0f),
                ),
                center = pod,
                radius = maxOf(radii.bandOuterRx, radii.bandOuterRy) * 1.15f,
            ),
            topLeft = Offset(pod.x - radii.bandOuterRx * 1.2f, pod.y - radii.bandOuterRy * 1.2f),
            size = Size(radii.bandOuterRx * 2.4f, radii.bandOuterRy * 2.4f),
        )

        // Main membrane body
        scope.drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.shellBase.copy(alpha = (0.14f + st * 0.1f) * op),
                    palette.innerChamberShadow.copy(alpha = (0.18f + st * 0.08f) * op),
                    seedPalette.gelEnvelope.copy(alpha = 0.08f * op),
                    Color(0xFF000000).copy(alpha = 0f),
                ),
                center = pod,
                radius = maxOf(radii.shellRx, radii.shellRy),
            ),
            topLeft = Offset(pod.x - radii.shellRx * 1.15f, pod.y - radii.shellRy * 1.15f),
            size = Size(radii.shellRx * 2.3f, radii.shellRy * 2.3f),
        )

        // Mid shell band (wafer ring)
        scope.drawOval(
            color = seedPalette.shellBandOuter.copy(alpha = (0.14f + edge * 0.12f) * op),
            topLeft = Offset(pod.x - radii.bandMidRx, pod.y - radii.bandMidRy),
            size = Size(radii.bandMidRx * 2f, radii.bandMidRy * 2f),
            style = Stroke(width = 1.4f + st * 2.2f),
        )

        // Inner rim
        scope.drawOval(
            color = palette.shellEdge.copy(alpha = (0.18f + st * 0.18f + edge * 0.1f) * op),
            topLeft = Offset(pod.x - radii.innerChamberRx * 1.05f, pod.y - radii.innerChamberRy * 1.05f),
            size = Size(radii.innerChamberRx * 2.1f, radii.innerChamberRy * 2.1f),
            style = Stroke(width = 1.2f + closed * 0.8f),
        )

        // Conductive seams (radial)
        val n = tuning.conductiveSeamCount
        val shimmer = (sin(phaseSec * 1.8f).toFloat() * 0.5f + 0.5f)
        for (i in 0 until n) {
            val ang = (i / n.toFloat()) * kotlin.math.PI.toFloat() * 2f
            val c = cos(ang)
            val s = sin(ang)
            val r1 = maxOf(radii.innerChamberRx, radii.innerChamberRy) * 0.35f
            val r2 = maxOf(radii.shellRx, radii.shellRy) * (0.92f + shimmer * 0.04f)
            val a = seedPalette.conductiveSeam.copy(alpha = (0.06f + edge * 0.1f) * op * (0.7f + shimmer * 0.3f))
            scope.drawLine(
                color = a,
                start = Offset(pod.x + c * r1, pod.y + s * r1),
                end = Offset(pod.x + c * r2, pod.y + s * r2),
                strokeWidth = 0.8f,
            )
        }

        // Mineral fracture arcs (short tangents)
        val frac = appearance.facetLineAlpha * 0.6f
        if (frac > 0.04f) {
            for (k in 0..3) {
                val a0 = (k / 4f) * kotlin.math.PI.toFloat() + phaseSec * 0.15f
                val arcLen = 0.45f + k * 0.08f
                val cx = pod.x + cos(a0) * radii.shellRx * 0.88f
                val cy = pod.y + sin(a0) * radii.shellRy * 0.88f
                scope.drawCircle(
                    color = seedPalette.mineralFracture.copy(alpha = frac * 0.35f),
                    radius = 2f + k * 0.5f,
                    center = Offset(cx, cy),
                    style = Stroke(0.9f),
                )
            }
        }
    }
}
