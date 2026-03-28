package com.velithorne.vessel.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Latent inner seed geometry — reads as pre-unfolding crystalline structure, not the legacy body.
 */
object GerminationPainter {

    fun draw(
        scope: DrawScope,
        center: Offset,
        specimenWidth: Float,
        specimenHeight: Float,
        scene: VesselSceneState,
        palette: com.velithorne.vessel.model.VesselPaletteState,
        pulse: Float,
        visibility: Float,
    ) {
        val v = visibility.coerceIn(0f, 1f)
        if (v < 0.02f) return
        val sv = scene.seedVisual
        val stress = sv.latticeStress.coerceIn(0f, 1f)
        val r = specimenWidth * 0.11f * (0.9f + sv.germinationProgress * 0.12f)
        val wobble = sin(pulse * 1.2f) * (0.012f + stress * 0.02f) * specimenWidth

        // Hex lattice hint (simplified as rotated ellipses)
        for (i in 0 until 6) {
            val ang = (i / 6f) * PI.toFloat() * 2f
            val ox = cos(ang) * r * 0.35f
            val oy = sin(ang) * r * 0.35f
            scope.drawCircle(
                color = palette.shellRimCool.copy(alpha = 0.06f * v * (1f - stress * 0.4f)),
                radius = r * 0.08f,
                center = Offset(center.x + ox * 0.9f + wobble * 0.3f, center.y + oy * 0.9f),
            )
        }
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.cortexNode.copy(alpha = 0.1f * v),
                    Color(0xFF000000).copy(alpha = 0f),
                ),
                center = center,
                radius = r * 0.9f,
            ),
            radius = r * 0.55f,
            center = center,
        )
    }
}
