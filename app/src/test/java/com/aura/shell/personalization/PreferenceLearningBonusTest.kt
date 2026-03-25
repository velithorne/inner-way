package com.aura.shell.personalization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PreferenceLearningBonusTest {

    @Test
    fun bonusZeroBelowThreshold() {
        assertEquals(0, PersonalLearningBonus.bonusPoints(0))
        assertEquals(0, PersonalLearningBonus.bonusPoints(1))
    }

    @Test
    fun bonusCapped() {
        val huge = PersonalLearningBonus.bonusPoints(100)
        assertTrue(huge <= PersonalLearningBonus.MAX_BONUS_POINTS)
    }
}
