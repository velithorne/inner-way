package com.velithorne.vessel.morphogenesis

/**
 * Where tissue is actively depositing — renderer uses for edge shimmer / buds.
 */
enum class GrowthFrontType {
    BRANCHING,
    THICKENING,
    SWELLING,
    EMBEDDING,
    COOLING_VEIL,
}

data class GrowthFront(
    val activeIntensity: Float,
    /** Normalized direction hint in body space (e.g. lateral spread). */
    val directionX: Float,
    val directionY: Float,
    val primaryType: GrowthFrontType,
    val secondaryType: GrowthFrontType,
)
