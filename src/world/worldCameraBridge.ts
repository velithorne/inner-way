import * as THREE from 'three';

const pos = new THREE.Vector3();
const quat = new THREE.Quaternion();
const prevPos = new THREE.Vector3();
const _tmp = new THREE.Vector3();
let lastTime = 0;
let speed = 0;
let hasPrev = false;

export function setWorldCameraPosition(x: number, y: number, z: number): void {
  const now = performance.now() / 1000;
  if (hasPrev && lastTime > 0) {
    const dt = Math.max(1e-4, now - lastTime);
    speed = prevPos.distanceTo(_tmp.set(x, y, z)) / dt;
  }
  prevPos.set(x, y, z);
  hasPrev = true;
  lastTime = now;
  pos.set(x, y, z);
}

export function getCameraSpeed(): number {
  return speed;
}

export function setWorldCameraQuaternion(x: number, y: number, z: number, w: number): void {
  quat.set(x, y, z, w);
}

export function getWorldCameraPosition(): THREE.Vector3 {
  return pos.clone();
}

export function getWorldCameraQuaternion(): THREE.Quaternion {
  return quat.clone();
}
