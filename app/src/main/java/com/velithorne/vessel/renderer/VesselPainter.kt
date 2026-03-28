package com.velithorne.vessel.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.velithorne.vessel.physiology.OrganType
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.max
import kotlin.math.sin

/**
 * Specimen orchestration: contour, membrane, pathways, organs, thermal.
 * Chamber backdrop and glass are composed in [VesselScene].
 */
object VesselPainter {

    private val layout = VesselLayout()

    fun draw(
        scope: DrawScope,
        scene: VesselSceneState,
        anim: VesselAnimationController,
        tuning: RenderTuning,
        parallax: Offset,
        particles: List<ParticleDraw>,
        camera: VesselCameraState,
        selection: VesselSelectionState,
        renderOffset: Offset,
    ) {
        val w = scope.size.width
        val h = scope.size.height
        val vc = Offset(w / 2f, h / 2f)
        val pulse = anim.pulsePhase(tuning.pulseFrequencyHz)
        val breath = anim.pulsePhase(tuning.breathFrequencyHz)

        scope.translate(renderOffset.x, renderOffset.y) {
            translate(vc.x, vc.y) {
                rotate(
                    degrees = (camera.rotationDeg + camera.tiltDeg * 0.35f + scene.stressShiver * tuning.stressShiverDegrees * 0.25f),
                    pivot = Offset.Zero,
                ) {
                    scale(camera.zoom.coerceIn(tuning.minZoom, tuning.maxZoom), camera.zoom.coerceIn(tuning.minZoom, tuning.maxZoom), pivot = Offset.Zero) {
                        translate(-vc.x + camera.panX, -vc.y + camera.panY) {
                            drawSpecimenContent(
                                scope = this,
                                w = w,
                                h = h,
                                scene = scene,
                                anim = anim,
                                tuning = tuning,
                                parallax = parallax,
                                particles = particles,
                                pulse = pulse,
                                breath = breath,
                                selection = selection,
                            )
                        }
                    }
                }
            }
            if (tuning.showFramingDebug || tuning.showSeedFirstDebug) {
                drawFramingDebug(scope = this, w = w, h = h, scene = scene, parallax = parallax, tuning = tuning)
            }
        }
    }

    private fun drawFramingDebug(
        scope: DrawScope,
        w: Float,
        h: Float,
        scene: VesselSceneState,
        parallax: Offset,
        tuning: RenderTuning,
    ) {
        val core = VesselFraming.computeCoreSpecimenBounds(w, h, scene, parallax)
        val vc = Offset(w / 2f, h / 2f)
        val sp = scene.seedPlacement
        val seedPt = Offset(w * sp.anchorXNormalized + parallax.x * 0.15f, h * sp.anchorYNormalized + parallax.y * 0.12f)
        val tc = Offset(core.centroid.x, h * if (scene.seedFirstFramingActive) sp.anchorYNormalized else tuning.defaultVisualCentroidTargetY)
        val envelope = VesselFraming.computeFxEnvelope(w, h, scene, parallax)
        val dbg = Color(0xFF00FFAA).copy(alpha = 0.55f)
        val dbg2 = Color(0xFFFF8800).copy(alpha = 0.35f)
        val dbg3 = Color(0xFFFF44CC).copy(alpha = 0.65f)
        scope.drawLine(dbg, start = Offset(vc.x - 18f, vc.y), end = Offset(vc.x + 18f, vc.y), strokeWidth = 2f)
        scope.drawLine(dbg, start = Offset(vc.x, vc.y - 18f), end = Offset(vc.x, vc.y + 18f), strokeWidth = 2f)
        scope.drawCircle(dbg, radius = 5f, center = vc)
        scope.drawCircle(dbg2, radius = 4f, center = core.centroid)
        scope.drawCircle(dbg, radius = 4f, center = tc)
        scope.drawCircle(dbg3, radius = 6f, center = seedPt)
        scope.drawLine(dbg3, start = Offset(seedPt.x - 10f, seedPt.y), end = Offset(seedPt.x + 10f, seedPt.y), strokeWidth = 1.5f)
        scope.drawLine(dbg3, start = Offset(seedPt.x, seedPt.y - 10f), end = Offset(seedPt.x, seedPt.y + 10f), strokeWidth = 1.5f)
        // Chamber rect (viewport)
        scope.drawRect(color = dbg.copy(alpha = 0.35f), style = Stroke(width = 1.5f), topLeft = Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(w, h))
        val fitRect = Rect(
            left = core.centroid.x - core.halfWidth,
            top = core.centroid.y - core.halfHeight,
            right = core.centroid.x + core.halfWidth,
            bottom = core.centroid.y + core.halfHeight,
        )
        scope.drawRect(color = dbg, style = Stroke(width = 2f), topLeft = fitRect.topLeft, size = fitRect.size)
        scope.drawCircle(dbg2.copy(alpha = 0.2f), radius = envelope.radius, center = envelope.center, style = Stroke(width = 1.5f))
    }

