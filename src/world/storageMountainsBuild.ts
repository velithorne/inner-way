import * as THREE from 'three';

import { WORLD, mulberry32 } from './worldConstants';

export type StorageMountainsHandles = {
  group: THREE.Group;
  peaks: THREE.Mesh[];
  dispose: () => void;
};

export function createStorageMountains(): StorageMountainsHandles {
  const group = new THREE.Group();
  group.position.copy(WORLD.storage);
  const rng = mulberry32(99);
  const peaks: THREE.Mesh[] = [];
  const matRock = new THREE.MeshStandardMaterial({
    color: 0x1a1033,
    emissive: 0x110022,
    emissiveIntensity: 0.15,
    roughness: 0.9,
    metalness: 0.1,
  });

  for (let i = 0; i < 15; i++) {
    const r = 4 + rng() * 10;
    const h = 20 + rng() * 25;
    const geo = new THREE.ConeGeometry(r, h, 8);
    const mesh = new THREE.Mesh(geo, matRock);
    mesh.position.set((rng() - 0.5) * 280, h / 2, (rng() - 0.5) * 60);
    group.add(mesh);
    peaks.push(mesh);
  }

  return {
    group,
    peaks,
    dispose: () => {
      peaks.forEach((p) => {
        p.geometry.dispose();
      });
      matRock.dispose();
    },
  };
}

export function updateStorageMountains(h: StorageMountainsHandles, usedFrac: number): void {
  const scale = 0.4 + usedFrac * 0.9;
  h.peaks.forEach((p, i) => {
    p.scale.setScalar(scale + (i % 5) * 0.02);
    const mat = p.material as THREE.MeshStandardMaterial;
    const snow = usedFrac < 0.7 ? 0.08 : 0;
    mat.emissive.setRGB(0.05 + snow, 0.02 + snow, 0.08 + snow);
  });
}
