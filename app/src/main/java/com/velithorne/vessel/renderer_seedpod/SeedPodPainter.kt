package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.velithorne.vessel.renderer.ParticleDraw
import com.velithorne.vessel.renderer.VesselAnimationController

/**
 * Seed pod canvas orchestration — **replaces** legacy [com.velithorne.vessel.renderer.VesselPainter] for the Vessel tab.
 */
object SeedPodPainter {

    fun draw(
        scope: DrawScope,
        scene: SeedPodSceneState,
        anim: VesselAnimationController,
        tuning: SeedPodTuning,
        parallax: Offset,
        particles: List<ParticleDraw>,
        camera: SeedPodCameraState,
    ) {
        val w = scope.size.width
        val h = scope.size.height
        val vc = Offset(w / 2f, h / 2f)
        val minDim = minOf(w, h)
        val d = scene.podDisplay
        val palette = scene.palette
        val seedPal = scene.seedPodPalette
        val appearance = scene.appearance
        val buds = scene.buds
        val thermal = scene.thermal

        val pod = SeedPodFraming.podCenterPx(w, h, parallax)
        val radii = SeedPodContourBuilder.radii(
            minDim = minDim,
            shellThickening = d.shellThickening,
            closedness = appearance.shellClosedness,
            tuning = tuning,
        )
        val coreR = SeedPodContourBuilder.nucleusRadius(minDim, tuning) *
            (0.92f + appearance.nucleusBrightnessMul * 0.06f)

        scope.translate(vc.x, vc.y) {
            rotate(
                degrees = camera.rotationDeg + camera.tiltDeg * 0.35f + scene.stressShiver * tuning.stressShiverDegrees * 0.25f,
                pivot = Offset.Zero,
            ) {
                scale(
                    camera.zoom.coerceIn(tuning.minZoom, tuning.maxZoom),
                    camera.zoom.coerceIn(tuning.minZoom, tuning.maxZoom),
                    pivot = Offset.Zero,
                ) {
                    translate(-vc.x + camera.panX, -vc.y + camera.panY) {
                        val podScope = this
                        drawChamberBackdrop(podScope, w, h, parallax, palette, scene)
                        SeedPodGlowPainter.drawSpotlight(podScope, w, h, pod, appearance, tuning)

                        // Shell / membrane (behind core)
                        SeedPodShellPainter.draw(
                            scope = podScope,
                            pod = pod,
                            radii = radii,
                            palette = palette,
                            seedPalette = seedPal,
                            appearance = appearance,
                            shellThickening = d.shellThickening,
                            phaseSec = anim.seconds,
                            tuning = tuning,
                        )

                        SeedPodGlowPainter.drawInnerChamberHaze(
                            scope = podScope,
                            pod = pod,
                            radii = radii,
                            palette = palette,
                            appearance = appearance,
                            tuning = tuning,
                        )

                        SeedPodCorePainter.draw(
                            scope = podScope,
                            pod = pod,
                            coreR = coreR,
                            palette = palette,
                            seedPalette = seedPal,
                            appearance = appearance,
                            anim = anim,
                            tuning = tuning,
                        )

                        SeedPodBudPainter.draw(
                            scope = podScope,
                            pod = pod,
                            minDim = minDim,
                            buds = buds,
                            radii = radii,
                            palette = palette,
                            tuning = tuning,
                        )

                        SeedPodGrowthFrontPainter.draw(
                            scope = podScope,
                            pod = pod,
                            radii = radii,
                            palette = palette,
                            appearance = appearance,
                            phaseSec = anim.seconds,
                            tuning = tuning,
                        )

                        SeedPodThermalPainter.draw(
                            scope = podScope,
                            pod = pod,
                            radii = radii,
                            thermal = thermal,
                            palette = palette,
                            phaseSec = anim.seconds,
                        )

                        val pScale = appearance.particleScale
                        for (p in particles.sortedBy { it.depth }) {
                            val c = if (p.depth > 0.55f) {
                                palette.accentSignal.copy(alpha = p.alpha * 0.5f * pScale)
                            } else {
                                palette.shellBase.copy(alpha = p.alpha * 0.35f * pScale)
                            }
                            drawCircle(color = c, radius = p.radius, center = p.position)
                        }

                        SeedPodGlassPainter.drawFrame(
                            scope = podScope,
                            w = w,
                            h = h,
                            appearance = appearance,
                            vitalityHint = scene.vitalityGlow.coerceIn(0f, 1f),
                        )

                        if (tuning.showSeedPodDebug) {
                            drawDebugOverlay(podScope, w, h, pod, d.stage.name)
                        }
                    }
                }
            }
        }
    }

    private fun drawChamberBackdrop(
        scope: DrawScope,
        w: Float,
        h: Float,
        parallax: Offset,
        palette: com.velithorne.vessel.model.VesselPaletteState,
        scene: SeedPodSceneState,
    ) {
        val ox = parallax.x * 0.06f
        val oy = parallax.y * 0.05f
        scope.drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF101828), Color(0xFF080C14), Color(0xFF030508)),
                startY = oy,
                endY = h + oy,
            ),
            topLeft = Offset.Zero,
            size = Size(w, h),
        )
        val fogA = (0.05f + scene.particleDensity * 0.12f + scene.feverIntensity * 0.06f).coerceIn(0.04f, 0.22f)
        val mist = palette.chamberMist
        scope.drawRect(
            brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to mist.copy(alpha = 0f),
                    0.28f to mist.copy(alpha = fogA * 0.12f),
                    0.55f to mist.copy(alpha = fogA * 0.45f),
                    1f to mist.copy(alpha = fogA * 0.85f),
                ),
                startY = 0f,
                endY = h,
            ),
            topLeft = Offset.Zero,
            size = Size(w, h),
        )
    }

    private fun drawDebugOverlay(scope: DrawScope, w: Float, h: Float, pod: Offset, stageName: String) {
        val dbg = Color(0xFF00FFAA).copy(alpha = 0.5f)
        scope.drawRect(color = dbg.copy(alpha = 0.15f), style = androidx.compose.ui.graphics.drawscope.Stroke(2f), topLeft = Offset.Zero, size = Size(w, h))
        scope.drawLine(dbg, Offset(pod.x - 14f, pod.y), Offset(pod.x + 14f, pod.y), strokeWidth = 2f)
        scope.drawLine(dbg, Offset(pod.x, pod.y - 14f), Offset(pod.x, pod.y + 14f), strokeWidth = 2f)
        scope.drawCircle(dbg, 5f, pod)
    }
}
