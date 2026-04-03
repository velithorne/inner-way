import * as THREE from 'three';

import { WORLD } from './worldConstants';

export type SensorOutpostsHandles = {
  group: THREE.Group;
  lod: THREE.LOD;
  compassNeedle: THREE.Group;
  compassDisc: THREE.Mesh;
  compassTower: THREE.Group;
  gyroRings: [THREE.Mesh, THREE.Mesh, THREE.Mesh];
  gyroMonument: THREE.Group;
  accelNeedle: THREE.Mesh;
  seismoLine: THREE.Line;
  seismoHistory: number[];
  baroNeedle: THREE.Mesh;
  anemometer: THREE.Group;
  telescope: THREE.Group;
  gpsDishes: THREE.Mesh[];
  anomalyRing: THREE.Mesh;
  dispose: () => void;
};

const _v = new THREE.Vector3();
const _fwd = new THREE.Vector3();
const _m = new THREE.Matrix4();
const _localDir = new THREE.Vector3();
const _yAxis = new THREE.Vector3(0, 1, 0);

function subLocal(wx: number, wy: number, wz: number, out: THREE.Vector3): THREE.Vector3 {
  return out.set(wx - WORLD.sensors.x, wy - WORLD.sensors.y, wz - WORLD.sensors.z);
}

