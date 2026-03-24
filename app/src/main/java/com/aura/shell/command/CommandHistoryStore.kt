package com.aura.shell.command

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray

/**
 * Last N raw command strings for replay in the command layer.
 */
class CommandHistoryStore(
    context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadHistory(): List<String> {
        val json = prefs.getString(KEY_LINES, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    add(arr.getString(i))
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun recordCommand(raw: String) {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return
        val current = loadHistory().toMutableList()
        current.removeAll { it.equals(trimmed, ignoreCase = true) }
        current.add(0, trimmed)
        val next = current.take(MAX_ENTRIES)
        val arr = JSONArray()
        next.forEach { arr.put(it) }
        prefs.edit { putString(KEY_LINES, arr.toString()) }
    }

    companion object {
        private const val PREFS_NAME = "aura_command_history"
        private const val KEY_LINES = "lines"
        private const val MAX_ENTRIES = 18
    }
}
