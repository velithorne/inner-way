package com.velithorne.vessel.renderer

import com.velithorne.vessel.physiology.OrganType

/**
 * Normalized body space (0..1), origin top-left of chamber viewport.
 * Coarse profile — tweak anchors for alternate species silhouettes (evolution phase).
 */
data class VesselLayout(
    val bodyCenterX: Float = 0.5f,
    val bodyCenterY: Float = 0.52f,
    val bodyWidth: Float = 0.42f,
    val bodyHeight: Float = 0.72f,
    val anchors: Map<OrganType, OrganAnchor> = defaultAnchors(),
) {
    data class OrganAnchor(
        val x: Float,
        val y: Float,
        val radius: Float,
    )

    companion object {
        fun defaultAnchors(): Map<OrganType, OrganAnchor> = mapOf(
            OrganType.METABOLIC_HEART to OrganAnchor(0.5f, 0.62f, 0.07f),
            OrganType.CORTEX_CLUSTER to OrganAnchor(0.5f, 0.32f, 0.09f),
            OrganType.NEURAL_GEL to OrganAnchor(0.5f, 0.38f, 0.12f),
            OrganType.ARCHIVE_VAULT to OrganAnchor(0.5f, 0.76f, 0.11f),
            OrganType.SIGNAL_LUNGS to OrganAnchor(0.35f, 0.48f, 0.065f), // left; mirror for right
            OrganType.VESTIBULAR_MUSCULATURE to OrganAnchor(0.5f, 0.52f, 0.14f),
            OrganType.THERMAL_MEMBRANE to OrganAnchor(0.5f, 0.5f, 0.5f),
        )
    }
}
