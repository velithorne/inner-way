package com.velithorne.vessel.physiology

/**
 * Target recovery level from charging rest vs active thermal/motion strain.
 * [PhysiologyEngine] smooths this into [SpeciesState.recovery].
 */
object RecoveryModel {

    fun target(
        isCharging: Boolean?,
        screenInteractive: Boolean?,
        motionNorm: Float,
        feverRaw: Float,
        thermalStrain: Float,
        hunger: Float,
        stress: Float,
    ): Float {
        val chargeBoost = when (isCharging) {
            true -> 0.55f
            false -> 0f
            null -> 0.22f
        }
        val calmBoost = (1f - motionNorm.coerceIn(0f, 1f)) * 0.25f
        val idlingBoost = if (screenInteractive == false) 0.18f else if (screenInteractive == true) 0f else 0.08f
        val thermalPenalty = (feverRaw * 0.35f + thermalStrain * 0.2f).coerceIn(0f, 0.55f)
        val deprivationPenalty = (hunger * 0.2f + stress * 0.15f).coerceIn(0f, 0.45f)
        val movingWhileHot = motionNorm * feverRaw * 0.25f
        return (0.12f + chargeBoost + calmBoost + idlingBoost - thermalPenalty - deprivationPenalty - movingWhileHot)
            .coerceIn(0f, 1f)
    }
}
