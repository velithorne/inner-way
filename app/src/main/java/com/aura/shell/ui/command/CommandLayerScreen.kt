package com.aura.shell.ui.command

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aura.shell.knowledge.KnowledgeListItem
import com.aura.shell.command.CommandHistoryEntry
import com.aura.shell.command.CommandInputSource
import com.aura.shell.command.CommandLayerUiState
import com.aura.shell.command.CommandSurfaceState
import com.aura.shell.command.SuggestionKind
import com.aura.shell.personalization.LearningSignal
import com.aura.shell.voice.HandsFreeUiState
import com.aura.shell.voice.VoiceSurfaceState
import com.aura.shell.model.LauncherAppInfo
import com.aura.shell.model.RecentAppEntry
import com.aura.shell.ui.components.DrawableImage
import com.aura.shell.ui.theme.AuraBackground
import com.aura.shell.ui.theme.AuraSurfaceElevated

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandLayerScreen(
    state: CommandLayerUiState,
    focusRequester: FocusRequester,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onHistoryPick: (CommandHistoryEntry) -> Unit,
    onAppPick: (String, String?, LearningSignal?) -> Unit,
    onRecentPick: (String) -> Unit,
    onMicClick: () -> Unit,
    onVoiceCancel: () -> Unit,
    onPermissionRetry: () -> Unit,
    onPassiveHandsFreeChange: (Boolean) -> Unit,
    onOpenPersonalization: () -> Unit,
    onDismissPersonalization: () -> Unit,
    onAddPersonalAlias: (String, String) -> Unit,
    onRemovePersonalAlias: (String) -> Unit,
    onClearLearnedOnly: () -> Unit,
    onClearAllPersonalization: () -> Unit,
    onKnowledgeItemPick: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        AuraBackground,
                        AuraSurfaceElevated.copy(alpha = 0.4f),
                        AuraBackground,
                    ),
                ),
            ),
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Aura Command") },
                    navigationIcon = {
                        TextButton(onClick = onBack) {
                            Text("Close")
                        }
                    },
                    actions = {
                        TextButton(onClick = onOpenPersonalization) {
                            Text("Personalize")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    ),
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp),
            ) {
                VoiceStatusBanner(
                    voice = state.voice,
                    onCancel = onVoiceCancel,
                    onPermissionRetry = onPermissionRetry,
                )

                CommandPassiveStrip(
                    enabled = state.passiveHandsFreeEnabled,
                    onEnabledChange = onPassiveHandsFreeChange,
                    handsFree = state.handsFree,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    OutlinedTextField(
                        value = state.inputText,
                        onValueChange = onInputChange,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        enabled = !state.isLoading && state.voice !is VoiceSurfaceState.Listening,
                        placeholder = { Text("What do you want to do?") },
                        label = { Text("Command") },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = { onSubmit() },
                        ),
                        singleLine = false,
                        maxLines = 3,
                    )
                    IconButton(
                        onClick = onMicClick,
                        enabled = !state.isLoading && state.voice !is VoiceSurfaceState.Processing,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MicNone,
                            contentDescription = "Voice input",
                            tint = when (state.voice) {
                                is VoiceSurfaceState.Listening -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (state.history.isNotEmpty()) {
                    Text(
                        text = "Recent commands",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 4.dp),
                    ) {
                        items(state.history.size) { i ->
                            val entry = state.history[i]
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .clickable { onHistoryPick(entry) },
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            ) {
                                Text(
                                    text = if (entry.source == CommandInputSource.Typed) {
                                        entry.original
                                    } else {
                                        "· ${entry.original}"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Text(
                    text = "Result",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))

                when {
                    state.isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                    }
                    else -> {
                        ResultPanel(
                            surface = state.surface,
                            onAppPick = onAppPick,
                            onRecentPick = onRecentPick,
                            onKnowledgeItemPick = onKnowledgeItemPick,
                        )
                    }
                }
            }
        }

        PersonalizationSheet(
            visible = state.showPersonalizationSheet,
            onDismiss = onDismissPersonalization,
            aliases = state.personalAliases,
            learned = state.learnedPreferences,
            installedApps = state.installedApps,
            onAddAlias = onAddPersonalAlias,
            onRemoveAlias = onRemovePersonalAlias,
            onClearLearned = onClearLearnedOnly,
            onClearAll = onClearAllPersonalization,
        )
    }
}

@Composable
private fun CommandPassiveStrip(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    handsFree: HandsFreeUiState,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Hands-free while this screen is open",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = enabled, onCheckedChange = onEnabledChange)
            }
            val line = when (handsFree) {
                HandsFreeUiState.Armed -> "Say “Aura” or “Aura, …”"
                HandsFreeUiState.ListeningForWake -> "Listening for “Aura”…"
                HandsFreeUiState.ListeningForCommand -> "Say your command…"
                HandsFreeUiState.Processing -> "Working…"
                is HandsFreeUiState.Success -> handsFree.message
                else -> null
            }
            line?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@Composable
private fun VoiceStatusBanner(
    voice: VoiceSurfaceState,
    onCancel: () -> Unit,
    onPermissionRetry: () -> Unit,
) {
    when (voice) {
        VoiceSurfaceState.Idle -> { }
        VoiceSurfaceState.Listening -> {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "Listening…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    TextButton(onClick = onCancel) {
                        Text("Cancel")
                    }
                }
            }
        }
        VoiceSurfaceState.Processing -> {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                    Text(
                        text = "Running command…",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        is VoiceSurfaceState.Error -> {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
            ) {
                Text(
                    text = voice.userMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
        VoiceSurfaceState.PermissionNeeded -> {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Microphone access lets Aura hear your commands. You can still type.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onPermissionRetry) {
                        Text("Allow microphone")
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultPanel(
    surface: CommandSurfaceState,
    onAppPick: (String, String?, LearningSignal?) -> Unit,
    onRecentPick: (String) -> Unit,
    onKnowledgeItemPick: (String) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
        tonalElevation = 1.dp,
    ) {
        when (surface) {
            is CommandSurfaceState.Empty -> {
                Text(
                    text = "Run a command above. Try help",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
            is CommandSurfaceState.Success -> {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = surface.message,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    surface.hint?.let { hint ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            is CommandSurfaceState.Unknown -> {
                Text(
                    text = surface.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
            is CommandSurfaceState.Help -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(surface.lines.size) { i ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                text = "· ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = surface.lines[i],
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
            is CommandSurfaceState.Suggestions -> {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = surface.title,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    surface.subtitle?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (surface.kind == SuggestionKind.DID_YOU_MEAN) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Pick one to confirm",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val learnKey = surface.resolutionTargetKey
                    surface.apps.forEach { app ->
                        AppResultRow(
                            app = app,
                            onClick = {
                                onAppPick(
                                    app.packageName,
                                    learnKey,
                                    if (learnKey != null) LearningSignal.SUGGESTION_PICK else null,
                                )
                            },
                        )
                    }
                }
            }
            is CommandSurfaceState.SearchResults -> {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Matches for \"${surface.query}\"",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    if (surface.apps.isEmpty()) {
                        Text(
                            text = "No apps matched.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        val learnKey = surface.resolutionTargetKey
                        surface.apps.forEach { app ->
                            AppResultRow(
                                app = app,
                                onClick = {
                                    onAppPick(
                                        app.packageName,
                                        learnKey,
                                        if (learnKey != null) LearningSignal.SUGGESTION_PICK else null,
                                    )
                                },
                            )
                        }
                    }
                }
            }
            is CommandSurfaceState.KnowledgeResults -> {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = surface.title,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    surface.subtitle?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (surface.items.isEmpty()) {
                        Text(
                            text = "Nothing to show.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        surface.items.forEach { item ->
                            KnowledgeResultRow(item = item, onClick = { onKnowledgeItemPick(item.id) })
                        }
                    }
                }
            }
            is CommandSurfaceState.RecentsList -> {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = surface.title,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    if (surface.entries.isEmpty()) {
                        Text(
                            text = "No recent launches yet. Open apps from Aura first.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        surface.entries.forEach { e ->
                            RecentResultRow(entry = e, onClick = { onRecentPick(e.packageName) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppResultRow(
    app: LauncherAppInfo,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DrawableImage(
            drawable = app.icon,
            contentDescription = app.label,
            modifier = Modifier.size(36.dp),
        )
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun KnowledgeResultRow(
    item: KnowledgeListItem,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
    ) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = item.snippet,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = item.sourceType.name.replace('_', ' '),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun RecentResultRow(
    entry: RecentAppEntry,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DrawableImage(
            drawable = entry.icon,
            contentDescription = entry.label,
            modifier = Modifier.size(36.dp),
        )
        Text(
            text = entry.label,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
