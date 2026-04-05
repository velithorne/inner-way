package com.velithorne.innerway.render

/**
 * Conductive filament between two nodes.
 */
data class GrowthEdge(
    val fromId: String,
    val toId: String,
    val thickness: Float,
    val conductivity: Float,
    val age: Float,
)
