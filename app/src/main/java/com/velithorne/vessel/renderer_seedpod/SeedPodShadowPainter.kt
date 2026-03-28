package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.velithorne.vessel.model.SeedPodDepthState
import com.velithorne.vessel.model.SeedPodVisualState

/** Lower-mass shadow and gentle rear depth darkening on pod silhouette. */
object SeedPodShadowPainter {

    fun drawPodGroundShadow(
        scope: DrawScope,
        pod: Offset,
        rx: Float,
        ry: Float,
        depth: SeedPodDepthState,
        appearance: SeedPodVisualState,
    ) {
        val hunger = appearance.shellOpacityMul.let { 1.2f - it * 0.15f }
        val a = (0.08f + depth.rearDarkening * 0.15f) * hunger.coerceIn(0.7f, 1.2f)
        scope.drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF000000).copy(alpha = a * 0.55f),
                    Color(0xFF000000).copy(alpha = 0f),
                ),
                center = Offset(pod.x, pod.y + ry * 1.15f),
                radius = rx * 1.1f,
            ),
            topLeft = Offset(pod.x - rx * 1.2f, pod.y + ry * 0.85f),
            size = Size(rx * 2.4f, ry * 0.55f),
        )
    }
}
