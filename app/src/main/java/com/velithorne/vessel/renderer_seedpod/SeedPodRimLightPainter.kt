package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.velithorne.vessel.model.SeedPodLightingState
import com.velithorne.vessel.model.VesselPaletteState

/** Front rim light + shell catchlight arc. */
object SeedPodRimLightPainter {

    fun draw(
        scope: DrawScope,
        pod: Offset,
        rx: Float,
        ry: Float,
        layerOffset: Offset,
        lighting: SeedPodLightingState,
        palette: VesselPaletteState,
        phaseSec: Float,
    ) {
        val c = pod + layerOffset
        val rim = lighting.rimLight.coerceIn(0f, 1f)
        val catchL = lighting.shellCatchlight.coerceIn(0f, 1f)
        // Upper-left rim arc
        scope.drawArc(
            color = palette.shellRimCool.copy(alpha = 0.12f + rim * 0.35f),
            startAngle = 135f,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = Offset(c.x - rx * 1.05f, c.y - ry * 1.05f),
            size = Size(rx * 2.1f, ry * 2.1f),
            style = Stroke(2.2f + rim * 1.5f),
        )
        // Catchlight blob
        val sh = kotlin.math.sin(phaseSec * 1.4f).toFloat() * 0.5f + 0.5f
        scope.drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.06f + catchL * 0.14f * sh),
                    Color(0xFF000000).copy(alpha = 0f),
                ),
                center = Offset(c.x - rx * 0.55f, c.y - ry * 0.5f),
                radius = rx * 0.45f,
            ),
            topLeft = Offset(c.x - rx * 0.75f, c.y - ry * 0.72f),
            size = Size(rx * 0.9f, ry * 0.55f),
        )
    }
}
