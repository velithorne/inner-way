package com.aura.shell.personalization

import android.graphics.drawable.ColorDrawable
import com.aura.shell.command.AppResolutionEngine
import com.aura.shell.command.ResolutionOutcome
import com.aura.shell.model.LauncherAppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LearningRankBoostTest {

    private val ctx get() = RuntimeEnvironment.getApplication()
    private val icon = ColorDrawable(0)

    @Test
    fun repeatedMusicPrefersSpotifyAfterEnoughWeight() {
        val aliasStore = PersonalAliasStore(ctx).also { it.clearAll() }
        val learn = PreferenceLearningStore(ctx).also { it.clearAll() }
        repeat(4) {
            learn.record("music", "com.spotify.music", LearningSignal.DIRECT_LAUNCH)
        }
        val personal = PersonalResolutionContext(aliasStore, learn)

        val apps = listOf(
            LauncherAppInfo("com.spotify.music", "Spotify", icon),
            LauncherAppInfo("com.google.android.apps.youtube.music", "YouTube Music", icon),
        )
        val engine = AppResolutionEngine()
        val r = engine.resolve("music", apps, personal)
        assertTrue(r is ResolutionOutcome.SingleLaunch)
        assertEquals("com.spotify.music", (r as ResolutionOutcome.SingleLaunch).app.packageName)
    }

    @Test
    fun resetLearnedClearsWeights() {
        val learn = PreferenceLearningStore(ctx).also { it.clearAll() }
        learn.record("browser", "com.android.chrome", LearningSignal.SUGGESTION_PICK)
        learn.clearAll()
        assertTrue(learn.getAllLearnedRows().isEmpty())
    }
}
