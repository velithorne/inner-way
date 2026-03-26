package com.aura.shell.data

import android.content.Context
import androidx.core.content.edit
import com.aura.shell.model.RecentAppEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists most-recently-used apps launched from Aura Shell (Phase 1 model).
 */
class RecentAppsStore(
    context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    suspend fun recordLaunch(packageName: String) = withContext(Dispatchers.IO) {
        val list = loadEntriesRaw().toMutableList()
        list.removeAll { it.packageName == packageName }
        list.add(0, RecentEntryRaw(packageName, System.currentTimeMillis()))
        val trimmed = list.take(MAX_RECENTS)
        saveEntries(trimmed)
    }

    suspend fun loadRecentEntries(
        repository: LauncherRepository,
    ): List<RecentAppEntry> = withContext(Dispatchers.Default) {
        val raw = loadEntriesRaw()
        val result = mutableListOf<RecentAppEntry>()
        for (entry in raw) {
            val icon = repository.getIconForPackage(entry.packageName) ?: continue
            val label = repository.getLabelForPackage(entry.packageName) ?: entry.packageName
            result.add(
                RecentAppEntry(
                    packageName = entry.packageName,
                    label = label,
                    icon = icon,
                    lastLaunchTimeMillis = entry.timestampMillis,
                ),
            )
        }
        result
    }

    private fun loadEntriesRaw(): List<RecentEntryRaw> {
        val json = prefs.getString(KEY_ENTRIES, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        RecentEntryRaw(
                            packageName = o.getString("package"),
                            timestampMillis = o.getLong("time"),
                        ),
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveEntries(entries: List<RecentEntryRaw>) {
        val arr = JSONArray()
        entries.forEach { e ->
            arr.put(
                JSONObject().apply {
                    put("package", e.packageName)
                    put("time", e.timestampMillis)
                },
            )
        }
        prefs.edit { putString(KEY_ENTRIES, arr.toString()) }
    }

    private data class RecentEntryRaw(
        val packageName: String,
        val timestampMillis: Long,
    )

    companion object {
        private const val PREFS_NAME = "aura_recent_launches"
        private const val KEY_ENTRIES = "entries"
        private const val MAX_RECENTS = 12
    }
}
