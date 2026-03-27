package com.velithorne.vessel.renderer_seedpod

/**
 * Logical depth ordering for 2.5D passes (lower = farther).
 */
object SeedPodDepthModel {

    const val LAYER_REAR_ATMOSPHERE = 0
    const val LAYER_REAR_SHELL = 1
    const val LAYER_REAR_HAZE = 2
    const val LAYER_NUCLEUS = 3
    const val LAYER_MID_CHAMBER = 4
    const val LAYER_BUDS = 5
    const val LAYER_FRONT_SHELL = 6
    const val LAYER_RIM = 7
    const val LAYER_THERMAL = 8
    const val LAYER_GROWTH_FRONT = 9
    const val LAYER_PARTICLES = 10
    const val LAYER_GLASS = 11
}
