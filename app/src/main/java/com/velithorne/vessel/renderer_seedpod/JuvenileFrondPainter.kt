package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.velithorne.vessel.juvenile_form.JuvenileFormState
import com.velithorne.vessel.model.VesselPaletteState
import kotlin.math.max
import kotlin.math.sin

object JuvenileFrondPainter {

    fun draw(
        scope: DrawScope,
        pod: Offset,
        radii: SeedPodContourBuilder.PodRadii,
        palette: VesselPaletteState,
        form: JuvenileFormState,
        phaseSec: Float,
    ) {
        if (!form.active || form.bodyPlan.lateralMass < 0.45f) return
        val span = max(radii.shellRx, radii.bandMidRx) * form.visual.lateralSpanMul * 0.95f
        val lm = form.bodyPlan.lateralMass * form.transition.juvenileEmergence
        for (side in listOf(-1f, 1f)) {
            val path = Path()
            val x0 = pod.x + side * span * 0.92f
            val y0 = pod.y + span * 0.04f
            val x1 = pod.x + side * span * 1.35f
            val y1 = pod.y - span * 0.12f + sin((phaseSec * 0.5f + side).toDouble()).toFloat() * span * 0.03f
            path.moveTo(x0, y0)
            path.quadraticTo(pod.x + side * span * 1.1f, pod.y - span * 0.08f, x1, y1)
            scope.drawPath(
                path = path,
                brush = Brush.linearGradient(
                    colors = listOf(
                        palette.accentSignal.copy(alpha = 0.12f + lm * 0.35f),
                        palette.lungFrond.copy(alpha = 0.06f + lm * 0.2f),
                    ),
                    start = Offset(x0, y0),
                    end = Offset(x1, y1),
                ),
                style = Stroke(width = 3f + lm * 4f, cap = StrokeCap.Round),
            )
        }
    }
}
