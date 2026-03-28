package com.velithorne.vessel.renderer_seedpod

import com.velithorne.vessel.morphogenesis_core.CanonicalLifeEra

/** Polar sample counts — avoid obvious low-poly silhouettes. */
object ContourSampleSet {

    const val MINIMUM = 32
    const val DEFAULT = 40
    const val COMPLEX = 48

    fun forEra(era: CanonicalLifeEra): Int = when (era) {
        CanonicalLifeEra.SEED,
        CanonicalLifeEra.VEIL_STAGE,
        -> MINIMUM
        CanonicalLifeEra.CORE_ESTABLISHMENT -> DEFAULT
        CanonicalLifeEra.BRANCHING_THRESHOLD,
        CanonicalLifeEra.ADULTHOOD,
        -> COMPLEX
    }
}
