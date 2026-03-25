package com.aura.shell.knowledge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class KnowledgeTextExtractorTest {

    @Test
    fun markdown_extracted() {
        val (text, st) = KnowledgeTextExtractor.extractBytes(
            "# hi\nbody".toByteArray(),
            "text/markdown",
            "note.md",
        )
        assertEquals(KnowledgeExtractionStatus.EXTRACTED, st)
        assertNotNull(text?.contains("body"))
    }

    @Test
    fun randomBinary_metadataOnly() {
        val (text, st) = KnowledgeTextExtractor.extractBytes(
            byteArrayOf(0, 1, 2, 3, -1),
            "application/octet-stream",
            "blob.bin",
        )
        assertEquals(KnowledgeExtractionStatus.METADATA_ONLY, st)
        assertNull(text)
    }
}
