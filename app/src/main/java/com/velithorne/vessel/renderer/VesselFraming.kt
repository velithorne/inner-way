package com.velithorne.vessel.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.velithorne.vessel.morphogenesis.StructuralGraph
import com.velithorne.vessel.morphogenesis.StructuralNodeKind
import com.velithorne.vessel.physiology.OrganType
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * **Core** specimen bounds for default fit/centering — uses **visual mass** centroid (seed + silhouette),
 * not raw organ-graph average (which pulled the read toward the lower body).
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
        val sp = scene.seedPlacement
        val cx = w * sp.anchorXNormalized + parallax.x * 0.15f
        val cy = h * sp.anchorYNormalized + parallax.y * 0.12f

        // Seed-first: fixed anchor + compact bounds — never pull framing from graph/contour mass.
        if (scene.seedFirstFramingActive) {
            val pulseApprox = 1f
            val breathApprox = 1f
            val baseW = w * layout.bodyWidth * scene.bodyScale * sp.seedViewportScale * pulseApprox * breathApprox
            val baseH = h * layout.bodyHeight * scene.bodyScale * sp.seedViewportScale * breathApprox * (0.98f + pulseApprox * 0.02f)
            val mode = scene.stageRenderMode
            val tight = when (mode) {
                VesselStageRenderMode.SEED_ONLY -> 0.22f
                VesselStageRenderMode.GERMINATING -> 0.28f
                VesselStageRenderMode.EARLY_BRANCHING -> 0.42f
                VesselStageRenderMode.MID_FORMATION -> 0.62f
                VesselStageRenderMode.ADVANCED_FORMATION -> 0.85f
            }
            val halfW = baseW * max(0.2f, tight)
            val halfH = baseH * max(0.22f, tight * 0.95f)
            return CoreSpecimenBounds(centroid = Offset(cx, cy), halfWidth = halfW, halfHeight = halfH)
        }

        val morphGraph: StructuralGraph? = scene.structuralGraph
        val pulseApprox = 1f
        val breathApprox = 1f
        val baseW = w * layout.bodyWidth * scene.bodyScale * pulseApprox * breathApprox
        val baseH = h * layout.bodyHeight * scene.bodyScale * breathApprox * (0.98f + pulseApprox * 0.02f)
        val seedBlend = scene.generated.seedFormBlend.coerceIn(0f, 1f)

        // Vesica / seed silhouette — weighted samples (top lobe + waist dominate visual mass; downweight tail apex).
        val silhouetteWeighted: List<Pair<Offset, Float>> = listOf(
            Offset(cx, cy - baseH * 0.42f) to 0.22f,
            Offset(cx - baseW * 0.18f, cy - baseH * 0.12f) to 0.14f,
            Offset(cx + baseW * 0.18f, cy - baseH * 0.12f) to 0.14f,
            Offset(cx, cy + baseH * 0.08f) to 0.18f,
            Offset(cx - baseW * 0.32f, cy + baseH * 0.06f) to 0.1f,
            Offset(cx + baseW * 0.32f, cy + baseH * 0.06f) to 0.1f,
            Offset(cx, cy + baseH * 0.38f) to 0.06f,
        )

        val organPoints = mutableListOf<Offset>()
        val organWeights = mutableListOf<Float>()
        val seenLungs = mutableSetOf<String>()
        if (morphGraph != null) {
            for (n in morphGraph.nodes) {
                if (n.kind != StructuralNodeKind.ORGAN || n.organType == OrganType.THERMAL_MEMBRANE) continue
                if (n.organType == OrganType.SIGNAL_LUNGS) {
                    val key = "${"%.4f".format(n.nx)}_${"%.4f".format(n.ny)}"
                    if (!seenLungs.add(key)) continue
                }
                val p = Offset(w * n.nx + parallax.x * 0.12f, h * n.ny + parallax.y * 0.1f)
                organPoints += p
                val ow = when (n.organType) {
                    OrganType.ARCHIVE_VAULT, OrganType.METABOLIC_HEART -> 0.85f
                    OrganType.CORTEX_CLUSTER, OrganType.NEURAL_GEL -> 1f
                    else -> 0.75f
                }
                organWeights += ow
            }
        } else {
            for (ov in scene.organVisuals) {
                if (ov.type == OrganType.THERMAL_MEMBRANE) continue
                if (ov.type == OrganType.SIGNAL_LUNGS) {
                    val key = "${"%.4f".format(ov.anchorX)}_${"%.4f".format(ov.anchorY)}"
                    if (!seenLungs.add(key)) continue
                }
                val p = Offset(
                    w * ov.anchorX + parallax.x * 0.12f * (0.6f + ov.baseRadius * 3f),
                    h * ov.anchorY + parallax.y * 0.1f * (0.6f + ov.baseRadius * 3f),
                )
                organPoints += p
                organWeights += 0.9f
            }
        }

        var sw = 0f
        var sx = 0f
        var sy = 0f
        for (pair in silhouetteWeighted) {
            val p = pair.first
            val wt = pair.second
            sw += wt
            sx += p.x * wt
            sy += p.y * wt
        }
        val organBlend = (0.18f + (1f - seedBlend) * 0.22f).coerceIn(0.12f, 0.38f)
        for (i in organPoints.indices) {
            val wt = organWeights[i] * organBlend
            sw += wt
            sx += organPoints[i].x * wt
            sy += organPoints[i].y * wt
        }
        if (sw < 1e-3f) sw = 1f
        var centroid = Offset(sx / sw, sy / sw)

        // Seed nucleus pulls visual mass slightly toward core (not fog/particles).
        val nucleusY = cy - baseH * 0.02f * seedBlend
        centroid = Offset(
            centroid.x * 0.88f + cx * 0.08f,
            centroid.y * 0.82f + nucleusY * 0.18f,
        )

        // Lift suspended specimen: vesica mass reads lower than path centroid — bias upward (smaller Y).
        val suspendedLift = baseH * (0.065f + scene.growthVisuals.stageVisualBias * 0.025f)
        centroid = Offset(centroid.x, centroid.y - suspendedLift)

        val organSpreadY = if (organPoints.isEmpty()) baseH * 0.32f else organPoints.map { abs(it.y - centroid.y) }.maxOrNull() ?: baseH * 0.32f
        val organSpreadX = if (organPoints.isEmpty()) baseW * 0.32f else organPoints.map { abs(it.x - centroid.x) }.maxOrNull() ?: baseW * 0.32f

        val halfW = max(baseW * 0.46f, organSpreadX + baseW * 0.1f)
        val halfH = max(baseH * 0.5f, organSpreadY + baseH * 0.12f)

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
        val sp = scene.seedPlacement
        val cx = w * (if (scene.seedFirstFramingActive) sp.anchorXNormalized else layout.bodyCenterX) + parallax.x * 0.15f
        val cy = h * (if (scene.seedFirstFramingActive) sp.anchorYNormalized else layout.bodyCenterY) + parallax.y * 0.12f
        val scale = if (scene.seedFirstFramingActive) sp.seedViewportScale else 1f
        val baseW = w * layout.bodyWidth * scene.bodyScale * scale
        val baseH = h * layout.bodyHeight * scene.bodyScale * scale
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

        val targetY = h * if (scene.seedFirstFramingActive) {
            scene.seedPlacement.anchorYNormalized
        } else {
            tuning.defaultVisualCentroidTargetY
        }
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
}
