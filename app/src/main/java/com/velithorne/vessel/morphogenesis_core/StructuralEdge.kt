package com.velithorne.vessel.morphogenesis_core

enum class MorphEdgeKind {
    SIGNAL_CONDUIT,
    RESERVE_CONDUIT,
    SUPPORT_BRACE,
    REPAIR_REROUTE,
    SCAR_PATH,
}

data class StructuralEdge(
    val id: String,
    val kind: MorphEdgeKind,
    val fromId: String,
    val toId: String,
    val strength: Float,
)
