package com.velithorne.vessel.genesis

import com.velithorne.vessel.BuildConfig

object BirthExplainer {
    fun minimumViableLabel(m: MinimumViableBody): String = when (m) {
        MinimumViableBody.CORE_RESERVE_SPARK -> "Core + reserve spark — shell not yet closed"
        MinimumViableBody.RESERVE_FIRST -> "Reserve-first genesis"
        MinimumViableBody.CROWN_FIRST -> "Crown-first genesis"
        MinimumViableBody.SHELL_PRESSURE_FIRST -> "Shell pressure nucleation"
        MinimumViableBody.SIGNAL_LATERAL_SINGLE -> "Single lateral signal root"
        MinimumViableBody.ASYMMETRY_FIRST -> "Asymmetry-first genesis"
        MinimumViableBody.MIXED_COHERENCE -> "Mixed coherent birth"
    }

    fun hiddenTraitHints(traits: HiddenSeedTraits): List<String> {
        if (!BuildConfig.DEBUG) return emptyList()
        return buildList {
            add("sym ${"%.2f".format(traits.symmetryBias)} · dens ${"%.2f".format(traits.densityBias)} · shell ${"%.2f".format(traits.shellBias)}")
            add("asym ${"%.2f".format(traits.latentAsymmetryBias)} · coh ${"%.2f".format(traits.coherenceBias)}")
        }
    }
}
