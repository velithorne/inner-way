package com.velithorne.vessel.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.velithorne.vessel.model.VesselPaletteState
import com.velithorne.vessel.physiology.OrganType
import kotlin.math.cos
import kotlin.math.sin

/** Phase 5 organ silhouettes — procedural, palette-driven, minimal glow-first. */
object OrganShapeBuilder {

    fun drawOrgan(
        scope: DrawScope,
        ov: OrganVisualModel,
        palette: VesselPaletteState,
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
        materialOrganHalo: Float,
    ) {
        val r = w * ov.baseRadius * (0.85f + ov.reserveLevel * 0.22f +
            sin(pulse * ov.pulseCoupling * 1.05f) * 0.055f)
        val flick = sin(anim.pulsePhase(tuning.neuralFlickerHz) + ov.flickerIntensity * 3f) * 0.5f + 0.5f
        val dimF = (1f - dimAlpha).coerceIn(0.42f, 1f)
        val organPalette = organColors(ov.type, palette)
        val haloMul = (materialOrganHalo * highlightMul).coerceIn(0.45f, 1.35f)
        val intensity = (ov.glowIntensity * highlightMul * (0.5f + flick * 0.45f * ov.flickerIntensity.coerceIn(0.05f, 1f)))
            .coerceIn(0.06f, 1.6f) * dimF * haloMul

        GlowSystem.radialBloom(
            scope = scope,
            center = center,
            radius = r * (1.05f + sin(breath * ov.pulseCoupling) * 0.06f),
            core = organPalette.core.copy(alpha = organPalette.core.alpha * dimF * 0.55f),
            halo = organPalette.halo.copy(alpha = organPalette.halo.alpha * dimF * 0.35f),
            intensity = intensity * 0.5f,
        )

        val fillA = { base: Float -> (base * dimF).coerceIn(0.04f, 1f) }

        when (ov.type) {
            OrganType.METABOLIC_HEART -> drawMetabolicHeart(scope, center, r, pulse, organPalette, fillA, intensity)
            OrganType.CORTEX_CLUSTER -> drawCortexCluster(scope, center, r, pulse, breath, flick, organPalette, fillA, tuning)
            OrganType.NEURAL_GEL -> drawNeuralGel(scope, center, r, pulse, ov, palette, fillA, intensity)
            OrganType.ARCHIVE_VAULT -> drawArchiveVault(scope, center, r, pulse, ov, palette, fillA)
            OrganType.SIGNAL_LUNGS -> drawSignalLungs(scope, center, r, breath, organPalette, fillA, scope.size.width)
            OrganType.VESTIBULAR_MUSCULATURE -> { /* drawn as pathways + membrane bands */ }
            OrganType.THERMAL_MEMBRANE -> { }
        }

        if (isSelected && ov.type != OrganType.VESTIBULAR_MUSCULATURE && ov.type != OrganType.THERMAL_MEMBRANE) {
            OrganHighlightPainter.drawSelectionRing(
                scope = scope,
                center = center,
                baseRadius = r * 1.02f,
                accent = Color(0xFFD8E8EA).copy(alpha = 0.55f),
                phase = selectionPhase,
                strength = tuning.selectionGlowStrength * tuning.selectionFocusIntensity * 0.85f,
            )
        }
    }

    private data class OrganColors(val core: Color, val halo: Color, val filament: Color)

    private fun organColors(type: OrganType, p: VesselPaletteState) = when (type) {
        OrganType.METABOLIC_HEART -> OrganColors(p.heartCore, p.heartRing, p.heartRing)
        OrganType.CORTEX_CLUSTER -> OrganColors(p.cortexNode, p.cortexFilament.copy(alpha = p.cortexFilament.alpha.coerceAtLeast(0.35f)), p.cortexFilament)
        OrganType.NEURAL_GEL -> OrganColors(p.gelMedium, p.gelGrain, p.accentSignal)
        OrganType.ARCHIVE_VAULT -> OrganColors(p.archivePlate, p.archiveDeep, p.archivePlate)
        OrganType.SIGNAL_LUNGS -> OrganColors(p.lungFrond, p.accentSignal, p.accentSignal)
        OrganType.VESTIBULAR_MUSCULATURE -> OrganColors(p.musculatureTension, p.shellBase, p.musculatureTension)
        OrganType.THERMAL_MEMBRANE -> OrganColors(p.thermalHot, p.thermalEdge, p.thermalEdge)
    }

