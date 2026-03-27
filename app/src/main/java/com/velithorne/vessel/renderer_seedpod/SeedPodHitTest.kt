package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.geometry.Offset
import kotlin.math.hypot
import kotlin.math.min

/** Screen ↔ pod plane for the seed pod renderer only. */
object SeedPodHitTest {

    fun clampPan(panX: Float, panY: Float, viewportW: Float, viewportH: Float, tuning: SeedPodTuning): Pair<Float, Float> {
        val maxX = viewportW * tuning.maxPanFraction
        val maxY = viewportH * tuning.maxPanFraction
        return Pair(
            panX.coerceIn(-maxX, maxX),
            panY.coerceIn(-maxY, maxY),
        )
    }

    fun screenToPodPlane(
        tapCanvas: Offset,
        camera: SeedPodCameraState,
        tuning: SeedPodTuning,
        viewportW: Float,
        viewportH: Float,
        stressShiverDeg: Float,
    ): Offset {
        val vc = Offset(viewportW / 2f, viewportH / 2f)
        val rotDeg = camera.rotationDeg + camera.tiltDeg * 0.35f + stressShiverDeg * 0.25f
        val rad = Math.toRadians(-rotDeg.toDouble()).toFloat()
        val c = kotlin.math.cos(rad.toDouble()).toFloat()
        val s = kotlin.math.sin(rad.toDouble()).toFloat()
        val z = camera.zoom.coerceIn(tuning.minZoom, tuning.maxZoom)
        val p = tapCanvas - vc
        val prx = p.x * c - p.y * s
        val pry = p.x * s + p.y * c
        val u = prx / z + vc.x - camera.panX
        val v = pry / z + vc.y - camera.panY
        return Offset(u, v)
    }

    /**
     * @return "pod" for nucleus/shell hit, "thermal" for outer veil, null if miss
     */
    fun hitTarget(
        tapCanvas: Offset,
        camera: SeedPodCameraState,
        tuning: SeedPodTuning,
        viewportW: Float,
        viewportH: Float,
        parallax: Offset,
        stressShiverDeg: Float,
    ): String? {
        if (viewportW <= 1f) return null
        val p = screenToPodPlane(tapCanvas, camera, tuning, viewportW, viewportH, stressShiverDeg)
        val pod = SeedPodFraming.podCenterPx(viewportW, viewportH, parallax)
        val d = hypot(p.x - pod.x, p.y - pod.y)
        val minDim = min(viewportW, viewportH)
        val rCore = minDim * tuning.podCoreRadiusMul * 2.2f
        val rShell = minDim * tuning.podShellRadiusMul * 2.4f
        val rVeil = minDim * 0.22f
        return when {
            d <= rCore -> "pod"
            d <= rShell -> "pod"
            d <= rVeil -> "thermal"
            else -> null
        }
    }
}
