import * as THREE from 'three';

import { WORLD } from './worldConstants';

export type NetworkWeatherHandles = {
  group: THREE.Group;
  rain: THREE.InstancedMesh;
  upload: THREE.InstancedMesh;
  stars: THREE.Points;
  gpsStars: THREE.Points;
  cloudBlobs: THREE.InstancedMesh;
  lightning: THREE.Line;
  lightningT: number;
  dummy: THREE.Object3D;
  planeGeo: THREE.PlaneGeometry;
  rxActive: number;
  txActive: number;
  dispose: () => void;
};

const MAX_PACKETS = 2000;

function makeStarfieldHemisphere(count: number): THREE.Points {
  const geo = new THREE.BufferGeometry();
  const pos = new Float32Array(count * 3);
  const sizes = new Float32Array(count);
  for (let i = 0; i < count; i++) {
    const u = Math.random();
    const v = Math.random();
    const theta = 2 * Math.PI * u;
    const phi = Math.acos(0.2 + v * 0.6);
    const r = 520 + Math.random() * 120;
    const sinP = Math.sin(phi);
    pos[i * 3] = r * sinP * Math.cos(theta);
    pos[i * 3 + 1] = Math.abs(r * Math.cos(phi)) + 40;
    pos[i * 3 + 2] = r * sinP * Math.sin(theta);
    const roll = Math.random();
    sizes[i] = roll < 0.6 ? 0.5 : roll < 0.9 ? 1.0 : 1.8;
  }
  geo.setAttribute('position', new THREE.BufferAttribute(pos, 3));
  geo.setAttribute('size', new THREE.BufferAttribute(sizes, 1));
  return new THREE.Points(
    geo,
    new THREE.PointsMaterial({
      color: 0xffffff,
      size: 1,
      transparent: true,
      opacity: 0.85,
      depthWrite: false,
      sizeAttenuation: true,
    }),
  );
}

function makeGpsStars(): THREE.Points {
  const n = 10;
  const geo = new THREE.BufferGeometry();
  const pos = new Float32Array(n * 3);
  for (let i = 0; i < n; i++) {
    const a = (i / n) * Math.PI * 2;
    pos[i * 3] = Math.cos(a) * 400;
    pos[i * 3 + 1] = 200 + (i % 3) * 15;
    pos[i * 3 + 2] = Math.sin(a) * 400;
  }
  geo.setAttribute('position', new THREE.BufferAttribute(pos, 3));
  return new THREE.Points(
    geo,
    new THREE.PointsMaterial({
      color: 0xffeedd,
      size: 2.2,
      transparent: true,
      opacity: 0.95,
      depthWrite: false,
      blending: THREE.AdditiveBlending,
    }),
  );
}

export function createNetworkWeather(): NetworkWeatherHandles {
  const group = new THREE.Group();
  group.position.copy(WORLD.networkSky);

  const stars = makeStarfieldHemisphere(5000);
  const gpsStars = makeGpsStars();
  group.add(stars, gpsStars);

  const dummy = new THREE.Object3D();
  const planeGeo = new THREE.PlaneGeometry(0.3, 0.6);
  const rxMat = new THREE.MeshBasicMaterial({
    color: 0x00ffe5,
    transparent: true,
    opacity: 0.7,
    side: THREE.DoubleSide,
    depthWrite: false,
    blending: THREE.AdditiveBlending,
  });
  const txMat = rxMat.clone();
  txMat.color = new THREE.Color(0xffb700);

  const rain = new THREE.InstancedMesh(planeGeo, rxMat, MAX_PACKETS);
  const upload = new THREE.InstancedMesh(planeGeo, txMat, MAX_PACKETS);
  rain.count = 0;
  upload.count = 0;
  group.add(rain, upload);

  const sphereGeo = new THREE.SphereGeometry(18, 10, 10);
  const cloudMat = new THREE.MeshStandardMaterial({
    color: 0x223344,
    transparent: true,
    opacity: 0.18,
    depthWrite: false,
    metalness: 0.2,
    roughness: 0.8,
  });
  const cloudBlobs = new THREE.InstancedMesh(sphereGeo, cloudMat, 200);
  for (let i = 0; i < 200; i++) {
    dummy.position.set((Math.random() - 0.5) * 280, (Math.random() - 0.5) * 80, (Math.random() - 0.5) * 280);
    const s = 15 + Math.random() * 35;
    dummy.scale.setScalar(s);
    dummy.rotation.set(Math.random() * 6, Math.random() * 6, Math.random() * 6);
    dummy.updateMatrix();
    cloudBlobs.setMatrixAt(i, dummy.matrix);
  }
  cloudBlobs.instanceMatrix.needsUpdate = true;
  group.add(cloudBlobs);

  const lightningGeo = new THREE.BufferGeometry().setFromPoints([
    new THREE.Vector3(0, 180, 0),
    new THREE.Vector3(0, -120, 0),
  ]);
  const lightning = new THREE.Line(
    lightningGeo,
    new THREE.LineBasicMaterial({ color: 0xe0ffff, transparent: true, opacity: 0 }),
  );
  group.add(lightning);

  return {
    group,
    rain,
    upload,
    stars,
    gpsStars,
    cloudBlobs,
    lightning,
    lightningT: -1,
    dummy,
    planeGeo,
    rxActive: 0,
    txActive: 0,
    dispose: () => {
      stars.geometry.dispose();
      (stars.material as THREE.Material).dispose();
      gpsStars.geometry.dispose();
      (gpsStars.material as THREE.Material).dispose();
      planeGeo.dispose();
      rxMat.dispose();
      txMat.dispose();
      sphereGeo.dispose();
      cloudMat.dispose();
      lightningGeo.dispose();
      (lightning.material as THREE.Material).dispose();
    },
  };
}

