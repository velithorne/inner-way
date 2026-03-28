package com.velithorne.vessel.genesis

import com.velithorne.vessel.morphogenesis_core.GrowthPressureState

/**
 * Species grammar for birth — where reserve/crown/signal tend before full structure exists.
 */
object GenesisGrammar {
    fun reserveNyBias(pressure: GrowthPressureState, traits: HiddenSeedTraits): Float =
        (0.58f + pressure.reserve * 0.12f + traits.reserveCompressionBias * 0.08f).coerceIn(0.48f, 0.82f)

    fun crownNyBias(pressure: GrowthPressureState, traits: HiddenSeedTraits): Float =
        (0.18f + pressure.signal * 0.1f + traits.crownLiftBias * 0.12f).coerceIn(0.12f, 0.42f)

    fun lateralSignalNx(pressure: GrowthPressureState, traits: HiddenSeedTraits, sideSign: Float): Float =
        (0.5f + sideSign * (0.12f + traits.signalSpreadBias * 0.18f + pressure.signal * 0.1f)).coerceIn(0.08f, 0.92f)

    fun shellCoherenceGate(pressure: GrowthPressureState, traits: HiddenSeedTraits): Float =
        (pressure.coherence * 0.45f + traits.coherenceBias * 0.35f + traits.shellBias * 0.2f).coerceIn(0f, 1f)
}