    private fun drawMetabolicHeart(
        scope: DrawScope,
        center: Offset,
        r: Float,
        pulse: Float,
        pal: OrganColors,
        fillA: (Float) -> Float,
        intensity: Float,
    ) {
        val hp = heartChamberPath(center, r * 0.92f)
        val ringPulse = 1f + sin(pulse * 2f) * 0.05f * intensity.coerceIn(0f, 1f)
        scope.drawPath(
            hp,
            brush = Brush.radialGradient(
                colors = listOf(
                    pal.core.copy(alpha = fillA(0.35f + intensity * 0.12f)),
                    pal.halo.copy(alpha = fillA(0.2f)),
                ),
                center = Offset(center.x, center.y - r * 0.08f),
                radius = r * 1.1f,
            ),
        )
        scope.drawCircle(
            color = pal.core.copy(alpha = fillA(0.45f)),
            radius = r * 0.22f * ringPulse,
            center = center,
        )
        scope.drawPath(
            hp,
            color = pal.halo.copy(alpha = fillA(0.38f)),
            style = Stroke(width = 1.9f + intensity * 0.6f),
        )
        // Energy egress stubs
        repeat(4) { i ->
            val ang = (i / 4f) * Math.PI.toFloat() * 2f + pulse * 0.15f
            val len = r * (0.35f + intensity * 0.15f)
            val ex = center.x + cos(ang) * len
            val ey = center.y + sin(ang) * len * 0.75f
            scope.drawLine(
                pal.halo.copy(alpha = fillA(0.18f)),
                center,
                Offset(ex, ey),
                strokeWidth = 1.1f,
            )
        }
    }

    private fun heartChamberPath(c: Offset, s: Float): Path = Path().apply {
        moveTo(c.x, c.y + s * 0.22f)
        cubicTo(
            c.x - s * 0.52f, c.y - s * 0.32f,
            c.x - s * 0.42f, c.y - s * 0.7f,
            c.x, c.y - s * 0.42f,
        )
        cubicTo(
            c.x + s * 0.42f, c.y - s * 0.7f,
            c.x + s * 0.52f, c.y - s * 0.32f,
            c.x, c.y + s * 0.22f,
        )
        close()
    }

    private fun drawCortexCluster(
        scope: DrawScope,
        center: Offset,
        r: Float,
        pulse: Float,
        breath: Float,
        flick: Float,
        pal: OrganColors,
        fillA: (Float) -> Float,
        tuning: RenderTuning,
    ) {
        val nodes = 9
        val stem = Path().apply {
            moveTo(center.x, center.y + r * 0.4f)
            quadraticTo(
                center.x + r * 0.12f, center.y,
                center.x, center.y - r * 0.55f,
            )
            quadraticTo(
                center.x - r * 0.12f, center.y,
                center.x, center.y + r * 0.4f,
            )
            close()
        }
        scope.drawPath(stem, color = pal.halo.copy(alpha = fillA(0.14f)))
        val surge = sin(pulse * tuning.neuralArcIntensity * 2.2f) * 0.5f + 0.5f
        for (i in 0 until nodes) {
            val layer = i % 3
            val ang = i / nodes.toFloat() * Math.PI.toFloat() * 2f + pulse * 0.35f + layer * 0.4f
            val rad = r * (0.38f + layer * 0.14f)
            val p = Offset(
                center.x + cos(ang) * rad,
                center.y + sin(ang) * rad * 0.68f - r * 0.12f + sin(breath + i) * 1.2f,
            )
            val br = r * (0.14f + (layer * 0.04f) + flick * 0.04f * surge)
            scope.drawCircle(pal.core.copy(alpha = fillA(0.28f + flick * 0.12f * surge)), br, p)
            val nextAng = ang + 0.45f
            val q = Offset(center.x + cos(nextAng) * rad * 0.75f, center.y + sin(nextAng) * rad * 0.55f - r * 0.08f)
            scope.drawLine(
                pal.filament.copy(alpha = fillA(0.2f)),
                p,
                q,
                strokeWidth = 0.9f,
            )
        }
    }

