package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Closed Catmull–Rom style smooth loop from polar radius samples.
 */
object ContourSplineBuilder {

    fun buildClosedLoop(
        center: Offset,
        rx: Float,
        ry: Float,
        radiusMul: FloatArray,
        phaseSec: Float,
        wobbleAmp: Float,
    ): Path {
        val path = Path()
        val n = radiusMul.size
        if (n < 3) return path
        val pts = Array(n) { i ->
            val t = (i / n.toFloat()) * (2f * PI.toFloat())
            val m = radiusMul[i]
            val w = sin(phaseSec * 0.6f + i * 0.4f) * wobbleAmp
            Offset(
                center.x + rx * (m + w) * cos(t),
                center.y + ry * (m + w) * sin(t),
            )
        }
        val first = pts[0]
        path.moveTo(first.x, first.y)
        for (i in 0 until n) {
            val p0 = pts[(i + n - 1) % n]
            val p1 = pts[i]
            val p2 = pts[(i + 1) % n]
            val p3 = pts[(i + 2) % n]
            val c1x = p1.x + (p2.x - p0.x) / 6f
            val c1y = p1.y + (p2.y - p0.y) / 6f
            val c2x = p2.x - (p3.x - p1.x) / 6f
            val c2y = p2.y - (p3.y - p1.y) / 6f
            path.cubicTo(c1x, c1y, c2x, c2y, p2.x, p2.y)
        }
        path.close()
        return path
    }
}
