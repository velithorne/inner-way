package com.aura.shell.command

import android.graphics.drawable.ColorDrawable
import com.aura.shell.model.LauncherAppInfo
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppResolutionEngineTest {

    private val icon = ColorDrawable(0)
    private val apps = listOf(
        LauncherAppInfo("a", "Camera", icon),
        LauncherAppInfo("b", "Settings", icon),
        LauncherAppInfo("c", "YouTube", icon),
    )

    private val engine = AppResolutionEngine()

    @Test
    fun typoSettings_resolvesOrSuggests() {
        val o = engine.resolve("setings", apps)
        assertTrue(o is ResolutionOutcome.SingleLaunch || o is ResolutionOutcome.Suggest)
    }

    @Test
    fun yt_showsMultipleOrYoutube() {
        val o = engine.resolve("yt", apps)
        assertTrue(
            o is ResolutionOutcome.Suggest ||
                (o is ResolutionOutcome.SingleLaunch && (o as ResolutionOutcome.SingleLaunch).app.label.contains("YouTube", ignoreCase = true)),
        )
    }

    @Test
    fun messagesAlias_prefersExactMessagesLabelWhenUnique() {
        val messagingApps = listOf(
            LauncherAppInfo("com.google.android.apps.messaging", "Messages", icon),
            LauncherAppInfo("com.facebook.orca", "Messenger", icon),
            LauncherAppInfo("com.example.sms", "SMS Organizer", icon),
        )
        val o = engine.resolve("messages", messagingApps)
        assertTrue(o is ResolutionOutcome.SingleLaunch)
        assertTrue((o as ResolutionOutcome.SingleLaunch).app.label.equals("Messages", ignoreCase = true))
    }
}
