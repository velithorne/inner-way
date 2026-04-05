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
import com.velithorne.innerway.memory.MemoryKind

@Composable
fun EvolutionTimelineView(memories: List<MemoryEntity>) {
    val events = memories.filter {
        it.memoryKind == MemoryKind.EVOLUTION_UNLOCK || !it.growthTransition.isNullOrBlank()
    }.take(6)

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Evolution timeline", style = MaterialTheme.typography.titleMedium)
            if (events.isEmpty()) {
                Text(
                    "No major transitions yet — continuity is still forming.",
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                events.forEach { m ->
                    Text(
                        "• ${m.growthTransition ?: m.memoryKind.name}: ${m.importantEvents}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
