package com.aura.shell.knowledge

import java.util.LinkedHashSet

/**
 * Local-only “working memory” for Knowledge: current item, recent session opens, co-open counts.
 * Not persisted; resets on process death — intentional for lightweight context.
 */
object KnowledgeContextStore {

    private const val SESSION_CAP = 24
    private const val SESSION_MAX_AGE_MS = 45 * 60 * 1000L

    @Volatile
    var currentKnowledgeItemId: String? = null

    private var sessionStartMs: Long = 0L
    private val sessionOrder = LinkedHashSet<String>()

    /** Normalized pair key "min|max" -> count */
    private val coViewCounts = mutableMapOf<String, Int>()

    fun setCurrentKnowledgeItem(id: String?) {
        currentKnowledgeItemId = id
    }

    fun recordOpenInSession(itemId: String) {
        val now = System.currentTimeMillis()
        if (now - sessionStartMs > SESSION_MAX_AGE_MS) {
            sessionOrder.clear()
            sessionStartMs = now
        }
        val prev = currentKnowledgeItemId
        if (prev != null && prev != itemId) {
            recordCoView(prev, itemId)
        }
        synchronized(sessionOrder) {
            sessionOrder.remove(itemId)
            sessionOrder.add(itemId)
            while (sessionOrder.size > SESSION_CAP) {
                val first = sessionOrder.first()
                sessionOrder.remove(first)
            }
        }
        currentKnowledgeItemId = itemId
    }

    fun clearCurrentIfMatches(itemId: String) {
        if (currentKnowledgeItemId == itemId) {
            currentKnowledgeItemId = null
        }
        synchronized(sessionOrder) {
            sessionOrder.remove(itemId)
        }
    }

    fun sessionPeerIds(excludeItemId: String): List<String> {
        synchronized(sessionOrder) {
            return sessionOrder.filter { it != excludeItemId }.reversed()
        }
    }

    fun recordCoView(fromId: String, toId: String) {
        val key = pairKey(fromId, toId)
        coViewCounts[key] = (coViewCounts[key] ?: 0) + 1
    }

    fun coViewStrength(a: String, b: String): Int = coViewCounts[pairKey(a, b)] ?: 0

    private fun pairKey(a: String, b: String): String {
        return if (a <= b) "$a|$b" else "$b|$a"
    }
}
