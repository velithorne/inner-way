package com.velithorne.vessel.morphogenesis_core

/**
 * Soft tie-break weights for graph/grammar decisions — from long-term ecology memory.
 */
data class LineagePersonality(
    val favorLateral: Float,
    val favorVertical: Float,
    val favorReserve: Float,
    val favorArchive: Float,
    val mutationTolerance: Float,
)
