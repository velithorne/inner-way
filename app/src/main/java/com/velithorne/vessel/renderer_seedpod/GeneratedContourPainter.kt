package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.velithorne.vessel.model.ContourGeometryState
import com.velithorne.vessel.model.GeneratedAnatomyState
import com.velithorne.vessel.model.VisibleMorphologyState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Primary visible silhouette — high sample count, smoothing, Catmull–Rom spline closure.
 */
object GeneratedContourPainter {

    fun draw(
        scope: DrawScope,
        pod: Offset,
        base: SeedPodContourBuilder.PodRadii,
        anatomy: GeneratedAnatomyState?,
        visible: VisibleMorphologyState,
        geometry: ContourGeometryState,
        paletteLine: Color,
        phaseSec: Float,
    ) {
        val g = visible.generatedTopologyInfluence.coerceIn(0f, 1f)
        if (g < 0.04f || anatomy == null) return
        val raw = anatomy.silhouettePolarMul
        val hard = anatomy.contourHardEdgePreserve.takeIf { it.size == raw.size }
        val targetN = geometry.sampleCount.coerceIn(ContourSampleSet.MINIMUM, ContourSampleSet.COMPLEX)
        val resampled = if (raw.size == targetN) raw.copyOf() else resamplePolar(raw, targetN)
        val preserve = if (hard != null && hard.size == raw.size && raw.size == targetN) {
            hard.copyOf()
        } else if (hard != null) {
            resamplePolar(hard, targetN)
        } else {
            FloatArray(targetN) { 0f }
        }
        var mul = ContourSmoothingEngine.smoothClosed(
            radii = resampled,
            passes = geometry.smoothingPasses,
            hardEdgePreserve = preserve,
        )
        mul = ContourRelaxationEngine.relax(
            radii = mul,
            iterations = geometry.relaxationIterations,
            strength = 0.12f + g * 0.1f,
        )
        val wobble = 0.006f + g * 0.008f
        val path = if (geometry.splineEnabled) {
            ContourSplineBuilder.buildClosedLoop(
                center = pod,
                rx = base.shellRx,
                ry = base.shellRy,
                radiusMul = mul,
                phaseSec = phaseSec,
                wobbleAmp = wobble * g,
            )
        } else {
            buildPolyPath(pod, base.shellRx, base.shellRy, mul, targetN, phaseSec, wobble * g)
        }
        val alpha = (0.18f + g * 0.42f).coerceIn(0.12f, 0.62f)
        scope.drawPath(
            path = path,
            color = paletteLine.copy(alpha = alpha),
            style = Stroke(width = 2.4f + g * 2.8f),
        )
        scope.drawPath(
            path = path,
            color = Color.White.copy(alpha = alpha * 0.22f),
            style = Stroke(width = 1.1f),
        )
    }

    private fun resamplePolar(src: FloatArray, n: Int): FloatArray {
        if (src.isEmpty()) return FloatArray(n) { 1f }
        val out = FloatArray(n)
        val sn = src.size
        for (i in 0 until n) {
            val pos = (i / n.toFloat()) * sn
            val i0 = kotlin.math.floor(pos.toDouble()).toInt() % sn
            val i1 = (i0 + 1) % sn
            val t = (pos - kotlin.math.floor(pos.toDouble())).toFloat()
            out[i] = src[i0] * (1f - t) + src[i1] * t
        }
        return out
    }

    private fun buildPolyPath(
        pod: Offset,
        rx: Float,
        ry: Float,
        mul: FloatArray,
        n: Int,
        phaseSec: Float,
        wobble: Float,
    ): Path {
        val path = Path()
        for (i in 0 until n) {
            val t = (i / n.toFloat()) * (2f * PI.toFloat())
            val m = mul[i]
            val w = sin(phaseSec * 0.7f + i * 0.5f) * wobble
            val px = pod.x + rx * (m + w) * cos(t)
            val py = pod.y + ry * (m + w) * sin(t)
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        return path
    }
}
