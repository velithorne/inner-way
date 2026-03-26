package com.aura.shell.knowledge

data class RelatedKnowledgeItem(
    val id: String,
    val title: String,
    val snippet: String,
    val reason: String,
    val strength: RelatedStrength,
)

enum class RelatedStrength {
    MANUAL_LINK,
    SHARED_TAG,
    TOKEN_OVERLAP,
    RECENT_SESSION,
    CO_VIEWED,
}
