package com.velithorne.vessel.renderer_genesis

import com.velithorne.vessel.model.GeneratedAnatomyState
import com.velithorne.vessel.model.VesselPaletteState
import com.velithorne.vessel.model.VisibleMorphologyState
import com.velithorne.vessel.model.ContourGeometryState
import com.velithorne.vessel.model.BiographyVisualState
import com.velithorne.vessel.morphogenesis_core.GrowthPressureState

/**
 * Minimal state for genesis-only canvas — field + graph + contour geometry.
 */
data class GenesisSceneState(
    val palette: VesselPaletteState,
    val visible: VisibleMorphologyState,
    val anatomy: GeneratedAnatomyState?,
    val contourGeometry: ContourGeometryState,
    val biography: BiographyVisualState,
    val growthPressure: GrowthPressureState?,
    val animTimeSec: Float,
)
