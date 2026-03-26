package com.velithorne.vessel.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.sin

/** Crystalline seed nucleus — layered translucent core at specimen center. */
object SeedPainter {

    fun draw(
        scope: DrawScope,
        center: Offset,
        specimenWidth: Float,
        specimenHeight: Float,
        scene: VesselSceneState,
        palette: com.velithorne.vessel.model.VesselPaletteState,
        pulse: Float,
    ) {
        val sv = scene.seedVisual
        val gen = scene.generated
        // Prominent crystalline core — primary “seed” read when organs are hidden.
        val r = specimenWidth * sv.coreRadiusNorm.coerceIn(0.07f, 0.14f) * (0.95f + gen.tissueBodyFillMul * 0.12f)
        val stress = sv.latticeStress.coerceIn(0f, 1f)
        val lum = sv.reserveLuminance.coerceIn(0.1f, 1f)
        val pulseWobble = sin(pulse * 1.15f) * 0.04f * stress

        val core = palette.cortexNode.copy(alpha = 0.22f * lum * (1f + sv.seedDensity * 0.35f))
        val halo = palette.shellRimCool.copy(alpha = 0.14f * sv.shellCoherence)
        val nucleus = palette.heartCore.copy(alpha = 0.35f * sv.seedDensity * lum)

        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    nucleus.copy(alpha = nucleus.alpha * (0.9f + pulseWobble)),
                    core,
                    halo.copy(alpha = halo.alpha * 0.5f),
                    Color(0xFF000000).copy(alpha = 0f),
                ),
                center = center,
                radius = r * (1.65f + sv.branchLatentEnergy * 0.15f),
            ),
            radius = r * (1.55f + sv.germinationProgress * 0.25f),
            center = center,
        )
        // Inner wafer ring
        scope.drawCircle(
            color = palette.shellEdge.copy(alpha = 0.12f + stress * 0.08f),
            radius = r * (1.02f + sin(pulse * 0.9f) * 0.02f),
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.4f + stress * 1.2f),
        )
    }
}
