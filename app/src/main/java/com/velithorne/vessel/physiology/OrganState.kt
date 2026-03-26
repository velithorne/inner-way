package com.velithorne.vessel.physiology

/**
 * Per-organ projection for diagnostics and future organ-scoped animation (Phase 3 renderer).
 */
data class OrganState(
    val organType: OrganType,
    val health: Float,
    val load: Float,
    val activity: Float,
    val inflammation: Float,
    val reserve: Float,
    val note: String,
)
