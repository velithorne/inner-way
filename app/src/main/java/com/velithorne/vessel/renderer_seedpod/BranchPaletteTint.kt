package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.graphics.Color
import com.velithorne.vessel.model.BranchVisualState
import com.velithorne.vessel.model.VesselPaletteState

/**
 * Restrained palette shifts from branch affinity — same species family, different emphasis.
 */
object BranchPaletteTint {

    private val warmRim = Color(0xFFFF8A65)
    private val coolConductive = Color(0xFF4DD0E1)
    private val crownBloom = Color(0xFFB39DDB)
    private val reserveAmber = Color(0xFFFFB74D)

    fun apply(base: VesselPaletteState, branch: BranchVisualState): VesselPaletteState {
        val m = branch.visualExpressionMagnitude.coerceIn(0f, 1f) * (0.35f + branch.branchReadiness * 0.25f)
        if (m < 0.02f) return base
        fun blend(a: Color, b: Color, t: Float): Color {
            val w = t.coerceIn(0f, 0.45f)
            return Color(
                red = a.red + (b.red - a.red) * w,
                green = a.green + (b.green - a.green) * w,
                blue = a.blue + (b.blue - a.blue) * w,
                alpha = a.alpha,
            )
        }
        val wArm = branch.paletteWarmthBias.coerceIn(-0.2f, 0.35f)
        val wCool = branch.paletteCoolSideBias.coerceIn(-0.2f, 0.35f)
        val wCr = branch.paletteCrownTintBias.coerceIn(-0.1f, 0.35f)
        val wRes = branch.paletteReserveTintBias.coerceIn(-0.1f, 0.35f)
        return base.copy(
            shellEdge = blend(base.shellEdge, warmRim, m * wArm.coerceAtLeast(0f)),
            thermalEdge = blend(base.thermalEdge, warmRim, m * (wArm * 0.8f).coerceAtLeast(0f)),
            accentSignal = blend(base.accentSignal, coolConductive, m * wCool.coerceAtLeast(0f)),
            lungFrond = blend(base.lungFrond, coolConductive, m * wCool.coerceAtLeast(0f) * 0.85f),
            cortexNode = blend(base.cortexNode, crownBloom, m * wCr.coerceAtLeast(0f)),
            neuralPathway = blend(base.neuralPathway, crownBloom, m * wCr.coerceAtLeast(0f) * 0.7f),
            archiveDeep = blend(base.archiveDeep, reserveAmber, m * wRes.coerceAtLeast(0f) * 0.5f),
            heartCore = blend(base.heartCore, reserveAmber, m * wRes.coerceAtLeast(0f) * 0.55f),
        )
    }
}
