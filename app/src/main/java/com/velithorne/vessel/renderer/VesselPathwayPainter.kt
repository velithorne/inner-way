package com.velithorne.vessel.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.velithorne.vessel.model.VesselMaterialState
import com.velithorne.vessel.model.VesselPaletteState
import com.velithorne.vessel.physiology.OrganType
import kotlin.math.abs
import kotlin.math.sin

/** Metabolic + neural pathways with traveling pulse. */
object VesselPathwayPainter {

    fun draw(
        scope: DrawScope,
        scene: VesselSceneState,
        palette: VesselPaletteState,
        material: VesselMaterialState,
        anim: VesselAnimationController,
        tuning: RenderTuning,
        w: Float,
        h: Float,
        parallax: Offset,
        pulse: Float,
        selectedOrgan: OrganType?,
        focus: Float,
    ) {
        fun xy(ov: OrganVisualModel) = Offset(
            w * ov.anchorX + parallax.x * 0.12f * (0.6f + ov.baseRadius * 3f),
            h * ov.anchorY + parallax.y * 0.1f * (0.6f + ov.baseRadius * 3f),
        )

        val byType = scene.organVisuals.groupBy { it.type }.mapValues { it.value.first() }

        val heart = byType[OrganType.METABOLIC_HEART]?.let(::xy) ?: return
        val cortex = byType[OrganType.CORTEX_CLUSTER]?.let(::xy)
        val vault = byType[OrganType.ARCHIVE_VAULT]?.let(::xy)
        val gel = byType[OrganType.NEURAL_GEL]?.let(::xy)
        val lungs = scene.organVisuals.filter { it.type == OrganType.SIGNAL_LUNGS }
        val lungL = lungs.minByOrNull { it.anchorX }?.let(::xy)
        val lungR = lungs.maxByOrNull { it.anchorX }?.let(::xy)

        val g = scene.generated
        val pulseSpeed = tuning.pathwayPulseSpeed * material.pathwayPulseSpeedMul * scene.vascularPulse.coerceIn(0.2f, 1f)
        val travel = anim.seconds * pulseSpeed * 1.8f
        val baseMet = material.pathwayBaseAlpha * (0.85f + scene.vitalityGlow * 0.15f).coerceIn(0.5f, 1.2f)
        val baseNeu = baseMet * (0.75f + scene.neuralDrive * 0.35f)

        fun metabolicBoostConnects(toType: OrganType?): Float {
            if (toType == null || focus < 0.05f) return 0f
            val h = selectedOrgan == OrganType.METABOLIC_HEART
            val t = selectedOrgan == toType
            return if (h || t) 0.2f * focus else 0f
        }

        fun neuralBoost(target: OrganType): Float {
            if (focus < 0.05f) return 0f
            val c = selectedOrgan == OrganType.CORTEX_CLUSTER
            val t = selectedOrgan == target
            return if (c || t) 0.16f * focus else 0f
        }

        cortex?.let { c ->
            drawPathway(scope, heart, c, palette.metabolicPathway, baseMet * (0.55f + scene.vascularPulse * 0.35f), tuning.pathwayWidthMetabolic * g.metabolicPathwayMul, pulse, travel, metabolicBoostConnects(OrganType.CORTEX_CLUSTER))
        }
        vault?.let { v ->
            drawPathway(scope, heart, v, palette.metabolicPathway, baseMet * (0.5f + scene.structuralMass * 0.2f), tuning.pathwayWidthMetabolic * g.metabolicPathwayMul, pulse + 0.4f, travel + 0.3f, metabolicBoostConnects(OrganType.ARCHIVE_VAULT))
        }
        lungL?.let { l ->
            drawPathway(scope, heart, l, palette.metabolicPathway, baseMet * 0.48f, tuning.pathwayWidthMetabolic * g.metabolicPathwayMul, pulse + 0.8f, travel + 1.1f, metabolicBoostConnects(OrganType.SIGNAL_LUNGS))
        }
        lungR?.let { r ->
            drawPathway(scope, heart, r, palette.metabolicPathway, baseMet * 0.48f, tuning.pathwayWidthMetabolic * g.metabolicPathwayMul, pulse + 1.1f, travel + 1.4f, metabolicBoostConnects(OrganType.SIGNAL_LUNGS))
        }

        if (cortex != null) {
            gel?.let { gp ->
                drawPathway(scope, cortex, gp, palette.neuralPathway, baseNeu * 0.55f, tuning.pathwayWidthNeural * g.neuralPathwayMul, pulse * 1.2f, travel, neuralBoost(OrganType.NEURAL_GEL))
            }
            vault?.let { v ->
                drawPathway(scope, cortex, v, palette.neuralPathway, baseNeu * 0.42f, tuning.pathwayWidthNeural * g.neuralPathwayMul, pulse * 1.05f, travel + 0.5f, neuralBoost(OrganType.ARCHIVE_VAULT))
            }
            lungL?.let { l ->
                drawPathway(scope, cortex, l, palette.neuralPathway, baseNeu * (0.48f + scene.signalBrightness * 0.12f), tuning.pathwayWidthNeural * g.neuralPathwayMul, pulse * 1.3f, travel + 0.8f, neuralBoost(OrganType.SIGNAL_LUNGS))
            }
            lungR?.let { r ->
                drawPathway(scope, cortex, r, palette.neuralPathway, baseNeu * (0.48f + scene.signalBrightness * 0.12f), tuning.pathwayWidthNeural * g.neuralPathwayMul, pulse * 1.35f, travel + 1f, neuralBoost(OrganType.SIGNAL_LUNGS))
            }
        }
    }

    private fun drawPathway(
        scope: DrawScope,
        a: Offset,
        b: Offset,
        color: Color,
        baseAlpha: Float,
        width: Float,
        pulse: Float,
        travel: Float,
        selectionBoost: Float,
    ) {
        val mid = Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f - abs(a.x - b.x) * 0.09f)
        val path = Path().apply {
            moveTo(a.x, a.y)
            quadraticTo(mid.x + sin(pulse) * 5f, mid.y, b.x, b.y)
        }
        val wave = (sin(travel * 6.28318548f + a.x * 0.01f) * 0.5f + 0.5f) * 0.35f + 0.65f
        val alpha = ((baseAlpha + selectionBoost) * wave).coerceIn(0.03f, 0.72f)
        scope.drawPath(path, color = color.copy(alpha = alpha), style = Stroke(width = width + 2.2f))
        scope.drawPath(path, color = color.copy(alpha = alpha * 0.5f), style = Stroke(width = width))
    }
}
