package com.aura.shell.knowledge

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aura.shell.ui.theme.AuraSurfaceElevated
import java.text.DateFormat
import java.util.Date
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun KnowledgeDetailScreen(
    detail: KnowledgeDetailUi,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onCopy: (String) -> Unit,
    onOpenUri: (Uri) -> Unit,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    onApplySuggestion: (String) -> Unit,
    onRelatedClick: (String) -> Unit,
    onUnlinkManual: (String) -> Unit,
    onOpenLinkPicker: () -> Unit,
) {
    val entity = detail.entity
    var showDelete by rememberSaveable { mutableStateOf(false) }
    val meta = buildString {
        append(entity.sourceType.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() })
        entity.mimeType?.let { append(" · ").append(it) }
        entity.fileName?.let { append(" · ").append(it) }
    }
    val updated = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(entity.updatedAt))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(entity.title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenLinkPicker) {
                        Icon(Icons.Filled.Link, contentDescription = "Link item")
                    }
                    if (entity.isAuraAuthored) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit")
                        }
                    }
                    IconButton(onClick = { onCopy(entity.fullText.ifBlank { entity.snippetPreview }) }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy")
                    }
                    run {
                        val u = entity.sourceUri?.let { Uri.parse(it) }
                        if (u != null && u.scheme != null) {
                            IconButton(onClick = { onOpenUri(u) }) {
                                Icon(Icons.Filled.OpenInBrowser, contentDescription = "Open")
                            }
                        }
                    }
                    IconButton(onClick = { showDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(text = meta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "Updated $updated", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = "Tags you add stay on this device. Suggestions are hints only until you tap them.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 12.dp),
            ) {
                detail.tags.forEach { t ->
                    AssistChip(
                        onClick = { onRemoveTag(t) },
                        label = { Text(t) },
                    )
                }
            }
            if (detail.tagSuggestions.isNotEmpty()) {
                Text(
                    text = "Suggested",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 12.dp),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 6.dp),
                ) {
                    detail.tagSuggestions.forEach { s ->
                        SuggestionChip(
                            onClick = { onApplySuggestion(s) },
                            label = { Text(s) },
                        )
                    }
                }
            }
            var newTag by remember { mutableStateOf("") }
            OutlinedTextField(
                value = newTag,
                onValueChange = { newTag = it },
                label = { Text("Add tag") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                singleLine = true,
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = {
                        val t = newTag.trim()
                        if (t.isNotEmpty()) {
                            onAddTag(t)
                            newTag = ""
                        }
                    },
                    enabled = newTag.trim().isNotEmpty(),
                ) { Text("Add") }
            }

            if (entity.extractionStatus == KnowledgeExtractionStatus.METADATA_ONLY.name) {
                Text(
                    text = "Preview not available for this file type yet. Open the original or delete if not needed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            Text(
                text = entity.fullText.ifBlank { entity.snippetPreview },
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 16.dp),
            )

            Text(
                text = "Related",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 22.dp),
            )
            Text(
                text = "Manual links are definite. Others are best guesses from tags, words, or recent viewing.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            detail.related.forEach { r ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { onRelatedClick(r.id) },
                    colors = CardDefaults.cardColors(containerColor = AuraSurfaceElevated),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = r.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (r.strength == RelatedStrength.MANUAL_LINK) {
                                TextButton(onClick = { onUnlinkManual(r.id) }) {
                                    Text("Unlink")
                                }
                            }
                        }
                        Text(
                            text = r.reason,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                        )
                        Text(
                            text = r.snippet,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Remove from Knowledge?") },
            text = { Text("This deletes the saved copy on this device. It does not remove files elsewhere.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDelete = false
                        onDelete()
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("Cancel") }
            },
        )
    }
}
