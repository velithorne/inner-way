package com.velithorne.vessel.data.prefs

import android.content.Context
import com.velithorne.vessel.growth_seedpod.SeedPodDisplayState
import com.velithorne.vessel.growth_seedpod.SeedPodGrowthBudget
import com.velithorne.vessel.growth_seedpod.SeedPodGrowthStage
import com.velithorne.vessel.growth_seedpod.SeedPodGrowthState
import com.velithorne.vessel.progression.StructuralGrowthState

private const val PREFS = "velithorne_seedpod"
private const val KEY_HAS = "has"
private const val KEY_BUILD = "saved_version_code"

class SeedPodStateStore(context: Context) {
    private val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * Persist pod state. [buildVersionCode] must match current [com.velithorne.vessel.BuildConfig.VERSION_CODE].
     */
    fun save(state: SeedPodGrowthState, buildVersionCode: Int) {
        val d = state.display
        val b = state.budget
        p.edit()
            .putBoolean(KEY_HAS, true)
            .putInt(KEY_BUILD, buildVersionCode)
            .putInt("stage", d.stage.ordinal)
            .putFloat("crown", d.crownNub)
            .putFloat("latL", d.lateralBudLeft)
            .putFloat("latR", d.lateralBudRight)
            .putFloat("res", d.reserveBulb)
            .putFloat("shell", d.shellThickening)
            .putFloat("therm", d.thermalVeil)
            .putFloat("haze", d.tissueHaze)
            .putFloat("coh", d.podCoherence)
            .putLong("wall", d.lastWallClockMs)
            .putFloat("b_crown", b.crown)
            .putFloat("b_lat", b.lateral)
            .putFloat("b_res", b.reserve)
            .putFloat("b_shell", b.shell)
            .putFloat("b_therm", b.thermal)
            .putFloat("b_coh", b.coherence)
            .apply()
    }

    /**
     * Returns null if no state or if [currentBuildVersionCode] differs from stored pod snapshot
     * (new dev APK install → fresh pod).
     */
    fun loadOrNull(currentBuildVersionCode: Int): SeedPodGrowthState? {
        if (!p.getBoolean(KEY_HAS, false)) return null
        val savedBuild = p.getInt(KEY_BUILD, -1)
        if (savedBuild != currentBuildVersionCode) return null
        val st = SeedPodGrowthStage.entries.getOrNull(p.getInt("stage", 0)) ?: SeedPodGrowthStage.DORMANT_POD
        val d = SeedPodDisplayState(
            stage = st,
            crownNub = p.getFloat("crown", 0f),
            lateralBudLeft = p.getFloat("latL", 0f),
            lateralBudRight = p.getFloat("latR", 0f),
            reserveBulb = p.getFloat("res", 0f),
            shellThickening = p.getFloat("shell", 0f),
            thermalVeil = p.getFloat("therm", 0f),
            tissueHaze = p.getFloat("haze", 0f),
            podCoherence = p.getFloat("coh", 0.5f),
            lastWallClockMs = p.getLong("wall", System.currentTimeMillis()),
        )
        val b = SeedPodGrowthBudget(
            crown = p.getFloat("b_crown", 0f),
            lateral = p.getFloat("b_lat", 0f),
            reserve = p.getFloat("b_res", 0f),
            shell = p.getFloat("b_shell", 0f),
            thermal = p.getFloat("b_therm", 0f),
            coherence = p.getFloat("b_coh", 0f),
        )
        return SeedPodGrowthState(
            display = d,
            budget = b,
            structural = StructuralGrowthState.initial(d.lastWallClockMs),
        )
    }

    fun clear() {
        p.edit().clear().apply()
    }
}
