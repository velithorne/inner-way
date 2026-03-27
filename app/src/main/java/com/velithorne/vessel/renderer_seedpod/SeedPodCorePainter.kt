package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.velithorne.vessel.model.SeedPodVisualState
import com.velithorne.vessel.model.VesselPaletteState
import com.velithorne.vessel.renderer.VesselAnimationController
import kotlin.math.cos
import kotlin.math.sin

/** Crystalline layered nucleus — latent engine, not a flat blob. */
object SeedPodCorePainter {

    fun draw(
        scope: DrawScope,
        pod: Offset,
        coreR: Float,
        palette: VesselPaletteState,
        seedPalette: SeedPodPalette,
        appearance: SeedPodVisualState,
        anim: VesselAnimationController,
        tuning: SeedPodTuning,
    ) {
        val pulse = anim.pulsePhase(1.05f)
        val nb = appearance.nucleusBrightnessMul.coerceIn(0.25f, 1.4f)
        val bloom = appearance.nucleusBloomMul.coerceIn(0.2f, 1.25f)
        val facetA = appearance.facetLineAlpha.coerceIn(0.06f, 0.55f)

        val breathe = 0.96f + sin(pulse * kotlin.math.PI.toFloat() * 2f) * 0.04f
        val r = coreR * breathe

        // Outer bloom
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.cortexNode.copy(alpha = 0.12f * bloom * nb),
                    palette.heartCore.copy(alpha = 0.08f * nb),
                    Color(0xFF000000).copy(alpha = 0f),
                ),
                center = pod,
                radius = r * 2.4f,
            ),
            radius = r * 1.85f,
            center = pod,
        )

        // Mid crystalline body
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.heartCore.copy(alpha = 0.52f * nb),
                    seedPalette.nucleusMid.copy(alpha = 0.38f * nb),
                    seedPalette.nucleusDeep.copy(alpha = 0.55f),
                    Color(0xFF000000).copy(alpha = 0f),
                ),
                center = pod,
                radius = r * 1.6f,
            ),
            radius = r * 1.25f,
            center = pod,
        )

        // Bright core pin
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFE8F4FF).copy(alpha = 0.35f * nb * bloom),
                    palette.heartCore.copy(alpha = 0.45f * nb),
                    Color(0xFF000000).copy(alpha = 0f),
                ),
                center = Offset(pod.x - r * 0.12f, pod.y - r * 0.1f),
                radius = r * 0.9f,
            ),
            radius = r * 0.42f,
            center = pod,
        )

        // Facet lines (hex)
        val n = tuning.nucleusFacetCount
        for (i in 0 until n) {
            val ang = (i / n.toFloat()) * kotlin.math.PI.toFloat() * 2f + pulse * 0.08f
            val ox = cos(ang) * r * 0.75f
            val oy = sin(ang) * r * 0.75f
            scope.drawLine(
                color = palette.shellRimCool.copy(alpha = facetA * 0.85f),
                start = pod,
                end = Offset(pod.x + ox, pod.y + oy),
                strokeWidth = 1.1f,
            )
        }

        // Secondary nodules on inner ring
        for (i in 0 until n) {
            val ang = (i / n.toFloat()) * kotlin.math.PI.toFloat() * 2f + 0.2f
            val ox = cos(ang) * r * 0.48f
            val oy = sin(ang) * r * 0.48f
            scope.drawCircle(
                color = palette.cortexNode.copy(alpha = 0.22f * nb * bloom),
                radius = r * 0.11f,
                center = Offset(pod.x + ox, pod.y + oy),
            )
        }

        // Inner ring stroke
        scope.drawCircle(
            color = palette.heartRing.copy(alpha = 0.28f * nb),
            radius = r * 0.92f,
            center = pod,
            style = Stroke(1f),
        )
    }
}
