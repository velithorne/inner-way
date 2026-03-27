package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Gestures for seed pod chamber — **not** [com.velithorne.vessel.renderer.VesselGestureController].
 */
class SeedPodGestureController(
    private val tuning: SeedPodTuning,
) {
    var camera: SeedPodCameraState = SeedPodCameraState.default(tuning)
        private set

    private var pendingReset: Boolean = false

    private fun copyTargets(c: SeedPodCameraState) = c.copy(
        targetZoom = c.zoom,
        targetPanX = c.panX,
        targetPanY = c.panY,
        targetUserZoom = c.userZoom,
        targetUserPanX = c.userPanX,
        targetUserPanY = c.userPanY,
        targetRotationDeg = c.rotationDeg,
        targetTiltDeg = c.tiltDeg,
    )

    fun resetToDefault(viewportW: Float, viewportH: Float, parallax: Offset) {
        camera = copyTargets(
            SeedPodFraming.computeDefaultCamera(
                viewportSize = Size(viewportW, viewportH),
                parallax = parallax,
                tuning = tuning,
            ),
        )
    }

    fun requestResetNextFrame() {
        pendingReset = true
    }

    fun consumePendingReset(viewportW: Float, viewportH: Float, parallax: Offset): Boolean {
        if (!pendingReset) return false
        pendingReset = false
        resetToDefault(viewportW, viewportH, parallax)
        return true
    }

    fun applyTransform(
        pan: Offset,
        zoomFactor: Float,
        rotationRad: Float,
        viewportW: Float,
        viewportH: Float,
    ) {
        val nextUz = camera.targetUserZoom * zoomFactor
        val r = (camera.targetRotationDeg + (rotationRad * (180.0 / PI).toFloat()) * 0.42f)
            .coerceIn(-tuning.maxRotationDeg, tuning.maxRotationDeg)
        val tiltDelta = pan.y * 0.035f + (zoomFactor - 1f) * 2.8f
        val t = (camera.targetTiltDeg + tiltDelta).coerceIn(-tuning.maxTiltDeg, tuning.maxTiltDeg)
        var tpx = camera.targetUserPanX + pan.x
        var tpy = camera.targetUserPanY + pan.y
        val panTotX = camera.basePanX + tpx
        val panTotY = camera.basePanY + tpy
        val clamped = SeedPodHitTest.clampPan(panTotX, panTotY, viewportW, viewportH, tuning)
        tpx = clamped.first - camera.basePanX
        tpy = clamped.second - camera.basePanY
        var next = camera.copy(
            targetUserZoom = nextUz,
            targetUserPanX = tpx,
            targetUserPanY = tpy,
            targetRotationDeg = r,
            targetTiltDeg = t,
        ).clampUserZoom(tuning)
        next = next.copy(
            userZoom = next.targetUserZoom,
            userPanX = next.targetUserPanX,
            userPanY = next.targetUserPanY,
            rotationDeg = next.targetRotationDeg,
            tiltDeg = next.targetTiltDeg,
        ).recomputeCombined(tuning)
        camera = copyTargets(next)
    }

    fun smoothTowardsTargets(viewportW: Float, viewportH: Float) {
        val a = tuning.cameraSmoothing.coerceIn(0.05f, 1f)
        val uz = camera.userZoom + (camera.targetUserZoom - camera.userZoom) * a
        val upx = camera.userPanX + (camera.targetUserPanX - camera.userPanX) * a
        val upy = camera.userPanY + (camera.targetUserPanY - camera.userPanY) * a
        val pr = camera.rotationDeg + (camera.targetRotationDeg - camera.rotationDeg) * a
        val pt = camera.tiltDeg + (camera.targetTiltDeg - camera.tiltDeg) * a
        var next = camera.copy(
            userZoom = uz,
            userPanX = upx,
            userPanY = upy,
            rotationDeg = pr,
            tiltDeg = pt,
        ).clampUserZoom(tuning).recomputeCombined(tuning)
        val clamped = SeedPodHitTest.clampPan(next.panX, next.panY, viewportW, viewportH, tuning)
        val ddx = clamped.first - next.panX
        val ddy = clamped.second - next.panY
        next = next.copy(
            userPanX = next.userPanX + ddx,
            userPanY = next.userPanY + ddy,
            targetUserPanX = next.targetUserPanX + ddx,
            targetUserPanY = next.targetUserPanY + ddy,
        ).recomputeCombined(tuning)
        camera = copyTargets(next)
    }
}
