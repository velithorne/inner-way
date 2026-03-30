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
import com.collide.app.domain.model.RunConfigSnapshot
import com.collide.app.domain.transforms.TransformRegistry
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = CollideColors.accent)
            }
            Text("SETTINGS", color = CollideColors.accent, fontSize = 14.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Baseline ──────────────────────────────────────────────────
            LabCard(title = "DEFAULT BASELINE") {
                BaselineStrategy.entries.forEach { s ->
                    SettingRadioRow(s.displayName, settings.defaultBaseline == s) { viewModel.setBaseline(s) }
                }
            }

            // ── Run Mode ──────────────────────────────────────────────────
            LabCard(title = "DEFAULT RUN MODE") {
                RunMode.entries.forEach { m ->
                    SettingRadioRow(
                        "${m.displayName} (≤${m.maxCandidates} candidates, chain ≤${m.maxChainLength})",
                        settings.defaultRunMode == m
                    ) { viewModel.setRunMode(m) }
                }
            }

            // ── Max candidates ────────────────────────────────────────────
            LabCard(title = "DEFAULT MAX CANDIDATES") {
                Text("Current: ${settings.defaultMaxCandidates}", color = CollideColors.onSurface, fontSize = 13.sp)
                Slider(
                    value = settings.defaultMaxCandidates.toFloat(),
                    onValueChange = { viewModel.setMaxCandidates(it.toInt()) },
                    valueRange = 10f..200f, steps = 18,
                    colors = SliderDefaults.colors(thumbColor = CollideColors.accent, activeTrackColor = CollideColors.accent)
                )
            }

            // ── Chain length / chain 4 ────────────────────────────────────
            LabCard(title = "SEARCH DEPTH") {
                Text("Default Max Chain Length: ${settings.defaultMaxChainLength}", color = CollideColors.onSurface, fontSize = 13.sp)
                Slider(
                    value = settings.defaultMaxChainLength.toFloat(),
                    onValueChange = { viewModel.setMaxChainLength(it.toInt()) },
                    valueRange = 1f..if (settings.allowChainLength4) 4f else 3f,
                    steps = if (settings.allowChainLength4) 2 else 1,
                    colors = SliderDefaults.colors(thumbColor = CollideColors.accentAlt, activeTrackColor = CollideColors.accentAlt)
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Enable Chain Length 4", color = CollideColors.onSurface, fontSize = 13.sp,
                            fontWeight = FontWeight.Medium)
                        Text("Significantly larger search space. Use Burst mode + low max candidates.",
                            color = CollideColors.muted, fontSize = 11.sp)
                    }
                    Switch(
                        checked = settings.allowChainLength4,
                        onCheckedChange = { viewModel.setAllowChainLength4(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CollideColors.accentAlt,
                            checkedTrackColor = CollideColors.accentAlt.copy(0.4f)
                        )
                    )
                }
            }

            // ── Transform toggles ─────────────────────────────────────────
            LabCard(title = "ENABLED TRANSFORMS") {
                Text(
                    if (settings.enabledTransformIds.isEmpty()) "All transforms enabled (default)"
                    else "${settings.enabledTransformIds.size} of ${TransformRegistry.allExcludingIdentity.size} enabled",
                    color = CollideColors.muted, fontSize = 11.sp
                )
                Spacer(Modifier.height(8.dp))

                // "All" toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (settings.enabledTransformIds.isEmpty()) CollideColors.accent.copy(0.1f) else CollideColors.surfaceVariant)
                        .clickable {
                            viewModel.setEnabledTransforms(emptySet())
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = settings.enabledTransformIds.isEmpty(),
                        onClick = { viewModel.setEnabledTransforms(emptySet()) },
                        colors = RadioButtonDefaults.colors(selectedColor = CollideColors.accent, unselectedColor = CollideColors.muted)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("All transforms", color = CollideColors.onSurface, fontSize = 13.sp)
                }

                Spacer(Modifier.height(4.dp))

                TransformRegistry.allExcludingIdentity.forEach { transform ->
                    val id = transform.spec.id
                    val isEnabled = settings.enabledTransformIds.isEmpty() || id in settings.enabledTransformIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isEnabled && settings.enabledTransformIds.isNotEmpty()) CollideColors.accent.copy(0.08f) else CollideColors.surfaceVariant)
                            .clickable {
                                val current = if (settings.enabledTransformIds.isEmpty())
                                    TransformRegistry.allExcludingIdentity.map { it.spec.id }.toSet()
                                else
                                    settings.enabledTransformIds.toMutableSet()
                                val updated = if (id in current) current - id else current + id
                                viewModel.setEnabledTransforms(if (updated.size == TransformRegistry.allExcludingIdentity.size) emptySet() else updated)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isEnabled,
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(
                                checkedColor = CollideColors.accent,
                                uncheckedColor = CollideColors.muted
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(transform.spec.displayName, color = CollideColors.onSurface, fontSize = 13.sp)
                            Text(id, color = CollideColors.muted, fontSize = 10.sp)
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                }
            }

            // ── Max file size ─────────────────────────────────────────────
            LabCard(title = "MAX FILE SIZE") {
                listOf(512 * 1024L to "512 KB", 1L * 1024 * 1024 to "1 MB",
                       2L * 1024 * 1024 to "2 MB", 5L * 1024 * 1024 to "5 MB").forEach { (bytes, label) ->
                    SettingRadioRow(label, settings.maxFileSizeBytes == bytes) { viewModel.setMaxFileSize(bytes) }
                }
                Text("Current: ${formatBytes(settings.maxFileSizeBytes)}", color = CollideColors.muted, fontSize = 11.sp)
            }

            // ── Diagnostics ───────────────────────────────────────────────
            LabCard(title = "DIAGNOSTICS") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Save Near-Miss Diagnostics", color = CollideColors.onSurface, fontSize = 13.sp,
                            fontWeight = FontWeight.Medium)
                        Text("Keep non-winning but close-call results", color = CollideColors.muted, fontSize = 11.sp)
                    }
                    Switch(
                        checked = settings.saveNearMiss,
                        onCheckedChange = { viewModel.setSaveNearMiss(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CollideColors.accent,
                            checkedTrackColor = CollideColors.accent.copy(0.4f)
                        )
                    )
                }
            }

            // ── About ─────────────────────────────────────────────────────
            LabCard(title = "ABOUT") {
                SettingInfoRow("App", "COLLIDE")
                SettingInfoRow("Phase", "2 — Honest Compression Research Upgrade")
                SettingInfoRow("Version", "2.0.0-phase2")
                SettingInfoRow("Engine", RunConfigSnapshot.ENGINE_VERSION)
                SettingInfoRow("Min SDK", "26 (Android 8.0)")
                SettingInfoRow("Architecture", "MVVM + Clean Domain")
                SettingInfoRow("Verification", "Byte equality + SHA-256")
                SettingInfoRow("Winner Criteria", "Strict size + verified exact reconstruction")
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
            .then(if (selected) Modifier.background(CollideColors.accent.copy(0.08f)) else Modifier)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected, onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = CollideColors.accent, unselectedColor = CollideColors.muted)
        )
        Spacer(Modifier.width(8.dp))
        Text(label, color = CollideColors.onSurface, fontSize = 13.sp)
    }
}

@Composable
private fun SettingInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = CollideColors.muted, fontSize = 12.sp)
        Text(value, color = CollideColors.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
