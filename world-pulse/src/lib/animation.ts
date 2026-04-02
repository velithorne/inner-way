/**
 * Easing functions for pulse animations.
 */

export function easeOutCubic(t: number): number {
  return 1 - Math.pow(1 - t, 3);
}

export function easeInOutSine(t: number): number {
  return -(Math.cos(Math.PI * t) - 1) / 2;
}

export function easeOutExpo(t: number): number {
  return t === 1 ? 1 : 1 - Math.pow(2, -10 * t);
}

/**
 * Compute normalized pulse progress (0 → 1) given startTime and duration.
 */
export function pulseProgress(startTime: number, duration: number, now = Date.now()): number {
  return Math.min(1, (now - startTime) / duration);
}

/**
 * Opacity envelope: fade in fast, hold, then fade out.
 */
export function pulseOpacity(progress: number): number {
  if (progress < 0.1) return progress / 0.1;
  if (progress > 0.7) return 1 - (progress - 0.7) / 0.3;
  return 1;
}

/**
 * Ring scale: starts at 0, expands outward.
 */
export function ringScale(progress: number, maxScale = 3.5): number {
  return easeOutExpo(progress) * maxScale;
}
