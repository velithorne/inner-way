package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.velithorne.vessel.model.SeedPodDepthState
import com.velithorne.vessel.model.SeedPodVisualState

/** Front shell occludes upper nucleus — soft radial shadow on core top. */
object SeedPodOcclusionPainter {

    fun drawNucleusOcclusion(
        scope: DrawScope,
        nucleusCenter: Offset,
        coreR: Float,
        depth: SeedPodDepthState,
        appearance: SeedPodVisualState,
        tuning: SeedPodTuning,
        seedNucleusAlpha: Float = 1f,
    ) {
        val sa = seedNucleusAlpha.coerceIn(0f, 1f)
        val a = (tuning.depth.occlusionAlphaMax * (0.35f + depth.shellThicknessVisual * 0.55f)).coerceIn(0.04f, 0.24f) * sa
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF050810).copy(alpha = a * 0.95f),
                    Color(0xFF050810).copy(alpha = 0f),
                ),
                center = Offset(nucleusCenter.x, nucleusCenter.y - coreR * 0.42f),
                radius = coreR * 1.9f,
            ),
            radius = coreR * 1.28f,
            center = nucleusCenter,
        )
        scope.drawCircle(
            color = Color(0xFF02060C).copy(alpha = a * 0.35f * (1f - appearance.nucleusBloomMul * 0.12f)),
            radius = coreR * 1.02f,
            center = nucleusCenter,
            style = Stroke(1.5f),
        )
    }
}
