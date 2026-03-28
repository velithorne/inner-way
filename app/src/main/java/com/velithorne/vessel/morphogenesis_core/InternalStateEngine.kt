package com.velithorne.vessel.morphogenesis_core

object InternalStateEngine {

    fun step(prev: InternalHiddenState, pressure: GrowthPressureState, biography: BiographyState): InternalHiddenState {
        val reserve = (prev.reserveLevel * 0.97f + pressure.reserve * 0.06f + (1f - pressure.starvation) * 0.02f).coerceIn(0f, 1f)
        val thermal = (prev.thermalTension * 0.98f + pressure.thermal * 0.04f).coerceIn(0f, 1f)
        val sigCoh = (prev.signalCoherence * 0.99f + pressure.signal * 0.03f + pressure.coherence * 0.02f).coerceIn(0f, 1f)
        val repair = (prev.repairDebt * 0.995f + pressure.starvation * 0.08f - pressure.recovery * 0.05f).coerceIn(0f, 1f)
        val growth = (pressure.signal + pressure.archive + pressure.motion) / 3f
        val conf = (prev.lineageConfidence * 0.998f + (if (biography.thresholdFlags.isNotEmpty()) 0.002f else 0f)).coerceIn(0.2f, 1f)
        return InternalHiddenState(
            reserveLevel = reserve,
            thermalTension = thermal,
            signalCoherence = sigCoh,
            repairDebt = repair,
            growthPressure = growth.coerceIn(0f, 1f),
            lineageConfidence = conf,
        )
    }
}
