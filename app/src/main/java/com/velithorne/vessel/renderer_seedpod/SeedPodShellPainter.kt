package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.velithorne.vessel.model.SeedPodDepthState
import com.velithorne.vessel.model.SeedPodLightingState
import com.velithorne.vessel.model.SeedPodVisualState
import com.velithorne.vessel.model.VesselPaletteState
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** Translucent wafer shell — split rear / front for pseudo-thickness. */
object SeedPodShellPainter {

    fun drawRear(
        scope: DrawScope,
        pod: Offset,
        rearOffset: Offset,
        radii: SeedPodContourBuilder.PodRadii,
        palette: VesselPaletteState,
        seedPalette: SeedPodPalette,
        appearance: SeedPodVisualState,
        depth: SeedPodDepthState,
        shellThickening: Float,
        tuning: SeedPodTuning,
        /** 0..1 — dims authored vesica shell when generated topology dominates. */
        seedFallbackAlpha: Float = 1f,
        outerGhostScale: Float = 1f,
        outerGhostAlphaMul: Float = 1f,
    ) {
        val c = pod + rearOffset
        val st = shellThickening.coerceIn(0f, 1f)
        val gs = outerGhostScale.coerceIn(0.55f, 1f)
        val gam = outerGhostAlphaMul.coerceIn(0.15f, 1f)
        val op = appearance.shellOpacityMul.coerceIn(0.3f, 1.3f) * seedFallbackAlpha.coerceIn(0f, 1f) * gam
        val dark = 1f - depth.rearDarkening * 0.35f

        scope.drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    seedPalette.siliconVeil.copy(alpha = 0.1f * op * dark),
                    Color(0xFF000000).copy(alpha = 0f),
                ),
                center = c,
                radius = max(radii.bandOuterRx, radii.bandOuterRy) * 1.15f * gs,
            ),
            topLeft = Offset(c.x - radii.bandOuterRx * 1.2f * gs, c.y - radii.bandOuterRy * 1.2f * gs),
            size = Size(radii.bandOuterRx * 2.4f * gs, radii.bandOuterRy * 2.4f * gs),
        )

        scope.drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.shellBase.copy(alpha = (0.12f + st * 0.1f) * op * dark),
                    palette.innerChamberShadow.copy(alpha = (0.2f + st * 0.08f) * op * dark),
                    seedPalette.gelEnvelope.copy(alpha = 0.07f * op),
                    Color(0xFF000000).copy(alpha = 0f),
                ),
                center = c,
                radius = max(radii.shellRx, radii.shellRy) * gs,
            ),
            topLeft = Offset(c.x - radii.shellRx * 1.15f * gs, c.y - radii.shellRy * 1.15f * gs),
            size = Size(radii.shellRx * 2.3f * gs, radii.shellRy * 2.3f * gs),
        )

        scope.drawOval(
            color = seedPalette.shellBandOuter.copy(alpha = (0.1f + st * 0.08f) * op * dark),
            topLeft = Offset(c.x - radii.bandMidRx * gs, c.y - radii.bandMidRy * gs),
            size = Size(radii.bandMidRx * 2f * gs, radii.bandMidRy * 2f * gs),
            style = Stroke(width = 1.2f + st * 1.8f),
        )
    }

    fun drawFront(
        scope: DrawScope,
        pod: Offset,
        frontOffset: Offset,
        radii: SeedPodContourBuilder.PodRadii,
        palette: VesselPaletteState,
        seedPalette: SeedPodPalette,
        appearance: SeedPodVisualState,
        lighting: SeedPodLightingState,
        shellThickening: Float,
        phaseSec: Float,
        closedness: Float,
        tuning: SeedPodTuning,
        seedFallbackAlpha: Float = 1f,
        outerGhostScale: Float = 1f,
        outerGhostAlphaMul: Float = 1f,
    ) {
        val c = pod + frontOffset
        val st = shellThickening.coerceIn(0f, 1f)
        val gs = outerGhostScale.coerceIn(0.55f, 1f)
        val gam = outerGhostAlphaMul.coerceIn(0.15f, 1f)
        val op = appearance.shellOpacityMul.coerceIn(0.3f, 1.3f) * seedFallbackAlpha.coerceIn(0f, 1f) * gam
        val edge = appearance.shellEdgeBright.coerceIn(0.3f, 1.3f) * (0.85f + lighting.shellCatchlight * 0.15f)

        scope.drawOval(
            color = palette.shellEdge.copy(alpha = (0.16f + st * 0.18f + edge * 0.12f) * op),
            topLeft = Offset(c.x - radii.innerChamberRx * 1.05f * gs, c.y - radii.innerChamberRy * 1.05f * gs),
            size = Size(radii.innerChamberRx * 2.1f * gs, radii.innerChamberRy * 2.1f * gs),
            style = Stroke(width = 1.1f + closedness * 0.9f + st * 0.8f),
        )

        val n = (tuning.conductiveSeamCount * seedFallbackAlpha.coerceIn(0f, 1f) * gam).toInt().coerceAtLeast(0)
        val shimmer = (sin(phaseSec * 1.8f).toFloat() * 0.5f + 0.5f)
        for (i in 0 until n) {
            val ang = (i / n.toFloat()) * kotlin.math.PI.toFloat() * 2f
            val cc = cos(ang)
            val ss = sin(ang)
            val r1 = max(radii.innerChamberRx, radii.innerChamberRy) * 0.35f
            val r2 = max(radii.shellRx, radii.shellRy) * (0.92f + shimmer * 0.04f) * gs
            val a = seedPalette.conductiveSeam.copy(
                alpha = (0.06f + edge * 0.12f) * op * (0.7f + shimmer * 0.3f) * (0.85f + lighting.lateralSheen * 0.15f),
            )
            scope.drawLine(
                color = a,
                start = Offset(c.x + cc * r1, c.y + ss * r1),
                end = Offset(c.x + cc * r2, c.y + ss * r2),
                strokeWidth = 0.85f,
            )
        }

        val frac = appearance.facetLineAlpha * 0.6f
        if (frac > 0.04f) {
            for (k in 0..3) {
                val a0 = (k / 4f) * kotlin.math.PI.toFloat() + phaseSec * 0.15f
                val cx = c.x + cos(a0) * radii.shellRx * 0.88f
                val cy = c.y + sin(a0) * radii.shellRy * 0.88f
                scope.drawCircle(
                    color = seedPalette.mineralFracture.copy(alpha = frac * 0.35f),
                    radius = 2f + k * 0.5f,
                    center = Offset(cx, cy),
                    style = Stroke(0.9f),
                )
            }
        }
    }
}
