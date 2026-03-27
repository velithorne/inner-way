package com.velithorne.vessel.branching

import com.velithorne.vessel.lineage.AdaptationMarker
import com.velithorne.vessel.telemetry.TelemetrySnapshot

/**
 * Target affinity vector from device profile + usage ecology.
 */
object BranchInfluenceModel {

    fun targetAffinity(
        device: DeviceProfile,
        ecology: UsageEcologyProfile,
        tuning: BranchingTuning,
    ): BranchAffinity {
        val wDev = tuning.deviceVsAdaptationWeight
        val wEco = 1f - wDev
        fun blend(a: Float, b: Float) = a * wDev + b * wEco

        val thermal = blend(
            device.lowThermalHeadroom * 0.7f + device.stableChargingProfile * 0.15f,
            ecology.thermalStrainHistory * 0.85f + (1f - ecology.recoveryStability) * 0.15f,
        )
        val signal = blend(
            device.highSignalDependency * 0.75f + device.stableChargingProfile * 0.05f,
            ecology.signalPressureHistory * 0.7f + ecology.mobileDataHeavy * 0.3f,
        )
        val crown = blend(
            device.highSensorRichness * 0.45f + (1f - device.lowMemoryPressureTolerance) * 0.25f,
            ecology.neuralLoadHistory * 0.85f,
        )
        val reserve = blend(
            device.stableChargingProfile * 0.4f + (1f - device.storageDenseProfile) * 0.2f,
            ecology.reserveStressHistory * 0.8f,
        )
        val archive = blend(
            device.storageDenseProfile * 0.85f + device.lowMemoryPressureTolerance * 0.15f,
            ecology.archiveBurdenHistory * 0.9f,
        )
        val motion = blend(
            device.highMotionLife * 0.9f,
            (ecology.neuralLoadHistory * 0.2f + (1f - ecology.recoveryStability) * 0.15f),
        )
        val balanced = 0.18f + (1f - maxOf(thermal, signal, crown, reserve, archive, motion)) * 0.25f
        return BranchAffinity(
            thermalShell = thermal.coerceIn(0.02f, 1f),
            signalFrond = signal.coerceIn(0.02f, 1f),
            crownNeural = crown.coerceIn(0.02f, 1f),
            reserveBasin = reserve.coerceIn(0.02f, 1f),
            archiveCore = archive.coerceIn(0.02f, 1f),
            motionBraced = motion.coerceIn(0.02f, 1f),
            balanced = balanced.coerceIn(0.02f, 1f),
        ).normalized()
    }

    fun fromTelemetryForStep(
        telemetry: TelemetrySnapshot,
        adaptationMarkers: List<AdaptationMarker>,
    ): Pair<DeviceProfile, UsageEcologyProfile> {
        val dev = DeviceProfileAnalyzer.analyze(telemetry)
        val instant = UsageEcologyProfile(
            mobileDataHeavy = if (telemetry.networkType == com.velithorne.vessel.telemetry.NetworkTransport.CELLULAR) 0.5f else 0.2f,
            wifiHeavy = if (telemetry.networkType == com.velithorne.vessel.telemetry.NetworkTransport.WIFI) 0.5f else 0.2f,
            thermalStrainHistory = 0.35f,
            neuralLoadHistory = 0.35f,
            signalPressureHistory = 0.35f,
            reserveStressHistory = 0.35f,
            archiveBurdenHistory = 0.35f,
            recoveryStability = 0.35f,
        )
        if (adaptationMarkers.isEmpty()) return dev to instant
        val fromMarkers = UsageEcologyProfileBuilder.fromMarkers(adaptationMarkers)
        val ecology = blendEcology(instant, fromMarkers, 0.65f)
        return dev to ecology
    }
}
