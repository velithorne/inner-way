package com.collide.app.ui.eventdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.collide.app.domain.engine.replay.EventReplayer
import com.collide.app.domain.model.ReplayStatus
import com.collide.app.domain.model.SavedEvent
import com.collide.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EventDetailScreen(
    eventId: String,
    viewModel: EventDetailViewModel,
    onNavigateBack: () -> Unit,
    onShare: (String) -> Unit
) {
    LaunchedEffect(eventId) { viewModel.loadEvent(eventId) }

    val uiState by viewModel.uiState.collectAsState()
    val shareSummary = uiState.shareSummary

    LaunchedEffect(shareSummary) {
        if (shareSummary != null) {
            onShare(shareSummary)
            viewModel.clearShareSummary()
        }
    }

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
                "Event Detail",
                style = MaterialTheme.typography.titleLarge.copy(color = CollideAccentAlt),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { viewModel.buildShareSummary() }) {
                Icon(Icons.Default.Share, "Share", tint = CollideDim)
            }
        }

        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CollideAccent)
                }
            }
            uiState.event == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Event not found", style = MaterialTheme.typography.bodyLarge.copy(color = CollideDim))
                }
            }
            else -> {
                val event = uiState.event!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EventHeaderSection(event)
                    InputMetadataSection(event)
                    CandidatePreviewSection(event)
                    DetectorResultsSection(event)
                    ReplaySection(
                        event = event,
                        replayResult = uiState.replayResult,
                        isReplaying = uiState.isReplaying,
                        onReplay = { viewModel.replayEvent(event.candidatePreview, null) }
                    )
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun EventHeaderSection(event: SavedEvent) {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CollideCard)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                event.eventType.displayName.uppercase(),
                style = MaterialTheme.typography.headlineSmall.copy(color = CollideAccent)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoItem("ID", event.id.take(12) + "...")
                InfoItem("Time", sdf.format(Date(event.timestamp)))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoItem("Mode", event.mode.name)
                InfoItem("Replayable", if (event.replayable) "Yes" else "No")
                InfoItem("Replay", event.replayStatus.name.replace('_', ' ').lowercase())
            }
        }
    }
}

@Composable
private fun InputMetadataSection(event: SavedEvent) {
    SectionCard(
        title = "INPUT METADATA",
        titleColor = CollideAccentAlt
    ) {
        MetaRow("Input A", event.inputLabelA)
        MetaRow("Profile A", event.inputProfileA)
        if (event.inputLabelB != null) {
            MetaRow("Input B", event.inputLabelB)
            MetaRow("Profile B", event.inputProfileB ?: "unknown")
        }
        MetaRow("Collision Mode", event.mode.name.replace('_', ' ').lowercase())
    }
}

@Composable
private fun CandidatePreviewSection(event: SavedEvent) {
    SectionCard(title = "CANDIDATE OUTPUT", titleColor = CollideAccent) {
        if (event.candidatePreview.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CollideSurface, RoundedCornerShape(6.dp))
                    .border(1.dp, CollideDim.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                    .padding(12.dp)
            ) {
                Text(
                    event.candidatePreview.take(500),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = CollideOnSurface,
                        lineHeight = 18.sp
                    )
                )
            }
            if (event.candidatePreview.length >= 500) {
                Text(
                    "...preview truncated to 500 chars",
                    style = MaterialTheme.typography.labelSmall.copy(color = CollideDim)
                )
            }
        } else {
            Text("No candidate preview available", style = MaterialTheme.typography.bodySmall.copy(color = CollideDim))
        }

        if (event.notes.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Notes: ${event.notes}",
                style = MaterialTheme.typography.bodySmall.copy(color = CollideDim)
            )
        }
    }
}

