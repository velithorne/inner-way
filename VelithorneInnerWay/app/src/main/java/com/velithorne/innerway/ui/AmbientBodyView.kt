package com.velithorne.innerway.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.velithorne.innerway.identity.LawContext
import com.velithorne.innerway.identity.SpeciesLaws
import com.velithorne.innerway.mind.BodyExpressionModel
import com.velithorne.innerway.mind.GrowthStage
import com.velithorne.innerway.mind.InternalState
import com.velithorne.innerway.perception.EnvironmentalContext
import com.velithorne.innerway.render.GrowthState
import com.velithorne.innerway.render.drawGrowthField
import com.velithorne.innerway.render.rememberPulsePhase
import com.velithorne.innerway.render.thermalTint

/**
 * Substrate growth field: persistent graph + pulse along conductive paths (not a central orb).
 */
@Composable
fun AmbientBodyView(
    environment: EnvironmentalContext,
    internalState: InternalState,
    stage: GrowthStage,
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
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .onSizeChanged { sz ->
                onCanvasSize(sz.width.toFloat(), sz.height.toFloat())
            },
    ) {
        drawGrowthField(
            growth = growthState,
            environment = environment,
            pulsePhase = pulse,
            baseTint = tint,
            expression = bodyExpression,
        )
    }
}
