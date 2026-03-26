package com.velithorne.vessel.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.sin

/** Selection ring and vignette for inspection mode. */
object OrganHighlightPainter {

    fun drawSelectionRing(
        scope: DrawScope,
        center: Offset,
        baseRadius: Float,
        accent: Color,
        phase: Float,
        strength: Float,
    ) {
        val s = strength.coerceIn(0f, 2f)
        val pulse = 1f + sin(phase * 2f * Math.PI.toFloat()) * 0.06f
        scope.drawCircle(
            color = accent.copy(alpha = (0.35f + sin(phase) * 0.15f) * s),
            radius = baseRadius * pulse * 1.35f,
            center = center,
            style = Stroke(width = 3f),
        )
        scope.drawCircle(
            color = accent.copy(alpha = (0.12f * s)),
            radius = baseRadius * pulse * 1.55f,
            center = center,
            style = Stroke(width = 1.3f),
        )
    }

    fun drawFocusVignette(
        scope: DrawScope,
        focusCenter: Offset,
        vignetteStrength: Float,
    ) {
        if (vignetteStrength < 0.05f) return
        val v = vignetteStrength.coerceIn(0f, 1f)
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0f),
                    Color.Black.copy(alpha = v * 0.45f),
                ),
                center = focusCenter,
                radius = scope.size.maxDimension * 0.9f,
            ),
            radius = scope.size.maxDimension,
            center = focusCenter,
        )
    }
}
