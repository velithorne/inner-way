package com.velithorne.vessel.renderer

/**
 * Chamber camera: **base** framing (layout fit) + **user** gesture offsets.
 * Final transform: `effectiveZoom = fitZoom * userZoom`, `effectivePan = basePan + userPan`.
 */
data class VesselCameraState(
    /** Pan in chamber px so centroid sits at composition line after zoom (neutral user). */
    val basePanX: Float = 0f,
    val basePanY: Float = 0f,
    /** Auto-fit zoom from core bounds only (before user pinch). */
    val fitZoom: Float = 1f,

    val userPanX: Float = 0f,
    val userPanY: Float = 0f,
    /** Multiplier relative to [fitZoom] (1 = default fit). */
    val userZoom: Float = 1f,

    /** Legacy combined (kept in sync via [recomputeCombined]). */
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
    fun effectiveZoom(tuning: RenderTuning): Float =
        (fitZoom * userZoom).coerceIn(tuning.minZoom, tuning.maxZoom)

    fun recomputeCombined(tuning: RenderTuning): VesselCameraState {
        val z = effectiveZoom(tuning)
        val px = basePanX + userPanX
        val py = basePanY + userPanY
        return copy(zoom = z, panX = px, panY = py)
    }

    fun clampUserZoom(tuning: RenderTuning): VesselCameraState {
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
        fun default(tuning: RenderTuning) = VesselCameraState(
            fitZoom = tuning.defaultZoom,
            userZoom = 1f,
            targetUserZoom = 1f,
        ).recomputeCombined(tuning)
    }
}
