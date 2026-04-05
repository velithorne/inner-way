import * as THREE from 'three';
import { networkColour } from '../services/colourFromBssid';
import type { RouterEstimate } from '../services/routerTriangulator';
import type { WifiNetwork } from '../types/wifi';

/** Max segments per stream (proximity burst uses full allocation) */
export const MAX_LINES_PER_NETWORK = 75;
const BASE_LINES = 25;
const SPREAD_W = 2.0;
const SPREAD_H = 3.0;
const SPAWN_D_MIN = 4.0;
const SPAWN_D_MAX = 6.0;
const LEN_MIN = 1.5;
const LEN_MAX = 3.0;
const RESET_NEAR = 0.35;

const PROXIMITY_RSSI = -45;
const FORWARD_OVERRIDE_RSSI = -40;

function bssidElevationRad(bssid: string): number {
  let h = 0;
  for (const c of bssid) h = (h * 31 + c.charCodeAt(0)) % 1000;
  return ((h % 17) - 8) * 0.04;
}

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

export type LineStreamOptions = {
  proximityBurst: boolean;
};

/**
 * Single-strand + glow duplicate (additive, offset) for perf.
 * Proximity: 3× lines, 2.5× speed, 1.8× opacity; forward override when RSSI > -40 dBm.
 */
export class SignalLineStream {
  readonly group: THREE.Group;
  readonly bssid: string;
  private colour = new THREE.Color();
  private incomingDir = new THREE.Vector3();
  private right = new THREE.Vector3();
  private up = new THREE.Vector3();
  private speed = 0.02;
  private lineCount = BASE_LINES;
  private proximityBurst = false;
  private forwardOverride = false;

  private readonly mainLine: THREE.LineSegments;
  private readonly glowLine: THREE.LineSegments;
  private readonly state: Float32Array;
  private phase = 0;

  constructor(net: WifiNetwork, est: RouterEstimate, options: LineStreamOptions) {
    this.bssid = net.bssid;
    this.colour.copy(networkColour(net.bssid, net.frequency));
    this.applyOptions(net, options);
    this.rebuildBasisAndSpeed(net, est);

    this.group = new THREE.Group();
    this.group.renderOrder = 2;

    const n = MAX_LINES_PER_NETWORK;
    this.state = new Float32Array(n * 4);
    for (let i = 0; i < n; i++) {
      this.resetLine(i);
    }

    const geoMain = new THREE.BufferGeometry();
    geoMain.setAttribute('position', new THREE.BufferAttribute(new Float32Array(n * 6), 3));
    const matMain = new THREE.LineBasicMaterial({
      color: this.colour.clone(),
      transparent: true,
      opacity: 0.55,
      depthTest: false,
      depthWrite: false,
      blending: THREE.AdditiveBlending,
    });
    this.mainLine = new THREE.LineSegments(geoMain, matMain);
    this.mainLine.renderOrder = 2;
    this.mainLine.frustumCulled = false;

    const geoGlow = new THREE.BufferGeometry();
    geoGlow.setAttribute('position', new THREE.BufferAttribute(new Float32Array(n * 6), 3));
    const matGlow = new THREE.LineBasicMaterial({
      color: this.colour.clone(),
      transparent: true,
      opacity: 0.2,
      depthTest: false,
      depthWrite: false,
      blending: THREE.AdditiveBlending,
    });
    this.glowLine = new THREE.LineSegments(geoGlow, matGlow);
    this.glowLine.renderOrder = 2;
    this.glowLine.frustumCulled = false;

    this.group.add(this.mainLine);
    this.group.add(this.glowLine);
  }

  private applyOptions(net: WifiNetwork, options: LineStreamOptions) {
    this.proximityBurst = options.proximityBurst && net.rssi > PROXIMITY_RSSI;
    this.lineCount = this.proximityBurst ? MAX_LINES_PER_NETWORK : BASE_LINES;
    this.forwardOverride = net.rssi > FORWARD_OVERRIDE_RSSI;
  }

