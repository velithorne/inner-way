import * as THREE from 'three';
import { networkColour } from '../services/colourFromBssid';
import { rssiToDistance } from '../services/rssiToDistance';
import type { WifiNetwork } from '../types/wifi';

const RINGS_PER_NETWORK = 6;
const THETA_SEGMENTS = 48;

export function baseOpacityFromRssi(rssi: number): number {
  const t = Math.max(0, Math.min(1, (rssi + 90) / 60));
  return 0.08 + t * 0.52;
}

export function ringSpacingForFrequency(freqMHz: number): number {
  return freqMHz > 4000 ? 1.2 : 2.0;
}

export function placeholderEmitterPosition(bssid: string): THREE.Vector3 {
  const hex = bssid.replace(/:/g, '');
  let h = 0;
  for (let i = 0; i < hex.length; i++) {
    h = (h * 31 + hex.charCodeAt(i)) % 9973;
  }
  const ang = (h / 9973) * Math.PI * 2;
  const rad = 0.8 + (h % 100) / 100;
  return new THREE.Vector3(Math.cos(ang) * rad, Math.sin(ang) * rad * 0.6, -4.2);
}

/**
 * Expanding additive wave rings per WiFi network (Phase 2).
 * Rings reuse one RingGeometry; scale + opacity animate each frame.
 */
export class WaveEmitter {
  readonly bssid: string;
  readonly group: THREE.Group;
  readonly colour: THREE.Color;
  maxRadius: number;
  baseOpacity: number;
  readonly ringSpacing: number;
  readonly phase: number;
  private readonly rings: THREE.Mesh[];
  private readonly ringGeometry: THREE.RingGeometry;

  constructor(net: WifiNetwork) {
    this.bssid = net.bssid;
    this.colour = networkColour(net.bssid, net.frequency);
    this.maxRadius = Math.max(1.2, Math.min(40, rssiToDistance(net.rssi, net.frequency)));
    this.baseOpacity = baseOpacityFromRssi(net.rssi);
    this.ringSpacing = ringSpacingForFrequency(net.frequency);
    let h = 0;
    for (const c of net.bssid) {
      h = (h * 31 + c.charCodeAt(0)) % 1000;
    }
    this.phase = h / 1000;

    this.group = new THREE.Group();
    this.group.position.copy(placeholderEmitterPosition(net.bssid));

    this.ringGeometry = new THREE.RingGeometry(0.96, 1.0, THETA_SEGMENTS);
    this.rings = [];
    for (let i = 0; i < RINGS_PER_NETWORK; i++) {
      const mat = new THREE.MeshBasicMaterial({
        color: this.colour.clone(),
        transparent: true,
        opacity: 0.35,
        blending: THREE.AdditiveBlending,
        depthWrite: false,
        side: THREE.DoubleSide,
      });
      const mesh = new THREE.Mesh(this.ringGeometry, mat);
      mesh.rotation.x = -Math.PI / 2;
      this.rings.push(mesh);
      this.group.add(mesh);
    }
  }

  update(timeSec: number) {
    const maxR = this.maxRadius;
    const spacing = this.ringSpacing;
    const speed = 1.0;
    const cycle = (maxR - 0.5) / speed;

    for (let k = 0; k < this.rings.length; k++) {
      const mesh = this.rings[k];
      const mat = mesh.material as THREE.MeshBasicMaterial;
      const offset = (k * spacing) / Math.max(maxR, 0.01);
      const u = (timeSec * speed + this.phase * cycle + offset) % cycle;
      const t = u / cycle;
      const rMid = 0.5 + t * (maxR - 0.5);
      mesh.scale.set(rMid, rMid, rMid);
      mesh.rotation.x = -Math.PI / 2;
      const fade = 1 - t;
      mat.opacity = fade * this.baseOpacity;
      mat.color.copy(this.colour);
    }
  }

  syncNetwork(net: WifiNetwork) {
    this.colour.copy(networkColour(net.bssid, net.frequency));
    this.maxRadius = Math.max(1.2, Math.min(40, rssiToDistance(net.rssi, net.frequency)));
    this.baseOpacity = baseOpacityFromRssi(net.rssi);
  }

  dispose() {
    this.ringGeometry.dispose();
    for (const m of this.rings) {
      (m.material as THREE.MeshBasicMaterial).dispose();
    }
  }
}
