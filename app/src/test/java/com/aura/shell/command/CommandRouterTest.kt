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
class CommandRouterTest {

    private val dummyIcon = ColorDrawable(0)

    private val apps = listOf(
        LauncherAppInfo("com.android.settings", "Settings", dummyIcon),
        LauncherAppInfo("com.android.camera2", "Camera", dummyIcon),
        LauncherAppInfo("com.whatsapp", "WhatsApp", dummyIcon),
    )

    private val router = CommandRouter()

    @Test
    fun openSettings_launches() {
        val d = router.route("open settings", apps, emptyList())
        assertTrue(d is CommandDispatch.LaunchApp && (d as CommandDispatch.LaunchApp).packageName == "com.android.settings")
    }

    @Test
    fun help_returnsHelp() {
        val d = router.route("help", apps, emptyList())
        assertTrue(d is CommandDispatch.ShowHelp)
    }

    @Test
    fun searchApps_findsMatches() {
        val d = router.route("search apps for camera", apps, emptyList())
        assertTrue(d is CommandDispatch.SearchMatches)
        val s = d as CommandDispatch.SearchMatches
        assertTrue(s.matches.any { it.packageName == "com.android.camera2" })
    }

    @Test
    fun showApps_opensDrawer() {
        val d = router.route("show apps", apps, emptyList())
        assertTrue(d is CommandDispatch.OpenAppDrawer)
    }
}
