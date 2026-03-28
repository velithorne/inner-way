package com.velithorne.vessel.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/** Layered soft bloom without blur shader — cheap stacked fills. */
object GlowSystem {

    fun radialBloom(
        scope: DrawScope,
        center: Offset,
        radius: Float,
        core: Color,
        halo: Color,
        layers: Int = 4,
        intensity: Float = 1f,
    ) {
        val i = intensity.coerceIn(0f, 2f)
        repeat(layers) { k ->
            val t = (k + 1) / layers.toFloat()
            val r = radius * (1f + t * 1.35f)
            val alpha = (0.14f * (1f - t * 0.7f) * i).coerceIn(0.008f, 0.35f)
            scope.drawCircle(
                color = halo.copy(alpha = alpha),
                radius = r,
                center = center,
            )
        }
        scope.drawCircle(
            color = core.copy(alpha = (0.25f * i).coerceIn(0.05f, 0.55f)),
            radius = radius * 0.55f,
            center = center,
        )
    }

    fun ringPulse(
        scope: DrawScope,
        center: Offset,
        radius: Float,
        color: Color,
        phase: Float,
        width: Float = 3f,
    ) {
        val breathe = 1f + kotlin.math.sin(phase * Math.PI.toFloat() * 2f) * 0.06f
        scope.drawCircle(
            color = color.copy(alpha = 0.35f),
            radius = radius * breathe,
            center = center,
            style = Stroke(width = width),
        )
    }
}
