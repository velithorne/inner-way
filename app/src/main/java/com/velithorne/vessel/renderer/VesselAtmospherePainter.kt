package com.velithorne.vessel.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.velithorne.vessel.model.VesselPaletteState

/** Chamber depth — **rectangular** gradients only (large drawCircle + clip caused a dark curved band at the top). */
object VesselAtmospherePainter {

    fun drawBackdrop(
        scope: DrawScope,
        scene: VesselSceneState,
        palette: VesselPaletteState,
        tuning: RenderTuning,
        parallax: Offset,
    ) {
        val w = scope.size.width
        val h = scope.size.height
        val ox = parallax.x * tuning.rearParallaxMul * 0.35f
        val oy = parallax.y * tuning.rearParallaxMul * 0.25f
        scope.drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF101828),
                    Color(0xFF080C14),
                    Color(0xFF030508),
                ),
                startY = 0f + oy,
                endY = h + oy,
            ),
            topLeft = Offset(0f, 0f),
            size = scope.size,
        )
        scope.drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF0A1018).copy(alpha = 0.45f),
                    Color(0xFF000000).copy(alpha = 0f),
                    Color(0xFF0A1018).copy(alpha = 0.45f),
                ),
                startX = 0f + ox * 0.5f,
                endX = w + ox * 0.5f,
            ),
            topLeft = Offset(0f, 0f),
            size = scope.size,
        )
        val fogA = (0.045f + scene.fogDensity * tuning.fogDensityScale * 0.16f + scene.feverIntensity * 0.07f)
            .coerceIn(0.04f, 0.32f)
        scope.drawRect(
            color = palette.chamberMist.copy(alpha = fogA * palette.chamberMist.alpha * 0.85f),
            topLeft = Offset(0f, h * 0.35f),
            size = Size(w, h * 0.65f),
        )
    }

    fun drawScanSheen(scope: DrawScope, neural: Float, tuning: RenderTuning) {
        val a = (tuning.scanSheenAlpha * (0.35f + neural * 0.65f)).coerceIn(0f, 0.09f)
        val w = scope.size.width
        val h = scope.size.height
        scope.drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF4ECDC4).copy(alpha = 0f),
                    Color(0xFF8FD4E8).copy(alpha = a),
                    Color(0xFF4ECDC4).copy(alpha = 0f),
                ),
                start = Offset(w * 0.2f, 0f),
                end = Offset(w * 0.75f, h * 0.55f),
            ),
            size = scope.size,
        )
    }
}
