package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.velithorne.vessel.juvenile_form.JuvenileFormState
import com.velithorne.vessel.model.VesselPaletteState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Juvenile body silhouette — stretched beyond pod oval when [JuvenileVisualState.active]. */
object JuvenileContourPainter {

    fun draw(
        scope: DrawScope,
        pod: Offset,
        radii: SeedPodContourBuilder.PodRadii,
        palette: VesselPaletteState,
        form: JuvenileFormState,
        phaseSec: Float,
    ) {
        val v = form.visual
        if (!v.active) return
        val rx = radii.shellRx * v.bodySilhouetteStretchX * (1f + form.expansion.expansionFactor * 0.12f)
        val ry = radii.shellRy * v.bodySilhouetteStretchY * (1f + form.expansion.expansionFactor * 0.1f)
        val path = Path()
        val n = 48
        for (i in 0..n) {
            val t = (i / n.toFloat()) * (2f * PI.toFloat())
            val bulge = sin(phaseSec * 0.4f + t * 2f) * 0.012f * form.traits.contourBodyLikeness
            val px = pod.x + rx * (1f + bulge) * cos(t)
            val py = pod.y + ry * (1f + bulge) * sin(t) + form.expansion.upperBias * ry * 0.35f
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        val a = (0.08f + form.transition.juvenileEmergence * 0.22f).coerceIn(0.06f, 0.32f)
        scope.drawPath(
            path = path,
            color = palette.neuralPathway.copy(alpha = a),
            style = Stroke(width = 2f + form.transition.juvenileEmergence * 2.5f),
        )
        scope.drawPath(
            path = path,
            color = Color.White.copy(alpha = a * 0.2f),
            style = Stroke(width = 1f),
        )
    }
}
