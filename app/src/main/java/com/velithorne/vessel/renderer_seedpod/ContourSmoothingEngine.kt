package com.velithorne.vessel.renderer_seedpod

import kotlin.math.abs

/**
 * Laplacian-style smoothing on a closed ring; [hardEdgePreserve] 0..1 reduces smoothing at scars/plates.
 */
object ContourSmoothingEngine {

    fun smoothClosed(
        radii: FloatArray,
        passes: Int,
        hardEdgePreserve: FloatArray?,
    ): FloatArray {
        if (radii.isEmpty()) return radii
        val n = radii.size
        var cur = radii.copyOf()
        val preserve = hardEdgePreserve?.takeIf { it.size == n }
        repeat(passes.coerceIn(1, 4)) {
            val next = FloatArray(n)
            for (i in 0 until n) {
                val prev = cur[(i + n - 1) % n]
                val nextV = cur[(i + 1) % n]
                val blended = (prev + cur[i] * 2f + nextV) * 0.25f
                val p = preserve?.get(i)?.coerceIn(0f, 1f) ?: 0f
                next[i] = blended * (1f - p * 0.85f) + cur[i] * (p * 0.85f)
            }
            cur = next
        }
        return cur
    }
}
