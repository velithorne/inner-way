package com.velithorne.vessel.data.db.mapper

import com.velithorne.vessel.background.EcologySnapshot
import com.velithorne.vessel.background.EcologySnapshotSourceType
import com.velithorne.vessel.data.db.entity.EcologySnapshotEntity
import com.velithorne.vessel.telemetry.NetworkTransport
import com.velithorne.vessel.telemetry.TelemetrySnapshot

object EcologySnapshotMapper {

    fun toEntity(specimenId: String, snap: EcologySnapshot): EcologySnapshotEntity =
        EcologySnapshotEntity(
            specimenId = specimenId,
            timestampMillis = snap.timestampMillis,
            batteryPct = snap.telemetry.batteryPct,
            isCharging = snap.telemetry.isCharging,
            batteryTempC = snap.telemetry.batteryTempC,
            powerSaveEnabled = snap.telemetry.powerSaveEnabled,
            storageUsedPct = snap.telemetry.storageUsedPct,
            lowMemoryFlag = snap.telemetry.lowMemoryFlag,
            networkConnected = snap.telemetry.networkConnected,
            networkTypeOrdinal = snap.telemetry.networkType.ordinal,
            networkMetered = snap.telemetry.networkMetered,
            motionIntensityApprox = snap.telemetry.motionIntensity,
            screenInteractive = snap.telemetry.screenInteractive,
            uptimeMillis = snap.telemetry.uptimeMillis,
            specimenStageOrdinal = snap.specimenStageOrdinal,
            branchLeadingOrdinal = snap.branchLeadingOrdinal,
            sourceTypeOrdinal = snap.sourceType.ordinal,
            notes = snap.notes,
        )

    fun fromEntity(e: EcologySnapshotEntity): EcologySnapshot {
        val net = NetworkTransport.entries.getOrNull(e.networkTypeOrdinal) ?: NetworkTransport.NONE
        val telem = TelemetrySnapshot(
            timestampMillis = e.timestampMillis,
            batteryPct = e.batteryPct,
            isCharging = e.isCharging,
            batteryTempC = e.batteryTempC,
            powerSaveEnabled = e.powerSaveEnabled,
            storageUsedBytes = null,
            storageFreeBytes = null,
            storageUsedPct = e.storageUsedPct,
            memoryClassMb = null,
            lowMemoryFlag = e.lowMemoryFlag,
            lowRamDevice = null,
            networkConnected = e.networkConnected,
            networkType = net,
            networkMetered = e.networkMetered,
            motionIntensity = e.motionIntensityApprox,
            orientationPitchDeg = null,
            orientationRollDeg = null,
            ambientLightLux = null,
            screenInteractive = e.screenInteractive,
            uptimeMillis = e.uptimeMillis,
            deviceModel = "",
            androidVersion = "",
        )
        return EcologySnapshot(
            timestampMillis = e.timestampMillis,
            telemetry = telem,
            specimenStageOrdinal = e.specimenStageOrdinal,
            branchLeadingOrdinal = e.branchLeadingOrdinal,
            sourceType = EcologySnapshotSourceType.entries.getOrNull(e.sourceTypeOrdinal)
                ?: EcologySnapshotSourceType.PERIODIC_WORK,
            notes = e.notes,
        )
    }
}
