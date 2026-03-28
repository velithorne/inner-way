package com.velithorne.vessel.model

/**
 * How far silhouette has diverged from seed oval / pod frame.
 */
data class TopologyExpansionState(
    val expansionFactor: Float,
    val upperBias: Float,
    val lowerBias: Float,
    val lateralAsymmetry: Float,
)
