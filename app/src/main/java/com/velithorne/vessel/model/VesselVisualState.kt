package com.velithorne.vessel.model

import com.velithorne.vessel.renderer.VesselSceneState

/**
 * UI-facing bundle for the vessel experience (scene + optional future HUD extras).
 * Phase 3 keeps this as a thin wrapper for cleaner ViewModel APIs.
 */
data class VesselVisualState(
    val scene: VesselSceneState,
)
