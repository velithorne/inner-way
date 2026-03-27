package com.velithorne.vessel.background

import com.velithorne.vessel.growth_seedpod.SeedPodGrowthBudget
import com.velithorne.vessel.telemetry.NetworkTransport

/**
 * Derives slow budget nudges and summary lines from stored ecology snapshots — no per-frame simulation.
 */
object AmbientGrowthAccumulator {

    data class Result(
        val budgetDelta: SeedPodGrowthBudget,
        val dominantDriver: String,
        val chargingSamples: Int,
        val cellularSamples: Int,
        val warmThermalSamples: Int,
    )

    fun accumulate(
        snapshots: List<EcologySnapshot>,
        tuning: BackgroundTuning,
    ): Result {
        if (snapshots.isEmpty()) {
            return Result(
                budgetDelta = SeedPodGrowthBudget(),
                dominantDriver = "none",
                chargingSamples = 0,
                cellularSamples = 0,
                warmThermalSamples = 0,
            )
        }
        var chg = 0
        var cell = 0
        var warm = 0
        var reserveStress = 0
        var archiveHeavy = 0
        for (s in snapshots) {
            val t = s.telemetry
            if (t.isCharging == true) chg++
            if (t.networkType == NetworkTransport.CELLULAR) cell++
            val temp = t.batteryTempC
            if (temp != null && temp >= tuning.warmBatteryTempC) warm++
            val bat = t.batteryPct
            if (bat != null && bat < 25f) reserveStress++
            val st = t.storageUsedPct
            if (st != null && st > 0.82f) archiveHeavy++
        }
        val n = snapshots.size.coerceAtLeast(1)
        val scale = (n / 20f).coerceIn(0.4f, 1.5f)
        val delta = SeedPodGrowthBudget(
            crown = (cell * 0.004f * scale).coerceIn(0f, 0.06f),
            lateral = (cell * 0.006f * scale).coerceIn(0f, 0.08f),
            reserve = (chg * 0.005f * scale + reserveStress * 0.004f * scale).coerceIn(0f, 0.08f),
            shell = (warm * 0.005f * scale).coerceIn(0f, 0.07f),
            thermal = (warm * 0.006f * scale).coerceIn(0f, 0.08f),
            coherence = (chg * 0.004f * scale).coerceIn(0f, 0.06f),
        )
        val dominant = when {
            cell >= chg && cell >= warm -> "signal_mobile"
            warm >= chg && warm >= cell -> "thermal"
            chg >= warm && chg >= cell -> "charging_recovery"
            reserveStress > n / 3 -> "reserve_stress"
            archiveHeavy > n / 4 -> "archive"
            else -> "mixed"
        }
        return Result(
            budgetDelta = delta,
            dominantDriver = dominant,
            chargingSamples = chg,
            cellularSamples = cell,
            warmThermalSamples = warm,
        )
    }
}
