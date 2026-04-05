package com.velithorne.innerway.render

/**
 * Crystalline / chip-like plate anchored to substrate nodes.
 */
data class GrowthPlate(
    val anchorNodeIds: List<String>,
    val opacity: Float,
    val age: Float,
)
