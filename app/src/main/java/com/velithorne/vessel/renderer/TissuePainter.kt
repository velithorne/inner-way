package com.velithorne.vessel.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.sin

/** Growth shimmer / budding overlay (morphogenesis visible activity). */
object TissuePainter {

    fun drawGrowthShimmer(
        scope: DrawScope,
        center: Offset,
        width: Float,
        height: Float,
        activity: Float,
        phase: Float,
    ) {
        if (activity < 0.04f) return
        val a = activity.coerceIn(0f, 1f)
        val shim = sin(phase * 2.2f) * 0.5f + 0.5f
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF5EEAD4).copy(alpha = 0f),
                    Color(0xFF4ECDC4).copy(alpha = a * 0.06f * shim),
                    Color(0xFFE8A85C).copy(alpha = a * 0.04f * (1f - shim)),
                ),
                center = Offset(center.x, center.y - height * 0.08f),
                radius = width * (0.55f + a * 0.15f),
            ),
            radius = width * 0.65f,
            center = center,
        )
    }
}
