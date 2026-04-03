import * as THREE from 'three';

import { WORLD, hashString, mulberry32 } from './worldConstants';

export type StorageMountainsHandles = {
  group: THREE.Group;
  lod: THREE.LOD;
  photoPeakWorld: THREE.Vector3;
  photoSprite: THREE.Sprite;
  photoAntennas: THREE.Mesh[];
  rocks: THREE.InstancedMesh;
  dispose: () => void;
};

const LAYER_STOPS: { t: number; c: THREE.Color }[] = [
  { t: 0, c: new THREE.Color(0x0d0d1a) },
  { t: 0.15, c: new THREE.Color(0x1a1044) },
  { t: 0.35, c: new THREE.Color(0x2d1a00) },
  { t: 0.55, c: new THREE.Color(0x001a22) },
  { t: 0.7, c: new THREE.Color(0x2a1500) },
  { t: 0.9, c: new THREE.Color(0x334455) },
  { t: 1, c: new THREE.Color(0x334455) },
];

function layerColor(normalizedY: number): THREE.Color {
  const t = Math.max(0, Math.min(1, normalizedY));
  for (let i = 0; i < LAYER_STOPS.length - 1; i++) {
    const a = LAYER_STOPS[i];
    const b = LAYER_STOPS[i + 1];
    if (t <= b.t) {
      const u = (t - a.t) / Math.max(1e-6, b.t - a.t);
      return a.c.clone().lerp(b.c, u);
    }
  }
  return LAYER_STOPS[LAYER_STOPS.length - 1].c.clone();
}

function buildOrganicMountain(
  rng: () => number,
  ringSeg: number,
  baseR: number,
  height: number,
  peakJitter: THREE.Vector3,
): THREE.BufferGeometry {
  const basePts: THREE.Vector3[] = [];
  const midPts: THREE.Vector3[] = [];
  const topPts: THREE.Vector3[] = [];

  for (let i = 0; i < ringSeg; i++) {
    const a = (i / ringSeg) * Math.PI * 2;
    const wobble = 1 + (rng() - 0.5) * 0.12;
    basePts.push(
      new THREE.Vector3(Math.cos(a) * baseR * wobble, 0, Math.sin(a) * baseR * wobble),
    );
  }
  const midR = baseR * (0.48 + (rng() - 0.5) * 0.15);
  const midY = height * 0.42;
  for (let i = 0; i < ringSeg; i++) {
    const a = (i / ringSeg) * Math.PI * 2;
    const wobble = 1 + (rng() - 0.5) * 0.18;
    midPts.push(
      new THREE.Vector3(Math.cos(a) * midR * wobble, midY, Math.sin(a) * midR * wobble),
    );
  }
  const topR = baseR * (0.08 + rng() * 0.06);
  const tip = new THREE.Vector3(peakJitter.x, height, peakJitter.z);
  for (let i = 0; i < ringSeg; i++) {
    const a = (i / ringSeg) * Math.PI * 2;
    topPts.push(
      new THREE.Vector3(
        tip.x + Math.cos(a) * topR,
        tip.y,
        tip.z + Math.sin(a) * topR,
      ),
    );
  }

  const vertices: number[] = [];
  const colors: number[] = [];
  const indices: number[] = [];

  const pushRing = (pts: THREE.Vector3[]) => {
    for (const p of pts) {
      vertices.push(p.x, p.y, p.z);
      const col = layerColor(p.y / height);
      colors.push(col.r, col.g, col.b);
    }
  };

  pushRing(basePts);
  pushRing(midPts);
  pushRing(topPts);

  const n = ringSeg;
  const triStrip = (a: number, b: number) => {
    for (let i = 0; i < n; i++) {
      const iNext = (i + 1) % n;
      indices.push(a + i, b + i, a + iNext);
      indices.push(a + iNext, b + i, b + iNext);
    }
  };

  triStrip(0, n);
  triStrip(n, n * 2);

  const capCenter = vertices.length / 3;
  vertices.push(tip.x, tip.y, tip.z);
  colors.push(0.25, 0.14, 0.08);
  for (let i = 0; i < n; i++) {
    const iNext = (i + 1) % n;
    indices.push(capCenter, n * 2 + i, n * 2 + iNext);
  }

  const geo = new THREE.BufferGeometry();
  geo.setAttribute('position', new THREE.Float32BufferAttribute(vertices, 3));
  geo.setAttribute('color', new THREE.Float32BufferAttribute(colors, 3));
  geo.setIndex(indices);
  geo.computeVertexNormals();
  return geo;
}

