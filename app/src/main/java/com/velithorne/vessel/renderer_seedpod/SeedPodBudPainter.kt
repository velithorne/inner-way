package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.velithorne.vessel.model.SeedBudVisualState
import com.velithorne.vessel.model.SeedPodDepthState
import com.velithorne.vessel.model.SeedPodLayerState
import com.velithorne.vessel.model.SeedPodLightingState
import com.velithorne.vessel.model.VesselPaletteState
import kotlin.math.max

/** Crown / lateral / reserve with depth offsets and local shadow. */
object SeedPodBudPainter {

    fun draw(
        scope: DrawScope,
        pod: Offset,
        minDim: Float,
        buds: SeedBudVisualState,
        layers: SeedPodLayerState,
        radii: SeedPodContourBuilder.PodRadii,
        depth: SeedPodDepthState,
        lighting: SeedPodLightingState,
        palette: VesselPaletteState,
        tuning: SeedPodTuning,
    ) {
        val w = minDim
        val crown = buds.crown
        if (crown > 0.02f) {
            val mul = tuning.crownBudSizeCurve * depth.budDepthMul
            val cBase = pod + layers.budsCrown
            val cy = cBase.y - max(radii.shellRy, radii.shellRx) * (0.85f + crown * 0.12f * mul)
            val cx = cBase.x
            val br = w * (0.04f + crown * 0.055f) * mul
            scope.drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF000000).copy(alpha = 0.12f * crown),
                        Color(0xFF000000).copy(alpha = 0f),
                    ),
                    center = Offset(cx, cy + br * 0.4f),
                    radius = br * 1.4f,
                ),
                topLeft = Offset(cx - br * 1.2f, cy + br * 0.15f),
                size = Size(br * 2.4f, br * 0.5f),
            )
            scope.drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.cortexNode.copy(alpha = crown * 0.45f * (0.85f + lighting.coreBloom * 0.15f)),
                        palette.accentSignal.copy(alpha = crown * 0.2f * (0.8f + lighting.lateralSheen * 0.2f)),
                        Color(0xFF000000).copy(alpha = 0f),
                    ),
                    center = Offset(cx - br * 0.35f, cy),
                    radius = br * 1.2f,
                ),
                radius = br * 0.9f,
                center = Offset(cx - br * 0.35f, cy),
            )
            scope.drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.cortexNode.copy(alpha = crown * 0.4f),
                        Color(0xFF000000).copy(alpha = 0f),
                    ),
                    center = Offset(cx + br * 0.35f, cy),
                    radius = br * 1.1f,
                ),
                radius = br * 0.85f,
                center = Offset(cx + br * 0.35f, cy),
            )
        }

        val latL = buds.lateralLeft
        val latR = buds.lateralRight
        if (max(latL, latR) > 0.02f) {
            val mul = tuning.lateralBudSizeCurve * depth.budDepthMul
            val y = pod.y + layers.budsLateral.y + w * 0.015f
            val span = max(radii.shellRx, radii.bandMidRx) * 1.05f
            for (side in listOf(-1f, 1f)) {
                val strength = if (side < 0) latL else latR
                if (strength < 0.02f) continue
                val bx = pod.x + layers.budsLateral.x + side * (span + w * 0.02f * strength * mul)
                scope.drawOval(
                    color = Color(0xFF000000).copy(alpha = 0.1f * strength),
                    topLeft = Offset(bx - w * 0.04f, y + w * 0.02f),
                    size = Size(w * 0.08f * strength, w * 0.03f),
                )
                val rw = w * (0.022f + strength * 0.038f) * mul
                val rh = w * (0.045f + strength * 0.06f) * mul
                scope.drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            palette.accentSignal.copy(alpha = strength * 0.35f * (0.9f + lighting.lateralSheen * 0.1f)),
                            palette.shellBase.copy(alpha = strength * 0.22f),
                        ),
                        start = Offset(bx - rw, y - rh * 0.5f),
                        end = Offset(bx + rw, y + rh * 0.5f),
                    ),
                    topLeft = Offset(bx - rw, y - rh * 0.5f),
                    size = Size(rw * 2f, rh),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(rw * 0.6f, rw * 0.6f),
                )
                scope.drawRoundRect(
                    color = palette.accentSignal.copy(alpha = strength * 0.35f),
                    topLeft = Offset(bx - rw, y - rh * 0.5f),
                    size = Size(rw * 2f, rh),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(rw * 0.6f, rw * 0.6f),
                    style = Stroke(1f),
                )
            }
        }

        val res = buds.reserve
        if (res > 0.02f) {
            val mul = tuning.reserveBudSizeCurve * depth.budDepthMul
            val by = pod.y + layers.budsReserve.y + max(radii.shellRy, radii.shellRx) * (0.92f + res * 0.06f)
            val dropR = w * (0.035f + res * 0.07f) * mul
            scope.drawOval(
                color = Color(0xFF000000).copy(alpha = 0.14f * res),
                topLeft = Offset(pod.x + layers.budsReserve.x - dropR * 1.1f, by + dropR * 0.35f),
                size = Size(dropR * 2.2f, dropR * 0.45f),
            )
            scope.drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.archiveDeep.copy(alpha = res * 0.5f * (0.9f + (1f - lighting.sideFalloff) * 0.1f)),
                        palette.gelMedium.copy(alpha = res * 0.25f),
                        Color(0xFF000000).copy(alpha = 0f),
                    ),
                    center = Offset(pod.x + layers.budsReserve.x, by),
                    radius = dropR * 1.3f,
                ),
                topLeft = Offset(pod.x + layers.budsReserve.x - dropR * 1.1f, by - dropR * 0.85f),
                size = Size(dropR * 2.2f, dropR * 1.7f),
            )
            scope.drawLine(
                color = palette.recoverySheen.copy(alpha = res * 0.35f),
                start = Offset(pod.x + layers.budsReserve.x - dropR * 0.5f, by - dropR * 0.35f),
                end = Offset(pod.x + layers.budsReserve.x + dropR * 0.45f, by - dropR * 0.42f),
                strokeWidth = 1.2f,
            )
        }
    }
}
