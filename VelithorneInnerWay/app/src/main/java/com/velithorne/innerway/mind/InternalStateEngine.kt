package com.velithorne.innerway.mind

import com.velithorne.innerway.perception.EnvironmentalContext

/**
 * Resolves internal state from fused perception. Phase 4 implements weighted rules.
 */
class InternalStateEngine {

    fun resolve(context: EnvironmentalContext): InternalState {
        // Phase 1: neutral substrate until body systems feed perception.
        return InternalState.CALM
    }
}
