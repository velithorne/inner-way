package com.aura.shell.knowledge

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    existingId: String?,
    initialTitle: String,
    initialBody: String,
    isAuraNote: Boolean,
    onSave: (title: String, body: String) -> Unit,
    onCancel: () -> Unit,
) {
    val titleState = remember { mutableStateOf(initialTitle) }
    val bodyState = remember { mutableStateOf(initialBody) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existingId == null) "New note" else "Edit note") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (isAuraNote) {
                                onSave(titleState.value, bodyState.value)
                            }
                        },
                        enabled = isAuraNote,
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = "Save")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Text(
                text = "Notes stay on this phone.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = titleState.value,
                onValueChange = { titleState.value = it },
                label = { Text("Title") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                singleLine = true,
            )
            OutlinedTextField(
                value = bodyState.value,
                onValueChange = { bodyState.value = it },
                label = { Text("Body") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
                    .padding(top = 12.dp),
                minLines = 10,
            )
        }
    }
}
