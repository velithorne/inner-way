package com.aura.shell.command

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

/**
 * Recent commands with optional normalized form for stable replay.
 */
class CommandHistoryStore(
    context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadHistory(): List<CommandHistoryEntry> {
        val json = prefs.getString(KEY_LINES, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val item = arr.get(i)
                    when (item) {
                        is String -> add(CommandHistoryEntry(original = item, normalized = null))
                        is JSONObject -> add(
                            CommandHistoryEntry(
                                original = item.optString("original", ""),
                                normalized = item.optString("normalized", "").takeIf { it.isNotEmpty() },
                            ),
                        )
                        else -> { }
                    }
                }
            }.filter { it.original.isNotBlank() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun recordCommand(raw: String, normalizedForReplay: String? = null) {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return
        val current = loadHistory().toMutableList()
        current.removeAll { it.original.equals(trimmed, ignoreCase = true) }
        val entry = CommandHistoryEntry(
            original = trimmed,
            normalized = normalizedForReplay?.takeIf { it.isNotBlank() && it != trimmed },
        )
        current.add(0, entry)
        val next = current.take(MAX_ENTRIES)
        val arr = JSONArray()
        next.forEach { e ->
            val o = JSONObject()
            o.put("original", e.original)
            e.normalized?.let { o.put("normalized", it) }
            arr.put(o)
        }
        prefs.edit { putString(KEY_LINES, arr.toString()) }
    }

    companion object {
        private const val PREFS_NAME = "aura_command_history"
        private const val KEY_LINES = "lines"
        private const val MAX_ENTRIES = 18
    }
}

data class CommandHistoryEntry(
    val original: String,
    val normalized: String?,
)
