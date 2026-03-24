package com.aura.shell.command

/**
 * Broadens phrase patterns and strips filler before routing.
 */
object CommandNormalizer {

    private val fillerWords = setOf(
        "my", "the", "a", "an", "please", "just", "can", "you",
    )

    fun normalize(raw: String): String {
        var s = raw
            .lowercase()
            .replace(Regex("[^a-z0-9 ?]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")

        if (s.isEmpty()) return ""

        // Natural phrase → imperative form
        s = applyPhraseTemplates(s)

        // Collapse duplicate spaces again
        s = s.replace(Regex("\\s+"), " ").trim()
        return s
    }

    /**
     * Strip leading filler after open verbs: "open my messages" -> "open messages"
     */
    fun stripTargetFiller(target: String): String {
        val parts = target.split(' ').filter { it.isNotBlank() }.toMutableList()
        while (parts.isNotEmpty() && parts[0] in fillerWords) {
            parts.removeAt(0)
        }
        return parts.joinToString(" ").trim()
    }

    private fun applyPhraseTemplates(s: String): String {
        var out = s

        val phraseOpens = listOf(
            Regex("^take me to (.+)$"),
            Regex("^bring up (.+)$"),
            Regex("^bring me (.+)$"),
            Regex("^show me (.+)$"),
            Regex("^give me (.+)$"),
            Regex("^go to (.+)$"),
            Regex("^switch to (.+)$"),
        )
        for (re in phraseOpens) {
            val m = re.find(out) ?: continue
            out = "open ${m.groupValues[1].trim()}"
            break
        }
        Regex("^start (.+)$").find(out)?.let { m ->
            out = "open ${m.groupValues[1].trim()}"
        }

        // "show camera" / "show settings" -> open (launcher action)
        val showVerb = Regex("^show ([^?]+)$")
        showVerb.find(out)?.let { m ->
            val rest = m.groupValues[1].trim()
            if (!isDrawerPhrase(rest) && !isRecentsPhrase(rest)) {
                out = "open $rest"
            }
        }

        // "hide the apps" -> "hide apps"
        out = out.replace(Regex("^hide the apps$"), "hide apps")
        out = out.replace(Regex("^close the apps$"), "close apps")
        out = out.replace(Regex("^close the app drawer$"), "close apps")

        return out
    }

    private fun isDrawerPhrase(rest: String): Boolean {
        val r = rest.trim()
        return r == "apps" || r == "app drawer" || r == "applications" || r == "all apps"
    }

    private fun isRecentsPhrase(rest: String): Boolean {
        val r = rest.trim()
        return r == "recents" || r.startsWith("recent")
    }
}
