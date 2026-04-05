package com.velithorne.innerway.mind

/**
 * How the organism is currently reading the habitat for growth (debug / analytics).
 */
enum class TerritoryPreferenceMode {
    BALANCED,
    SAFE_CORE,
    FRONTIER_PROBE,
    CAUTIOUS_RECONNECT,
    ROOT_AND_REST,
    SHELTER_SEEK,
    TOUCH_CURIOUS,
    TOUCH_AVOID,
}
