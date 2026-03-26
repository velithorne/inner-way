package com.velithorne.vessel.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import kotlin.math.sin

/**
 * Adult spindle vs compact **seed** vesica; [GeneratedAnatomyParams.seedFormBlend] morphs between them.
 */
object VesselContourBuilder {

    fun bodyShellPath(
        center: Offset,
        width: Float,
        height: Float,
        gen: GeneratedAnatomyParams = GeneratedAnatomyParams.identity,
    ): Path {
        val t = gen.seedFormBlend.coerceIn(0f, 1f)
        return if (t > 0.35f) {
            seedVesicaPath(center, width, height, gen, t)
        } else {
            adultSpindlePath(center, width, height, gen)
        }
    }

    private fun adultSpindlePath(center: Offset, width: Float, height: Float, gen: GeneratedAnatomyParams): Path {
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

    /**
     * Compact silicon **seed**: short vesica / double lobe (distinct from adult spindle).
     */
    private fun seedVesicaPath(
        center: Offset,
        width: Float,
        height: Float,
        gen: GeneratedAnatomyParams,
        seedStrength: Float,
    ): Path {
        val ax = gen.asymmetryX * width * 0.05f
        val c = Offset(center.x + ax, center.y - gen.thermalBulge * height * 0.012f)
        val compact = 0.55f + (1f - seedStrength.coerceIn(0f, 1f)) * 0.12f
        val hw = width * 0.24f * compact * gen.thoraxWidthMul.coerceIn(0.9f, 1.1f)
        val hh = height * 0.34f * compact * gen.tailLengthMul.coerceIn(0.88f, 1.05f)
        val path = Path()
        val top = Offset(c.x, c.y - hh * 0.92f)
        val bottom = Offset(c.x, c.y + hh * 0.88f)
        val midR = Offset(c.x + hw * 0.92f, c.y + hh * 0.02f)
        val midL = Offset(c.x - hw * 0.92f, c.y + hh * 0.02f)
        path.moveTo(top.x, top.y)
        path.cubicTo(
            c.x + hw * 0.5f, c.y - hh * 0.38f,
            c.x + hw * 0.88f, c.y - hh * 0.08f,
            midR.x, midR.y,
        )
        path.cubicTo(
            c.x + hw * 0.42f, c.y + hh * 0.42f,
            c.x + hw * 0.18f, c.y + hh * 0.78f,
            bottom.x, bottom.y,
        )
        path.cubicTo(
            c.x - hw * 0.18f, c.y + hh * 0.78f,
            c.x - hw * 0.42f, c.y + hh * 0.42f,
            midL.x, midL.y,
        )
        path.cubicTo(
            c.x - hw * 0.88f, c.y - hh * 0.08f,
            c.x - hw * 0.5f, c.y - hh * 0.38f,
            top.x, top.y,
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
