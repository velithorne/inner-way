package com.velithorne.vessel.morphogenesis

/**
 * Normalized 2D body-space zones (0..1 x, 0..1 y). Cheap field for growth routing.
 */
enum class BodyZone {
    CORTICAL,
    CENTRAL_CHAMBER,
    LATERAL_SIGNAL_LEFT,
    LATERAL_SIGNAL_RIGHT,
    LOWER_ARCHIVE,
    PERIMETER_SHELL,
    SUPPORT_TENDON,
}

data class GrowthPressureField(
    val cortical: Float,
    val centralChamber: Float,
    val lateralSignal: Float,
    val lowerArchive: Float,
    val perimeterShell: Float,
    val supportTendon: Float,
) {
    fun zoneValue(zone: BodyZone): Float = when (zone) {
        BodyZone.CORTICAL -> cortical
        BodyZone.CENTRAL_CHAMBER -> centralChamber
        BodyZone.LATERAL_SIGNAL_LEFT, BodyZone.LATERAL_SIGNAL_RIGHT -> lateralSignal
        BodyZone.LOWER_ARCHIVE -> lowerArchive
        BodyZone.PERIMETER_SHELL -> perimeterShell
        BodyZone.SUPPORT_TENDON -> supportTendon
    }
}
