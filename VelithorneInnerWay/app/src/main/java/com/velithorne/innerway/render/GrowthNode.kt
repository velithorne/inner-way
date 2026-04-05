package com.velithorne.innerway.render

/**
 * A point in the substrate coordinate system (0..1 in x and y).
 */
data class GrowthNode(
    val id: String,
    val x: Float,
    val y: Float,
    /** Local energy / metabolic emphasis (0..1). */
    val energy: Float,
    /** Seconds since creation (simulation age). */
    val age: Float,
    val type: NodeType,
    /** Growth tips that are still extending; stalled under stress. */
    val active: Boolean,
    /** Preferred growth direction for [NodeType.ACTIVE_TIP] (radians). */
    val directionRad: Float = 0f,
    val growthPhase: GrowthPhase = GrowthPhase.MATURE,
    /** 0..1 — formation / stabilization; seed/core starts at 1. */
    val growthProgress: Float = 1f,
    /** Wall-clock ms when created (for highlight fade). */
    val createdAt: Long = 0L,
)
