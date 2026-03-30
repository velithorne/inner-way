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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.collide.app.domain.model.*
import com.collide.app.ui.theme.*

@Composable
fun ColliderScreen(
    viewModel: ColliderViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val filePickerA = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { viewModel.loadFileA(context, it) } }

    val filePickerB = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { viewModel.loadFileB(context, it) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CollideBackground)
    ) {
        // Top bar
        ColliderTopBar(
            onBack = onNavigateBack,
            status = uiState.runStatus
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Two-input mode toggle
            TwoInputToggle(
                enabled = uiState.twoInputMode,
                onToggle = { viewModel.setTwoInputMode(it) }
            )

            // Input A
            InputPanel(
                label = uiState.inputLabelA,
                text = uiState.inputTextA,
                profile = uiState.profileA,
                slot = "A",
                onTextChange = { viewModel.updateInputA(it) },
                onLabelChange = { viewModel.updateLabelA(it) },
                onPickFile = { filePickerA.launch("text/*") },
                onLoadFixture = { name -> viewModel.loadFixture(context, name, "A") },
                fixtures = remember { viewModel.getAvailableFixtures(context) }
            )

            // Input B (if two-input mode)
            if (uiState.twoInputMode) {
                InputPanel(
                    label = uiState.inputLabelB,
                    text = uiState.inputTextB,
                    profile = uiState.profileB,
                    slot = "B",
                    onTextChange = { viewModel.updateInputB(it) },
                    onLabelChange = { viewModel.updateLabelB(it) },
                    onPickFile = { filePickerB.launch("text/*") },
                    onLoadFixture = { name -> viewModel.loadFixture(context, name, "B") },
                    fixtures = remember { viewModel.getAvailableFixtures(context) }
                )
            }

            // Config panel
            ConfigPanel(
                runMode = uiState.selectedRunMode,
                sensitivity = uiState.selectedSensitivity,
                maxCandidates = uiState.maxCandidates,
                searchDepth = uiState.searchDepth,
                onRunModeChange = { viewModel.setRunMode(it) },
                onSensitivityChange = { viewModel.setSensitivity(it) },
                onMaxCandidatesChange = { viewModel.setMaxCandidates(it) },
                onSearchDepthChange = { viewModel.setSearchDepth(it) }
            )

            // Run controls
            RunControls(
                status = uiState.runStatus,
                onStart = { viewModel.startRun() },
                onCancel = { viewModel.cancelRun() },
                onReset = { viewModel.resetRun() }
            )

            // Live progress panel
            uiState.progress?.let { progress ->
                LiveProgressPanel(progress = progress)
            }

            // Error display
            uiState.errorMessage?.let { error ->
                ErrorBanner(error) { viewModel.clearError() }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ColliderTopBar(onBack: () -> Unit, status: RunStatus) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CollideSurface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, "Back", tint = CollideOnSurface)
        }
        Text(
            "Code Collider",
            style = MaterialTheme.typography.titleLarge.copy(color = CollideAccent),
            modifier = Modifier.weight(1f)
        )
        StatusBadge(status)
    }
}

@Composable
private fun StatusBadge(status: RunStatus) {
    val (color, label) = when (status) {
        RunStatus.IDLE -> CollideDim to "IDLE"
        RunStatus.RUNNING -> CollideAmber to "RUNNING"
        RunStatus.COMPLETED -> CollideGreen to "DONE"
        RunStatus.CANCELLED -> CollideAmber to "CANCELLED"
        RunStatus.ERROR -> CollideRed to "ERROR"
    }
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(color = color))
    }
}

@Composable
private fun TwoInputToggle(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CollideCard, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (enabled) Icons.Default.CompareArrows else Icons.Default.Transform,
            null, tint = CollideAccent, modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (enabled) "Two-Input Recombination" else "Single-Input Mutation",
                style = MaterialTheme.typography.titleSmall.copy(color = CollideOnBackground)
            )
            Text(
                if (enabled) "Splice, compare, hybridize, detect contradictions"
                else "Mutate, simplify, reorder, mirror",
                style = MaterialTheme.typography.labelSmall.copy(color = CollideDim)
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CollideBackground,
                checkedTrackColor = CollideAccent,
                uncheckedThumbColor = CollideBackground,
                uncheckedTrackColor = CollideDim
            )
        )
    }
}

