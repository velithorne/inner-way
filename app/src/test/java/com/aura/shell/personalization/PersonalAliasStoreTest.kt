package com.aura.shell.personalization

import android.graphics.drawable.ColorDrawable
import com.aura.shell.command.AppResolutionEngine
import com.aura.shell.model.LauncherAppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PersonalAliasStoreTest {

    private val ctx get() = RuntimeEnvironment.getApplication()
    private val icon = ColorDrawable(0)

    @Test
    fun setAndLookup() {
        val store = PersonalAliasStore(ctx)
        store.clearAll()
        store.setAlias("vids", "com.youtube.app")
        assertEquals("com.youtube.app", store.getPackageForAlias("vids"))
        assertEquals("com.youtube.app", store.getPackageForAlias("  VIDS  "))
    }

    @Test
    fun removeAlias() {
        val store = PersonalAliasStore(ctx)
        store.setAlias("tunes", "com.spotify.music")
        store.removeAlias("tunes")
        assertNull(store.getPackageForAlias("tunes"))
    }

    @Test
    fun userAliasOverridesLearned_boostStillSecondaryToExact() {
        val aliasStore = PersonalAliasStore(ctx).also { it.clearAll() }
        val learn = PreferenceLearningStore(ctx).also { it.clearAll() }
        learn.record("music", "com.google.android.music", LearningSignal.SUGGESTION_PICK)
        learn.record("music", "com.google.android.music", LearningSignal.SUGGESTION_PICK)
        val personal = PersonalResolutionContext(aliasStore, learn)
        aliasStore.setAlias("music", "com.spotify.music")

        val apps = listOf(
            LauncherAppInfo("com.spotify.music", "Spotify", icon),
            LauncherAppInfo("com.google.android.music", "YouTube Music", icon),
        )
        val engine = AppResolutionEngine()
        val r = engine.resolve("music", apps, personal)
        require(r is com.aura.shell.command.ResolutionOutcome.SingleLaunch)
        assertEquals("com.spotify.music", r.app.packageName)
    }
}
