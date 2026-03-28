package com.velithorne.vessel.model

import com.velithorne.vessel.morphogenesis_core.CanonicalLifeEra

/**
 * Stage- and history-driven policy: how much the authored seed silhouette vs generated topology wins.
 */
data class VisibleMorphologyState(
    /** 0..1 — retained seed-pod scaffold read (ellipse bands, stock bud layout). */
    val fallbackSeedInfluence: Float,
    /** 0..1 — generated self-assembly contour / chambers / fronds / shell zones. */
    val generatedTopologyInfluence: Float,
    val era: CanonicalLifeEra,
    /** Dominant visual driver for debug / copy. */
    val dominantContourDriver: String,
    /** 0..1 visible asymmetry (species-coherent, not noise). */
    val visibleAsymmetryScore: Float,
    /** Crown chamber center in normalized body space 0..1 (x from left). */
    val crownChamberNx: Float,
    val crownChamberNy: Float,
    val crownChamberRadiusMul: Float,
    /** Reserve basin center and depth bias. */
    val reserveBasinNx: Float,
    val reserveBasinNy: Float,
    val reserveBasinDepthMul: Float,
    /** Frond roots (not mirror-perfect). */
    val frondRootLeftNx: Float,
    val frondRootRightNx: Float,
    val frondCurvatureMul: Float,
    val frondDensityMul: Float,
    val frondAsymmetryMul: Float,
    /** Regional shell 0..1 (upper, lower, left, right perimeter). */
    val shellUpperPlate: Float,
    val shellLowerPlate: Float,
    val shellLeftWing: Float,
    val shellRightWing: Float,
    /** Summary lines for Lineage / Vessel (must match what we draw). */
    val topologySummaryLines: List<String>,
) {
    companion object {
        fun neutral(era: CanonicalLifeEra = CanonicalLifeEra.SEED) = VisibleMorphologyState(
            fallbackSeedInfluence = 0.94f,
            generatedTopologyInfluence = 0.06f,
            era = era,
            dominantContourDriver = "seed scaffold",
            visibleAsymmetryScore = 0.05f,
            crownChamberNx = 0.5f,
            crownChamberNy = 0.22f,
            crownChamberRadiusMul = 0.9f,
            reserveBasinNx = 0.5f,
            reserveBasinNy = 0.72f,
            reserveBasinDepthMul = 0.7f,
            frondRootLeftNx = 0.32f,
            frondRootRightNx = 0.68f,
            frondCurvatureMul = 1f,
            frondDensityMul = 1f,
            frondAsymmetryMul = 0.05f,
            shellUpperPlate = 0.4f,
            shellLowerPlate = 0.42f,
            shellLeftWing = 0.4f,
            shellRightWing = 0.4f,
            topologySummaryLines = emptyList(),
        )

        /** Archetype-free birth: no authored pod scaffold — topology-only (used in genesis / early seed). */
        fun genesisFieldFirst(era: CanonicalLifeEra = CanonicalLifeEra.SEED) = VisibleMorphologyState(
            fallbackSeedInfluence = 0f,
            generatedTopologyInfluence = 1f,
            era = era,
            dominantContourDriver = "genesis tissue field",
            visibleAsymmetryScore = 0.12f,
            crownChamberNx = 0.5f,
            crownChamberNy = 0.28f,
            crownChamberRadiusMul = 0.45f,
            reserveBasinNx = 0.5f,
            reserveBasinNy = 0.65f,
            reserveBasinDepthMul = 0.45f,
            frondRootLeftNx = 0.42f,
            frondRootRightNx = 0.58f,
            frondCurvatureMul = 0.85f,
            frondDensityMul = 0.75f,
            frondAsymmetryMul = 0.15f,
            shellUpperPlate = 0.18f,
            shellLowerPlate = 0.2f,
            shellLeftWing = 0.16f,
            shellRightWing = 0.16f,
            topologySummaryLines = emptyList(),
        )
    }
}
