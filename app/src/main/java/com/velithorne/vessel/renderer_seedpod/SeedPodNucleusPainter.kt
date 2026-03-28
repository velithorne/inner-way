package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.velithorne.vessel.model.SeedPodDepthState
import com.velithorne.vessel.model.SeedPodLightingState
import com.velithorne.vessel.model.SeedPodVisualState
import com.velithorne.vessel.model.VesselPaletteState
import com.velithorne.vessel.renderer.VesselAnimationController
import kotlin.math.cos
import kotlin.math.sin

/** Crystalline layered nucleus — recessed, embedded, lit from lighting model. */
object SeedPodNucleusPainter {

    fun draw(
        scope: DrawScope,
        pod: Offset,
        layerOffset: Offset,
        recess: Offset,
        coreR: Float,
        depth: SeedPodDepthState,
        lighting: SeedPodLightingState,
        palette: VesselPaletteState,
        seedPalette: SeedPodPalette,
        appearance: SeedPodVisualState,
        anim: VesselAnimationController,
        tuning: SeedPodTuning,
        /** Dims stock ring-stack nucleus when generated chambers dominate. */
        seedNucleusAlpha: Float = 1f,
    ) {
        val center = pod + layerOffset + recess
        val pulse = anim.pulsePhase(1.05f)
        val nb = appearance.nucleusBrightnessMul.coerceIn(0.25f, 1.4f) * seedNucleusAlpha.coerceIn(0f, 1f)
        val bloom = appearance.nucleusBloomMul.coerceIn(0.2f, 1.25f) * lighting.coreBloom.coerceIn(0.2f, 1.2f)
        val facetA = appearance.facetLineAlpha.coerceIn(0.06f, 0.55f)

        val breathe = 0.96f + sin(pulse * kotlin.math.PI.toFloat() * 2f) * 0.04f
        val r = coreR * breathe * depth.nucleusBurialScale

        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.cortexNode.copy(alpha = 0.12f * bloom * nb),
                    palette.heartCore.copy(alpha = 0.08f * nb),
                    Color(0xFF000000).copy(alpha = 0f),
                ),
                center = center,
                radius = r * 2.4f,
            ),
            radius = r * 1.85f,
            center = center,
        )

        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.heartCore.copy(alpha = 0.48f * nb * (0.85f + 0.15f * lighting.sideFalloff)),
                    seedPalette.nucleusMid.copy(alpha = 0.38f * nb),
                    seedPalette.nucleusDeep.copy(alpha = 0.55f + (1f - depth.nucleusBurialScale) * 0.15f),
                    Color(0xFF000000).copy(alpha = 0f),
                ),
                center = center,
                radius = r * 1.6f,
            ),
            radius = r * 1.22f,
            center = center,
        )

        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFE8F4FF).copy(alpha = 0.32f * nb * bloom * lighting.rimLight),
                    palette.heartCore.copy(alpha = 0.42f * nb),
                    Color(0xFF000000).copy(alpha = 0f),
                ),
                center = Offset(center.x - r * 0.12f, center.y - r * 0.1f),
                radius = r * 0.9f,
            ),
            radius = r * 0.4f,
            center = center,
        )

        // Mineral kernel
        scope.drawCircle(
            color = seedPalette.mineralFracture.copy(alpha = 0.35f * facetA * nb),
            radius = r * 0.14f,
            center = Offset(center.x + r * 0.08f, center.y - r * 0.06f),
        )

        val n = tuning.nucleusFacetCount
        for (i in 0 until n) {
            val ang = (i / n.toFloat()) * kotlin.math.PI.toFloat() * 2f + pulse * 0.08f
            val ox = cos(ang) * r * 0.75f
            val oy = sin(ang) * r * 0.75f
            scope.drawLine(
                color = palette.shellRimCool.copy(alpha = facetA * 0.85f),
                start = center,
                end = Offset(center.x + ox, center.y + oy),
                strokeWidth = 1.1f,
            )
        }

        for (i in 0 until n) {
            val ang = (i / n.toFloat()) * kotlin.math.PI.toFloat() * 2f + 0.2f
            val ox = cos(ang) * r * 0.48f
            val oy = sin(ang) * r * 0.48f
            scope.drawCircle(
                color = palette.cortexNode.copy(alpha = 0.22f * nb * bloom),
                radius = r * 0.11f,
                center = Offset(center.x + ox, center.y + oy),
            )
        }

        scope.drawCircle(
            color = palette.heartRing.copy(alpha = 0.26f * nb),
            radius = r * 0.92f,
            center = center,
            style = Stroke(1f),
        )
    }
}
