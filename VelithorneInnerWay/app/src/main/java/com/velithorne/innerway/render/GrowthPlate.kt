package com.velithorne.innerway.render

/**
 * Crystalline / chip-like plate anchored to substrate nodes — crystallizes over time.
 */
data class GrowthPlate(
    val anchorNodeIds: List<String>,
    /** Target opacity when [growthProgress] reaches 1. */
    val opacity: Float,
    val age: Float,
    val growthPhase: GrowthPhase = GrowthPhase.FORMING,
    val growthProgress: Float = 0f,
    val createdAt: Long = 0L,
)
