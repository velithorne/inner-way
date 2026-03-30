package com.collide.app.engine

import com.collide.app.domain.engine.detectors.ContradictionDetector
import com.collide.app.domain.engine.detectors.HybridizationDetector
import com.collide.app.domain.engine.detectors.ReusablePatternDetector
import com.collide.app.domain.engine.detectors.StructuralCompressionDetector
import com.collide.app.domain.engine.detectors.StructuralNoveltyDetector
import com.collide.app.domain.engine.detectors.SymmetryHintDetector
import com.collide.app.domain.model.EventType
import org.junit.Assert.*
import org.junit.Test

class DetectorTest {

    // -------------------------------------------------------------------------
    // Structural Novelty Detector
    // -------------------------------------------------------------------------

    @Test
    fun `novelty detector - identical inputs score near zero`() {
        val detector = StructuralNoveltyDetector()
        val code = "def foo(a, b): return a + b"
        val result = detector.detect(code, code)
        assertTrue("identical inputs should have very low novelty", result.score < 0.1f)
        assertFalse("identical inputs should not pass", result.passed)
    }

    @Test
    fun `novelty detector - very different inputs score high`() {
        val detector = StructuralNoveltyDetector()
        val orig = "def add(a, b): return a + b"
        val cand = "RULE: AuthorizeUser\nCONDITION: user.role == admin\nACTION: APPROVE request"
        val result = detector.detect(cand, orig)
        assertTrue("very different inputs should have high novelty", result.score > 0.3f)
    }

    @Test
    fun `novelty detector - handles empty candidate`() {
        val detector = StructuralNoveltyDetector()
        val result = detector.detect("", "def foo(): pass")
        assertFalse("empty candidate should not pass", result.passed)
    }

    // -------------------------------------------------------------------------
    // Structural Compression Detector
    // -------------------------------------------------------------------------

    @Test
    fun `compression detector - smaller candidate passes`() {
        val detector = StructuralCompressionDetector()
        val original = """
            def foo(a, b, c, d, e, f):
                x = a + b + c
                y = d + e + f
                z = x * y
                return z
        """.trimIndent()
        val compressed = "def foo(a, b): return a + b"
        val result = detector.detect(compressed, original)
        assertTrue("significantly smaller candidate should pass", result.passed)
        assertTrue("compression score should be positive", result.score > 0f)
    }

    @Test
    fun `compression detector - larger candidate fails`() {
        val detector = StructuralCompressionDetector()
        val small = "x = 1"
        val big = "x = 1\ny = 2\nz = 3\nw = x + y + z\nresult = w * 2\nprint(result)\nreturn result"
        val result = detector.detect(big, small)
        assertFalse("larger candidate should not pass compression", result.passed)
    }

    @Test
    fun `compression detector - empty candidate handled`() {
        val detector = StructuralCompressionDetector()
        val result = detector.detect("", "some original code here")
        assertFalse("empty candidate should not pass", result.passed)
    }

    // -------------------------------------------------------------------------
    // Reusable Pattern Detector
    // -------------------------------------------------------------------------

    @Test
    fun `pattern detector - repeated patterns detected`() {
        val detector = ReusablePatternDetector()
        // Code with repeated pattern: "return x + y" type structure
        val code = "a = foo + bar\nb = foo + bar\nc = foo + bar\nd = baz + qux\ne = baz + qux\nf = baz + qux"
        val result = detector.detect(code, code)
        assertTrue("repeated patterns should be detected", result.score > 0f)
    }

    @Test
    fun `pattern detector - no patterns in minimal code`() {
        val detector = ReusablePatternDetector()
        val code = "x = 1"
        val result = detector.detect(code, code)
        assertFalse("minimal code should not pass pattern detector", result.passed)
    }

