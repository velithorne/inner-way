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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CollideColors.surface)
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CollideColors.accent)
            }
            Text(
                text = "EVENT ARCHIVE",
                color = CollideColors.accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "${events.size} events",
                color = CollideColors.muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(end = 16.dp)
            )
        }

        if (events.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "No events yet.",
                        color = CollideColors.muted,
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Run the Compression Collider to discover winning recipes.",
                        color = CollideColors.muted.copy(alpha = 0.6f),
                        fontSize = 13.sp
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
                    EventCard(event = event, onClick = { onEventClick(event.id) })
                }
            }
        }
    }
}

@Composable
private fun EventCard(event: SavedEvent, onClick: () -> Unit) {
    val dateStr = remember(event.timestamp) {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(event.timestamp))
    }
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
            Text(
                text = "EVENT #${event.id}",
                color = CollideColors.winner,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            if (event.verificationPassed) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Verified",
                    tint = CollideColors.winner,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = event.inputFileName,
            color = CollideColors.onBackground,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = dateStr,
            color = CollideColors.muted,
            fontSize = 11.sp
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MiniStat("Original", formatBytes(event.inputSize), modifier = Modifier.weight(1f))
            MiniStat("Baseline", formatBytes(event.baselineSize), modifier = Modifier.weight(1f))
            MiniStat("Winning", formatBytes(event.winningSize), valueColor = CollideColors.winner, modifier = Modifier.weight(1f))
            MiniStat("Saved", formatBytes(event.byteSavings), valueColor = CollideColors.winner, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = event.recipeSummary,
            color = CollideColors.accentAlt,
            fontSize = 11.sp,
            maxLines = 2
        )
    }
}

@Composable
private fun MiniStat(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = CollideColors.onSurface,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Text(label, color = CollideColors.muted, fontSize = 9.sp, letterSpacing = 0.5.sp)
    }
}
