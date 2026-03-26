package com.aura.shell.knowledge

import com.aura.shell.knowledge.db.KnowledgeItemEntity
import java.util.LinkedHashMap

/**
 * Conservative local tag suggestions from title, filename, and text cues. No ML.
 */
object TagSuggestionEngine {

    private val stopWords = setOf(
        "the", "and", "for", "with", "from", "this", "that", "have", "has", "are", "was", "were",
        "into", "your", "about", "what", "when", "where", "which", "note", "file", "link", "text",
    )

    fun suggestionsForItem(
        entity: KnowledgeItemEntity,
        existingTagKeys: Set<String>,
        frequentUserTagKeys: List<String>,
    ): List<String> {
        val out = LinkedHashMap<String, Unit>()
        val rawBlob = buildString {
            append(entity.title)
            append(' ')
            entity.fileName?.let { append(it).append(' ') }
            append(entity.fullText.take(800))
        }
        tokenize(rawBlob).forEach { t ->
            val key = TagNormalizer.normalize(t)
            if (key.length >= 3 && key !in existingTagKeys) {
                out[key] = Unit
            }
        }
        // Combined alphanumeric tokens like phase9
        Regex("(?i)[a-z]+\\d+|\\d+[a-z]+").findAll(rawBlob).forEach { m ->
            val key = TagNormalizer.normalize(m.value)
            if (key.length >= 3 && key !in existingTagKeys) {
                out[key] = Unit
            }
        }
        sourceCue(entity).let { cue ->
            if (cue != null && cue !in existingTagKeys) {
                out[cue] = Unit
            }
        }
        frequentUserTagKeys.forEach { fk ->
            if (fk !in existingTagKeys && rawBlob.lowercase().contains(fk)) {
                out[fk] = Unit
            }
        }
        return out.keys.take(12)
    }

    private fun sourceCue(entity: KnowledgeItemEntity): String? {
        return when (entity.sourceType) {
            KnowledgeSourceType.IMPORTED_FILE.name -> "import"
            KnowledgeSourceType.SHARED_URL.name -> "link"
            KnowledgeSourceType.AURA_NOTE.name -> null
            KnowledgeSourceType.SHARED_TEXT.name -> null
            KnowledgeSourceType.CLIPBOARD.name -> null
            else -> null
        }
    }

    private fun tokenize(text: String): List<String> {
        return text
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), " ")
            .split(Regex("\\s+"))
            .filter { it.length >= 4 && it !in stopWords }
            .distinct()
    }
}
