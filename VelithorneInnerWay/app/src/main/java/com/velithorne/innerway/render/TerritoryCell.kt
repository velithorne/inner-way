package com.velithorne.innerway.render

/**
 * One cell in the habitat grid (normalized screen space 0–1 covers the substrate).
 * Values are smoothed over time; persistence is handled by [com.velithorne.innerway.mind.TerritoryEngine].
 */
data class TerritoryCell(
    val xIndex: Int,
    val yIndex: Int,
    /** Structural calm — rises with stillness and successful reinforcement. */
    val stability: Float,
    /** Motion / nervous load mapped into local unease (device motion is global; tips smear locally). */
    val disturbance: Float,
    /** Recent user touch / drag exposure (decays). */
    val touchExposure: Float,
    /** How much growth structure has passed through this zone. */
    val occupancy: Float,
    /** Learned preference for extending growth here. */
    val growthAffinity: Float,
    /** Preference for resting / rooting (mature reinforcement). */
    val restAffinity: Float,
)
