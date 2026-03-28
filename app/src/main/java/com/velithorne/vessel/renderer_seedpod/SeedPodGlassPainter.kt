package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.velithorne.vessel.model.SeedPodVisualState

/** Chamber glass rim — restrained, premium. */
object SeedPodGlassPainter {

    fun drawFrame(
        scope: DrawScope,
        w: Float,
        h: Float,
        glassOffset: Offset,
        appearance: SeedPodVisualState,
        vitalityHint: Float,
    ) {
        val a = appearance.glassReflectionAlpha.coerceIn(0.06f, 0.32f)
        val pad = 12f
        val rect = Rect(Offset(pad + glassOffset.x, pad + glassOffset.y), Size(w - pad * 2, h - pad * 2))
        scope.drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF9ECBD8).copy(alpha = a * 0.45f + vitalityHint * 0.06f),
                    Color(0xFF5A8A9C).copy(alpha = a * 0.75f),
                ),
                start = rect.topLeft,
                end = Offset(rect.right, rect.bottom),
            ),
            topLeft = rect.topLeft,
            size = rect.size,
            cornerRadius = CornerRadius(22f, 22f),
            style = Stroke(width = 2f),
        )
        // Top sheen
        scope.drawLine(
            color = Color.White.copy(alpha = a * 0.35f),
            start = Offset(rect.left + 24f, rect.top + 5f),
            end = Offset(rect.right - 24f, rect.top + 5f),
            strokeWidth = 1.2f,
        )
    }
}
