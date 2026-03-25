package com.aura.shell.command

import com.aura.shell.voice.VoiceCommandPipeline
import com.aura.shell.data.LauncherRepository
import com.aura.shell.data.RecentAppsStore
import com.aura.shell.model.LauncherAppInfo
import com.aura.shell.model.RecentAppEntry
import com.aura.shell.personalization.LearningSignal
import com.aura.shell.personalization.PreferenceLearningStore
import com.aura.shell.personalization.PersonalResolutionContext
import com.aura.shell.ui.home.AppDrawerSessionState

/**
 * Shared submit path for typed, tap-mic, and passive foreground voice.
 */
suspend fun executeCommandPipeline(
    text: String,
    source: CommandInputSource,
    repository: LauncherRepository,
    recentStore: RecentAppsStore,
    historyStore: CommandHistoryStore,
    router: CommandRouter,
    learningStore: PreferenceLearningStore,
    personal: PersonalResolutionContext,
): PipelineResult {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) {
        return PipelineResult.Error("Enter a command.")
    }
    val apps = repository.loadLaunchableApps()
    val recent = recentStore.loadRecentEntries(repository)
    val normalized = CommandNormalizer.normalize(trimmed)
    historyStore.recordCommand(trimmed, normalizedForReplay = normalized, source = source)

    val dispatch = VoiceCommandPipeline.process(
        transcript = trimmed,
        installedApps = apps,
        recentApps = recent,
        appDrawerExpanded = AppDrawerSessionState.expanded,
        router = router,
        personal = personal,
    )
    val history = historyStore.loadHistory()

    return when (dispatch) {
        is CommandDispatch.LaunchApp -> {
            val ok = repository.launchApp(dispatch.packageName)
            if (!ok) {
                return PipelineResult.Error("Couldn’t open ${dispatch.displayLabel}. Try again or pick from suggestions.")
            }
            recentStore.recordLaunch(dispatch.packageName)
            recordLaunchLearning(learningStore, history, trimmed, dispatch)
            PipelineResult.Launched(
                displayLabel = dispatch.displayLabel,
                hint = dispatch.resultHint,
                history = history,
                recentApps = recentStore.loadRecentEntries(repository),
            )
        }
        is CommandDispatch.OpenPersonalizationPanel ->
            PipelineResult.OpenPersonalization(history = history)
        is CommandDispatch.ClearLearnedPreferences -> {
            learningStore.clearAll()
            PipelineResult.SurfaceOnly(
                surface = CommandSurfaceState.Success("Cleared learned preferences. Your custom aliases are unchanged."),
                history = history,
            )
        }
        is CommandDispatch.ClearAllPersonalization -> {
            learningStore.clearAll()
            personal.aliasStore.clearAll()
            PipelineResult.SurfaceOnly(
                surface = CommandSurfaceState.Success("Cleared all personalization on this device."),
                history = history,
            )
        }
        is CommandDispatch.OpenAppDrawer -> PipelineResult.OpenDrawer(history = history)
        is CommandDispatch.CloseAppDrawer -> PipelineResult.CloseDrawer(history = history)
        is CommandDispatch.GoHome -> {
            val ok = repository.goHome()
            if (!ok) {
                return PipelineResult.Error("Couldn’t return home. Try your device’s Home button.")
            }
            PipelineResult.GoHome(history = history)
        }
        else -> PipelineResult.SurfaceOnly(
            surface = surfaceFromDispatch(dispatch),
            history = history,
        )
    }
}

private fun recordLaunchLearning(
    learningStore: PreferenceLearningStore,
    history: List<CommandHistoryEntry>,
    trimmed: String,
    dispatch: CommandDispatch.LaunchApp,
) {
    if (dispatch.usedPersonalAlias) return
    val key = dispatch.resolutionTargetKey?.takeIf { it.isNotBlank() } ?: return
    learningStore.record(key, dispatch.packageName, LearningSignal.DIRECT_LAUNCH)
    val prev = history.getOrNull(1)
    if (prev != null) {
        val same = prev.normalized?.let {
            learningStore.normalizeQueryKey(it) == learningStore.normalizeQueryKey(key)
        } ?: (learningStore.normalizeQueryKey(prev.original) == learningStore.normalizeQueryKey(key))
        if (same) {
            learningStore.record(key, dispatch.packageName, LearningSignal.REPEAT_PATTERN)
        }
    }
}

internal fun surfaceFromDispatch(dispatch: CommandDispatch): CommandSurfaceState {
    return when (dispatch) {
        is CommandDispatch.LaunchApp,
        is CommandDispatch.OpenAppDrawer,
        is CommandDispatch.CloseAppDrawer,
        is CommandDispatch.GoHome,
        is CommandDispatch.OpenPersonalizationPanel,
        is CommandDispatch.ClearLearnedPreferences,
        is CommandDispatch.ClearAllPersonalization,
        -> CommandSurfaceState.Empty
        is CommandDispatch.PickFromSuggestions -> CommandSurfaceState.Suggestions(
            title = dispatch.message,
            subtitle = dispatch.subtitle,
            apps = dispatch.candidates,
            kind = dispatch.kind,
            resolutionTargetKey = dispatch.resolutionTargetKey,
        )
        is CommandDispatch.SearchMatches -> CommandSurfaceState.SearchResults(
            query = dispatch.query,
            apps = dispatch.matches,
            resolutionTargetKey = dispatch.resolutionTargetKey,
        )
        is CommandDispatch.RecentMatches -> CommandSurfaceState.RecentsList(
            title = dispatch.title,
            entries = dispatch.entries,
        )
        is CommandDispatch.ShowHelp -> CommandSurfaceState.Help(dispatch.lines)
        is CommandDispatch.Unknown -> CommandSurfaceState.Unknown(dispatch.message)
    }
}

sealed class PipelineResult {
    data class Launched(
        val displayLabel: String,
        val hint: String?,
        val history: List<CommandHistoryEntry>,
        val recentApps: List<RecentAppEntry>,
    ) : PipelineResult()

    data class OpenDrawer(val history: List<CommandHistoryEntry>) : PipelineResult()
    data class CloseDrawer(val history: List<CommandHistoryEntry>) : PipelineResult()
    data class GoHome(val history: List<CommandHistoryEntry>) : PipelineResult()
    data class OpenPersonalization(val history: List<CommandHistoryEntry>) : PipelineResult()
    data class SurfaceOnly(
        val surface: CommandSurfaceState,
        val history: List<CommandHistoryEntry>,
    ) : PipelineResult()

    data class Error(val message: String) : PipelineResult()
}
