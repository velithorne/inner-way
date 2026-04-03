import * as THREE from 'three';

export function batteryLevelToEmissive(level: number): THREE.Color {
  const l = Math.max(0, Math.min(100, level));
  if (l >= 80) return new THREE.Color(0xffd700);
  if (l >= 50) return new THREE.Color(0xffb700);
  if (l >= 20) return new THREE.Color(0xff6600);
  if (l >= 10) return new THREE.Color(0xff2200);
  return new THREE.Color(0xcc0000);
}
