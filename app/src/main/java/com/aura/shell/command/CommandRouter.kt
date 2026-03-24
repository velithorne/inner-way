package com.aura.shell.command

import com.aura.shell.model.LauncherAppInfo
import com.aura.shell.model.RecentAppEntry

/**
 * Deterministic local parser — no network, no ML.
 */
class CommandRouter {

    fun route(
        rawInput: String,
        installedApps: List<LauncherAppInfo>,
        recentApps: List<RecentAppEntry>,
    ): CommandDispatch {
        val normalized = normalize(rawInput)
        if (normalized.isEmpty()) {
            return CommandDispatch.Unknown("Enter a command, or type help.")
        }

        if (isHelpIntent(normalized)) {
            return CommandDispatch.ShowHelp(HELP_LINES)
        }

        if (normalized == "open recent messages") {
            return dispatchRecentMessages(recentApps)
        }

        if (isShowAppsIntent(normalized)) {
            return CommandDispatch.OpenAppDrawer
        }
        if (isHideAppsIntent(normalized)) {
            return CommandDispatch.CloseAppDrawer
        }

        val searchQuery = extractSearchQuery(normalized)
        if (searchQuery != null) {
            val matches = filterApps(installedApps, searchQuery)
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

        val recentDispatch = tryRecentListCommands(normalized, recentApps)
        if (recentDispatch != null) return recentDispatch

        return tryOpenLaunch(normalized, installedApps)
    }

    private fun dispatchOpenRecent(
        target: String,
        recentApps: List<RecentAppEntry>,
    ): CommandDispatch {
        val matches = filterRecents(recentApps, target)
        return when {
            matches.isEmpty() -> CommandDispatch.RecentMatches(
                title = "No recent app matched \"$target\"",
                entries = recentApps,
            )
            matches.size == 1 -> CommandDispatch.LaunchApp(
                packageName = matches[0].packageName,
                displayLabel = matches[0].label,
            )
            else -> CommandDispatch.RecentMatches(
                title = "Recent matching \"$target\"",
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

    private fun tryOpenLaunch(
        normalized: String,
        installedApps: List<LauncherAppInfo>,
    ): CommandDispatch {
        val target = extractOpenTarget(normalized) ?: return CommandDispatch.Unknown(
            "Try: open settings, or open camera, or help.",
        )
        val matches = scoreAndRank(installedApps, target)
        return when {
            matches.isEmpty() -> CommandDispatch.Unknown(
                "No app matched \"$target\". Try search apps for $target",
            )
            matches.size == 1 || (matches[0].score >= STRONG_MATCH && matches[1].score < matches[0].score - 15) -> {
                val best = matches[0].app
                CommandDispatch.LaunchApp(
                    packageName = best.packageName,
                    displayLabel = best.label,
                )
            }
            else -> {
                val top = matches.take(8).map { it.app }
                CommandDispatch.PickFromSuggestions(
                    message = "Several apps match \"$target\"",
                    candidates = top,
                )
            }
        }
    }

    private data class Scored(
        val app: LauncherAppInfo,
        val score: Int,
    )

    private fun scoreAndRank(apps: List<LauncherAppInfo>, target: String): List<Scored> {
        val t = target.lowercase()
        return apps
            .map { app -> Scored(app, scoreLabel(app.label, t)) }
            .filter { it.score > 0 }
            .sortedByDescending { it.score }
    }

    private fun scoreLabel(label: String, target: String): Int {
        val l = label.lowercase().replace(Regex("[^a-z0-9 ]+"), " ").trim().replace(Regex("\\s+"), " ")
        val tgt = target.lowercase()
        if (l == tgt) return 1000
        if (l.startsWith(tgt)) return 800 - (l.length - tgt.length).coerceAtMost(100)
        if (l.contains(tgt)) return 500 - (l.indexOf(tgt)).coerceAtMost(50)
        val tokens = tgt.split(' ').filter { it.isNotBlank() }
        var s = 0
        for (tok in tokens) {
            if (tok.length < 2) continue
            if (l.contains(tok)) s += 120
        }
        return s
    }

    private fun filterApps(apps: List<LauncherAppInfo>, query: String): List<LauncherAppInfo> {
        val q = query.lowercase()
        return apps
            .map { app -> app to scoreLabel(app.label, q) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    private fun filterRecents(entries: List<RecentAppEntry>, target: String): List<RecentAppEntry> {
        val q = target.lowercase()
        return entries
            .map { e -> e to scoreLabel(e.label, q) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    companion object {
        private const val STRONG_MATCH = 400

        val HELP_LINES = listOf(
            "open <app> — launch an installed app (e.g. open camera, open settings)",
            "launch <app> — same as open",
            "search apps for <text> — list matching apps",
            "find <text> — search apps by name",
            "show apps / hide apps — open or close the app drawer on home",
            "show recents — apps you opened recently from Aura",
            "open recent <name> — launch a recent app by name",
            "help — this list",
        )

        fun normalize(raw: String): String {
            return raw
                .lowercase()
                .replace(Regex("[^a-z0-9 ?]+"), " ")
                .trim()
                .replace(Regex("\\s+"), " ")
        }

        private fun isHelpIntent(n: String): Boolean {
            return n == "help" || n == "?" || n == "h" ||
                n == "what can you do" || n == "what can i do" || n == "commands"
        }

        private fun isShowAppsIntent(n: String): Boolean {
            return n == "show apps" || n == "open apps" || n == "open app drawer" ||
                n == "app drawer" || n == "list apps" || n == "open all apps"
        }

        private fun isHideAppsIntent(n: String): Boolean {
            return n == "hide apps" || n == "close apps" || n == "dismiss apps" ||
                n == "close app drawer" || n == "close drawer"
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
            val prefixes = listOf("open ", "launch ", "start ", "run ")
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
