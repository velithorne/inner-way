import * as THREE from 'three';
import { networkColour } from '../services/colourFromBssid';
import type { RouterEstimate } from '../services/routerTriangulator';
import type { WifiNetwork } from '../types/wifi';

/** 40 lines × 3 strands per network */
export const LINES_PER_NETWORK = 40;
const STRANDS = 3;
const OFFSET_STRAND = 0.015;
const SPREAD_W = 3.0;
const SPREAD_H = 5.0;
const SPAWN_D_MIN = 4.0;
const SPAWN_D_MAX = 6.0;
const LEN_MIN = 1.5;
const LEN_MAX = 3.0;
const RESET_NEAR = 0.35;

const STRAND_OPACITY = [0.7, 0.4, 0.4];

function bssidElevationRad(bssid: string): number {
  let h = 0;
  for (const c of bssid) h = (h * 31 + c.charCodeAt(0)) % 1000;
  return ((h % 17) - 8) * 0.04;
}

/**
 * 3D unit vector from router toward camera (world space).
 * Small per-BSSID elevation breaks coplanar "scan line" look on screen.
 */
function incomingDirection3D(bearingDeg: number, bssid: string): THREE.Vector3 {
  const b = (bearingDeg * Math.PI) / 180;
  const e = bssidElevationRad(bssid);
  const ce = Math.cos(e);
  const x = -ce * Math.sin(b);
  const y = Math.sin(e);
  const z = -ce * Math.cos(b);
  return new THREE.Vector3(x, y, z).normalize();
}

function buildSpawnBasis(incoming: THREE.Vector3): { right: THREE.Vector3; up: THREE.Vector3 } {
  const worldUp = new THREE.Vector3(0, 1, 0);
  let right = new THREE.Vector3().crossVectors(incoming, worldUp);
  if (right.lengthSq() < 1e-8) {
    right = new THREE.Vector3().crossVectors(incoming, new THREE.Vector3(1, 0, 0));
  }
  right.normalize();
  const up = new THREE.Vector3().crossVectors(right, incoming).normalize();
  return { right, up };
}

function speedFromRssi(rssi: number): number {
  const rssiNorm = Math.max(0, Math.min(1, (rssi + 100) / 70));
  return 0.012 + rssiNorm * 0.02;
}

/**
 * Segments lie along incomingDir (bearing + elevation); spawn plane ⟂ incomingDir.
 * Per-network colour; triple-strand offset in full 3D ⟂ segment.
 */
export class SignalLineStream {
  readonly group: THREE.Group;
  readonly bssid: string;
  private colour = new THREE.Color();
  private incomingDir = new THREE.Vector3();
  private right = new THREE.Vector3();
  private up = new THREE.Vector3();
  private readonly lines: THREE.LineSegments[];
  private speed = 0.02;

  /** start x,y,z, lineLen */
  private readonly state: Float32Array;

  constructor(net: WifiNetwork, est: RouterEstimate) {
    this.bssid = net.bssid;
    this.colour.copy(networkColour(net.bssid, net.frequency));
    this.rebuildBasisAndSpeed(net, est);

    this.group = new THREE.Group();
    this.group.renderOrder = 2;

    const n = LINES_PER_NETWORK;
    this.state = new Float32Array(n * 4);
    for (let i = 0; i < n; i++) {
      this.resetLine(i);
    }

    this.lines = [];
    const offMul = [-1, 0, 1];
    for (let s = 0; s < STRANDS; s++) {
      const geo = new THREE.BufferGeometry();
      const posArr = new Float32Array(n * 6);
      geo.setAttribute('position', new THREE.BufferAttribute(posArr, 3));
      const mat = new THREE.LineBasicMaterial({
        color: this.colour.clone(),
        transparent: true,
        opacity: STRAND_OPACITY[s],
        depthTest: false,
        depthWrite: false,
        blending: THREE.NormalBlending,
      });
      const line = new THREE.LineSegments(geo, mat);
      line.renderOrder = 2;
      line.frustumCulled = false;
      (line.userData as { off: number }).off = offMul[s] * OFFSET_STRAND;
      this.lines.push(line);
      this.group.add(line);
    }
  }

