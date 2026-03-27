package com.velithorne.vessel.growth_seedpod

import android.content.Context
import com.velithorne.vessel.BuildConfig
import com.velithorne.vessel.data.prefs.SeedPodStateStore
import com.velithorne.vessel.physiology.PhysiologySnapshot

/**
 * Owns seed-pod growth state + persistence. **Not** the legacy morphogenesis / vessel renderer path.
 *
 * **Build reset (fresh pod on new APK):** [SeedPodStateStore] stores [BuildConfig.VERSION_CODE] with
 * each save. On load, if the stored code differs from the current build, [loadOrNull] returns null
 * and this coordinator starts from [SeedPodGrowthEngine.initialDisplay] — see [SeedPodStateStore.loadOrNull].
 * (Morphogenesis prefs are cleared separately by [com.velithorne.vessel.growthtime.GrowthResetPolicy].)
 */
class SeedPodGrowthCoordinator(
    context: Context,
) {
    private val store = SeedPodStateStore(context)

    private var state: SeedPodGrowthState
    private var lastWallMs: Long = System.currentTimeMillis()

    init {
        // Fresh pod on new APK: prefs tied to version code in [SeedPodStateStore.loadOrNull].
        state = store.loadOrNull(BuildConfig.VERSION_CODE) ?: SeedPodGrowthState(
            display = SeedPodGrowthEngine.initialDisplay(),
            budget = SeedPodGrowthBudget(),
        )
        lastWallMs = state.display.lastWallClockMs
    }

    fun process(phys: PhysiologySnapshot): SeedPodGrowthState {
        val now = System.currentTimeMillis()
        val dtSec = ((now - lastWallMs) / 1000f).coerceIn(0.001f, 2f)
        lastWallMs = now
        state = SeedPodGrowthEngine.step(phys, state, dtSec)
        store.save(state, BuildConfig.VERSION_CODE)
        return state
    }

    fun current(): SeedPodGrowthState = state

    fun progressFraction(): Float {
        val d = state.display
        return (
            d.crownNub * 0.22f + d.lateralBudLeft * 0.18f + d.reserveBulb * 0.18f +
                d.tissueHaze * 0.22f + d.podCoherence * 0.2f
            ).coerceIn(0f, 1f)
    }
}
