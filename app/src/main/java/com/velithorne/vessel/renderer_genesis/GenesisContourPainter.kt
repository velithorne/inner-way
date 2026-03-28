package com.velithorne.vessel.renderer_genesis

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.velithorne.vessel.model.GeneratedAnatomyState
import com.velithorne.vessel.model.VisibleMorphologyState
import com.velithorne.vessel.renderer_seedpod.ContourSampleSet
import com.velithorne.vessel.renderer_seedpod.GeneratedContourPainter
import com.velithorne.vessel.renderer_seedpod.SeedPodContourBuilder
import com.velithorne.vessel.model.ContourGeometryState

/**
 * Birth contour — delegates to field-derived polar silhouette (no stock oval assumption beyond sample closure).
 */
object GenesisContourPainter {
    fun draw(
        scope: androidx.compose.ui.graphics.drawscope.DrawScope,
        pod: Offset,
        base: SeedPodContourBuilder.PodRadii,
        anatomy: GeneratedAnatomyState?,
        visible: VisibleMorphologyState,
        geometry: ContourGeometryState,
        line: Color,
        phaseSec: Float,
    ) {
        GeneratedContourPainter.draw(
            scope = scope,
            pod = pod,
            base = base,
            anatomy = anatomy,
            visible = visible,
            geometry = geometry.copy(sampleCount = geometry.sampleCount.coerceAtLeast(ContourSampleSet.DEFAULT)),
            paletteLine = line,
            phaseSec = phaseSec,
        )
    }
}
