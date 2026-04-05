package com.velithorne.innerway.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.velithorne.innerway.identity.LawContext
import com.velithorne.innerway.identity.SpeciesLaws
import com.velithorne.innerway.mind.GrowthStage
import com.velithorne.innerway.mind.InternalState
import com.velithorne.innerway.perception.EnvironmentalContext
import com.velithorne.innerway.render.drawSiliconBody
import com.velithorne.innerway.render.rememberPulsePhase
import com.velithorne.innerway.render.sleepDimming
import com.velithorne.innerway.render.stressJitter
import com.velithorne.innerway.render.thermalTint

@Composable
fun AmbientBodyView(
    environment: EnvironmentalContext,
    internalState: InternalState,
    stage: GrowthStage,
    law: LawContext,
) {
    val lawAllowsAnimation = SpeciesLaws.canAnimate(law)
    val pulse = rememberPulsePhase(if (lawAllowsAnimation) 3200 else 6000)
    val contraction = if (SpeciesLaws.shouldContract(law)) 0.65f else 0.2f
    val jitter = stressJitter(internalState, environment.nervousLoad)
    val brightness = sleepDimming(internalState) * environment.energyRatio.coerceIn(0.25f, 1f)
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
            contraction = contraction,
            jitter = jitter,
            brightness = brightness,
        )
    }
}
