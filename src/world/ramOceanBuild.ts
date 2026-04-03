import * as THREE from 'three';

import { WORLD, hashString, mulberry32 } from './worldConstants';

export type RamOceanHandles = {
  group: THREE.Group;
  ocean: THREE.Mesh;
  oceanMat: THREE.ShaderMaterial;
  subsurface: THREE.Mesh;
  floor: THREE.Mesh;
  islands: THREE.Group[];
  ridge: THREE.Mesh;
  dispose: () => void;
};

const oceanVert = `
uniform float uTime;
uniform float uWaveH;
varying vec3 vPos;
varying vec3 vNorm;
void main() {
  vPos = position;
  float w = sin(uTime * 0.8 + position.x * 0.04 + position.z * 0.04) * uWaveH
    + cos(uTime * 0.6 + position.x * 0.03 - position.z * 0.025) * uWaveH * 0.35;
  vec3 p = position;
  p.y += w;
  vNorm = normalize(normalMatrix * normal);
  gl_Position = projectionMatrix * modelViewMatrix * vec4(p, 1.0);
}
`;

const oceanFrag = `
precision mediump float;
uniform vec3 uDeep;
uniform vec3 uEdge;
uniform vec3 uCamPos;
varying vec3 vPos;
varying vec3 vNorm;
void main() {
  vec3 viewDir = normalize(uCamPos - vPos);
  float fresnel = pow(1.0 - max(dot(vNorm, viewDir), 0.0), 2.5);
  vec3 col = mix(uDeep, uEdge, fresnel * 0.85 + 0.1);
  gl_FragColor = vec4(col, 0.92);
}
`;

function displaceIslandTop(geo: THREE.CylinderGeometry, amp: number, rng: () => number): void {
  const pos = geo.attributes.position.array as Float32Array;
  for (let i = 0; i < pos.length / 3; i++) {
    const y = pos[i * 3 + 1];
    if (y > 0.01) {
      pos[i * 3] += (rng() - 0.5) * amp;
      pos[i * 3 + 2] += (rng() - 0.5) * amp;
    }
  }
  geo.computeVertexNormals();
}

function makeIsland(
  rng: () => number,
  radius: number,
  height: number,
  color: number,
  emissive: number,
  x: number,
  z: number,
): THREE.Group {
  const g = new THREE.Group();
  const segs = 24;
  const geo = new THREE.CylinderGeometry(radius, radius * 1.05, height, segs, 4, false);
  displaceIslandTop(geo, 2, rng);
  const mat = new THREE.MeshStandardMaterial({
    color,
    emissive,
    emissiveIntensity: 0.25,
    metalness: 0.8,
    roughness: 0.15,
  });
  const mesh = new THREE.Mesh(geo, mat);
  mesh.position.y = height / 2;
  g.add(mesh);

  const edge = new THREE.Mesh(
    new THREE.TorusGeometry(radius * 1.02, 0.15, 8, 32),
    new THREE.MeshBasicMaterial({
      color: 0x00ffe5,
      transparent: true,
      opacity: 0.4,
    }),
  );
  edge.rotation.x = Math.PI / 2;
  edge.position.y = 0.15;
  g.add(edge);

  for (let b = 0; b < 10; b++) {
    const bx = new THREE.Mesh(
      new THREE.BoxGeometry(1.2, 2 + rng() * 2, 1.2),
      new THREE.MeshStandardMaterial({ color: 0x0a0014, emissive: 0x001122, metalness: 0.7, roughness: 0.2 }),
    );
    bx.position.set((rng() - 0.5) * radius * 1.4, height + 1 + rng() * 2, (rng() - 0.5) * radius * 1.4);
    g.add(bx);
  }

  g.position.set(x, 0, z);
  return g;
}

