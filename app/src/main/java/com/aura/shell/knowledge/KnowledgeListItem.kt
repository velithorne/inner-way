package com.aura.shell.knowledge

data class KnowledgeListItem(
    val id: String,
    val title: String,
    val snippet: String,
    val sourceType: KnowledgeSourceType,
    val updatedAt: Long,
    val extractionStatus: KnowledgeExtractionStatus,
    /** Mirrors normalized tag keys (from junction); comma-separated in [KnowledgeItemEntity.tagsCsv]. */
    val tagKeys: List<String> = emptyList(),
    /** Subtle line for related-item commands (not shown on all surfaces). */
    val relationHint: String? = null,
)
