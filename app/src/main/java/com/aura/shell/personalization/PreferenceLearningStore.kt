package com.aura.shell.personalization

import android.content.Context
import androidx.core.content.edit
import android.util.Base64
import com.aura.shell.command.CommandNormalizer

/**
 * Weighted (query phrase → package) counts from confirmed launches and suggestion picks.
 * Local only. Weights are additive; [bonusFor] applies caps and a minimum history threshold.
 */
class PreferenceLearningStore(
    context: Context,
) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun normalizeQueryKey(raw: String): String {
        return CommandNormalizer.normalize(raw).trim().replace(Regex("\\s+"), " ")
    }

    private fun storageKey(queryKey: String, packageName: String): String {
        val raw = "$queryKey\n$packageName".toByteArray(Charsets.UTF_8)
        val b64 = Base64.encodeToString(raw, Base64.NO_WRAP or Base64.URL_SAFE)
        return "$KEY_PREFIX$b64"
    }

    fun getWeight(queryKey: String, packageName: String): Int {
        val q = normalizeQueryKey(queryKey)
        if (q.isEmpty()) return 0
        return prefs.getInt(storageKey(q, packageName), 0).coerceAtLeast(0)
    }

    /**
     * Record a user-confirmed outcome. Use [LearningSignal] for weight deltas.
     */
    fun record(queryKey: String, packageName: String, signal: LearningSignal) {
        val q = normalizeQueryKey(queryKey)
        if (q.isEmpty() || packageName.isBlank()) return
        val key = storageKey(q, packageName)
        val next = (prefs.getInt(key, 0) + signal.delta).coerceIn(0, MAX_WEIGHT)
        prefs.edit { putInt(key, next) }
    }

    fun clearAll() {
        val ed = prefs.edit()
        prefs.all.keys.forEach { k ->
            if (k.startsWith(KEY_PREFIX)) ed.remove(k)
        }
        ed.apply()
    }

    /**
     * For management UI: list non-zero weights grouped by query.
     */
    fun getAllLearnedRows(): List<LearnedPreferenceRow> {
        val byQuery = mutableMapOf<String, MutableMap<String, Int>>()
        prefs.all.forEach { (fullKey, value) ->
            if (!fullKey.startsWith(KEY_PREFIX) || value !is Int) return@forEach
            val b64 = fullKey.removePrefix(KEY_PREFIX)
            val decoded = try {
                String(Base64.decode(b64, Base64.NO_WRAP or Base64.URL_SAFE), Charsets.UTF_8)
            } catch (_: Exception) {
                return@forEach
            }
            val parts = decoded.split('\n', limit = 2)
            if (parts.size != 2) return@forEach
            val q = parts[0]
            val pkg = parts[1]
            if (value <= 0) return@forEach
            byQuery.getOrPut(q) { mutableMapOf() }[pkg] = value
        }
        return byQuery.entries
            .sortedBy { it.key }
            .flatMap { (query, pkgs) ->
                pkgs.entries
                    .sortedByDescending { it.value }
                    .map { (pkg, w) ->
                        LearnedPreferenceRow(queryKey = query, packageName = pkg, weight = w)
                    }
            }
    }

    companion object {
        private const val PREFS_NAME = "aura_preference_learning"
        private const val KEY_PREFIX = "lr:"
        private const val MAX_WEIGHT = 500
    }
}

enum class LearningSignal(val delta: Int) {
    /** Typed or voice direct launch (single confident outcome). */
    DIRECT_LAUNCH(2),

    /** User tapped a suggestion — strong confirmation. */
    SUGGESTION_PICK(5),

    /** Same command repeated soon — light reinforcement (history heuristic). */
    REPEAT_PATTERN(1),
}

data class LearnedPreferenceRow(
    val queryKey: String,
    val packageName: String,
    val weight: Int,
)

object PersonalLearningBonus {
    /** No bonus until the user has confirmed at least this many times. */
    const val MIN_COUNT_FOR_BONUS = 2

    /** Cap added to base resolution score (keeps personalization from overpowering explicit match). */
    const val MAX_BONUS_POINTS = 95

    /**
     * Extra score points from learned weights. Sublinear so high counts don’t dominate exact matches.
     */
    fun bonusPoints(weight: Int): Int {
        if (weight < MIN_COUNT_FOR_BONUS) return 0
        val over = weight - MIN_COUNT_FOR_BONUS
        val raw = 22 + over * 12
        return raw.coerceAtMost(MAX_BONUS_POINTS)
    }
}
