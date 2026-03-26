package com.velithorne.vessel.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.velithorne.vessel.model.VesselMaterialState
import com.velithorne.vessel.model.VesselPaletteState
import kotlin.math.sin

/** Translucent silicon shell, inner haze, hunger veil, stabilizer bands, thermal overlay. */
object VesselMembranePainter {

    fun drawShell(
        scope: DrawScope,
        center: Offset,
        width: Float,
        height: Float,
        scene: VesselSceneState,
        palette: VesselPaletteState,
        material: VesselMaterialState,
        tuning: RenderTuning,
        pulsePhase: Float,
        dimAlpha: Float,
    ) {
        val gen = scene.generated
        val path = VesselContourBuilder.bodyShellPath(center, width, height, gen)
        val fillAlpha = material.shellFillOpacity * gen.shellOpacityMul * (1f - scene.sleepDimming * 0.5f) * (1f - scene.hungerDim * 0.28f) * (1f - dimAlpha)
        val recovery = scene.recoveryGlow * material.recoverySheenAlpha
        scope.drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(
                    palette.shellBase.copy(alpha = fillAlpha * 0.28f + recovery * 0.08f),
                    palette.innerChamberShadow.copy(alpha = fillAlpha * 0.75f + scene.structuralMass * 0.06f),
                    palette.archiveDeep.copy(alpha = fillAlpha * 0.9f),
                ),
                startY = center.y - height * 0.55f,
                endY = center.y + height * 0.55f,
            ),
        )
        val innerHaze = material.innerHazeAlpha * (0.6f + scene.fogDensity * 0.25f)
        scope.drawPath(
            path,
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.accentSignal.copy(alpha = 0f),
                    palette.shellRimCool.copy(alpha = innerHaze * 0.35f),
                    Color.Black.copy(alpha = innerHaze * 0.2f),
                ),
                center = Offset(center.x, center.y - height * 0.06f),
                radius = height * 0.48f,
            ),
        )
        val strokeGlow = material.shellEdgeAlpha + sin(pulsePhase) * 0.035f * scene.recoveryGlow
        scope.drawPath(
            path = path,
            color = palette.shellEdge.copy(alpha = strokeGlow.coerceIn(0.1f, 0.58f) * (1f - dimAlpha * 0.35f)),
            style = Stroke(width = material.shellEdgeThicknessPx),
        )
        if (scene.hungerDim > 0.12f) {
            scope.drawPath(
                path = path,
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF000000).copy(alpha = 0f),
                        Color(0xFF050810).copy(alpha = scene.hungerDim * 0.42f),
                    ),
                    center = Offset(center.x, center.y + height * 0.1f),
                    radius = height * 0.6f,
                ),
            )
        }
    }

    fun drawThermalVeil(
        scope: DrawScope,
        center: Offset,
        width: Float,
        height: Float,
        scene: VesselSceneState,
        palette: VesselPaletteState,
        material: VesselMaterialState,
        anim: VesselAnimationController,
        tuning: RenderTuning,
        membraneDim: Float,
    ) {
        if (scene.feverIntensity < 0.035f && scene.stressTint < 0.5f && material.heatTintStrength < 0.08f) return
        val gen = scene.generated
        val path = VesselContourBuilder.bodyShellPath(center, width, height, gen)
        val shim = sin(anim.pulsePhase(0.32f)) * scene.feverIntensity * tuning.feverShimmerScale * material.thermalShimmerStrength * gen.coolingVeilMul
        val d = (1f - membraneDim).coerceIn(0.55f, 1f)
        val edge = material.thermalEdgeBleed * d
        scope.drawPath(
            path,
            color = palette.thermalEdge.copy(alpha = (0.08f + edge * 0.22f) * d),
            style = Stroke(width = 2.2f + edge * 3f),
        )
        scope.drawPath(
            path = path,
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.thermalHot.copy(alpha = 0f),
                    palette.thermalHot.copy(alpha = (scene.feverIntensity * 0.22f + shim * 0.06f + material.heatTintStrength * 0.15f) * d),
                    Color(0xFFFF2200).copy(alpha = scene.thermalAgitation * 0.12f * d),
                ),
                center = Offset(center.x + width * 0.06f * scene.stressTint, center.y - height * 0.1f),
                radius = height * (0.62f + scene.feverIntensity * 0.08f),
            ),
        )
    }

    fun drawStabilizerBands(
        scope: DrawScope,
        center: Offset,
        baseW: Float,
        baseH: Float,
        musc: OrganVisualModel,
        mobility: Float,
        pulse: Float,
        palette: VesselPaletteState,
        tendonVisibilityMul: Float,
    ) {
        val tension = (musc.strain * 0.55f + mobility * 0.45f).coerceIn(0f, 1f) * tendonVisibilityMul.coerceIn(0.35f, 1.4f)
        val yFactorsH = listOf(-0.22f, 0f, 0.22f)
        for ((i, yf) in yFactorsH.withIndex()) {
            val yOff = baseH * yf + sin(pulse + i * 0.7f) * mobility * baseH * 0.035f
            val band = Path().apply {
                moveTo(center.x - baseW * 0.38f, center.y + yOff)
                quadraticTo(
                    center.x,
                    center.y + yOff - baseH * 0.05f * (0.45f + tension),
                    center.x + baseW * 0.38f,
                    center.y + yOff,
                )
            }
            scope.drawPath(
                band,
                color = palette.musculatureTension.copy(alpha = (0.06f + tension * 0.16f) * tendonVisibilityMul.coerceIn(0.4f, 1.2f)),
                style = Stroke(width = 1.4f + tension * 1.8f),
            )
        }
    }
}
