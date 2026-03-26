package com.aura.shell.command

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WakePhraseProcessorTest {

    @Test
    fun auraOpenCamera_stripsWake() {
        val r = WakePhraseProcessor.classify("Aura, open camera", followUpMode = false)
        assertTrue(r is WakeProcessResult.Command)
        assertEquals("open camera", (r as WakeProcessResult.Command).text)
    }

    @Test
    fun auraOnly_isWakeOnly() {
        val r = WakePhraseProcessor.classify("Aura", followUpMode = false)
        assertTrue(r is WakeProcessResult.WakeOnly)
    }

    @Test
    fun followUp_acceptsPlainCommand() {
        val r = WakePhraseProcessor.classify("open chrome", followUpMode = true)
        assertTrue(r is WakeProcessResult.Command)
        assertEquals("open chrome", (r as WakeProcessResult.Command).text)
    }

    @Test
    fun noAura_prefix_isNotWake() {
        val r = WakePhraseProcessor.classify("open camera", followUpMode = false)
        assertTrue(r is WakeProcessResult.NotWake)
    }
}
