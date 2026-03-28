package com.velithorne.vessel.genesis

import com.velithorne.vessel.config.SimulationMode

/**
 * Dev accelerates visible divergence at birth; release stays history-weighted.
 */
data class GenesisTuning(
    val birthDivergenceMul: Float,
    val fieldVisibilityMul: Float,
    val contourSampleFloor: Int,
) {
    companion object {
        fun forMode(mode: SimulationMode): GenesisTuning = when (mode) {
            SimulationMode.DEV_SIMULATION -> GenesisTuning(
                birthDivergenceMul = 1.35f,
                fieldVisibilityMul = 1.15f,
                contourSampleFloor = 40,
            )
            SimulationMode.RELEASE_REALTIME -> GenesisTuning(
                birthDivergenceMul = 1f,
                fieldVisibilityMul = 1f,
                contourSampleFloor = 36,
            )
        }
    }
}
