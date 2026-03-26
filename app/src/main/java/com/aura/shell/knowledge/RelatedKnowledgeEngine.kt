package com.aura.shell.knowledge

import com.aura.shell.knowledge.db.KnowledgeDao
import com.aura.shell.knowledge.db.KnowledgeItemEntity

/**
 * Merges manual links with inferred overlap (tags, tokens, session, co-views).
 * Heavier items win for ranking; explanations stay human-readable.
 */
class RelatedKnowledgeEngine(
    private val dao: KnowledgeDao,
) {

    private companion object {
        const val DAY_MS = 24L * 60 * 60 * 1000
    }

    suspend fun relatedForItem(centerId: String, limit: Int = 12): List<RelatedKnowledgeItem> {
        val center = dao.getById(centerId) ?: return emptyList()
        val manual = dao.getManuallyLinkedItems(centerId)
            .map { e ->
                RelatedKnowledgeItem(
                    id = e.id,
                    title = e.title,
                    snippet = e.snippetPreview,
                    reason = "Linked by you",
                    strength = RelatedStrength.MANUAL_LINK,
                )
            }
            .associateBy { it.id }

        val centerTags = dao.getTagKeysForItem(centerId).toSet()
        val centerTokens = tokenSet(center)

        val candidates = mutableMapOf<String, Candidate>()

        fun addCand(e: KnowledgeItemEntity, boost: Int, reason: RelatedReason) {
            if (e.id == centerId) return
            val cur = candidates[e.id]
            val mergedReason = if (cur == null) {
                reason
            } else {
                if (boost > cur.score) reason else cur.reason
            }
            val newScore = maxOf(cur?.score ?: 0, boost)
            candidates[e.id] = Candidate(e, newScore, mergedReason)
        }

        for (row in dao.getRecent(80)) {
            if (row.id == centerId) continue
            // Shared tags
            val otherTags = dao.getTagKeysForItem(row.id).toSet()
            val shared = centerTags.intersect(otherTags)
            if (shared.isNotEmpty()) {
                val label = if (shared.size == 1) {
                    "Shared tag: ${shared.first()}"
                } else {
                    "Shared tags: ${shared.take(3).joinToString(", ")}"
                }
                addCand(row, 80 + shared.size * 15, RelatedReason(label, RelatedStrength.SHARED_TAG))
            }
            // Token overlap
            val ot = tokenSet(row)
            val overlap = centerTokens.intersect(ot).size
            if (overlap >= 2) {
                addCand(
                    row,
                    40 + overlap * 10,
                    RelatedReason("Similar words in title or text", RelatedStrength.TOKEN_OVERLAP),
                )
            }
            // Time proximity (same day import/create)
            if (kotlin.math.abs(row.createdAt - center.createdAt) < DAY_MS &&
                row.id != centerId
            ) {
                addCand(row, 25, RelatedReason("Opened or saved around the same time", RelatedStrength.RECENT_SESSION))
            }
        }

        // Session peers
        for (peerId in KnowledgeContextStore.sessionPeerIds(centerId).take(8)) {
            val e = dao.getById(peerId) ?: continue
            addCand(e, 35, RelatedReason("Recent work in this session", RelatedStrength.RECENT_SESSION))
        }

        // Co-viewed
        for (row in dao.getRecent(60)) {
            if (row.id == centerId) continue
            val cv = KnowledgeContextStore.coViewStrength(centerId, row.id)
            if (cv > 0) {
                addCand(
                    row,
                    30 + cv * 15,
                    RelatedReason("Often viewed together recently", RelatedStrength.CO_VIEWED),
                )
            }
        }

        val merged = manual.toMutableMap()
        for ((id, cand) in candidates) {
            if (manual.containsKey(id)) continue
            val existing = merged[id]
            if (existing == null || cand.score > 120) {
                merged[id] = RelatedKnowledgeItem(
                    id = cand.entity.id,
                    title = cand.entity.title,
                    snippet = cand.entity.snippetPreview,
                    reason = cand.reason.label,
                    strength = cand.reason.strength,
                )
            }
        }

        return merged.values
            .sortedWith(
                compareByDescending<RelatedKnowledgeItem> {
                    when (it.strength) {
                        RelatedStrength.MANUAL_LINK -> 1000
                        RelatedStrength.SHARED_TAG -> 400
                        RelatedStrength.TOKEN_OVERLAP -> 300
                        RelatedStrength.CO_VIEWED -> 250
                        RelatedStrength.RECENT_SESSION -> 200
                    }
                }.thenByDescending { it.title },
            )
            .take(limit)
    }

    private data class RelatedReason(val label: String, val strength: RelatedStrength)

    private data class Candidate(
        val entity: KnowledgeItemEntity,
        val score: Int,
        val reason: RelatedReason,
    )

    private fun tokenSet(e: KnowledgeItemEntity): Set<String> {
        val s = "${e.title} ${e.fileName ?: ""} ${e.snippetPreview}"
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), " ")
        return s.split(Regex("\\s+"))
            .filter { it.length >= 3 }
            .toSet()
    }
}
