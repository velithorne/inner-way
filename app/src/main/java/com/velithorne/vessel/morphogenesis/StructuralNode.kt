package com.velithorne.vessel.morphogenesis

import com.velithorne.vessel.physiology.OrganType

enum class StructuralNodeKind {
    ORGAN,
    GROWTH_CENTER,
    ANCHOR_MASS,
    BRANCH_POINT,
    RESERVOIR_LOCUS,
    SHELL_CONTROL,
}

data class StructuralNode(
    val id: String,
    val kind: StructuralNodeKind,
    val organType: OrganType?,
    /** Normalized body space 0..1 */
    val nx: Float,
    val ny: Float,
    val influenceRadius: Float,
    val mass: Float,
)
