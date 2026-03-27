package com.velithorne.vessel.morphogenesis_core

import com.velithorne.vessel.config.SimulationMode

/**
 * Procedural morphogenesis pacing — dev accelerates observation.
 */
data class MorphogenesisTuning(
    val pressureAlphaSlow: Float,
    val pressureAlphaFast: Float,
) {
    companion object {
        fun forMode(mode: SimulationMode): MorphogenesisTuning = when (mode) {
            SimulationMode.DEV_SIMULATION -> MorphogenesisTuning(
                pressureAlphaSlow = 0.035f,
                pressureAlphaFast = 0.09f,
            )
            SimulationMode.RELEASE_REALTIME -> MorphogenesisTuning(
                pressureAlphaSlow = 0.012f,
                pressureAlphaFast = 0.04f,
            )
        }
    }
}
