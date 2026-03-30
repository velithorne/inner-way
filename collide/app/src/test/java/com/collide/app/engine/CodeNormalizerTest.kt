package com.collide.app.engine

import com.collide.app.domain.engine.normalize.CodeNormalizer
import com.collide.app.domain.model.InputSample
import com.collide.app.domain.model.TokenKind
import org.junit.Assert.*
import org.junit.Test

class CodeNormalizerTest {

    private val normalizer = CodeNormalizer()

    @Test
    fun `tokenizes basic python function`() {
        val text = "def add(a, b):\n    return a + b"
        val tokens = normalizer.tokenize(text)

        assertTrue("should have tokens", tokens.isNotEmpty())
        val keywords = tokens.filter { it.kind == TokenKind.KEYWORD }
        assertTrue("should have keyword tokens", keywords.isNotEmpty())
        val defToken = tokens.firstOrNull { it.text == "def" }
        assertNotNull("should have 'def' keyword", defToken)
        assertEquals(TokenKind.KEYWORD, defToken?.kind)
    }

    @Test
    fun `recognizes operators correctly`() {
        val text = "x == y && a != b || c >= d"
        val tokens = normalizer.tokenize(text)
        val ops = tokens.filter { it.kind == TokenKind.OPERATOR }
        val opTexts = ops.map { it.text }.toSet()
        assertTrue("should contain ==", "==" in opTexts)
        assertTrue("should contain &&", "&&" in opTexts)
        assertTrue("should contain !=", "!=" in opTexts)
        assertTrue("should contain ||", "||" in opTexts)
        assertTrue("should contain >=", ">=" in opTexts)
    }

    @Test
    fun `number literals are tagged correctly`() {
        val text = "x = 42 + 3.14"
        val tokens = normalizer.tokenize(text)
        val nums = tokens.filter { it.kind == TokenKind.LITERAL_NUMBER }
        assertEquals("should have 2 number literals", 2, nums.size)
    }

    @Test
    fun `string literals are tagged and abstracted`() {
        val text = """greeting = "hello world" """
        val tokens = normalizer.tokenize(text)
        val str = tokens.firstOrNull { it.kind == TokenKind.LITERAL_STRING }
        assertNotNull("should have string literal", str)
        assertEquals("abstracted text should be placeholder", "\"STR\"", str?.abstractedText)
    }

    @Test
    fun `comments are tagged`() {
        val text = "x = 1 // this is a comment\n# hash comment\ny = 2"
        val tokens = normalizer.tokenize(text)
        val comments = tokens.filter { it.kind == TokenKind.COMMENT }
        assertEquals("should have 2 comments", 2, comments.size)
    }

    @Test
    fun `identifier abstraction is stable`() {
        val text = "myVar + myVar + otherVar"
        val tokens = normalizer.tokenize(text)
        val ids = tokens.filter { it.kind == TokenKind.IDENTIFIER }
        // Same identifier should get same abstracted form
        val myVarAbstracts = ids.filter { it.text == "myVar" }.map { it.abstractedText }.toSet()
        assertEquals("same identifier should have same abstracted form", 1, myVarAbstracts.size)
    }

    @Test
    fun `line numbers are tracked`() {
        val text = "a = 1\nb = 2\nc = 3"
        val tokens = normalizer.tokenize(text)
        val lineNums = tokens.map { it.lineNumber }.distinct().sorted()
        assertTrue("should have multiple line numbers", lineNums.size >= 2)
    }

    @Test
    fun `normalization is deterministic`() {
        val text = "def foo(a, b):\n    return a + b"
        val sample = InputSample("s1", "test", text)
        val m1 = normalizer.normalize(sample)
        val m2 = normalizer.normalize(sample)
        assertEquals("token count should be stable", m1.tokenCount, m2.tokenCount)
        assertEquals("block count should be stable", m1.blockCount, m2.blockCount)
        assertEquals("signature fingerprint should be stable",
            m1.operatorSignature.toFingerprint(),
            m2.operatorSignature.toFingerprint())
    }

    @Test
    fun `similar functions with different names produce same abstracted signature`() {
        val code1 = "def add(a, b):\n    return a + b"
        val code2 = "def sum_values(x, y):\n    return x + y"

        val m1 = normalizer.normalize(InputSample("s1", "code1", code1))
        val m2 = normalizer.normalize(InputSample("s2", "code2", code2))

        // Both have 1 arithmetic op (+), 1 return
        assertEquals("same arithmetic op count", m1.operatorSignature.arithmeticOps, m2.operatorSignature.arithmeticOps)
        assertEquals("same return count", m1.operatorSignature.returnCount, m2.operatorSignature.returnCount)
    }

    @Test
    fun `operator signature counts correctly`() {
        val text = """
            if (x > 0 && y < 10) {
                z = x + y - 1;
            }
        """.trimIndent()
        val tokens = normalizer.tokenize(text)
        val sig = normalizer.computeSignature(tokens)
        assertTrue("should have arithmetic ops", sig.arithmeticOps >= 2)
        assertTrue("should have comparison ops", sig.comparisonOps >= 2)
        assertTrue("should have logical ops", sig.logicalOps >= 1)
        assertTrue("should have conditional", sig.conditionalCount >= 1)
    }
}
