package com.velithorne.vessel.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Minimal chamber floor / containment read for seed-first modes (no legacy rear silhouette).
 */
object ChamberPainter {

    fun drawSeedChamberFloor(
        scope: DrawScope,
        w: Float,
        h: Float,
        seedCenter: Offset,
        visibility: Float,
    ) {
        val v = visibility.coerceIn(0f, 1f)
        if (v < 0.02f) return
        val y = h * 0.92f
        scope.drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF1A2838).copy(alpha = 0f),
                    Color(0xFF1A2838).copy(alpha = 0.12f * v),
                    Color(0xFF1A2838).copy(alpha = 0f),
                ),
                startX = 0f,
                endX = w,
            ),
            start = Offset(w * 0.08f, y),
            end = Offset(w * 0.92f, y),
            strokeWidth = 1.2f,
        )
        // Subtle specimen plinth ellipse under seed (suspended feel)
        scope.drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF4ECDC4).copy(alpha = 0.04f * v),
                    Color(0xFF000000).copy(alpha = 0f),
                ),
                center = Offset(seedCenter.x, y - h * 0.02f),
                radius = w * 0.22f,
            ),
            topLeft = Offset(seedCenter.x - w * 0.18f, y - h * 0.06f),
            size = Size(w * 0.36f, h * 0.05f),
        )
    }
}
