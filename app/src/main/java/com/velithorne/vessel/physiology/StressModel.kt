package com.velithorne.vessel.physiology

/**
 * Combines real resource strains into a single stress scalar (pre-smoothing).
 * Evolution engine (later) may use this as fitness pressure input.
 */
object StressModel {

    fun aggregate(
        thermalStrain: Float,
        memoryStrain: Float,
        storageStrain: Float,
        hunger: Float,
        energyDeficit: Float,
        motionNorm: Float,
        meteredStrain: Float,
    ): Float {
        val terms = listOf(
            thermalStrain * 0.22f,
            memoryStrain * 0.22f,
            storageStrain * 0.14f,
            hunger * 0.12f,
            energyDeficit * 0.14f,
            motionNorm * 0.1f,
            meteredStrain * 0.06f,
        )
        return clamp01(terms.sum())
    }

    private fun clamp01(v: Float): Float = v.coerceIn(0f, 1f)
}
