package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.velithorne.vessel.model.VesselPaletteState
import kotlin.math.sin

/** Local growth emerging from the pod only — no legacy pathways. */
object SeedPodGrowthPainter {

    fun draw(
        scope: DrawScope,
        podCenter: Offset,
        minDim: Float,
        d: com.velithorne.vessel.growth_seedpod.SeedPodDisplayState,
        palette: VesselPaletteState,
        phaseSec: Float,
    ) {
        val w = minDim
        // Crown nub — just above pod
        val cn = d.crownNub.coerceIn(0f, 1f)
        if (cn > 0.04f) {
            val c = Offset(podCenter.x, podCenter.y - w * (0.09f + cn * 0.04f))
            scope.drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.cortexNode.copy(alpha = cn * 0.2f),
                        Color(0xFF000000).copy(alpha = 0f),
                    ),
                    center = c,
                    radius = w * 0.06f * cn,
                ),
                radius = w * 0.055f * cn,
                center = c,
            )
        }
        // Lateral buds
        val lat = (d.lateralBudLeft + d.lateralBudRight) * 0.5f
        if (lat > 0.04f) {
            for (side in listOf(-1f, 1f)) {
                val b = Offset(podCenter.x + side * w * 0.05f, podCenter.y + w * 0.02f)
                scope.drawCircle(
                    color = palette.accentSignal.copy(alpha = lat * 0.18f),
                    radius = w * 0.028f * lat,
                    center = b,
                )
            }
        }
        // Reserve droplet below
        val rb = d.reserveBulb.coerceIn(0f, 1f)
        if (rb > 0.05f) {
            val b = Offset(podCenter.x, podCenter.y + w * 0.1f)
            scope.drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.archiveDeep.copy(alpha = rb * 0.22f),
                        Color(0xFF000000).copy(alpha = 0f),
                    ),
                    center = b,
                    radius = w * 0.07f * rb,
                ),
                radius = w * 0.065f * rb,
                center = b,
            )
        }
        // Tissue haze envelope
        val hz = d.tissueHaze.coerceIn(0f, 1f)
        if (hz > 0.04f) {
            scope.drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.shellBase.copy(alpha = hz * 0.08f),
                        Color(0xFF000000).copy(alpha = 0f),
                    ),
                    center = podCenter,
                    radius = w * (0.14f + hz * 0.1f),
                ),
                radius = w * (0.16f + hz * 0.08f),
                center = podCenter,
            )
        }
        // Growth front shimmer ring
        val shim = sin(phaseSec * 2.2f) * 0.5f + 0.5f
        val th = d.shellThickening.coerceIn(0f, 1f)
        if (th > 0.08f || hz > 0.1f) {
            scope.drawCircle(
                color = palette.accentSignal.copy(alpha = (0.06f + th * 0.1f) * shim),
                radius = w * (0.12f + th * 0.04f),
                center = podCenter,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f + th * 2f),
            )
        }
        // Thermal veil — perimeter
        val tv = d.thermalVeil.coerceIn(0f, 1f)
        if (tv > 0.06f) {
            scope.drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.thermalHot.copy(alpha = 0f),
                        palette.thermalEdge.copy(alpha = tv * 0.14f),
                        Color(0xFF000000).copy(alpha = 0f),
                    ),
                    center = podCenter,
                    radius = w * 0.2f,
                ),
                radius = w * 0.19f,
                center = podCenter,
            )
        }
    }
}