    private fun drawNeuralGel(
        scope: DrawScope,
        center: Offset,
        r: Float,
        pulse: Float,
        ov: OrganVisualModel,
        palette: VesselPaletteState,
        fillA: (Float) -> Float,
        intensity: Float,
    ) {
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.gelMedium.copy(alpha = fillA(0.06f)),
                    palette.gelMedium.copy(alpha = fillA(0.2f + intensity * 0.06f)),
                    palette.innerChamberShadow.copy(alpha = fillA(0.04f)),
                ),
                center = center,
                radius = r * 1.15f,
            ),
            radius = r * 1.08f,
            center = center,
        )
        val grains = (6 + ov.densityLines * 10f).toInt().coerceIn(6, 18)
        for (i in 0 until grains) {
            val ang = i / grains.toFloat() * Math.PI.toFloat() * 2f + pulse * 0.2f
            val rr = r * (0.35f + (i % 4) * 0.11f)
            val gx = center.x + cos(ang) * rr
            val gy = center.y + sin(ang) * rr * 0.85f
            scope.drawCircle(
                palette.gelGrain.copy(alpha = fillA(0.04f + ov.densityLines * 0.04f)),
                radius = 1.2f + ov.densityLines * 1.8f,
                center = Offset(gx, gy),
            )
        }
        scope.drawCircle(
            color = palette.accentSignal.copy(alpha = fillA(0.06f)),
            radius = r * 0.94f,
            center = center,
            style = Stroke(1f + ov.densityLines * 1.4f),
        )
    }

    private fun drawArchiveVault(
        scope: DrawScope,
        center: Offset,
        r: Float,
        pulse: Float,
        ov: OrganVisualModel,
        palette: VesselPaletteState,
        fillA: (Float) -> Float,
    ) {
        val layers = (4 + ov.densityLines * 7f).toInt().coerceIn(4, 12)
        for (i in 0 until layers) {
            val rr = r * (0.32f + i * 0.085f)
            val skew = sin(pulse * 0.8f + i * 0.35f) * r * 0.04f * ov.densityLines
            scope.drawCircle(
                color = palette.archivePlate.copy(alpha = fillA(0.05f + ov.densityLines * 0.07f)),
                radius = rr,
                center = Offset(center.x + skew * 0.3f, center.y + skew * 0.15f),
                style = Stroke(1.2f + i * 0.18f + ov.densityLines * 0.5f),
            )
        }
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.archiveDeep.copy(alpha = fillA(0.35f)),
                    palette.archivePlate.copy(alpha = fillA(0.08f)),
                ),
                center = Offset(center.x, center.y + r * 0.08f),
                radius = r * 0.85f,
            ),
            radius = r * 0.72f,
            center = center,
        )
    }

    private fun drawSignalLungs(
        scope: DrawScope,
        center: Offset,
        r: Float,
        breath: Float,
        pal: OrganColors,
        fillA: (Float) -> Float,
        canvasW: Float,
    ) {
        val mirror = center.x < canvasW * 0.5f
        val sx = if (mirror) -1f else 1f
        val breathMag = 0.06f + sin(breath) * 0.12f
        val path = Path().apply {
            moveTo(center.x, center.y - r * 0.12f)
            quadraticTo(
                center.x + sx * r * (0.48f + breathMag),
                center.y - r * 0.95f,
                center.x + sx * r * (0.92f + breathMag * 0.5f),
                center.y + r * 0.04f,
            )
            quadraticTo(
                center.x + sx * r * 0.28f,
                center.y + r * (0.62f + breathMag * 0.3f),
                center.x,
                center.y - r * 0.12f,
            )
            close()
        }
        scope.drawPath(path, color = pal.core.copy(alpha = fillA(0.26f)))
        // Gill branches
        repeat(3) { b ->
            val t = (b + 1) / 4f
            val bx = center.x + sx * r * (0.35f + t * 0.35f)
            val by = center.y - r * (0.15f + t * 0.55f) + sin(breath + b) * r * 0.04f
            scope.drawLine(
                pal.halo.copy(alpha = fillA(0.14f)),
                Offset(center.x + sx * r * 0.15f, center.y - r * 0.05f),
                Offset(bx, by),
                strokeWidth = 1f,
            )
        }
        scope.drawPath(
            path,
            color = pal.halo.copy(alpha = fillA(0.16f)),
            style = Stroke(1.4f),
        )
    }
}
