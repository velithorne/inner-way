package com.velithorne.vessel.model

import com.velithorne.vessel.branching.LineageBranch

/**
 * Subtle morphology multipliers from persisted [com.velithorne.vessel.branching.MorphologyBranchState].
 * Kept small so the pod stays one species family.
 */
data class BranchVisualState(
    val leadingBranch: LineageBranch,
    /** 0..1 — UI / readiness from structural progression. */
    val branchReadiness: Float,
    /** 0..3 — soft → established specialization. */
    val commitmentLevel: Int,
    /** Shell band / rim emphasis (thermal path). */
    val shellBandMul: Float,
    /** Lateral bud length / sheen (signal path). */
    val lateralFrondMul: Float,
    /** Crown / nucleus upper bias (neural path). */
    val crownBloomMul: Float,
    /** Lower reserve bulb scale (reserve path). */
    val reserveBulbMul: Float,
    /** Inner haze / archive density (archive path). */
    val innerMassMul: Float,
    /** Silhouette vertical stiffness (motion path). */
    val bracingMul: Float,
    /** Overall blend toward moderate form (balanced). */
    val balancedBlend: Float,
    /** Vesica horizontal stretch for shell silhouette (thermal / signal families). */
    val contourStretchXMul: Float,
    /** Vesica vertical stretch (reserve / crown bias). */
    val contourStretchYMul: Float,
    /** Extra lateral frond length vs base bud strength (SIGNAL_FROND). */
    val lateralReachMul: Float,
    /** 0..~0.12 — extra angle/length jitter for signal-path asymmetry. */
    val lateralAsymmetryBoost: Float,
    /** Bracing / tension stroke visibility (MOTION_BRACED). */
    val bracingLineAlpha: Float,
) {
    companion object {
        fun neutral() = BranchVisualState(
            leadingBranch = LineageBranch.BALANCED,
            branchReadiness = 0f,
            commitmentLevel = 0,
            shellBandMul = 1f,
            lateralFrondMul = 1f,
            crownBloomMul = 1f,
            reserveBulbMul = 1f,
            innerMassMul = 1f,
            bracingMul = 1f,
            balancedBlend = 1f,
            contourStretchXMul = 1f,
            contourStretchYMul = 1f,
            lateralReachMul = 1f,
            lateralAsymmetryBoost = 0f,
            bracingLineAlpha = 0f,
        )
    }
}
