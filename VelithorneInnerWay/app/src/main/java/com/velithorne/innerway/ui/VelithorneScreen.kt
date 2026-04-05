package com.velithorne.innerway.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VelithorneScreen(viewModel: VelithorneViewModel) {
    val environment by viewModel.environment.collectAsState()
    val state by viewModel.internalState.collectAsState()
    val bodyExpression by viewModel.bodyExpression.collectAsState()
    val growthState by viewModel.growthState.collectAsState()
    val growthImprint by viewModel.growthImprint.collectAsState()
    val growthDebug by viewModel.growthDebug.collectAsState()
    val stage by viewModel.stage.collectAsState()
    val memories by viewModel.memories.collectAsState()
    val law by viewModel.lawContext.collectAsState()
    val somaticHints by viewModel.somaticHints.collectAsState()
    var debug by remember { mutableStateOf(false) }
    var fullscreenSpecies by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Velithorne Inner Way",
            fontSize = 22.sp,
            fontFamily = FontFamily.SansSerif,
        )
        Text(
            text = "Substrate growth — state + stage",
            fontSize = 13.sp,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = { fullscreenSpecies = true }) {
                Icon(Icons.Default.OpenInFull, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Fullscreen species")
            }
        }

        AmbientBodyView(
            environment = environment,
            internalState = state,
            stage = stage,
            law = law,
            bodyExpression = bodyExpression,
            growthImprint = growthImprint,
            growthState = growthState,
            onCanvasSize = { w, h -> viewModel.setGrowthCanvasSize(w, h) },
        )

        SpeciesFullscreenViewer(
            visible = fullscreenSpecies,
            onDismiss = { fullscreenSpecies = false },
            environment = environment,
            law = law,
            bodyExpression = bodyExpression,
            growthImprint = growthImprint,
            growthState = growthState,
            onCanvasSize = { w, h -> viewModel.setGrowthCanvasSize(w, h) },
        )

        StatusOverlay(
            stage = stage,
            environment = environment,
            internalState = state,
            lastMemory = memories.firstOrNull(),
        )

        EvolutionTimelineView(memories = memories)

        MemoryView(memories = memories)

        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Debug body panel")
            Switch(checked = debug, onCheckedChange = { debug = it })
        }

        if (debug) {
            DebugBodyPanel(
                environment = environment,
                internalState = state,
                law = law,
                somaticHints = somaticHints,
                bodyExpression = bodyExpression,
                stage = stage,
                growthDebug = growthDebug,
                growthImprint = growthImprint,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
