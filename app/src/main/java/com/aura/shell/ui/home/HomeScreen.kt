package com.aura.shell.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aura.shell.command.DrawerRequest
import com.aura.shell.knowledge.KnowledgeListItem
import com.aura.shell.voice.HandsFreeUiState
import com.aura.shell.model.LauncherAppInfo
import com.aura.shell.model.RecentAppEntry
import com.aura.shell.ui.components.DrawableImage
import com.aura.shell.ui.theme.AuraBackground
import com.aura.shell.ui.theme.AuraSurfaceElevated
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val RECENT_ON_HOME_MAX = 4

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    passiveHandsFreeEnabled: Boolean,
    onPassiveHandsFreeChange: (Boolean) -> Unit,
    handsFree: HandsFreeUiState,
    onAppClick: (String) -> Unit,
    onCommandBarClick: () -> Unit,
    onMicClick: () -> Unit,
    onPinnedClick: (PinnedCardUi) -> Unit,
    snackbarHostState: SnackbarHostState,
    drawerRequest: DrawerRequest? = null,
    onDrawerRequestConsumed: () -> Unit = {},
    knowledgePreview: KnowledgeListItem? = null,
    onOpenKnowledge: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val pinned = remember { defaultPinnedCards() }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var showRecentOverflow by remember { mutableStateOf(false) }

    val sheetState = rememberStandardBottomSheetState(
        skipHiddenState = false,
    )
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = sheetState,
    )

    LaunchedEffect(drawerRequest?.nonce) {
        val req = drawerRequest ?: return@LaunchedEffect
        if (req.expand) {
            sheetState.expand()
        } else {
            sheetState.partialExpand()
        }
        onDrawerRequestConsumed()
    }

    LaunchedEffect(sheetState.currentValue) {
        AppDrawerSessionState.expanded = sheetState.currentValue == SheetValue.Expanded
    }

    val peekAlpha by animateFloatAsState(
        targetValue = if (sheetState.currentValue == SheetValue.Expanded) 0.92f else 1f,
        animationSpec = tween(220),
        label = "contentDim",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        AuraBackground,
                        AuraSurfaceElevated.copy(alpha = 0.28f),
                        AuraBackground,
                    ),
                ),
            ),
    ) {
        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetPeekHeight = 72.dp,
            sheetDragHandle = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BottomSheetDefaults.DragHandle()
                    Text(
                        text = "Installed apps",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            },
            sheetContent = {
                AppDrawerContent(
                    state = state,
                    onAppClick = { pkg ->
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAppClick(pkg)
                        scope.launch { sheetState.partialExpand() }
                    },
                    modifier = Modifier.navigationBarsPadding(),
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .statusBarsPadding()
                    .graphicsLayer { alpha = peekAlpha }
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 24.dp),
            ) {
                HomeHeader()

                Spacer(modifier = Modifier.height(12.dp))
                PassiveHandsFreeStrip(
                    enabled = passiveHandsFreeEnabled,
                    onEnabledChange = onPassiveHandsFreeChange,
                    handsFree = handsFree,
                )

                Spacer(modifier = Modifier.height(16.dp))

                CommandBarBlock(
                    onCommandAreaClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCommandBarClick()
                    },
                    onMicClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onMicClick()
                    },
                )

                Spacer(modifier = Modifier.height(14.dp))
                KnowledgeHomeCard(
                    preview = knowledgePreview,
                    onOpenKnowledge = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOpenKnowledge()
                    },
                )

                Spacer(modifier = Modifier.height(22.dp))

                SectionLabel("Modules")
                Spacer(modifier = Modifier.height(8.dp))
                PinnedRow(cards = pinned, onCardClick = onPinnedClick)

                Spacer(modifier = Modifier.height(20.dp))

                SectionLabel("Recent")
                Spacer(modifier = Modifier.height(8.dp))
                CompactRecentRow(
                    recent = state.recentApps,
                    onAppClick = onAppClick,
                    onSeeMore = {
                        showRecentOverflow = true
                    },
                )
            }
        }

        if (showRecentOverflow) {
            RecentOverflowDialog(
                entries = state.recentApps.drop(RECENT_ON_HOME_MAX),
                onDismiss = { showRecentOverflow = false },
                onAppClick = { pkg ->
                    showRecentOverflow = false
                    onAppClick(pkg)
                },
            )
        }
    }
}

