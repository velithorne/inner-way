package com.velithorne.vessel.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.velithorne.vessel.physiology.OrganType
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * **Core** specimen bounds: silhouette + organ anchors — drives default fit/centering.
 * **FX** bounds — for debug overlay only; must not shift default framing.
 */
data class CoreSpecimenBounds(
    val centroid: Offset,
    val halfWidth: Float,
    val halfHeight: Float,
)

data class FxEnvelopeBounds(
    val center: Offset,
    val radius: Float,
)

object VesselFraming {

    private val layout = VesselLayout()

    fun computeCoreSpecimenBounds(
        viewportW: Float,
        viewportH: Float,
        scene: VesselSceneState,
        parallax: Offset,
    ): CoreSpecimenBounds {
        val w = viewportW
        val h = viewportH
        val cx = w * layout.bodyCenterX + parallax.x * 0.15f
        val cy = h * layout.bodyCenterY + parallax.y * 0.12f
        val pulseApprox = 1f
        val breathApprox = 1f
        val baseW = w * layout.bodyWidth * scene.bodyScale * pulseApprox * breathApprox
        val baseH = h * layout.bodyHeight * scene.bodyScale * breathApprox * (0.98f + pulseApprox * 0.02f)

        val silhouetteSamples = listOf(
            Offset(cx, cy - baseH * 0.48f),
            Offset(cx, cy + baseH * 0.5f),
            Offset(cx - baseW * 0.42f, cy + baseH * 0.05f),
            Offset(cx + baseW * 0.42f, cy + baseH * 0.05f),
        )

        val organPoints = mutableListOf<Offset>()
        val seenLungs = mutableSetOf<String>()
        for (ov in scene.organVisuals) {
            if (ov.type == OrganType.THERMAL_MEMBRANE) continue
            if (ov.type == OrganType.SIGNAL_LUNGS) {
                val key = "${"%.4f".format(ov.anchorX)}_${"%.4f".format(ov.anchorY)}"
                if (!seenLungs.add(key)) continue
            }
            val ox = w * ov.anchorX + parallax.x * 0.12f * (0.6f + ov.baseRadius * 3f)
            val oy = h * ov.anchorY + parallax.y * 0.1f * (0.6f + ov.baseRadius * 3f)
            organPoints += Offset(ox, oy)
        }

        val allPts = silhouetteSamples + organPoints
        var sx = 0f
        var sy = 0f
        val n = max(1, allPts.size)
        for (p in allPts) {
            sx += p.x
            sy += p.y
        }
        var centroid = Offset(sx / n, sy / n)

        val organSpreadY = organPoints.map { abs(it.y - centroid.y) }.maxOrNull() ?: baseH * 0.35f
        val organSpreadX = organPoints.map { abs(it.x - centroid.x) }.maxOrNull() ?: baseW * 0.35f

        centroid = Offset(
            x = cx * 0.12f + centroid.x * 0.88f,
            y = (cy + baseH * tuningCoreLift(scene)) * 0.22f + centroid.y * 0.78f,
        )

        val halfW = max(baseW * 0.48f, organSpreadX + baseW * 0.08f)
        val halfH = max(baseH * 0.52f, organSpreadY + baseH * 0.06f)

        return CoreSpecimenBounds(centroid = centroid, halfWidth = halfW, halfHeight = halfH)
    }

    fun computeFxEnvelope(
        viewportW: Float,
        viewportH: Float,
        scene: VesselSceneState,
        parallax: Offset,
    ): FxEnvelopeBounds {
        val w = viewportW
        val h = viewportH
        val cx = w * layout.bodyCenterX + parallax.x * 0.15f
        val cy = h * layout.bodyCenterY + parallax.y * 0.12f
        val baseW = w * layout.bodyWidth * scene.bodyScale
        val baseH = h * layout.bodyHeight * scene.bodyScale
        val r = maxOf(baseW, baseH) * 0.72f * (1f + scene.feverIntensity * 0.15f)
        return FxEnvelopeBounds(Offset(cx, cy), r)
    }

    fun computeDefaultCamera(
        scene: VesselSceneState,
        viewportSize: Size,
        parallax: Offset,
        tuning: RenderTuning,
    ): VesselCameraState {
        val w = viewportSize.width.coerceAtLeast(1f)
        val h = viewportSize.height.coerceAtLeast(1f)
        val vc = Offset(w / 2f, h / 2f)
        val core = computeCoreSpecimenBounds(w, h, scene, parallax)

        val margin = tuning.defaultFramingMargin
        val zFitW = (w * (0.5f - margin)) / max(core.halfWidth, 1f)
        val zFitH = (h * (0.5f - margin)) / max(core.halfHeight, 1f)
        val fitRaw = min(zFitW, zFitH)
        val fitZoom = (fitRaw * tuning.defaultFramingFitMultiplier).coerceIn(
            tuning.minZoom,
            min(tuning.maxZoom, tuning.defaultFitZoomCap),
        )

        // Pan is in *specimen* space before scale: screen = vc + z * (specimen - vc + pan).
        // So to place core.centroid at (vc.x, targetY): pan = (target - vc) / z + vc - centroid.
        val targetY = h * tuning.defaultCompositionY
        val invZ = 1f / max(fitZoom, 1e-3f)
        val basePanX = vc.x - core.centroid.x
        val basePanY = (vc.y - core.centroid.y) + (targetY - vc.y) * invZ

        val c = VesselCameraState(
            basePanX = basePanX,
            basePanY = basePanY,
            fitZoom = fitZoom,
            userPanX = 0f,
            userPanY = 0f,
            userZoom = 1f,
            rotationDeg = 0f,
            tiltDeg = 0f,
            targetUserZoom = 1f,
            targetUserPanX = 0f,
            targetUserPanY = 0f,
            targetRotationDeg = 0f,
            targetTiltDeg = 0f,
        ).clampUserZoom(tuning).recomputeCombined(tuning)

        return c.copy(
            targetZoom = c.zoom,
            targetPanX = c.panX,
            targetPanY = c.panY,
        )
    }

    private fun tuningCoreLift(scene: VesselSceneState): Float =
        -(0.022f + scene.structuralMass * 0.012f)
}
