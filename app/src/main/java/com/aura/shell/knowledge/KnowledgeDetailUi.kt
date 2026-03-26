package com.aura.shell.knowledge

import com.aura.shell.knowledge.db.KnowledgeItemEntity

data class KnowledgeDetailUi(
    val entity: KnowledgeItemEntity,
    val tags: List<String>,
    val tagSuggestions: List<String>,
    val related: List<RelatedKnowledgeItem>,
)
