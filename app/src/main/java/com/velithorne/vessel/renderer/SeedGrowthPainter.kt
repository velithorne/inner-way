package com.velithorne.vessel.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import com.velithorne.vessel.model.VesselPaletteState
import kotlin.math.cos
import kotlin.math.sin

/**
 * Growth that **emerges from the seed** — short fronds, crown hint, lower basin hint, shell halo.
 * Uses [VesselSceneState.seedLocalEmergence] so early stages stay compact around the nucleus.
 */
object SeedGrowthPainter {

    fun draw(
        scope: DrawScope,
        center: Offset,
        width: Float,
        height: Float,
        scene: VesselSceneState,
        palette: VesselPaletteState,
        phase: Float,
        pulse: Float,
    ) {
        val em = scene.seedLocalEmergence.coerceIn(0f, 1f)
        if (em < 0.02f) return

        val gen = scene.generated
        val gv = scene.growthVisuals
        val mode = scene.stageRenderMode
        val shellPath = VesselContourBuilder.bodyShellPath(center, width, height, gen)

        // Tighter radii in early modes — growth hugs the seed
        val spread = when (mode) {
            VesselStageRenderMode.SEED_ONLY -> 0.22f
            VesselStageRenderMode.GERMINATING -> 0.32f
            VesselStageRenderMode.EARLY_BRANCHING -> 0.42f
            else -> 0.55f
        } * em

        scope.clipPath(shellPath) {
            // Upper crown bloom — close to seed
            val cr = (gen.budNeuralCrownMul * 0.4f + gv.crownBloomIntensity * 0.6f).coerceIn(0f, 1f) * em
            if (cr > 0.03f) {
                val crown = Offset(center.x + cos(phase) * width * 0.02f * spread, center.y - height * (0.12f + 0.08f * spread))
                scope.drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            palette.cortexNode.copy(alpha = cr * 0.22f),
                            palette.cortexFilament.copy(alpha = cr * 0.12f),
                            Color(0xFF000000).copy(alpha = 0f),
                        ),
                        center = crown,
                        radius = width * 0.14f * cr * spread,
                    ),
                    radius = width * 0.16f * cr * spread,
                    center = crown,
                )
            }

            // Short lateral buds from seed equator
            val fr = (gen.budSignalFrondMul * 0.45f + gv.frondBudLengthLeft * 0.55f).coerceIn(0f, 1f) * em
            if (fr > 0.04f) {
                for (side in listOf(-1f, 1f)) {
                    val base = Offset(center.x + side * width * 0.04f, center.y + height * 0.02f)
                    val path = Path().apply {
                        moveTo(base.x, base.y)
                        quadraticTo(
                            base.x + side * width * 0.1f * fr * spread,
                            base.y - height * 0.04f * fr * spread,
                            base.x + side * width * 0.14f * fr * spread,
                            base.y + height * 0.01f * fr,
                        )
                    }
                    scope.drawPath(
                        path = path,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                palette.accentSignal.copy(alpha = fr * 0.22f),
                                palette.lungFrond.copy(alpha = fr * 0.14f),
                            ),
                            start = base,
                            end = Offset(base.x + side * width * 0.12f * spread, base.y),
                        ),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.4f + fr * 1f),
                    )
                }
            }

            // Tiny lower reservoir hint — directly under nucleus
            val basin = (gv.lowerReservoirDepth * 0.55f + gen.tissueBodyFillMul * 0.2f).coerceIn(0f, 1f) * em
            if (basin > 0.04f) {
                scope.drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            palette.archiveDeep.copy(alpha = basin * 0.14f),
                            Color(0xFF000000).copy(alpha = 0f),
                        ),
                        center = Offset(center.x, center.y + height * 0.1f * (0.6f + spread)),
                        radius = width * 0.12f * basin,
                    ),
                    radius = width * 0.14f * basin,
                    center = Offset(center.x, center.y + height * 0.1f),
                )
            }

            // Shell halo thickening — perimeter around seed
            val sh = (gen.shellOpacityMul * 0.35f + gv.shellThickeningIntensity * 0.45f).coerceIn(0f, 1f) * em
            if (sh > 0.04f) {
                val wobble = sin(pulse * 1.1f) * 0.02f
                scope.drawCircle(
                    color = palette.shellEdge.copy(alpha = sh * 0.14f),
                    radius = width * (0.14f + spread * 0.08f) + wobble * width * 0.02f,
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f + sh * 0.8f),
                )
            }
        }
    }
}
