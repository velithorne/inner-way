package com.velithorne.vessel.renderer

import com.velithorne.vessel.physiology.OrganType

/**
 * Organ focus for inspection UI + painter highlights.
 * [focusProgress] 0..1 eases ring emphasis (driven per-frame in gesture controller).
 */
data class VesselSelectionState(
    val selectedOrgan: OrganType? = null,
    val isSheetVisible: Boolean = false,
    val focusProgress: Float = 0f,
)
