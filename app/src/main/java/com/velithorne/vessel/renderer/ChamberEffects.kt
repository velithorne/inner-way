package com.velithorne.vessel.renderer

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/** Containment chamber backdrop, rim, glass sheen, and fog mask. */
object ChamberEffects {

    fun drawChamberBackdrop(scope: DrawScope, fog: Float, thermal: Float) {
        val w = scope.size.width
        val h = scope.size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = kotlin.math.max(w, h) * 0.85f
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF12192A).copy(alpha = 0.95f),
                    Color(0xFF070A0F),
                    Color(0xFF030508),
                ),
                center = Offset(cx, cy * 0.9f),
                radius = r,
            ),
            radius = r,
            center = Offset(cx, cy),
        )
        val fogA = (0.05f + fog * 0.18f + thermal * 0.08f).coerceIn(0.04f, 0.35f)
        scope.drawCircle(
            color = Color(0xFF6B8AA0).copy(alpha = fogA * 0.35f),
            radius = r * 0.92f,
            center = Offset(cx, cy * 0.92f),
        )
    }

    fun drawGridSheen(scope: DrawScope, alpha: Float) {
        val a = alpha.coerceIn(0f, 0.12f)
        val step = 48f
        var x = 0f
        while (x < scope.size.width) {
            scope.drawLine(
                Color(0xFF2EF3D0).copy(alpha = a * 0.35f),
                Offset(x, 0f),
                Offset(x, scope.size.height),
                strokeWidth = 1f,
            )
            x += step
        }
        var y = 0f
        while (y < scope.size.height) {
            scope.drawLine(
                Color(0xFF2EF3D0).copy(alpha = a * 0.25f),
                Offset(0f, y),
                Offset(scope.size.width, y),
                strokeWidth = 1f,
            )
            y += step
        }
    }

    fun drawChamberFrame(scope: DrawScope, cornerRadius: Float, vitalityGlow: Float) {
        val pad = 12f
        val rect = Rect(
            offset = Offset(pad, pad),
            size = Size(scope.size.width - pad * 2, scope.size.height - pad * 2),
        )
        val edge = Color(0xFF4ECDC4).copy(alpha = 0.12f + vitalityGlow * 0.15f)
        scope.drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    edge.copy(alpha = edge.alpha * 0.4f),
                    edge,
                    edge.copy(alpha = edge.alpha * 0.3f),
                ),
                start = rect.topLeft,
                end = Offset(rect.right, rect.bottom),
            ),
            topLeft = rect.topLeft,
            size = rect.size,
            cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            style = Stroke(width = 2.2f),
        )
    }

    fun drawGlassReflection(scope: DrawScope) {
        val w = scope.size.width
        val brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0f),
                Color.White.copy(alpha = 0.04f),
                Color.White.copy(alpha = 0f),
            ),
            start = Offset(w * 0.15f, 0f),
            end = Offset(w * 0.55f, scope.size.height * 0.4f),
        )
        scope.drawRect(brush = brush, size = scope.size)
    }
}