@Composable
private fun InputPanel(
    label: String,
    text: String,
    profile: InputProfile?,
    slot: String,
    onTextChange: (String) -> Unit,
    onLabelChange: (String) -> Unit,
    onPickFile: () -> Unit,
    onLoadFixture: (String) -> Unit,
    fixtures: List<String>
) {
    var showFixtures by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CollideCard),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, CollideAccent.copy(alpha = 0.2f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(CollideAccent.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(slot, style = MaterialTheme.typography.labelMedium.copy(color = CollideAccent))
                }
                Spacer(Modifier.width(10.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = onLabelChange,
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.labelMedium.copy(color = CollideOnBackground),
                    placeholder = { Text("Input label...", color = CollideDim, fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CollideAccent.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = CollideSurface,
                        unfocusedContainerColor = CollideSurface
                    ),
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onPickFile, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.FolderOpen, "Load file", tint = CollideAccent, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { showFixtures = !showFixtures }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Science, "Fixture", tint = CollideAccentAlt, modifier = Modifier.size(20.dp))
                }
            }

            // Fixture picker
            if (showFixtures && fixtures.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .background(CollideSurface, RoundedCornerShape(6.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Built-in fixtures:",
                        style = MaterialTheme.typography.labelSmall.copy(color = CollideDim)
                    )
                    fixtures.forEach { name ->
                        TextButton(
                            onClick = { onLoadFixture(name); showFixtures = false },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                name,
                                style = MaterialTheme.typography.labelSmall.copy(color = CollideAccentAlt),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Text input
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 200.dp),
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = CollideOnBackground,
                    fontSize = 12.sp
                ),
                placeholder = {
                    Text(
                        "Paste code here or load a fixture...",
                        color = CollideDim,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CollideAccent.copy(alpha = 0.5f),
                    unfocusedBorderColor = CollideDim.copy(alpha = 0.3f),
                    focusedContainerColor = CollideSurface,
                    unfocusedContainerColor = CollideSurface
                )
            )

            // Profile info
            if (profile != null) {
                InputProfileRow(profile = profile, textLength = text.length)
            } else if (text.isNotBlank()) {
                Text(
                    "Profiling...",
                    style = MaterialTheme.typography.labelSmall.copy(color = CollideDim)
                )
            }
        }
    }
}

@Composable
private fun InputProfileRow(profile: InputProfile, textLength: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CollideSurface, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProfileChip("~${profile.estimatedLanguage.displayName()}", CollideAccentAlt)
        ProfileChip("${profile.tokenCountEstimate}t", CollideDim)
        ProfileChip("${profile.lineCount}L", CollideDim)
        ProfileChip("${textLength}b", CollideDim)
        if (profile.keywordsDetected.isNotEmpty()) {
            ProfileChip(profile.keywordsDetected.take(2).joinToString(","), CollideDim.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun ProfileChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(color = color))
    }
}

@Composable
private fun ConfigPanel(
    runMode: RunMode,
    sensitivity: DetectorSensitivity,
    maxCandidates: Int,
    searchDepth: Int,
    onRunModeChange: (RunMode) -> Unit,
    onSensitivityChange: (DetectorSensitivity) -> Unit,
    onMaxCandidatesChange: (Int) -> Unit,
    onSearchDepthChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CollideCard)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "RUN CONFIGURATION",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = CollideDim, letterSpacing = 2.sp
                )
            )

            // Run mode
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Mode:",
                    style = MaterialTheme.typography.labelMedium.copy(color = CollideOnSurface),
                    modifier = Modifier.width(80.dp)
                )
                RunMode.entries.forEach { mode ->
                    ModeButton(
                        label = mode.label,
                        selected = runMode == mode,
                        color = when (mode) {
                            RunMode.SAFE -> CollideGreen
                            RunMode.BALANCED -> CollideAccent
                            RunMode.BURST -> CollideRed
                        },
                        onClick = { onRunModeChange(mode) }
                    )
                }
            }

            // Detector sensitivity
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Detect:",
                    style = MaterialTheme.typography.labelMedium.copy(color = CollideOnSurface),
                    modifier = Modifier.width(80.dp)
                )
                DetectorSensitivity.entries.forEach { s ->
                    ModeButton(
                        label = s.label,
                        selected = sensitivity == s,
                        color = CollideAccentAlt,
                        onClick = { onSensitivityChange(s) }
                    )
                }
            }

            // Candidate count and depth sliders
            ConfigSlider(
                label = "Max candidates: $maxCandidates",
                value = maxCandidates.toFloat(),
                valueRange = 5f..200f,
                steps = 38,
                onValueChange = { onMaxCandidatesChange(it.toInt()) }
            )
            ConfigSlider(
                label = "Search depth: $searchDepth",
                value = searchDepth.toFloat(),
                valueRange = 1f..6f,
                steps = 4,
                onValueChange = { onSearchDepthChange(it.toInt()) }
            )
        }
    }
}