function makeRockInstancedMesh(rng: () => number, peaks: { x: number; z: number; h: number }[]): THREE.InstancedMesh {
  const box = new THREE.BoxGeometry(1, 1, 1);
  const mat = new THREE.MeshStandardMaterial({
    color: 0x2a2038,
    emissive: 0x0a0a12,
    emissiveIntensity: 0.08,
    roughness: 0.9,
    metalness: 0.15,
  });
  const count = 1000;
  const inst = new THREE.InstancedMesh(box, mat, count);
  inst.castShadow = false;
  const m = new THREE.Matrix4();
  const pos = new THREE.Vector3();
  const quat = new THREE.Quaternion();
  const sc = new THREE.Vector3();
  for (let i = 0; i < count; i++) {
    const pk = peaks[Math.floor(rng() * peaks.length)];
    const ang = rng() * Math.PI * 2;
    const rad = pk.h * 0.25 + rng() * pk.h * 0.35;
    const lx = Math.cos(ang) * rad * 0.35;
    const lz = Math.sin(ang) * rad * 0.35;
    const ly = 1 + rng() * pk.h * 0.88;
    pos.set(pk.x + lx, ly, pk.z + lz);
    quat.setFromEuler(new THREE.Euler(rng() * 6, rng() * 6, rng() * 6));
    const s = 0.3 + rng() * 0.5;
    sc.set(s, s * (0.6 + rng() * 0.8), s);
    m.compose(pos, quat, sc);
    inst.setMatrixAt(i, m);
  }
  inst.instanceMatrix.needsUpdate = true;
  return inst;
}

function makePhotoSprite(): THREE.Sprite {
  const size = 64;
  const data = new Uint8Array(size * size * 4);
  for (let y = 0; y < size; y++) {
    for (let x = 0; x < size; x++) {
      const i = (y * size + x) * 4;
      const cx = x - size / 2;
      const cy = y - size / 2;
      const d = Math.hypot(cx, cy);
      if (d < 26 && d > 20) {
        data[i] = 255;
        data[i + 1] = 180;
        data[i + 2] = 100;
        data[i + 3] = 255;
      } else if (d <= 20) {
        data[i] = 30;
        data[i + 1] = 40;
        data[i + 2] = 55;
        data[i + 3] = 255;
      } else {
        data[i] = data[i + 1] = data[i + 2] = data[i + 3] = 0;
      }
    }
  }
  const tex = new THREE.DataTexture(data, size, size, THREE.RGBAFormat);
  tex.needsUpdate = true;
  const mat = new THREE.SpriteMaterial({
    map: tex,
    transparent: true,
    opacity: 0.95,
  });
  const spr = new THREE.Sprite(mat);
  spr.scale.set(14, 14, 1);
  return spr;
}

