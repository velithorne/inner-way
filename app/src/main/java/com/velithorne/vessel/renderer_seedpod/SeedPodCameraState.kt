package com.velithorne.vessel.renderer_seedpod

/**
 * Camera for seed pod viewport — same combined model as legacy [com.velithorne.vessel.renderer.VesselCameraState]
 * but isolated so the Vessel tab never touches legacy vessel camera code.
 */
data class SeedPodCameraState(
    val basePanX: Float = 0f,
    val basePanY: Float = 0f,
    val fitZoom: Float = 1f,
    val userPanX: Float = 0f,
    val userPanY: Float = 0f,
    val userZoom: Float = 1f,
    val zoom: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val rotationDeg: Float = 0f,
    val tiltDeg: Float = 0f,
    val targetZoom: Float = zoom,
    val targetPanX: Float = panX,
    val targetPanY: Float = panY,
    val targetRotationDeg: Float = rotationDeg,
    val targetTiltDeg: Float = tiltDeg,
    val targetUserZoom: Float = userZoom,
    val targetUserPanX: Float = userPanX,
    val targetUserPanY: Float = userPanY,
) {
    fun effectiveZoom(tuning: SeedPodTuning): Float =
        (fitZoom * userZoom).coerceIn(tuning.minZoom, tuning.maxZoom)

    fun recomputeCombined(tuning: SeedPodTuning): SeedPodCameraState {
        val z = effectiveZoom(tuning)
        return copy(zoom = z, panX = basePanX + userPanX, panY = basePanY + userPanY)
    }

    fun clampUserZoom(tuning: SeedPodTuning): SeedPodCameraState {
        val minUz = tuning.minZoom / maxOf(fitZoom, 1e-3f)
        val maxUz = tuning.maxZoom / maxOf(fitZoom, 1e-3f)
        return copy(
            userZoom = userZoom.coerceIn(minUz, maxUz),
            targetUserZoom = targetUserZoom.coerceIn(minUz, maxUz),
            rotationDeg = rotationDeg.coerceIn(-tuning.maxRotationDeg, tuning.maxRotationDeg),
            targetRotationDeg = targetRotationDeg.coerceIn(-tuning.maxRotationDeg, tuning.maxRotationDeg),
            tiltDeg = tiltDeg.coerceIn(-tuning.maxTiltDeg, tuning.maxTiltDeg),
            targetTiltDeg = targetTiltDeg.coerceIn(-tuning.maxTiltDeg, tuning.maxTiltDeg),
        ).recomputeCombined(tuning)
    }

    companion object {
        fun default(tuning: SeedPodTuning) = SeedPodCameraState(
            fitZoom = tuning.defaultFitZoom,
            userZoom = 1f,
            targetUserZoom = 1f,
        ).clampUserZoom(tuning)
    }
}
