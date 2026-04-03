import * as THREE from 'three';

/** Fixed world anchors (blueprint) */
export const WORLD = {
  processor: new THREE.Vector3(-120, 0, -80),
  ramOcean: new THREE.Vector3(0, -60, 0),
  storage: new THREE.Vector3(0, 0, 120),
  sensors: new THREE.Vector3(80, 20, -40),
  networkSky: new THREE.Vector3(0, 120, 0),
  display: new THREE.Vector3(0, 40, -160),
} as const;

export const INITIAL_CAMERA_POS = new THREE.Vector3(0, 80, 200);
export const ENTRY_START_POS = new THREE.Vector3(0, 0, -600);
export const ORBITAL_CAMERA_POS = new THREE.Vector3(0, 300, 400);

/** Deterministic PRNG for layout */
export function mulberry32(seed: number): () => number {
  return () => {
    let t = (seed += 0x6d2b79f5);
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

export function hashString(s: string): number {
  let h = 2166136261;
  for (let i = 0; i < s.length; i++) {
    h ^= s.charCodeAt(i);
    h = Math.imul(h, 16777619);
  }
  return h >>> 0;
}
