package com.velithorne.vessel.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import kotlin.math.sin

/**
 * Synthetic shell outline: crown, thoracic chamber, core waist, tapered reservoir tail.
 */
object VesselContourBuilder {

    fun bodyShellPath(
        center: Offset,
        width: Float,
        height: Float,
        gen: GeneratedAnatomyParams = GeneratedAnatomyParams.identity,
    ): Path {
        val hw = width * 0.5f * gen.thoraxWidthMul * (1f + gen.crownWidthMul * 0.15f - 0.15f)
        val h = height * gen.tailLengthMul
        val ax = gen.asymmetryX * width * 0.08f
        val cShift = Offset(center.x + ax, center.y - gen.thermalBulge * height * 0.02f)
        val path = Path()
        val c = cShift

        val crownW = hw * (0.92f + gen.crownWidthMul * 0.08f)
        val apex = Offset(c.x, c.y - h * 0.48f)
        val shoulderR = Offset(c.x + crownW * 0.48f, c.y - h * 0.1f)
        val flankR = Offset(c.x + hw * 0.52f, c.y + h * 0.06f)
        val hipR = Offset(c.x + hw * 0.34f, c.y + h * 0.36f)
        val tailR = Offset(c.x + hw * 0.12f, c.y + h * 0.5f)
        val tailL = Offset(c.x - hw * 0.12f, c.y + h * 0.5f)
        val hipL = Offset(c.x - hw * 0.34f, c.y + h * 0.36f)
        val flankL = Offset(c.x - hw * 0.52f, c.y + h * 0.06f)
        val shoulderL = Offset(c.x - crownW * 0.48f, c.y - h * 0.1f)

        path.moveTo(apex.x, apex.y)
        // Crown facet → right shoulder (engineered widening)
        path.cubicTo(
            c.x + crownW * 0.26f, c.y - h * 0.38f,
            c.x + crownW * 0.4f, c.y - h * 0.22f,
            shoulderR.x, shoulderR.y,
        )
        path.cubicTo(
            c.x + hw * 0.55f, c.y + h * 0.02f,
            flankR.x, flankR.y + h * 0.02f,
            hipR.x, hipR.y,
        )
        path.quadraticTo(c.x + hw * 0.05f, c.y + h * 0.52f, tailR.x, tailR.y)
        path.quadraticTo(c.x, c.y + h * 0.56f, tailL.x, tailL.y)
        path.quadraticTo(c.x - hw * 0.05f, c.y + h * 0.52f, hipL.x, hipL.y)
        path.cubicTo(
            flankL.x, flankL.y + h * 0.02f,
            c.x - hw * 0.55f, c.y + h * 0.02f,
            shoulderL.x, shoulderL.y,
        )
        path.cubicTo(
            c.x - crownW * 0.4f, c.y - h * 0.22f,
            c.x - crownW * 0.26f, c.y - h * 0.38f,
            apex.x, apex.y,
        )
        path.close()
        return path
    }

    fun rearSilhouettePath(
        center: Offset,
        width: Float,
        height: Float,
        microBreathe: Float,
        gen: GeneratedAnatomyParams = GeneratedAnatomyParams.identity,
    ): Path {
        val k = 1f + sin(microBreathe) * 0.012f
        return bodyShellPath(center, width * k * 0.96f, height * k * 0.97f, gen)
    }
}
