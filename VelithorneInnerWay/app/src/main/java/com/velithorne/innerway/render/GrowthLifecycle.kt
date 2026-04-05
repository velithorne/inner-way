package com.velithorne.innerway.render

/**
 * Time-based construction state for substrate structures.
 */
enum class GrowthPhase {
    /** Just created — invisible → emerging */
    FORMING,
    /** Actively extending */
    GROWING,
    /** Slowing, settling */
    STABILIZING,
    /** Fully part of the organism */
    MATURE,
}

fun growthPhaseFromProgress(p: Float): GrowthPhase {
    val x = p.coerceIn(0f, 1f)
    return when {
        x < 0.15f -> GrowthPhase.FORMING
        x < 0.6f -> GrowthPhase.GROWING
        x < 1f -> GrowthPhase.STABILIZING
        else -> GrowthPhase.MATURE
    }
}
