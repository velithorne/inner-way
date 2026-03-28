package com.velithorne.vessel.morphogenesis

/**
 * Trait state — drifted by pressures; Room-serializable later.
 */
data class SpeciesGenome(
    val shellThickness: Float,
    val organSpacingBias: Float,
    val cranialExpansionBias: Float,
    val lowerReservoirBias: Float,
    val antennaBranchBias: Float,
    val coolingVeilBias: Float,
    val archiveLamellaBias: Float,
    val tendonDensityBias: Float,
    val conduitDensityBias: Float,
    val asymmetryBias: Float,
    val resilienceBias: Float,
) {
    companion object {
        fun initial(seed: SpeciesSeed) = SpeciesGenome(
            shellThickness = 0.45f + seed.shellBias * 0.25f,
            organSpacingBias = 0.5f + seed.coherenceBias * 0.15f,
            cranialExpansionBias = 0.42f + seed.densityBias * 0.2f,
            lowerReservoirBias = 0.48f + seed.archiveBias * 0.22f,
            antennaBranchBias = 0.4f + seed.branchingBias * 0.35f,
            coolingVeilBias = 0.45f + seed.thermalToleranceBias * 0.25f,
            archiveLamellaBias = 0.46f + seed.archiveBias * 0.28f,
            tendonDensityBias = 0.44f + seed.musculatureBias * 0.3f,
            conduitDensityBias = 0.42f + seed.signalBias * 0.28f,
            asymmetryBias = (1f - seed.symmetryBias) * 0.35f,
            resilienceBias = 0.5f + seed.reserveBias * 0.25f,
        )
    }
}
