package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.Stroke
import com.velithorne.vessel.renderer.ParticleDraw
import com.velithorne.vessel.renderer.VesselAnimationController
import kotlin.math.cos
import kotlin.math.sin

/**
 * Seed pod canvas — **replaces** legacy [com.velithorne.vessel.renderer.VesselPainter] for the Vessel tab.
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
        renderOffset: Offset,
    ) {
        val w = scope.size.width
        val h = scope.size.height
        val vc = Offset(w / 2f, h / 2f)
        val minDim = minOf(w, h)
        val pulse = anim.pulsePhase(1.05f)
        val d = scene.podDisplay
        val palette = scene.palette

        scope.translate(renderOffset.x, renderOffset.y) {
            translate(vc.x, vc.y) {
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
                            val pod = SeedPodFraming.podCenterPx(w, h, parallax)

                            // Halo
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        palette.shellRimCool.copy(alpha = 0.14f * d.podCoherence),
                                        Color(0xFF000000).copy(alpha = 0f),
                                    ),
                                    center = pod,
                                    radius = minDim * 0.2f,
                                ),
                                radius = minDim * 0.22f,
                                center = pod,
                            )

                            // Translucent shell
                            val shellR = minDim * tuning.podShellRadiusMul * (1f + d.shellThickening * 0.12f)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        palette.shellBase.copy(alpha = 0.18f + d.shellThickening * 0.12f),
                                        palette.innerChamberShadow.copy(alpha = 0.12f),
                                        Color(0xFF000000).copy(alpha = 0f),
                                    ),
                                    center = pod,
                                    radius = shellR * 1.2f,
                                ),
                                radius = shellR,
                                center = pod,
                            )
                            drawCircle(
                                color = palette.shellEdge.copy(alpha = 0.22f + d.shellThickening * 0.15f),
                                radius = shellR,
                                center = pod,
                                style = Stroke(width = 1.8f + d.shellThickening * 2f),
                            )

                            // Nucleus core
                            val coreR = minDim * tuning.podCoreRadiusMul * (0.95f + sin(pulse) * 0.04f)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        palette.heartCore.copy(alpha = 0.42f),
                                        palette.cortexNode.copy(alpha = 0.2f),
                                        Color(0xFF000000).copy(alpha = 0f),
                                    ),
                                    center = pod,
                                    radius = coreR * 1.8f,
                                ),
                                radius = coreR * 1.35f,
                                center = pod,
                            )

                            // Latent crystalline geometry (hex dots)
                            for (i in 0 until 6) {
                                val ang = (i / 6f) * kotlin.math.PI.toFloat() * 2f
                                val ox = cos(ang) * coreR * 0.55f
                                val oy = sin(ang) * coreR * 0.55f
                                drawCircle(
                                    color = palette.shellRimCool.copy(alpha = 0.12f),
                                    radius = coreR * 0.12f,
                                    center = Offset(pod.x + ox, pod.y + oy),
                                )
                            }

                            SeedPodGrowthPainter.draw(
                                scope = podScope,
                                podCenter = pod,
                                minDim = minDim,
                                d = d,
                                palette = palette,
                                phaseSec = anim.seconds,
                            )

                            for (p in particles.sortedBy { it.depth }) {
                                val c = if (p.depth > 0.55f) {
                                    palette.accentSignal.copy(alpha = p.alpha * 0.5f)
                                } else {
                                    palette.shellBase.copy(alpha = p.alpha * 0.35f)
                                }
                                drawCircle(color = c, radius = p.radius, center = p.position)
                            }

                            // Glass rim (reuse style, no legacy scene)
                            val pad = 12f
                            val rect = Rect(Offset(pad, pad), Size(w - pad * 2, h - pad * 2))
                            drawRoundRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF7AB8C8).copy(alpha = 0.08f + scene.vitalityGlow * 0.1f),
                                        Color(0xFF7AB8C8).copy(alpha = 0.14f),
                                    ),
                                    start = rect.topLeft,
                                    end = Offset(rect.right, rect.bottom),
                                ),
                                topLeft = rect.topLeft,
                                size = rect.size,
                                cornerRadius = CornerRadius(22f, 22f),
                                style = Stroke(width = 2f),
                            )

                            if (tuning.showSeedPodDebug) {
                                drawDebugOverlay(this, w, h, pod, d.stage.name)
                            }
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
        scope.drawRect(
            color = palette.chamberMist.copy(alpha = fogA * 0.85f),
            topLeft = Offset(0f, h * 0.35f),
            size = Size(w, h * 0.65f),
        )
    }

    private fun drawDebugOverlay(scope: DrawScope, w: Float, h: Float, pod: Offset, stageName: String) {
        val dbg = Color(0xFF00FFAA).copy(alpha = 0.5f)
        scope.drawRect(color = dbg.copy(alpha = 0.15f), style = Stroke(2f), topLeft = Offset.Zero, size = Size(w, h))
        scope.drawLine(dbg, Offset(pod.x - 14f, pod.y), Offset(pod.x + 14f, pod.y), strokeWidth = 2f)
        scope.drawLine(dbg, Offset(pod.x, pod.y - 14f), Offset(pod.x, pod.y + 14f), strokeWidth = 2f)
        scope.drawCircle(dbg, 5f, pod)
        // Stage text omitted (would need native canvas); crosshair + chamber = enough for QA.
    }
}
