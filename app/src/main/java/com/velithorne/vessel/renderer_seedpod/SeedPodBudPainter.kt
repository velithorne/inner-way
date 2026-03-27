package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.velithorne.vessel.model.SeedBudVisualState
import com.velithorne.vessel.model.SeedPodDepthState
import com.velithorne.vessel.model.SeedPodLayerState
import com.velithorne.vessel.model.SeedPodLightingState
import com.velithorne.vessel.model.VesselPaletteState
import kotlin.math.max
import kotlin.random.Random

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
            val shellR = max(radii.shellRx, radii.bandMidRx)
            val specimenSeed = pod.x.toBits().toLong() xor (pod.y.toBits().toLong() shl 32)
            drawSignalLungFronds(
                scope = scope,
                pod = pod,
                layersOffset = layers.budsLateral,
                shellRx = shellR,
                minDim = w,
                baseY = y,
                latL = latL,
                latR = latR,
                mul = mul,
                lighting = lighting,
                palette = palette,
                specimenSeed = specimenSeed,
            )
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

    /**
     * Lateral signal “lungs”: curved fronds emerging from the shell — not flat green tabs.
     * Shape is deterministic per specimen anchor; growth follows [latL]/[latR] over time.
     */
    private fun drawSignalLungFronds(
        scope: DrawScope,
        pod: Offset,
        layersOffset: Offset,
        shellRx: Float,
        minDim: Float,
        baseY: Float,
        latL: Float,
        latR: Float,
        mul: Float,
        lighting: SeedPodLightingState,
        palette: VesselPaletteState,
        specimenSeed: Long,
    ) {
        val cx = pod.x + layersOffset.x
        val attachY = baseY
        val span = shellRx * 1.02f
        val sheen = lighting.lateralSheen.coerceIn(0f, 1f)

        for (side in listOf(-1f, 1f)) {
            val strength = (if (side < 0) latL else latR).coerceIn(0f, 1.2f)
            if (strength < 0.02f) continue

            val rnd = Random(specimenSeed xor (if (side < 0) 0x4C1L else 0x4C2L))
            val angleJitter = (rnd.nextFloat() - 0.5f) * 0.14f
            val lengthJitter = 0.92f + rnd.nextFloat() * 0.16f
            val bulgeJitter = 0.94f + rnd.nextFloat() * 0.12f

            val reach = minDim * (0.04f + strength * 0.11f) * mul * lengthJitter
            val rootX = cx + side * (span + minDim * 0.008f * strength)
            val midX = rootX + side * reach * 0.48f * bulgeJitter
            val tipX = rootX + side * reach * 1.05f
            val midY = attachY - reach * (0.22f + strength * 0.08f + angleJitter)
            val tipY = attachY - reach * (0.05f * rnd.nextFloat())

            val path = Path().apply {
                moveTo(rootX, attachY)
                quadraticTo(midX, midY, tipX, tipY)
            }
            val strokeMain = 1.4f + strength * 2.8f
            val core = palette.lungFrond.copy(alpha = strength * 0.42f * (0.85f + sheen * 0.15f))
            val rim = palette.accentSignal.copy(alpha = strength * 0.28f * (0.9f + sheen * 0.1f))

            scope.drawPath(
                path = path,
                brush = Brush.linearGradient(
                    colors = listOf(
                        palette.shellBase.copy(alpha = strength * 0.2f),
                        core,
                        rim,
                    ),
                    start = Offset(rootX, attachY),
                    end = Offset(tipX, tipY),
                ),
                style = Stroke(width = strokeMain, cap = StrokeCap.Round),
            )
            scope.drawPath(
                path = path,
                color = palette.neuralPathway.copy(alpha = strength * 0.22f),
                style = Stroke(width = (strokeMain * 0.45f).coerceAtLeast(0.6f), cap = StrokeCap.Round),
            )

            // Secondary branch (only when strong — reads like lobes growing)
            if (strength > 0.35f) {
                val brLen = reach * (0.45f + rnd.nextFloat() * 0.15f)
                val bx = rootX + side * brLen * 0.55f
                val by = attachY - brLen * (0.35f + rnd.nextFloat() * 0.12f)
                val b2 = Path().apply {
                    moveTo(rootX + side * shellRx * 0.04f, attachY - shellRx * 0.02f)
                    quadraticTo(
                        bx + side * brLen * 0.2f,
                        by - brLen * 0.2f,
                        bx + side * brLen * 0.85f,
                        by,
                    )
                }
                scope.drawPath(
                    path = b2,
                    color = palette.lungFrond.copy(alpha = strength * 0.25f),
                    style = Stroke(width = 1f + strength * 1.2f, cap = StrokeCap.Round),
                )
            }

            scope.drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF000000).copy(alpha = 0.12f * strength),
                        Color(0xFF000000).copy(alpha = 0f),
                    ),
                    center = Offset(tipX, tipY + minDim * 0.01f),
                    radius = reach * 0.35f,
                ),
                topLeft = Offset(tipX - reach * 0.25f, tipY),
                size = Size(reach * 0.5f, reach * 0.22f),
            )
        }
    }
}
