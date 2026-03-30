package com.collide.app.ui.collider

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.collide.app.domain.engine.RunResult
import com.collide.app.domain.engine.RunStatus
import com.collide.app.domain.model.BaselineStrategy
import com.collide.app.domain.model.CandidateResult
import com.collide.app.domain.model.RunMode
import com.collide.app.ui.theme.CollideColors

@Composable
fun ColliderScreen(
    viewModel: ColliderViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val progress by viewModel.runProgress.collectAsStateWithLifecycle()

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { viewModel.loadFile(context, it) } }

    var showFixturePicker by remember { mutableStateOf(false) }
    var showRunSummary by remember { mutableStateOf(false) }

    val isRunning = progress.status == RunStatus.RUNNING

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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CollideColors.accent)
            }
            Text(
                "COMPRESSION COLLIDER",
                color = CollideColors.accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FileSelectionCard(
                fileName = uiState.selectedFileName,
                fileSize = uiState.selectedFileSize,
                mimeType = uiState.selectedMimeType,
                fileLoadError = uiState.fileLoadError,
                onPickFile = { filePickerLauncher.launch(arrayOf("*/*")) },
                onPickFixture = { showFixturePicker = true },
                enabled = !isRunning
            )

            ConfigCard(
                baseline = uiState.selectedBaseline,
                runMode = uiState.selectedRunMode,
                maxCandidates = uiState.maxCandidates,
                maxChainLength = uiState.maxChainLength,
                onBaselineChange = viewModel::setBaseline,
                onRunModeChange = viewModel::setRunMode,
                onMaxCandidatesChange = viewModel::setMaxCandidates,
                onMaxChainLengthChange = viewModel::setMaxChainLength,
                enabled = !isRunning
            )

            RunControlBar(
                isRunning = isRunning,
                fileLoaded = uiState.selectedFileName != null,
                onStart = viewModel::startRun,
                onCancel = viewModel::cancelRun
            )

            if (progress.status != RunStatus.IDLE) {
                LiveRunPanel(progress = progress)
            }

            uiState.lastRunResult?.let { result ->
                if (!showRunSummary) {
                    RunSummaryBanner(
                        result = result,
                        onExpand = { showRunSummary = true },
                        onDismiss = {
                            viewModel.clearLastRunResult()
                            showRunSummary = false
                        }
                    )
                } else {
                    RunSummaryCard(result = result, onDismiss = {
                        viewModel.clearLastRunResult()
                        showRunSummary = false
                    })
                }
            }

            uiState.lastRunMessage?.let { msg ->
                RunResultBanner(message = msg, onDismiss = viewModel::clearLastRunMessage)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showFixturePicker) {
        FixturePickerDialog(
            onSelect = {
                viewModel.loadBuiltInFixture(context, it)
                showFixturePicker = false
            },
            onDismiss = { showFixturePicker = false }
        )
    }
}

