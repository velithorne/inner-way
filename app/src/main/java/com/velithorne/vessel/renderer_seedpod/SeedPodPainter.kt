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
 * Depth-aware orchestration: atmosphere → shell rear → inner volume → nucleus → occlusion → buds →
 * shell front → growth front → rim → thermal → particles → glass.
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
        val depth = scene.depthState
        val lighting = scene.lightingState
        val layers = SeedPodParallaxModel.compute(parallax, depth, tuning)

        val pod = SeedPodFraming.podCenterPx(w, h, parallax)
        val bv = scene.branchVisual
        val radii = SeedPodContourBuilder.radii(
            minDim = minDim,
            shellThickening = d.shellThickening,
            closedness = appearance.shellClosedness,
            tuning = tuning,
            branchStretchX = bv.contourStretchXMul,
            branchStretchY = bv.contourStretchYMul,
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

                        SeedPodAtmospherePainter.drawBackdrop(podScope, w, h, parallax, palette, scene, tuning)
                        SeedPodAtmospherePainter.drawRearDepthFog(podScope, w, h, pod, layers.rearAtmosphere, depth.rearDarkening, tuning)
                        SeedPodAtmospherePainter.drawVolumetricBand(podScope, w, h, parallax, 0f)
                        SeedPodAtmospherePainter.drawVolumetricBand(podScope, w, h, parallax, 1f)

                        SeedPodGlowPainter.drawSpotlight(podScope, w, h, pod, appearance, tuning)

                        SeedPodShadowPainter.drawPodGroundShadow(
                            podScope,
                            pod,
                            radii.shellRx,
                            radii.shellRy,
                            depth,
                            appearance,
                        )

                        SeedPodShellPainter.drawRear(
                            scope = podScope,
                            pod = pod,
                            rearOffset = layers.rearShell,
                            radii = radii,
                            palette = palette,
                            seedPalette = seedPal,
                            appearance = appearance,
                            depth = depth,
                            shellThickening = d.shellThickening,
                            tuning = tuning,
                        )

                        SeedPodInnerVolumePainter.draw(
                            scope = podScope,
                            pod = pod,
                            layerOffset = layers.innerHaze,
                            radii = radii,
                            palette = palette,
                            appearance = appearance,
                            depth = depth,
                            tuning = tuning,
                        )

                        SeedPodNucleusPainter.draw(
                            scope = podScope,
                            pod = pod,
                            layerOffset = layers.nucleus,
                            recess = depth.nucleusRecessOffset,
                            coreR = coreR,
                            depth = depth,
                            lighting = lighting,
                            palette = palette,
                            seedPalette = seedPal,
                            appearance = appearance,
                            anim = anim,
                            tuning = tuning,
                        )

                        SeedPodOcclusionPainter.drawNucleusOcclusion(
                            podScope,
                            nucleusCenter = pod + layers.nucleus + depth.nucleusRecessOffset,
                            coreR = coreR * depth.nucleusBurialScale,
                            depth = depth,
                            appearance = appearance,
                            tuning = tuning,
                        )

                        SeedPodBudPainter.draw(
                            scope = podScope,
                            pod = pod,
                            minDim = minDim,
                            buds = buds,
                            layers = layers,
                            radii = radii,
                            depth = depth,
                            lighting = lighting,
                            palette = palette,
                            tuning = tuning,
                            branch = bv,
                        )

                        SeedPodBracingPainter.draw(
                            scope = podScope,
                            pod = pod,
                            radii = radii,
                            palette = palette,
                            branch = bv,
                            phaseSec = anim.seconds,
                        )

                        SeedPodShellPainter.drawFront(
                            scope = podScope,
                            pod = pod,
                            frontOffset = layers.frontShell,
                            radii = radii,
                            palette = palette,
                            seedPalette = seedPal,
                            appearance = appearance,
                            lighting = lighting,
                            shellThickening = d.shellThickening,
                            phaseSec = anim.seconds,
                            closedness = appearance.shellClosedness,
                            tuning = tuning,
                        )

                        SeedPodGrowthFrontPainter.draw(
                            scope = podScope,
                            pod = pod,
                            layerOffset = layers.frontShell,
                            radii = radii,
                            palette = palette,
                            appearance = appearance,
                            phaseSec = anim.seconds,
                            tuning = tuning,
                        )

                        SeedPodRimLightPainter.draw(
                            scope = podScope,
                            pod = pod,
                            rx = radii.shellRx,
                            ry = radii.shellRy,
                            layerOffset = layers.rimLight,
                            lighting = lighting,
                            palette = palette,
                            phaseSec = anim.seconds,
                        )

                        SeedPodThermalPainter.draw(
                            scope = podScope,
                            pod = pod,
                            layerOffset = layers.frontShell,
                            radii = radii,
                            thermal = thermal,
                            lighting = lighting,
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
                            glassOffset = layers.glass,
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

    private fun drawDebugOverlay(scope: DrawScope, w: Float, h: Float, pod: Offset, stageName: String) {
        val dbg = Color(0xFF00FFAA).copy(alpha = 0.5f)
        scope.drawRect(color = dbg.copy(alpha = 0.15f), style = androidx.compose.ui.graphics.drawscope.Stroke(2f), topLeft = Offset.Zero, size = Size(w, h))
        scope.drawLine(dbg, Offset(pod.x - 14f, pod.y), Offset(pod.x + 14f, pod.y), strokeWidth = 2f)
        scope.drawLine(dbg, Offset(pod.x, pod.y - 14f), Offset(pod.x, pod.y + 14f), strokeWidth = 2f)
        scope.drawCircle(dbg, 5f, pod)
    }
}
