package com.velithorne.vessel.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.velithorne.vessel.physiology.OrganType
import kotlin.math.PI
import kotlin.math.abs

/**
 * Base framing from [VesselFraming]; user fields from gestures. Reset reapplies default fit.
 */
class VesselGestureController(
    private val tuning: RenderTuning,
) {
    var camera: VesselCameraState = VesselCameraState.default(tuning)
        private set

    /** Reset on next frame when parallax is available (used from UI callbacks). */
    private var pendingClearUserAndRefit: Boolean = false

    private fun copyTargetsFromCurrent(c: VesselCameraState) = c.copy(
        targetZoom = c.zoom,
        targetPanX = c.panX,
        targetPanY = c.panY,
        targetUserZoom = c.userZoom,
        targetUserPanX = c.userPanX,
        targetUserPanY = c.userPanY,
        targetRotationDeg = c.rotationDeg,
        targetTiltDeg = c.tiltDeg,
    )

    fun resetToDefault(
        scene: VesselSceneState,
        viewportW: Float,
        viewportH: Float,
        parallax: Offset,
    ) {
        camera = copyTargetsFromCurrent(
            VesselFraming.computeDefaultCamera(
                scene = scene,
                viewportSize = Size(viewportW, viewportH),
                parallax = parallax,
                tuning = tuning,
            ),
        )
    }

    /** Schedule neutral whole-specimen framing on the next draw (parallax from that frame). */
    fun requestResetNextFrame() {
        pendingClearUserAndRefit = true
    }

    fun consumePendingReset(
        scene: VesselSceneState,
        viewportW: Float,
        viewportH: Float,
        parallax: Offset,
    ): Boolean {
        if (!pendingClearUserAndRefit) return false
        pendingClearUserAndRefit = false
        resetToDefault(scene, viewportW, viewportH, parallax)
        return true
    }

    /**
     * While the user is not offsetting the camera, keep base fit aligned with scene + parallax
     * (whole-specimen default framing tracks telemetry tilt smoothly).
     */
    fun refreshBaseFramingIfNeutral(
        scene: VesselSceneState,
        viewportW: Float,
        viewportH: Float,
        parallax: Offset,
    ) {
        val c = camera
        val neutralUser = abs(c.userPanX) < 0.02f && abs(c.userPanY) < 0.02f &&
            abs(c.targetUserPanX) < 0.02f && abs(c.targetUserPanY) < 0.02f &&
            abs(c.userZoom - 1f) < 0.002f && abs(c.targetUserZoom - 1f) < 0.002f
        val neutralRot = abs(c.rotationDeg) < 0.02f && abs(c.tiltDeg) < 0.02f &&
            abs(c.targetRotationDeg) < 0.02f && abs(c.targetTiltDeg) < 0.02f
        if (!neutralUser || !neutralRot) return

        val next = VesselFraming.computeDefaultCamera(
            scene = scene,
            viewportSize = Size(viewportW, viewportH),
            parallax = parallax,
            tuning = tuning,
        )
        camera = c.copy(
            basePanX = next.basePanX,
            basePanY = next.basePanY,
            fitZoom = next.fitZoom,
        ).clampUserZoom(tuning).recomputeCombined(tuning).let(::copyTargetsFromCurrent)
    }

    fun setCamera(c: VesselCameraState) {
        camera = copyTargetsFromCurrent(c.clampUserZoom(tuning).recomputeCombined(tuning))
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
        val clamped = VesselHitTest.clampPan(panTotX, panTotY, viewportW, viewportH, tuning)
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
        camera = copyTargetsFromCurrent(next)
    }

    fun smoothTowardsTargets(viewportW: Float, viewportH: Float) {
        val a = tuning.cameraSmoothing.coerceIn(0.05f, 1f)
        val uz = camera.userZoom + (camera.targetUserZoom - camera.userZoom) * a
        var upx = camera.userPanX + (camera.targetUserPanX - camera.userPanX) * a
        var upy = camera.userPanY + (camera.targetUserPanY - camera.userPanY) * a
        val pr = camera.rotationDeg + (camera.targetRotationDeg - camera.rotationDeg) * a
        val pt = camera.tiltDeg + (camera.targetTiltDeg - camera.tiltDeg) * a
        var next = camera.copy(
            userZoom = uz,
            userPanX = upx,
            userPanY = upy,
            rotationDeg = pr,
            tiltDeg = pt,
        ).clampUserZoom(tuning).recomputeCombined(tuning)
        val clamped = VesselHitTest.clampPan(next.panX, next.panY, viewportW, viewportH, tuning)
        val ddx = clamped.first - next.panX
        val ddy = clamped.second - next.panY
        next = next.copy(
            userPanX = next.userPanX + ddx,
            userPanY = next.userPanY + ddy,
            targetUserPanX = next.targetUserPanX + ddx,
            targetUserPanY = next.targetUserPanY + ddy,
        ).recomputeCombined(tuning)
        camera = copyTargetsFromCurrent(next)
    }

    fun focusCameraOn(
        organ: OrganType,
        scene: VesselSceneState,
        parallax: Offset,
        viewportW: Float,
        viewportH: Float,
    ) {
        camera = copyTargetsFromCurrent(
            VesselHitTest.focusCameraOnOrgan(
                organ = organ,
                scene = scene,
                parallax = parallax,
                viewportW = viewportW,
                viewportH = viewportH,
                tuning = tuning,
                focusZoomTotal = tuning.focusZoom,
            ),
        )
    }

}