export function createSensorOutposts(): SensorOutpostsHandles {
  const group = new THREE.Group();
  group.position.copy(WORLD.sensors);

  const detail = new THREE.Group();

  const compassTower = new THREE.Group();
  compassTower.position.copy(subLocal(95, 0, -30, new THREE.Vector3()));
  const cyl = new THREE.Mesh(
    new THREE.CylinderGeometry(0.8, 0.9, 12, 16),
    new THREE.MeshStandardMaterial({ color: 0x223344, metalness: 0.3, roughness: 0.6 }),
  );
  cyl.position.y = 6;
  compassTower.add(cyl);

  const compassDisc = new THREE.Mesh(
    new THREE.CylinderGeometry(4, 4, 0.3, 32),
    new THREE.MeshStandardMaterial({ color: 0x004455, emissive: 0x002233, metalness: 0.2 }),
  );
  compassDisc.position.y = 12.2;
  compassTower.add(compassDisc);

  const needleGeo = new THREE.BoxGeometry(0.25, 3.2, 0.12);
  const needleNorth = new THREE.Mesh(
    needleGeo,
    new THREE.MeshStandardMaterial({ color: 0x00ffe5, emissive: 0x004440 }),
  );
  needleNorth.position.set(0, 0.4, 0);
  const needleSouth = new THREE.Mesh(
    needleGeo.clone(),
    new THREE.MeshStandardMaterial({ color: 0xff2200, emissive: 0x440000 }),
  );
  needleSouth.position.set(0, -0.4, 0);
  needleSouth.scale.set(1, 0.85, 1);
  const compassNeedle = new THREE.Group();
  compassNeedle.add(needleNorth, needleSouth);
  compassNeedle.position.y = 12.35;
  compassTower.add(compassNeedle);

  const torus = new THREE.Mesh(
    new THREE.TorusGeometry(4.2, 0.08, 8, 32),
    new THREE.MeshBasicMaterial({ color: 0x00bcd4, transparent: true, opacity: 0.35 }),
  );
  torus.rotation.x = Math.PI / 2;
  torus.position.y = 12.2;
  compassTower.add(torus);

  const anomalyRing = new THREE.Mesh(
    new THREE.TorusGeometry(5, 0.12, 8, 32),
    new THREE.MeshBasicMaterial({
      color: 0xffd700,
      transparent: true,
      opacity: 0,
      blending: THREE.AdditiveBlending,
    }),
  );
  anomalyRing.rotation.x = Math.PI / 2;
  anomalyRing.position.y = 12.2;
  compassTower.add(anomalyRing);

  detail.add(compassTower);

  const gyroMonument = new THREE.Group();
  gyroMonument.position.copy(subLocal(80, 0, -50, new THREE.Vector3()));
  const ringGeo = new THREE.TorusGeometry(3.2, 0.12, 12, 32);
  const ringMat = new THREE.MeshStandardMaterial({
    color: 0x8899aa,
    metalness: 0.9,
    roughness: 0.1,
    emissive: 0x003344,
    emissiveIntensity: 0.2,
  });
  const gOuter = new THREE.Mesh(ringGeo, ringMat);
  const gMid = new THREE.Mesh(ringGeo.clone(), ringMat.clone());
  const gInner = new THREE.Mesh(ringGeo.clone(), ringMat.clone());
  gMid.rotation.x = Math.PI / 2;
  gInner.rotation.y = Math.PI / 2;
  gOuter.position.y = 4;
  gMid.position.y = 4;
  gInner.position.y = 4;
  gyroMonument.add(gOuter, gMid, gInner);
  detail.add(gyroMonument);

  const seismo = new THREE.Group();
  seismo.position.copy(subLocal(70, 0, -40, new THREE.Vector3()));
  const base = new THREE.Mesh(
    new THREE.BoxGeometry(8, 1, 4),
    new THREE.MeshStandardMaterial({ color: 0x2a3344 }),
  );
  base.position.y = 0.5;
  seismo.add(base);
  const post = new THREE.Mesh(
    new THREE.CylinderGeometry(0.15, 0.2, 6, 8),
    new THREE.MeshStandardMaterial({ color: 0x445566 }),
  );
  post.position.y = 4;
  seismo.add(post);
  const accelNeedle = new THREE.Mesh(
    new THREE.ConeGeometry(0.25, 1.2, 8),
    new THREE.MeshStandardMaterial({ color: 0xffaa77, emissive: 0x331100 }),
  );
  accelNeedle.position.y = 6.2;
  accelNeedle.geometry.rotateX(Math.PI / 2);
  seismo.add(accelNeedle);

  const histPts: THREE.Vector3[] = [];
  for (let i = 0; i < 128; i++) histPts.push(new THREE.Vector3(i * 0.08 - 5, 2.5, -2.2));
  const seismoGeo = new THREE.BufferGeometry().setFromPoints(histPts);
  const seismoLine = new THREE.Line(
    seismoGeo,
    new THREE.LineBasicMaterial({ color: 0x00ffe5, transparent: true, opacity: 0.85 }),
  );
  seismo.add(seismoLine);
  detail.add(seismo);

  const baro = new THREE.Group();
  baro.position.copy(subLocal(90, 0, -45, new THREE.Vector3()));
  const dome = new THREE.Mesh(
    new THREE.SphereGeometry(3, 24, 16, 0, Math.PI * 2, 0, Math.PI / 2),
    new THREE.MeshStandardMaterial({
      color: 0x334455,
      metalness: 0.4,
      roughness: 0.35,
      side: THREE.DoubleSide,
    }),
  );
  dome.position.y = 0.1;
  baro.add(dome);
  const gauge = new THREE.Mesh(
    new THREE.RingGeometry(1.2, 1.5, 32),
    new THREE.MeshBasicMaterial({ color: 0x00ffe5, side: THREE.DoubleSide }),
  );
  gauge.rotation.x = -Math.PI / 2;
  gauge.position.y = 3.05;
  baro.add(gauge);
  const baroNeedle = new THREE.Mesh(
    new THREE.BoxGeometry(0.08, 1.1, 0.04),
    new THREE.MeshStandardMaterial({ color: 0xffeedd }),
  );
  baroNeedle.position.y = 3.2;
  baro.add(baroNeedle);

  const anemometer = new THREE.Group();
  anemometer.position.set(3.5, 3.5, 0);
  for (let i = 0; i < 3; i++) {
    const cup = new THREE.Mesh(
      new THREE.SphereGeometry(0.25, 8, 8),
      new THREE.MeshStandardMaterial({ color: 0x8899aa }),
    );
    cup.position.set(Math.cos((i / 3) * Math.PI * 2) * 0.9, 0, Math.sin((i / 3) * Math.PI * 2) * 0.9);
    anemometer.add(cup);
  }
  baro.add(anemometer);
  detail.add(baro);

  const telescope = new THREE.Group();
  telescope.position.copy(subLocal(105, 0, -35, new THREE.Vector3()));
  const tBase = new THREE.Mesh(
    new THREE.CylinderGeometry(1.2, 1.4, 18, 16),
    new THREE.MeshStandardMaterial({ color: 0x2a2a38, metalness: 0.5 }),
  );
  tBase.position.y = 9;
  telescope.add(tBase);
  const barrel = new THREE.Mesh(
    new THREE.CylinderGeometry(0.5, 0.65, 6, 12),
    new THREE.MeshStandardMaterial({ color: 0x1a1a22, metalness: 0.6 }),
  );
  barrel.position.set(0, 17, 1.2);
  barrel.rotation.x = Math.PI / 2.2;
  const beam = new THREE.Mesh(
    new THREE.CylinderGeometry(0.12, 0.18, 140, 8),
    new THREE.MeshBasicMaterial({
      color: 0xaaccff,
      transparent: true,
      opacity: 0.12,
      blending: THREE.AdditiveBlending,
      depthWrite: false,
    }),
  );
  beam.position.y = 70;
  barrel.add(beam);
  telescope.add(barrel);
  detail.add(telescope);

  const gpsGroup = new THREE.Group();
  gpsGroup.position.copy(subLocal(75, 0, -55, new THREE.Vector3()));
  const gpsDishes: THREE.Mesh[] = [];
  for (let i = 0; i < 4; i++) {
    const dish = new THREE.Group();
    const hem = new THREE.Mesh(
      new THREE.SphereGeometry(1.2, 16, 12, 0, Math.PI * 2, 0, Math.PI / 2),
      new THREE.MeshStandardMaterial({ color: 0x445566, emissive: 0x112233 }),
    );
    hem.rotation.x = Math.PI;
    const stem = new THREE.Mesh(
      new THREE.CylinderGeometry(0.15, 0.2, 1.5, 8),
      new THREE.MeshStandardMaterial({ color: 0x333344 }),
    );
    stem.position.y = 0.8;
    dish.add(hem, stem);
    dish.position.set((i % 2) * 3 - 1.5, 0, Math.floor(i / 2) * 3 - 1.5);
    dish.rotation.y = i * 0.4;
    gpsGroup.add(dish);
    gpsDishes.push(hem);
  }
  detail.add(gpsGroup);

  const proxy = new THREE.Mesh(
    new THREE.BoxGeometry(40, 24, 40),
    new THREE.MeshBasicMaterial({ color: 0x222233, wireframe: true, transparent: true, opacity: 0.15 }),
  );
  proxy.position.y = 10;

  const lod = new THREE.LOD();
  lod.addLevel(detail, 0);
  lod.addLevel(proxy, 100);
  group.add(lod);

  return {
    group,
    lod,
    compassNeedle,
    compassDisc,
    compassTower,
    gyroRings: [gOuter, gMid, gInner],
    gyroMonument,
    accelNeedle,
    seismoLine,
    seismoHistory: new Array(128).fill(0),
    baroNeedle,
    anemometer,
    telescope,
    gpsDishes,
    anomalyRing,
    dispose: () => {
      detail.traverse((o) => {
        if (o instanceof THREE.Mesh) {
          o.geometry.dispose();
          const m = o.material as THREE.Material | THREE.Material[];
          if (Array.isArray(m)) m.forEach((x) => x.dispose());
          else m.dispose();
        }
      });
      seismoGeo.dispose();
      (seismoLine.material as THREE.Material).dispose();
      proxy.geometry.dispose();
      (proxy.material as THREE.Material).dispose();
    },
  };
}