@Composable
private fun PassiveHandsFreeStrip(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    handsFree: HandsFreeUiState,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hands-free “Aura”",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        text = "Only while this screen is open. Not always-on.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                )
            }
            val status = when (handsFree) {
                HandsFreeUiState.Disabled,
                is HandsFreeUiState.Error,
                -> null
                HandsFreeUiState.Armed -> "Aura is ready — say “Aura” or “Aura, …”"
                HandsFreeUiState.ListeningForWake -> "Listening for “Aura”…"
                HandsFreeUiState.WakeDetected -> "Wake heard"
                HandsFreeUiState.ListeningForCommand -> "Say your command…"
                HandsFreeUiState.Processing -> "Working…"
                is HandsFreeUiState.Success -> handsFree.message
                HandsFreeUiState.Timeout -> "Listening paused"
                HandsFreeUiState.PermissionNeeded -> "Allow microphone in system settings"
            }
            if (enabled && status != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                )
            }
        }
    }
}

@Composable
private fun HomeHeader() {
    val clock = remember { ClockFormatter() }
    var tick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            tick = System.currentTimeMillis()
        }
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = clock.timeLine(tick),
            style = MaterialTheme.typography.displayLarge,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = clock.dateLine(tick),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Aura",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "Command-first surface",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CommandBarBlock(
    onCommandAreaClick: () -> Unit,
    onMicClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 132.dp)
            .clip(RoundedCornerShape(26.dp))
            .clickable(onClick = onCommandAreaClick),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
        tonalElevation = 3.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "INTERFACE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "What do you want to do?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Ask Aura to open, find, continue, or search",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledIconButton(
                onClick = onMicClick,
                shape = CircleShape,
                modifier = Modifier.size(52.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Icon(
                    imageVector = Icons.Outlined.MicNone,
                    contentDescription = "Voice (placeholder)",
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
    )
}

@Composable
private fun PinnedRow(
    cards: List<PinnedCardUi>,
    onCardClick: (PinnedCardUi) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        cards.forEach { card ->
            PinnedCard(card = card, onClick = { onCardClick(card) })
        }
    }
}

@Composable
private fun PinnedCard(
    card: PinnedCardUi,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .width(136.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
            Box(
                modifier = Modifier
                    .size(3.dp, 18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(card.accent),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = card.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = card.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RecentOverflowDialog(
    entries: List<RecentAppEntry>,
    onDismiss: () -> Unit,
    onAppClick: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        title = { Text("More recent") },
        text = {
            Column(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                entries.forEach { entry ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onAppClick(entry.packageName)
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            DrawableImage(
                                drawable = entry.icon,
                                contentDescription = entry.label,
                                modifier = Modifier.size(28.dp),
                            )
                            Text(
                                text = entry.label,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun CompactRecentRow(
    recent: List<RecentAppEntry>,
    onAppClick: (String) -> Unit,
    onSeeMore: () -> Unit,
) {
    if (recent.isEmpty()) {
        Text(
            text = "Launches from Aura appear here.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
        )
        return
    }

    val shown = recent.take(RECENT_ON_HOME_MAX)
    val overflow = recent.size > RECENT_ON_HOME_MAX

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        shown.forEach { entry ->
            RecentChip(
                entry = entry,
                onClick = { onAppClick(entry.packageName) },
            )
        }
        if (overflow) {
            TextButton(
                onClick = onSeeMore,
                modifier = Modifier.padding(start = 4.dp),
            ) {
                Text("See more")
            }
        }
    }
}

@Composable
private fun RecentChip(
    entry: RecentAppEntry,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            DrawableImage(
                drawable = entry.icon,
                contentDescription = entry.label,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = entry.label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AppDrawerContent(
    state: HomeUiState,
    onAppClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        when {
            state.isLoadingApps -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp,
                    )
                }
            }

            state.loadError != null -> {
                Text(
                    text = state.loadError,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                )
            }

            state.installedApps.isEmpty() -> {
                Text(
                    text = "No launchable apps found.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                )
            }

            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(
                        items = state.installedApps,
                        key = { it.packageName },
                    ) { app ->
                        AppDrawerRow(
                            app = app,
                            onClick = { onAppClick(app.packageName) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppDrawerRow(
    app: LauncherAppInfo,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        DrawableImage(
            drawable = app.icon,
            contentDescription = app.label,
            modifier = Modifier.size(40.dp),
        )
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}
