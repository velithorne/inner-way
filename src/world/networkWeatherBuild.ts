import * as THREE from 'three';

import { WORLD } from './worldConstants';

export type NetworkWeatherHandles = {
  group: THREE.Group;
  rain: THREE.Points;
  upload: THREE.Points;
  stars: THREE.Points;
  cloud: THREE.Mesh;
  rainCount: number;
  uploadCount: number;
  dispose: () => void;
};

function makeParticles(count: number, color: number, spread: number): THREE.Points {
  const geo = new THREE.BufferGeometry();
  const pos = new Float32Array(count * 3);
  for (let i = 0; i < count; i++) {
    pos[i * 3] = (Math.random() - 0.5) * spread;
    pos[i * 3 + 1] = 80 + Math.random() * 80;
    pos[i * 3 + 2] = (Math.random() - 0.5) * spread;
  }
  geo.setAttribute('position', new THREE.BufferAttribute(pos, 3));
  const mat = new THREE.PointsMaterial({
    color,
    size: 0.6,
    transparent: true,
    opacity: 0.5,
    blending: THREE.AdditiveBlending,
    depthWrite: false,
  });
  return new THREE.Points(geo, mat);
}

export function createNetworkWeather(): NetworkWeatherHandles {
  const group = new THREE.Group();
  group.position.copy(WORLD.networkSky);

  const rain = makeParticles(2000, 0x00ffe5, 400);
  const upload = makeParticles(800, 0xffb700, 400);
  const starsGeo = new THREE.BufferGeometry();
  const starPos = new Float32Array(64 * 3);
  for (let i = 0; i < 64; i++) {
    starPos[i * 3] = (Math.random() - 0.5) * 500;
    starPos[i * 3 + 1] = Math.random() * 80 + 20;
    starPos[i * 3 + 2] = (Math.random() - 0.5) * 500;
  }
  starsGeo.setAttribute('position', new THREE.BufferAttribute(starPos, 3));
  const stars = new THREE.Points(
    starsGeo,
    new THREE.PointsMaterial({ color: 0xffffff, size: 0.35, transparent: true, opacity: 0.7 }),
  );

  const cloud = new THREE.Mesh(
    new THREE.SphereGeometry(120, 12, 12),
    new THREE.MeshBasicMaterial({
      color: 0x334455,
      transparent: true,
      opacity: 0.12,
      depthWrite: false,
    }),
  );
  cloud.position.set(0, 0, 0);

  group.add(rain, upload, stars, cloud);

  return {
    group,
    rain,
    upload,
    stars,
    cloud,
    rainCount: 2000,
    uploadCount: 800,
    dispose: () => {
      rain.geometry.dispose();
      (rain.material as THREE.Material).dispose();
      upload.geometry.dispose();
      (upload.material as THREE.Material).dispose();
      stars.geometry.dispose();
      (stars.material as THREE.Material).dispose();
      cloud.geometry.dispose();
      (cloud.material as THREE.Material).dispose();
    },
  };
}

export function updateNetworkWeather(
  h: NetworkWeatherHandles,
  rxBps: number,
  txBps: number,
  time: number,
): void {
  const rxK = rxBps / 1024;
  const txK = txBps / 1024;
  const rainVis = Math.min(1, rxK / 500);
  const upVis = Math.min(1, txK / 500);
  ;(h.rain.material as THREE.PointsMaterial).opacity = 0.05 + rainVis * 0.55;
  ;(h.upload.material as THREE.PointsMaterial).opacity = 0.05 + upVis * 0.5;

  const posR = h.rain.geometry.attributes.position.array as Float32Array;
  for (let i = 0; i < posR.length / 3; i++) {
    posR[i * 3 + 1] -= (0.5 + rainVis * 3) * (0.5 + Math.random() * 0.01);
    if (posR[i * 3 + 1] < -200) posR[i * 3 + 1] = 120 + Math.random() * 40;
  }
  h.rain.geometry.attributes.position.needsUpdate = true;

  const posU = h.upload.geometry.attributes.position.array as Float32Array;
  for (let i = 0; i < posU.length / 3; i++) {
    posU[i * 3 + 1] += (0.3 + upVis * 2) * 0.8;
    if (posU[i * 3 + 1] > 200) posU[i * 3 + 1] = -80;
  }
  h.upload.geometry.attributes.position.needsUpdate = true;

  h.cloud.rotation.y = time * 0.02;
  const cloudMat = h.cloud.material as THREE.MeshBasicMaterial;
  cloudMat.opacity = 0.05 + Math.sin(time * 0.3) * 0.03;
}
