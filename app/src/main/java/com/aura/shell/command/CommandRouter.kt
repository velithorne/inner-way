package com.aura.shell.command

import com.aura.shell.model.LauncherAppInfo
import com.aura.shell.model.RecentAppEntry

/**
 * Routes normalized text to [CommandDispatch] using [AppResolutionEngine] and [CommandAliasRegistry].
 */
class CommandRouter(
    private val resolution: AppResolutionEngine = AppResolutionEngine(),
) {

    fun route(
        rawInput: String,
        installedApps: List<LauncherAppInfo>,
        recentApps: List<RecentAppEntry>,
    ): CommandDispatch {
        val normalized = CommandNormalizer.normalize(rawInput)
        if (normalized.isEmpty()) {
            return CommandDispatch.Unknown("Enter a command, or type help.")
        }

        if (isHelpIntent(normalized)) {
            return CommandDispatch.ShowHelp(HELP_LINES)
        }

        if (normalized == "open recent messages") {
            return dispatchRecentMessages(recentApps)
        }

        if (CommandAliasRegistry.matchesAppDrawerIntent(normalized)) {
            return CommandDispatch.OpenAppDrawer
        }
        if (CommandAliasRegistry.matchesHideDrawerIntent(normalized)) {
            return CommandDispatch.CloseAppDrawer
        }

        val searchQuery = extractSearchQuery(normalized)
        if (searchQuery != null) {
            val matches = resolution.searchApps(searchQuery, installedApps)
            return CommandDispatch.SearchMatches(
                query = searchQuery,
                matches = matches,
            )
        }

        val openRecentTarget = extractAfterPrefix(normalized, "open recent ")
            ?: extractAfterPrefix(normalized, "launch recent ")
            ?: extractAfterPrefix(normalized, "start recent ")
        if (openRecentTarget != null) {
            return dispatchOpenRecent(openRecentTarget, recentApps)
        }

        tryRecentListCommands(normalized, recentApps)?.let { return it }

        val openTarget = extractOpenTarget(normalized)
        if (openTarget != null) {
            return dispatchAppResolution(openTarget, installedApps)
        }

        // Bare shorthand: "calc", "yt", "msgs", "setings"
        return dispatchAppResolution(normalized, installedApps)
    }

    private fun dispatchAppResolution(
        target: String,
        installedApps: List<LauncherAppInfo>,
    ): CommandDispatch {
        val cleaned = CommandNormalizer.stripTargetFiller(target)
        if (cleaned.isEmpty()) {
            return CommandDispatch.Unknown("Try: open camera, or yt, or help.")
        }

        return when (val outcome = resolution.resolve(cleaned, installedApps)) {
            is ResolutionOutcome.SingleLaunch -> CommandDispatch.LaunchApp(
                packageName = outcome.app.packageName,
                displayLabel = outcome.app.label,
                resultHint = outcome.hint,
            )
            is ResolutionOutcome.Suggest -> CommandDispatch.PickFromSuggestions(
                message = outcome.title,
                candidates = outcome.candidates,
                subtitle = outcome.subtitle,
                kind = outcome.kind,
            )
            is ResolutionOutcome.NoMatch -> CommandDispatch.Unknown(outcome.message)
        }
    }

    private fun dispatchOpenRecent(
        target: String,
        recentApps: List<RecentAppEntry>,
    ): CommandDispatch {
        val cleaned = CommandNormalizer.stripTargetFiller(target)
        val matches = filterRecents(recentApps, cleaned)
        return when {
            matches.isEmpty() -> CommandDispatch.RecentMatches(
                title = "No recent app matched \"$cleaned\"",
                entries = recentApps,
            )
            matches.size == 1 -> CommandDispatch.LaunchApp(
                packageName = matches[0].packageName,
                displayLabel = matches[0].label,
                resultHint = "Recent: ${matches[0].label}",
            )
            else -> CommandDispatch.RecentMatches(
                title = "Recent matching \"$cleaned\"",
                entries = matches,
            )
        }
    }

    private fun dispatchRecentMessages(recentApps: List<RecentAppEntry>): CommandDispatch {
        val filtered = recentApps.filter { e ->
            val l = e.label.lowercase()
            l.contains("message") || l.contains("sms") || l.contains("messenger") || l.contains("chat")
        }
        return when {
            filtered.isEmpty() -> CommandDispatch.RecentMatches(
                title = "No messaging app in recents yet",
                entries = recentApps,
            )
            filtered.size == 1 -> CommandDispatch.LaunchApp(
                packageName = filtered[0].packageName,
                displayLabel = filtered[0].label,
                resultHint = "Recent: ${filtered[0].label}",
            )
            else -> CommandDispatch.RecentMatches(
                title = "Messaging apps (recent)",
                entries = filtered,
            )
        }
    }

    private fun tryRecentListCommands(
        normalized: String,
        recentApps: List<RecentAppEntry>,
    ): CommandDispatch? {
        if (normalized == "show recents" ||
            normalized == "list recents" ||
            normalized == "recent apps"
        ) {
            return CommandDispatch.RecentMatches(
                title = "Recent (from Aura)",
                entries = recentApps,
            )
        }
        return null
    }

    private fun filterRecents(entries: List<RecentAppEntry>, target: String): List<RecentAppEntry> {
        val key = target.lowercase().trim()
        return entries
            .map { e ->
                val compact = e.label.lowercase().replace(Regex("[^a-z0-9]+"), "")
                val score = FuzzyMatcher.fuzzyTokenScore(key.replace(" ", ""), compact)
                e to score
            }
            .filter { (e, score) -> score > 400 || eLabelContains(e.label, key) }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    private fun eLabelContains(label: String, key: String): Boolean {
        val l = label.lowercase()
        return l.contains(key) || key.split(' ').any { it.length >= 2 && l.contains(it) }
    }

    companion object {
        val HELP_LINES = listOf(
            "open camera — or: opn camra, cam, photo",
            "open settings — or: setings, prefs",
            "open msg / msgs / messages",
            "yt — YouTube & related apps",
            "take me to chrome — or: bring up settings",
            "show me apps — open the app drawer",
            "close apps — hide the drawer",
            "search apps for music",
            "show recents — help",
        )

        private fun isHelpIntent(n: String): Boolean {
            return n == "help" || n == "?" || n == "h" ||
                n == "what can you do" || n == "what can i do" || n == "commands"
        }

        private fun extractSearchQuery(normalized: String): String? {
            extractAfterPrefix(normalized, "search apps for ")?.let { return it }
            extractAfterPrefix(normalized, "search for ")?.let { return it }
            extractAfterPrefix(normalized, "search apps ")?.let { return it }
            if (normalized.startsWith("find ")) {
                return normalized.removePrefix("find ").trim().takeIf { it.isNotEmpty() }
            }
            if (normalized.startsWith("apps for ")) {
                return normalized.removePrefix("apps for ").trim().takeIf { it.isNotEmpty() }
            }
            return null
        }

        private fun extractOpenTarget(normalized: String): String? {
            val prefixes = listOf(
                "open ",
                "launch ",
                "start ",
                "run ",
            )
            for (p in prefixes) {
                extractAfterPrefix(normalized, p)?.let { return it }
            }
            return null
        }

        private fun extractAfterPrefix(normalized: String, prefix: String): String? {
            if (!normalized.startsWith(prefix)) return null
            val rest = normalized.removePrefix(prefix).trim()
            return rest.takeIf { it.isNotEmpty() }
        }
    }
}
