package com.velithorne.vessel.renderer

import androidx.compose.ui.geometry.Offset
import com.velithorne.vessel.physiology.OrganType
import kotlin.math.PI
import kotlin.math.hypot

/**
 * Camera + pinch/pan/rotate for the specimen viewport. Selection lives in ViewModel.
 */
class VesselGestureController(
    private val tuning: RenderTuning,
) {
    var camera: VesselCameraState = VesselCameraState.default(tuning)
        private set

    fun resetCamera() {
        camera = VesselCameraState.default(tuning)
    }

    fun setCamera(c: VesselCameraState) {
        camera = c.clamped(tuning)
    }

    /**
     * Compose [detectTransformGestures]: incremental pan, multiplicative zoom, rotation radians.
     */
    fun applyTransform(
        pan: Offset,
        zoomFactor: Float,
        rotationRad: Float,
        viewportW: Float,
        viewportH: Float,
    ) {
        val z = (camera.zoom * zoomFactor).coerceIn(tuning.minZoom, tuning.maxZoom)
        val (px, py) = VesselHitTest.clampPan(
            camera.panX + pan.x,
            camera.panY + pan.y,
            viewportW,
            viewportH,
            tuning,
        )
        val rotDeg = camera.rotationDeg + (rotationRad * (180.0 / PI).toFloat()) * 0.42f
        val r = rotDeg.coerceIn(-tuning.maxRotationDeg, tuning.maxRotationDeg)
        val tiltDelta = pan.y * 0.035f + (zoomFactor - 1f) * 2.8f
        val t = (camera.tiltDeg + tiltDelta).coerceIn(-tuning.maxTiltDeg, tuning.maxTiltDeg)
        camera = camera.copy(
            zoom = z,
            targetZoom = z,
            panX = px,
            panY = py,
            targetPanX = px,
            targetPanY = py,
            rotationDeg = r,
            targetRotationDeg = r,
            tiltDeg = t,
            targetTiltDeg = t,
        )
    }

    fun smoothTowardsTargets(viewportW: Float, viewportH: Float) {
        val a = tuning.cameraSmoothing.coerceIn(0.05f, 1f)
        val z = camera.zoom + (camera.targetZoom - camera.zoom) * a
        var px = camera.panX + (camera.targetPanX - camera.panX) * a
        var py = camera.panY + (camera.targetPanY - camera.panY) * a
        val pr = camera.rotationDeg + (camera.targetRotationDeg - camera.rotationDeg) * a
        val pt = camera.tiltDeg + (camera.targetTiltDeg - camera.tiltDeg) * a
        val clamped = VesselHitTest.clampPan(px, py, viewportW, viewportH, tuning)
        px = clamped.first
        py = clamped.second
        camera = camera.copy(
            zoom = z.coerceIn(tuning.minZoom, tuning.maxZoom),
            panX = px,
            panY = py,
            rotationDeg = pr,
            tiltDeg = pt,
        )
    }

    fun focusCameraOn(
        organ: OrganType,
        scene: VesselSceneState,
        parallax: Offset,
        viewportW: Float,
        viewportH: Float,
    ) {
        val next = VesselHitTest.focusCameraOnOrgan(
            organ = organ,
            scene = scene,
            parallax = parallax,
            viewportW = viewportW,
            viewportH = viewportH,
            tuning = tuning,
            zoom = tuning.focusZoom,
        )
        camera = camera.copy(
            zoom = next.zoom,
            targetZoom = next.targetZoom,
            panX = next.panX,
            panY = next.panY,
            targetPanX = next.targetPanX,
            targetPanY = next.targetPanY,
            rotationDeg = next.rotationDeg,
            tiltDeg = next.tiltDeg,
            targetRotationDeg = next.targetRotationDeg,
            targetTiltDeg = next.targetTiltDeg,
        )
    }

    companion object {
        fun span(pointers: List<Offset>): Float {
            if (pointers.size < 2) return 1f
            return hypot(pointers[1].x - pointers[0].x, pointers[1].y - pointers[0].y).coerceAtLeast(1f)
        }
    }
}
