import * as THREE from 'three';

import { WORLD } from './worldConstants';
import { batteryLevelToEmissive } from './batteryColors';

const TARGETS = [
  WORLD.processor,
  WORLD.ramOcean,
  WORLD.storage,
  WORLD.sensors,
  WORLD.networkSky,
  WORLD.display,
];

export type BatterySunHandles = {
  group: THREE.Group;
  sunCore: THREE.Mesh;
  corona1: THREE.Mesh;
  corona2: THREE.Mesh;
  rivers: THREE.Mesh[];
  sunLight: THREE.PointLight;
  dispose: () => void;
};

export function createBatterySun(): BatterySunHandles {
  const group = new THREE.Group();

  const baseMat = new THREE.MeshStandardMaterial({
    color: 0xffb300,
    emissive: 0xffa000,
    emissiveIntensity: 1,
    roughness: 0.3,
    metalness: 0.1,
  });
  const geo = new THREE.SphereGeometry(18, 48, 48);
  const sunCore = new THREE.Mesh(geo, baseMat);
  group.add(sunCore);

  const c1 = new THREE.Mesh(
    new THREE.SphereGeometry(18 * 1.4, 32, 32),
    new THREE.MeshBasicMaterial({
      transparent: true,
      opacity: 0.06,
      depthWrite: false,
      blending: THREE.AdditiveBlending,
    }),
  );
  const c2 = new THREE.Mesh(
    new THREE.SphereGeometry(18 * 1.8, 24, 24),
    new THREE.MeshBasicMaterial({
      transparent: true,
      opacity: 0.03,
      depthWrite: false,
      blending: THREE.AdditiveBlending,
    }),
  );
  group.add(c1, c2);

  const sunLight = new THREE.PointLight(0xffcc66, 1.2, 800, 2);
  sunLight.position.set(0, 0, 0);
  group.add(sunLight);

  const rivers: THREE.Mesh[] = [];
  const curveMat = new THREE.MeshBasicMaterial({
    transparent: true,
    opacity: 0.15,
    blending: THREE.AdditiveBlending,
    depthWrite: false,
  });

  for (let i = 0; i < TARGETS.length; i++) {
    const end = TARGETS[i];
    const pts = [
      new THREE.Vector3(0, 0, 0),
      new THREE.Vector3(end.x * 0.35, end.y * 0.35, end.z * 0.35),
      end.clone(),
    ];
    const curve = new THREE.CatmullRomCurve3(pts);
    const tube = new THREE.TubeGeometry(curve, 24, 0.3, 8, false);
    const mesh = new THREE.Mesh(tube, curveMat.clone());
    rivers.push(mesh);
    group.add(mesh);
  }

  return {
    group,
    sunCore,
    corona1: c1,
    corona2: c2,
    rivers,
    sunLight,
    dispose: () => {
      geo.dispose();
      baseMat.dispose();
      c1.geometry.dispose();
      (c1.material as THREE.Material).dispose();
      c2.geometry.dispose();
      (c2.material as THREE.Material).dispose();
      rivers.forEach((m) => {
        m.geometry.dispose();
        (m.material as THREE.Material).dispose();
      });
    },
  };
}

export function updateBatterySun(
  h: BatterySunHandles,
  batteryLevel: number,
  powerWatts: number,
  time: number,
): void {
  const lvl = Math.max(0, Math.min(100, batteryLevel));
  const r = 18 * (lvl / 100) * 0.6 + 7.2;
  h.sunCore.scale.setScalar(r / 18);
  h.corona1.scale.setScalar((r / 18) * 1.4);
  h.corona2.scale.setScalar((r / 18) * 1.8);

  const col = batteryLevelToEmissive(lvl);
  const mat = h.sunCore.material as THREE.MeshStandardMaterial;
  mat.emissive.copy(col);
  mat.color.copy(col);
  mat.emissiveIntensity = 0.8 + (lvl / 100) * 1.2;

  ;(h.corona1.material as THREE.MeshBasicMaterial).color.copy(col);
  ;(h.corona2.material as THREE.MeshBasicMaterial).color.copy(col);

  h.sunLight.color.copy(col);
  h.sunLight.intensity = 0.6 + (lvl / 100) * 1.4;

  const pulse = 1 + Math.sin(time * Math.PI * 2 * 0.25) * 0.02;
  h.group.scale.setScalar(pulse);

  h.group.rotation.y += 0.002;

  const pNorm = powerWatts > 0 ? Math.min(1, powerWatts / 8) : 0.05;
  const riverBright = 0.08 + pNorm * 0.35;
  h.rivers.forEach((river, i) => {
    const m = river.material as THREE.MeshBasicMaterial;
    m.color.copy(col);
    const boost = i === 0 ? 1.25 : i === 5 ? 1.1 : 1;
    m.opacity = Math.min(0.45, riverBright * boost);
  });
}
