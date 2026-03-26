package com.velithorne.vessel.renderer

import com.velithorne.vessel.physiology.OrganType

/**
 * Normalized organ knobs for drawing. Built from [com.velithorne.vessel.physiology.OrganState].
 * Future: tap targets and mutation-driven silhouette deltas attach here.
 */
data class OrganVisualModel(
    val type: OrganType,
    val anchorX: Float,
    val anchorY: Float,
    val baseRadius: Float,
    val glowIntensity: Float,
    val pulseCoupling: Float,
    val flickerIntensity: Float,
    val strain: Float,
    val densityLines: Float,
    val thermalCoupling: Float,
    val reserveLevel: Float,
    /** 0..1 — depth inside accreted tissue (higher = more embedded). */
    val tissueEmbedding: Float = 0.5f,
)
