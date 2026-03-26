package com.velithorne.vessel.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.velithorne.vessel.model.VesselPaletteState

/** Chamber depth, suspension haze, subtle lab scan — specimen remains focal. */
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
        val cx = w / 2f + parallax.x * tuning.rearParallaxMul * 0.35f
        val cy = h / 2f + parallax.y * tuning.rearParallaxMul * 0.25f
        val center = Offset(cx, cy)
        val r = kotlin.math.max(w, h) * 0.88f
        // Gradient center MUST match drawCircle center — offset center caused a dark arc band at the top.
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF141C2E).copy(alpha = 0.94f),
                    Color(0xFF080C14),
                    Color(0xFF020408),
                ),
                center = center,
                radius = r,
            ),
            radius = r,
            center = center,
        )
        val fogA = (0.045f + scene.fogDensity * tuning.fogDensityScale * 0.16f + scene.feverIntensity * 0.07f)
            .coerceIn(0.04f, 0.32f)
        scope.drawCircle(
            color = palette.chamberMist.copy(alpha = fogA * palette.chamberMist.alpha),
            radius = r * 0.91f,
            center = center,
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
