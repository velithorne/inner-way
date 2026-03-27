package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.velithorne.vessel.model.SeedPodVisualState
import com.velithorne.vessel.model.VesselPaletteState

/** Spotlight falloff + inner chamber haze between shell and core. */
object SeedPodGlowPainter {

    fun drawSpotlight(
        scope: DrawScope,
        w: Float,
        h: Float,
        pod: Offset,
        appearance: SeedPodVisualState,
        tuning: SeedPodTuning,
    ) {
        val s = appearance.spotlightStrength.coerceIn(0.2f, 1f)
        val fall = tuning.chamberSpotlightFalloff
        scope.drawOval(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to Color(0xFF1A2838).copy(alpha = 0f),
                    fall to Color(0xFF0A1018).copy(alpha = 0.35f * s),
                    1f to Color(0xFF050508).copy(alpha = 0.55f * s),
                ),
                center = Offset(w * 0.5f, h * 0.35f),
                radius = maxOf(w, h) * 0.65f,
            ),
            topLeft = Offset.Zero,
            size = Size(w, h),
        )
    }

    fun drawInnerChamberHaze(
        scope: DrawScope,
        pod: Offset,
        radii: SeedPodContourBuilder.PodRadii,
        palette: VesselPaletteState,
        appearance: SeedPodVisualState,
        tuning: SeedPodTuning,
    ) {
        val d = (appearance.innerHazeDensity * tuning.innerHazeMax).coerceIn(0f, 1f)
        if (d < 0.05f) return
        scope.drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.gelMedium.copy(alpha = d * 0.22f),
                    palette.innerChamberShadow.copy(alpha = d * 0.18f),
                    Color(0xFF000000).copy(alpha = 0f),
                ),
                center = pod,
                radius = maxOf(radii.innerChamberRx, radii.innerChamberRy) * 1.4f,
            ),
            topLeft = Offset(
                pod.x - radii.innerChamberRx * 1.35f,
                pod.y - radii.innerChamberRy * 1.35f,
            ),
            size = Size(radii.innerChamberRx * 2.7f, radii.innerChamberRy * 2.7f),
        )
    }
}
