package com.aura.shell.knowledge

import android.content.Context
import android.net.Uri
import com.aura.shell.AuraApplication
import com.aura.shell.knowledge.db.KnowledgeDao
import com.aura.shell.knowledge.db.KnowledgeItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class KnowledgeRepository(
    context: Context,
    database: com.aura.shell.knowledge.db.KnowledgeDatabase? = null,
) {
    private val appContext = context.applicationContext
    private val dao: KnowledgeDao = (database ?: (appContext as AuraApplication).knowledgeDatabase).knowledgeDao()

    fun observeRecent(limit: Int = 40): Flow<List<KnowledgeListItem>> {
        return dao.observeRecent(limit).map { list -> list.map { it.toListItem() } }
    }

    suspend fun getRecent(limit: Int): List<KnowledgeListItem> = withContext(Dispatchers.IO) {
        dao.getRecent(limit).map { it.toListItem() }
    }

    suspend fun getById(id: String): KnowledgeItemEntity? = withContext(Dispatchers.IO) {
        dao.getById(id)
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
        dao.insert(
            existing.copy(
                title = t,
                fullText = full,
                snippetPreview = KnowledgeTextExtractor.snippet(full),
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        dao.getById(id)?.let { dao.delete(it) }
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
