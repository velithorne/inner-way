package com.velithorne.vessel.renderer

/**
 * Timebase for procedural motion. Updated each frame from nanoseconds.
 * Keeps frequencies in Hz consistent across devices.
 */
class VesselAnimationController {
    var seconds: Float = 0f
        private set

    /** Last frame delta in seconds (capped). */
    var deltaSeconds: Float = 0f
        private set

    /** Eased 0..1 for selection vignette/ring (updated in [onFrame]). */
    var selectionFocus: Float = 0f
        private set

    private var lastNs: Long = -1L

    fun setSelectionFocusTarget(selected: Boolean) {
        selectionFocusTarget = if (selected) 1f else 0f
    }

    private var selectionFocusTarget = 0f

    /** Snap outline/vignette immediately (e.g. after reset view). */
    fun resetSelectionFocus() {
        selectionFocus = 0f
        selectionFocusTarget = 0f
    }

    fun onFrame(frameTimeNanos: Long) {
        if (lastNs < 0) {
            lastNs = frameTimeNanos
            deltaSeconds = 0f
            return
        }
        val dt = ((frameTimeNanos - lastNs).coerceAtMost(50_000_000L)) / 1_000_000_000f
        lastNs = frameTimeNanos
        deltaSeconds = dt
        seconds += dt
        val speed = if (selectionFocusTarget > 0.5f) 3.5f else 2.6f
        selectionFocus += (selectionFocusTarget - selectionFocus) * (speed * dt).coerceIn(0f, 1f)
        selectionFocus = selectionFocus.coerceIn(0f, 1f)
    }

    fun pulsePhase(hz: Float): Float = (seconds * hz * Math.PI.toFloat() * 2f)

    fun slowNoise(seed: Float): Float {
        val s = seconds + seed
        return kotlin.math.sin(s * 1.7).toFloat() * 0.5f +
            kotlin.math.sin(s * 2.9 + 1.1f).toFloat() * 0.25f
    }
}
