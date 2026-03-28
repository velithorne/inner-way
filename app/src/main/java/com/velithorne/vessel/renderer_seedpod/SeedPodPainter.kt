package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.velithorne.vessel.renderer.ParticleDraw
import com.velithorne.vessel.renderer.VesselAnimationController
import com.velithorne.vessel.BuildConfig
import com.velithorne.vessel.renderer_genesis.GenesisRenderer
import com.velithorne.vessel.renderer_genesis.GenesisSceneState
import com.velithorne.vessel.renderer_genesis.GenesisViewportMapper

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
        val ga = scene.generatedAnatomy
        val bio = scene.biographyVisual
        val vm = scene.visibleMorphology
        val trace = scene.seedTrace
        val burial = scene.seedBurial
        val contourGeom = scene.contourGeometry
        val rerouteInt = scene.rerouteIntegration
        val gInf = vm.generatedTopologyInfluence.coerceIn(0f, 1f)
        val seedFallback = vm.fallbackSeedInfluence.coerceIn(0f, 1f)
        val phaseSec = anim.seconds + scene.animTimeSec
        val radii = SeedPodContourBuilder.radii(
            minDim = minDim,
            shellThickening = d.shellThickening,
            closedness = appearance.shellClosedness,
            tuning = tuning,
            branchStretchX = bv.contourStretchXMul * (ga?.shellRxMul ?: 1f),
            branchStretchY = bv.contourStretchYMul * (ga?.shellRyMul ?: 1f),
            shellThicknessMul = bv.shellThicknessMul,
        )
        val coreR = SeedPodContourBuilder.nucleusRadius(minDim, tuning) *
            (0.92f + appearance.nucleusBrightnessMul * 0.06f)
        val jForm = scene.juvenileForm
        val podDraw = pod + Offset(0f, (ga?.verticalSkew ?: 0f) * minDim * 0.06f) +
            JuvenileViewportMapper.bodyOffset(pod, minDim, jForm)
        val zoomMul = 1f + JuvenileViewportMapper.zoomBias(jForm)

        val genesisCanvas = scene.genesisBirthStage && ga != null && scene.genesisVisual.genesisRenderPathActive
        if (genesisCanvas) {
            val gPod = podDraw + GenesisViewportMapper.bodyOffset(minDim, scene.growthPressure)
            scope.translate(vc.x, vc.y) {
                rotate(
                    degrees = camera.rotationDeg + camera.tiltDeg * 0.35f + scene.stressShiver * tuning.stressShiverDegrees * 0.25f,
                    pivot = Offset.Zero,
                ) {
                    scale(
                        camera.zoom.coerceIn(tuning.minZoom, tuning.maxZoom) * zoomMul,
                        camera.zoom.coerceIn(tuning.minZoom, tuning.maxZoom) * zoomMul,
                        pivot = Offset.Zero,
                    ) {
                        translate(-vc.x + camera.panX, -vc.y + camera.panY) {
                            val podScope = this
                            SeedPodAtmospherePainter.drawBackdrop(podScope, w, h, parallax, palette, scene, tuning)
                            SeedPodAtmospherePainter.drawRearDepthFog(podScope, w, h, gPod, layers.rearAtmosphere, depth.rearDarkening, tuning)
                            SeedPodAtmospherePainter.drawVolumetricBand(podScope, w, h, parallax, 0f)
                            SeedPodAtmospherePainter.drawVolumetricBand(podScope, w, h, parallax, 1f)
                            SeedPodGlowPainter.drawSpotlight(podScope, w, h, gPod, appearance, tuning)
                            SeedPodShadowPainter.drawPodGroundShadow(
                                podScope,
                                gPod,
                                radii.shellRx * 0.85f,
                                radii.shellRy * 0.85f,
                                depth,
                                appearance,
                            )
                            val gScene = GenesisSceneState(
                                palette = palette,
                                visible = vm,
                                anatomy = ga,
                                contourGeometry = contourGeom,
                                biography = bio,
                                growthPressure = scene.growthPressure,
                                animTimeSec = scene.animTimeSec,
                            )
                            GenesisRenderer.draw(
                                scope = podScope,
                                scene = gScene,
                                pod = gPod,
                                minDim = minDim,
                                tuning = tuning,
                                phaseSec = phaseSec,
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
                                drawGenesisDebugOverlay(
                                    podScope, w, h, gPod, scene,
                                )
                            }
                        }
                    }
                }
            }
            return
        }

        scope.translate(vc.x, vc.y) {
            rotate(
                degrees = camera.rotationDeg + camera.tiltDeg * 0.35f + scene.stressShiver * tuning.stressShiverDegrees * 0.25f,
                pivot = Offset.Zero,
            ) {
                scale(
                    camera.zoom.coerceIn(tuning.minZoom, tuning.maxZoom) * zoomMul,
                    camera.zoom.coerceIn(tuning.minZoom, tuning.maxZoom) * zoomMul,
                    pivot = Offset.Zero,
                ) {
                    translate(-vc.x + camera.panX, -vc.y + camera.panY) {
                        val podScope = this

                        SeedPodAtmospherePainter.drawBackdrop(podScope, w, h, parallax, palette, scene, tuning)
                        SeedPodAtmospherePainter.drawRearDepthFog(podScope, w, h, podDraw, layers.rearAtmosphere, depth.rearDarkening, tuning)
                        SeedPodAtmospherePainter.drawVolumetricBand(podScope, w, h, parallax, 0f)
                        SeedPodAtmospherePainter.drawVolumetricBand(podScope, w, h, parallax, 1f)

                        SeedPodGlowPainter.drawSpotlight(podScope, w, h, podDraw, appearance, tuning)

                        SeedPodShadowPainter.drawPodGroundShadow(
                            podScope,
                            podDraw,
                            radii.shellRx,
                            radii.shellRy,
                            depth,
                            appearance,
                        )

                        SeedPodShellPainter.drawRear(
                            scope = podScope,
                            pod = podDraw,
                            rearOffset = layers.rearShell,
                            radii = radii,
                            palette = palette,
                            seedPalette = seedPal,
                            appearance = appearance,
                            depth = depth,
                            shellThickening = d.shellThickening,
                            tuning = tuning,
                            seedFallbackAlpha = seedFallback,
                            outerGhostScale = burial.outerShellGhostScale,
                            outerGhostAlphaMul = burial.outerShellGhostAlphaMul,
                        )

                        GeneratedContourPainter.draw(
                            scope = podScope,
                            pod = podDraw,
                            base = radii,
                            anatomy = ga,
                            visible = vm,
                            geometry = contourGeom,
                            paletteLine = palette.shellEdge,
                            phaseSec = phaseSec,
                        )

                        JuvenileContourPainter.draw(
                            scope = podScope,
                            pod = podDraw,
                            radii = radii,
                            palette = palette,
                            form = jForm,
                            phaseSec = phaseSec,
                        )

                        GeneratedShellPainter.draw(
                            scope = podScope,
                            pod = podDraw,
                            radii = radii,
                            palette = palette,
                            visible = vm,
                            phaseSec = phaseSec,
                        )

                        JuvenileRegionPainter.draw(
                            scope = podScope,
                            pod = podDraw,
                            radii = radii,
                            palette = palette,
                            form = jForm,
                        )

                        JuvenileDepthPainter.draw(
                            scope = podScope,
                            pod = podDraw,
                            radii = radii,
                            palette = palette,
                            depth = depth,
                            form = jForm,
                        )

                        SeedPodInnerVolumePainter.draw(
                            scope = podScope,
                            pod = podDraw,
                            layerOffset = layers.innerHaze,
                            radii = radii,
                            palette = palette,
                            appearance = appearance,
                            depth = depth,
                            tuning = tuning,
                            branch = bv,
                            anatomy = ga,
                            generatedInfluence = gInf,
                        )

                        SeedTracePainter.draw(
                            scope = podScope,
                            pod = podDraw,
                            minDim = minDim,
                            radii = radii,
                            palette = palette,
                            trace = trace,
                        )

                        GeneratedChamberPainter.draw(
                            scope = podScope,
                            pod = podDraw,
                            minDim = minDim,
                            radii = radii,
                            palette = palette,
                            visible = vm,
                            phaseSec = phaseSec,
                        )

                        JuvenileBasinPainter.draw(
                            scope = podScope,
                            pod = podDraw,
                            radii = radii,
                            palette = palette,
                            form = jForm,
                        )

                        RerouteIntegrationPainter.draw(
                            scope = podScope,
                            pod = podDraw,
                            radii = radii,
                            bio = bio,
                            visible = vm,
                            integration = rerouteInt,
                            palette = palette,
                            phaseSec = phaseSec,
                            pass = RerouteIntegrationPainter.ReroutePass.SUBSURFACE_BEHIND,
                        )

                        SeedPodNucleusPainter.draw(
                            scope = podScope,
                            pod = podDraw,
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
                            seedNucleusAlpha = seedFallback,
                        )

                        SeedPodOcclusionPainter.drawNucleusOcclusion(
                            podScope,
                            nucleusCenter = podDraw + layers.nucleus + depth.nucleusRecessOffset,
                            coreR = coreR * depth.nucleusBurialScale,
                            depth = depth,
                            appearance = appearance,
                            tuning = tuning,
                            seedNucleusAlpha = seedFallback,
                        )

                        GeneratedFrondPainter.draw(
                            scope = podScope,
                            pod = podDraw,
                            minDim = minDim,
                            buds = buds,
                            radii = radii,
                            palette = palette,
                            visible = vm,
                            phaseSec = phaseSec,
                        )

                        SeedPodBudPainter.draw(
                            scope = podScope,
                            pod = podDraw,
                            minDim = minDim,
                            buds = buds,
                            layers = layers,
                            radii = radii,
                            depth = depth,
                            lighting = lighting,
                            palette = palette,
                            tuning = tuning,
                            branch = bv,
                            stockLateralAlpha = seedFallback,
                        )

                        SeedPodBracingPainter.draw(
                            scope = podScope,
                            pod = podDraw,
                            radii = radii,
                            palette = palette,
                            branch = bv,
                            phaseSec = phaseSec,
                        )

                        JuvenilePlatePainter.draw(
                            scope = podScope,
                            pod = podDraw,
                            radii = radii,
                            palette = palette,
                            form = jForm,
                            phaseSec = phaseSec,
                        )

                        JuvenileFrondPainter.draw(
                            scope = podScope,
                            pod = podDraw,
                            radii = radii,
                            palette = palette,
                            form = jForm,
                            phaseSec = phaseSec,
                        )

                        JuvenileSupportPainter.draw(
                            scope = podScope,
                            pod = podDraw,
                            radii = radii,
                            palette = palette,
                            form = jForm,
                            phaseSec = phaseSec,
                        )

                        SeedPodShellPainter.drawFront(
                            scope = podScope,
                            pod = podDraw,
                            frontOffset = layers.frontShell,
                            radii = radii,
                            palette = palette,
                            seedPalette = seedPal,
                            appearance = appearance,
                            lighting = lighting,
                            shellThickening = d.shellThickening,
                            phaseSec = phaseSec,
                            closedness = appearance.shellClosedness,
                            tuning = tuning,
                            seedFallbackAlpha = seedFallback,
                            outerGhostScale = burial.outerShellGhostScale,
                            outerGhostAlphaMul = burial.outerShellGhostAlphaMul,
                        )

                        RerouteIntegrationPainter.draw(
                            scope = podScope,
                            pod = podDraw,
                            radii = radii,
                            bio = bio,
                            visible = vm,
                            integration = rerouteInt,
                            palette = palette,
                            phaseSec = phaseSec,
                            pass = RerouteIntegrationPainter.ReroutePass.THROUGH_SURFACE,
                        )

                        SeedPodGrowthFrontPainter.draw(
                            scope = podScope,
                            pod = podDraw,
                            layerOffset = layers.frontShell,
                            radii = radii,
                            palette = palette,
                            appearance = appearance,
                            phaseSec = phaseSec,
                            tuning = tuning,
                        )

                        SeedPodRimLightPainter.draw(
                            scope = podScope,
                            pod = podDraw,
                            rx = radii.shellRx,
                            ry = radii.shellRy,
                            layerOffset = layers.rimLight,
                            lighting = lighting,
                            palette = palette,
                            phaseSec = phaseSec,
                        )

                        GeneratedScarPainter.draw(
                            scope = podScope,
                            pod = podDraw,
                            radii = radii,
                            bio = bio,
                            visible = vm,
                            phaseSec = phaseSec,
                        )

                        SeedPodThermalPainter.draw(
                            scope = podScope,
                            pod = podDraw,
                            layerOffset = layers.frontShell,
                            radii = radii,
                            thermal = thermal,
                            lighting = lighting,
                            palette = palette,
                            phaseSec = phaseSec,
                            thermalVeilEmphasisMul = bv.thermalVeilEmphasisMul,
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
                            drawDebugOverlay(
                                podScope, w, h, pod, d.stage.name, vm, scene.fallbackMode,
                                scene.seedBurial, scene.contourGeometry,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun drawGenesisDebugOverlay(
        scope: DrawScope,
        w: Float,
        h: Float,
        pod: Offset,
        scene: SeedPodSceneState,
    ) {
        if (!BuildConfig.DEBUG) return
        val dbg = Color(0xFF66FFCC).copy(alpha = 0.55f)
        val gs = scene.genesisSnapshot?.state
        val lines = buildList {
            add("GENESIS active=${scene.genesisVisual.genesisRenderPathActive} legacyOff=${scene.genesisVisual.legacySeedScaffoldSuppressed}")
            add("fallbackSeed ${"%.0f".format(scene.visibleMorphology.fallbackSeedInfluence * 100f)}% genTopo ${"%.0f".format(scene.visibleMorphology.generatedTopologyInfluence * 100f)}%")
            gs?.let { g ->
                add("MVB ${g.minimumViableBody.name} n=${g.graph.nodes.size} e=${g.graph.edges.size}")
                add(g.genesisContourDriver)
                add("sym=${"%.2f".format(g.traits.symmetryBias)} coh=${"%.2f".format(g.traits.coherenceBias)} asym=${"%.2f".format(g.traits.latentAsymmetryBias)}")
            }
        }
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(220, 120, 255, 210)
            textSize = 22f
            isAntiAlias = true
        }
        var y = 28f
        scope.drawContext.canvas.nativeCanvas.apply {
            for (line in lines) {
                drawText(line, 14f, y, paint)
                y += 26f
            }
        }
        scope.drawLine(dbg, Offset(pod.x - 12f, pod.y), Offset(pod.x + 12f, pod.y), strokeWidth = 2f)
        scope.drawLine(dbg, Offset(pod.x, pod.y - 12f), Offset(pod.x, pod.y + 12f), strokeWidth = 2f)
    }

    private fun drawDebugOverlay(
        scope: DrawScope,
        w: Float,
        h: Float,
        pod: Offset,
        stageName: String,
        vm: com.velithorne.vessel.model.VisibleMorphologyState,
        mode: SeedPodFallbackMode,
        burial: SeedBurialState,
        contour: com.velithorne.vessel.model.ContourGeometryState,
    ) {
        val dbg = Color(0xFF00FFAA).copy(alpha = 0.5f)
        scope.drawRect(color = dbg.copy(alpha = 0.15f), style = androidx.compose.ui.graphics.drawscope.Stroke(2f), topLeft = Offset.Zero, size = Size(w, h))
        scope.drawLine(dbg, Offset(pod.x - 14f, pod.y), Offset(pod.x + 14f, pod.y), strokeWidth = 2f)
        scope.drawLine(dbg, Offset(pod.x, pod.y - 14f), Offset(pod.x, pod.y + 14f), strokeWidth = 2f)
        scope.drawCircle(dbg, 5f, pod)
    }
}
