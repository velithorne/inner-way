package com.velithorne.vessel.growthtime

import com.velithorne.vessel.morphogenesis.BodyMassFieldState
import com.velithorne.vessel.morphogenesis.PressureAccumulator

object ReturnRevealEngine {

    fun summarize(
        prevDisplay: DisplayMorphState?,
        nowDisplay: DisplayMorphState,
        acc: PressureAccumulator,
        offlineSeconds: Long,
    ): GrowthSessionSummary {
        if (prevDisplay == null || offlineSeconds < 30) {
            return GrowthSessionSummary(offlineSeconds, emptyList(), 0L)
        }
        val deltas = mutableListOf<GrowthDelta>()
        val p = prevDisplay
        val n = nowDisplay
        if (n.seedFormBlend - p.seedFormBlend < -0.02f) {
            deltas += GrowthDelta("Seed form relaxed toward environment.", "silhouette")
        }
        if (n.chamberMass.cranialCortex - p.chamberMass.cranialCortex > 0.04f) {
            deltas += GrowthDelta("Crown tissue mass increased while inactive.", "crown")
        }
        if (n.chamberMass.lateralSignal - p.chamberMass.lateralSignal > 0.04f) {
            deltas += GrowthDelta("Signal fronds extended under sustained connectivity.", "signal")
        }
        if (n.chamberMass.lowerArchiveBasin - p.chamberMass.lowerArchiveBasin > 0.04f) {
            deltas += GrowthDelta("Lower archive basin deepened.", "archive")
        }
        if (n.chamberMass.perimeterShell - p.chamberMass.perimeterShell > 0.04f) {
            deltas += GrowthDelta("Perimeter shell accreted.", "thermal")
        }
        if (deltas.isEmpty() && offlineSeconds > 120) {
            deltas += GrowthDelta("Developmental budget accumulated while away.", "budget")
        }
        return GrowthSessionSummary(
            offlineSeconds = offlineSeconds,
            deltas = deltas.take(3),
            simulatedCatchUpMs = offlineSeconds * 1000L,
        )
    }
}
