package com.velithorne.vessel.morphogenesis

enum class StructuralEdgeKind {
    METABOLIC_CONDUIT,
    NEURAL_PATH,
    SIGNAL_BRANCH,
    SUPPORT_TENDON,
}

data class StructuralEdge(
    val id: String,
    val fromId: String,
    val toId: String,
    val kind: StructuralEdgeKind,
    val strength: Float,
    val branchIndex: Int = 0,
)
