package com.velithorne.vessel.morphogenesis

/**
 * Serializable-friendly growth milestone (for future Room).
 */
data class GrowthEvent(
    val timestampMillis: Long,
    val kind: String,
    val detail: String,
    val pressureSnapshot: String,
)
