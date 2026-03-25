package com.aura.shell.knowledge

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aura.shell.knowledge.db.KnowledgeItemEntity
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeDetailScreen(
    entity: KnowledgeItemEntity,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onCopy: (String) -> Unit,
    onOpenUri: (Uri) -> Unit,
) {
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
