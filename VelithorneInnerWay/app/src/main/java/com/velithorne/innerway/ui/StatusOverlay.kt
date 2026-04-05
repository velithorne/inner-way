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
import com.velithorne.innerway.memory.MemoryEntity
import com.velithorne.innerway.mind.GrowthStage
import com.velithorne.innerway.mind.InternalState
import com.velithorne.innerway.perception.EnvironmentalContext

@Composable
fun StatusOverlay(
    stage: GrowthStage,
    environment: EnvironmentalContext,
    internalState: InternalState,
    lastMemory: MemoryEntity?,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Stage: ${stage.name}", style = MaterialTheme.typography.titleMedium)
            Text("State: ${internalState.name}", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Energy: ${(environment.energyRatio * 100).toInt()}%  ·  Heat stress: ${(environment.thermalRatio * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Rhythm: ${"%.2f".format(environment.circadianPhase)}  ·  Atmosphere: ${"%.2f".format(environment.networkOpenness)}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "Last memory: ${lastMemory?.importantEvents ?: "—"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            )
        }
    }
}
