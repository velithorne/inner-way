package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.velithorne.vessel.juvenile_form.JuvenileFormState
import com.velithorne.vessel.model.VesselPaletteState
import kotlin.math.max

object JuvenileBasinPainter {

    fun draw(
        scope: DrawScope,
        pod: Offset,
        radii: SeedPodContourBuilder.PodRadii,
        palette: VesselPaletteState,
        form: JuvenileFormState,
    ) {
        if (!form.active || form.bodyPlan.reserveMass < 0.42f) return
        val r = max(radii.shellRx, radii.shellRy) * form.visual.reserveDropMul
        val w = form.bodyPlan.reserveMass * form.transition.juvenileEmergence
        val c = Offset(pod.x, pod.y + r * 0.55f)
        scope.drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.archiveDeep.copy(alpha = 0.15f + w * 0.35f),
                    palette.innerChamberShadow.copy(alpha = 0.12f + w * 0.2f),
                    Color(0xFF000000).copy(alpha = 0f),
                ),
                center = c,
                radius = r * 0.55f,
            ),
            topLeft = Offset(c.x - r * 0.55f, c.y - r * 0.35f),
            size = Size(r * 1.1f, r * 0.75f),
        )
    }
}