    private fun drawSpecimenContent(
        scope: DrawScope,
        w: Float,
        h: Float,
        scene: VesselSceneState,
        anim: VesselAnimationController,
        tuning: RenderTuning,
        parallax: Offset,
        particles: List<ParticleDraw>,
        pulse: Float,
        breath: Float,
        selection: VesselSelectionState,
    ) {
        val palette = scene.palette
        val material = scene.material
        val sp = scene.seedPlacement
        val cx = w * sp.anchorXNormalized + parallax.x * 0.15f
        val cy = h * sp.anchorYNormalized + parallax.y * 0.12f

        val shiver = scene.stressShiver * tuning.stressShiverDegrees
        val pulseScale = 1f + sin(pulse) * scene.bodyPulseAmplitude
        val breathScale = 1f + sin(breath) * scene.bodyBreathAmplitude * (0.4f + scene.respirationDrive * 0.6f)
        val bodyJitter = anim.slowNoise(0.2f) * scene.stressTint * 2.5f

        val baseW = w * layout.bodyWidth * scene.bodyScale * sp.seedViewportScale * pulseScale * breathScale
        val baseH = h * layout.bodyHeight * scene.bodyScale * sp.seedViewportScale * breathScale * (0.98f + pulseScale * 0.02f)
        val sel = selection.selectedOrgan
        val dim = if (sel != null) tuning.selectionDimAlpha * material.selectionPeerDim else 0f
        val focus = selection.focusProgress.coerceIn(0f, 1f)
        val seedBlend = scene.generated.seedFormBlend.coerceIn(0f, 1f)
        val seedOnly = scene.seedPresentationActive
        val seedOrganVeil = if (seedOnly) 1f else ((seedBlend - 0.5f) / 0.5f).coerceIn(0f, 1f)

        val mode = scene.stageRenderMode
        val leg = scene.legacyScaffoldVisibility.coerceIn(0f, 1f)
        val seedEmergence = scene.seedLocalEmergence.coerceIn(0f, 1f)

        if (scene.seedFirstCanvasActive && leg < 0.95f) {
            ChamberPainter.drawSeedChamberFloor(scope, w, h, Offset(cx, cy), 0.55f + leg * 0.25f)
        }

        val rearParallax = Offset(parallax.x * tuning.rearParallaxMul, parallax.y * tuning.rearParallaxMul)
        if (leg > 0.02f) {
            scope.translate(rearParallax.x, rearParallax.y) {
                val rearPath = VesselContourBuilder.rearSilhouettePath(
                    Offset(cx, cy + h * 0.015f),
                    baseW * 1.06f,
                    baseH * 1.04f,
                    microBreathe = pulse * 0.3f,
                    gen = scene.generated,
                )
                drawPath(path = rearPath, color = Color(0xFF0A1018).copy(alpha = (0.52f + scene.structuralMass * 0.22f) * leg))
            }
        }

        SeedPainter.draw(
            scope = scope,
            center = Offset(cx, cy),
            specimenWidth = baseW,
            specimenHeight = baseH,
            scene = scene,
            palette = palette,
            pulse = pulse,
        )

        if (mode != VesselStageRenderMode.ADVANCED_FORMATION || leg < 0.9f) {
            GerminationPainter.draw(
                scope = scope,
                center = Offset(cx, cy),
                specimenWidth = baseW,
                specimenHeight = baseH,
                scene = scene,
                palette = palette,
                pulse = pulse,
                visibility = when (mode) {
                    VesselStageRenderMode.SEED_ONLY -> 0.95f
                    VesselStageRenderMode.GERMINATING -> 0.75f
                    VesselStageRenderMode.EARLY_BRANCHING -> 0.5f
                    else -> 0.25f * (1f - leg)
                },
            )
        }

        ChamberMassPainter.draw(
            scope = scope,
            center = Offset(cx, cy),
            width = baseW,
            height = baseH,
            scene = scene,
            palette = palette,
            breath = breath,
            visibilityMul = leg,
        )

        val focusCenter = sel?.let { type ->
            when (type) {
                OrganType.THERMAL_MEMBRANE -> Offset(cx, cy)
                else -> scene.organVisuals.firstOrNull { it.type == type }?.let { ov ->
                    organCenter(w, h, parallax, ov)
                }
            }
        }

        scope.rotate(shiver + bodyJitter + scene.mobilitySway * 4f, pivot = Offset(cx, cy)) {
            scale(scaleX = 1f + parallax.x / w * 0.02f, scaleY = 1f + parallax.y / h * 0.015f, pivot = Offset(cx, cy)) {
                val shellVis = if (mode == VesselStageRenderMode.SEED_ONLY) 0.18f else leg.coerceIn(0.12f, 1f)
                VesselMembranePainter.drawShell(
                    scope = this,
                    center = Offset(cx, cy),
                    width = baseW,
                    height = baseH,
                    scene = scene,
                    palette = palette,
                    material = material,
                    tuning = tuning,
                    pulsePhase = pulse,
                    dimAlpha = if (sel != null && sel != OrganType.THERMAL_MEMBRANE) dim * 0.28f else 0f,
                    visibilityMul = shellVis,
                )

                if (seedEmergence > 0.04f) {
                    SeedGrowthPainter.draw(
                        scope = this,
                        center = Offset(cx, cy),
                        width = baseW,
                        height = baseH,
                        scene = scene,
                        palette = palette,
                        phase = anim.seconds,
                        pulse = pulse,
                    )
                }

                if (!seedOnly) {
                    VesselPathwayPainter.draw(
                        scope = this,
                        scene = scene,
                        palette = palette,
                        material = material,
                        anim = anim,
                        tuning = tuning,
                        w = w,
                        h = h,
                        parallax = parallax,
                        pulse = pulse,
                        selectedOrgan = sel,
                        focus = focus,
                        visibilityMul = leg,
                    )
                }

                val musc = scene.organVisuals.firstOrNull { it.type == OrganType.VESTIBULAR_MUSCULATURE }
                if (!seedOnly && musc != null && leg > 0.08f) {
                    VesselMembranePainter.drawStabilizerBands(
                        scope = this,
                        center = Offset(cx, cy),
                        baseW = baseW,
                        baseH = baseH,
                        musc = musc,
                        mobility = scene.mobilitySway,
                        pulse = pulse,
                        palette = palette,
                        tendonVisibilityMul = scene.generated.tendonVisibilityMul * leg,
                    )
                }

                TissuePainter.drawGrowthShimmer(
                    scope = this,
                    center = Offset(cx, cy),
                    width = baseW,
                    height = baseH,
                    scene = scene,
                    phase = anim.seconds,
                    visibilityMul = max(leg, seedEmergence * 0.45f),
                )

                if (!seedOnly) {
                    BuddingPainter.draw(
                        scope = this,
                        center = Offset(cx, cy),
                        width = baseW,
                        height = baseH,
                        scene = scene,
                        palette = palette,
                        phase = anim.seconds,
                        visibilityMul = leg,
                    )

                    GrowthFrontPainter.draw(
                        scope = this,
                        center = Offset(cx, cy),
                        width = baseW,
                        height = baseH,
                        scene = scene,
                        palette = palette,
                        animSeconds = anim.seconds,
                        visibilityMul = leg,
                    )
                }

                if (focusCenter != null && focus > 0.02f) {
                    OrganHighlightPainter.drawFocusVignette(this, focusCenter, focus * (0.72f + material.selectionFocusBoost * 0.08f))
                }

                val innerOrgans = if (seedOnly) emptyList() else scene.organVisuals.filter { it.type != OrganType.THERMAL_MEMBRANE }
                for (ov in innerOrgans) {
                    val oCenter = organCenter(w, h, parallax, ov)
                    val isSel = sel == ov.type
                    val peerDim = if (sel != null && !isSel) dim * (0.55f + focus * 0.28f) else 0f
                    val seedDim = seedOrganVeil * 0.38f * (if (isSel) 0.35f else 1f)
                    val hl = if (isSel) tuning.selectionGlowStrength * material.selectionFocusBoost * (0.82f + focus * 0.45f) else 1f
                    OrganShapeBuilder.drawOrgan(
                        scope = this,
                        ov = ov,
                        palette = palette,
                        center = oCenter,
                        w = w,
                        anim = anim,
                        tuning = tuning,
                        pulse = pulse,
                        breath = breath,
                        dimAlpha = ((peerDim + seedDim + (1f - leg) * 0.55f)).coerceIn(0f, 0.92f),
                        highlightMul = hl * (1f - seedOrganVeil * 0.35f + if (isSel) seedOrganVeil * 0.25f else 0f) * leg,
                        isSelected = isSel,
                        selectionPhase = pulse * 1.08f + focus * 2f,
                        materialOrganHalo = material.organHaloIntensity * leg,
                        gelEnvelopeMul = scene.generated.gelEnvelopeMul,
                        archiveLamellaMul = scene.generated.archiveLamellaDensityMul,
                    )
                }

                scene.organVisuals.firstOrNull { it.type == OrganType.THERMAL_MEMBRANE }?.let {
                    VesselMembranePainter.drawThermalVeil(
                        scope = this,
                        center = Offset(cx, cy),
                        width = baseW * 1.08f,
                        height = baseH * 1.06f,
                        scene = scene,
                        palette = palette,
                        material = material,
                        anim = anim,
                        tuning = tuning,
                        membraneDim = if (sel == OrganType.THERMAL_MEMBRANE) 0f else if (sel != null) dim * 0.22f else 0f,
                        visibilityMul = max(0.15f, leg),
                    )
                }
                if (sel == OrganType.THERMAL_MEMBRANE && focus > 0.05f) {
                    OrganHighlightPainter.drawSelectionRing(
                        scope = this,
                        center = Offset(cx, cy),
                        baseRadius = maxOf(baseW, baseH) * 0.36f,
                        accent = palette.thermalHot,
                        phase = pulse,
                        strength = focus * tuning.selectionGlowStrength * material.selectionFocusBoost * leg,
                    )
                }
            }
        }

        for (p in particles.sortedBy { it.depth }) {
            val nearMul = if (p.depth < 0.5f) tuning.chamberFogDepthNearMul else tuning.chamberFogDepthFarMul
            val c = if (p.depth > 0.55f) {
                palette.accentSignal.copy(alpha = p.alpha * nearMul * (0.65f + scene.signalBrightness * 0.35f))
            } else {
                palette.shellBase.copy(alpha = p.alpha * nearMul * 0.45f)
            }
            scope.drawCircle(color = c, radius = p.radius * (0.85f + p.depth * 0.2f), center = p.position)
        }
    }

    private fun organCenter(w: Float, h: Float, parallax: Offset, ov: OrganVisualModel): Offset =
        Offset(
            w * ov.anchorX + parallax.x * 0.12f * (0.6f + ov.baseRadius * 3f),
            h * ov.anchorY + parallax.y * 0.1f * (0.6f + ov.baseRadius * 3f),
        )
}
