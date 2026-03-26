package com.aura.shell.command

import android.graphics.drawable.ColorDrawable
import com.aura.shell.model.LauncherAppInfo
import kotlinx.coroutines.runBlocking
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
        LauncherAppInfo("com.google.android.youtube", "YouTube", dummyIcon),
    )

    private val router = CommandRouter()

    @Test
    fun openSettings_launches() = runBlocking {
        val d = router.route("open settings", apps, emptyList(), knowledgeRepository = null)
        assertTrue(d is CommandDispatch.LaunchApp && (d as CommandDispatch.LaunchApp).packageName == "com.android.settings")
    }

    @Test
    fun opnCamra_typoGetsCamera() = runBlocking {
        val d = router.route("opn camra", apps, emptyList(), knowledgeRepository = null)
        assertTrue(d is CommandDispatch.LaunchApp || d is CommandDispatch.PickFromSuggestions)
    }

    @Test
    fun help_returnsHelp() = runBlocking {
        val d = router.route("help", apps, emptyList(), knowledgeRepository = null)
        assertTrue(d is CommandDispatch.ShowHelp)
    }

    @Test
    fun searchApps_findsMatches() = runBlocking {
        val d = router.route("search apps for camera", apps, emptyList(), knowledgeRepository = null)
        assertTrue(d is CommandDispatch.SearchMatches)
        val s = d as CommandDispatch.SearchMatches
        assertTrue(s.matches.any { it.packageName == "com.android.camera2" })
    }

    @Test
    fun showApps_opensDrawer() = runBlocking {
        val d = router.route("show apps", apps, emptyList(), knowledgeRepository = null)
        assertTrue(d is CommandDispatch.OpenAppDrawer)
    }

    @Test
    fun showMeApps_opensDrawer() = runBlocking {
        val d = router.route("show me apps", apps, emptyList(), knowledgeRepository = null)
        assertTrue(d is CommandDispatch.OpenAppDrawer)
    }

    @Test
    fun bareClose_whenDrawerExpanded_hidesDrawer() = runBlocking {
        val d = router.route("close", apps, emptyList(), appDrawerExpanded = true, knowledgeRepository = null)
        assertTrue(d is CommandDispatch.CloseAppDrawer)
    }

    @Test
    fun bareClose_whenDrawerPeek_goHome() = runBlocking {
        val d = router.route("close", apps, emptyList(), appDrawerExpanded = false, knowledgeRepository = null)
        assertTrue(d is CommandDispatch.GoHome)
    }

    @Test
    fun goHome_explicit() = runBlocking {
        val d = router.route("go home", apps, emptyList(), knowledgeRepository = null)
        assertTrue(d is CommandDispatch.GoHome)
    }

    @Test
    fun takeMeToChrome_routes() = runBlocking {
        val withChrome = apps + LauncherAppInfo("com.android.chrome", "Chrome", dummyIcon)
        val d = router.route("take me to chrome", withChrome, emptyList(), knowledgeRepository = null)
        assertTrue(d is CommandDispatch.LaunchApp || d is CommandDispatch.PickFromSuggestions)
    }

    @Test
    fun findBenchmarkNotes_routesToSearch() = runBlocking {
        val ctx = org.robolectric.RuntimeEnvironment.getApplication()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(ctx, com.aura.shell.knowledge.db.KnowledgeDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = com.aura.shell.knowledge.KnowledgeRepository(ctx, db)
        repo.insertNote("Benchmark", "phase 9 numbers here")
        val d = router.route("find benchmark notes", apps, emptyList(), knowledgeRepository = repo)
        assertTrue(d is CommandDispatch.KnowledgeSearchResults)
    }
}
