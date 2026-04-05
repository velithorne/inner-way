package com.velithorne.innerway.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.velithorne.innerway.identity.LawContext
import com.velithorne.innerway.identity.SpeciesLaws
import com.velithorne.innerway.mind.BodyExpressionModel
import com.velithorne.innerway.mind.GrowthStage
import com.velithorne.innerway.mind.InternalState
import com.velithorne.innerway.perception.EnvironmentalContext
import com.velithorne.innerway.render.drawSiliconBody
import com.velithorne.innerway.render.rememberPulsePhase
import com.velithorne.innerway.render.thermalTint

/**
 * Central organism: same infinite pulse/breath substrate as Phase 1; Phase 2 varies duration and
 * coefficients via [bodyExpression] (physiological state), not a separate animation system.
 */
@Composable
fun AmbientBodyView(
    environment: EnvironmentalContext,
    internalState: InternalState,
    stage: GrowthStage,
    law: LawContext,
    bodyExpression: BodyExpressionModel,
) {
    val lawAllowsAnimation = SpeciesLaws.canAnimate(law)
    val baseDurationMs = 3200
    val breathDurationMs = (baseDurationMs / bodyExpression.breathRate.coerceIn(0.08f, 1f))
        .toInt()
        .coerceIn(900, 14_000)
    val pulse = rememberPulsePhase(if (lawAllowsAnimation) breathDurationMs else breathDurationMs * 2)

    val lawContractionExtra = if (SpeciesLaws.shouldContract(law)) 0.22f else 0f
    val tint = thermalTint(environment.thermalRatio)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
    ) {
        drawSiliconBody(
            environment = environment,
            stage = stage,
            pulsePhase = pulse,
            baseColor = tint,
            bodyExpression = bodyExpression,
            lawContractionExtra = lawContractionExtra,
        )
    }
}