@Composable
private fun DetectorResultsSection(event: SavedEvent) {
    SectionCard(title = "DETECTOR RESULTS", titleColor = CollideGreen) {
        // Parse detector summary JSON for display
        val summaryLines = event.detectorSummaryJson
            .replace("[", "").replace("]", "")
            .split("},")
            .filter { it.isNotBlank() }
            .take(6)

        if (summaryLines.isEmpty()) {
            Text(event.detectorSummaryJson.take(300), style = MaterialTheme.typography.bodySmall.copy(
                color = CollideDim, fontFamily = FontFamily.Monospace
            ))
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                summaryLines.forEach { line ->
                    val passed = line.contains("\"passed\":true")
                    val nameMatch = Regex("\"name\":\"([^\"]+)\"").find(line)
                    val scoreMatch = Regex("\"score\":([\\d.]+)").find(line)
                    val reasonMatch = Regex("\"reason\":\"([^\"]+)\"").find(line)

                    val name = nameMatch?.groupValues?.get(1) ?: "Detector"
                    val score = scoreMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
                    val reason = reasonMatch?.groupValues?.get(1) ?: ""

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CollideSurface, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (passed) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            null,
                            tint = if (passed) CollideGreen else CollideDim,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            name,
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = if (passed) CollideGreen else CollideDim
                            ),
                            modifier = Modifier.width(140.dp)
                        )
                        Text(
                            "${(score * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = if (passed) CollideGreen else CollideDim
                            )
                        )
                        Text(
                            reason.take(80),
                            style = MaterialTheme.typography.labelSmall.copy(color = CollideDim),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        HonestDisclaimer("Detector results reflect structural measurements only. No behavioral or semantic correctness is implied.")
    }
}

@Composable
private fun ReplaySection(
    event: SavedEvent,
    replayResult: EventReplayer.ReplayResult?,
    isReplaying: Boolean,
    onReplay: () -> Unit
) {
    SectionCard(title = "REPLAY", titleColor = CollideAmber) {
        if (!event.replayable) {
            Text(
                "This event was marked non-replayable at save time.",
                style = MaterialTheme.typography.bodySmall.copy(color = CollideDim)
            )
        } else {
            Button(
                onClick = onReplay,
                enabled = !isReplaying,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CollideAmber.copy(alpha = 0.8f))
            ) {
                if (isReplaying) {
                    CircularProgressIndicator(
                        color = CollideBackground,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Replaying...")
                } else {
                    Icon(Icons.Default.Replay, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Replay Event")
                }
            }

            replayResult?.let { result ->
                Spacer(Modifier.height(8.dp))
                val (statusColor, statusLabel) = when (result.status) {
                    ReplayStatus.REPLAY_MATCHED -> CollideGreen to "Replay Matched"
                    ReplayStatus.REPLAY_DIFFERED -> CollideAmber to "Replay Differed"
                    ReplayStatus.REPLAY_FAILED -> CollideRed to "Replay Failed"
                    ReplayStatus.NOT_REPLAYED -> CollideDim to "Not Replayed"
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(statusColor.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                        .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(statusLabel, style = MaterialTheme.typography.titleSmall.copy(color = statusColor))
                        Text(result.notes, style = MaterialTheme.typography.bodySmall.copy(color = CollideDim))
                        if (result.detectorSummary.isNotBlank()) {
                            Text(
                                result.detectorSummary,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = CollideDim, fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    titleColor: Color = CollideOnBackground,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CollideCard)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = titleColor, letterSpacing = 2.sp
                )
            )
            content()
        }
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(color = CollideDim),
            modifier = Modifier.width(80.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall.copy(color = CollideOnSurface)
        )
    }
}

@Composable
private fun InfoItem(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(color = CollideDim, fontSize = 10.sp))
        Text(value, style = MaterialTheme.typography.bodySmall.copy(color = CollideOnSurface))
    }
}

@Composable
private fun HonestDisclaimer(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CollideDim.copy(alpha = 0.06f), RoundedCornerShape(4.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Info, null,
            tint = CollideDim,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelSmall.copy(color = CollideDim, fontSize = 10.sp))
    }
}
