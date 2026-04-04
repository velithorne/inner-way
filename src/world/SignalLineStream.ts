import * as THREE from 'three';
import { networkColour } from '../services/colourFromBssid';
import type { RouterEstimate } from '../services/routerTriangulator';
import type { WifiNetwork } from '../types/wifi';
import { estimateToWorldPosition } from './space';

export const LINES_PER_NETWORK = 200;
const STRANDS = 3;
const OFFSET_STRAND = 0.008;
const FAR = 28;
const SEG_LEN_MIN = 3;
const SEG_LEN_MAX = 6;

function speedFromRssi(rssi: number): number {
  const norm = Math.max(0, Math.min(1, (rssi + 90) / 60));
  return (0.015 + norm * 0.025) * 60;
}

/**
 * Triple-strand field lines along router bearing (NormalBlending, on top of shells).
 */
export class SignalLineStream {
  readonly group: THREE.Group;
  readonly bssid: string;
  private readonly colour: THREE.Color;
  private readonly R: THREE.Vector3;
  private readonly perp: THREE.Vector3;
  private readonly bitan: THREE.Vector3;
  private readonly lines: THREE.LineSegments[];
  private rssiNorm = 0.5;
  private speed = 1.2;

  private readonly state: Float32Array;

  constructor(net: WifiNetwork, est: RouterEstimate) {
    this.bssid = net.bssid;
    this.colour = networkColour(net.bssid, net.frequency);
    const pos = estimateToWorldPosition(est, net.frequency);
    this.R = pos.lengthSq() > 1e-6 ? pos.clone().normalize() : new THREE.Vector3(0, 0, -1);
    const up = new THREE.Vector3(0, 1, 0);
    this.perp = new THREE.Vector3().crossVectors(this.R, up);
    if (this.perp.lengthSq() < 1e-6) this.perp.set(1, 0, 0);
    else this.perp.normalize();
    this.bitan = new THREE.Vector3().crossVectors(this.perp, this.R).normalize();

    const rssi = net.rssi;
    this.rssiNorm = Math.max(0, Math.min(1, (rssi + 90) / 60));
    this.speed = speedFromRssi(rssi);

    this.group = new THREE.Group();
    this.group.renderOrder = 2;

    const n = LINES_PER_NETWORK;
    this.state = new Float32Array(n * 4);
    for (let i = 0; i < n; i++) {
      const b = i * 4;
      this.state[b] = FAR + Math.random() * 8;
      this.state[b + 1] = SEG_LEN_MIN + Math.random() * (SEG_LEN_MAX - SEG_LEN_MIN);
      this.state[b + 2] = (Math.random() - 0.5) * 14;
      this.state[b + 3] = (Math.random() - 0.5) * 14;
    }

    this.lines = [];
    const opacities = [0.45, 0.25, 0.25];
    const offMul = [-1, 0, 1];
    for (let s = 0; s < STRANDS; s++) {
      const geo = new THREE.BufferGeometry();
      const posArr = new Float32Array(n * 6);
      geo.setAttribute('position', new THREE.BufferAttribute(posArr, 3));
      const mat = new THREE.LineBasicMaterial({
        color: this.colour.clone(),
        transparent: true,
        opacity: opacities[s],
        depthTest: false,
        depthWrite: false,
        blending: THREE.NormalBlending,
      });
      const line = new THREE.LineSegments(geo, mat);
      line.renderOrder = 2;
      line.frustumCulled = false;
      (line.userData as { strand: number; off: number }).strand = s;
      (line.userData as { off: number }).off = offMul[s] * OFFSET_STRAND;
      this.lines.push(line);
      this.group.add(line);
    }
  }

  syncNetwork(net: WifiNetwork) {
    this.colour.copy(networkColour(net.bssid, net.frequency));
    this.rssiNorm = Math.max(0, Math.min(1, (net.rssi + 90) / 60));
    this.speed = speedFromRssi(net.rssi);
  }

  update(deltaSec: number) {
    const R = this.R;
    const perp = this.perp;
    const bitan = this.bitan;
    const n = LINES_PER_NETWORK;

    for (let i = 0; i < n; i++) {
      const base = i * 4;
      let s = this.state[base];
      const segLen = this.state[base + 1];
      const ox = this.state[base + 2];
      const oy = this.state[base + 3];

      s -= this.speed * deltaSec;
      if (s < segLen * 0.15) {
        s = FAR + Math.random() * 10;
        this.state[base + 1] = SEG_LEN_MIN + Math.random() * (SEG_LEN_MAX - SEG_LEN_MIN);
        this.state[base + 2] = (Math.random() - 0.5) * 14;
        this.state[base + 3] = (Math.random() - 0.5) * 14;
      }
      this.state[base] = s;

      const diskOff = perp.clone().multiplyScalar(ox).add(bitan.clone().multiplyScalar(oy));

      for (let si = 0; si < STRANDS; si++) {
        const line = this.lines[si];
        const off = (line.userData as { off: number }).off;
        const side = perp.clone().multiplyScalar(off);
        const p0 = R.clone()
          .multiplyScalar(s)
          .add(diskOff)
          .add(side);
        const p1 = p0.clone().addScaledVector(R, -segLen);

        const posAttr = line.geometry.attributes.position.array as Float32Array;
        const idx = i * 6;
        posAttr[idx] = p0.x;
        posAttr[idx + 1] = p0.y;
        posAttr[idx + 2] = p0.z;
        posAttr[idx + 3] = p1.x;
        posAttr[idx + 4] = p1.y;
        posAttr[idx + 5] = p1.z;
      }
    }

    const bright = 0.75 + 0.25 * this.rssiNorm;
    for (let si = 0; si < STRANDS; si++) {
      const line = this.lines[si];
      const mat = line.material as THREE.LineBasicMaterial;
      const baseOp = [0.45, 0.25, 0.25][si];
      mat.opacity = Math.min(1, baseOp * bright);
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
