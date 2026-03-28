package com.velithorne.vessel.physiology

import kotlin.math.PI

/**
 * Central thresholds for telemetry → physiology mapping.
 * Calibrate per device family using field studies; values are deliberately conservative.
 */
data class PhysiologyTuning(
    /** Battery % above this reduces hunger sharply (0..100). */
    val hungerBatterySatietyAbove: Float = 55f,
    /** Battery % below this pushes hunger toward famine (0..100). */
    val hungerBatteryFamineBelow: Float = 20f,
    /**
     * Battery temperature interpretation (°C). Packs vary; widen/tighten per OEM.
     * Idle phones often read ~25–32°C; charging can read warmer.
     */
    val thermalCoolMaxC: Float = 32f,
    val thermalWarmC: Float = 38f,
    val thermalHotC: Float = 42f,
    /** Normalize motion intensity from repository (unitless EMA) to 0..1. */
    val motionIntensityFullScale: Float = 2.5f,
    /** Degrees of combined orientation change per tick considered “max” for mobility. */
    val orientationDeltaFullScaleDeg: Float = 25f,
    /** Treat storage used % above this as structural congestion. */
    val storageHighPct: Float = 88f,
    val storageMidPct: Float = 72f,
    /** Uptime hours at which sleep pressure approaches saturation (asymptotic). */
    val uptimeSaturationHours: Float = 18f,
    /** Smoothing alpha (EMA) per field — higher = faster tracking. */
    val smoothVitality: Float = 0.18f,
    val smoothStress: Float = 0.22f,
    val smoothHunger: Float = 0.2f,
    val smoothFever: Float = 0.16f,
    val smoothRecovery: Float = 0.14f,
    val smoothRespiration: Float = 0.28f,
    val smoothNeural: Float = 0.24f,
    val smoothMobility: Float = 0.3f,
    val smoothSleep: Float = 0.12f,
    val smoothSignal: Float = 0.25f,
    val smoothStructural: Float = 0.18f,
    /** Circadian prior: amplitude of night-bias on sleep pressure (0..1). */
    val circadianNightAmplitude: Float = 0.35f,
) {
    companion object {
        /** Two π for circadian sine. */
        val TAU: Double = 2.0 * PI
    }
}
