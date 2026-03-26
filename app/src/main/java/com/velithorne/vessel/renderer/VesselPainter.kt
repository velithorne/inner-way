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
            if (tuning.showFramingDebug) {
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
        val tc = Offset(core.centroid.x, h * tuning.defaultCompositionY)
        val envelope = VesselFraming.computeFxEnvelope(w, h, scene, parallax)
        val dbg = Color(0xFF00FFAA).copy(alpha = 0.55f)
        val dbg2 = Color(0xFFFF8800).copy(alpha = 0.35f)
        scope.drawLine(dbg, start = Offset(vc.x - 18f, vc.y), end = Offset(vc.x + 18f, vc.y), strokeWidth = 2f)
        scope.drawLine(dbg, start = Offset(vc.x, vc.y - 18f), end = Offset(vc.x, vc.y + 18f), strokeWidth = 2f)
        scope.drawCircle(dbg, radius = 5f, center = vc)
        scope.drawCircle(dbg2, radius = 4f, center = core.centroid)
        scope.drawCircle(dbg, radius = 4f, center = tc)
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
        val cx = w * layout.bodyCenterX + parallax.x * 0.15f
        val cy = h * layout.bodyCenterY + parallax.y * 0.12f

        val shiver = scene.stressShiver * tuning.stressShiverDegrees
        val pulseScale = 1f + sin(pulse) * scene.bodyPulseAmplitude
        val breathScale = 1f + sin(breath) * scene.bodyBreathAmplitude * (0.4f + scene.respirationDrive * 0.6f)
        val bodyJitter = anim.slowNoise(0.2f) * scene.stressTint * 2.5f

        val baseW = w * layout.bodyWidth * scene.bodyScale * pulseScale * breathScale
        val baseH = h * layout.bodyHeight * scene.bodyScale * breathScale * (0.98f + pulseScale * 0.02f)
        val sel = selection.selectedOrgan
        val dim = if (sel != null) tuning.selectionDimAlpha * material.selectionPeerDim else 0f
        val focus = selection.focusProgress.coerceIn(0f, 1f)
        val seedBlend = scene.generated.seedFormBlend.coerceIn(0f, 1f)
        val seedOrganVeil = ((seedBlend - 0.5f) / 0.5f).coerceIn(0f, 1f)

        val rearParallax = Offset(parallax.x * tuning.rearParallaxMul, parallax.y * tuning.rearParallaxMul)
        scope.translate(rearParallax.x, rearParallax.y) {
            val rearPath = VesselContourBuilder.rearSilhouettePath(
                Offset(cx, cy + h * 0.015f),
                baseW * 1.06f,
                baseH * 1.04f,
                microBreathe = pulse * 0.3f,
                gen = scene.generated,
            )
            drawPath(path = rearPath, color = Color(0xFF0A1018).copy(alpha = 0.52f + scene.structuralMass * 0.22f))
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

        ChamberMassPainter.draw(
            scope = scope,
            center = Offset(cx, cy),
            width = baseW,
            height = baseH,
            scene = scene,
            palette = palette,
            breath = breath,
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
                )

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
                )

                val musc = scene.organVisuals.firstOrNull { it.type == OrganType.VESTIBULAR_MUSCULATURE }
                if (musc != null) {
                    VesselMembranePainter.drawStabilizerBands(
                        scope = this,
                        center = Offset(cx, cy),
                        baseW = baseW,
                        baseH = baseH,
                        musc = musc,
                        mobility = scene.mobilitySway,
                        pulse = pulse,
                        palette = palette,
                        tendonVisibilityMul = scene.generated.tendonVisibilityMul,
                    )
                }

                TissuePainter.drawGrowthShimmer(
                    scope = this,
                    center = Offset(cx, cy),
                    width = baseW,
                    height = baseH,
                    activity = scene.generated.visibleGrowthActivity,
                    phase = anim.seconds,
                )

                BuddingPainter.draw(
                    scope = this,
                    center = Offset(cx, cy),
                    width = baseW,
                    height = baseH,
                    scene = scene,
                    palette = palette,
                    phase = anim.seconds,
                )

                GrowthFrontPainter.draw(
                    scope = this,
                    center = Offset(cx, cy),
                    width = baseW,
                    height = baseH,
                    scene = scene,
                    palette = palette,
                    animSeconds = anim.seconds,
                )

                if (focusCenter != null && focus > 0.02f) {
                    OrganHighlightPainter.drawFocusVignette(this, focusCenter, focus * (0.72f + material.selectionFocusBoost * 0.08f))
                }

                val innerOrgans = scene.organVisuals.filter { it.type != OrganType.THERMAL_MEMBRANE }
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
                        dimAlpha = (peerDim + seedDim).coerceIn(0f, 0.92f),
                        highlightMul = hl * (1f - seedOrganVeil * 0.35f + if (isSel) seedOrganVeil * 0.25f else 0f),
                        isSelected = isSel,
                        selectionPhase = pulse * 1.08f + focus * 2f,
                        materialOrganHalo = material.organHaloIntensity,
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
                    )
                }
                if (sel == OrganType.THERMAL_MEMBRANE && focus > 0.05f) {
                    OrganHighlightPainter.drawSelectionRing(
                        scope = this,
                        center = Offset(cx, cy),
                        baseRadius = maxOf(baseW, baseH) * 0.36f,
                        accent = palette.thermalHot,
                        phase = pulse,
                        strength = focus * tuning.selectionGlowStrength * material.selectionFocusBoost,
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
