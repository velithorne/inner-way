package com.aura.shell.knowledge

enum class KnowledgeSourceType {
    AURA_NOTE,
    SHARED_TEXT,
    SHARED_URL,
    IMPORTED_FILE,
    CLIPBOARD,
}

enum class KnowledgeExtractionStatus {
    /** Plain text, markdown, json, csv, log-like body extracted. */
    EXTRACTED,

    /** Only name, uri, mime — browse/original only. */
    METADATA_ONLY,

    FAILED,
}
