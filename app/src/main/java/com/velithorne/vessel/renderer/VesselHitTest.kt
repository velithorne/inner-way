package com.velithorne.vessel.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.velithorne.vessel.physiology.OrganType
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

/** Screen-space ↔ specimen space (matches [VesselPainter] camera stack including base+user pan). */
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
        val panX = camera.panX
        val panY = camera.panY

        val p = tapCanvas - renderOffset - vc
        val prx = p.x * c - p.y * s
        val pry = p.x * s + p.y * c
        val u = prx / z + vc.x - panX
        val v = pry / z + vc.y - panY
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

    /**
     * Centers [organ] at viewport center at total zoom [focusZoomTotal].
     * Refreshes base framing from core fit, then sets user pan/zoom relative to that baseline.
     */
    fun focusCameraOnOrgan(
        organ: OrganType,
        scene: VesselSceneState,
        parallax: Offset,
        viewportW: Float,
        viewportH: Float,
        tuning: RenderTuning,
        focusZoomTotal: Float,
        layout: VesselLayout = VesselLayout(),
    ): VesselCameraState {
        val base = VesselFraming.computeDefaultCamera(
            scene = scene,
            viewportSize = Size(viewportW, viewportH),
            parallax = parallax,
            tuning = tuning,
        )
        val basis = computeBasis(viewportW, viewportH, scene, parallax, layout)
        val (ox, oy) = when (organ) {
            OrganType.THERMAL_MEMBRANE -> basis.centerX to basis.centerY
            else -> {
                val ov = scene.organVisuals.firstOrNull { it.type == organ }
                    ?: return base
                val x = viewportW * ov.anchorX + parallax.x * 0.12f * (0.6f + ov.baseRadius * 3f)
                val y = viewportH * ov.anchorY + parallax.y * 0.1f * (0.6f + ov.baseRadius * 3f)
                x to y
            }
        }
        val z = focusZoomTotal.coerceIn(tuning.minZoom, tuning.maxZoom)
        val invZ = 1f / max(z, 1e-3f)
        val vpCx = viewportW / 2f
        val vpCy = viewportH / 2f
        val vcX = viewportW / 2f
        val vcY = viewportH / 2f
        var panTotX = (vcX - ox) + (vpCx - vcX) * invZ
        var panTotY = (vcY - oy) + (vpCy - vcY) * invZ
        val clamped = clampPan(panTotX, panTotY, viewportW, viewportH, tuning)
        panTotX = clamped.first
        panTotY = clamped.second
        val uz = z / maxOf(base.fitZoom, 1e-3f)
        val upx = panTotX - base.basePanX
        val upy = panTotY - base.basePanY
        return base.copy(
            userZoom = uz,
            userPanX = upx,
            userPanY = upy,
            targetUserZoom = uz,
            targetUserPanX = upx,
            targetUserPanY = upy,
            rotationDeg = 0f,
            tiltDeg = 0f,
            targetRotationDeg = 0f,
            targetTiltDeg = 0f,
        ).clampUserZoom(tuning).recomputeCombined(tuning)
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