export function createRamOcean(): RamOceanHandles {
  const group = new THREE.Group();
  group.position.copy(WORLD.ramOcean);

  const segs = 60;
  const plane = new THREE.PlaneGeometry(520, 520, segs, segs);
  plane.rotateX(-Math.PI / 2);

  const oceanMat = new THREE.ShaderMaterial({
    uniforms: {
      uTime: { value: 0 },
      uWaveH: { value: 1.5 },
      uDeep: { value: new THREE.Color(0x001833) },
      uEdge: { value: new THREE.Color(0x00ffe5) },
      uCamPos: { value: new THREE.Vector3() },
    },
    vertexShader: oceanVert,
    fragmentShader: oceanFrag,
    transparent: true,
    side: THREE.DoubleSide,
  });
  const ocean = new THREE.Mesh(plane, oceanMat);
  ocean.position.y = 0;
  group.add(ocean);

  const sub = new THREE.Mesh(
    new THREE.PlaneGeometry(520, 520, 1, 1),
    new THREE.MeshStandardMaterial({
      color: 0x000d22,
      emissive: 0x000d22,
      emissiveIntensity: 0.4,
      transparent: true,
      opacity: 0.65,
      side: THREE.DoubleSide,
    }),
  );
  sub.rotation.x = -Math.PI / 2;
  sub.position.y = -2;
  group.add(sub);

  const floor = new THREE.Mesh(
    new THREE.PlaneGeometry(900, 900, 80, 80),
    new THREE.MeshStandardMaterial({
      color: 0x000408,
      emissive: 0x000408,
      metalness: 0.85,
      roughness: 0.12,
    }),
  );
  floor.rotation.x = -Math.PI / 2;
  floor.position.y = -20;
  group.add(floor);

  const ridge = new THREE.Mesh(
    new THREE.BoxGeometry(4, 0.4, 120),
    new THREE.MeshStandardMaterial({
      color: 0x001a33,
      emissive: 0x002244,
      emissiveIntensity: 0.08,
      transparent: true,
      opacity: 0.35,
    }),
  );
  ridge.position.set(15, -6, 0);
  group.add(ridge);

  return {
    group,
    ocean,
    oceanMat,
    subsurface: sub,
    floor,
    islands: [],
    ridge,
    dispose: () => {
      plane.dispose();
      oceanMat.dispose();
      sub.geometry.dispose();
      (sub.material as THREE.Material).dispose();
      floor.geometry.dispose();
      (floor.material as THREE.Material).dispose();
      ridge.geometry.dispose();
      (ridge.material as THREE.Material).dispose();
    },
  };
}

function islandPos(seed: number): THREE.Vector3 {
  const r = mulberry32(seed);
  return new THREE.Vector3((r() - 0.5) * 200, 0, (r() - 0.5) * 200);
}

export function ensureIslands(h: RamOceanHandles, apps: { packageName: string; appName: string; memoryBytes: number }[]): void {
  while (h.islands.length) {
    const g = h.islands.pop()!;
    h.group.remove(g);
    g.traverse((o) => {
      if (o instanceof THREE.Mesh) {
        o.geometry.dispose();
        const m = o.material as THREE.Material | THREE.Material[];
        if (Array.isArray(m)) m.forEach((x) => x.dispose());
        else m.dispose();
      }
    });
  }

  for (const app of apps) {
    const mb = app.memoryBytes / (1024 * 1024);
    const isSelf = app.packageName.includes('silicon') || app.appName.toLowerCase().includes('silicon');
    const rad = isSelf ? 6 + Math.sqrt(mb) * 0.15 : 14 + Math.sqrt(mb) * 0.12;
    const height = isSelf ? 6 + mb / 400 : 4 + mb / 500;
    const seed = hashString(app.packageName);
    const rng = mulberry32(seed);
    const p = islandPos(seed);
    const col = isSelf ? 0xcc44ff : 0x0a1420;
    const em = isSelf ? 0x441166 : 0x001020;
    const isl = makeIsland(rng, rad, height, col, em, p.x, p.z);
    h.group.add(isl);
    h.islands.push(isl);
  }
}

export function updateRamOcean(
  h: RamOceanHandles,
  ramPressurePct: number,
  lowMemory: boolean,
  time: number,
  camera: THREE.PerspectiveCamera,
): void {
  const pressure = Math.max(0, Math.min(100, ramPressurePct)) / 100;
  const waveH = 0.5 + pressure * 3.5 + (lowMemory ? 0.4 : 0);
  h.oceanMat.uniforms.uTime.value = time;
  h.oceanMat.uniforms.uWaveH.value = waveH;
  const wp = new THREE.Vector3();
  camera.getWorldPosition(wp);
  h.oceanMat.uniforms.uCamPos.value.copy(wp).sub(WORLD.ramOcean);

  h.islands.forEach((g) => {
    const bob = Math.sin(time * 1.2 + g.position.x * 0.01) * 0.08;
    g.position.y = bob;
  });
}
