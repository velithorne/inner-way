import * as THREE from 'three';

import { WORLD, mulberry32 } from './worldConstants';

const UNIT = 4;
const HEIGHTS = [4, 8, 12, 16, 20];

export type ProcessorCityHandles = {
  group: THREE.Group;
  buildings: THREE.InstancedMesh;
  buildingColors: Float32Array;
  dummy: THREE.Object3D;
  mat: THREE.MeshStandardMaterial;
  die: THREE.Mesh;
  dieEdges: THREE.LineSegments;
  corridors: THREE.Mesh[];
  cacheRings: THREE.Mesh[];
  pulseData: { mesh: THREE.Mesh; path: THREE.Vector3[]; t: number; speed: number }[];
  dispose: () => void;
};

function districtColor(d: 'down' | 'mid' | 'sub', loadT: number): THREE.Color {
  const base =
    d === 'down' ? new THREE.Color(0xff4400) : d === 'mid' ? new THREE.Color(0xff2200) : new THREE.Color(0xcc1100);
  const dim = new THREE.Color(0x050010);
  return dim.clone().lerp(base, 0.2 + loadT * 0.8);
}

export function createProcessorCity(): ProcessorCityHandles {
  const group = new THREE.Group();
  group.position.copy(WORLD.processor);

  const rng = mulberry32(42);

  const dieGeo = new THREE.BoxGeometry(160, 4, 120);
  const dieMat = new THREE.MeshStandardMaterial({
    color: 0x050010,
    emissive: 0x020008,
    emissiveIntensity: 0.15,
    metalness: 0.8,
    roughness: 0.15,
  });
  const die = new THREE.Mesh(dieGeo, dieMat);
  die.position.y = 2;
  group.add(die);

  const dieEdges = new THREE.LineSegments(
    new THREE.EdgesGeometry(dieGeo, 35),
    new THREE.LineBasicMaterial({ color: 0x00ffe5, transparent: true, opacity: 0.4 }),
  );
  dieEdges.position.copy(die.position);
  group.add(dieEdges);

  const boxGeo = new THREE.BoxGeometry(UNIT, 1, UNIT);
  const mat = new THREE.MeshStandardMaterial({
    color: 0x0a0014,
    metalness: 0.8,
    roughness: 0.15,
    vertexColors: true,
  });
  const count = 450;
  const buildings = new THREE.InstancedMesh(boxGeo, mat, count);
  const buildingColors = new Float32Array(count * 3);
  const dummy = new THREE.Object3D();

  let idx = 0;
  const placeDistrict = (
    cx: number,
    cz: number,
    spread: number,
    n: number,
    district: 'down' | 'mid' | 'sub',
  ) => {
    for (let i = 0; i < n && idx < count; i++) {
      const gw = 1 + Math.floor(rng() * 2);
      const gd = 1 + Math.floor(rng() * 2);
      const h = HEIGHTS[Math.floor(rng() * HEIGHTS.length)];
      const x = cx + (rng() - 0.5) * spread;
      const z = cz + (rng() - 0.5) * spread;
      dummy.position.set(x, 4 + h / 2, z);
      dummy.scale.set(gw * UNIT, h, gd * UNIT);
      dummy.updateMatrix();
      buildings.setMatrixAt(idx, dummy.matrix);
      const c = districtColor(district, 0.5);
      buildingColors[idx * 3] = c.r;
      buildingColors[idx * 3 + 1] = c.g;
      buildingColors[idx * 3 + 2] = c.b;
      idx++;
    }
  };

  placeDistrict(0, 0, 40, 180, 'down');
  placeDistrict(50, 30, 35, 150, 'mid');
  placeDistrict(-30, 50, 120, 120, 'sub');

  buildings.instanceColor = new THREE.InstancedBufferAttribute(buildingColors, 3);
  buildings.instanceMatrix.needsUpdate = true;
  group.add(buildings);

  const corridors: THREE.Mesh[] = [];
  const corrMat = new THREE.MeshStandardMaterial({
    color: 0x001122,
    emissive: 0x00ffe5,
    emissiveIntensity: 0.15,
    metalness: 0.6,
    roughness: 0.3,
  });
  const seg = [
    { x: 0, z: 0, dx: 50, dz: 30 },
    { x: 0, z: 0, dx: -30, dz: 50 },
    { x: 50, z: 30, dx: -80, dz: 20 },
  ];
  for (const s of seg) {
    const len = Math.hypot(s.dx, s.dz);
    const m = new THREE.Mesh(new THREE.BoxGeometry(2, 0.5, len), corrMat);
    m.position.set(s.x + s.dx / 2, 4.35, s.z + s.dz / 2);
    m.rotation.y = Math.atan2(s.dx, s.dz);
    group.add(m);
    corridors.push(m);
  }

  const cacheRings: THREE.Mesh[] = [];
  for (let r = 0; r < 3; r++) {
    const ring = new THREE.Mesh(
      new THREE.RingGeometry(12 + r * 5, 12.4 + r * 5, 48),
      new THREE.MeshBasicMaterial({
        color: 0xff6600,
        transparent: true,
        opacity: 0.25 - r * 0.06,
        side: THREE.DoubleSide,
      }),
    );
    ring.rotation.x = -Math.PI / 2;
    ring.position.set(0, 4.2, 0);
    group.add(ring);
    cacheRings.push(ring);
  }

  const pulseGeo = new THREE.SphereGeometry(0.4, 8, 8);
  const pulseMat = new THREE.MeshBasicMaterial({
    color: 0xaaffff,
    transparent: true,
    opacity: 0.9,
    blending: THREE.AdditiveBlending,
  });
  const pulseData: ProcessorCityHandles['pulseData'] = [];
  const pathA = [
    new THREE.Vector3(-20, 4.5, 0),
    new THREE.Vector3(20, 4.5, 15),
    new THREE.Vector3(40, 4.5, 30),
  ];
  const pathB = [
    new THREE.Vector3(0, 4.5, -20),
    new THREE.Vector3(-30, 4.5, 40),
    new THREE.Vector3(-50, 4.5, 50),
  ];
  for (let p = 0; p < 12; p++) {
    const mesh = new THREE.Mesh(pulseGeo, pulseMat);
    const path = p % 2 === 0 ? pathA : pathB;
    pulseData.push({ mesh, path: [...path], t: rng(), speed: 0.12 + rng() * 0.12 });
    group.add(mesh);
  }

  return {
    group,
    buildings,
    buildingColors,
    dummy,
    mat,
    die,
    dieEdges,
    corridors,
    cacheRings,
    pulseData,
    dispose: () => {
      dieGeo.dispose();
      dieMat.dispose();
      dieEdges.geometry.dispose();
      (dieEdges.material as THREE.Material).dispose();
      boxGeo.dispose();
      mat.dispose();
      corridors.forEach((m) => {
        m.geometry.dispose();
      });
      corrMat.dispose();
      cacheRings.forEach((r) => {
        r.geometry.dispose();
        (r.material as THREE.Material).dispose();
      });
      pulseGeo.dispose();
      pulseMat.dispose();
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

  const cols = h.buildingColors;
  const n = h.buildings.count;
  let idx = 0;
  const applyBlock = (count: number, district: 'down' | 'mid' | 'sub', load: number) => {
    const c = districtColor(district, load / 100);
    for (let i = 0; i < count && idx < n; i++) {
      cols[idx * 3] = c.r;
      cols[idx * 3 + 1] = c.g;
      cols[idx * 3 + 2] = c.b;
      idx++;
    }
  };
  applyBlock(180, 'down', downtownAvg);
  applyBlock(150, 'mid', midAvg);
  applyBlock(120, 'sub', subAvg);

  h.buildings.instanceColor!.needsUpdate = true;

  h.cacheRings.forEach((ring, i) => {
    ring.rotation.z = time * (0.8 + i * 0.2);
    const m = ring.material as THREE.MeshBasicMaterial;
    m.opacity = 0.12 + (downtownAvg / 100) * 0.2 - i * 0.04;
  });

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
