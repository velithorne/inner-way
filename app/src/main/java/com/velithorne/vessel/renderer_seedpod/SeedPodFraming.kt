package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

/**
 * Default framing: **zero** base pan — the pod is drawn at [SeedPodLayout] anchor; reset = identity user transform.
 * Does **not** use legacy [com.velithorne.vessel.renderer.VesselFraming] or contour fitting.
 */
object SeedPodFraming {

    fun podCenterPx(viewportW: Float, viewportH: Float, parallax: Offset): Offset =
        Offset(
            viewportW * SeedPodLayout.anchorXNormalized + parallax.x * 0.12f,
            viewportH * SeedPodLayout.anchorYNormalized + parallax.y * 0.1f,
        )

    fun computeDefaultCamera(
        viewportSize: Size,
        parallax: Offset,
        tuning: SeedPodTuning,
    ): SeedPodCameraState {
        // Pod stays at fixed anchor; no centroid fit — base pan 0, fit zoom 1.
        return SeedPodCameraState(
            basePanX = 0f,
            basePanY = 0f,
            fitZoom = tuning.defaultFitZoom,
            userPanX = 0f,
            userPanY = 0f,
            userZoom = 1f,
            rotationDeg = 0f,
            tiltDeg = 0f,
            targetUserPanX = 0f,
            targetUserPanY = 0f,
            targetUserZoom = 1f,
            targetRotationDeg = 0f,
            targetTiltDeg = 0f,
        ).clampUserZoom(tuning).let { c ->
            c.copy(
                targetZoom = c.zoom,
                targetPanX = c.panX,
                targetPanY = c.panY,
            )
        }
    }
}