  private rebuildBasisAndSpeed(net: WifiNetwork, est: RouterEstimate) {
    this.incomingDir.copy(incomingDirection3D(est.bearing, net.bssid));
    const basis = buildSpawnBasis(this.incomingDir);
    this.right.copy(basis.right);
    this.up.copy(basis.up);
    this.speed = speedFromRssi(net.rssi);
  }

  syncNetwork(net: WifiNetwork, est: RouterEstimate) {
    this.colour.copy(networkColour(net.bssid, net.frequency));
    this.rebuildBasisAndSpeed(net, est);
    for (const line of this.lines) {
      (line.material as THREE.LineBasicMaterial).color.copy(this.colour);
    }
  }

  private resetLine(i: number) {
    const base = i * 4;
    const D = SPAWN_D_MIN + Math.random() * (SPAWN_D_MAX - SPAWN_D_MIN);
    const u = (Math.random() - 0.5) * SPREAD_W;
    const v = (Math.random() - 0.5) * SPREAD_H; // vertical spread on spawn plane
    const len = LEN_MIN + Math.random() * (LEN_MAX - LEN_MIN);

    const inc = this.incomingDir;
    const r = this.right;
    const up = this.up;
    const start = r
      .clone()
      .multiplyScalar(u)
      .add(up.clone().multiplyScalar(v))
      .add(inc.clone().multiplyScalar(-D));

    this.state[base] = start.x;
    this.state[base + 1] = start.y;
    this.state[base + 2] = start.z;
    this.state[base + 3] = len;
  }

  update(deltaSec: number) {
    const inc = this.incomingDir;
    const n = LINES_PER_NETWORK;
    const move = this.speed * deltaSec;

    for (let i = 0; i < n; i++) {
      const base = i * 4;
      let sx = this.state[base];
      let sy = this.state[base + 1];
      let sz = this.state[base + 2];
      const len = this.state[base + 3];

      sx += inc.x * move;
      sy += inc.y * move;
      sz += inc.z * move;

      const dist = Math.sqrt(sx * sx + sy * sy + sz * sz);
      if (dist < RESET_NEAR) {
        this.resetLine(i);
        sx = this.state[base];
        sy = this.state[base + 1];
        sz = this.state[base + 2];
      } else {
        this.state[base] = sx;
        this.state[base + 1] = sy;
        this.state[base + 2] = sz;
      }

      const ax = sx;
      const ay = sy;
      const az = sz;
      const bx = sx + inc.x * len;
      const by = sy + inc.y * len;
      const bz = sz + inc.z * len;

      const seg = new THREE.Vector3(bx - ax, by - ay, bz - az);
      if (seg.lengthSq() < 1e-12) continue;
      const dir = seg.normalize();
      let ribbon = new THREE.Vector3().crossVectors(dir, this.up);
      if (ribbon.lengthSq() < 1e-10) ribbon = new THREE.Vector3().crossVectors(dir, this.right);
      if (ribbon.lengthSq() < 1e-10) ribbon = new THREE.Vector3().crossVectors(dir, new THREE.Vector3(0, 1, 0));
      ribbon.normalize();

      for (let si = 0; si < STRANDS; si++) {
        const line = this.lines[si];
        const t = (line.userData as { off: number }).off;
        const side = ribbon.clone().multiplyScalar(t);
        const p0x = ax + side.x;
        const p0y = ay + side.y;
        const p0z = az + side.z;
        const p1x = bx + side.x;
        const p1y = by + side.y;
        const p1z = bz + side.z;

        const posAttr = line.geometry.attributes.position.array as Float32Array;
        const idx = i * 6;
        posAttr[idx] = p0x;
        posAttr[idx + 1] = p0y;
        posAttr[idx + 2] = p0z;
        posAttr[idx + 3] = p1x;
        posAttr[idx + 4] = p1y;
        posAttr[idx + 5] = p1z;
      }
    }

    for (let si = 0; si < STRANDS; si++) {
      const line = this.lines[si];
      const mat = line.material as THREE.LineBasicMaterial;
      mat.opacity = Math.min(1, STRAND_OPACITY[si]);
      mat.color.copy(this.colour);
      line.geometry.attributes.position.needsUpdate = true;
    }
  }

  dispose() {
    for (const line of this.lines) {
      line.geometry.dispose();
      (line.material as THREE.LineBasicMaterial).dispose();
    }
  }
}
