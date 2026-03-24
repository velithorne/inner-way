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
    ) : CommandDispatch()

    data class PickFromSuggestions(
        val message: String,
        val candidates: List<LauncherAppInfo>,
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

    data class ShowHelp(val lines: List<String>) : CommandDispatch()

    data class Unknown(val message: String) : CommandDispatch()
}
