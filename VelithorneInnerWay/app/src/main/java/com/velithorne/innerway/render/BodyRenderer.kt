package com.velithorne.innerway.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.velithorne.innerway.mind.BodyExpressionModel
import com.velithorne.innerway.mind.GrowthStage
import com.velithorne.innerway.perception.EnvironmentalContext
import kotlin.math.cos
import kotlin.math.sin

/**
 * Living seed visualization. Phase 2 modulates the same sine-phase organism using [BodyExpressionModel]
 * (breath depth, pulse, openness, instability) — baseline breathing signal preserved, not replaced.
 */
fun DrawScope.drawSiliconBody(
    environment: EnvironmentalContext,
    stage: GrowthStage,
    pulsePhase: Float,
    baseColor: Color,
    bodyExpression: BodyExpressionModel,
    /** Extra contraction from species laws (pain/protection). */
    lawContractionExtra: Float = 0f,
) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f

    val contraction = (bodyExpression.contraction + lawContractionExtra).coerceIn(0f, 1f)
    val opennessScale = 0.86f + bodyExpression.openness * 0.24f
    val radius = (minOf(w, h) * 0.38f) * opennessScale * (1f - contraction * 0.2f)

    val stageBoost = when (stage) {
        GrowthStage.SEED -> 0f
        GrowthStage.INFANT -> 0.02f
        GrowthStage.CHILD -> 0.04f
        GrowthStage.ADOLESCENT -> 0.06f
        GrowthStage.MATURE -> 0.08f
    }

    // Baseline living breath (Phase 1): sine undulation; depth scales physiology.
    val breatheAmp = 0.04f * bodyExpression.breathDepth * (1f + bodyExpression.pulseIntensity * 0.42f)
    val breathe = 1f + breatheAmp * sin(pulsePhase * Math.PI.toFloat() * 2f)
    val r = radius * breathe * (1f + stageBoost)

    val shimmerDamp = (1f - bodyExpression.sleepDepth * 0.82f).coerceIn(0.12f, 1f)
    val jitter = bodyExpression.instability * shimmerDamp
    val jx = sin(pulsePhase * 6.2831853f) * jitter * w * 0.035f
    val jy = cos(pulsePhase * 4.7123889f) * jitter * h * 0.035f

    val brightness = bodyExpression.brightness.coerceIn(0.08f, 1f)

    val brush = Brush.radialGradient(
        colors = listOf(
            baseColor.copy(alpha = 0.95f * brightness),
            baseColor.copy(alpha = 0.35f * brightness),
            Color.Transparent,
        ),
        center = Offset(cx + jx, cy + jy),
        radius = r * 1.35f,
    )
    drawCircle(
        brush = brush,
        radius = r,
        center = Offset(cx + jx, cy + jy),
    )

    val ringAlpha = 0.25f * brightness *
        bodyExpression.pulseIntensity.coerceIn(0.1f, 1f) *
        environment.energyRatio.coerceIn(0.2f, 1f)
    drawCircle(
        color = baseColor.copy(alpha = ringAlpha),
        radius = r * 1.08f,
        center = Offset(cx, cy),
        style = Stroke(width = 2f),
    )

    val shard = Size(r * 0.35f, r * 0.08f)
    val shardMotion = environment.motionEnergy * 40f * (0.35f + bodyExpression.pulseIntensity * 0.65f)
    rotate(
        degrees = pulsePhase * (10f + bodyExpression.pulseIntensity * 8f) + shardMotion,
        pivot = Offset(cx, cy),
    ) {
        drawRoundRect(
            color = Color.White.copy(alpha = 0.12f * brightness * bodyExpression.pulseIntensity),
            topLeft = Offset(cx - shard.width / 2f, cy - shard.height / 2f),
            size = shard,
            cornerRadius = CornerRadius(6f, 6f),
        )
    }
}
