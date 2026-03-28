package com.velithorne.vessel.morphogenesis

import com.velithorne.vessel.util.Smoothing

/**
 * EMA-smoothed pressures for slow structural memory.
 */
data class PressureAccumulator(
    val thermal: Float = 0f,
    val reserve: Float = 0f,
    val hunger: Float = 0f,
    val signal: Float = 0f,
    val archive: Float = 0f,
    val motion: Float = 0f,
    val neural: Float = 0f,
    val recovery: Float = 0f,
    val sleep: Float = 0f,
    val attachment: Float = 0f,
) {
    fun step(raw: GrowthPressure, alpha: Float): PressureAccumulator = PressureAccumulator(
        thermal = Smoothing.lerp(thermal, raw.thermalPressure, alpha),
        reserve = Smoothing.lerp(reserve, raw.reservePressure, alpha),
        hunger = Smoothing.lerp(hunger, raw.hungerPressure, alpha),
        signal = Smoothing.lerp(signal, raw.signalPressure, alpha),
        archive = Smoothing.lerp(archive, raw.archivePressure, alpha),
        motion = Smoothing.lerp(motion, raw.motionPressure, alpha),
        neural = Smoothing.lerp(neural, raw.neuralPressure, alpha),
        recovery = Smoothing.lerp(recovery, raw.recoveryPressure, alpha),
        sleep = Smoothing.lerp(sleep, raw.sleepPressure, alpha),
        attachment = Smoothing.lerp(attachment, raw.environmentalAttachmentPressure, alpha),
    )
}