@Composable
private fun FileSelectionCard(
    fileName: String?,
    fileSize: Long,
    mimeType: String?,
    fileLoadError: String?,
    onPickFile: () -> Unit,
    onPickFixture: () -> Unit,
    enabled: Boolean
) {
    LabCard(title = "INPUT FILE") {
        if (fileName != null) {
            MetricRow("File", fileName)
            MetricRow("Size", formatBytes(fileSize))
            if (mimeType != null) MetricRow("Type", mimeType)
        } else {
            Text("No file selected", color = CollideColors.muted, fontSize = 13.sp)
        }
        fileLoadError?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, color = CollideColors.failure, fontSize = 12.sp)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onPickFile,
                enabled = enabled,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CollideColors.accent),
                border = androidx.compose.foundation.BorderStroke(1.dp, CollideColors.accent.copy(alpha = 0.6f)),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Browse", fontSize = 13.sp)
            }
            OutlinedButton(
                onClick = onPickFixture,
                enabled = enabled,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CollideColors.accentAlt),
                border = androidx.compose.foundation.BorderStroke(1.dp, CollideColors.accentAlt.copy(alpha = 0.6f)),
                modifier = Modifier.weight(1f)
            ) {
                Text("Fixtures", fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun ConfigCard(
    baseline: BaselineStrategy, runMode: RunMode,
    maxCandidates: Int, maxChainLength: Int,
    onBaselineChange: (BaselineStrategy) -> Unit, onRunModeChange: (RunMode) -> Unit,
    onMaxCandidatesChange: (Int) -> Unit, onMaxChainLengthChange: (Int) -> Unit,
    enabled: Boolean
) {
    LabCard(title = "CONFIGURATION") {
        Text("Baseline", color = CollideColors.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BaselineStrategy.entries.forEach { s ->
                ChipOption(s.displayName, baseline == s, enabled) { onBaselineChange(s) }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text("Run Mode", color = CollideColors.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RunMode.entries.forEach { m ->
                ChipOption(m.displayName, runMode == m, enabled) { onRunModeChange(m) }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text("Max Candidates: $maxCandidates", color = CollideColors.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Slider(
            value = maxCandidates.toFloat(),
            onValueChange = { onMaxCandidatesChange(it.toInt()) },
            valueRange = 10f..200f, steps = 18, enabled = enabled,
            colors = SliderDefaults.colors(thumbColor = CollideColors.accent, activeTrackColor = CollideColors.accent)
        )
        Text("Max Chain Length: $maxChainLength", color = CollideColors.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Slider(
            value = maxChainLength.toFloat(),
            onValueChange = { onMaxChainLengthChange(it.toInt()) },
            valueRange = 1f..4f, steps = 2, enabled = enabled,
            colors = SliderDefaults.colors(thumbColor = CollideColors.accentAlt, activeTrackColor = CollideColors.accentAlt)
        )
    }
}

@Composable
private fun RunControlBar(isRunning: Boolean, fileLoaded: Boolean, onStart: () -> Unit, onCancel: () -> Unit) {
    if (!isRunning) {
        Button(
            onClick = onStart, enabled = fileLoaded,
            colors = ButtonDefaults.buttonColors(
                containerColor = CollideColors.accent, contentColor = Color(0xFF003040),
                disabledContainerColor = CollideColors.muted.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Icon(Icons.Default.PlayArrow, null)
            Spacer(Modifier.width(8.dp))
            Text("START RUN", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    } else {
        Button(
            onClick = onCancel,
            colors = ButtonDefaults.buttonColors(containerColor = CollideColors.failure, contentColor = Color.White),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Icon(Icons.Default.Stop, null)
            Spacer(Modifier.width(8.dp))
            Text("CANCEL", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun LiveRunPanel(progress: com.collide.app.domain.engine.RunProgress) {
    LabCard(title = "LIVE RUN") {
        MetricRow("File", progress.fileName)
        MetricRow("Status", progress.status.name)
        MetricRow("Candidates Generated", progress.candidatesGenerated.toString())
        MetricRow("Candidates Evaluated", progress.candidatesEvaluated.toString())
        MetricRow("Exactness Failures", progress.exactnessFailures.toString(),
            valueColor = if (progress.exactnessFailures > 0) CollideColors.warning else CollideColors.onSurface)
        MetricRow("Hash Mismatches", progress.hashMismatches.toString(),
            valueColor = if (progress.hashMismatches > 0) CollideColors.failure else CollideColors.onSurface)
        MetricRow("No-Gain", progress.noGainResults.toString())
        MetricRow("Verified Winners", progress.winnersFound.toString(),
            valueColor = if (progress.winnersFound > 0) CollideColors.winner else CollideColors.onSurface)
        MetricRow("Best Total Encoded Size",
            if (progress.bestSizeSoFar == Long.MAX_VALUE) "—" else formatBytes(progress.bestSizeSoFar))
        MetricRow("Baseline Size", formatBytes(progress.baselineSize))
        MetricRow("Elapsed", "${progress.elapsedMs} ms")
        Spacer(Modifier.height(6.dp))
        Text(
            "Recipe: ${progress.currentRecipe.ifEmpty { "—" }}",
            color = CollideColors.muted, fontSize = 11.sp, maxLines = 2,
            overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall
        )
        if (progress.status == RunStatus.RUNNING) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = CollideColors.accent, trackColor = CollideColors.surfaceVariant
            )
        }
    }
}

@Composable
private fun RunSummaryBanner(result: RunResult, onExpand: () -> Unit, onDismiss: () -> Unit) {
    val stats = result.stats
    val hasWinners = stats.strictWinnerCount > 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (hasWinners) CollideColors.winner.copy(alpha = 0.12f) else CollideColors.surface)
            .border(1.dp,
                if (hasWinners) CollideColors.winner.copy(alpha = 0.4f) else CollideColors.muted.copy(0.3f),
                RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                if (hasWinners) "${stats.strictWinnerCount} Verified Exact-Reconstruction Winner(s)"
                else "Run Complete — No Strict Improvements Found",
                color = if (hasWinners) CollideColors.winner else CollideColors.onSurface,
                fontSize = 13.sp, fontWeight = FontWeight.Medium
            )
            Text(
                "${stats.candidatesEvaluated} evaluated · ${stats.exactnessFailures} exactness failures · ${stats.noGainCount} no-gain",
                color = CollideColors.muted, fontSize = 11.sp
            )
        }
        TextButton(onClick = onExpand) { Text("Details", color = CollideColors.accent, fontSize = 12.sp) }
        TextButton(onClick = onDismiss) { Text("OK", color = CollideColors.muted, fontSize = 12.sp) }
    }
}

@Composable
private fun RunSummaryCard(result: RunResult, onDismiss: () -> Unit) {
    val stats = result.stats
    val baseline = result.baselineResult
    val topFive = result.allResults
        .filter { it.encodedSize > 0 }
        .sortedBy { it.encodedSize }
        .take(5)

    LabCard(title = "RUN SUMMARY — LAB REPORT") {
        MetricRow("Baseline Strategy", baseline.strategy.displayName)
        MetricRow("Baseline Total Size", formatBytes(baseline.encodedSize))
        Spacer(Modifier.height(8.dp))
        MetricRow("Candidates Seen", stats.candidatesSeen.toString())
        MetricRow("Pruned Pre-Eval", stats.candidatesPrunedPreEval.toString())
        MetricRow("Candidates Evaluated", stats.candidatesEvaluated.toString())
        MetricRow("Verified Exact-Reconstruction Winners", stats.strictWinnerCount.toString(),
            valueColor = if (stats.strictWinnerCount > 0) CollideColors.winner else CollideColors.onSurface)
        MetricRow("No Strict Improvement", stats.noGainCount.toString())
        MetricRow("Exactness Failures", stats.exactnessFailures.toString(),
            valueColor = if (stats.exactnessFailures > 0) CollideColors.warning else CollideColors.onSurface)
        MetricRow("Hash Mismatches", stats.hashMismatches.toString(),
            valueColor = if (stats.hashMismatches > 0) CollideColors.failure else CollideColors.onSurface)
        MetricRow("Not Applicable", stats.notApplicableCount.toString())
        MetricRow("Encode Errors", stats.encodeErrors.toString())
        MetricRow("Decode Errors", stats.decodeErrors.toString())
        MetricRow("Total Elapsed", "${stats.elapsedMs} ms")

        if (topFive.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("Top 5 by Total Encoded Size", color = CollideColors.muted, fontSize = 11.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(4.dp))
            topFive.forEachIndexed { i, r ->
                val isWinner = r.isWinner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isWinner) CollideColors.winner.copy(0.08f) else CollideColors.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "#${i + 1} ${r.recipeSpec.summary()}",
                        color = if (isWinner) CollideColors.winner else CollideColors.onSurface,
                        fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(formatBytes(r.encodedSize), color = CollideColors.accent, fontSize = 10.sp)
                }
                Spacer(Modifier.height(3.dp))
            }
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
            Text("Dismiss", color = CollideColors.muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun RunResultBanner(message: String, onDismiss: () -> Unit) {
    val isWin = message.contains("winner")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isWin) CollideColors.winner.copy(0.15f) else CollideColors.surface)
            .border(1.dp,
                if (isWin) CollideColors.winner.copy(0.5f) else CollideColors.muted.copy(0.3f),
                RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(message, color = if (isWin) CollideColors.winner else CollideColors.onSurface,
            fontSize = 13.sp, modifier = Modifier.weight(1f))
        TextButton(onClick = onDismiss) { Text("OK", color = CollideColors.accent, fontSize = 12.sp) }
    }
}

@Composable
private fun FixturePickerDialog(onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val fixtures = listOf(
        "fixtures/sample_text.txt" to "Plain text",
        "fixtures/sample_data.json" to "JSON data",
        "fixtures/sample_table.csv" to "CSV table",
        "fixtures/sample_binary.bin" to "Repetitive binary",
        "fixtures/sample_mixed.txt" to "Mixed structured text",
        "fixtures/sample_source.kt" to "Kotlin source code",
        "fixtures/sample_dense.bin" to "Dense binary"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Built-in Fixtures", color = CollideColors.accent, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                fixtures.forEach { (path, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CollideColors.surfaceVariant)
                            .clickable { onSelect(path) }
                            .padding(12.dp)
                    ) {
                        Text(label, color = CollideColors.onBackground, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = CollideColors.muted) } },
        containerColor = CollideColors.surface
    )
}

// ── Reusable composables ──────────────────────────────────────────────────────

@Composable
fun LabCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CollideColors.surface)
            .border(1.dp, CollideColors.muted.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(title, color = CollideColors.muted, fontSize = 10.sp, letterSpacing = 2.sp,
            fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
fun MetricRow(label: String, value: String, valueColor: Color = CollideColors.onSurface) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = CollideColors.muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.Medium,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
}

@Composable
private fun ChipOption(text: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) CollideColors.accent.copy(0.2f) else CollideColors.surfaceVariant)
            .border(1.dp, if (selected) CollideColors.accent else CollideColors.muted.copy(0.3f), RoundedCornerShape(20.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, color = if (selected) CollideColors.accent else CollideColors.muted,
            fontSize = 12.sp, fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal)
    }
}

fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "%.2f MB".format(bytes / (1024.0 * 1024.0))
}
