package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.velithorne.vessel.model.GeneratedAnatomyState
import com.velithorne.vessel.model.VisibleMorphologyState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Primary visible silhouette — polar-sampled outline driven by [GeneratedAnatomyState.silhouettePolarMul].
 */
object GeneratedContourPainter {

    private const val N = 12

    fun draw(
        scope: DrawScope,
        pod: Offset,
        base: SeedPodContourBuilder.PodRadii,
        anatomy: GeneratedAnatomyState?,
        visible: VisibleMorphologyState,
        paletteLine: Color,
        phaseSec: Float,
    ) {
        val g = visible.generatedTopologyInfluence.coerceIn(0f, 1f)
        if (g < 0.04f || anatomy == null) return
        val mul = anatomy.silhouettePolarMul.takeIf { it.size == N } ?: FloatArray(N) { 1f }
        val rx = base.shellRx
        val ry = base.shellRy
        val path = Path()
        for (i in 0 until N) {
            val t = (i / N.toFloat()) * (2f * PI.toFloat())
            val lerpMul = 1f + (mul[i] - 1f) * g
            val wobble = sin(phaseSec * 0.7f + i * 0.5f) * 0.012f * g
            val px = pod.x + rx * (lerpMul + wobble) * cos(t)
            val py = pod.y + ry * (lerpMul + wobble) * sin(t)
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        val alpha = (0.14f + g * 0.38f).coerceIn(0.1f, 0.55f)
        scope.drawPath(
            path = path,
            color = paletteLine.copy(alpha = alpha),
            style = Stroke(width = 2.2f + g * 2.5f),
        )
        scope.drawPath(
            path = path,
            color = Color.White.copy(alpha = alpha * 0.18f),
            style = Stroke(width = 1f),
        )
    }
}
