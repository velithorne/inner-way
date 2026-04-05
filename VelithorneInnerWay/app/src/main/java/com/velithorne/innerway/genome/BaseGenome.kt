package com.velithorne.innerway.genome

/**
 * Immutable species constants and law version binding.
 */
object BaseGenome {
    const val SPECIES_NAME = "Velithorne siliconis"
    const val LAW_VERSION = 1

    /** Default low-battery threshold (0..1). */
    const val DEFAULT_ENERGY_LOW = 0.18f

    /** Heat ratio considered distressing (normalized 0..1, device-relative). */
    const val DEFAULT_THERMAL_DISTRESS = 0.82f
}
