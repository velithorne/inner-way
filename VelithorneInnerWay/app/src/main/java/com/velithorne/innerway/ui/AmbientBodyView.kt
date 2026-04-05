package com.velithorne.innerway.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.velithorne.innerway.identity.LawContext
import com.velithorne.innerway.mind.BodyExpressionModel
import com.velithorne.innerway.mind.GrowthImprintModel
import com.velithorne.innerway.mind.GrowthStage
import com.velithorne.innerway.mind.InternalState
import com.velithorne.innerway.perception.EnvironmentalContext
import com.velithorne.innerway.render.GrowthState
import com.velithorne.innerway.render.TerritoryMap

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
    growthImprint: GrowthImprintModel,
    growthState: GrowthState,
    territoryMap: TerritoryMap,
    showTerritoryDebugOverlay: Boolean = false,
    onCanvasSize: (widthPx: Float, heightPx: Float) -> Unit = { _, _ -> },
    onSubstrateTouch: (normalizedX: Float, normalizedY: Float) -> Unit = { _, _ -> },
) {
    SubstrateGrowthCanvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        environment = environment,
        law = law,
        bodyExpression = bodyExpression,
        growthImprint = growthImprint,
        growthState = growthState,
        territoryMap = territoryMap,
        showTerritoryDebugOverlay = showTerritoryDebugOverlay,
        onCanvasSize = onCanvasSize,
        onSubstrateTouch = onSubstrateTouch,
    )
}
