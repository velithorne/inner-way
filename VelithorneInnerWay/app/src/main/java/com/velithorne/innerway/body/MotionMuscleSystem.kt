package com.velithorne.innerway.body

/**
 * Motion / muscle: Phase 2 attaches SensorManager listeners; Phase 1 returns idle baseline.
 */
class MotionMuscleSystem {

    data class Sample(
        val motionIntensity: Float,
        val timestampMillis: Long,
    )

    suspend fun sample(): Sample {
        return Sample(motionIntensity = 0f, timestampMillis = System.currentTimeMillis())
    }
}
