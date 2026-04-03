import * as THREE from 'three';

const pos = new THREE.Vector3();

export function setWorldCameraPosition(x: number, y: number, z: number): void {
  pos.set(x, y, z);
}

export function getWorldCameraPosition(): THREE.Vector3 {
  return pos.clone();
}
