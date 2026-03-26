package com.velithorne.vessel.renderer

/**
 * Inspection camera for the specimen viewport. Values are in **screen pixels** (pan),
 * **scale** (zoom), and **degrees** (rotation/tilt).
 *
 * Smoothing targets optional for gesture-end physics (updated in [VesselGestureController]).
 */
data class VesselCameraState(
    val zoom: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f,
    /** In-plane specimen rotation (limited 2.5D). */
    val rotationDeg: Float = 0f,
    /** Secondary tilt bias (smaller effective coupling in painter). */
    val tiltDeg: Float = 0f,
    val targetZoom: Float = zoom,
    val targetPanX: Float = panX,
    val targetPanY: Float = panY,
    val targetRotationDeg: Float = rotationDeg,
    val targetTiltDeg: Float = tiltDeg,
) {
    fun clamped(tuning: RenderTuning): VesselCameraState {
        val z = zoom.coerceIn(tuning.minZoom, tuning.maxZoom)
        val tz = targetZoom.coerceIn(tuning.minZoom, tuning.maxZoom)
        val r = rotationDeg.coerceIn(-tuning.maxRotationDeg, tuning.maxRotationDeg)
        val tr = targetRotationDeg.coerceIn(-tuning.maxRotationDeg, tuning.maxRotationDeg)
        val t = tiltDeg.coerceIn(-tuning.maxTiltDeg, tuning.maxTiltDeg)
        val tt = targetTiltDeg.coerceIn(-tuning.maxTiltDeg, tuning.maxTiltDeg)
        return copy(
            zoom = z,
            targetZoom = tz,
            rotationDeg = r,
            targetRotationDeg = tr,
            tiltDeg = t,
            targetTiltDeg = tt,
        )
    }

    companion object {
        fun default(tuning: RenderTuning) = VesselCameraState(
            zoom = tuning.defaultZoom,
            targetZoom = tuning.defaultZoom,
        )
    }
}
