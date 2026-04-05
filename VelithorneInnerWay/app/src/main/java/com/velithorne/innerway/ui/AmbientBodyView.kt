package com.velithorne.innerway.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.velithorne.innerway.identity.LawContext
import com.velithorne.innerway.mind.BodyExpressionModel
import com.velithorne.innerway.mind.GrowthStage
import com.velithorne.innerway.mind.InternalState
import com.velithorne.innerway.perception.EnvironmentalContext
import com.velithorne.innerway.render.GrowthState

/**
 * Compact strip of the substrate growth field (main screen).
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
    SubstrateGrowthCanvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        environment = environment,
        law = law,
        bodyExpression = bodyExpression,
        growthState = growthState,
        onCanvasSize = onCanvasSize,
    )
}
