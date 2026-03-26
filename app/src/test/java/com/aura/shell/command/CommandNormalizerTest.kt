package com.aura.shell.command

import org.junit.Assert.assertEquals
import org.junit.Test

class CommandNormalizerTest {

    @Test
    fun takeMeToChrome_becomesOpenChrome() {
        val n = CommandNormalizer.normalize("take me to chrome")
        assertEquals("open chrome", n)
    }

    @Test
    fun showMeApps_opensDrawerPhrase() {
        val n = CommandNormalizer.normalize("show me apps")
        assertEquals("open apps", n)
    }

    @Test
    fun stripMy_fromTarget() {
        assertEquals("messages", CommandNormalizer.stripTargetFiller("my messages"))
    }
}
