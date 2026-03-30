package com.collide.app.engine

import com.collide.app.domain.engine.normalize.InputProfiler
import com.collide.app.domain.model.InputSample
import com.collide.app.domain.model.InputSourceType
import com.collide.app.domain.model.LanguageHint
import org.junit.Assert.*
import org.junit.Test

class InputProfilerTest {

    private val profiler = InputProfiler()

    @Test
    fun `detects python-like code`() {
        val sample = InputSample("t1", "test", """
            def add(a, b):
                return a + b
            
            def greet(name):
                if name == None:
                    return "Hello, World"
                return "Hello, " + name
        """.trimIndent())
        val profile = profiler.profile(sample)
        assertEquals(LanguageHint.PYTHON_LIKE, profile.estimatedLanguage)
        assertTrue("confidence should be > 0.3", profile.confidence > 0.3f)
    }

    @Test
    fun `detects javascript-like code`() {
        val sample = InputSample("t2", "test", """
            function reverseString(str) {
                const chars = str.split('');
                return chars.reverse().join('');
            }
            
            const greet = (name) => {
                let result = "Hello, " + name;
                return result;
            };
        """.trimIndent())
        val profile = profiler.profile(sample)
        assertEquals(LanguageHint.JAVASCRIPT_LIKE, profile.estimatedLanguage)
        assertTrue("confidence should be > 0.3", profile.confidence > 0.3f)
    }

    @Test
    fun `detects kotlin-like code`() {
        val sample = InputSample("t3", "test", """
            fun computeTotal(items: List<Item>): Float {
                return items.fold(0f) { acc, item -> acc + item.value }
            }
            
            data class Item(val id: String, val value: Float)
            
            val result = computeTotal(emptyList())
        """.trimIndent())
        val profile = profiler.profile(sample)
        assertEquals(LanguageHint.KOTLIN_LIKE, profile.estimatedLanguage)
    }

    @Test
    fun `detects rule snippet`() {
        val sample = InputSample("t4", "test", """
            RULE: AuthorizeUser
            CONDITION: user.role == "admin" AND session.active == true
            ACTION: APPROVE request
            IF user.attempts > 3 THEN
                BLOCK user
            END IF
        """.trimIndent())
        val profile = profiler.profile(sample)
        assertEquals(LanguageHint.RULE_SNIPPET, profile.estimatedLanguage)
    }

    @Test
    fun `detects json-like structure`() {
        val sample = InputSample("t5", "test", """
            {
                "name": "Alice",
                "age": 30,
                "address": {
                    "city": "Springfield",
                    "zip": "12345"
                }
            }
        """.trimIndent())
        val profile = profiler.profile(sample)
        assertEquals(LanguageHint.JSON_LIKE, profile.estimatedLanguage)
    }

    @Test
    fun `token count estimate is reasonable`() {
        val sample = InputSample("t6", "test", "def foo(): return 1 + 2")
        val profile = profiler.profile(sample)
        assertTrue("token count should be > 0", profile.tokenCountEstimate > 0)
        assertTrue("token count should be reasonable", profile.tokenCountEstimate < 100)
    }

    @Test
    fun `line count is correct`() {
        val code = "a = 1\nb = 2\nc = a + b"
        val sample = InputSample("t7", "test", code)
        val profile = profiler.profile(sample)
        assertEquals(3, profile.lineCount)
    }

    @Test
    fun `bracket depth is computed`() {
        val sample = InputSample("t8", "test", "if (a && (b || (c > 0))) { }")
        val profile = profiler.profile(sample)
        assertTrue("bracket depth should be > 0", profile.bracketDepthMax > 0)
    }

    @Test
    fun `handles empty input gracefully`() {
        val sample = InputSample("t9", "test", "")
        val profile = profiler.profile(sample)
        assertEquals(0, profile.tokenCountEstimate)
        assertNotNull(profile.estimatedLanguage)
    }

    @Test
    fun `profiles are deterministic`() {
        val sample = InputSample("t10", "test", "def foo(a, b): return a + b")
        val p1 = profiler.profile(sample)
        val p2 = profiler.profile(sample)
        assertEquals(p1.estimatedLanguage, p2.estimatedLanguage)
        assertEquals(p1.tokenCountEstimate, p2.tokenCountEstimate)
        assertEquals(p1.lineCount, p2.lineCount)
    }
}
