package com.falcor.civilization.ui.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.falcor.civilization.engine.orchestrator.CycleOrchestrator

@Composable
fun ArenaTab(viewModel: MainViewModel) {
    val events = viewModel.arenaEvents.collectAsState().value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Adversary vs Detector",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "Skeptic injections vs Auditor/Detector outcomes",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.height(16.dp))

        if (events.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Science, null, tint = MaterialTheme.colorScheme.secondary)
                    Text(
                        "Run a Demo Cycle to see adversary/detector interactions.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(events) { event ->
                    ArenaEventCard(event)
                }
            }
        }
    }
}

@Composable
private fun ArenaEventCard(event: CycleOrchestrator.ArenaEvent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    event.runId.take(16) + "...",
                    style = MaterialTheme.typography.labelMedium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Chip(
                        label = { Text("Artifact: ${if (event.artifactInjected) "Yes" else "No"}") },
                        modifier = Modifier.height(24.dp)
                    )
                    Chip(
                        label = { Text("Triggered: ${if (event.detectorTriggered) "Yes" else "No"}") },
                        modifier = Modifier.height(24.dp)
                    )
                    Chip(
                        label = { Text("Caught: ${if (event.auditorCaught) "Yes" else "No"}") },
                        modifier = Modifier.height(24.dp),
                        colors = ChipDefaults.chipColors(
                            containerColor = if (event.auditorCaught)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }
            Icon(
                if (event.auditorCaught) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (event.auditorCaught)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.secondary
            )
        }
    }
}
