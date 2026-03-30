import * as THREE from 'three';
import { EMOTION_MAP } from '../data/emotions';
import type { EmotionKey } from '../types';

export function emotionToColor(key: EmotionKey): THREE.Color {
  const def = EMOTION_MAP.get(key);
  return new THREE.Color(def?.color ?? '#ffffff');
}

export function emotionToHex(key: EmotionKey): string {
  const def = EMOTION_MAP.get(key);
  return def?.color ?? '#ffffff';
}

export function emotionGlowStrength(key: EmotionKey): number {
  const def = EMOTION_MAP.get(key);
  return def?.glowStrength ?? 0.5;
}

/**
 * Lerp between two hex colors by t (0-1).
 */
export function lerpColor(a: string, b: string, t: number): string {
  const ca = new THREE.Color(a);
  const cb = new THREE.Color(b);
  ca.lerp(cb, t);
  return `#${ca.getHexString()}`;
}

/**
 * Return a CSS rgba string with an alpha channel.
 */
export function hexWithAlpha(hex: string, alpha: number): string {
  const c = new THREE.Color(hex);
  const r = Math.round(c.r * 255);
  const g = Math.round(c.g * 255);
  const b = Math.round(c.b * 255);
  return `rgba(${r},${g},${b},${alpha})`;
}
