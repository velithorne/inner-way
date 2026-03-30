package com.collide.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.collide.app.domain.model.BaselineStrategy
import com.collide.app.domain.model.RunMode
import com.collide.app.ui.collider.LabCard
import com.collide.app.ui.collider.formatBytes
import com.collide.app.ui.theme.CollideColors

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

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
                text = "SETTINGS",
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
            LabCard(title = "DEFAULT BASELINE") {
                BaselineStrategy.entries.forEach { strategy ->
                    SettingRadioRow(
                        label = strategy.displayName,
                        selected = settings.defaultBaseline == strategy,
                        onClick = { viewModel.setBaseline(strategy) }
                    )
                }
            }

            LabCard(title = "DEFAULT RUN MODE") {
                RunMode.entries.forEach { mode ->
                    SettingRadioRow(
                        label = "${mode.displayName} (max ${mode.maxCandidates} candidates, chain ≤${mode.maxChainLength})",
                        selected = settings.defaultRunMode == mode,
                        onClick = { viewModel.setRunMode(mode) }
                    )
                }
            }

            LabCard(title = "DEFAULT MAX CANDIDATES") {
                Text(
                    "Current: ${settings.defaultMaxCandidates}",
                    color = CollideColors.onSurface,
                    fontSize = 13.sp
                )
                Slider(
                    value = settings.defaultMaxCandidates.toFloat(),
                    onValueChange = { viewModel.setMaxCandidates(it.toInt()) },
                    valueRange = 10f..200f,
                    steps = 18,
                    colors = SliderDefaults.colors(
                        thumbColor = CollideColors.accent,
                        activeTrackColor = CollideColors.accent
                    )
                )
            }

            LabCard(title = "DEFAULT MAX CHAIN LENGTH") {
                Text(
                    "Current: ${settings.defaultMaxChainLength}",
                    color = CollideColors.onSurface,
                    fontSize = 13.sp
                )
                Slider(
                    value = settings.defaultMaxChainLength.toFloat(),
                    onValueChange = { viewModel.setMaxChainLength(it.toInt()) },
                    valueRange = 1f..3f,
                    steps = 1,
                    colors = SliderDefaults.colors(
                        thumbColor = CollideColors.accentAlt,
                        activeTrackColor = CollideColors.accentAlt
                    )
                )
            }

            LabCard(title = "MAX FILE SIZE") {
                val sizes = listOf(
                    512 * 1024L to "512 KB",
                    1 * 1024 * 1024L to "1 MB",
                    2 * 1024 * 1024L to "2 MB",
                    5 * 1024 * 1024L to "5 MB"
                )
                sizes.forEach { (bytes, label) ->
                    SettingRadioRow(
                        label = label,
                        selected = settings.maxFileSizeBytes == bytes,
                        onClick = { viewModel.setMaxFileSize(bytes) }
                    )
                }
                Text(
                    "Current: ${formatBytes(settings.maxFileSizeBytes)}",
                    color = CollideColors.muted,
                    fontSize = 11.sp
                )
            }

            LabCard(title = "DIAGNOSTICS") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Save Near-Miss Diagnostics",
                            color = CollideColors.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Keep non-winning but close-call results",
                            color = CollideColors.muted,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = settings.saveNearMiss,
                        onCheckedChange = { viewModel.setSaveNearMiss(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CollideColors.accent,
                            checkedTrackColor = CollideColors.accent.copy(alpha = 0.4f)
                        )
                    )
                }
            }

            LabCard(title = "ABOUT") {
                SettingInfoRow("App", "COLLIDE")
                SettingInfoRow("Phase", "1 — Compression Collider")
                SettingInfoRow("Version", "1.0.0")
                SettingInfoRow("Min SDK", "26 (Android 8.0)")
                SettingInfoRow("Architecture", "MVVM + Clean Domain")
                SettingInfoRow("Persistence", "Room + DataStore")
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingRadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .then(if (selected) Modifier.background(CollideColors.accent.copy(alpha = 0.08f)) else Modifier)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = CollideColors.accent,
                unselectedColor = CollideColors.muted
            )
        )
        Spacer(Modifier.width(8.dp))
        Text(label, color = CollideColors.onSurface, fontSize = 13.sp)
    }
}

@Composable
private fun SettingInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = CollideColors.muted, fontSize = 12.sp)
        Text(value, color = CollideColors.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
