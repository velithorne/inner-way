package com.velithorne.vessel.renderer

import androidx.compose.ui.geometry.Offset
import com.velithorne.vessel.physiology.OrganType
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** Screen-space ↔ specimen space for organ taps (matches [VesselPainter] camera stack). */
object VesselHitTest {

    data class BodyBasis(
        val centerX: Float,
        val centerY: Float,
        val baseW: Float,
        val baseH: Float,
    )

    fun computeBasis(
        viewportW: Float,
        viewportH: Float,
        scene: VesselSceneState,
        parallax: Offset,
        layout: VesselLayout = VesselLayout(),
    ): BodyBasis {
        val w = viewportW
        val h = viewportH
        val cx = w * layout.bodyCenterX + parallax.x * 0.15f
        val cy = h * layout.bodyCenterY + parallax.y * 0.12f
        val pulseApprox = 1f
        val breathApprox = 1f
        val baseW = w * layout.bodyWidth * scene.bodyScale * pulseApprox * breathApprox
        val baseH = h * layout.bodyHeight * scene.bodyScale * breathApprox * (0.98f + pulseApprox * 0.02f)
        return BodyBasis(cx, cy, baseW, baseH)
    }

    /** Map canvas tap to organ space (same coords as [VesselPainter] cx, cy, organ anchors). */
    fun screenToSpecimenPlane(
        tapCanvas: Offset,
        renderOffset: Offset,
        camera: VesselCameraState,
        scene: VesselSceneState,
        tuning: RenderTuning,
        viewportW: Float,
        viewportH: Float,
    ): Offset {
        val vc = Offset(viewportW / 2f, viewportH / 2f)
        val rotDeg = camera.rotationDeg + camera.tiltDeg * 0.35f +
            scene.stressShiver * tuning.stressShiverDegrees * 0.25f
        val rad = Math.toRadians(-rotDeg.toDouble()).toFloat()
        val c = cos(rad)
        val s = sin(rad)
        val z = camera.zoom.coerceIn(tuning.minZoom, tuning.maxZoom)

        val p = tapCanvas - renderOffset - vc
        val prx = p.x * c - p.y * s
        val pry = p.x * s + p.y * c
        val u = prx / z + vc.x - camera.panX
        val v = pry / z + vc.y - camera.panY
        return Offset(u, v)
    }

    fun hitOrgan(
        tapCanvas: Offset,
        renderOffset: Offset,
        scene: VesselSceneState,
        camera: VesselCameraState,
        parallax: Offset,
        viewportW: Float,
        viewportH: Float,
        tuning: RenderTuning,
        layout: VesselLayout = VesselLayout(),
    ): OrganType? {
        if (viewportW <= 1f || viewportH <= 1f) return null
        val p = screenToSpecimenPlane(tapCanvas, renderOffset, camera, scene, tuning, viewportW, viewportH)
        val w = viewportW
        val h = viewportH

        val candidates = mutableListOf<Pair<OrganType, Float>>()

        for (ov in scene.organVisuals) {
            val ox = w * ov.anchorX + parallax.x * 0.12f * (0.6f + ov.baseRadius * 3f)
            val oy = h * ov.anchorY + parallax.y * 0.1f * (0.6f + ov.baseRadius * 3f)
            val hitR = w * ov.baseRadius * tuning.organHitPadMultiplier
            val d = hypot(p.x - ox, p.y - oy)
            if (d <= hitR) {
                candidates += ov.type to d
            }
        }

        val basis = computeBasis(viewportW, viewportH, scene, parallax, layout)
        val maxR = maxOf(basis.baseW, basis.baseH) * 0.55f * tuning.thermalHitFullBodyMultiplier
        val dTherm = hypot(p.x - basis.centerX, p.y - basis.centerY)
        if (dTherm <= maxR) {
            candidates += OrganType.THERMAL_MEMBRANE to (dTherm + maxR * 0.12f)
        }

        if (candidates.isEmpty()) return null
        return candidates.minByOrNull { it.second }?.first
    }

    fun focusCameraOnOrgan(
        organ: OrganType,
        scene: VesselSceneState,
        parallax: Offset,
        viewportW: Float,
        viewportH: Float,
        tuning: RenderTuning,
        zoom: Float = tuning.focusZoom,
        layout: VesselLayout = VesselLayout(),
    ): VesselCameraState {
        val basis = computeBasis(viewportW, viewportH, scene, parallax, layout)
        val w = viewportW
        val h = viewportH
        val (ox, oy) = when (organ) {
            OrganType.THERMAL_MEMBRANE -> basis.centerX to basis.centerY
            else -> {
                val ov = scene.organVisuals.firstOrNull { it.type == organ }
                    ?: return VesselCameraState.default(tuning)
                val x = w * ov.anchorX + parallax.x * 0.12f * (0.6f + ov.baseRadius * 3f)
                val y = h * ov.anchorY + parallax.y * 0.1f * (0.6f + ov.baseRadius * 3f)
                x to y
            }
        }
        val z = zoom.coerceIn(tuning.minZoom, tuning.maxZoom)
        val vpCx = viewportW / 2f
        val vpCy = viewportH / 2f
        val panX = vpCx - (ox - basis.centerX) * z - basis.centerX
        val panY = vpCy - (oy - basis.centerY) * z - basis.centerY
        val (clampedPanX, clampedPanY) = clampPan(panX, panY, viewportW, viewportH, tuning)
        return VesselCameraState(
            zoom = z,
            targetZoom = z,
            panX = clampedPanX,
            panY = clampedPanY,
            targetPanX = clampedPanX,
            targetPanY = clampedPanY,
            rotationDeg = 0f,
            tiltDeg = 0f,
            targetRotationDeg = 0f,
            targetTiltDeg = 0f,
        )
    }

    fun clampPan(
        panX: Float,
        panY: Float,
        viewportW: Float,
        viewportH: Float,
        tuning: RenderTuning,
    ): Pair<Float, Float> {
        val maxX = viewportW * tuning.maxPanFraction
        val maxY = viewportH * tuning.maxPanFraction
        return panX.coerceIn(-maxX, maxX) to panY.coerceIn(-maxY, maxY)
    }
}
