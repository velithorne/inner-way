package com.falcor.civilization.engine.analysis

import org.junit.Assert.*
import org.junit.Test

class BenjaminiHochbergTest {

    @Test
    fun emptyListReturnsEmpty() {
        val result = BenjaminiHochberg.correct(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun singlePValue() {
        val result = BenjaminiHochberg.correct(listOf(0.01))
        assertEquals(1, result.size)
        assertTrue(result[0] <= 0.05)
    }

    @Test
    fun multiplePValuesCorrected() {
        val pValues = listOf(0.001, 0.01, 0.05, 0.1)
        val corrected = BenjaminiHochberg.correct(pValues, 0.05)
        assertEquals(4, corrected.size)
        assertTrue(corrected[0] < corrected[1])
        assertTrue(corrected[1] < corrected[2])
    }

    @Test
    fun rejectNull() {
        val corrected = listOf(0.01, 0.06, 0.03)
        val reject = BenjaminiHochberg.rejectNull(corrected, 0.05)
        assertEquals(listOf(true, false, true), reject)
    }
}
