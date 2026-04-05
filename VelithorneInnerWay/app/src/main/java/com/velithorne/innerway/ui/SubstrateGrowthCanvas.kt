package com.velithorne.innerway.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import com.velithorne.innerway.identity.LawContext
import com.velithorne.innerway.identity.SpeciesLaws
import com.velithorne.innerway.mind.BodyExpressionModel
import com.velithorne.innerway.perception.EnvironmentalContext
import com.velithorne.innerway.render.GrowthState
import com.velithorne.innerway.render.drawGrowthField
import com.velithorne.innerway.render.rememberPulsePhase
import com.velithorne.innerway.render.thermalTint

/**
 * Shared substrate growth drawing — use any [Modifier] (card strip or fullscreen).
 */
@Composable
fun SubstrateGrowthCanvas(
    modifier: Modifier,
    environment: EnvironmentalContext,
    law: LawContext,
    bodyExpression: BodyExpressionModel,
    growthState: GrowthState,
    onCanvasSize: (widthPx: Float, heightPx: Float) -> Unit = { _, _ -> },
) {
    val lawAllowsAnimation = SpeciesLaws.canAnimate(law)
    val baseDurationMs = 3200
    val breathDurationMs = (baseDurationMs / bodyExpression.breathRate.coerceIn(0.08f, 1f))
        .toInt()
        .coerceIn(900, 14_000)
    val pulse = rememberPulsePhase(if (lawAllowsAnimation) breathDurationMs else breathDurationMs * 2)
    val tint = thermalTint(environment.thermalRatio)

    Canvas(
        modifier = modifier.onSizeChanged { sz ->
            onCanvasSize(sz.width.toFloat(), sz.height.toFloat())
        },
    ) {
        drawGrowthField(
            growth = growthState,
            environment = environment,
            pulsePhase = pulse,
            baseTint = tint,
            expression = bodyExpression,
            nowMillis = System.currentTimeMillis(),
        )
    }
}
