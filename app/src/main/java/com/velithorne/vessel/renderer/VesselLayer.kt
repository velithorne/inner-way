package com.velithorne.vessel.renderer

/**
 * Back-to-front draw order for the specimen chamber.
 */
enum class VesselLayer(val zIndex: Int) {
    CHAMBER_BACK(0),
    REAR_DEPTH(1),
    OUTER_MEMBRANE(2),
    ORGANS(3),
    VASCULAR(4),
    ATMOSPHERE(5),
    HUD_GLASS(6),
}
