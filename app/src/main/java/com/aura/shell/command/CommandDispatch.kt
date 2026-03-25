package com.aura.shell.command

import com.aura.shell.model.LauncherAppInfo
import com.aura.shell.model.RecentAppEntry

/**
 * Outcome of parsing a user command — executed by [com.aura.shell.command.CommandLayerViewModel].
 */
sealed class CommandDispatch {
    data class LaunchApp(
        val packageName: String,
        val displayLabel: String,
        /** Short line for UI, e.g. "Best match: Camera" */
        val resultHint: String? = null,
    ) : CommandDispatch()

    data class PickFromSuggestions(
        val message: String,
        val candidates: List<LauncherAppInfo>,
        val subtitle: String? = null,
        val kind: SuggestionKind = SuggestionKind.DISAMBIGUATION,
    ) : CommandDispatch()

    data class SearchMatches(
        val query: String,
        val matches: List<LauncherAppInfo>,
    ) : CommandDispatch()

    data class RecentMatches(
        val title: String,
        val entries: List<RecentAppEntry>,
    ) : CommandDispatch()

    data object OpenAppDrawer : CommandDispatch()

    data object CloseAppDrawer : CommandDispatch()

    /** Bring the default HOME (Aura) to the front — press Home, then voice works hands-free again. */
    data object GoHome : CommandDispatch()

    data class ShowHelp(val lines: List<String>) : CommandDispatch()

    data class Unknown(val message: String) : CommandDispatch()
}
