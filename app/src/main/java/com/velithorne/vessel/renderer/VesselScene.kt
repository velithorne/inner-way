package com.velithorne.vessel.renderer

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
import com.velithorne.vessel.physiology.OrganType
import com.velithorne.vessel.physiology.PhysiologySnapshot
import kotlin.math.sin
import kotlinx.coroutines.isActive

/**
 * Live 2.5D specimen: tap select/deselect, double-tap reset or focus, pinch/pan/rotate (clamped).
 */
@Composable
fun VesselScene(
    physiology: PhysiologySnapshot,
    scene: VesselSceneState,
    selectedOrgan: OrganType?,
    gestureController: VesselGestureController,
    renderOffset: Offset,
    onSelectOrgan: (OrganType?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tuning = remember { RenderTuning() }
    val parallax = remember(tuning) { ParallaxController(tuning) }
    val particles = remember(tuning) { ParticleSystem(tuning) }
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
            // Transform first in chain (inner); tap second (outer) so quick taps reach organs.
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
            .pointerInput(scene, physiology.timestampMillis, selectedOrgan, renderOffset) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        val parallaxOff = parallax.step(
                            physiology.telemetry.orientationPitchDeg,
                            physiology.telemetry.orientationRollDeg,
                            physiology.species.mobility,
                        )
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        if (selectedOrgan == null) {
                            gestureController.requestResetNextFrame()
                            onSelectOrgan(null)
                            return@detectTapGestures
                        }
                        gestureController.focusCameraOn(
                            organ = selectedOrgan,
                            scene = scene,
                            parallax = parallaxOff,
                            viewportW = w,
                            viewportH = h,
                        )
                    },
                    onTap = { offset ->
                        val parallaxOff = parallax.step(
                            physiology.telemetry.orientationPitchDeg,
                            physiology.telemetry.orientationRollDeg,
                            physiology.species.mobility,
                        )
                        val hit = VesselHitTest.hitOrgan(
                            tapCanvas = offset,
                            renderOffset = renderOffset,
                            scene = scene,
                            camera = gestureController.camera,
                            parallax = parallaxOff,
                            viewportW = size.width.toFloat(),
                            viewportH = size.height.toFloat(),
                            tuning = tuning,
                        )
                        if (hit == null) {
                            onSelectOrgan(null)
                        } else {
                            onSelectOrgan(hit)
                        }
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
        if (gestureController.consumePendingReset(scene, size.width, size.height, parallaxOff)) {
            anim.resetSelectionFocus()
        }
        gestureController.refreshBaseFramingIfNeutral(scene, size.width, size.height, parallaxOff)
        anim.setSelectionFocusTarget(selectedOrgan != null)
        anim.onFrame(frame)
        gestureController.smoothTowardsTargets(size.width, size.height)
        val dt = anim.deltaSeconds.coerceIn(0.001f, 0.05f).let { if (it <= 0f) 0.016f else it }

        val selectionDraw = VesselSelectionState(
            selectedOrgan = selectedOrgan,
            isSheetVisible = false,
            focusProgress = anim.selectionFocus,
        )

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

        VesselAtmospherePainter.drawBackdrop(this, scene, scene.palette, tuning, parallaxOff)
        ChamberEffects.drawGridSheen(this, 0.07f * (0.3f + scene.neuralDrive * 0.45f))
        VesselAtmospherePainter.drawScanSheen(this, scene.neuralDrive, tuning)

        val nearMul = tuning.chamberFogDepthNearMul
        for (p in motes) {
            if (p.depth < 0.5f) {
                drawCircle(
                    scene.palette.shellBase.copy(alpha = p.alpha * nearMul * 0.5f),
                    p.radius * 0.85f,
                    p.position,
                )
            }
        }

        VesselPainter.draw(
            scope = this,
            scene = scene,
            anim = anim,
            tuning = tuning,
            parallax = parallaxOff,
            particles = emptyList(),
            camera = gestureController.camera,
            selection = selectionDraw,
            renderOffset = renderOffset,
        )

        val farMul = tuning.chamberFogDepthFarMul
        for (p in motes) {
            if (p.depth >= 0.5f) {
                val c = scene.palette.accentSignal.copy(alpha = p.alpha * farMul * (0.65f + scene.signalBrightness * 0.35f))
                drawCircle(c, p.radius, p.position)
            }
        }

        if (physiology.telemetry.isCharging == true) {
            val s = anim.pulsePhase(0.45f)
            VesselGlassPainter.drawReflectionSweep(this, tuning, parallaxOff)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF4ECDC4).copy(alpha = 0f),
                        Color(0xFF4ECDC4).copy(alpha = 0.12f + sin(s) * 0.06f),
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.55f),
                    radius = size.minDimension * 0.45f,
                ),
                radius = size.minDimension * 0.45f,
                center = Offset(size.width * 0.5f, size.height * 0.55f),
            )
        }

        VesselGlassPainter.drawFrame(this, scene.vitalityGlow, tuning, parallaxOff)
        VesselGlassPainter.drawReflectionSweep(this, tuning, parallaxOff)

        val sleepVignette = scene.sleepDimming.coerceIn(0f, 1f)
        if (sleepVignette > 0.05f) {
            val a = sleepVignette * 0.45f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0f),
                        Color.Black.copy(alpha = a * 0.85f),
                    ),
                    startY = 0f,
                    endY = size.height,
                ),
                topLeft = Offset.Zero,
                size = size,
            )
        }
    }
}
