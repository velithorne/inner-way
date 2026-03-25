package com.aura.shell.voice

import android.graphics.drawable.ColorDrawable
import com.aura.shell.command.CommandDispatch
import com.aura.shell.command.CommandRouter
import com.aura.shell.model.LauncherAppInfo
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VoiceCommandPipelineTest {

    private val icon = ColorDrawable(0)
    private val apps = listOf(
        LauncherAppInfo("c", "Camera", icon),
        LauncherAppInfo("s", "Settings", icon),
    )

    private val router = CommandRouter()

    @Test
    fun voiceTranscript_sameAsTypedRoute() {
        val spoken = "open camera"
        val typed = router.route(spoken, apps, emptyList())
        val via = VoiceCommandPipeline.process(
            transcript = spoken,
            installedApps = apps,
            recentApps = emptyList(),
            appDrawerExpanded = false,
            router = router,
        )
        assertTrue(typed == via)
    }

    @Test
    fun transcript_openCamera_dispatch() {
        val d = VoiceCommandPipeline.process(
            transcript = "open camera",
            installedApps = apps,
            recentApps = emptyList(),
            appDrawerExpanded = false,
            router = router,
        )
        assertTrue(d is CommandDispatch.LaunchApp)
    }
}
