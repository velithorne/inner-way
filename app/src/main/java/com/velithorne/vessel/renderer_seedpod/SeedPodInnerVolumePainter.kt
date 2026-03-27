package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.velithorne.vessel.model.BranchVisualState
import com.velithorne.vessel.model.GeneratedAnatomyState
import com.velithorne.vessel.model.SeedPodDepthState
import com.velithorne.vessel.model.SeedPodVisualState
import com.velithorne.vessel.model.VesselPaletteState
import kotlin.math.max

/** Inner chamber volume falloff — rear haze + mid chamber glow between shell and core. */
object SeedPodInnerVolumePainter {

    fun draw(
        scope: DrawScope,
        pod: Offset,
        layerOffset: Offset,
        radii: SeedPodContourBuilder.PodRadii,
        palette: VesselPaletteState,
        appearance: SeedPodVisualState,
        depth: SeedPodDepthState,
        tuning: SeedPodTuning,
        branch: BranchVisualState = BranchVisualState.neutral(),
        anatomy: GeneratedAnatomyState? = null,
        generatedInfluence: Float = 0f,
    ) {
        val g = generatedInfluence.coerceIn(0f, 1f)
        val offX = (anatomy?.innerChamberOffsetNx ?: 0f) * radii.innerChamberRx * 1.8f * g
        val offY = (anatomy?.innerChamberOffsetNy ?: 0f) * radii.innerChamberRy * 1.8f * g
        val c = pod + layerOffset + Offset(offX, offY)
        val focus = branch.innerVolumeFocusY.coerceIn(-0.55f, 0.55f)
        val mag = branch.visualExpressionMagnitude.coerceIn(0f, 1f)
        val d = (appearance.innerHazeDensity * tuning.innerHazeMax * tuning.depth.innerVolumeFalloffMul * depth.innerVolumeExpand).coerceIn(0f, 1f)
        if (d < 0.04f) return
        // Rear inner haze (cooler, deeper)
        scope.drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.innerChamberShadow.copy(alpha = d * 0.28f * (1f - g * 0.35f)),
                    palette.gelMedium.copy(alpha = d * 0.18f),
                    Color(0xFF000000).copy(alpha = 0f),
                ),
                center = Offset(c.x, c.y + radii.innerChamberRy * (0.08f - focus * 0.12f * mag)),
                radius = maxOf(radii.innerChamberRx, radii.innerChamberRy) * 1.25f,
            ),
            topLeft = Offset(
                c.x - radii.innerChamberRx * 1.3f,
                c.y - radii.innerChamberRy * 1.25f,
            ),
            size = Size(radii.innerChamberRx * 2.6f, radii.innerChamberRy * 2.5f),
        )
        // Mid chamber lift (neural / vitality)
        scope.drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.neuralPathway.copy(alpha = d * 0.08f),
                    Color(0xFF000000).copy(alpha = 0f),
                ),
                center = Offset(c.x, c.y - radii.shellRy * (0.15f + focus * 0.22f * mag)),
                radius = maxOf(radii.shellRx, radii.shellRy) * 0.55f,
            ),
            topLeft = Offset(c.x - radii.shellRx * 0.6f, c.y - radii.shellRy * 0.65f),
            size = Size(radii.shellRx * 1.2f, radii.shellRy * 1.1f),
        )
    }
}
