package com.velithorne.innerway.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.velithorne.innerway.identity.LawContext
import com.velithorne.innerway.identity.SpeciesLaws
import com.velithorne.innerway.mind.BodyExpressionModel
import com.velithorne.innerway.mind.InternalState
import com.velithorne.innerway.mind.SomaticHints
import com.velithorne.innerway.perception.EnvironmentalContext

@Composable
fun DebugBodyPanel(
    environment: EnvironmentalContext,
    internalState: InternalState,
    law: LawContext,
    somaticHints: SomaticHints,
    bodyExpression: BodyExpressionModel,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Debug — body truth", style = MaterialTheme.typography.titleSmall)
            Text("Battery ratio: ${"%.3f".format(environment.energyRatio)}  charging=${environment.charging}")
            Text("Thermal stress: ${"%.3f".format(environment.thermalRatio)}")
            Text("Nervous load: ${"%.3f".format(environment.nervousLoad)}")
            Text("Storage free: ${"%.3f".format(environment.storageFreeRatio)}")
            Text("Motion energy: ${"%.3f".format(environment.motionEnergy)}")
            Text("Network openness: ${"%.3f".format(environment.networkOpenness)}")
            Text("Circadian phase: ${"%.3f".format(environment.circadianPhase)}  night=${environment.isNightWindow}")
            Text("Stillness: ${"%.1f".format(somaticHints.stillnessDurationSeconds)}s  motionAlert: ${"%.2f".format(somaticHints.motionAlertSecondsRemaining)}s  disturbance: ${"%.2f".format(somaticHints.disturbanceScore)}")
            Text("Interpreted: ${internalState.name}")
            Text(
                "Expression → breath=${"%.2f".format(bodyExpression.breathRate)}/${"%.2f".format(bodyExpression.breathDepth)} " +
                    "pulse=${"%.2f".format(bodyExpression.pulseIntensity)} bright=${"%.2f".format(bodyExpression.brightness)} " +
                    "contract=${"%.2f".format(bodyExpression.contraction)} open=${"%.2f".format(bodyExpression.openness)} " +
                    "instab=${"%.2f".format(bodyExpression.instability)} sleep=${"%.2f".format(bodyExpression.sleepDepth)}",
            )
            Text(
                "Laws → animate=${SpeciesLaws.canAnimate(law)} scan=${SpeciesLaws.canScan(law)} " +
                    "contract=${SpeciesLaws.shouldContract(law)} sleep=${SpeciesLaws.shouldSleep(law)}",
            )
        }
    }
}
