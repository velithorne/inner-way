package com.aura.shell.knowledge

/**
 * Deterministic knowledge intents from normalized command text (lowercase, normalized punctuation).
 */
object KnowledgeCommandParser {

    sealed class Intent {
        data object None : Intent()
        data object ShowAll : Intent()
        data object RecentImports : Intent()
        data object NewOrSaveNote : Intent()
        data object SaveFromClipboard : Intent()
        data object ContinueRecentWork : Intent()
        data object OpenImportPicker : Intent()
        data class SearchKnowledge(val query: String) : Intent()
        data class SearchNotes(val query: String?) : Intent()
        data class OpenLinkAbout(val query: String) : Intent()
        // Phase 4.1
        data class ShowTagged(val tag: String) : Intent()
        data class FindNotesTagged(val tag: String) : Intent()
        data object RelatedCurrent : Intent()
        data class RelatedTopic(val topic: String) : Intent()
        data class AddTagCurrent(val tag: String) : Intent()
        data class RemoveTagCurrent(val tag: String) : Intent()
    }

    fun parse(normalized: String): Intent {
        val n = normalized.trim()
        if (n.isEmpty()) return Intent.None

        // Tag mutations on current item (require open detail in Knowledge)
        extractAfterPrefixes(n, listOf("tag this as ", "tag this ", "add tag ", "tag add "))?.let { t ->
            return Intent.AddTagCurrent(t)
        }
        extractAfterPrefixes(n, listOf("remove tag ", "delete tag ", "untag "))?.let { t ->
            return Intent.RemoveTagCurrent(t)
        }

        // Browse by tag
        extractAfterPrefixes(n, listOf("show items tagged ", "items tagged ", "browse tag ", "filter tag "))?.let { t ->
            return Intent.ShowTagged(t)
        }
        extractAfterPrefixes(n, listOf("find notes tagged ", "notes tagged ", "show notes tagged "))?.let { t ->
            return Intent.FindNotesTagged(t)
        }

        // Related
        Regex("^find (.+) related items$").find(n)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }?.let {
            return Intent.RelatedTopic(it)
        }
        if (n == "show related items" || n == "show related" || n == "related items" || n == "show related to this") {
            return Intent.RelatedCurrent
        }
        extractAfterPrefix(n, "show related to ")?.let {
            return Intent.RelatedTopic(it)
        }
        Regex("^show (.+) related items$").find(n)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }?.let {
            return Intent.RelatedTopic(it)
        }

        if (n == "show my notes" || n == "my notes" || n == "list notes") {
            return Intent.SearchNotes(null)
        }
        if (n == "show recent imports" || n == "recent imports" || n == "show imports") {
            return Intent.RecentImports
        }
        if (n == "show knowledge" || n == "knowledge" || n == "open knowledge" || n == "my knowledge") {
            return Intent.ShowAll
        }
        if (n == "new note" || n == "save a note" || n == "create note" || n == "add note") {
            return Intent.NewOrSaveNote
        }
        if (n == "save from clipboard" || n == "paste to knowledge" || n == "save clipboard") {
            return Intent.SaveFromClipboard
        }
        if (n == "continue my recent work" || n == "continue my work" || n == "recent work") {
            return Intent.ContinueRecentWork
        }
        if (n == "import file" || n == "open knowledge import" || n == "pick file to import") {
            return Intent.OpenImportPicker
        }

        extractAfterPrefixes(n, listOf("search knowledge for ", "search knowledge ", "find knowledge "))?.let {
            return Intent.SearchKnowledge(it)
        }
        extractAfterPrefixes(n, listOf("search notes for ", "search notes ", "find notes for ", "find notes "))?.let {
            return Intent.SearchNotes(it)
        }
        Regex("^find (.+) notes$").find(n)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }?.let {
            return Intent.SearchNotes(it)
        }
        extractAfterPrefix(n, "open saved link about ")?.let {
            return Intent.OpenLinkAbout(it)
        }
        extractAfterPrefix(n, "remember this ")?.let {
            if (it.isNotBlank()) return Intent.SearchKnowledge(it)
        }
        if (n == "remember this" || n == "save this note" || n == "save this") {
            return Intent.NewOrSaveNote
        }

        return Intent.None
    }

    private fun extractAfterPrefix(n: String, prefix: String): String? {
        if (!n.startsWith(prefix)) return null
        return n.removePrefix(prefix).trim().takeIf { it.isNotEmpty() }
    }

    private fun extractAfterPrefixes(n: String, prefixes: List<String>): String? {
        for (p in prefixes) {
            extractAfterPrefix(n, p)?.let { return it }
        }
        return null
    }
}
