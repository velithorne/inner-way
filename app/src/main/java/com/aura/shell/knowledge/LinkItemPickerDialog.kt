package com.aura.shell.knowledge

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun LinkItemPickerDialog(
    excludeItemId: String,
    loadItems: suspend (String) -> List<KnowledgeListItem>,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    val query = remember { mutableStateOf("") }
    val items = remember { mutableStateOf<List<KnowledgeListItem>>(emptyList()) }

    LaunchedEffect(query.value) {
        items.value = loadItems(query.value)
    }
    LaunchedEffect(Unit) {
        if (items.value.isEmpty()) {
            items.value = loadItems("")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Link to another item") },
        text = {
            Column {
                OutlinedTextField(
                    value = query.value,
                    onValueChange = { query.value = it },
                    label = { Text("Search titles") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                    items(items.value, key = { it.id }) { item ->
                        Text(
                            text = item.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onPick(item.id)
                                    onDismiss()
                                }
                                .padding(vertical = 10.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
