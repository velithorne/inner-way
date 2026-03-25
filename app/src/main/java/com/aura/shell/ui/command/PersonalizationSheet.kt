package com.aura.shell.ui.command

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aura.shell.model.LauncherAppInfo
import com.aura.shell.personalization.LearnedPreferenceRow
import com.aura.shell.personalization.PersonalAliasEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalizationSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    aliases: List<PersonalAliasEntry>,
    learned: List<LearnedPreferenceRow>,
    installedApps: List<LauncherAppInfo>,
    onAddAlias: (alias: String, packageName: String) -> Unit,
    onRemoveAlias: (alias: String) -> Unit,
    onClearLearned: () -> Unit,
    onClearAll: () -> Unit,
) {
    if (!visible) return

    var newAlias by remember { mutableStateOf("") }
    var pickedPackage by remember { mutableStateOf<String?>(null) }
    var showPicker by remember { mutableStateOf(false) }
    var confirmClearAll by remember { mutableStateOf(false) }

    fun labelForPkg(pkg: String): String =
        installedApps.find { it.packageName == pkg }?.label ?: pkg

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Text(
                text = "Personalization",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "Aliases and learned choices stay on this device only. You can reset anytime.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
            )

            Text("Your aliases", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = newAlias,
                onValueChange = { newAlias = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Shorthand (e.g. vids)") },
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = { showPicker = true },
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(pickedPackage?.let { "Target: ${labelForPkg(it)}" } ?: "Pick app for alias…")
            }
            TextButton(
                onClick = {
                    val pkg = pickedPackage ?: return@TextButton
                    val al = newAlias.trim()
                    if (al.isNotEmpty()) {
                        onAddAlias(al, pkg)
                        newAlias = ""
                        pickedPackage = null
                    }
                },
                enabled = newAlias.isNotBlank() && pickedPackage != null,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("Save alias")
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 160.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(aliases, key = { it.alias }) { a ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(a.alias, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                labelForPkg(a.packageName),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = { onRemoveAlias(a.alias) }) {
                            Text("Remove")
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text("Learned preferences", style = MaterialTheme.typography.labelLarge)
            Text(
                "When you pick an app from suggestions, Aura remembers for similar commands.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(learned) { row ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        ),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = "\"${row.queryKey}\" → ${labelForPkg(row.packageName)} · ×${row.weight}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(onClick = onClearLearned) {
                    Text("Reset learned")
                }
                TextButton(onClick = { confirmClearAll = true }) {
                    Text("Reset all")
                }
            }
        }
    }

    if (showPicker) {
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("Choose app") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(installedApps, key = { it.packageName }) { app ->
                        Text(
                            text = app.label,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    pickedPackage = app.packageName
                                    showPicker = false
                                }
                                .padding(vertical = 10.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            },
        )
    }

    if (confirmClearAll) {
        AlertDialog(
            onDismissRequest = { confirmClearAll = false },
            title = { Text("Reset all personalization?") },
            text = { Text("Removes learned preferences and all your custom aliases on this device.") },
            confirmButton = {
                TextButton(onClick = {
                    onClearAll()
                    confirmClearAll = false
                }) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearAll = false }) { Text("Cancel") }
            },
        )
    }
}
