package com.velithorne.innerway.render

import com.velithorne.innerway.mind.InternalState

fun sleepDimming(state: InternalState): Float {
    return when (state) {
        InternalState.RESTING, InternalState.DORMANT -> 0.45f
        InternalState.HUNGRY -> 0.55f
        else -> 0.85f
    }
}
