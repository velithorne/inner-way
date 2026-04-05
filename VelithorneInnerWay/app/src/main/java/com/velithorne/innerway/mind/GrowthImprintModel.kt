package com.velithorne.innerway.mind

/**
 * Long-term developmental bias on substrate growth (0..1 each).
 * Shaped by lived history — not only instantaneous state.
 */
data class GrowthImprintModel(
    val stressLoad: Float = 0.35f,
    val calmReserve: Float = 0.4f,
    val recoveryStrength: Float = 0.35f,
    val disturbanceBias: Float = 0.25f,
    val stillnessAffinity: Float = 0.3f,
    val chargeTrust: Float = 0.35f,
    val asymmetryBias: Float = 0.25f,
    val branchingConfidence: Float = 0.4f,
    val plateFormationBias: Float = 0.35f,
    val contractionMemory: Float = 0.3f,
) {
    fun clamped(): GrowthImprintModel = GrowthImprintModel(
        stressLoad = stressLoad.coerceIn(0f, 1f),
        calmReserve = calmReserve.coerceIn(0f, 1f),
        recoveryStrength = recoveryStrength.coerceIn(0f, 1f),
        disturbanceBias = disturbanceBias.coerceIn(0f, 1f),
        stillnessAffinity = stillnessAffinity.coerceIn(0f, 1f),
        chargeTrust = chargeTrust.coerceIn(0f, 1f),
        asymmetryBias = asymmetryBias.coerceIn(0f, 1f),
        branchingConfidence = branchingConfidence.coerceIn(0f, 1f),
        plateFormationBias = plateFormationBias.coerceIn(0f, 1f),
        contractionMemory = contractionMemory.coerceIn(0f, 1f),
    )
}
