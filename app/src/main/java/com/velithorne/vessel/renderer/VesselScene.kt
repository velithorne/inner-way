package com.velithorne.vessel.renderer

import androidx.compose.foundation.Canvas
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
import com.velithorne.vessel.physiology.PhysiologySnapshot
import kotlin.math.sin
import kotlinx.coroutines.isActive

/**
 * Live 2.5D specimen viewport: combines mapping, parallax, particles, and draw passes.
 *
 * Future: record gesture hits per organ; evolution swaps [VesselLayout] presets.
 */
@Composable
fun VesselScene(
    physiology: PhysiologySnapshot,
    modifier: Modifier = Modifier,
) {
    val tuning = remember { RenderTuning() }
    val mapper = remember(tuning) { VesselRenderer(tuning = tuning) }
    val parallax = remember(tuning) { ParallaxController(tuning) }
    val particles = remember(tuning) { ParticleSystem(tuning) }
    val anim = remember { VesselAnimationController() }

    val scene = remember(physiology, mapper) { mapper.map(physiology) }
    var frame by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameNanos { frame = it }
        }
    }

    Canvas(modifier = modifier) {
        if (frame == 0L) return@Canvas
        anim.onFrame(frame)

        val parallaxOff = parallax.step(
            physiology.telemetry.orientationPitchDeg,
            physiology.telemetry.orientationRollDeg,
            physiology.species.mobility,
        )
        val seed = (physiology.timestampMillis / 1000L).toInt() and 0x7fffffff
        particles.ensureInitialized(size.width, size.height, seed)

        val dt = anim.deltaSeconds.coerceIn(0.001f, 0.05f).let { if (it <= 0f) 0.016f else it }
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

        ChamberEffects.drawChamberBackdrop(this, scene.fogDensity, scene.feverIntensity)
        ChamberEffects.drawGridSheen(this, 0.08f * (0.3f + scene.neuralDrive * 0.5f))

        // Rear particle pass (dimmer)
        for (p in motes) {
            if (p.depth < 0.5f) {
                drawCircle(
                    Color(0xFF445566).copy(alpha = p.alpha * 0.55f),
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
        )

        // Fore particle pass
        for (p in motes) {
            if (p.depth >= 0.5f) {
                val c = scene.accentBias.copy(alpha = p.alpha * (0.65f + scene.signalBrightness * 0.35f))
                drawCircle(c, p.radius, p.position)
            }
        }

        if (physiology.telemetry.isCharging == true) {
            val s = anim.pulsePhase(0.45f)
            ChamberEffects.drawGlassReflection(this)
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

        ChamberEffects.drawChamberFrame(this, 22f, scene.vitalityGlow)
        ChamberEffects.drawGlassReflection(this)

        val sleepVignette = scene.sleepDimming.coerceIn(0f, 1f)
        if (sleepVignette > 0.05f) {
            val vc = Offset(size.width / 2f, size.height / 2f - size.height * 0.05f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0f),
                        Color.Black.copy(alpha = sleepVignette * 0.5f),
                    ),
                    center = vc,
                    radius = size.maxDimension * 0.75f,
                ),
                radius = size.maxDimension * 0.9f,
                center = vc,
            )
        }
    }
}
