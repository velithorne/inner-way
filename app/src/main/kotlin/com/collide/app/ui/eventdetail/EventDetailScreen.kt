package com.collide.app.ui.eventdetail

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.collide.app.domain.model.SavedEvent
import com.collide.app.domain.recipes.RecipeSerializer
import com.collide.app.ui.collider.LabCard
import com.collide.app.ui.collider.MetricRow
import com.collide.app.ui.collider.formatBytes
import com.collide.app.ui.theme.CollideColors
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EventDetailScreen(
    viewModel: EventDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val event by viewModel.event.collectAsStateWithLifecycle()
    val context = LocalContext.current

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
                text = "EVENT DETAIL",
                color = CollideColors.accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.weight(1f))
            event?.let { evt ->
                IconButton(onClick = {
                    val shareText = buildShareText(evt)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        putExtra(Intent.EXTRA_SUBJECT, "COLLIDE Event #${evt.id}")
                    }
                    context.startActivity(Intent.createChooser(intent, "Share Event"))
                }) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = CollideColors.accent)
                }
            }
        }

        if (event == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CollideColors.accent)
            }
            return@Column
        }

        val evt = event!!
        val dateStr = remember(evt.timestamp) {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US).format(Date(evt.timestamp))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Event header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CollideColors.winner.copy(alpha = 0.1f))
                    .border(1.dp, CollideColors.winner.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = CollideColors.winner,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "EVENT #${evt.id} · STRICT WINNER",
                        color = CollideColors.winner,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(dateStr, color = CollideColors.muted, fontSize = 11.sp)
                }
            }

            LabCard(title = "INPUT METADATA") {
                MetricRow("File Name", evt.inputFileName)
                MetricRow("Original Size", formatBytes(evt.inputSize))
                MetricRow("Baseline Strategy", evt.baselineType)
                MetricRow("Baseline Size", formatBytes(evt.baselineSize))
            }

            LabCard(title = "COLLISION RESULT") {
                MetricRow("Winning Encoded Size", formatBytes(evt.winningSize), valueColor = CollideColors.winner)
                MetricRow("Byte Savings", formatBytes(evt.byteSavings), valueColor = CollideColors.winner)
                MetricRow(
                    "Compression Gain",
                    "%.1f%%".format(evt.compressionRatioPct),
                    valueColor = CollideColors.winner
                )
                MetricRow("Verification Passed", if (evt.verificationPassed) "YES" else "NO",
                    valueColor = if (evt.verificationPassed) CollideColors.winner else CollideColors.failure)
                MetricRow("Elapsed", "${evt.elapsedMs} ms")
            }

            LabCard(title = "RECIPE CHAIN") {
                Text(
                    text = evt.recipeSummary,
                    color = CollideColors.accentAlt,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Full recipe JSON:",
                    color = CollideColors.muted,
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(CollideColors.surfaceVariant)
                        .padding(10.dp)
                ) {
                    Text(
                        text = formatRecipeJson(evt.recipeJson),
                        color = CollideColors.onSurface,
                        fontSize = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }

            if (evt.notes.isNotEmpty()) {
                LabCard(title = "COLLISION NOTES") {
                    Text(text = evt.notes, color = CollideColors.onSurface, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

private fun formatRecipeJson(json: String): String {
    return try {
        val recipe = RecipeSerializer.deserialize(json)
        buildString {
            appendLine("id: ${recipe.id}")
            appendLine("transforms:")
            recipe.transformIds.forEach { appendLine("  - $it") }
            appendLine("backend: ${recipe.backend.displayName}")
        }.trim()
    } catch (e: Exception) {
        json
    }
}

private fun buildShareText(event: SavedEvent): String = buildString {
    appendLine("COLLIDE — Compression Collision Event")
    appendLine("=====================================")
    appendLine("Event ID:      #${event.id}")
    appendLine("Timestamp:     ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US).format(Date(event.timestamp))}")
    appendLine()
    appendLine("INPUT")
    appendLine("  File:        ${event.inputFileName}")
    appendLine("  Size:        ${formatBytes(event.inputSize)}")
    appendLine()
    appendLine("BASELINE")
    appendLine("  Strategy:    ${event.baselineType}")
    appendLine("  Size:        ${formatBytes(event.baselineSize)}")
    appendLine()
    appendLine("RESULT")
    appendLine("  Winning:     ${formatBytes(event.winningSize)}")
    appendLine("  Savings:     ${formatBytes(event.byteSavings)}")
    appendLine("  Gain:        ${"%.1f".format(event.compressionRatioPct)}%")
    appendLine("  Verified:    ${if (event.verificationPassed) "YES" else "NO"}")
    appendLine()
    appendLine("RECIPE")
    appendLine("  ${event.recipeSummary}")
    appendLine()
    appendLine("  JSON: ${event.recipeJson}")
    appendLine()
    if (event.notes.isNotEmpty()) {
        appendLine("NOTES")
        appendLine("  ${event.notes}")
    }
}
