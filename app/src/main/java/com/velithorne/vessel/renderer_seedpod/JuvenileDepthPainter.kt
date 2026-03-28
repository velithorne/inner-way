package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.velithorne.vessel.juvenile_form.JuvenileFormState
import com.velithorne.vessel.model.SeedPodDepthState
import com.velithorne.vessel.model.VesselPaletteState
import kotlin.math.max

object JuvenileDepthPainter {

    fun draw(
        scope: DrawScope,
        pod: Offset,
        radii: SeedPodContourBuilder.PodRadii,
        palette: VesselPaletteState,
        depth: SeedPodDepthState,
        form: JuvenileFormState,
    ) {
        if (!form.active) return
        val rx = max(radii.shellRx, radii.shellRy)
        val veil = (0.08f + form.regions.crownDepth * 0.06f) * (1f - depth.rearDarkening * 0.3f)
        scope.drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF02060C).copy(alpha = veil * form.transition.juvenileEmergence),
                    Color(0xFF000000).copy(alpha = 0f),
                ),
                center = Offset(pod.x, pod.y + rx * 0.15f),
                radius = rx * 1.2f,
            ),
            topLeft = Offset(pod.x - rx * 1.1f, pod.y - rx * 0.9f),
            Size(rx * 2.2f, rx * 1.8f),
        )
    }
}
