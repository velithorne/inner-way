package com.velithorne.vessel.model

import com.velithorne.vessel.morphogenesis_core.StructuralGraph

/**
 * UI-facing snapshot of procedural topology (graph summary + pressure channel read).
 */
data class GeneratedTopologyState(
    val nodeCount: Int,
    val edgeCount: Int,
    val rerouteCount: Int,
    val scarCount: Int,
    val maxPressureChannelLabel: String,
    val graph: StructuralGraph?,
)