export function updateNetworkWeather(
  h: NetworkWeatherHandles,
  rxBps: number,
  txBps: number,
  time: number,
  camera: THREE.PerspectiveCamera,
  wifiStrength: number | null,
): void {
  const rxK = rxBps / 1024;
  const txK = txBps / 1024;

  h.rxActive = Math.min(MAX_PACKETS, Math.floor(rxK * 2));
  h.txActive = Math.min(MAX_PACKETS, Math.floor(txK * 2));
  h.rain.count = h.rxActive;
  h.upload.count = h.txActive;

  const inv = 1 - (wifiStrength ?? 0.7);
  const cloudOp = 0.08 + inv * 0.55;
  ;(h.cloudBlobs.material as THREE.MeshStandardMaterial).opacity = cloudOp;
  h.cloudBlobs.rotation.y = time * 0.015;

  const gpsMat = h.gpsStars.material as THREE.PointsMaterial;
  gpsMat.opacity = 0.5 + Math.sin(time * 2.2) * 0.35;

  const dummy = h.dummy;
  const spread = 380;
  for (let i = 0; i < h.rxActive; i++) {
    const seed = i * 0.731;
    let y = ((time * 3 + seed * 50) % 1) * 260 - 40;
    const x = Math.sin(seed * 12) * spread * 0.5 + (i % 50) * 6;
    const z = Math.cos(seed * 8) * spread * 0.5;
    dummy.position.set(x, y, z);
    dummy.lookAt(camera.position);
    dummy.updateMatrix();
    h.rain.setMatrixAt(i, dummy.matrix);
  }
  h.rain.instanceMatrix.needsUpdate = true;

  for (let i = 0; i < h.txActive; i++) {
    const seed = i * 0.519;
    let y = ((time * 2.5 + seed * 40) % 1) * 200 - 100;
    y = -70 + (1 - ((time * 2 + seed) % 1)) * 200;
    const x = Math.sin(seed * 10) * spread * 0.45;
    const z = Math.cos(seed * 9) * spread * 0.45;
    dummy.position.set(x, y, z);
    dummy.lookAt(camera.position);
    dummy.updateMatrix();
    h.upload.setMatrixAt(i, dummy.matrix);
  }
  h.upload.instanceMatrix.needsUpdate = true;

  const hi = rxK > 500 || txK > 500;
  if (hi && h.lightningT < 0 && Math.random() < 0.02) {
    h.lightningT = time;
    const lx = (Math.random() - 0.5) * 200;
    const lz = (Math.random() - 0.5) * 200;
    h.lightning.position.set(lx, 0, lz);
    const pts = h.lightning.geometry.attributes.position.array as Float32Array;
    pts[0] = lx;
    pts[1] = 200;
    pts[2] = lz;
    pts[3] = lx + (Math.random() - 0.5) * 20;
    pts[4] = -80;
    pts[5] = lz + (Math.random() - 0.5) * 20;
    h.lightning.geometry.attributes.position.needsUpdate = true;
    ;(h.lightning.material as THREE.LineBasicMaterial).opacity = 1;
  }
  if (h.lightningT >= 0 && time - h.lightningT > 0.08) {
    ;(h.lightning.material as THREE.LineBasicMaterial).opacity = 0;
    h.lightningT = -1;
  }

  const starOp = Math.max(0.25, 0.9 - cloudOp * 0.85);
  ;(h.stars.material as THREE.PointsMaterial).opacity = starOp;
}
