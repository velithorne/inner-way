package com.velithorne.innerway.genome

/**
 * Action patterns for wake, defense, bonding, and curiosity cadence.
 */
data class BehaviorGenome(
    val wakeStyle: String = "soft_rise",
    val lowEnergyResponse: String = "dim_inward",
    val overheatingDefense: String = "contract_throttle",
    val bondingStyle: String = "ambient_attune",
    val curiosityFrequency: Float = 0.25f,
)
