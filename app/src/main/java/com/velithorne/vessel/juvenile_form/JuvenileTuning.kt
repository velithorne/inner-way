package com.velithorne.vessel.juvenile_form

import com.velithorne.vessel.config.SimulationMode

/**
 * Grouped tuning — dev accelerates juvenile emergence for testing.
 */
data class JuvenileTuning(
    val minStageOrdinalForJuvenile: Int,
    val emergenceFromTopologyMul: Float,
    val emergenceFromProgressMul: Float,
    val regionMassBlend: Float,
    val upperDominanceMul: Float,
    val lateralDominanceMul: Float,
    val lowerDominanceMul: Float,
    val shellToChamberRatioBias: Float,
    val supportVisibilityThreshold: Float,
    val contourExpansionStrength: Float,
    val topologyDivergenceStrength: Float,
    val branchArchitectureWeight: Float,
    val viewportOverflowSoftness: Float,
    val devEmergenceBoost: Float,
) {
    companion object {
        fun forMode(mode: SimulationMode): JuvenileTuning = when (mode) {
            SimulationMode.DEV_SIMULATION -> JuvenileTuning(
                minStageOrdinalForJuvenile = 6,
                emergenceFromTopologyMul = 0.95f,
                emergenceFromProgressMul = 0.55f,
                regionMassBlend = 0.62f,
                upperDominanceMul = 1.15f,
                lateralDominanceMul = 1.12f,
                lowerDominanceMul = 1.1f,
                shellToChamberRatioBias = 0.55f,
                supportVisibilityThreshold = 0.14f,
                contourExpansionStrength = 0.42f,
                topologyDivergenceStrength = 0.48f,
                branchArchitectureWeight = 0.72f,
                viewportOverflowSoftness = 0.22f,
                devEmergenceBoost = 0.18f,
            )
            SimulationMode.RELEASE_REALTIME -> JuvenileTuning(
                minStageOrdinalForJuvenile = 7,
                emergenceFromTopologyMul = 0.75f,
                emergenceFromProgressMul = 0.42f,
                regionMassBlend = 0.55f,
                upperDominanceMul = 1f,
                lateralDominanceMul = 1f,
                lowerDominanceMul = 1f,
                shellToChamberRatioBias = 0.5f,
                supportVisibilityThreshold = 0.22f,
                contourExpansionStrength = 0.32f,
                topologyDivergenceStrength = 0.36f,
                branchArchitectureWeight = 0.58f,
                viewportOverflowSoftness = 0.14f,
                devEmergenceBoost = 0f,
            )
        }
    }
}