export function updateSensorOutposts(
  h: SensorOutpostsHandles,
  mag: { x: number; y: number; z: number } | null,
  gyro: { x: number; y: number; z: number } | null,
  acc: { x: number; y: number; z: number } | null,
  baro: { pressure: number; pressureDelta: number } | null,
  camera: THREE.PerspectiveCamera,
): void {
  h.lod.update(camera);

  if (mag) {
    const heading = Math.atan2(mag.y, mag.x);
    h.compassNeedle.rotation.y = -heading;
    const magStr = Math.hypot(mag.x, mag.y, mag.z);
    const pulse = magStr > 80 ? 1 : 0;
    (h.anomalyRing.material as THREE.MeshBasicMaterial).opacity = pulse * 0.55;
    h.anomalyRing.scale.setScalar(1 + Math.sin(performance.now() * 0.008) * 0.04 * pulse);
  }

  if (gyro) {
    const gt = performance.now() * 0.001;
    h.gyroRings[0].rotation.z += gyro.z * 0.08;
    h.gyroRings[1].rotation.x += gyro.y * 0.08;
    h.gyroRings[2].rotation.y += gyro.x * 0.08;
    const drift = 0.0015 * Math.sin(gt * 0.7);
    h.gyroRings[0].rotation.z += drift;
    h.gyroRings[1].rotation.x += drift * 0.8;
    h.gyroRings[2].rotation.y += drift * 0.6;
  }

  if (acc) {
    h.accelNeedle.rotation.x = acc.y * 0.5;
    h.accelNeedle.rotation.z = -acc.x * 0.5;
    const magAcc = Math.hypot(acc.x, acc.y, acc.z);
    const hist = h.seismoHistory;
    hist.push(magAcc);
    if (hist.length > 128) hist.shift();
    const pts = h.seismoLine.geometry.attributes.position.array as Float32Array;
    const n = pts.length / 3;
    for (let i = 0; i < n; i++) {
      const v = hist[Math.max(0, hist.length - n + i)] ?? 0;
      pts[i * 3 + 1] = 2.5 + v * 1.2;
    }
    h.seismoLine.geometry.attributes.position.needsUpdate = true;
  }

  if (baro) {
    const n = THREE.MathUtils.clamp((baro.pressure - 980) / 60, 0, 1);
    h.baroNeedle.rotation.z = (n - 0.5) * 2.2;
    h.anemometer.rotation.y += baro.pressureDelta * 2.5;
  }

  camera.getWorldDirection(_fwd);
  const barrel = h.telescope.children[1] as THREE.Mesh | undefined;
  if (barrel) {
    _m.copy(h.telescope.matrixWorld).invert();
    _localDir.copy(_fwd).transformDirection(_m);
    if (_localDir.lengthSq() > 1e-8) {
      barrel.quaternion.setFromUnitVectors(_yAxis, _localDir.normalize());
    }
  }

  const t = performance.now() * 0.001;
  h.gpsDishes.forEach((dish, i) => {
    const lit = i < 3 ? 1 : 0;
    const em = (dish.material as THREE.MeshStandardMaterial).emissive;
    em.setRGB(0.1 + lit * 0.3, 0.15 + lit * 0.35, 0.2 + lit * 0.3);
    dish.rotation.x = Math.PI * 0.5 + Math.sin(t * 0.4 + i) * 0.12;
  });
}
