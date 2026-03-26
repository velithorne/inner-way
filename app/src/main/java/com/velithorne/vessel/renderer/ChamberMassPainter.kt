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
        val fill = gen.tissueBodyFillMul.coerceIn(0f, 1f)
        val interior = gen.chamberInteriorMul.coerceIn(0f, 1f)
        if (fill < 0.08f) return

        val shellPath = VesselContourBuilder.bodyShellPath(center, width, height, gen)
        val membraneA = 0.72f
        val chamberA = 0.58f
        val archiveA = 0.65f

        scope.clipPath(shellPath) {
            val bx = sin(breath * 0.7f) * width * 0.012f

            // Cranial / upper
            val cranial = Offset(center.x + bx, center.y - height * 0.28f)
            scope.drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.cortexNode.copy(alpha = 0f),
                        palette.cortexNode.copy(alpha = interior * 0.14f * membraneA * fill),
                        palette.innerChamberShadow.copy(alpha = interior * 0.1f * chamberA),
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
            val latA = Offset(center.x - width * 0.28f, center.y + height * 0.02f)
            val latB = Offset(center.x + width * 0.28f, center.y + height * 0.02f)
            for (p in listOf(latA, latB)) {
                scope.drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            palette.accentSignal.copy(alpha = interior * 0.1f * fill * membraneA),
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
            scope.drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.archiveDeep.copy(alpha = interior * 0.16f * fill * archiveA),
                        palette.innerChamberShadow.copy(alpha = interior * 0.08f),
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
