package com.aura.shell.command

import org.junit.Assert.assertTrue
import org.junit.Test

class FuzzyMatcherTest {

    @Test
    fun camra_typoStillScores() {
        val s = FuzzyMatcher.fuzzyTokenScore("camra", "camera")
        assertTrue(s >= 500)
    }

    @Test
    fun unrelatedStringsLowScore() {
        val s = FuzzyMatcher.fuzzyTokenScore("xyz", "camera")
        assertTrue(s < 300)
    }
}
