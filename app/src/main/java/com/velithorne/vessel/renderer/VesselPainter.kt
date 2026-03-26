package com.velithorne.vessel.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.velithorne.vessel.physiology.OrganType
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * All Canvas drawing for the specimen. Mapping stays in [VesselRenderer].
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
        }
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
        val cx = w * layout.bodyCenterX + parallax.x * 0.15f
        val cy = h * layout.bodyCenterY + parallax.y * 0.12f

        val shiver = scene.stressShiver * tuning.stressShiverDegrees
        val pulseScale = 1f + sin(pulse) * scene.bodyPulseAmplitude
        val breathScale = 1f + sin(breath) * scene.bodyBreathAmplitude * (0.4f + scene.respirationDrive * 0.6f)
        val bodyJitter = anim.slowNoise(0.2f) * scene.stressTint * 2.5f

        val baseW = w * layout.bodyWidth * scene.bodyScale * pulseScale * breathScale
        val baseH = h * layout.bodyHeight * scene.bodyScale * breathScale * (0.98f + pulseScale * 0.02f)
        val sel = selection.selectedOrgan
        val dim = if (sel != null) tuning.selectionDimAlpha else 0f
        val focus = selection.focusProgress.coerceIn(0f, 1f)

        // ---- Rear depth silhouettes
        scope.translate(parallax.x * 0.08f, parallax.y * 0.06f) {
            drawSoftSilhouette(
                center = Offset(cx, cy + h * 0.02f),
                width = baseW * 1.08f,
                height = baseH * 1.05f,
                color = Color(0xFF0A1018).copy(alpha = 0.55f + scene.structuralMass * 0.2f),
                phase = pulse * 0.3f,
            )
        }

        // ---- Main membrane body
        scope.rotate(shiver + bodyJitter + scene.mobilitySway * 4f, pivot = Offset(cx, cy)) {
            scale(scaleX = 1f + parallax.x / w * 0.02f, scaleY = 1f + parallax.y / h * 0.015f, pivot = Offset(cx, cy)) {
                drawMembraneBody(
                    center = Offset(cx, cy),
                    width = baseW,
                    height = baseH,
                    scene = scene,
                    tuning = tuning,
                    pulsePhase = pulse,
                    dimAlpha = if (sel != null && sel != OrganType.THERMAL_MEMBRANE) dim * 0.35f else 0f,
                )
            }
        }

        val focusCenter = sel?.let { type ->
            when (type) {
                OrganType.THERMAL_MEMBRANE -> Offset(cx, cy)
                else -> scene.organVisuals.firstOrNull { it.type == type }?.let { ov ->
                    Offset(
                        w * ov.anchorX + parallax.x * 0.12f * (0.6f + ov.baseRadius * 3f),
                        h * ov.anchorY + parallax.y * 0.1f * (0.6f + ov.baseRadius * 3f),
                    )
                }
            }
        }
        if (focusCenter != null && focus > 0.02f) {
            OrganHighlightPainter.drawFocusVignette(scope, focusCenter, focus * 0.85f)
        }

        // ---- Organs (excluding thermal full-body duplicate for inner organs only)
        val innerOrgans = scene.organVisuals.filter { it.type != OrganType.THERMAL_MEMBRANE }
        for (ov in innerOrgans) {
            val ox = w * ov.anchorX + parallax.x * 0.12f * (0.6f + ov.baseRadius * 3f)
            val oy = h * ov.anchorY + parallax.y * 0.1f * (0.6f + ov.baseRadius * 3f)
            val isSel = sel == ov.type
            val d = if (sel != null && !isSel) dim * (0.6f + focus * 0.35f) else 0f
            val hl = if (isSel) tuning.selectionGlowStrength * (0.85f + focus * 0.5f) else 1f
            drawOrgan(
                scope = scope,
                ov = ov,
                center = Offset(ox, oy),
                w = w,
                anim = anim,
                tuning = tuning,
                pulse = pulse,
                breath = breath,
                dimAlpha = d,
                highlightMul = hl,
                isSelected = isSel,
                selectionPhase = pulse * 1.15f + focus * 2f,
            )
        }

        // ---- Vascular arcs
        val heart = scene.organVisuals.firstOrNull { it.type == OrganType.METABOLIC_HEART }
        val cortex = scene.organVisuals.firstOrNull { it.type == OrganType.CORTEX_CLUSTER }
        if (heart != null && cortex != null) {
            val hPos = Offset(w * heart.anchorX + parallax.x * 0.1f, h * heart.anchorY + parallax.y * 0.08f)
            val cPos = Offset(w * cortex.anchorX + parallax.x * 0.12f, h * cortex.anchorY + parallax.y * 0.1f)
            val linkBoost = if (sel == OrganType.METABOLIC_HEART || sel == OrganType.CORTEX_CLUSTER) 0.35f else 0f
            drawArcLink(
                scope = scope,
                a = cPos,
                b = hPos,
                color = scene.accentBias.copy(alpha = 0.25f + scene.vascularPulse * 0.35f + linkBoost),
                phase = pulse + scene.neuralDrive * 1.7f,
            )
        }

        // ---- Thermal membrane overlay
        scene.organVisuals.firstOrNull { it.type == OrganType.THERMAL_MEMBRANE }?.let {
            drawThermalVeil(
                scope = scope,
                center = Offset(cx, cy),
                width = baseW * 1.25f,
                height = baseH * 1.2f,
                scene = scene,
                anim = anim,
                tuning = tuning,
                membraneDim = if (sel == OrganType.THERMAL_MEMBRANE) 0f else if (sel != null) dim * 0.25f else 0f,
            )
        }

        if (sel == OrganType.THERMAL_MEMBRANE && focus > 0.05f) {
            OrganHighlightPainter.drawSelectionRing(
                scope = scope,
                center = Offset(cx, cy),
                baseRadius = maxOf(baseW, baseH) * 0.38f,
                accent = scene.thermalTint,
                phase = pulse,
                strength = focus * tuning.selectionGlowStrength,
            )
        }

        // ---- Particles (in front of body, behind HUD)
        for (p in particles.sortedBy { it.depth }) {
            val c = if (p.depth > 0.55f) {
                scene.accentBias.copy(alpha = p.alpha * (0.7f + scene.signalBrightness * 0.3f))
            } else {
                Color(0xFFB8C5CE).copy(alpha = p.alpha)
            }
            scope.drawCircle(color = c, radius = p.radius, center = p.position)
        }

        // ---- Musculature bands (stabilizers)
        val musc = scene.organVisuals.firstOrNull { it.type == OrganType.VESTIBULAR_MUSCULATURE }
        if (musc != null) {
            drawStabilizerBands(scope, Offset(cx, cy), baseW, baseH, musc.strain, scene.mobilitySway, pulse)
        }
    }

    private fun DrawScope.drawMembraneBody(
        center: Offset,
        width: Float,
        height: Float,
        scene: VesselSceneState,
        tuning: RenderTuning,
        pulsePhase: Float,
        dimAlpha: Float = 0f,
    ) {
        val path = teardropPath(center, width, height)
        val fillAlpha = (0.14f + scene.vitalityGlow * 0.18f) * (1f - scene.sleepDimming * 0.55f) * (1f - scene.hungerDim * 0.25f) * (1f - dimAlpha)
        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF2EF3D0).copy(alpha = fillAlpha * 0.35f),
                    Color(0xFF1A3D45).copy(alpha = fillAlpha * 0.85f),
                    Color(0xFF0D1A22).copy(alpha = fillAlpha * 0.95f),
                ),
                startY = center.y - height * 0.55f,
                endY = center.y + height * 0.55f,
            ),
        )
        val strokeGlow = 0.18f + scene.vitalityGlow * 0.22f + sin(pulsePhase) * 0.04f * scene.recoveryGlow
        drawPath(
            path = path,
            color = scene.accentBias.copy(alpha = strokeGlow.coerceIn(0.12f, 0.55f)),
            style = Stroke(width = 2.5f + scene.stressTint * 1.2f),
        )
        // Inner hunger dim
        if (scene.hungerDim > 0.15f) {
            drawPath(
                path = path,
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF000000).copy(alpha = 0f),
                        Color(0xFF050810).copy(alpha = scene.hungerDim * 0.45f),
                    ),
                    center = Offset(center.x, center.y + height * 0.12f),
                    radius = height * 0.65f,
                ),
            )
        }
    }

    private fun teardropPath(center: Offset, width: Float, height: Float): Path {
        val path = Path()
        val top = Offset(center.x, center.y - height * 0.48f)
        val bottom = Offset(center.x, center.y + height * 0.5f)
        val l = Offset(center.x - width * 0.42f, center.y + height * 0.05f)
        val r = Offset(center.x + width * 0.42f, center.y + height * 0.05f)
        path.moveTo(top.x, top.y)
        path.cubicTo(
            top.x + width * 0.35f, top.y + height * 0.15f,
            r.x, r.y - height * 0.08f,
            r.x, r.y,
        )
        path.quadraticTo(center.x + width * 0.08f, bottom.y + height * 0.06f, bottom.x, bottom.y)
        path.quadraticTo(center.x - width * 0.08f, bottom.y + height * 0.06f, l.x, l.y)
        path.cubicTo(
            l.x, l.y - height * 0.08f,
            top.x - width * 0.35f, top.y + height * 0.15f,
            top.x, top.y,
        )
        path.close()
        return path
    }

    private fun DrawScope.drawSoftSilhouette(
        center: Offset,
        width: Float,
        height: Float,
        color: Color,
        phase: Float,
    ) {
        val path = teardropPath(center, width * (1f + sin(phase) * 0.015f), height)
        drawPath(path, color = color)
    }

    private fun drawOrgan(
        scope: DrawScope,
        ov: OrganVisualModel,
        center: Offset,
        w: Float,
        anim: VesselAnimationController,
        tuning: RenderTuning,
        pulse: Float,
        breath: Float,
        dimAlpha: Float,
        highlightMul: Float,
        isSelected: Boolean,
        selectionPhase: Float,
    ) {
        val r = w * ov.baseRadius * (0.85f + ov.reserveLevel * 0.25f + sin(pulse * ov.pulseCoupling) * 0.06f)
        val flick = sin(anim.pulsePhase(tuning.neuralFlickerHz) + ov.flickerIntensity * 3f) * 0.5f + 0.5f
        val glowC = when (ov.type) {
            OrganType.SIGNAL_LUNGS -> Color(0xFF5EEAD4)
            OrganType.METABOLIC_HEART -> Color(0xFFFF6B8A)
            OrganType.CORTEX_CLUSTER -> Color(0xFFB794F6)
            OrganType.NEURAL_GEL -> Color(0xFF7FD6E8)
            OrganType.ARCHIVE_VAULT -> Color(0xFF9FA8DA)
            OrganType.VESTIBULAR_MUSCULATURE -> Color(0xFF718096)
            OrganType.THERMAL_MEMBRANE -> Color(0xFFFFB347)
        }
        val dimF = (1f - dimAlpha).coerceIn(0.35f, 1f)
        val intensity = (ov.glowIntensity * highlightMul * (0.55f + flick * 0.45f * ov.flickerIntensity)).coerceIn(0.05f, 1.8f) * dimF
        GlowSystem.radialBloom(
            scope = scope,
            center = center,
            radius = r * (1.1f + sin(breath) * 0.08f * ov.pulseCoupling),
            core = glowC.copy(alpha = glowC.alpha * dimF),
            halo = glowC.copy(alpha = 0.5f * dimF),
            intensity = intensity,
        )
        val fillA = { base: Float -> (base * dimF).coerceIn(0.04f, 1f) }
        when (ov.type) {
            OrganType.CORTEX_CLUSTER -> {
                val stem = Path().apply {
                    moveTo(center.x, center.y + r * 0.35f)
                    quadraticTo(center.x + r * 0.15f, center.y - r * 0.05f, center.x, center.y - r * 0.55f)
                    quadraticTo(center.x - r * 0.15f, center.y - r * 0.05f, center.x, center.y + r * 0.35f)
                    close()
                }
                scope.drawPath(stem, color = glowC.copy(alpha = fillA(0.22f + intensity * 0.1f)))
                for (i in 0 until 7) {
                    val ang = i / 7f * Math.PI.toFloat() * 2f + pulse * 0.4f
                    val br = r * (0.22f + (i % 3) * 0.1f)
                    val rad = r * (0.52f + (i % 4) * 0.06f)
                    val p = Offset(center.x + cos(ang) * rad, center.y + sin(ang) * rad * 0.72f - r * 0.1f)
                    scope.drawCircle(glowC.copy(alpha = fillA(0.32f + flick * 0.18f)), br, p)
                }
            }
            OrganType.ARCHIVE_VAULT -> {
                val layers = (3 + ov.densityLines * 5f).toInt().coerceIn(3, 10)
                for (i in 0 until layers) {
                    val rr = r * (0.4f + i * 0.09f)
                    scope.drawCircle(
                        color = glowC.copy(alpha = fillA(0.07f + ov.densityLines * 0.065f)),
                        radius = rr,
                        center = center,
                        style = Stroke(1.5f + i * 0.12f),
                    )
                }
            }
            OrganType.SIGNAL_LUNGS -> {
                val mirror = center.x < scope.size.width * 0.5f
                val sx = if (mirror) -1f else 1f
                val path = Path().apply {
                    moveTo(center.x, center.y - r * 0.15f)
                    quadraticTo(
                        center.x + sx * r * 0.42f,
                        center.y - r * 0.92f,
                        center.x + sx * r * 0.88f,
                        center.y + r * 0.06f,
                    )
                    quadraticTo(center.x + sx * r * 0.22f, center.y + r * 0.58f, center.x, center.y - r * 0.15f)
                    close()
                }
                scope.drawPath(path, color = glowC.copy(alpha = fillA(0.22f + intensity * 0.14f)))
                scope.drawPath(
                    path,
                    color = glowC.copy(alpha = fillA(0.12f)),
                    style = Stroke(1.2f),
                )
            }
            OrganType.NEURAL_GEL -> {
                scope.drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowC.copy(alpha = fillA(0.08f)),
                            glowC.copy(alpha = fillA(0.22f + intensity * 0.08f)),
                            glowC.copy(alpha = fillA(0.05f)),
                        ),
                        center = center,
                        radius = r * 1.1f,
                    ),
                    radius = r * 1.05f,
                    center = center,
                )
                scope.drawCircle(
                    color = glowC.copy(alpha = fillA(0.06f)),
                    radius = r * 0.92f,
                    center = center,
                    style = Stroke(1f + ov.densityLines * 1.5f),
                )
            }
            OrganType.METABOLIC_HEART -> {
                val hp = heartPath(center, r)
                scope.drawPath(hp, color = glowC.copy(alpha = fillA(0.28f + intensity * 0.18f)))
                scope.drawPath(hp, color = Color(0xFFFF8FA8).copy(alpha = fillA(0.35f)), style = Stroke(1.8f))
            }
            else -> {
                scope.drawCircle(glowC.copy(alpha = fillA(0.22f + intensity * 0.15f)), r * 0.75f, center)
            }
        }
        if (isSelected) {
            OrganHighlightPainter.drawSelectionRing(
                scope = scope,
                center = center,
                baseRadius = r * 1.05f,
                accent = Color(0xFFE8F1F2),
                phase = selectionPhase,
                strength = tuning.selectionGlowStrength,
            )
        }
    }

    private fun heartPath(center: Offset, r: Float): Path {
        val c = center
        val s = r * 1.1f
        return Path().apply {
            moveTo(c.x, c.y + s * 0.25f)
            cubicTo(
                c.x - s * 0.55f, c.y - s * 0.35f,
                c.x - s * 0.45f, c.y - s * 0.75f,
                c.x, c.y - s * 0.45f,
            )
            cubicTo(
                c.x + s * 0.45f, c.y - s * 0.75f,
                c.x + s * 0.55f, c.y - s * 0.35f,
                c.x, c.y + s * 0.25f,
            )
            close()
        }
    }

    private fun drawArcLink(scope: DrawScope, a: Offset, b: Offset, color: Color, phase: Float) {
        val mid = Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f - abs(a.x - b.x) * 0.12f)
        val path = Path().apply {
            moveTo(a.x, a.y)
            quadraticTo(mid.x + sin(phase) * 8f, mid.y, b.x, b.y)
        }
        scope.drawPath(path, color = color.copy(alpha = color.alpha * (0.5f + sin(phase * 2f) * 0.3f)), style = Stroke(width = 2f))
    }

    private fun drawThermalVeil(
        scope: DrawScope,
        center: Offset,
        width: Float,
        height: Float,
        scene: VesselSceneState,
        anim: VesselAnimationController,
        tuning: RenderTuning,
        membraneDim: Float = 0f,
    ) {
        if (scene.feverIntensity < 0.04f && scene.stressTint < 0.55f) return
        val path = teardropPath(center, width, height)
        val shim = sin(anim.pulsePhase(0.35f)) * scene.feverIntensity * tuning.feverShimmerScale
        val d = (1f - membraneDim).coerceIn(0.55f, 1f)
        scope.drawPath(
            path = path,
            brush = Brush.radialGradient(
                colors = listOf(
                    scene.thermalTint.copy(alpha = 0f),
                    scene.thermalTint.copy(alpha = (scene.feverIntensity * 0.35f + shim * 0.08f) * d),
                    Color(0xFFFF4500).copy(alpha = scene.thermalAgitation * 0.18f * d),
                ),
                center = Offset(center.x, center.y - height * 0.08f),
                radius = height * 0.75f,
            ),
        )
    }

    private fun drawStabilizerBands(
        scope: DrawScope,
        center: Offset,
        bodyW: Float,
        bodyH: Float,
        strain: Float,
        mobility: Float,
        pulse: Float,
    ) {
        val tension = (strain * 0.6f + mobility * 0.4f).coerceIn(0f, 1f)
        repeat(3) { i ->
            val t = i - 1f
            val yOff = bodyH * 0.12f * t + sin(pulse + i) * mobility * bodyH * 0.04f
            val path = Path().apply {
                moveTo(center.x - bodyW * 0.35f, center.y + yOff)
                quadraticTo(
                    center.x,
                    center.y + yOff - bodyH * 0.06f * (0.4f + tension),
                    center.x + bodyW * 0.35f,
                    center.y + yOff,
                )
            }
            scope.drawPath(
                path,
                color = Color(0xFF4ECDC4).copy(alpha = 0.08f + tension * 0.18f),
                style = Stroke(width = 1.5f + tension * 2f),
            )
        }
    }
}

private fun sin(x: Float): Float = sin(x.toDouble()).toFloat()
