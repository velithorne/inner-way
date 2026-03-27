package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.velithorne.vessel.model.SeedPodLightingState
import com.velithorne.vessel.model.SeedThermalVisualState
import com.velithorne.vessel.model.VesselPaletteState
import kotlin.math.cos
import kotlin.math.sin

/** Thermal edge shimmer and warm hotspots — local to pod perimeter. */
object SeedPodThermalPainter {

    fun draw(
        scope: DrawScope,
        pod: Offset,
        layerOffset: Offset,
        radii: SeedPodContourBuilder.PodRadii,
        thermal: SeedThermalVisualState,
        lighting: SeedPodLightingState,
        palette: VesselPaletteState,
        phaseSec: Float,
    ) {
        val c = pod + layerOffset
        val edge = thermal.edgeShimmer.coerceIn(0f, 1f)
        if (edge < 0.04f && thermal.hotspotAlpha < 0.06f) return

        val rx = radii.shellRx * 1.02f
        val ry = radii.shellRy * 1.02f
        val shim = sin(phaseSec * 2.4f).toFloat() * 0.5f + 0.5f

        scope.drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    thermal.warmTint.copy(alpha = 0f),
                    palette.thermalEdge.copy(alpha = edge * 0.22f * (0.6f + shim * 0.4f) * (0.85f + lighting.thermalHotspot * 0.15f)),
                    thermal.coolRim.copy(alpha = edge * 0.12f),
                    Color(0xFF000000).copy(alpha = 0f),
                ),
                center = c,
                radius = maxOf(rx, ry) * 1.25f,
            ),
            topLeft = Offset(c.x - rx * 1.2f, c.y - ry * 1.2f),
            size = androidx.compose.ui.geometry.Size(rx * 2.4f, ry * 2.4f),
        )

        if (thermal.hotspotAlpha > 0.05f) {
            val hx = c.x + rx * 0.55f * cos(phaseSec * 0.7f)
            val hy = c.y + ry * 0.5f * sin(phaseSec * 0.55f)
            scope.drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.thermalHot.copy(alpha = thermal.hotspotAlpha * 0.5f),
                        Color(0xFF000000).copy(alpha = 0f),
                    ),
                    center = Offset(hx, hy),
                    radius = maxOf(rx, ry) * 0.22f,
                ),
                radius = maxOf(rx, ry) * 0.18f,
                center = Offset(hx, hy),
            )
        }

        // Thin outer veil stroke
        if (edge > 0.08f) {
            scope.drawOval(
                color = palette.thermalEdge.copy(alpha = edge * 0.18f),
                topLeft = Offset(c.x - rx, c.y - ry),
                size = androidx.compose.ui.geometry.Size(rx * 2f, ry * 2f),
                style = Stroke(1.2f + edge * 0.8f),
            )
        }
    }
}
