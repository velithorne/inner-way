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
            device.lowThermalHeadroom * 0.55f + device.stableChargingProfile * 0.12f + device.batteryThermalTrend * 0.28f,
            ecology.thermalStrainHistory * 0.82f + (1f - ecology.recoveryStability) * 0.12f,
        )
        val signal = blend(
            device.highSignalDependency * 0.75f + device.stableChargingProfile * 0.05f,
            ecology.signalPressureHistory * 0.7f + ecology.mobileDataHeavy * 0.3f,
        )
        val crown = blend(
            device.highSensorRichness * 0.4f + (1f - device.lowMemoryPressureTolerance) * 0.22f + device.hardwareTierNorm * 0.12f,
            ecology.neuralLoadHistory * 0.82f,
        )
        val reserve = blend(
            device.stableChargingProfile * 0.35f + (1f - device.storageDenseProfile) * 0.15f + ecology.frequentChargerAttachment * 0.25f,
            ecology.reserveStressHistory * 0.75f,
        )
        val archive = blend(
            device.storageDenseProfile * 0.75f + device.storageCapacityClass * 0.15f + device.lowMemoryPressureTolerance * 0.1f,
            ecology.archiveBurdenHistory * 0.88f,
        )
        val motion = blend(
            device.highMotionLife * 0.72f + device.uptimeRhythmTendency * 0.18f,
            (ecology.neuralLoadHistory * 0.18f + (1f - ecology.recoveryStability) * 0.14f),
        )
        val balanced = (
            0.18f +
                (1f - maxOf(thermal, signal, crown, reserve, archive, motion)) * 0.25f +
                ecology.erraticStressRhythm * 0.08f +
                device.nocturnalUsageBias * 0.05f
            ).coerceIn(0.05f, 1f)
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
            frequentChargerAttachment = if (telemetry.isCharging == true) 0.45f else 0.25f,
            erraticStressRhythm = 0.2f,
        )
        if (adaptationMarkers.isEmpty()) return dev to instant
        val fromMarkers = UsageEcologyProfileBuilder.fromMarkers(adaptationMarkers)
        val ecology = blendEcology(instant, fromMarkers, 0.65f)
        return dev to ecology
    }
}
