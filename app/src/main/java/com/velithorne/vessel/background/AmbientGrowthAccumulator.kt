package com.velithorne.vessel.background

import com.velithorne.vessel.growth_seedpod.SeedPodGrowthBudget
import com.velithorne.vessel.telemetry.NetworkTransport

/**
 * Derives slow budget nudges, pattern counts, and abstract "pressure" scalars from stored ecology snapshots.
 * No per-frame simulation — values feed reopen catch-up and summaries only.
 */
object AmbientGrowthAccumulator {

    data class Result(
        val budgetDelta: SeedPodGrowthBudget,
        /** Counts used for summaries and adaptation nudges. */
        val snapshotCount: Int,
        val chargingSamples: Int,
        val cellularSamples: Int,
        val warmThermalSamples: Int,
        val hotThermalSamples: Int,
        val reserveStressSamples: Int,
        val archiveHeavySamples: Int,
        /** Calm / idle-ish samples (screen off, low motion, not hot). */
        val idleStableSamples: Int,
        /** Elevated motion proxy when available. */
        val motionHighSamples: Int,
        val powerSaveSamples: Int,
        val meteredNetworkSamples: Int,
        val dominantDriver: String,
        /** 0..1 abstract pressure toward structural readiness (post-chamber stages). */
        val ambientStructuralReadinessNudge: Float,
        /** 0..1 abstract pressure toward branch affinity shift. */
        val ambientAffinityNudge: Float,
    )

    fun accumulate(
        snapshots: List<EcologySnapshot>,
        tuning: BackgroundTuning,
    ): Result {
        if (snapshots.isEmpty()) {
            return Result(
                budgetDelta = SeedPodGrowthBudget(),
                snapshotCount = 0,
                chargingSamples = 0,
                cellularSamples = 0,
                warmThermalSamples = 0,
                hotThermalSamples = 0,
                reserveStressSamples = 0,
                archiveHeavySamples = 0,
                idleStableSamples = 0,
                motionHighSamples = 0,
                powerSaveSamples = 0,
                meteredNetworkSamples = 0,
                dominantDriver = "none",
                ambientStructuralReadinessNudge = 0f,
                ambientAffinityNudge = 0f,
            )
        }
        var chg = 0
        var cell = 0
        var warm = 0
        var hot = 0
        var reserveStress = 0
        var archiveHeavy = 0
        var idle = 0
        var motionHi = 0
        var pwrSave = 0
        var metered = 0
        for (s in snapshots) {
            val t = s.telemetry
            if (t.isCharging == true) chg++
            if (t.networkType == NetworkTransport.CELLULAR) cell++
            val temp = t.batteryTempC
            if (temp != null && temp >= tuning.warmBatteryTempC) warm++
            if (temp != null && temp >= tuning.hotBatteryTempC) hot++
            val bat = t.batteryPct
            if (bat != null && bat < 25f) reserveStress++
            val st = t.storageUsedPct
            if (st != null && st > 0.82f) archiveHeavy++
            if (t.powerSaveEnabled == true) pwrSave++
            if (t.networkMetered == true) metered++
            val motion = t.motionIntensity
            val motionLow = motion == null || motion < tuning.idleMotionIntensityMax
            val screenOff = t.screenInteractive == false
            val notHot = temp == null || temp < tuning.warmBatteryTempC
            if (screenOff && motionLow && notHot) idle++
            if (motion != null && motion >= tuning.motionHighIntensityMin) motionHi++
        }
        val n = snapshots.size.coerceAtLeast(1)
        val scale = (n / 20f).coerceIn(0.4f, 1.5f)
        val delta = SeedPodGrowthBudget(
            crown = (cell * 0.004f * scale).coerceIn(0f, 0.06f),
            lateral = (cell * 0.006f * scale).coerceIn(0f, 0.08f),
            reserve = (chg * 0.005f * scale + reserveStress * 0.004f * scale).coerceIn(0f, 0.08f),
            shell = (warm * 0.005f * scale).coerceIn(0f, 0.07f),
            thermal = (warm * 0.006f * scale + hot * 0.002f * scale).coerceIn(0f, 0.08f),
            coherence = (chg * 0.004f * scale + idle * 0.003f * scale).coerceIn(0f, 0.06f),
        )
        val dominant = when {
            cell >= chg && cell >= warm && cell >= idle -> "signal_mobile"
            warm >= chg && warm >= cell && warm >= idle -> "thermal"
            chg >= warm && chg >= cell && chg >= idle -> "charging_recovery"
            idle >= chg && idle >= cell && idle >= warm -> "idle_coherence"
            reserveStress > n / 3 -> "reserve_stress"
            archiveHeavy > n / 4 -> "archive"
            motionHi > n / 5 -> "motion"
            else -> "mixed"
        }
        // Abstract pressures for structural / branch integration (bounded 0..1)
        val readinessRaw = (
            chg * 0.02f + cell * 0.018f + warm * 0.015f + idle * 0.012f +
                reserveStress * 0.01f + archiveHeavy * 0.01f + motionHi * 0.008f
            ) / n.toFloat()
        val affinityRaw = (
            cell * 0.025f + warm * 0.02f + chg * 0.015f + idle * 0.01f +
                reserveStress * 0.018f + archiveHeavy * 0.02f + motionHi * 0.012f
            ) / n.toFloat()
        return Result(
            budgetDelta = delta,
            snapshotCount = n,
            chargingSamples = chg,
            cellularSamples = cell,
            warmThermalSamples = warm,
            hotThermalSamples = hot,
            reserveStressSamples = reserveStress,
            archiveHeavySamples = archiveHeavy,
            idleStableSamples = idle,
            motionHighSamples = motionHi,
            powerSaveSamples = pwrSave,
            meteredNetworkSamples = metered,
            dominantDriver = dominant,
            ambientStructuralReadinessNudge = readinessRaw.coerceIn(0f, 1f),
            ambientAffinityNudge = affinityRaw.coerceIn(0f, 1f),
        )
    }
}
