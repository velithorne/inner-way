package com.velithorne.innerway.render

/**
 * Immutable snapshot of the living growth graph (in-memory for app session).
 */
data class GrowthState(
    val nodes: List<GrowthNode>,
    val edges: List<GrowthEdge>,
    val plates: List<GrowthPlate>,
)
