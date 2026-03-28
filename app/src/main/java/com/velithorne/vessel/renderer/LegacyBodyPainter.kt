package com.velithorne.vessel.renderer

/**
 * Legacy full-body scaffold (rear mass, shell, pathways, organs) is drawn from [VesselPainter]
 * with [VesselSceneState.legacyScaffoldVisibility] — not a separate class path.
 *
 * Early [VesselStageRenderMode] values keep this near zero so the seed remains the visual root.
 */
object LegacyBodyPainter
