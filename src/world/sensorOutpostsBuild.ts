import * as THREE from 'three';

import { WORLD } from './worldConstants';

export type SensorOutpostsHandles = {
  group: THREE.Group;
  gyroX: THREE.Object3D;
  gyroY: THREE.Object3D;
  gyroZ: THREE.Object3D;
  needle: THREE.Mesh;
  compassDisc: THREE.Mesh;
  dispose: () => void;
};

export function createSensorOutposts(): SensorOutpostsHandles {
  const group = new THREE.Group();
  group.position.copy(WORLD.sensors);

  const gyroGroup = new THREE.Group();
  const ringGeo = new THREE.TorusGeometry(3, 0.15, 8, 24);
  const ringMat = new THREE.MeshStandardMaterial({ color: 0x00bcd4, emissive: 0x004455 });
  const gx = new THREE.Mesh(ringGeo, ringMat);
  const gy = new THREE.Mesh(ringGeo, ringMat.clone());
  const gz = new THREE.Mesh(ringGeo, ringMat.clone());
  gy.rotation.x = Math.PI / 2;
  gz.rotation.y = Math.PI / 2;
  gyroGroup.add(gx, gy, gz);
  group.add(gyroGroup);

  const base = new THREE.Mesh(
    new THREE.CylinderGeometry(4, 5, 1, 16),
    new THREE.MeshStandardMaterial({ color: 0x223344 }),
  );
  base.position.y = 0.5;
  group.add(base);

  const needle = new THREE.Mesh(
    new THREE.BoxGeometry(0.2, 4, 0.2),
    new THREE.MeshStandardMaterial({ color: 0xff7043, emissive: 0x441100 }),
  );
  needle.position.y = 3;
  group.add(needle);

  const compassDisc = new THREE.Mesh(
    new THREE.CylinderGeometry(2.5, 2.5, 0.1, 32),
    new THREE.MeshStandardMaterial({ color: 0x006064, emissive: 0x001122 }),
  );
  compassDisc.position.set(-8, 6, 4);
  group.add(compassDisc);

  return {
    group,
    gyroX: gx,
    gyroY: gy,
    gyroZ: gz,
    needle,
    compassDisc,
    dispose: () => {
      ringGeo.dispose();
      ringMat.dispose();
      ;(gy.material as THREE.MeshStandardMaterial).dispose();
      ;(gz.material as THREE.MeshStandardMaterial).dispose();
      base.geometry.dispose();
      (base.material as THREE.Material).dispose();
      needle.geometry.dispose();
      (needle.material as THREE.Material).dispose();
      compassDisc.geometry.dispose();
      (compassDisc.material as THREE.Material).dispose();
    },
  };
}

export function updateSensorOutposts(
  h: SensorOutpostsHandles,
  mag: { x: number; y: number; z: number } | null,
  gyro: { x: number; y: number; z: number } | null,
  acc: { x: number; y: number; z: number } | null,
): void {
  if (gyro) {
    h.gyroX.rotation.x += gyro.x * 0.05;
    h.gyroY.rotation.y += gyro.y * 0.05;
    h.gyroZ.rotation.z += gyro.z * 0.05;
  }
  if (mag) {
    const heading = Math.atan2(mag.y, mag.x);
    h.compassDisc.rotation.y = heading;
  }
  if (acc) {
    h.needle.rotation.z = -acc.x * 0.8;
    h.needle.rotation.x = acc.y * 0.5;
  }
}
