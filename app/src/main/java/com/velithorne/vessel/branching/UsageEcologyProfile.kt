package com.velithorne.vessel.branching

import com.velithorne.vessel.lineage.AdaptationKind
import com.velithorne.vessel.lineage.AdaptationMarker

/**
 * Long-term ecology derived from adaptation markers + simple heuristics.
 */
fun blendEcology(a: UsageEcologyProfile, b: UsageEcologyProfile, t: Float): UsageEcologyProfile {
    fun m(x: Float, y: Float) = x + (y - x) * t
    return UsageEcologyProfile(
        mobileDataHeavy = m(a.mobileDataHeavy, b.mobileDataHeavy),
        wifiHeavy = m(a.wifiHeavy, b.wifiHeavy),
        thermalStrainHistory = m(a.thermalStrainHistory, b.thermalStrainHistory),
        neuralLoadHistory = m(a.neuralLoadHistory, b.neuralLoadHistory),
        signalPressureHistory = m(a.signalPressureHistory, b.signalPressureHistory),
        reserveStressHistory = m(a.reserveStressHistory, b.reserveStressHistory),
        archiveBurdenHistory = m(a.archiveBurdenHistory, b.archiveBurdenHistory),
        recoveryStability = m(a.recoveryStability, b.recoveryStability),
    )
}

data class UsageEcologyProfile(
    val mobileDataHeavy: Float,
    val wifiHeavy: Float,
    val thermalStrainHistory: Float,
    val neuralLoadHistory: Float,
    val signalPressureHistory: Float,
    val reserveStressHistory: Float,
    val archiveBurdenHistory: Float,
    val recoveryStability: Float,
)

object UsageEcologyProfileBuilder {
    fun fromMarkers(markers: List<AdaptationMarker>): UsageEcologyProfile {
        fun k(kind: AdaptationKind) = markers.find { it.kind == kind }?.accumulatedIntensity ?: 0f
        val thermal = k(AdaptationKind.THERMAL)
        val signal = k(AdaptationKind.SIGNAL)
        val neural = k(AdaptationKind.NEURAL)
        val reserve = k(AdaptationKind.RESERVE)
        val archive = k(AdaptationKind.ARCHIVE)
        val recovery = k(AdaptationKind.RECOVERY)
        val sum = (thermal + signal + neural + reserve + archive + recovery).coerceAtLeast(0.01f)
        return UsageEcologyProfile(
            mobileDataHeavy = (signal * 0.6f).coerceIn(0f, 1f),
            wifiHeavy = ((1f - signal * 0.3f) * 0.4f).coerceIn(0f, 1f),
            thermalStrainHistory = (thermal / sum).coerceIn(0f, 1f),
            neuralLoadHistory = (neural / sum).coerceIn(0f, 1f),
            signalPressureHistory = (signal / sum).coerceIn(0f, 1f),
            reserveStressHistory = (reserve / sum).coerceIn(0f, 1f),
            archiveBurdenHistory = (archive / sum).coerceIn(0f, 1f),
            recoveryStability = (recovery / sum).coerceIn(0f, 1f),
        )
    }
}
