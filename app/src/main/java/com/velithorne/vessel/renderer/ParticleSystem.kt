package com.velithorne.vessel.renderer

import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Fixed-capacity motes; deterministic re-seed from physiology hash for stability across frames.
 * Phase 4+: persisted wind fields; evolution may alter particle chemistry.
 */
class ParticleSystem(
    private val tuning: RenderTuning = RenderTuning(),
) {
    private val px = FloatArray(tuning.particleCount)
    private val py = FloatArray(tuning.particleCount)
    private val vz = FloatArray(tuning.particleCount)
    private val vx = FloatArray(tuning.particleCount)
    private val vy = FloatArray(tuning.particleCount)
    private val phase = FloatArray(tuning.particleCount)
    private var inited = false

    fun ensureInitialized(width: Float, height: Float, seed: Int) {
        if (inited || width <= 1f) return
        val r = Random(seed)
        for (i in px.indices) {
            px[i] = r.nextFloat() * width
            py[i] = r.nextFloat() * height
            vz[i] = 0.2f + r.nextFloat() * 0.8f
            vx[i] = (r.nextFloat() - 0.5f) * tuning.particleBaseSpeed * 0.04f
            vy[i] = (r.nextFloat() - 0.5f) * tuning.particleBaseSpeed * 0.04f
            phase[i] = r.nextFloat() * 6.28f
        }
        inited = true
    }

    fun step(
        width: Float,
        height: Float,
        density: Float,
        neural: Float,
        fever: Float,
        dt: Float,
        globalPhase: Float,
        parallax: Offset,
    ): List<ParticleDraw> {
        if (width <= 1f || height <= 1f || !inited) return emptyList()
        val d = density.coerceIn(0f, 1f)
        val count = (px.size * (0.35f + d * 0.65f)).toInt().coerceAtLeast(8).coerceAtMost(px.size)
        val list = ArrayList<ParticleDraw>(count)
        val speedMul = tuning.particleBaseSpeed * (0.45f + d * 0.9f + neural * 0.25f + fever * 0.2f)
        for (i in 0 until count) {
            val z = vz[i]
            val driftX = cos(globalPhase * 0.7f + phase[i]).toFloat() * 12f * z * d
            val driftY = sin(globalPhase * 0.55f + phase[i]).toFloat() * 10f * z * d
            px[i] += (vx[i] * speedMul + driftX + parallax.x * 0.08f * z) * dt
            py[i] += (vy[i] * speedMul + driftY + parallax.y * 0.08f * z) * dt
            if (px[i] < -20f) px[i] = width + 20f
            if (px[i] > width + 20f) px[i] = -20f
            if (py[i] < -20f) py[i] = height + 20f
            if (py[i] > height + 20f) py[i] = -20f
            val alpha = (0.06f + d * 0.18f + neural * 0.08f) * (0.35f + z * 0.65f)
            val r = (1.2f + z * 2.4f) * (0.85f + fever * 0.35f)
            list.add(
                ParticleDraw(
                    position = Offset(px[i], py[i]),
                    radius = r,
                    alpha = alpha.coerceIn(0.03f, 0.45f),
                    depth = z,
                ),
            )
        }
        return list
    }
}

data class ParticleDraw(
    val position: Offset,
    val radius: Float,
    val alpha: Float,
    val depth: Float,
)
