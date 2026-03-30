package com.collide.app.ui.archive

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.collide.app.domain.model.EventType
import com.collide.app.domain.model.ReplayStatus
import com.collide.app.domain.model.SavedEvent
import com.collide.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ArchiveScreen(
    viewModel: ArchiveViewModel,
    onEventClick: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val events by viewModel.events.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CollideBackground)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CollideSurface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = CollideOnSurface)
            }
            Text(
                "Event Archive",
                style = MaterialTheme.typography.titleLarge.copy(color = CollideAccentAlt),
                modifier = Modifier.weight(1f)
            )
            Text(
                "${events.size} events",
                style = MaterialTheme.typography.labelSmall.copy(color = CollideDim)
            )
        }

        if (events.isEmpty()) {
            EmptyArchive()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(events, key = { it.id }) { event ->
                    EventCard(
                        event = event,
                        onClick = { onEventClick(event.id) }
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun EmptyArchive() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Archive,
            null,
            tint = CollideDim,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "No events yet",
            style = MaterialTheme.typography.headlineSmall.copy(color = CollideDim)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Run a collision to discover structural events",
            style = MaterialTheme.typography.bodySmall.copy(color = CollideDim.copy(alpha = 0.6f))
        )
    }
}

@Composable
private fun EventCard(event: SavedEvent, onClick: () -> Unit) {
    val eventColor = eventTypeColor(event.eventType)
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CollideCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, eventColor.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Event type chip
                Box(
                    modifier = Modifier
                        .background(eventColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .border(1.dp, eventColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        event.eventType.displayName,
                        style = MaterialTheme.typography.labelSmall.copy(color = eventColor)
                    )
                }

                Spacer(Modifier.weight(1f))

                // Replay status indicator
                if (event.replayStatus != ReplayStatus.NOT_REPLAYED) {
                    ReplayBadge(event.replayStatus)
                }

                // Timestamp
                Text(
                    sdf.format(Date(event.timestamp)),
                    style = MaterialTheme.typography.labelSmall.copy(color = CollideDim)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Inputs
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (event.inputLabelB != null) Icons.Default.CompareArrows
                    else Icons.Default.Transform,
                    null,
                    tint = CollideDim,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (event.inputLabelB != null) "${event.inputLabelA} ⊕ ${event.inputLabelB}"
                    else event.inputLabelA,
                    style = MaterialTheme.typography.titleSmall.copy(color = CollideOnBackground),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(4.dp))

            // Profile + replayable
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MiniChip(event.inputProfileA, CollideDim)
                event.inputProfileB?.let { MiniChip(it, CollideDim) }
                Spacer(Modifier.weight(1f))
                if (event.replayable) {
                    Icon(
                        Icons.Default.Replay, "replayable",
                        tint = CollideGreen.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Preview
            if (event.candidatePreview.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    event.candidatePreview.take(120).replace('\n', ' '),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = CollideDim.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    event.id.take(8),
                    style = MaterialTheme.typography.labelSmall.copy(color = CollideDim.copy(alpha = 0.4f))
                )
            }
        }
    }
}

@Composable
private fun ReplayBadge(status: ReplayStatus) {
    val (color, label) = when (status) {
        ReplayStatus.REPLAY_MATCHED -> CollideGreen to "matched"
        ReplayStatus.REPLAY_DIFFERED -> CollideAmber to "differed"
        ReplayStatus.REPLAY_FAILED -> CollideRed to "failed"
        ReplayStatus.NOT_REPLAYED -> CollideDim to "not replayed"
    }
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(color = color, fontSize = 10.sp))
    }
}

@Composable
private fun MiniChip(label: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(color = color, fontSize = 10.sp))
    }
}

private fun eventTypeColor(type: EventType) = when (type) {
    EventType.STRUCTURAL_SIMPLIFICATION -> CollideGreen
    EventType.HYBRID_STRUCTURE_FOUND -> CollideAccent
    EventType.REUSABLE_SCAFFOLD_EXTRACTED -> CollideAccentAlt
    EventType.SYMMETRY_HINT -> CollideAmber
    EventType.CONTRADICTION_DETECTED -> CollideRed
    EventType.UNEXPECTED_MERGE -> CollideAmber
    EventType.COHERENT_VARIANT -> CollideDim
}
