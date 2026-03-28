package com.velithorne.vessel.renderer_genesis

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.velithorne.vessel.renderer_seedpod.SeedPodContourBuilder
import com.velithorne.vessel.renderer_seedpod.SeedPodTuning

/**
 * Archetype-free birth path — fields and graph-derived forms only (no stock pod stack).
 */
object GenesisRenderer {

    fun draw(
        scope: DrawScope,
        scene: GenesisSceneState,
        pod: Offset,
        minDim: Float,
        tuning: SeedPodTuning,
        phaseSec: Float,
    ) {
        val ga = scene.anatomy ?: return
        val d = scene.visible
        val radii = SeedPodContourBuilder.radii(
            minDim = minDim,
            shellThickening = 0.25f,
            closedness = 0.35f,
            tuning = tuning,
            branchStretchX = ga.shellRxMul,
            branchStretchY = ga.shellRyMul,
            shellThicknessMul = 1f,
        )
        GenesisFieldPainter.draw(scope, pod, minDim, ga, scene.palette, phaseSec)
        GenesisShellPainter.draw(scope, pod, minDim, ga, scene.palette, phaseSec)
        GenesisCorePainter.draw(scope, pod, minDim, ga, scene.palette)
        GenesisReservePainter.draw(scope, pod, minDim, ga, scene.palette, phaseSec)
        GenesisGrowthCenterPainter.draw(scope, pod, minDim, ga, scene.palette, phaseSec)
        GenesisSignalPainter.draw(scope, pod, minDim, ga, scene.palette, phaseSec)
        GenesisContourPainter.draw(
            scope = scope,
            pod = pod,
            base = radii,
            anatomy = ga,
            visible = d,
            geometry = scene.contourGeometry,
            line = scene.palette.shellEdge,
            phaseSec = phaseSec,
        )
        GenesisScarPainter.draw(scope, pod, minDim, scene.biography, scene.palette, phaseSec)
    }
}
