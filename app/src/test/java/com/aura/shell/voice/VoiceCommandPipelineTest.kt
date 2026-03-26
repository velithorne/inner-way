package com.aura.shell.voice

import android.graphics.drawable.ColorDrawable
import com.aura.shell.command.CommandDispatch
import com.aura.shell.command.CommandRouter
import com.aura.shell.model.LauncherAppInfo
import kotlinx.coroutines.runBlocking
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
    fun voiceTranscript_sameAsTypedRoute() = runBlocking {
        val spoken = "open camera"
        val typed = router.route(spoken, apps, emptyList(), knowledgeRepository = null)
        val via = VoiceCommandPipeline.process(
            transcript = spoken,
            installedApps = apps,
            recentApps = emptyList(),
            appDrawerExpanded = false,
            router = router,
            knowledgeRepository = null,
        )
        assertTrue(typed == via)
    }

    @Test
    fun transcript_openCamera_dispatch() = runBlocking {
        val d = VoiceCommandPipeline.process(
            transcript = "open camera",
            installedApps = apps,
            recentApps = emptyList(),
            appDrawerExpanded = false,
            router = router,
            knowledgeRepository = null,
        )
        assertTrue(d is CommandDispatch.LaunchApp)
    }
}
