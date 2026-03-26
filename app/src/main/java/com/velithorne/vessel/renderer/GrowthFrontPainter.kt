package com.velithorne.vessel.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import com.velithorne.vessel.model.VesselPaletteState
import kotlin.math.sin

/** Active tissue deposition edge — shimmer along shell + directional bias. */
object GrowthFrontPainter {

    fun draw(
        scope: DrawScope,
        center: Offset,
        width: Float,
        height: Float,
        scene: VesselSceneState,
        palette: VesselPaletteState,
        animSeconds: Float,
    ) {
        val gen = scene.generated
        val g = scene.growthStageVisual
        val a = gen.growthFrontMul * (0.35f + g.growthFrontIntensity * 0.65f)
        if (a < 0.04f) return

        val shellPath = VesselContourBuilder.bodyShellPath(center, width, height, scene.generated)
        val shim = sin(animSeconds * 2.4f + g.growthFrontDirX * 2f) * 0.5f + 0.5f
        val edgeCol = when (g.primaryFront) {
            com.velithorne.vessel.morphogenesis.GrowthFrontType.BRANCHING -> palette.accentSignal
            com.velithorne.vessel.morphogenesis.GrowthFrontType.COOLING_VEIL -> palette.thermalEdge
            com.velithorne.vessel.morphogenesis.GrowthFrontType.THICKENING -> palette.shellEdge
            com.velithorne.vessel.morphogenesis.GrowthFrontType.SWELLING -> palette.recoverySheen
            com.velithorne.vessel.morphogenesis.GrowthFrontType.EMBEDDING -> palette.gelMedium
        }

        scope.clipPath(shellPath) {
            scope.drawPath(
                path = shellPath,
                color = edgeCol.copy(alpha = a * 0.06f * (0.6f + shim * 0.4f)),
                style = Stroke(width = 2.8f + a * 2f),
            )
            val tip = Offset(
                center.x + g.growthFrontDirX * width * 0.22f,
                center.y + g.growthFrontDirY * height * 0.18f,
            )
            scope.drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        edgeCol.copy(alpha = a * 0.14f * shim),
                        Color(0xFF000000).copy(alpha = 0f),
                    ),
                    center = tip,
                    radius = width * 0.35f,
                ),
                radius = width * 0.28f,
                center = tip,
            )
        }
    }
}