  private rebuildBasisAndSpeed(net: WifiNetwork, est: RouterEstimate) {
    if (this.forwardOverride) {
      this.incomingDir.set(0, 0, -1);
    } else {
      this.incomingDir.copy(incomingDirection3D(est.bearing, net.bssid));
    }
    const basis = buildSpawnBasis(this.incomingDir);
    this.right.copy(basis.right);
    this.up.copy(basis.up);
    let sp = speedFromRssi(net.rssi);
    if (this.proximityBurst) sp *= 2.5;
    this.speed = sp;
  }

  syncNetwork(net: WifiNetwork, est: RouterEstimate, options: LineStreamOptions) {
    this.colour.copy(networkColour(net.bssid, net.frequency));
    this.applyOptions(net, options);
    this.rebuildBasisAndSpeed(net, est);
    (this.mainLine.material as THREE.LineBasicMaterial).color.copy(this.colour);
    (this.glowLine.material as THREE.LineBasicMaterial).color.copy(this.colour);
  }

  private resetLine(i: number) {
    const base = i * 4;
    const D = SPAWN_D_MIN + Math.random() * (SPAWN_D_MAX - SPAWN_D_MIN);
    const u = (Math.random() - 0.5) * SPREAD_W;
    const v = (Math.random() - 0.5) * SPREAD_H;
    const len = LEN_MIN + Math.random() * (LEN_MAX - LEN_MIN);

    const inc = this.incomingDir;
    const start = this.right
      .clone()
      .multiplyScalar(u)
      .add(this.up.clone().multiplyScalar(v))
      .add(inc.clone().multiplyScalar(-D));

    this.state[base] = start.x;
    this.state[base + 1] = start.y;
    this.state[base + 2] = start.z;
    this.state[base + 3] = len;
  }

  update(deltaSec: number) {
    this.phase += deltaSec * 8;
    const inc = this.incomingDir;
    const nActive = this.lineCount;
    const move = this.speed * deltaSec;
    const opMul = this.proximityBurst ? 1.8 : 1;
    const wobble = Math.sin(this.phase) * 0.006;

    for (let i = 0; i < nActive; i++) {
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

      const offMain = ribbon.clone().multiplyScalar(wobble);
      const offGlow = ribbon.clone().multiplyScalar(0.012 + wobble);

      const posMain = this.mainLine.geometry.attributes.position.array as Float32Array;
      const posGlow = this.glowLine.geometry.attributes.position.array as Float32Array;
      const idx = i * 6;
      posMain[idx] = ax + offMain.x;
      posMain[idx + 1] = ay + offMain.y;
      posMain[idx + 2] = az + offMain.z;
      posMain[idx + 3] = bx + offMain.x;
      posMain[idx + 4] = by + offMain.y;
      posMain[idx + 5] = bz + offMain.z;
      posGlow[idx] = ax + offGlow.x;
      posGlow[idx + 1] = ay + offGlow.y;
      posGlow[idx + 2] = az + offGlow.z;
      posGlow[idx + 3] = bx + offGlow.x;
      posGlow[idx + 4] = by + offGlow.y;
      posGlow[idx + 5] = bz + offGlow.z;
    }

    const mMain = this.mainLine.material as THREE.LineBasicMaterial;
    const mGlow = this.glowLine.material as THREE.LineBasicMaterial;
    mMain.opacity = Math.min(1, 0.55 * opMul);
    mGlow.opacity = Math.min(1, 0.3 * opMul);
    mMain.color.copy(this.colour);
    mGlow.color.copy(this.colour);
    const posMain = this.mainLine.geometry.attributes.position.array as Float32Array;
    const posGlow = this.glowLine.geometry.attributes.position.array as Float32Array;
    for (let i = nActive; i < MAX_LINES_PER_NETWORK; i++) {
      const idx = i * 6;
      for (let k = 0; k < 6; k++) {
        posMain[idx + k] = 0;
        posGlow[idx + k] = 0;
      }
    }
    this.mainLine.geometry.attributes.position.needsUpdate = true;
    this.glowLine.geometry.attributes.position.needsUpdate = true;
  }

  dispose() {
    this.mainLine.geometry.dispose();
    (this.mainLine.material as THREE.LineBasicMaterial).dispose();
    this.glowLine.geometry.dispose();
    (this.glowLine.material as THREE.LineBasicMaterial).dispose();
  }
}
