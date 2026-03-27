package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.velithorne.vessel.model.SeedPodVisualState
import com.velithorne.vessel.model.VesselPaletteState
import kotlin.math.sin

/** Animated ring where tissue is accreting — subtle, stage-aware. */
object SeedPodGrowthFrontPainter {

    fun draw(
        scope: DrawScope,
        pod: Offset,
        radii: SeedPodContourBuilder.PodRadii,
        palette: VesselPaletteState,
        appearance: SeedPodVisualState,
        phaseSec: Float,
        tuning: SeedPodTuning,
    ) {
        val a = (appearance.growthFrontAlpha * tuning.growthFrontAlphaMax).coerceIn(0f, 1f)
        if (a < 0.04f) return
        val shim = sin(phaseSec * 2.2f).toFloat() * 0.5f + 0.5f
        val rx = radii.shellRx * (1.0f + 0.06f * shim)
        val ry = radii.shellRy * (1.0f + 0.06f * shim)
        scope.drawOval(
            color = palette.accentSignal.copy(alpha = a * (0.12f + shim * 0.18f)),
            topLeft = Offset(pod.x - rx, pod.y - ry),
            size = androidx.compose.ui.geometry.Size(rx * 2f, ry * 2f),
            style = Stroke(width = 1.2f + a * 4f),
        )
    }
}
