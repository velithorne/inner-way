package com.aura.shell.knowledge

import java.nio.charset.Charset

/**
 * Best-effort text extraction for Phase 4. Unsupported types → null body, [KnowledgeExtractionStatus.METADATA_ONLY].
 */
object KnowledgeTextExtractor {

    private val textishMimePrefixes = listOf(
        "text/",
        "application/json",
        "application/xml",
        "application/x-yaml",
        "application/sql",
    )

    fun isProbablyTextLike(mime: String?, fileName: String?): Boolean {
        val m = mime?.lowercase() ?: ""
        if (textishMimePrefixes.any { m.startsWith(it) || m == it }) return true
        val ext = fileName?.substringAfterLast('.', "")?.lowercase() ?: ""
        return ext in setOf(
            "txt", "md", "markdown", "json", "csv", "log", "tsv",
            "xml", "html", "htm", "yaml", "yml", "sql", "cfg", "ini",
        )
    }

    /**
     * @return pair of (extracted text or null, status)
     */
    fun extractBytes(
        bytes: ByteArray,
        mime: String?,
        fileName: String?,
    ): Pair<String?, KnowledgeExtractionStatus> {
        if (bytes.isEmpty()) {
            return null to KnowledgeExtractionStatus.METADATA_ONLY
        }
        val textish = isProbablyTextLike(mime, fileName)
        if (!textish) {
            if (mime?.startsWith("text/html") == true) {
                return stripHtmlBasic(decodeUtf8(bytes)) to KnowledgeExtractionStatus.EXTRACTED
            }
            return null to KnowledgeExtractionStatus.METADATA_ONLY
        }
        val text = decodeUtf8(bytes)
        if (text.isBlank()) {
            return null to KnowledgeExtractionStatus.METADATA_ONLY
        }
        return text to KnowledgeExtractionStatus.EXTRACTED
    }

    fun snippet(text: String, maxLen: Int = 220): String {
        val t = text.trim().replace(Regex("\\s+"), " ")
        if (t.length <= maxLen) return t
        return t.take(maxLen - 1) + "…"
    }

    private fun decodeUtf8(bytes: ByteArray): String {
        return try {
            bytes.toString(Charset.forName("UTF-8"))
        } catch (_: Exception) {
            bytes.toString(Charsets.UTF_8)
        }
    }

    private fun stripHtmlBasic(html: String): String {
        return html.replace(Regex("<[^>]+>"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
