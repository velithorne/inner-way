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
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.collide.app.domain.model.ReplayStatus
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

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
            Text("EVENT DETAIL", color = CollideColors.accent, fontSize = 14.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.weight(1f))
            uiState.event?.let { evt ->
                IconButton(onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, buildShareText(evt, uiState.replayResult))
                        putExtra(Intent.EXTRA_SUBJECT, "COLLIDE Event #${evt.id}")
                    }
                    context.startActivity(Intent.createChooser(intent, "Share Event"))
                }) {
                    Icon(Icons.Default.Share, null, tint = CollideColors.accent)
                }
            }
        }

        when {
            uiState.event == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CollideColors.accent)
                }
            }
            else -> {
                val evt = uiState.event!!
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
                    // ── Header badge ────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CollideColors.winner.copy(0.1f))
                            .border(1.dp, CollideColors.winner.copy(0.5f), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = CollideColors.winner,
                            modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("EVENT #${evt.id} · VERIFIED EXACT RECONSTRUCTION WINNER",
                                color = CollideColors.winner, fontSize = 12.sp,
                                fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                            Text(dateStr, color = CollideColors.muted, fontSize = 11.sp)
                        }
                    }

                    // ── Input metadata ──────────────────────────────────────
                    LabCard(title = "INPUT METADATA") {
                        MetricRow("File Name", evt.inputFileName)
                        MetricRow("Original Size", formatBytes(evt.inputSize))
                        MetricRow("Baseline Strategy", evt.baselineType)
                        MetricRow("Baseline Total Size", formatBytes(evt.baselineSize))
                        MetricRow("Candidate Index", if (evt.candidateIndex >= 0) "#${evt.candidateIndex}" else "—")
                        MetricRow("Engine Version", evt.engineVersion)
                    }

                    // ── Collision result ────────────────────────────────────
                    LabCard(title = "COLLISION RESULT") {
                        MetricRow("Total Encoded Size", formatBytes(evt.winningSize),
                            valueColor = CollideColors.winner)
                        MetricRow("Byte Savings", formatBytes(evt.byteSavings),
                            valueColor = CollideColors.winner)
                        MetricRow("Percent Savings", "%.2f%%".format(evt.compressionRatioPct),
                            valueColor = CollideColors.winner)
                        MetricRow("Elapsed", "${evt.elapsedMs} ms")
                    }

                    // ── Verification ────────────────────────────────────────
                    LabCard(title = "VERIFICATION STATUS") {
                        MetricRow("Verification Passed",
                            if (evt.verificationPassed) "YES" else "NO",
                            valueColor = if (evt.verificationPassed) CollideColors.winner else CollideColors.failure)
                        MetricRow("Method", evt.verificationMethod.replace('_', ' '))
                        if (evt.originalSha256.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text("Original SHA-256", color = CollideColors.muted, fontSize = 11.sp)
                            HashText(evt.originalSha256)
                            Spacer(Modifier.height(4.dp))
                            Text("Reconstructed SHA-256", color = CollideColors.muted, fontSize = 11.sp)
                            HashText(evt.reconstructedSha256)
                            Spacer(Modifier.height(4.dp))
                            MetricRow("Hashes Match",
                                if (evt.hashesMatch) "YES" else "NO",
                                valueColor = if (evt.hashesMatch) CollideColors.winner else CollideColors.failure)
                        } else {
                            Text("SHA-256 not available (Phase 1 event)",
                                color = CollideColors.muted, fontSize = 11.sp)
                        }
                    }

                    // ── Size breakdown ──────────────────────────────────────
                    evt.sizeBreakdown()?.let { bd ->
                        LabCard(title = "TOTAL ENCODED SIZE BREAKDOWN") {
                            bd.toDisplayMap().forEach { (label, value) ->
                                val isTotal = label == "Total Encoded"
                                MetricRow(label, value,
                                    valueColor = if (isTotal) CollideColors.winner else CollideColors.onSurface)
                            }
                        }
                    }

                    // ── Recipe chain ────────────────────────────────────────
                    val parsedRecipe = remember(evt.recipeJson) {
                        runCatching { RecipeSerializer.deserialize(evt.recipeJson) }.getOrNull()
                    }
                    LabCard(title = "RECIPE CHAIN") {
                        Text(evt.recipeSummary, color = CollideColors.accentAlt, fontSize = 13.sp,
                            fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(8.dp))
                        if (parsedRecipe != null) {
                            parsedRecipe.transformIds.forEachIndexed { i, id ->
                                Text("Step ${i + 1}: $id", color = CollideColors.onSurface, fontSize = 12.sp)
                            }
                            MetricRow("Backend", parsedRecipe.backend.displayName)
                        } else {
                            Text(evt.recipeJson, color = CollideColors.onSurface, fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace)
                        }
                    }

                    // ── Replay ──────────────────────────────────────────────
                    LabCard(title = "REPLAY") {
                        val replayStatus = runCatching { ReplayStatus.valueOf(evt.replayStatus) }
                            .getOrDefault(ReplayStatus.PENDING)
                        MetricRow("Replay Status", replayStatus.name,
                            valueColor = when (replayStatus) {
                                ReplayStatus.MATCHED -> CollideColors.winner
                                ReplayStatus.SIZE_MISMATCH -> CollideColors.warning
                                ReplayStatus.VERIFY_FAILED, ReplayStatus.ERROR -> CollideColors.failure
                                ReplayStatus.PENDING -> CollideColors.muted
                            })

                        uiState.replayResult?.let { rr ->
                            Spacer(Modifier.height(6.dp))
                            MetricRow("Replay Encoded Size", formatBytes(rr.replayEncodedSize))
                            MetricRow("Replay Verification",
                                if (rr.replayVerificationPassed) "PASSED" else "FAILED",
                                valueColor = if (rr.replayVerificationPassed) CollideColors.winner else CollideColors.failure)
                            MetricRow("Replay Elapsed", "${rr.replayElapsedMs} ms")
                            rr.errorMessage?.let { Text(it, color = CollideColors.warning, fontSize = 11.sp) }
                        }

                        uiState.replayError?.let {
                            Spacer(Modifier.height(4.dp))
                            Text(it, color = CollideColors.warning, fontSize = 11.sp)
                        }

                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { viewModel.startReplay(context) },
                            enabled = !uiState.isReplaying,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CollideColors.accentAlt,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (uiState.isReplaying) {
                                CircularProgressIndicator(color = Color.White,
                                    modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Replaying…")
                            } else {
                                Icon(Icons.Default.Replay, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Replay Event")
                            }
                        }
                    }

                    if (evt.notes.isNotEmpty()) {
                        LabCard(title = "NOTES") {
                            Text(evt.notes, color = CollideColors.onSurface, fontSize = 13.sp)
                        }
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun HashText(hash: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(CollideColors.surfaceVariant)
            .padding(6.dp)
    ) {
        Text(hash, color = CollideColors.onSurface, fontSize = 9.sp,
            fontFamily = FontFamily.Monospace, maxLines = 2)
    }
}

private fun buildShareText(event: SavedEvent, replayResult: com.collide.app.domain.model.ReplayResult?): String =
    buildString {
        appendLine("COLLIDE — Verified Exact-Reconstruction Compression Event")
        appendLine("=".repeat(55))
        appendLine("Event ID:          #${event.id}")
        appendLine("Timestamp:         ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US).format(Date(event.timestamp))}")
        appendLine("Engine Version:    ${event.engineVersion}")
        appendLine()
        appendLine("INPUT")
        appendLine("  File:            ${event.inputFileName}")
        appendLine("  Original Size:   ${formatBytes(event.inputSize)}")
        appendLine()
        appendLine("BASELINE")
        appendLine("  Strategy:        ${event.baselineType}")
        appendLine("  Total Size:      ${formatBytes(event.baselineSize)}")
        appendLine()
        appendLine("COLLISION RESULT")
        appendLine("  Total Encoded:   ${formatBytes(event.winningSize)}")
        appendLine("  Byte Savings:    ${formatBytes(event.byteSavings)}")
        appendLine("  Percent Savings: ${"%.2f".format(event.compressionRatioPct)}%")
        appendLine("  Elapsed:         ${event.elapsedMs} ms")
        appendLine()
        appendLine("VERIFICATION")
        appendLine("  Passed:          ${if (event.verificationPassed) "YES" else "NO"}")
        appendLine("  Method:          ${event.verificationMethod}")
        if (event.originalSha256.isNotEmpty()) {
            appendLine("  Original SHA-256:       ${event.originalSha256}")
            appendLine("  Reconstructed SHA-256:  ${event.reconstructedSha256}")
            appendLine("  Hashes Match:           ${if (event.hashesMatch) "YES" else "NO"}")
        }
        appendLine()
        event.sizeBreakdown()?.let { bd ->
            appendLine("SIZE BREAKDOWN")
            bd.toDisplayMap().forEach { (k, v) -> appendLine("  $k: $v") }
            appendLine()
        }
        appendLine("RECIPE")
        appendLine("  ${event.recipeSummary}")
        appendLine()
        replayResult?.let {
            appendLine("REPLAY")
            appendLine("  Status:          ${it.status.name}")
            appendLine("  Replay Size:     ${formatBytes(it.replayEncodedSize)}")
            appendLine("  Verified:        ${if (it.replayVerificationPassed) "YES" else "NO"}")
        }
    }