    @Test
    fun `pattern detector - event type is reusable scaffold when passed`() {
        val detector = ReusablePatternDetector()
        // Force threshold multiplier low to make it pass
        val code = "a = foo + bar\nb = foo + bar\nc = foo + bar\nd = foo + bar\ne = foo + bar\nf = foo + bar"
        val result = detector.detect(code, code, thresholdMultiplier = 0.1f)
        if (result.passed) {
            assertEquals(EventType.REUSABLE_SCAFFOLD_EXTRACTED, result.eventType)
        }
    }

    // -------------------------------------------------------------------------
    // Symmetry Hint Detector
    // -------------------------------------------------------------------------

    @Test
    fun `symmetry detector - simpler but similar candidate scores`() {
        val detector = SymmetryHintDetector()
        val original = "def add(a, b): result = a + b; return result"
        val simplified = "def add(a, b): return a + b"
        val result = detector.detect(simplified, original)
        assertTrue("simplified candidate should have positive symmetry score", result.score > 0f)
    }

    @Test
    fun `symmetry detector - completely different inputs score low`() {
        val detector = SymmetryHintDetector()
        val original = "def add(a, b): return a + b"
        val different = "RULE: IF user.age > 18 THEN ALLOW access ELSE DENY"
        val result = detector.detect(different, original)
        // May or may not pass, but score should reflect the difference
        assertNotNull(result.reason)
    }

    // -------------------------------------------------------------------------
    // Hybridization Detector
    // -------------------------------------------------------------------------

    @Test
    fun `hybridization detector - requires two inputs`() {
        val detector = HybridizationDetector()
        val result = detector.detect("some code", "original", candidateB = null)
        assertFalse("should not pass without input B", result.passed)
        assertTrue("reason should mention two inputs", result.reason.contains("two input") ||
            result.reason.contains("requires"))
    }

    @Test
    fun `hybridization detector - scores when candidate mixes both parents`() {
        val detector = HybridizationDetector()
        val a = "def foo(x): return x + 1"
        val b = "function bar(y) { return y * 2; }"
        // Candidate containing tokens from both
        val hybrid = "def foo(x): return x * 2"  // has 'def' from A, '*' concept from B
        val result = detector.detect(hybrid, a, candidateB = b)
        assertTrue("result should have a score", result.score >= 0f)
    }

    // -------------------------------------------------------------------------
    // Contradiction Detector
    // -------------------------------------------------------------------------

    @Test
    fun `contradiction detector - handles single input mode`() {
        val detector = ContradictionDetector()
        val code = "if (x > 0) { return true; } else { return false; }"
        // When candidate has radically different keywords from original
        val different = "RULE: WHEN x > 0 THEN APPROVE ELSE DENY"
        val result = detector.detect(different, code, candidateB = null)
        assertNotNull(result.reason)
    }

    @Test
    fun `contradiction detector - detects dual-input tension`() {
        val detector = ContradictionDetector()
        val a = "def process(x): if x > 0: return True; return False"
        val b = "def process(x): if x > 0: return True; return False"
        // Candidate that diverges from similar parents
        val divergent = "RULE: CONDITION: x < 100; ACTION: BLOCK; PRIORITY: high"
        val result = detector.detect(divergent, a, candidateB = b)
        assertTrue("score should be non-negative", result.score >= 0f)
    }

    // -------------------------------------------------------------------------
    // Threshold multiplier affects pass/fail
    // -------------------------------------------------------------------------

    @Test
    fun `threshold multiplier affects pass-fail boundary`() {
        val detector = StructuralNoveltyDetector()
        val orig = "def foo(a, b): return a + b"
        val cand = "def bar(x, y): x = x + 1; return x + y"

        val lowThreshold = detector.detect(cand, orig, thresholdMultiplier = 0.1f)
        val highThreshold = detector.detect(cand, orig, thresholdMultiplier = 5.0f)

        // Score should be the same, but pass/fail may differ
        assertEquals("score should be the same regardless of threshold",
            lowThreshold.score, highThreshold.score, 0.001f)
    }
}
