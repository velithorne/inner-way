import * as THREE from 'three';

import { WORLD } from './worldConstants';

const _stor = WORLD.storage.clone();

export type AtmosphereHandles = {
  dust: THREE.InstancedMesh;
  ground: THREE.Mesh;
  viaDots: THREE.InstancedMesh;
  dummy: THREE.Object3D;
  dustMat: THREE.Matrix4;
  pos: THREE.Vector3;
  quat: THREE.Quaternion;
  scale: THREE.Vector3;
  dispose: () => void;
};

export function createAtmosphere(): AtmosphereHandles {
  const dummy = new THREE.Object3D();

  const dustCount = 1500;
  const dustGeo = new THREE.SphereGeometry(0.12, 4, 4);
  const dustMat = new THREE.MeshBasicMaterial({
    color: 0xeeeeee,
    transparent: true,
    opacity: 0.35,
    depthWrite: false,
  });
  const dust = new THREE.InstancedMesh(dustGeo, dustMat, dustCount);
  for (let i = 0; i < dustCount; i++) {
    dummy.position.set((Math.random() - 0.5) * 500, (Math.random() - 0.5) * 400, (Math.random() - 0.5) * 500);
    const s = 0.8 + Math.random() * 0.4;
    dummy.scale.setScalar(s);
    dummy.updateMatrix();
    dust.setMatrixAt(i, dummy.matrix);
  }
  dust.instanceMatrix.needsUpdate = true;
  dust.count = dustCount;

  const groundGeo = new THREE.PlaneGeometry(2000, 2000, 120, 120);
  const pos = groundGeo.attributes.position.array as Float32Array;
  for (let i = 0; i < pos.length / 3; i++) {
    const ix = i * 3;
    const gx = pos[ix];
    const gz = pos[ix + 2];
    const onGrid = Math.abs(gx % 8) < 0.4 || Math.abs(gz % 8) < 0.4;
    if (onGrid) pos[ix + 1] += 0.06;
  }
  groundGeo.computeVertexNormals();
  const ground = new THREE.Mesh(
    groundGeo,
    new THREE.MeshStandardMaterial({
      color: 0x0a1a22,
      emissive: 0x050a10,
      emissiveIntensity: 0.08,
      roughness: 0.9,
      metalness: 0.2,
    }),
  );
  ground.rotation.x = -Math.PI / 2;
  ground.position.y = -80;
  ground.receiveShadow = true;

  const viaN = 2000;
  const viaGeo = new THREE.SphereGeometry(0.3, 6, 6);
  const viaMat = new THREE.MeshBasicMaterial({
    color: 0x00ffe5,
    transparent: true,
    opacity: 0.3,
    depthWrite: false,
  });
  const viaDots = new THREE.InstancedMesh(viaGeo, viaMat, viaN);
  let v = 0;
  for (let x = -400; x < 400 && v < viaN; x += 16) {
    for (let z = -400; z < 400 && v < viaN; z += 16) {
      dummy.position.set(x, -79.5, z);
      dummy.scale.setScalar(1);
      dummy.updateMatrix();
      viaDots.setMatrixAt(v++, dummy.matrix);
    }
  }
  viaDots.count = v;
  viaDots.instanceMatrix.needsUpdate = true;

  return {
    dust,
    ground,
    viaDots,
    dummy,
    dustMat: new THREE.Matrix4(),
    pos: new THREE.Vector3(),
    quat: new THREE.Quaternion(),
    scale: new THREE.Vector3(),
    dispose: () => {
      dustGeo.dispose();
      dustMat.dispose();
      groundGeo.dispose();
      (ground.material as THREE.Material).dispose();
      viaGeo.dispose();
      viaMat.dispose();
    },
  };
}

export function updateAtmosphere(
  h: AtmosphereHandles,
  time: number,
  camPos: THREE.Vector3,
  batteryLevel: number,
): void {
  const n = h.dust.count;
  const sunNear = 1 - Math.min(1, camPos.length() / 220);
  const drift = 0.08 + sunNear * 0.35;
  for (let i = 0; i < n; i++) {
    h.dust.getMatrixAt(i, h.dustMat);
    h.dustMat.decompose(h.pos, h.quat, h.scale);
    h.pos.x += Math.sin(time * 0.15 + i * 0.01) * drift * 0.01;
    h.pos.y += Math.cos(time * 0.12 + i * 0.02) * drift * 0.008;
    h.pos.z += Math.sin(time * 0.1 + i * 0.015) * drift * 0.01;
    if (Math.abs(h.pos.x) > 260) h.pos.x *= 0.96;
    if (Math.abs(h.pos.y) > 220) h.pos.y *= 0.96;
    if (Math.abs(h.pos.z) > 260) h.pos.z *= 0.96;
    h.dustMat.compose(h.pos, h.quat, h.scale);
    h.dust.setMatrixAt(i, h.dustMat);
  }
  h.dust.instanceMatrix.needsUpdate = true;
  const storNear = camPos.distanceTo(_stor) < 180 ? 0.35 : 1;
  ;(h.dust.material as THREE.MeshBasicMaterial).opacity = (0.2 + sunNear * 0.25) * storNear;
  if (batteryLevel < 15) {
    ;(h.dust.material as THREE.MeshBasicMaterial).opacity += 0.08;
  }
}
