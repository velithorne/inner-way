import * as THREE from 'three';

import { WORLD } from './worldConstants';

const _stor = WORLD.storage.clone();

export type AtmosphereHandles = {
  stars: THREE.Points;
  dust: THREE.Points;
  ground: THREE.Mesh;
  dispose: () => void;
};

export function createAtmosphere(): AtmosphereHandles {
  const starCount = 3000;
  const starGeo = new THREE.BufferGeometry();
  const starPos = new Float32Array(starCount * 3);
  const starOp = new Float32Array(starCount);
  const rng = () => Math.random();
  for (let i = 0; i < starCount; i++) {
    const u = rng();
    const v = rng();
    const theta = 2 * Math.PI * u;
    const phi = Math.acos(2 * v - 1);
    const r = 580 + rng() * 40;
    const sinP = Math.sin(phi);
    starPos[i * 3] = r * sinP * Math.cos(theta);
    starPos[i * 3 + 1] = r * Math.cos(phi);
    starPos[i * 3 + 2] = r * sinP * Math.sin(theta);
    starOp[i] = 0.3 + rng() * 0.7;
  }
  starGeo.setAttribute('position', new THREE.BufferAttribute(starPos, 3));
  const stars = new THREE.Points(
    starGeo,
    new THREE.PointsMaterial({
      color: 0xffffff,
      size: 0.8,
      transparent: true,
      opacity: 0.85,
      depthWrite: false,
    }),
  );

  const dustCount = 1500;
  const dustGeo = new THREE.BufferGeometry();
  const dustPos = new Float32Array(dustCount * 3);
  for (let i = 0; i < dustCount; i++) {
    dustPos[i * 3] = (rng() - 0.5) * 500;
    dustPos[i * 3 + 1] = (rng() - 0.5) * 400;
    dustPos[i * 3 + 2] = (rng() - 0.5) * 500;
  }
  dustGeo.setAttribute('position', new THREE.BufferAttribute(dustPos, 3));
  const dust = new THREE.Points(
    dustGeo,
    new THREE.PointsMaterial({
      color: 0xeeeeee,
      size: 0.15,
      transparent: true,
      opacity: 0.35,
      depthWrite: false,
    }),
  );

  const ground = new THREE.Mesh(
    new THREE.PlaneGeometry(2000, 2000, 1, 1),
    new THREE.MeshStandardMaterial({
      color: 0x000811,
      emissive: 0x000811,
      roughness: 1,
      metalness: 0,
    }),
  );
  ground.rotation.x = -Math.PI / 2;
  ground.position.y = -80;

  return {
    stars,
    dust,
    ground,
    dispose: () => {
      starGeo.dispose();
      (stars.material as THREE.Material).dispose();
      dustGeo.dispose();
      (dust.material as THREE.Material).dispose();
      ground.geometry.dispose();
      (ground.material as THREE.Material).dispose();
    },
  };
}

export function updateAtmosphere(
  h: AtmosphereHandles,
  time: number,
  camPos: THREE.Vector3,
  batteryLevel: number,
): void {
  const dPos = h.dust.geometry.attributes.position.array as Float32Array;
  const sunNear = 1 - Math.min(1, camPos.length() / 220);
  const drift = 0.08 + sunNear * 0.35;
  for (let i = 0; i < dPos.length / 3; i++) {
    dPos[i * 3] += Math.sin(time * 0.15 + i * 0.01) * drift * 0.01;
    dPos[i * 3 + 1] += Math.cos(time * 0.12 + i * 0.02) * drift * 0.008;
    dPos[i * 3 + 2] += Math.sin(time * 0.1 + i * 0.015) * drift * 0.01;
    if (Math.abs(dPos[i * 3]) > 260) dPos[i * 3] *= -0.95;
    if (Math.abs(dPos[i * 3 + 1]) > 220) dPos[i * 3 + 1] *= -0.95;
    if (Math.abs(dPos[i * 3 + 2]) > 260) dPos[i * 3 + 2] *= -0.95;
  }
  h.dust.geometry.attributes.position.needsUpdate = true;
  const storNear = camPos.distanceTo(_stor) < 180 ? 0.35 : 1;
  ;(h.dust.material as THREE.PointsMaterial).opacity = (0.2 + sunNear * 0.25) * storNear;

  const starMat = h.stars.material as THREE.PointsMaterial;
  starMat.opacity = 0.55 + Math.sin(time * 0.2) * 0.08;
  if (batteryLevel < 15) starMat.opacity += 0.1;
}
