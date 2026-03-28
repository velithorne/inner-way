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
        val pulse = 1f + sin(phase * 2f * Math.PI.toFloat()) * 0.045f
        scope.drawCircle(
            color = accent.copy(alpha = (0.28f + sin(phase) * 0.1f) * s),
            radius = baseRadius * pulse * 1.28f,
            center = center,
            style = Stroke(width = 2.4f),
        )
        scope.drawCircle(
            color = accent.copy(alpha = (0.1f * s)),
            radius = baseRadius * pulse * 1.45f,
            center = center,
            style = Stroke(width = 1.1f),
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
                    Color(0xFF05080C).copy(alpha = v * 0.36f),
                ),
                center = focusCenter,
                radius = scope.size.maxDimension * 0.92f,
            ),
            radius = scope.size.maxDimension,
            center = focusCenter,
        )
    }
}
