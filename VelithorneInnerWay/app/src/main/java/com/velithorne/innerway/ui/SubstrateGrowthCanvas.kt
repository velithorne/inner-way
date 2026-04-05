package com.velithorne.innerway.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import com.velithorne.innerway.identity.LawContext
import com.velithorne.innerway.identity.SpeciesLaws
import com.velithorne.innerway.mind.BodyExpressionModel
import com.velithorne.innerway.mind.GrowthImprintModel
import com.velithorne.innerway.perception.EnvironmentalContext
import com.velithorne.innerway.render.GrowthState
import com.velithorne.innerway.render.TerritoryMap
import com.velithorne.innerway.render.drawGrowthField
import com.velithorne.innerway.render.drawTerritoryDebugOverlay
import com.velithorne.innerway.render.rememberPulsePhase
import com.velithorne.innerway.render.thermalTint

/**
 * Shared substrate growth drawing — use any [Modifier] (card strip or fullscreen).
 * Touch is normalized to 0–1 and forwarded for habitat [TerritoryEngine] (not immediate growth).
 */
@Composable
fun SubstrateGrowthCanvas(
    modifier: Modifier,
    environment: EnvironmentalContext,
    law: LawContext,
    bodyExpression: BodyExpressionModel,
    growthImprint: GrowthImprintModel,
    growthState: GrowthState,
    territoryMap: TerritoryMap,
    showTerritoryDebugOverlay: Boolean = false,
    /** When the canvas is inside zoom/pan [graphicsLayer], map pointer back to layout space. */
    gestureScale: Float = 1f,
    gestureOffset: Offset = Offset.Zero,
    onCanvasSize: (widthPx: Float, heightPx: Float) -> Unit = { _, _ -> },
    onSubstrateTouch: (normalizedX: Float, normalizedY: Float) -> Unit = { _, _ -> },
) {
    val lawAllowsAnimation = SpeciesLaws.canAnimate(law)
    val baseDurationMs = 3200
    val breathDurationMs = (baseDurationMs / bodyExpression.breathRate.coerceIn(0.08f, 1f))
        .toInt()
        .coerceIn(900, 14_000)
    val pulse = rememberPulsePhase(if (lawAllowsAnimation) breathDurationMs else breathDurationMs * 2)
    val tint = thermalTint(environment.thermalRatio)

    var canvasW by remember { mutableStateOf(400f) }
    var canvasH by remember { mutableStateOf(260f) }

    Canvas(
        modifier = modifier
            .onSizeChanged { sz ->
                canvasW = sz.width.toFloat().coerceAtLeast(1f)
                canvasH = sz.height.toFloat().coerceAtLeast(1f)
                onCanvasSize(canvasW, canvasH)
            }
            .pointerInput(canvasW, canvasH, gestureScale, gestureOffset) {
                detectTapGestures { offset ->
                    val p = mapGestureToLayout(offset, canvasW, canvasH, gestureScale, gestureOffset)
                    onSubstrateTouch(
                        (p.x / canvasW).coerceIn(0f, 1f),
                        (p.y / canvasH).coerceIn(0f, 1f),
                    )
                }
            }
            .pointerInput(canvasW, canvasH, gestureScale, gestureOffset) {
                detectDragGestures { change, _ ->
                    val p = mapGestureToLayout(change.position, canvasW, canvasH, gestureScale, gestureOffset)
                    onSubstrateTouch(
                        (p.x / canvasW).coerceIn(0f, 1f),
                        (p.y / canvasH).coerceIn(0f, 1f),
                    )
                }
            },
    ) {
        drawGrowthField(
            growth = growthState,
            environment = environment,
            pulsePhase = pulse,
            baseTint = tint,
            expression = bodyExpression,
            imprint = growthImprint,
            nowMillis = System.currentTimeMillis(),
        )
        if (showTerritoryDebugOverlay) {
            drawTerritoryDebugOverlay(territoryMap)
        }
    }
}

private fun mapGestureToLayout(
    touch: Offset,
    w: Float,
    h: Float,
    scale: Float,
    translation: Offset,
): Offset {
    val s = scale.coerceAtLeast(0.05f)
    val cx = w * 0.5f
    val cy = h * 0.5f
    val x = (touch.x - translation.x - cx) / s + cx
    val y = (touch.y - translation.y - cy) / s + cy
    return Offset(x, y)
}
