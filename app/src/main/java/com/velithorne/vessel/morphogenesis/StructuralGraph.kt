package com.velithorne.vessel.morphogenesis

/**
 * Procedural vessel topology — organs + shell anchors + support loci.
 */
data class StructuralGraph(
    val nodes: List<StructuralNode>,
    val edges: List<StructuralEdge>,
)
