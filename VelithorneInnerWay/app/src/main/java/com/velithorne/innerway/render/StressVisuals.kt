package com.velithorne.innerway.render

import com.velithorne.innerway.mind.InternalState

fun stressJitter(state: InternalState, nervousLoad: Float): Float {
    val base = when (state) {
        InternalState.STRESSED, InternalState.DEFENSIVE -> 0.04f
        InternalState.DISTURBED, InternalState.ALERT -> 0.06f
        else -> 0.01f
    }
    return base + nervousLoad * 0.05f
}
