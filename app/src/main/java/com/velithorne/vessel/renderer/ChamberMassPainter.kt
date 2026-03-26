package com.velithorne.vessel.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import com.velithorne.vessel.model.VesselPaletteState
import kotlin.math.sin

/**
 * Regional chamber fill — soft metaball-like masses clipped to body shell for grown volume.
 */
object ChamberMassPainter {

    fun draw(
        scope: DrawScope,
        center: Offset,
        width: Float,
        height: Float,
        scene: VesselSceneState,
        palette: VesselPaletteState,
        breath: Float,
    ) {
        val gen = scene.generated
        val gv = scene.growthVisuals
        val fill = gen.tissueBodyFillMul.coerceIn(0f, 1f)
        val interior = gen.chamberInteriorMul.coerceIn(0f, 1f)
        if (fill < 0.06f) return

        val shellPath = VesselContourBuilder.bodyShellPath(center, width, height, gen)
        val membraneA = 0.72f
        val chamberA = 0.58f
        val archiveA = 0.65f

        scope.clipPath(shellPath) {
            val bx = sin(breath * 0.7f) * width * 0.012f

            // Cranial / upper
            val cranial = Offset(center.x + bx, center.y - height * 0.28f)
            val crownK = (0.55f + gv.crownBloomIntensity * 0.55f).coerceIn(0.4f, 1.15f)
            scope.drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.cortexNode.copy(alpha = 0f),
                        palette.cortexNode.copy(alpha = interior * 0.18f * membraneA * fill * crownK),
                        palette.innerChamberShadow.copy(alpha = interior * 0.12f * chamberA * crownK),
                    ),
                    center = cranial,
                    radius = width * 0.38f,
                ),
                radius = width * 0.42f,
                center = cranial,
            )
            // Central metabolic chamber
            scope.drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.heartCore.copy(alpha = interior * 0.12f * fill * membraneA),
                        palette.shellBase.copy(alpha = interior * 0.08f * chamberA),
                        Color(0xFF000000).copy(alpha = 0f),
                    ),
                    center = Offset(center.x, center.y + height * 0.04f),
                    radius = height * 0.32f,
                ),
                radius = height * 0.38f,
                center = Offset(center.x, center.y + height * 0.02f),
            )
            // Lateral signal chambers
            val latK = (0.5f + (gv.frondBudLengthLeft + gv.frondBudLengthRight) * 0.28f).coerceIn(0.45f, 1.2f)
            val latA = Offset(center.x - width * 0.28f, center.y + height * 0.02f)
            val latB = Offset(center.x + width * 0.28f, center.y + height * 0.02f)
            for (p in listOf(latA, latB)) {
                scope.drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            palette.accentSignal.copy(alpha = interior * 0.14f * fill * membraneA * latK),
                            Color(0xFF000000).copy(alpha = 0f),
                        ),
                        center = p,
                        radius = width * 0.22f,
                    ),
                    radius = width * 0.26f,
                    center = p,
                )
            }
            // Lower archive basin
            val basinK = (0.5f + gv.lowerReservoirDepth * 0.5f).coerceIn(0.45f, 1.15f)
            scope.drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.archiveDeep.copy(alpha = interior * 0.2f * fill * archiveA * basinK),
                        palette.innerChamberShadow.copy(alpha = interior * 0.1f * basinK),
                        Color(0xFF000000).copy(alpha = 0f),
                    ),
                    center = Offset(center.x, center.y + height * 0.32f),
                    radius = height * 0.38f,
                ),
                radius = height * 0.42f,
                center = Offset(center.x, center.y + height * 0.3f),
            )
        }
    }
}
