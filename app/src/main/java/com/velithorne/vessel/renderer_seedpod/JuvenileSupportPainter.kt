package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.velithorne.vessel.juvenile_form.JuvenileFormState
import com.velithorne.vessel.model.VesselPaletteState
import kotlin.math.max

object JuvenileSupportPainter {

    fun draw(
        scope: DrawScope,
        pod: Offset,
        radii: SeedPodContourBuilder.PodRadii,
        palette: VesselPaletteState,
        form: JuvenileFormState,
        phaseSec: Float,
    ) {
        if (!form.active || form.bodyPlan.supportMass < 0.42f) return
        val rx = max(radii.shellRx, radii.shellRy) * form.visual.supportCrossMul
        val w = form.bodyPlan.supportMass * form.transition.juvenileEmergence
        val a = 0.1f + w * 0.25f
        val sway = kotlin.math.sin(phaseSec * 0.6).toFloat() * rx * 0.02f
        scope.drawLine(
            color = palette.recoverySheen.copy(alpha = a),
            start = Offset(pod.x - rx * 0.85f + sway, pod.y - rx * 0.35f),
            end = Offset(pod.x + rx * 0.85f - sway, pod.y + rx * 0.45f),
            strokeWidth = 2f + w * 2.5f,
        )
        scope.drawLine(
            color = Color(0xFF8899AA).copy(alpha = a * 0.65f),
            start = Offset(pod.x - rx * 0.45f, pod.y + rx * 0.55f),
            end = Offset(pod.x + rx * 0.5f, pod.y - rx * 0.5f),
            strokeWidth = 1.2f + w * 1.5f,
        )
    }
}
