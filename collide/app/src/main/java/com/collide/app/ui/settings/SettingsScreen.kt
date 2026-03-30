package com.collide.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.collide.app.domain.model.DetectorSensitivity
import com.collide.app.domain.model.InputSample
import com.collide.app.domain.model.RunMode
import com.collide.app.ui.theme.*

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val config by viewModel.config.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CollideBackground)
    ) {
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
                "Settings",
                style = MaterialTheme.typography.titleLarge.copy(color = CollideOnBackground),
                modifier = Modifier.weight(1f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Run mode
            SettingSection("DEFAULT RUN MODE") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RunMode.entries.forEach { mode ->
                        FilterChip(
                            selected = config.runMode == mode,
                            onClick = { viewModel.setRunMode(mode) },
                            label = { Text(mode.label, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CollideAccent.copy(alpha = 0.2f),
                                selectedLabelColor = CollideAccent
                            )
                        )
                    }
                }
                Text(
                    "Safe: ${RunMode.SAFE.maxCandidates} candidates, depth ${RunMode.SAFE.searchDepth} | " +
                    "Balanced: ${RunMode.BALANCED.maxCandidates} | " +
                    "Burst: ${RunMode.BURST.maxCandidates}",
                    style = MaterialTheme.typography.labelSmall.copy(color = CollideDim)
                )
            }

            // Max candidates
            SettingSection("DEFAULT MAX CANDIDATES") {
                Text(
                    "${config.maxCandidates}",
                    style = MaterialTheme.typography.headlineSmall.copy(color = CollideAccent)
                )
                Slider(
                    value = config.maxCandidates.toFloat(),
                    onValueChange = { viewModel.setMaxCandidates(it.toInt()) },
                    valueRange = 5f..200f,
                    steps = 38,
                    colors = SliderDefaults.colors(thumbColor = CollideAccent, activeTrackColor = CollideAccent.copy(alpha = 0.5f))
                )
            }

            // Search depth
            SettingSection("DEFAULT SEARCH DEPTH") {
                Text(
                    "${config.searchDepth}",
                    style = MaterialTheme.typography.headlineSmall.copy(color = CollideAccent)
                )
                Slider(
                    value = config.searchDepth.toFloat(),
                    onValueChange = { viewModel.setSearchDepth(it.toInt()) },
                    valueRange = 1f..6f,
                    steps = 4,
                    colors = SliderDefaults.colors(thumbColor = CollideAccent, activeTrackColor = CollideAccent.copy(alpha = 0.5f))
                )
            }

            // Detector sensitivity
            SettingSection("DETECTOR SENSITIVITY") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetectorSensitivity.entries.forEach { s ->
                        FilterChip(
                            selected = config.detectorSensitivity == s,
                            onClick = { viewModel.setDetectorSensitivity(s) },
                            label = { Text(s.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CollideAccentAlt.copy(alpha = 0.2f),
                                selectedLabelColor = CollideAccentAlt
                            )
                        )
                    }
                }
                Text(
                    "High sensitivity = lower thresholds = more events saved",
                    style = MaterialTheme.typography.labelSmall.copy(color = CollideDim)
                )
            }

            // Max input size
            SettingSection("MAX INPUT SIZE") {
                Text(
                    "${config.maxInputSizeBytes / 1000}KB",
                    style = MaterialTheme.typography.headlineSmall.copy(color = CollideAccent)
                )
                Slider(
                    value = config.maxInputSizeBytes.toFloat(),
                    onValueChange = { viewModel.setMaxInputSize(it.toInt()) },
                    valueRange = 5000f..InputSample.MAX_INPUT_SIZE_BYTES.toFloat(),
                    steps = 9,
                    colors = SliderDefaults.colors(thumbColor = CollideAccent, activeTrackColor = CollideAccent.copy(alpha = 0.5f))
                )
            }

            // Toggles
            SettingSection("OPTIONS") {
                SettingToggle(
                    label = "Save near-miss diagnostics",
                    description = "Save candidates that were close but didn't pass thresholds",
                    checked = config.saveNearMisses,
                    onCheckedChange = { viewModel.setSaveNearMisses(it) }
                )
                SettingToggle(
                    label = "Two-input mode by default",
                    description = "Start the collider in dual-input mode",
                    checked = config.twoInputMode,
                    onCheckedChange = { viewModel.setTwoInputDefault(it) }
                )
            }

            // Phase info
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = CollideCard)
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = CollideDim, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "COLLIDE Phase 1 — Code Collider",
                            style = MaterialTheme.typography.titleSmall.copy(color = CollideOnBackground)
                        )
                        Text(
                            "Bounded structural discovery prototype. No semantic correctness claims.",
                            style = MaterialTheme.typography.labelSmall.copy(color = CollideDim)
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CollideCard)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = CollideDim, letterSpacing = 2.sp
                )
            )
            content()
        }
    }
}

@Composable
private fun SettingToggle(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium.copy(color = CollideOnSurface))
            Text(description, style = MaterialTheme.typography.labelSmall.copy(color = CollideDim))
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = CollideAccent,
                checkedThumbColor = CollideBackground
            )
        )
    }
}
