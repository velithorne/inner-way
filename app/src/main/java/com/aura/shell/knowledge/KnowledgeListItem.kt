package com.aura.shell.knowledge

data class KnowledgeListItem(
    val id: String,
    val title: String,
    val snippet: String,
    val sourceType: KnowledgeSourceType,
    val updatedAt: Long,
    val extractionStatus: KnowledgeExtractionStatus,
)
