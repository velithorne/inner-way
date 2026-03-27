package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.max

/**
 * Vesica / chrysalis silhouette: elliptical outer shell radii from min dimension and growth state.
 */
object SeedPodContourBuilder {

    data class PodRadii(
        val shellRx: Float,
        val shellRy: Float,
        val bandOuterRx: Float,
        val bandOuterRy: Float,
        val bandMidRx: Float,
        val bandMidRy: Float,
        val innerChamberRx: Float,
        val innerChamberRy: Float,
    )

    fun radii(
        minDim: Float,
        shellThickening: Float,
        closedness: Float,
        tuning: SeedPodTuning,
    ): PodRadii {
        val st = shellThickening.coerceIn(0f, 1f)
        val cl = closedness.coerceIn(0f, 1f)
        val baseR = minDim * tuning.podShellRadiusMul * (1f + st * 0.14f)
        val squeeze = 0.92f + cl * 0.08f
        val shellRx = baseR * tuning.shellVesicaStretchX * squeeze
        val shellRy = baseR * tuning.shellVesicaStretchY * (1f - cl * 0.06f)
        val bandOuterRx = shellRx * tuning.shellBandOuterMul
        val bandOuterRy = shellRy * tuning.shellBandOuterMul
        val bandMidRx = shellRx * tuning.shellBandMidMul
        val bandMidRy = shellRy * tuning.shellBandMidMul
        val innerRx = shellRx * tuning.shellBandInnerMul * (0.96f + st * 0.04f)
        val innerRy = shellRy * tuning.shellBandInnerMul * (0.96f + st * 0.04f)
        return PodRadii(
            shellRx = shellRx,
            shellRy = shellRy,
            bandOuterRx = bandOuterRx,
            bandOuterRy = bandOuterRy,
            bandMidRx = bandMidRx,
            bandMidRy = bandMidRy,
            innerChamberRx = max(innerRx, minDim * 0.04f),
            innerChamberRy = max(innerRy, minDim * 0.04f),
        )
    }

    fun nucleusRadius(minDim: Float, tuning: SeedPodTuning): Float =
        minDim * tuning.podCoreRadiusMul
}
