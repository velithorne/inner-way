package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.velithorne.vessel.juvenile_form.JuvenileFormState
import com.velithorne.vessel.model.VesselPaletteState
import kotlin.math.max

object JuvenilePlatePainter {

    fun draw(
        scope: DrawScope,
        pod: Offset,
        radii: SeedPodContourBuilder.PodRadii,
        palette: VesselPaletteState,
        form: JuvenileFormState,
        phaseSec: Float,
    ) {
        if (!form.active || form.bodyPlan.shellMass < 0.45f) return
        val sm = form.bodyPlan.shellMass * form.transition.juvenileEmergence
        val rx = radii.shellRx * form.visual.shellForwardMul
        val ry = radii.shellRy * form.visual.shellForwardMul
        val c = pod
        scope.drawArc(
            color = palette.shellEdge.copy(alpha = 0.12f + sm * 0.2f),
            startAngle = -160f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = Offset(c.x - rx, c.y - ry),
            size = Size(rx * 2f, ry * 2f),
            style = Stroke(2.5f + sm * 3f),
        )
        scope.drawArc(
            color = palette.thermalEdge.copy(alpha = 0.08f + sm * 0.12f),
            startAngle = 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(c.x - rx * 0.95f, c.y - ry * 0.95f),
            size = Size(rx * 1.9f, ry * 1.9f),
            style = Stroke(1.5f + sm * 2f),
        )
    }
}
