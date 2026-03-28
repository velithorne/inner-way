package com.velithorne.vessel.morphogenesis_core

/**
 * Birth morphology bias — one species, many body plans.
 */
data class SeedArchetype(
    val symmetryBias: Float,
    val densityBias: Float,
    val reserveBasinCompression: Float,
    val crownPotential: Float,
    val frondPotential: Float,
    val shellPlatingPotential: Float,
    val archivePlatePotential: Float,
    val asymmetryTolerance: Float,
    val coherenceBias: Float,
    val mutationTolerance: Float,
    val personalityWeights: List<Float>,
) {
    companion object {
        fun uniform() = SeedArchetype(
            symmetryBias = 0.5f,
            densityBias = 0.5f,
            reserveBasinCompression = 0.5f,
            crownPotential = 0.5f,
            frondPotential = 0.5f,
            shellPlatingPotential = 0.5f,
            archivePlatePotential = 0.5f,
            asymmetryTolerance = 0.35f,
            coherenceBias = 0.5f,
            mutationTolerance = 0.35f,
            personalityWeights = listOf(0.25f, 0.25f, 0.25f, 0.25f),
        )
    }
}
