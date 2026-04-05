package com.velithorne.innerway.render

/**
 * Conductive filament between two nodes — extends visually along its length over time.
 */
data class GrowthEdge(
    val fromId: String,
    val toId: String,
    /** Target stroke width when [growthProgress] reaches 1. */
    val thickness: Float,
    val conductivity: Float,
    val age: Float,
    val growthPhase: GrowthPhase = GrowthPhase.GROWING,
    /** 0..1 length of branch grown from [fromId] toward [toId]. */
    val growthProgress: Float = 0f,
    val createdAt: Long = 0L,
)
