package com.aura.shell.command

/**
 * Strips leading "Aura" wake phrase from speech transcripts.
 * Foreground passive mode only — not a custom hotword engine.
 */
object WakePhraseProcessor {

    private val wakePrefix = Regex(
        """^\s*aura\s*[,:]?\s*""",
        RegexOption.IGNORE_CASE,
    )

    fun stripWakePhrase(transcript: String): String? {
        val trimmed = transcript.trim()
        if (trimmed.isEmpty()) return null
        val afterWake = wakePrefix.replace(trimmed, "").trim()
        if (afterWake.isEmpty()) return null
        return afterWake
    }

    /**
     * [followUpMode] true = user already said "Aura"; accept any command text.
     */
    fun classify(transcript: String, followUpMode: Boolean): WakeProcessResult {
        val raw = transcript.trim()
        if (raw.isEmpty()) return WakeProcessResult.Empty

        if (followUpMode) {
            return WakeProcessResult.Command(raw)
        }

        val lower = raw.lowercase()
        if (!lower.startsWith("aura")) {
            return WakeProcessResult.NotWake
        }

        if (raw.length <= 4) {
            return WakeProcessResult.WakeOnly
        }

        val nextChar = raw.getOrNull(4)
        val boundaryOk = nextChar == null || nextChar.isWhitespace() || nextChar == ',' || nextChar == ':'
        if (!boundaryOk) {
            return WakeProcessResult.NotWake
        }

        val rest = stripWakePhrase(raw)
        return when {
            rest == null || rest.isEmpty() -> WakeProcessResult.WakeOnly
            else -> WakeProcessResult.Command(rest)
        }
    }
}

sealed class WakeProcessResult {
    data object Empty : WakeProcessResult()
    data object WakeOnly : WakeProcessResult()
    data class Command(val text: String) : WakeProcessResult()
    /** Spoken text did not start with Aura — ignore in passive wake listen. */
    data object NotWake : WakeProcessResult()
}
