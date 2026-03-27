package com.velithorne.vessel.morphogenesis_core

import com.velithorne.vessel.lineage.AdaptationKind
import com.velithorne.vessel.lineage.AdaptationMarker

/**
 * Derives biography updates from adaptation markers + pressure — structural memory.
 */
object BiographyEngine {

    fun step(
        prev: BiographyState,
        pressure: GrowthPressureState,
        markers: List<AdaptationMarker>,
        nowMs: Long,
    ): BiographyState {
        var scars = prev.scars.toMutableList()
        var flags = prev.thresholdFlags.toMutableSet()
        var molts = prev.moltCount

        val thermal = markers.find { it.kind == AdaptationKind.THERMAL }?.accumulatedIntensity ?: 0f
        if (thermal > 1.2f && ThresholdEventKind.FIRST_HEAT_CRISIS !in flags) {
            flags += ThresholdEventKind.FIRST_HEAT_CRISIS
            scars += ScarPlate("sc_heat_$nowMs", 0.42f, 0.38f, 0.55f, "thermal band")
        }
        val sig = markers.find { it.kind == AdaptationKind.SIGNAL }?.accumulatedIntensity ?: 0f
        if (sig > 1f && pressure.signal > 0.55f && ThresholdEventKind.FIRST_FROND_EXTENSION !in flags) {
            flags += ThresholdEventKind.FIRST_FROND_EXTENSION
        }
        val arch = markers.find { it.kind == AdaptationKind.ARCHIVE }?.accumulatedIntensity ?: 0f
        if (arch > 1.1f && ThresholdEventKind.FIRST_ARCHIVE_PLATE !in flags) {
            flags += ThresholdEventKind.FIRST_ARCHIVE_PLATE
            scars += ScarPlate("sc_arch_$nowMs", 0.5f, 0.62f, 0.45f, "archive plating")
        }
        if (pressure.recovery > 0.75f && pressure.starvation < 0.2f && ThresholdEventKind.FIRST_CLEAN_CHARGING_WEEK !in flags) {
            flags += ThresholdEventKind.FIRST_CLEAN_CHARGING_WEEK
        }
        if (pressure.mutation > 0.7f && molts == 0) {
            molts = 1
            flags += ThresholdEventKind.FIRST_MOLT
        }
        if (scars.size > 6 && ThresholdEventKind.FIRST_SCAR !in flags) {
            flags += ThresholdEventKind.FIRST_SCAR
        }

        return BiographyState(
            scars = scars.takeLast(12),
            thresholdFlags = flags,
            rerouteCount = prev.rerouteCount,
            moltCount = molts,
        )
    }

    fun maybeReroute(prev: BiographyState, pressure: GrowthPressureState): BiographyState {
        if (pressure.coherence < 0.25f && prev.rerouteCount < 8) {
            return prev.copy(rerouteCount = prev.rerouteCount + 1)
        }
        return prev
    }
}
