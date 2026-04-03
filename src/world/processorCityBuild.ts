import * as THREE from 'three';

import { WORLD, mulberry32 } from './worldConstants';

export type ProcessorCityHandles = {
  group: THREE.Group;
  matDowntown: THREE.MeshStandardMaterial;
  matMid: THREE.MeshStandardMaterial;
  matSub: THREE.MeshStandardMaterial;
  pulseData: { mesh: THREE.Mesh; path: THREE.Vector3[]; t: number; speed: number }[];
  dispose: () => void;
};

function lerpIdleHot(t: number): THREE.Color {
  const idle = new THREE.Color(0x001133);
  const hot = new THREE.Color(0xff6600);
  return idle.clone().lerp(hot, Math.max(0, Math.min(1, t)));
}

export function createProcessorCity(): ProcessorCityHandles {
  const group = new THREE.Group();
  group.position.copy(WORLD.processor);

  const rng = mulberry32(42);
  const boxGeo = new THREE.BoxGeometry(1, 1, 1);

  const matDowntown = new THREE.MeshStandardMaterial({
    color: 0x223344,
    emissive: 0x001133,
    emissiveIntensity: 0.6,
    metalness: 0.2,
    roughness: 0.85,
  });
  const matMid = matDowntown.clone();
  const matSub = matDowntown.clone();

  const addDistrict = (
    mat: THREE.MeshStandardMaterial,
    cx: number,
    cz: number,
    spread: number,
    count: number,
    hMin: number,
    hMax: number,
  ) => {
    for (let i = 0; i < count; i++) {
      const w = 1 + rng() * 3;
      const d = 1 + rng() * 3;
      const h = hMin + rng() * (hMax - hMin);
      const mesh = new THREE.Mesh(boxGeo, mat);
      mesh.position.set(cx + (rng() - 0.5) * spread, h / 2, cz + (rng() - 0.5) * spread);
      mesh.scale.set(w, h, d);
      group.add(mesh);
    }
  };

  addDistrict(matDowntown, 0, 0, 40, 200, 4, 20);
  addDistrict(matMid, 50, 30, 35, 150, 2, 12);
  addDistrict(matSub, -30, 50, 120, 100, 1, 6);

  const plane = new THREE.Mesh(
    new THREE.PlaneGeometry(200, 200),
    new THREE.MeshBasicMaterial({
      color: 0x112233,
      transparent: true,
      opacity: 0.08,
      side: THREE.DoubleSide,
    }),
  );
  plane.rotation.x = -Math.PI / 2;
  plane.position.y = 0.05;
  group.add(plane);

  const pulseGeo = new THREE.SphereGeometry(0.4, 8, 8);
  const pulseMat = new THREE.MeshBasicMaterial({
    color: 0xaaffff,
    transparent: true,
    opacity: 0.9,
    blending: THREE.AdditiveBlending,
  });
  const pulseData: ProcessorCityHandles['pulseData'] = [];
  for (let p = 0; p < 12; p++) {
    const mesh = new THREE.Mesh(pulseGeo, pulseMat);
    const path = [
      new THREE.Vector3(-15 + rng() * 30, 3, -15 + rng() * 30),
      new THREE.Vector3(15 + rng() * 20, 6, 10 + rng() * 20),
      new THREE.Vector3(-10 + rng() * 15, 4, 20 + rng() * 15),
    ];
    pulseData.push({ mesh, path, t: rng(), speed: 0.12 + rng() * 0.12 });
    group.add(mesh);
  }

  return {
    group,
    matDowntown,
    matMid,
    matSub,
    pulseData,
    dispose: () => {
      boxGeo.dispose();
      matDowntown.dispose();
      matMid.dispose();
      matSub.dispose();
      pulseGeo.dispose();
      pulseMat.dispose();
      plane.geometry.dispose();
      (plane.material as THREE.Material).dispose();
    },
  };
}

export function updateProcessorCity(h: ProcessorCityHandles, cpu: { usage: number }[], time: number): void {
  const u = (i: number) => cpu[i]?.usage ?? 0;
  const downtownAvg = (u(0) + u(1)) / 2;
  const midAvg = (u(2) + u(3)) / 2;
  let subSum = 0;
  for (let i = 4; i < 8; i++) subSum += u(i);
  const subAvg = subSum / 4;

  h.matDowntown.emissive.copy(lerpIdleHot(downtownAvg / 100));
  h.matMid.emissive.copy(lerpIdleHot(midAvg / 100));
  h.matSub.emissive.copy(lerpIdleHot(subAvg / 100));

  h.pulseData.forEach((pd, idx) => {
    pd.t += pd.speed * 0.016;
    if (pd.t >= 1) pd.t = 0;
    const seg = pd.t * (pd.path.length - 1);
    const a = Math.floor(seg);
    const b = Math.min(a + 1, pd.path.length - 1);
    const localT = seg - a;
    pd.mesh.position.lerpVectors(pd.path[a], pd.path[b], localT);
    const freq = (cpu[idx % Math.max(1, cpu.length)]?.usage ?? 50) / 100;
    pd.mesh.position.y += Math.sin(time * 4 + idx * 0.5) * 0.04 * freq;
  });
}
