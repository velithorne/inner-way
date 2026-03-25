package com.aura.shell.personalization

import android.content.Context
import androidx.core.content.edit
import com.aura.shell.command.CommandNormalizer

/**
 * User-defined alias → package name. Local only; keys normalized like the command pipeline.
 */
class PersonalAliasStore(
    context: Context,
) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun normalizeAliasKey(raw: String): String {
        return CommandNormalizer.normalize(raw)
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    fun getPackageForAlias(key: String): String? {
        val k = normalizeAliasKey(key)
        if (k.isEmpty()) return null
        return prefs.getString(KEY_PREFIX + k, null)
    }

    fun getAllAliases(): List<PersonalAliasEntry> {
        return prefs.all.entries
            .mapNotNull { (fullKey, value) ->
                if (!fullKey.startsWith(KEY_PREFIX) || value !is String) return@mapNotNull null
                val alias = fullKey.removePrefix(KEY_PREFIX)
                PersonalAliasEntry(alias = alias, packageName = value)
            }
            .sortedBy { it.alias }
    }

    fun setAlias(rawAlias: String, packageName: String) {
        val k = normalizeAliasKey(rawAlias)
        require(k.isNotEmpty()) { "Alias cannot be empty" }
        prefs.edit { putString(KEY_PREFIX + k, packageName) }
    }

    fun removeAlias(rawAlias: String) {
        val k = normalizeAliasKey(rawAlias)
        prefs.edit { remove(KEY_PREFIX + k) }
    }

    fun clearAll() {
        val ed = prefs.edit()
        prefs.all.keys.forEach { key ->
            if (key.startsWith(KEY_PREFIX)) ed.remove(key)
        }
        ed.apply()
    }

    companion object {
        private const val PREFS_NAME = "aura_personal_aliases"
        private const val KEY_PREFIX = "a:"
    }
}

data class PersonalAliasEntry(
    val alias: String,
    val packageName: String,
)
