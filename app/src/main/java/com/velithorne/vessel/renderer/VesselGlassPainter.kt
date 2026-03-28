package com.velithorne.vessel.renderer

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/** Containment rim + reflection sweep (parallax-shifted). */
object VesselGlassPainter {

    fun drawFrame(
        scope: DrawScope,
        vitalityGlow: Float,
        tuning: RenderTuning,
        parallax: Offset,
    ) {
        val pad = 12f + parallax.x * tuning.shellParallaxMul * 0.08f
        val rect = Rect(
            offset = Offset(pad, pad),
            size = Size(scope.size.width - pad * 2, scope.size.height - pad * 2),
        )
        val edge = Color(0xFF7AB8C8).copy(alpha = 0.1f + vitalityGlow * 0.14f)
        scope.drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    edge.copy(alpha = edge.alpha * 0.35f),
                    edge,
                    edge.copy(alpha = edge.alpha * 0.28f),
                ),
                start = rect.topLeft,
                end = Offset(rect.right, rect.bottom),
            ),
            topLeft = rect.topLeft,
            size = rect.size,
            cornerRadius = CornerRadius(22f, 22f),
            style = Stroke(width = 2f),
        )
    }

    fun drawReflectionSweep(scope: DrawScope, tuning: RenderTuning, parallax: Offset) {
        val w = scope.size.width
        val h = scope.size.height
        val ox = parallax.x * tuning.shellParallaxMul * 0.45f
        val oy = parallax.y * tuning.shellParallaxMul * 0.35f
        scope.drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0f),
                    Color.White.copy(alpha = tuning.reflectionSweepAlpha),
                    Color.White.copy(alpha = 0f),
                ),
                start = Offset(w * 0.12f + ox, oy),
                end = Offset(w * 0.58f + ox, h * 0.42f + oy),
            ),
            size = scope.size,
        )
    }
}
