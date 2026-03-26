package com.velithorne.vessel.physiology

/**
 * Immediate, unsmoothed scalars produced from a single [com.velithorne.vessel.telemetry.TelemetrySnapshot].
 * Useful for debugging formulas and unit tests; [PhysiologyEngine] smooths downstream species fields.
 */
data class PhysiologyInputs(
    val batteryNorm: Float,
    val hungerRaw: Float,
    val feverRaw: Float,
    val thermalStrain: Float,
    val memoryStrain: Float,
    val storageStrain: Float,
    val networkRespirationRaw: Float,
    val motionNorm: Float,
    val orientationDeltaNorm: Float,
    val uptimeHours: Float,
    val circadianPhase01: Float,
)
