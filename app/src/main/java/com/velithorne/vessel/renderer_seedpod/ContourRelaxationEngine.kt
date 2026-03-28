package com.velithorne.vessel.renderer_seedpod

/**
 * Light tension relaxation — pulls extreme spikes toward neighbors without full blobbing.
 */
object ContourRelaxationEngine {

    fun relax(
        radii: FloatArray,
        iterations: Int,
        strength: Float,
    ): FloatArray {
        if (radii.isEmpty()) return radii
        val n = radii.size
        var cur = radii.copyOf()
        val s = strength.coerceIn(0.05f, 0.45f)
        repeat(iterations.coerceIn(1, 3)) {
            val next = FloatArray(n)
            for (i in 0 until n) {
                val prev = cur[(i + n - 1) % n]
                val nextV = cur[(i + 1) % n]
                val target = (prev + nextV) * 0.5f
                next[i] = cur[i] + (target - cur[i]) * s
            }
            cur = next
        }
        return cur
    }
}
