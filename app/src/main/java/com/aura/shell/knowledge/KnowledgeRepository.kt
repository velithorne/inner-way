package com.aura.shell.knowledge

import android.content.Context
import android.net.Uri
import com.aura.shell.AuraApplication
import com.aura.shell.knowledge.db.KnowledgeDao
import com.aura.shell.knowledge.db.KnowledgeItemEntity
import com.aura.shell.knowledge.db.KnowledgeItemTagCrossRef
import com.aura.shell.knowledge.db.KnowledgeManualLinkEntity
import com.aura.shell.knowledge.db.TagEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.LinkedHashSet
import java.util.UUID

class KnowledgeRepository(
    context: Context,
    database: com.aura.shell.knowledge.db.KnowledgeDatabase? = null,
) {
    private val appContext = context.applicationContext
    private val dao: KnowledgeDao = (database ?: (appContext as AuraApplication).knowledgeDatabase).knowledgeDao()
    private val relatedEngine = RelatedKnowledgeEngine(dao)

    fun observeRecent(limit: Int = 40): Flow<List<KnowledgeListItem>> {
        return dao.observeRecent(limit).map { list -> list.map { it.toListItem() } }
    }

    suspend fun getRecent(limit: Int): List<KnowledgeListItem> = withContext(Dispatchers.IO) {
        dao.getRecent(limit).map { it.toListItem() }
    }

    suspend fun getById(id: String): KnowledgeItemEntity? = withContext(Dispatchers.IO) {
        dao.getById(id)
    }

    suspend fun getTagKeysForItem(itemId: String): List<String> = withContext(Dispatchers.IO) {
        dao.getTagKeysForItem(itemId)
    }

    suspend fun getAllTagKeys(limit: Int = 200): List<String> = withContext(Dispatchers.IO) {
        dao.getAllTagKeys(limit)
    }

    suspend fun getPopularTagKeys(limit: Int = 14): List<String> = withContext(Dispatchers.IO) {
        dao.getPopularTagKeys(limit)
    }

    suspend fun addTagToItem(itemId: String, rawTag: String) = withContext(Dispatchers.IO) {
        val key = TagNormalizer.normalize(rawTag)
        if (key.length < 2) return@withContext
        dao.insertTag(TagEntity(key))
        dao.insertItemTag(KnowledgeItemTagCrossRef(itemId, key))
        syncTagsCsv(itemId)
    }

    suspend fun removeTagFromItem(itemId: String, rawTag: String) = withContext(Dispatchers.IO) {
        val key = TagNormalizer.normalize(rawTag)
        dao.deleteItemTag(itemId, key)
        syncTagsCsv(itemId)
    }

    suspend fun tagSuggestionsFor(itemId: String): List<String> = withContext(Dispatchers.IO) {
        val e = dao.getById(itemId) ?: return@withContext emptyList()
        val existing = dao.getTagKeysForItem(itemId).toSet()
        val freq = dao.getPopularTagKeys(40).filter { it !in existing }
        TagSuggestionEngine.suggestionsForItem(e, existing, freq)
    }

    suspend fun linkItemsManually(fromId: String, toId: String) = withContext(Dispatchers.IO) {
        if (fromId == toId) return@withContext
        dao.insertManualLink(
            KnowledgeManualLinkEntity(
                fromItemId = fromId,
                toItemId = toId,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun unlinkItemsManually(a: String, b: String) = withContext(Dispatchers.IO) {
        dao.deleteManualLinkBidirectional(a, b)
    }

    suspend fun relatedFor(itemId: String, limit: Int = 12): List<RelatedKnowledgeItem> = withContext(Dispatchers.IO) {
        relatedEngine.relatedForItem(itemId, limit)
    }

    suspend fun relatedAsListItems(centerId: String, limit: Int = 16): List<KnowledgeListItem> = withContext(Dispatchers.IO) {
        relatedEngine.relatedForItem(centerId, limit).mapNotNull { r ->
            dao.getById(r.id)?.toListItem()?.copy(relationHint = r.reason)
        }
    }

    suspend fun searchByTag(tagRaw: String, limit: Int = 40): List<KnowledgeListItem> = withContext(Dispatchers.IO) {
        val key = TagNormalizer.normalize(tagRaw)
        if (key.length < 2) return@withContext emptyList()
        dao.getItemsWithTag(key, limit).map { it.toListItem() }
    }

    suspend fun searchKnowledgeWithOptionalTag(
        textQuery: String,
        tagRaw: String?,
        limit: Int = 24,
    ): List<KnowledgeListItem> = withContext(Dispatchers.IO) {
        val tagKey = tagRaw?.let { TagNormalizer.normalize(it) }?.takeIf { it.length >= 2 }
        val text = textQuery.trim()
        val base: List<KnowledgeItemEntity> = when {
            text.isEmpty() && tagKey != null -> dao.getItemsWithTag(tagKey, limit)
            tagKey == null && text.isNotEmpty() -> dao.searchFts(buildFtsQuery(text), limit * 2)
            tagKey != null && text.isNotEmpty() -> {
                val fts = dao.searchFts(buildFtsQuery(text), limit * 3).filter { e ->
                    dao.getTagKeysForItem(e.id).contains(tagKey)
                }
                if (fts.isNotEmpty()) fts else {
                    dao.getItemsWithTag(tagKey, limit * 2).filter { e ->
                        val blob = "${e.title} ${e.fullText}".lowercase()
                        text.lowercase().split(Regex("\\s+")).any { it.length >= 2 && blob.contains(it) }
                    }
                }
            }
            else -> emptyList()
        }
        base.take(limit).map { it.toListItem() }
    }

    /**
     * Cluster for “continue work”: anchor + co-view/session + tag overlap with recents.
     */
    suspend fun continueRecentCluster(limit: Int = 8): List<KnowledgeListItem> = withContext(Dispatchers.IO) {
        val recentIds = dao.getRecent(12).map { it.id }
        if (recentIds.isEmpty()) return@withContext emptyList()
        val anchor = recentIds.first()
        val relatedList = relatedAsListItems(anchor, limit * 3)
        val hintById = relatedList.associate { it.id to it.relationHint }
        val ordered = LinkedHashSet<String>()
        ordered.add(anchor)
        relatedList.forEach { r ->
            if (ordered.size >= limit) return@forEach
            ordered.add(r.id)
        }
        recentIds.drop(1).forEach { id ->
            if (ordered.size >= limit) return@forEach
            if (!ordered.contains(id)) ordered.add(id)
        }
        ordered.take(limit).mapNotNull { id ->
            val base = dao.getById(id)?.toListItem() ?: return@mapNotNull null
            val hint = when (id) {
                anchor -> "Latest in Knowledge"
                else -> hintById[id] ?: "Recent nearby"
            }
            base.copy(relationHint = hint)
        }
    }

    suspend fun itemsForPickLinkDialog(excludeId: String, query: String, limit: Int = 30): List<KnowledgeListItem> =
        withContext(Dispatchers.IO) {
            val q = query.trim().lowercase()
            dao.getRecent(120)
                .filter { it.id != excludeId }
                .filter {
                    if (q.isEmpty()) true else {
                        it.title.lowercase().contains(q) ||
                            it.snippetPreview.lowercase().contains(q)
                    }
                }
                .take(limit)
                .map { it.toListItem() }
        }

    suspend fun insertNote(title: String, body: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val full = body.trim()
        val t = title.ifBlank { KnowledgeTextExtractor.snippet(full, 48).ifBlank { "Note" } }
        val entity = KnowledgeItemEntity(
            id = id,
            title = t,
            fullText = full,
            snippetPreview = KnowledgeTextExtractor.snippet(full),
            sourceType = KnowledgeSourceType.AURA_NOTE.name,
            sourceUri = null,
            fileName = null,
            mimeType = null,
            tagsCsv = null,
            extractionStatus = KnowledgeExtractionStatus.EXTRACTED.name,
            isAuraAuthored = true,
            createdAt = now,
            updatedAt = now,
        )
        dao.insert(entity)
        id
    }

    suspend fun insertSharedText(text: String, suggestedTitle: String?) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val full = text.trim()
        val t = suggestedTitle?.trim()?.takeIf { it.isNotEmpty() }
            ?: KnowledgeTextExtractor.snippet(full, 60).ifBlank { "Shared text" }
        val entity = KnowledgeItemEntity(
            id = id,
            title = t,
            fullText = full,
            snippetPreview = KnowledgeTextExtractor.snippet(full),
            sourceType = KnowledgeSourceType.SHARED_TEXT.name,
            sourceUri = null,
            fileName = null,
            mimeType = "text/plain",
            tagsCsv = null,
            extractionStatus = KnowledgeExtractionStatus.EXTRACTED.name,
            isAuraAuthored = false,
            createdAt = now,
            updatedAt = now,
        )
        dao.insert(entity)
        id
    }

    suspend fun insertSharedUrl(url: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val u = url.trim()
        val entity = KnowledgeItemEntity(
            id = id,
            title = u,
            fullText = u,
            snippetPreview = KnowledgeTextExtractor.snippet(u, 120),
            sourceType = KnowledgeSourceType.SHARED_URL.name,
            sourceUri = u,
            fileName = null,
            mimeType = null,
            tagsCsv = null,
            extractionStatus = KnowledgeExtractionStatus.EXTRACTED.name,
            isAuraAuthored = false,
            createdAt = now,
            updatedAt = now,
        )
        dao.insert(entity)
        id
    }

    suspend fun importFromUri(uri: Uri, fileName: String?, mime: String?) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val resolver = appContext.contentResolver
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: byteArrayOf()
        val name = fileName ?: uri.lastPathSegment ?: "import"
        val (text, status) = KnowledgeTextExtractor.extractBytes(bytes, mime, name)
        val full = text ?: ""
        val title = name.ifBlank { "Imported file" }
        val entity = KnowledgeItemEntity(
            id = id,
            title = title,
            fullText = full,
            snippetPreview = when (status) {
                KnowledgeExtractionStatus.EXTRACTED -> KnowledgeTextExtractor.snippet(full)
                else -> "No preview · open original file"
            },
            sourceType = KnowledgeSourceType.IMPORTED_FILE.name,
            sourceUri = uri.toString(),
            fileName = name,
            mimeType = mime,
            tagsCsv = null,
            extractionStatus = status.name,
            isAuraAuthored = false,
            createdAt = now,
            updatedAt = now,
        )
        dao.insert(entity)
        id
    }

    suspend fun insertClipboardText(text: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val full = text.trim()
        val entity = KnowledgeItemEntity(
            id = id,
            title = KnowledgeTextExtractor.snippet(full, 48).ifBlank { "Clipboard" },
            fullText = full,
            snippetPreview = KnowledgeTextExtractor.snippet(full),
            sourceType = KnowledgeSourceType.CLIPBOARD.name,
            sourceUri = null,
            fileName = null,
            mimeType = "text/plain",
            tagsCsv = null,
            extractionStatus = KnowledgeExtractionStatus.EXTRACTED.name,
            isAuraAuthored = false,
            createdAt = now,
            updatedAt = now,
        )
        dao.insert(entity)
        id
    }

    suspend fun updateAuraNote(id: String, title: String, body: String) = withContext(Dispatchers.IO) {
        val existing = dao.getById(id) ?: return@withContext
        if (!existing.isAuraAuthored || existing.sourceType != KnowledgeSourceType.AURA_NOTE.name) return@withContext
        val full = body.trim()
        val t = title.ifBlank { KnowledgeTextExtractor.snippet(full, 48).ifBlank { "Note" } }
        dao.update(
            existing.copy(
                title = t,
                fullText = full,
                snippetPreview = KnowledgeTextExtractor.snippet(full),
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        KnowledgeContextStore.clearCurrentIfMatches(id)
        dao.getById(id)?.let { dao.delete(it) }
    }

    private suspend fun syncTagsCsv(itemId: String) {
        val keys = dao.getTagKeysForItem(itemId)
        val csv = keys.joinToString(",")
        val existing = dao.getById(itemId) ?: return
        dao.update(
            existing.copy(
                tagsCsv = csv.ifEmpty { null },
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun searchAll(query: String, limit: Int = 24): List<KnowledgeListItem> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isEmpty()) return@withContext emptyList()
        val match = buildFtsQuery(q)
        dao.searchFts(match, limit).map { it.toListItem() }
    }

    suspend fun searchNotes(query: String?, limit: Int = 24): List<KnowledgeListItem> = withContext(Dispatchers.IO) {
        val recent = dao.getRecent(80)
        val noteish = recent.filter {
            it.sourceType == KnowledgeSourceType.AURA_NOTE.name ||
                it.sourceType == KnowledgeSourceType.SHARED_TEXT.name ||
                it.sourceType == KnowledgeSourceType.CLIPBOARD.name
        }
        val q = query?.trim()?.lowercase() ?: ""
        val filtered = if (q.isEmpty()) noteish else {
            noteish.filter {
                it.title.lowercase().contains(q) ||
                    it.fullText.lowercase().contains(q) ||
                    (it.fileName?.lowercase()?.contains(q) == true)
            }
        }
        filtered.take(limit).map { it.toListItem() }
    }

    suspend fun recentImports(limit: Int = 24): List<KnowledgeListItem> = withContext(Dispatchers.IO) {
        dao.getRecent(100)
            .filter { it.sourceType == KnowledgeSourceType.IMPORTED_FILE.name }
            .take(limit)
            .map { it.toListItem() }
    }

    private fun KnowledgeItemEntity.toListItem() = KnowledgeListItem(
        id = id,
        title = title,
        snippet = snippetPreview,
        sourceType = KnowledgeSourceType.valueOf(sourceType),
        updatedAt = updatedAt,
        extractionStatus = try {
            KnowledgeExtractionStatus.valueOf(extractionStatus)
        } catch (_: Exception) {
            KnowledgeExtractionStatus.METADATA_ONLY
        },
        tagKeys = tagsCsv?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList(),
        relationHint = null,
    )

    /**
     * Prefix/wildcard FTS4 match: token* OR token2*
     */
    fun buildFtsQuery(userQuery: String): String {
        val tokens = userQuery
            .trim()
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]+"), " ")
            .split(Regex("\\s+"))
            .filter { it.length >= 2 }
            .take(8)
        if (tokens.isEmpty()) return "\"${userQuery.replace("\"", "\"\"")}\"*"
        return tokens.joinToString(" OR ") { token ->
            val safe = token.replace("\"", "\"\"")
            "\"$safe\"*"
        }
    }
}
