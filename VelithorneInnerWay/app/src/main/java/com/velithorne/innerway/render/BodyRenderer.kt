package com.velithorne.innerway.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.velithorne.innerway.mind.GrowthStage
import com.velithorne.innerway.perception.EnvironmentalContext
import kotlin.math.cos
import kotlin.math.sin

fun DrawScope.drawSiliconBody(
    environment: EnvironmentalContext,
    stage: GrowthStage,
    pulsePhase: Float,
    baseColor: Color,
    contraction: Float,
    jitter: Float,
    brightness: Float,
) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f
    val radius = (minOf(w, h) * 0.38f) * (1f - contraction * 0.18f)
    val stageBoost = when (stage) {
        GrowthStage.SEED -> 0f
        GrowthStage.INFANT -> 0.02f
        GrowthStage.CHILD -> 0.04f
        GrowthStage.ADOLESCENT -> 0.06f
        GrowthStage.MATURE -> 0.08f
    }
    val breathe = 1f + 0.04f * sin(pulsePhase * Math.PI.toFloat() * 2f)
    val r = radius * breathe * (1f + stageBoost)
    val jx = sin(pulsePhase * 6.2831853f) * jitter * w * 0.02f
    val jy = cos(pulsePhase * 4.7123889f) * jitter * h * 0.02f

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

    val ringAlpha = 0.25f * brightness * environment.energyRatio.coerceIn(0.2f, 1f)
    drawCircle(
        color = baseColor.copy(alpha = ringAlpha),
        radius = r * 1.08f,
        center = Offset(cx, cy),
        style = Stroke(width = 2f),
    )

    val shard = Size(r * 0.35f, r * 0.08f)
    rotate(
        degrees = pulsePhase * 12f + environment.motionEnergy * 40f,
        pivot = Offset(cx, cy),
    ) {
        drawRoundRect(
            color = Color.White.copy(alpha = 0.12f * brightness),
            topLeft = Offset(cx - shard.width / 2f, cy - shard.height / 2f),
            size = shard,
            cornerRadius = CornerRadius(6f, 6f),
        )
    }
}