export function createStorageMountains(): StorageMountainsHandles {
  const group = new THREE.Group();
  group.position.copy(WORLD.storage);

  const deviceId = 'silicon-device';
  const seed = hashString(deviceId);
  const rng = mulberry32(seed ^ 0x5f3759df);

  const count = 18;
  const footprints: { x: number; z: number; baseR: number; h: number; photo: boolean }[] = [];
  let maxH = 0;
  let photoIdx = 0;
  const baseFill = 0.85;

  for (let i = 0; i < count; i++) {
    const baseR = 8 + rng() * 18;
    const h = baseFill * 55 + rng() * 20;
    const x = (i / Math.max(1, count - 1) - 0.5) * 300 + (rng() - 0.5) * 24;
    const z = (rng() - 0.5) * 40;
    footprints.push({ x, z, baseR, h, photo: false });
    if (h > maxH) {
      maxH = h;
      photoIdx = i;
    }
  }
  footprints[photoIdx].photo = true;
  footprints[photoIdx].h *= 1.12;

  const photoPeakWorld = new THREE.Vector3(
    WORLD.storage.x + footprints[photoIdx].x,
    footprints[photoIdx].h * 0.95,
    WORLD.storage.z + footprints[photoIdx].z,
  );

  const peakLayouts = footprints.map((fp) => ({ x: fp.x, z: fp.z, h: fp.h }));

  const buildLodLevel = (ringSeg: number): THREE.Group => {
    const g = new THREE.Group();
    footprints.forEach((fp) => {
      const jitter = new THREE.Vector3((rng() - 0.5) * 5, 0, (rng() - 0.5) * 5);
      const geo = buildOrganicMountain(rng, ringSeg, fp.baseR, fp.h, jitter);
      const mat = new THREE.MeshStandardMaterial({
        vertexColors: true,
        color: 0x1a3322,
        emissive: 0x0a1a11,
        emissiveIntensity: 0.2,
        roughness: 0.7,
        metalness: 0.3,
      });
      if (fp.photo) {
        mat.emissive = new THREE.Color(0x2a1810);
        mat.emissiveIntensity = 0.28;
      }
      const mesh = new THREE.Mesh(geo, mat);
      mesh.position.set(fp.x, 0, fp.z);
      mesh.castShadow = true;
      mesh.receiveShadow = true;
      g.add(mesh);
    });
    return g;
  };

  const high = buildLodLevel(28);
  const mid = buildLodLevel(14);
  const low = buildLodLevel(8);

  const lod = new THREE.LOD();
  lod.addLevel(high, 0);
  lod.addLevel(mid, 55);
  lod.addLevel(low, 150);

  group.add(lod);

  const rocks = makeRockInstancedMesh(rng, peakLayouts);
  group.add(rocks);

  const photoSprite = makePhotoSprite();
  photoSprite.position.set(
    footprints[photoIdx].x,
    footprints[photoIdx].h + 8,
    footprints[photoIdx].z,
  );
  group.add(photoSprite);

  const fpPhoto = footprints[photoIdx];
  const photoAntennas: THREE.Mesh[] = [];
  for (let a = 0; a < 5; a++) {
    const ant = new THREE.Mesh(
      new THREE.CylinderGeometry(0.15, 0.2, 3 + rng() * 4, 6),
      new THREE.MeshStandardMaterial({
        color: 0x1a1020,
        emissive: 0xff8844,
        emissiveIntensity: 0.15,
        metalness: 0.85,
        roughness: 0.12,
      }),
    );
    const ang = (a / 5) * Math.PI * 2;
    ant.position.set(
      fpPhoto.x + Math.cos(ang) * (fpPhoto.baseR * 0.35),
      fpPhoto.h + 1.5,
      fpPhoto.z + Math.sin(ang) * (fpPhoto.baseR * 0.35),
    );
    group.add(ant);
    photoAntennas.push(ant);
  }

  return {
    group,
    lod,
    photoPeakWorld,
    photoSprite,
    photoAntennas,
    rocks,
    dispose: () => {
      const disposeGroup = (gg: THREE.Group) => {
        gg.traverse((o) => {
          if (o instanceof THREE.Mesh) {
            o.geometry.dispose();
            const mat = o.material as THREE.MeshStandardMaterial | THREE.MeshBasicMaterial;
            mat.dispose?.();
          }
        });
      };
      disposeGroup(high);
      disposeGroup(mid);
      disposeGroup(low);
      rocks.geometry.dispose();
      (rocks.material as THREE.Material).dispose();
      const sm = photoSprite.material as THREE.SpriteMaterial;
      sm.map?.dispose();
      sm.dispose();
      photoAntennas.forEach((ant: THREE.Mesh) => {
        ant.geometry.dispose();
        (ant.material as THREE.Material).dispose();
      });
    },
  };
}

export function updateStorageMountains(
  h: StorageMountainsHandles,
  usedFrac: number,
  appDataFrac: number,
  camera: THREE.PerspectiveCamera,
): void {
  h.lod.update(camera);
  h.photoSprite.lookAt(camera.position.clone().sub(WORLD.storage));
  ;(h.photoSprite.material as THREE.SpriteMaterial).opacity =
    0.75 + usedFrac * 0.2 + appDataFrac * 0.05;
}
