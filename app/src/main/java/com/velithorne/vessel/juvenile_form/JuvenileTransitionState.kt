package com.velithorne.vessel.juvenile_form

/**
 * How far the specimen has transitioned from pod/chrysalis to juvenile body (0..1).
 */
data class JuvenileTransitionState(
    /** 0 = seed/pod read, 1 = juvenile body read. */
    val juvenileEmergence: Float,
    val topologyExpandedBeyondSeed: Boolean,
    val seedTraceOnlyLineageMemory: Boolean,
)
