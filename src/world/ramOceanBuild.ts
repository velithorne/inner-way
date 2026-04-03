import * as THREE from 'three';

import { WORLD, hashString, mulberry32 } from './worldConstants';

export type RamOceanHandles = {
  group: THREE.Group;
  mesh: THREE.Mesh;
  positions: Float32Array;
  baseY: Float32Array;
  islands: THREE.Mesh[];
  dispose: () => void;
};

export function createRamOcean(): RamOceanHandles {
  const group = new THREE.Group();
  group.position.copy(WORLD.ramOcean);

  const segs = 40;
  const plane = new THREE.PlaneGeometry(400, 400, segs, segs);
  plane.rotateX(-Math.PI / 2);

  const pos = plane.attributes.position.array as Float32Array;
  const baseY = new Float32Array(pos.length / 3);
  for (let i = 0; i < pos.length / 3; i++) {
    baseY[i] = pos[i * 3 + 1];
  }
  // horizontal XZ plane, local Y = ripple height

  const mat = new THREE.MeshStandardMaterial({
    color: 0x001833,
    emissive: 0x001833,
    emissiveIntensity: 0.4,
    transparent: true,
    opacity: 0.85,
    metalness: 0.8,
    roughness: 0.2,
    side: THREE.DoubleSide,
  });
  const mesh = new THREE.Mesh(plane, mat);
  group.add(mesh);

  return {
    group,
    mesh,
    positions: pos,
    baseY,
    islands: [],
    dispose: () => {
      plane.dispose();
      mat.dispose();
    },
  };
}

function islandPos(seed: number): THREE.Vector3 {
  const r = mulberry32(seed);
  return new THREE.Vector3((r() - 0.5) * 160, 0, (r() - 0.5) * 160);
}

export function ensureIslands(h: RamOceanHandles, apps: { packageName: string; appName: string; memoryBytes: number }[]): void {
  while (h.islands.length) {
    const m = h.islands.pop()!;
    h.group.remove(m);
    m.geometry.dispose();
    (m.material as THREE.Material).dispose();
  }

  for (const app of apps) {
    const mb = app.memoryBytes / (1024 * 1024);
    const rad = Math.sqrt(Math.max(0.5, mb)) * 0.4;
    const height = Math.max(1, mb / 300);
    const geo = new THREE.CylinderGeometry(rad, rad, height, 12);
    const isSelf = app.packageName.includes('silicon') || app.appName === 'SILICON';
    const mat = new THREE.MeshStandardMaterial({
      color: isSelf ? 0xcc44ff : 0x334455,
      emissive: isSelf ? 0x331144 : 0x111822,
      roughness: 0.7,
      metalness: 0.2,
    });
    const mesh = new THREE.Mesh(geo, mat);
    const seed = hashString(app.packageName);
    const p = islandPos(seed);
    mesh.position.set(p.x, height / 2 + 0.5, p.z);
    mesh.userData.baseY = mesh.position.y;
    h.group.add(mesh);
    h.islands.push(mesh);
  }
}

export function updateRamOcean(
  h: RamOceanHandles,
  ramPressurePct: number,
  lowMemory: boolean,
  time: number,
): void {
  const pressure = Math.max(0, Math.min(100, ramPressurePct)) / 100;
  const waveAmp = pressure * 4;
  const waveSpeed = 0.5 + pressure * 1.2 + (lowMemory ? 0.8 : 0);
  const pos = h.positions;
  const base = h.baseY;
  for (let i = 0; i < base.length; i++) {
    const ix = i * 3;
    const x = pos[ix];
    const z = pos[ix + 2];
    pos[ix + 1] =
      base[i] + Math.sin(time * waveSpeed + x * 0.05) * waveAmp + Math.cos(time * 0.7 + z * 0.04) * waveAmp * 0.3;
  }
  h.mesh.geometry.attributes.position.needsUpdate = true;
  h.mesh.geometry.computeVertexNormals();

  const mat = h.mesh.material as THREE.MeshStandardMaterial;
  if (pressure > 0.8 || lowMemory) {
    mat.emissive.lerp(new THREE.Color(0xaaccff), 0.15);
  } else {
    mat.emissive.set(0x001833);
  }

  const bob = Math.sin(time * 1.5) * 0.15;
  h.islands.forEach((is) => {
    const base = (is.userData.baseY as number) ?? is.position.y;
    is.position.y = base + bob;
  });
}
