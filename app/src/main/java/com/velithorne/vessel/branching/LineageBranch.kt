package com.velithorne.vessel.branching

/**
 * Morphology families — explainable, not random.
 *
 * @property briefDescription One-line intent for UI / tooltips.
 */
enum class LineageBranch(
    val displayName: String,
    val briefDescription: String,
) {
    THERMAL_SHELL(
        "Thermal Shell",
        "Stronger shell bands, thicker perimeter, heat veil — armored pod.",
    ),
    SIGNAL_FROND(
        "Signal Frond",
        "Expressive lateral budding, conductive fronds — communication-oriented.",
    ),
    CROWN_NEURAL(
        "Crown Neural",
        "Stronger crown bloom, layered top chamber — cortex-biased growth.",
    ),
    RESERVE_BASIN(
        "Reserve Basin",
        "Larger lower bulb, endurance reservoir — energy-storage emphasis.",
    ),
    ARCHIVE_CORE(
        "Archive Core",
        "Denser inner chambering — storage burden as internal mass.",
    ),
    MOTION_BRACED(
        "Motion Braced",
        "Tension bands, bracing, tauter form — stability-oriented.",
    ),
    BALANCED(
        "Balanced",
        "No extreme specialization — smooth moderate development.",
    ),
}
