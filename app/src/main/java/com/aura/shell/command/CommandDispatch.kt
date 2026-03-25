package com.aura.shell.command

import com.aura.shell.knowledge.KnowledgeListItem
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
        /** Normalized phrase for [com.aura.shell.personalization.PreferenceLearningStore]. */
        val resolutionTargetKey: String? = null,
        val usedPersonalAlias: Boolean = false,
        val usedLearnedPreference: Boolean = false,
    ) : CommandDispatch()

    data class PickFromSuggestions(
        val message: String,
        val candidates: List<LauncherAppInfo>,
        val subtitle: String? = null,
        val kind: SuggestionKind = SuggestionKind.DISAMBIGUATION,
        val resolutionTargetKey: String? = null,
    ) : CommandDispatch()

    data class SearchMatches(
        val query: String,
        val matches: List<LauncherAppInfo>,
        /** Normalized key for learning when user picks from search results. */
        val resolutionTargetKey: String? = null,
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

    /** Open the in-app personalization sheet (aliases + learned + reset). */
    data object OpenPersonalizationPanel : CommandDispatch()

    data object ClearLearnedPreferences : CommandDispatch()

    data object ClearAllPersonalization : CommandDispatch()

    data class Unknown(val message: String) : CommandDispatch()

    /** Deep link into knowledge UI: list | imports | notes | new | clipboard | item:<id> | search:<q> | notesearch:<q> */
    data class OpenKnowledge(val path: String) : CommandDispatch()

    data class KnowledgeSearchResults(
        val title: String,
        val subtitle: String?,
        val items: List<KnowledgeListItem>,
    ) : CommandDispatch()
}
