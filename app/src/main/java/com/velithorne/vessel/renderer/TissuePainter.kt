package com.velithorne.vessel.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.sin

/** Growth shimmer / accretion pulse (ties to [com.velithorne.vessel.morphogenesis.GrowthVisualizer]). */
object TissuePainter {

    fun drawGrowthShimmer(
        scope: DrawScope,
        center: Offset,
        width: Float,
        height: Float,
        scene: VesselSceneState,
        phase: Float,
    ) {
        val gv = scene.growthVisuals
        val activity = (
            scene.generated.visibleGrowthActivity * 0.5f + gv.activeAccretionPulse * 0.35f +
                gv.growthFrontEdgeIntensity * 0.15f
            ).coerceIn(0f, 1f)
        if (activity < 0.03f) return
        val a = activity.coerceIn(0f, 1f)
        val shim = sin(phase * 2.2f) * 0.5f + 0.5f
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF5EEAD4).copy(alpha = 0f),
                    Color(0xFF4ECDC4).copy(alpha = a * 0.1f * shim),
                    Color(0xFFE8A85C).copy(alpha = a * 0.07f * (1f - shim)),
                ),
                center = Offset(center.x, center.y - height * 0.08f),
                radius = width * (0.55f + a * 0.18f),
            ),
            radius = width * 0.68f,
            center = center,
        )
    }
}
