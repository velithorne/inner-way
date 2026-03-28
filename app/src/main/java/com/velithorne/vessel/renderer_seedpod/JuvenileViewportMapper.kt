package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.geometry.Offset
import com.velithorne.vessel.juvenile_form.JuvenileFormState

/**
 * Shifts specimen slightly so juvenile overflow reads against chamber viewport.
 */
object JuvenileViewportMapper {

    fun bodyOffset(pod: Offset, minDim: Float, form: JuvenileFormState): Offset {
        if (!form.active) return Offset.Zero
        val o = form.traits.viewportOverflowHint
        return Offset(0f, -minDim * 0.02f * o)
    }

    fun zoomBias(form: JuvenileFormState): Float {
        if (!form.active) return 0f
        return -form.traits.viewportOverflowHint * 0.06f
    }
}
