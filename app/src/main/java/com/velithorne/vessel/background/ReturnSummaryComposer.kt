package com.velithorne.vessel.background

import com.velithorne.vessel.lineage.SeedPodReturnSummary

/**
 * Deterministic lines from ambient ecology patterns + [AmbientGrowthAccumulator] counts.
 */
object ReturnSummaryComposer {

    fun compose(
        awaySeconds: Long,
        acc: AmbientGrowthAccumulator.Result,
        tuning: BackgroundTuning,
    ): List<String> {
        val lines = mutableListOf<String>()
        if (acc.chargingSamples >= tuning.chargingSampleStrongThreshold) {
            lines += "Charging cycles while away fed recovery and shell coherence budgets."
        }
        if (acc.cellularSamples >= tuning.cellularSampleStrongThreshold) {
            lines += "Sustained mobile connectivity strengthened lateral signal tendency."
        }
        if (acc.warmThermalSamples >= 3) {
            lines += "Thermal stress samples accumulated toward shell-oriented development."
        }
        if (lines.isEmpty() && acc.dominantDriver != "none") {
            lines += when (acc.dominantDriver) {
                "signal_mobile" -> "Background ecology leaned on mobile connectivity patterns."
                "thermal" -> "Elevated heat samples nudged thermal adaptation."
                "charging_recovery" -> "Plugged-in intervals stabilized reserve and recovery channels."
                "reserve_stress" -> "Low-charge samples added reserve-stress shaping."
                "archive" -> "Heavy storage use nudged archive-core pressure."
                else -> "Ambient phone ecology contributed to slow background growth."
            }
        }
        if (lines.isEmpty()) return emptyList()
        val header = "Away ~${awaySeconds}s — ambient ecology absorbed."
        return (listOf(header) + lines).take(4)
    }

    fun mergeWithExisting(
        ambient: List<String>,
        existing: SeedPodReturnSummary?,
        awaySeconds: Long,
    ): SeedPodReturnSummary? {
        val baseLines = existing?.lines.orEmpty()
        if (ambient.isEmpty()) {
            if (baseLines.isEmpty()) return null
            return SeedPodReturnSummary(awaySeconds = awaySeconds, lines = baseLines.take(5))
        }
        val merged = (ambient + baseLines).distinct().take(5)
        return SeedPodReturnSummary(awaySeconds = awaySeconds, lines = merged)
    }
}
