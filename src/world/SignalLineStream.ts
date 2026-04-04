import * as THREE from 'three';
import type { RouterEstimate } from '../services/routerTriangulator';
import type { WifiNetwork } from '../types/wifi';

/** Per network; 3 strands × 60 */
export const LINES_PER_NETWORK = 60;
const STRANDS = 3;
/** Thick-line fake: offset along perpendicular to segment (horizontal) */
const OFFSET_STRAND = 0.015;
const SPREAD_W = 3.0;
const SPREAD_H = 4.0;
const SPAWN_MIN = 4.0;
const SPAWN_MAX = 6.0;
const LEN_MIN = 2.0;
const LEN_MAX = 4.0;
const RESET_NEAR = 0.35;

const LINE_GREEN = 0x00ff44;
const STRAND_OPACITY = [0.7, 0.4, 0.4];

/** Unit vector from router (on horizon at compass bearing) toward camera at origin. */
function streamDirectionFromBearingDeg(bearingDeg: number): THREE.Vector3 {
  const b = (bearingDeg * Math.PI) / 180;
  return new THREE.Vector3(-Math.sin(b), 0, -Math.cos(b)).normalize();
}

function speedFromRssi(rssi: number): number {
  const rssiNorm = Math.max(0, Math.min(1, (rssi + 100) / 70));
  return 0.012 + rssiNorm * 0.02;
}

/**
 * Lines spawn far along router bearing and flow along streamDir toward the camera.
 * Fixed bright lime green; triple-strand thickness (perp to segment).
 */
export class SignalLineStream {
  readonly group: THREE.Group;
  readonly bssid: string;
  private streamDir = new THREE.Vector3();
  private perp1 = new THREE.Vector3();
  private perp2 = new THREE.Vector3(0, 1, 0);
  private readonly lines: THREE.LineSegments[];
  private speed = 0.02;

  /** Per line: startPos x,y,z, lineLen */
  private readonly state: Float32Array;

  constructor(net: WifiNetwork, est: RouterEstimate) {
    this.bssid = net.bssid;
    this.updateBasisAndSpeed(est.bearing, net.rssi);

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
        color: LINE_GREEN,
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

  private updateBasisAndSpeed(bearingDeg: number, rssi: number) {
    this.streamDir.copy(streamDirectionFromBearingDeg(bearingDeg));
    const sd = this.streamDir;
    this.perp1.set(sd.z, 0, -sd.x);
    if (this.perp1.lengthSq() < 1e-8) this.perp1.set(1, 0, 0);
    else this.perp1.normalize();
    this.speed = speedFromRssi(rssi);
  }

  syncNetwork(net: WifiNetwork, est: RouterEstimate) {
    this.updateBasisAndSpeed(est.bearing, net.rssi);
    for (const line of this.lines) {
      const mat = line.material as THREE.LineBasicMaterial;
      mat.color.set(LINE_GREEN);
    }
  }

  private resetLine(i: number) {
    const base = i * 4;
    const D = SPAWN_MIN + Math.random() * (SPAWN_MAX - SPAWN_MIN);
    const u = (Math.random() - 0.5) * SPREAD_W;
    const v = (Math.random() - 0.5) * SPREAD_H;
    const len = LEN_MIN + Math.random() * (LEN_MAX - LEN_MIN);

    const offset = this.perp1.clone().multiplyScalar(u).add(this.perp2.clone().multiplyScalar(v));
    const start = this.streamDir
      .clone()
      .multiplyScalar(-D)
      .add(offset);
    this.state[base] = start.x;
    this.state[base + 1] = start.y;
    this.state[base + 2] = start.z;
    this.state[base + 3] = len;
  }

  update(deltaSec: number) {
    const sd = this.streamDir;
    const n = LINES_PER_NETWORK;
    const move = this.speed * deltaSec;

    for (let i = 0; i < n; i++) {
      const base = i * 4;
      let sx = this.state[base];
      let sy = this.state[base + 1];
      let sz = this.state[base + 2];
      const len = this.state[base + 3];

      sx += sd.x * move;
      sy += sd.y * move;
      sz += sd.z * move;

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
      const bx = sx + sd.x * len;
      const by = sy + sd.y * len;
      const bz = sz + sd.z * len;

      const dir = new THREE.Vector3(bx - ax, by - ay, bz - az);
      if (dir.lengthSq() < 1e-10) continue;
      dir.normalize();
      const perpLine = new THREE.Vector3(-dir.z, 0, dir.x);
      if (perpLine.lengthSq() < 1e-10) perpLine.set(0, 1, 0);
      else perpLine.normalize();

      for (let si = 0; si < STRANDS; si++) {
        const line = this.lines[si];
        const t = (line.userData as { off: number }).off;
        const side = perpLine.clone().multiplyScalar(t);
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
      mat.color.set(LINE_GREEN);
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