@Composable
private fun ModeButton(label: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    val bg = if (selected) color.copy(alpha = 0.2f) else Color.Transparent
    val borderColor = if (selected) color else CollideDim.copy(alpha = 0.4f)
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(bg, RoundedCornerShape(6.dp))
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(
            color = if (selected) color else CollideDim
        ))
    }
}

@Composable
private fun ConfigSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(color = CollideOnSurface))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = CollideAccent,
                activeTrackColor = CollideAccent.copy(alpha = 0.6f),
                inactiveTrackColor = CollideDim.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun RunControls(
    status: RunStatus,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (status) {
            RunStatus.IDLE, RunStatus.COMPLETED, RunStatus.CANCELLED, RunStatus.ERROR -> {
                Button(
                    onClick = onStart,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CollideAccent,
                        contentColor = CollideBackground
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "START COLLISION",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                }
                if (status != RunStatus.IDLE) {
                    OutlinedButton(
                        onClick = onReset,
                        modifier = Modifier.height(52.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CollideDim)
                    ) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                    }
                }
            }
            RunStatus.RUNNING -> {
                Button(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CollideRed.copy(alpha = 0.8f),
                        contentColor = CollideOnBackground
                    )
                ) {
                    Icon(Icons.Default.Stop, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("CANCEL RUN",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
fun LiveProgressPanel(progress: RunProgress) {
    val stats = progress.stats

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CollideCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, CollideAmber.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Analytics, null,
                    tint = CollideAmber, modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "LIVE PROGRESS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = CollideAmber, letterSpacing = 2.sp
                    )
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${stats.elapsedMs}ms",
                    style = MaterialTheme.typography.labelSmall.copy(color = CollideDim)
                )
            }

            // Stats grid
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatCell("Seen", stats.candidatesSeen, CollideOnSurface)
                StatCell("Pruned", stats.candidatesPrunedPreEval, CollideDim)
                StatCell("Evaluated", stats.candidatesEvaluated, CollideAccent)
                StatCell("Events", stats.savedEventCount, CollideGreen)
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatCell("Parse fail", stats.parseFailures, CollideRed)
                StatCell("Invalid", stats.invalidStructureCount, CollideRed.copy(alpha = 0.7f))
                StatCell("Not interesting", stats.notInterestingCount, CollideDim)
                StatCell("Candidate#", progress.currentCandidateIndex, CollideOnSurface)
            }

            // Current recipe
            if (progress.currentRecipeName.isNotBlank()) {
                Text(
                    "Recipe: ${progress.currentRecipeName}",
                    style = MaterialTheme.typography.labelSmall.copy(color = CollideAccentAlt),
                )
            }

            // Last detector summary
            if (progress.lastDetectorSummary.isNotBlank()) {
                Text(
                    progress.lastDetectorSummary,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = CollideDim, fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "$value",
            style = MaterialTheme.typography.titleMedium.copy(color = color, fontWeight = FontWeight.Bold)
        )
        Text(label, style = MaterialTheme.typography.labelSmall.copy(color = CollideDim, fontSize = 10.sp))
    }
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = CollideRed.copy(alpha = 0.1f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, CollideRed.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, null, tint = CollideRed, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(message, style = MaterialTheme.typography.bodySmall.copy(color = CollideRed),
                modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, "Dismiss", tint = CollideRed, modifier = Modifier.size(16.dp))
            }
        }
    }
}

