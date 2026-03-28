package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.velithorne.vessel.model.VisibleMorphologyState
import com.velithorne.vessel.model.VesselPaletteState
import kotlin.math.max

/**
 * Crown / reserve chamber placement — not the default centered stack when [VisibleMorphologyState.generatedTopologyInfluence] is high.
 */
object GeneratedChamberPainter {

    fun draw(
        scope: DrawScope,
        pod: Offset,
        minDim: Float,
        radii: SeedPodContourBuilder.PodRadii,
        palette: VesselPaletteState,
        visible: VisibleMorphologyState,
        phaseSec: Float,
    ) {
        val g = visible.generatedTopologyInfluence.coerceIn(0f, 1f)
        if (g < 0.12f) return
        val rx = max(radii.shellRx, radii.shellRy)
        val crown = Offset(
            pod.x + (visible.crownChamberNx - 0.5f) * rx * 1.85f,
            pod.y + (visible.crownChamberNy - 0.5f) * radii.shellRy * 1.9f,
        )
        val crownR = minDim * (0.028f + visible.crownChamberRadiusMul * 0.004f) * (0.55f + g * 0.55f)
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.shellBase.copy(alpha = 0.15f * g),
                    palette.cortexNode.copy(alpha = 0.08f * g),
                    Color(0xFF000000).copy(alpha = 0f),
                ),
                center = crown,
                radius = crownR * 1.8f,
            ),
            radius = crownR,
            center = crown,
        )
        val res = Offset(
            pod.x + (visible.reserveBasinNx - 0.5f) * rx * 1.75f,
            pod.y + (visible.reserveBasinNy - 0.48f) * radii.shellRy * 1.9f,
        )
        val resR = minDim * (0.04f + visible.reserveBasinDepthMul * 0.018f) * g
        scope.drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.innerChamberShadow.copy(alpha = 0.22f * g * visible.reserveBasinDepthMul.coerceIn(0.5f, 1.2f)),
                    Color(0xFF000000).copy(alpha = 0f),
                ),
                center = res,
                radius = resR * 2.2f,
            ),
            topLeft = Offset(res.x - resR * 1.4f, res.y - resR * 0.9f),
            Size(resR * 2.8f, resR * 1.8f),
        )
    }
}
