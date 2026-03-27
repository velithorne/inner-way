package com.velithorne.vessel.renderer_seedpod

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import com.velithorne.vessel.physiology.PhysiologySnapshot
import com.velithorne.vessel.renderer.ParticleSystem
import com.velithorne.vessel.renderer.ParallaxController
import com.velithorne.vessel.renderer.RenderTuning
import com.velithorne.vessel.renderer.VesselAnimationController
import kotlin.math.sin
import kotlinx.coroutines.isActive

/**
 * Seed pod chamber — **only** [SeedPodPainter]. Does not call [com.velithorne.vessel.renderer.VesselScene] or [com.velithorne.vessel.renderer.VesselPainter].
 */
@Composable
fun SeedPodScene(
    physiology: PhysiologySnapshot,
    scene: SeedPodSceneState,
    selectedTarget: String?,
    gestureController: SeedPodGestureController,
    renderOffset: Offset,
    onSelectTarget: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tuning = remember { SeedPodTuning() }
    val legacyTuning = remember { RenderTuning() }
    val parallax = remember(legacyTuning) { ParallaxController(legacyTuning) }
    val particles = remember(legacyTuning) { ParticleSystem(legacyTuning) }
    val anim = remember { VesselAnimationController() }

    var frame by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameNanos { frame = it }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(scene, physiology.timestampMillis) {
                detectTransformGestures(panZoomLock = true) { _, pan, zoom, rotation ->
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    gestureController.applyTransform(
                        pan = pan,
                        zoomFactor = zoom,
                        rotationRad = rotation,
                        viewportW = w,
                        viewportH = h,
                    )
                }
            }
            .pointerInput(scene, physiology.timestampMillis, selectedTarget, renderOffset) {
                detectTapGestures(
                    onDoubleTap = {
                        val parallaxOff = parallax.step(
                            physiology.telemetry.orientationPitchDeg,
                            physiology.telemetry.orientationRollDeg,
                            physiology.species.mobility,
                        )
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        gestureController.requestResetNextFrame()
                        onSelectTarget(null)
                    },
                    onTap = { offset ->
                        val parallaxOff = parallax.step(
                            physiology.telemetry.orientationPitchDeg,
                            physiology.telemetry.orientationRollDeg,
                            physiology.species.mobility,
                        )
                        val hit = SeedPodHitTest.hitTarget(
                            tapCanvas = offset,
                            renderOffset = renderOffset,
                            camera = gestureController.camera,
                            tuning = tuning,
                            viewportW = size.width.toFloat(),
                            viewportH = size.height.toFloat(),
                            parallax = parallaxOff,
                            stressShiverDeg = scene.stressShiver * tuning.stressShiverDegrees,
                        )
                        if (hit == null) anim.resetSelectionFocus()
                        onSelectTarget(hit)
                    },
                )
            },
    ) {
        if (frame == 0L) return@Canvas
        val parallaxOff = parallax.step(
            physiology.telemetry.orientationPitchDeg,
            physiology.telemetry.orientationRollDeg,
            physiology.species.mobility,
        )
        if (gestureController.consumePendingReset(size.width, size.height, parallaxOff)) {
            anim.resetSelectionFocus()
        }
        gestureController.smoothTowardsTargets(size.width, size.height)
        anim.setSelectionFocusTarget(selectedTarget != null)
        anim.onFrame(frame)
        val dt = anim.deltaSeconds.coerceIn(0.001f, 0.05f).let { if (it <= 0f) 0.016f else it }

        val seed = (physiology.timestampMillis / 1000L).toInt() and 0x7fffffff
        particles.ensureInitialized(size.width, size.height, seed)
        val motes = particles.step(
            width = size.width,
            height = size.height,
            density = scene.particleDensity,
            neural = scene.neuralDrive,
            fever = scene.feverIntensity,
            dt = dt,
            globalPhase = anim.seconds,
            parallax = Offset(parallaxOff.x * 0.5f, parallaxOff.y * 0.5f),
        )

        SeedPodPainter.draw(
            scope = this,
            scene = scene,
            anim = anim,
            tuning = tuning,
            parallax = parallaxOff,
            particles = motes,
            camera = gestureController.camera,
            renderOffset = renderOffset,
        )

        val sleepVignette = scene.sleepDimming.coerceIn(0f, 1f)
        if (sleepVignette > 0.05f) {
            val a = sleepVignette * 0.4f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Black.copy(alpha = 0f), Color.Black.copy(alpha = a * 0.85f)),
                    startY = 0f,
                    endY = size.height,
                ),
                topLeft = Offset.Zero,
                size = size,
            )
        }

        if (physiology.telemetry.isCharging == true) {
            val s = anim.pulsePhase(0.45f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF4ECDC4).copy(alpha = 0f),
                        Color(0xFF4ECDC4).copy(alpha = 0.1f + sin(s) * 0.05f),
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.55f),
                    radius = size.minDimension * 0.42f,
                ),
                radius = size.minDimension * 0.42f,
                center = Offset(size.width * 0.5f, size.height * 0.55f),
            )
        }
    }
}
