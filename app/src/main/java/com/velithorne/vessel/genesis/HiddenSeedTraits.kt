package com.velithorne.vessel.genesis

/**
 * Deterministic hidden traits per specimen — influence birth topology from the first frame.
 * Values in ~0..1 unless noted; stable across sessions for the same specimen id.
 */
data class HiddenSeedTraits(
    val symmetryBias: Float,
    val densityBias: Float,
    val shellBias: Float,
    val reserveCompressionBias: Float,
    val crownLiftBias: Float,
    val signalSpreadBias: Float,
    val archivePotential: Float,
    val supportTensionBias: Float,
    val mutationTolerance: Float,
    val coherenceBias: Float,
    val latentAsymmetryBias: Float,
    val lineagePersonalityWeight: Float,
) {
    companion object {
        fun fromSpecimenId(specimenId: String): HiddenSeedTraits {
            val h = specimenId.hashCode()
            fun f(seed: Int): Float = (((h xor seed) and 0xFFFF) / 65535f).coerceIn(0f, 1f)
            return HiddenSeedTraits(
                symmetryBias = (0.35f + f(1) * 0.5f).coerceIn(0.2f, 0.95f),
                densityBias = f(2),
                shellBias = f(3),
                reserveCompressionBias = (0.3f + f(4) * 0.55f).coerceIn(0.15f, 0.95f),
                crownLiftBias = (0.25f + f(5) * 0.6f).coerceIn(0.1f, 0.95f),
                signalSpreadBias = f(6),
                archivePotential = (0.2f + f(7) * 0.65f).coerceIn(0.1f, 0.95f),
                supportTensionBias = f(8),
                mutationTolerance = (0.25f + f(9) * 0.5f).coerceIn(0.15f, 0.85f),
                coherenceBias = f(10),
                latentAsymmetryBias = f(11),
                lineagePersonalityWeight = (0.15f + f(12) * 0.55f).coerceIn(0.1f, 0.9f),
            )
        }
    }
}
