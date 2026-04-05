package com.velithorne.innerway.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.velithorne.innerway.identity.LawContext
import com.velithorne.innerway.mind.BodyExpressionModel
import com.velithorne.innerway.mind.GrowthImprintModel
import com.velithorne.innerway.perception.EnvironmentalContext
import com.velithorne.innerway.render.GrowthState
import com.velithorne.innerway.render.TerritoryMap

private const val MIN_SCALE = 0.35f
private const val MAX_SCALE = 5f

@Composable
fun SpeciesFullscreenViewer(
    visible: Boolean,
    onDismiss: () -> Unit,
    environment: EnvironmentalContext,
    law: LawContext,
    bodyExpression: BodyExpressionModel,
    growthImprint: GrowthImprintModel,
    growthState: GrowthState,
    territoryMap: TerritoryMap,
    showTerritoryDebugOverlay: Boolean = false,
    onCanvasSize: (widthPx: Float, heightPx: Float) -> Unit,
    onSubstrateTouch: (normalizedX: Float, normalizedY: Float) -> Unit = { _, _ -> },
) {
    if (!visible) return

    var scale by remember(visible) { mutableFloatStateOf(1f) }
    var offset by remember(visible) { mutableStateOf(Offset.Zero) }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(MIN_SCALE, MAX_SCALE)
        offset += panChange
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                    Text(
                        text = "Velithorne — substrate",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                scale = (scale / 1.2f).coerceIn(MIN_SCALE, MAX_SCALE)
                            },
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Zoom out")
                        }
                        IconButton(
                            onClick = {
                                scale = (scale * 1.2f).coerceIn(MIN_SCALE, MAX_SCALE)
                            },
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Zoom in")
                        }
                        Spacer(Modifier.width(4.dp))
                        IconButton(
                            onClick = {
                                scale = 1f
                                offset = Offset.Zero
                            },
                        ) {
                            Icon(Icons.Default.CenterFocusStrong, contentDescription = "Reset view")
                        }
                    }
                }
                Text(
                    text = "Pinch to zoom · drag to pan",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    SubstrateGrowthCanvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y,
                                transformOrigin = TransformOrigin(0.5f, 0.5f),
                            )
                            .transformable(transformableState),
                        environment = environment,
                        law = law,
                        bodyExpression = bodyExpression,
                        growthImprint = growthImprint,
                        growthState = growthState,
                        territoryMap = territoryMap,
                        showTerritoryDebugOverlay = showTerritoryDebugOverlay,
                        gestureScale = scale,
                        gestureOffset = offset,
                        onCanvasSize = onCanvasSize,
                        onSubstrateTouch = onSubstrateTouch,
                    )
                }
            }
        }
    }
}
