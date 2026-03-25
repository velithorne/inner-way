package com.aura.shell.command

import com.aura.shell.model.LauncherAppInfo
import com.aura.shell.model.RecentAppEntry

import com.aura.shell.personalization.PersonalResolutionContext

/**
 * Routes normalized text to [CommandDispatch] using [AppResolutionEngine] and [CommandAliasRegistry].
 */
class CommandRouter(
    private val resolution: AppResolutionEngine = AppResolutionEngine(),
) {

    suspend fun route(
        rawInput: String,
        installedApps: List<LauncherAppInfo>,
        recentApps: List<RecentAppEntry>,
        appDrawerExpanded: Boolean = false,
        personal: PersonalResolutionContext? = null,
        knowledgeRepository: com.aura.shell.knowledge.KnowledgeRepository? = null,
    ): CommandDispatch {
        val normalized = CommandNormalizer.normalize(rawInput)
        if (normalized.isEmpty()) {
            return CommandDispatch.Unknown("Enter a command, or type help.")
        }

        if (isHelpIntent(normalized)) {
            return CommandDispatch.ShowHelp(HELP_LINES)
        }

        knowledgeRepository?.let { dispatchKnowledge(normalized, it) }?.let { return it }

        if (normalized == "open recent messages") {
            return dispatchRecentMessages(recentApps, personal)
        }

        routePersonalizationMeta(normalized)?.let { return it }

        if (CommandAliasRegistry.matchesAppDrawerIntent(normalized)) {
            return CommandDispatch.OpenAppDrawer
        }
        if (CommandAliasRegistry.matchesHideDrawerIntent(normalized)) {
            return CommandDispatch.CloseAppDrawer
        }
        if (appDrawerExpanded && CommandAliasRegistry.matchesBareCloseOrDismiss(normalized)) {
            return CommandDispatch.CloseAppDrawer
        }
        if (CommandAliasRegistry.matchesBareCloseOrDismiss(normalized) ||
            CommandAliasRegistry.matchesGoHomeIntent(normalized)
        ) {
            return CommandDispatch.GoHome
        }

        val searchQuery = extractSearchQuery(normalized)
        if (searchQuery != null) {
            val matches = resolution.searchApps(searchQuery, installedApps, personal)
            return CommandDispatch.SearchMatches(
                query = searchQuery,
                matches = matches,
                resolutionTargetKey = personal?.learningStore?.normalizeQueryKey(searchQuery),
            )
        }

        val openRecentTarget = extractAfterPrefix(normalized, "open recent ")
            ?: extractAfterPrefix(normalized, "launch recent ")
            ?: extractAfterPrefix(normalized, "start recent ")
        if (openRecentTarget != null) {
            return dispatchOpenRecent(openRecentTarget, recentApps, personal)
        }

        tryRecentListCommands(normalized, recentApps)?.let { return it }

        val openTarget = extractOpenTarget(normalized)
        if (openTarget != null) {
            return dispatchAppResolution(openTarget, installedApps, personal)
        }

        // Bare shorthand: "calc", "yt", "msgs", "setings"
        return dispatchAppResolution(normalized, installedApps, personal)
    }

    private suspend fun dispatchKnowledge(
        normalized: String,
        knowledgeRepository: com.aura.shell.knowledge.KnowledgeRepository,
    ): CommandDispatch? {
        return when (val k = com.aura.shell.knowledge.KnowledgeCommandParser.parse(normalized)) {
            is com.aura.shell.knowledge.KnowledgeCommandParser.Intent.None -> null
            is com.aura.shell.knowledge.KnowledgeCommandParser.Intent.ShowAll ->
                CommandDispatch.OpenKnowledge("list")
            is com.aura.shell.knowledge.KnowledgeCommandParser.Intent.OpenImportPicker ->
                CommandDispatch.OpenKnowledge("import")
            is com.aura.shell.knowledge.KnowledgeCommandParser.Intent.RecentImports ->
                CommandDispatch.OpenKnowledge("imports")
            is com.aura.shell.knowledge.KnowledgeCommandParser.Intent.NewOrSaveNote ->
                CommandDispatch.OpenKnowledge("new")
            is com.aura.shell.knowledge.KnowledgeCommandParser.Intent.SaveFromClipboard ->
                CommandDispatch.OpenKnowledge("clipboard")
            is com.aura.shell.knowledge.KnowledgeCommandParser.Intent.ContinueRecentWork -> {
                val first = knowledgeRepository.getRecent(1).firstOrNull()
                if (first == null) {
                    CommandDispatch.Unknown("No saved knowledge yet. Try “new note”.")
                } else {
                    CommandDispatch.OpenKnowledge("item:${first.id}")
                }
            }
            is com.aura.shell.knowledge.KnowledgeCommandParser.Intent.SearchKnowledge -> {
                val items = knowledgeRepository.searchAll(k.query, 24)
                if (items.isEmpty()) {
                    CommandDispatch.KnowledgeSearchResults(
                        title = "Knowledge",
                        subtitle = "Nothing matched “${k.query}”.",
                        items = emptyList(),
                    )
                } else {
                    CommandDispatch.KnowledgeSearchResults(
                        title = "Knowledge · “${k.query}”",
                        subtitle = "Stored only on this device",
                        items = items,
                    )
                }
            }
            is com.aura.shell.knowledge.KnowledgeCommandParser.Intent.SearchNotes -> {
                val items = knowledgeRepository.searchNotes(k.query, 24)
                if (items.isEmpty()) {
                    CommandDispatch.KnowledgeSearchResults(
                        title = "Notes",
                        subtitle = if (k.query == null) "No notes yet." else "Nothing matched “${k.query}”.",
                        items = emptyList(),
                    )
                } else {
                    CommandDispatch.KnowledgeSearchResults(
                        title = if (k.query != null) "Notes · “${k.query}”" else "Your notes",
                        subtitle = "Tap to open",
                        items = items,
                    )
                }
            }
            is com.aura.shell.knowledge.KnowledgeCommandParser.Intent.OpenLinkAbout -> {
                val items = knowledgeRepository.searchAll(k.query, 12)
                    .filter {
                        it.sourceType == com.aura.shell.knowledge.KnowledgeSourceType.SHARED_URL ||
                            it.title.contains("http", ignoreCase = true)
                    }
                if (items.isEmpty()) {
                    CommandDispatch.Unknown("No saved links matched. Try “show knowledge”.")
                } else if (items.size == 1) {
                    CommandDispatch.OpenKnowledge("item:${items[0].id}")
                } else {
                    CommandDispatch.KnowledgeSearchResults(
                        title = "Saved links",
                        subtitle = "Pick one",
                        items = items,
                    )
                }
            }
        }
    }

    private fun routePersonalizationMeta(normalized: String): CommandDispatch? {
        return when (normalized) {
            "manage aliases",
            "personalization",
            "personalize",
            "manage personalization",
            "my aliases",
            "show my aliases",
            "learned preferences",
            "show learned preferences",
            -> CommandDispatch.OpenPersonalizationPanel
            "reset preferences",
            "clear learned",
            "clear learned preferences",
            -> CommandDispatch.ClearLearnedPreferences
            "reset personalization",
            "clear personalization",
            "clear all personalization",
            -> CommandDispatch.ClearAllPersonalization
            else -> null
        }
    }

    private fun dispatchAppResolution(
        target: String,
        installedApps: List<LauncherAppInfo>,
        personal: PersonalResolutionContext?,
    ): CommandDispatch {
        val cleaned = CommandNormalizer.stripTargetFiller(target)
        if (cleaned.isEmpty()) {
            return CommandDispatch.Unknown("Try: open camera, or yt, or help.")
        }

        return when (val outcome = resolution.resolve(cleaned, installedApps, personal)) {
            is ResolutionOutcome.SingleLaunch -> CommandDispatch.LaunchApp(
                packageName = outcome.app.packageName,
                displayLabel = outcome.app.label,
                resultHint = outcome.hint,
                resolutionTargetKey = personal?.learningStore?.normalizeQueryKey(cleaned) ?: cleaned,
                usedPersonalAlias = outcome.usedPersonalAlias,
                usedLearnedPreference = outcome.usedLearnedPreference,
            )
            is ResolutionOutcome.Suggest -> CommandDispatch.PickFromSuggestions(
                message = outcome.title,
                candidates = outcome.candidates,
                subtitle = outcome.subtitle,
                kind = outcome.kind,
                resolutionTargetKey = personal?.learningStore?.normalizeQueryKey(cleaned) ?: cleaned,
            )
            is ResolutionOutcome.NoMatch -> CommandDispatch.Unknown(outcome.message)
        }
    }

    private fun dispatchOpenRecent(
        target: String,
        recentApps: List<RecentAppEntry>,
        personal: PersonalResolutionContext?,
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
                resolutionTargetKey = personal?.learningStore?.normalizeQueryKey(cleaned) ?: cleaned,
            )
            else -> CommandDispatch.RecentMatches(
                title = "Recent matching \"$cleaned\"",
                entries = matches,
            )
        }
    }

    private fun dispatchRecentMessages(
        recentApps: List<RecentAppEntry>,
        personal: PersonalResolutionContext?,
    ): CommandDispatch {
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
                resolutionTargetKey = personal?.learningStore?.normalizeQueryKey("open recent messages")
                    ?: "open recent messages",
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
            "Hands-free only while Aura’s home/command screen is visible — not while another app is on top (Android limit). Press Home first, then say “Aura”.",
            "Voice: tap the mic, speak, same commands as typing",
            "Personalization (local on device): open vids, tunes, workmail — add aliases in Personalization",
            "music, msg, browser — Aura learns which app you pick over time",
            "manage aliases · show my aliases — open the sheet · reset preferences · clear personalization",
            "go home / home / Aura, close — return to launcher (press device Home if another app is open first)",
            "close apps — hide Aura’s installed-apps drawer only",
            "open camera — or: opn camra, cam, photo",
            "open settings — or: setings, prefs",
            "open msg / msgs / messages — or say “open msg”",
            "yt / yt music — YouTube & related apps",
            "take me to chrome — or: bring up settings",
            "show me apps — open the app drawer",
            "close apps — hide the drawer",
            "search apps for music",
            "show recents — help",
            "Knowledge (local on device): new note · show my notes · search notes for …",
            "show knowledge · show recent imports · search knowledge for … · import file in app",
            "open saved link about … · continue my work · save from clipboard",
        )

        private fun isHelpIntent(n: String): Boolean {
            return n == "help" || n == "?" || n == "h" ||
                n == "what can you do" || n == "what can i do" || n == "commands"
        }

        private fun extractSearchQuery(normalized: String): String? {
            extractAfterPrefix(normalized, "search apps for ")?.let { return it }
            extractAfterPrefix(normalized, "search for ")?.let { return it }
            extractAfterPrefix(normalized, "search apps ")?.let { return it }
            // avoid capturing "find X notes" / knowledge find — handled by knowledge layer
            if (Regex("^find .+ notes$").matches(normalized) ||
                normalized.startsWith("find knowledge")
            ) {
                return null
            }
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
