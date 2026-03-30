package com.collide.app.ui.archive

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.collide.app.domain.model.ReplayStatus
import com.collide.app.domain.model.SavedEvent
import com.collide.app.ui.collider.formatBytes
import com.collide.app.ui.theme.CollideColors
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ArchiveScreen(
    viewModel: ArchiveViewModel,
    onNavigateBack: () -> Unit,
    onEventClick: (Long) -> Unit
) {
    val events by viewModel.events.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CollideColors.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CollideColors.surface)
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = CollideColors.accent)
            }
            Column {
                Text("LAB DISCOVERIES", color = CollideColors.accent, fontSize = 14.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("Verified Exact-Reconstruction Winners Only",
                    color = CollideColors.muted, fontSize = 9.sp, letterSpacing = 1.sp)
            }
            Spacer(Modifier.weight(1f))
            Text("${events.size} events", color = CollideColors.muted, fontSize = 12.sp,
                modifier = Modifier.padding(end = 16.dp))
        }

        if (events.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Text("No discoveries yet.", color = CollideColors.muted, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Run the Compression Collider to find recipes that beat the baseline " +
                        "with verified exact reconstruction.",
                        color = CollideColors.muted.copy(alpha = 0.6f),
                        fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(events, key = { it.id }) { event ->
                    DiscoveryCard(event = event, onClick = { onEventClick(event.id) })
                }
            }
        }
    }
}

@Composable
private fun DiscoveryCard(event: SavedEvent, onClick: () -> Unit) {
    val dateStr = remember(event.timestamp) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(event.timestamp))
    }
    val replayStatus = runCatching { ReplayStatus.valueOf(event.replayStatus) }
        .getOrDefault(ReplayStatus.PENDING)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CollideColors.surface)
            .border(1.dp, CollideColors.winner.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("DISCOVERY #${event.id}",
                color = CollideColors.winner, fontSize = 11.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ReplayBadge(replayStatus)
                if (event.verificationPassed) {
                    Icon(Icons.Default.CheckCircle, "Verified Exact Reconstruction",
                        tint = CollideColors.winner, modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(event.inputFileName, color = CollideColors.onBackground, fontSize = 14.sp,
            fontWeight = FontWeight.Medium)
        Text(dateStr, color = CollideColors.muted, fontSize = 11.sp)

        if (event.hashesMatch) {
            Spacer(Modifier.height(2.dp))
            Text("SHA-256 Verified", color = CollideColors.winner.copy(0.8f), fontSize = 10.sp,
                letterSpacing = 0.5.sp)
        }

        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MiniStat("Original", formatBytes(event.inputSize), modifier = Modifier.weight(1f))
            MiniStat("Baseline", formatBytes(event.baselineSize), modifier = Modifier.weight(1f))
            MiniStat("Total Encoded", formatBytes(event.winningSize),
                valueColor = CollideColors.winner, modifier = Modifier.weight(1f))
            MiniStat("Saved", "%.1f%%".format(event.compressionRatioPct),
                valueColor = CollideColors.winner, modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(8.dp))
        Text(event.recipeSummary, color = CollideColors.accentAlt, fontSize = 11.sp, maxLines = 2)
    }
}

@Composable
private fun ReplayBadge(status: ReplayStatus) {
    val (label, color) = when (status) {
        ReplayStatus.MATCHED -> "Replayed ✓" to CollideColors.winner
        ReplayStatus.SIZE_MISMATCH -> "Replay ≠" to CollideColors.warning
        ReplayStatus.VERIFY_FAILED -> "Replay ✗" to CollideColors.failure
        ReplayStatus.ERROR -> "Replay ERR" to CollideColors.failure
        ReplayStatus.PENDING -> "Replay?" to CollideColors.muted
    }
    Text(label, color = color, fontSize = 9.sp, letterSpacing = 0.5.sp)
}

@Composable
private fun MiniStat(
    label: String, value: String,
    valueColor: Color = CollideColors.onSurface,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Text(label, color = CollideColors.muted, fontSize = 9.sp, letterSpacing = 0.5.sp)
    }
}
