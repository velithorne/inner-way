import * as THREE from 'three';
import { networkColour } from '../services/colourFromBssid';
import { rssiToDistance } from '../services/rssiToDistance';
import type { WifiNetwork } from '../types/wifi';

const SHELLS = 5;
const SHELL_OFFSETS = [0, 0.2, 0.4, 0.6, 0.8];

/** Spec: -30 dBm → 0.5, -90 → 0.08 */
export function shellBaseOpacity(rssi: number): number {
  const t = Math.max(0, Math.min(1, (rssi + 90) / 60));
  return 0.08 + t * 0.42;
}

function sphereSegments(freqMHz: number): [number, number] {
  return freqMHz > 4000 ? [16, 12] : [12, 6];
}

export class WaveEmitter {
  readonly bssid: string;
  readonly group: THREE.Group;
  readonly colour: THREE.Color;
  private readonly shells: THREE.Mesh[];
  private readonly geometries: THREE.SphereGeometry[];
  private readonly anchor: THREE.Mesh | null;
  maxRadius: number;
  baseOpacity: number;
  private wavePhase = 0;
  private rssiNorm = 0.5;
  readonly currentPos: THREE.Vector3;
  private readonly targetPos: THREE.Vector3;
  confidence: number;
  private highlight = 1;

  constructor(
    net: WifiNetwork,
    options: {
      position: THREE.Vector3;
      bearingRad: number;
      confidence: number;
    }
  ) {
    this.bssid = net.bssid;
    this.colour = networkColour(net.bssid, net.frequency);
    this.confidence = options.confidence;
    const rawMax = rssiToDistance(net.rssi, net.frequency);
    this.maxRadius = Math.max(0.15, rawMax * 0.08);
    this.baseOpacity = shellBaseOpacity(net.rssi);
    this.rssiNorm = Math.max(0, Math.min(1, (net.rssi + 90) / 60));

    this.currentPos = options.position.clone();
    this.targetPos = options.position.clone();

    this.group = new THREE.Group();
    this.group.position.copy(this.currentPos);
    this.group.rotation.y = options.bearingRad;

    const [wSeg, hSeg] = sphereSegments(net.frequency);
    this.geometries = [];
    this.shells = [];
    for (let i = 0; i < SHELLS; i++) {
      const geo = new THREE.SphereGeometry(1, wSeg, hSeg);
      this.geometries.push(geo);
      const mat = new THREE.MeshBasicMaterial({
        color: this.colour.clone(),
        transparent: true,
        opacity: 0.2,
        wireframe: true,
        side: THREE.FrontSide,
        blending: THREE.AdditiveBlending,
        depthWrite: false,
      });
      const mesh = new THREE.Mesh(geo, mat);
      mesh.scale.setScalar(0.1);
      this.shells.push(mesh);
      this.group.add(mesh);
    }

    const ag = new THREE.SphereGeometry(0.06, 8, 8);
    const am = new THREE.MeshBasicMaterial({
      color: 0xffffff,
      transparent: true,
      opacity: 0.9,
      blending: THREE.AdditiveBlending,
      depthWrite: false,
    });
    const anchorMesh = new THREE.Mesh(ag, am);
    anchorMesh.visible = this.confidence > 60;
    this.group.add(anchorMesh);
    this.anchor = anchorMesh;
  }

  setTargetPosition(pos: THREE.Vector3) {
    this.targetPos.copy(pos);
  }

  setConfidence(c: number) {
    this.confidence = c;
    if (this.anchor) this.anchor.visible = c > 60;
  }

  setHighlight(on: boolean) {
    this.highlight = on ? 1.75 : 1;
  }

  update(_timeSec: number, deltaSec: number) {
    this.currentPos.lerp(this.targetPos, Math.min(1, deltaSec * 12));
    this.group.position.copy(this.currentPos);

    const expansionSpeed = 0.08 + this.rssiNorm * 0.06;
    this.wavePhase = (this.wavePhase + deltaSec * expansionSpeed) % 1;

    const maxR = this.maxRadius;
    for (let k = 0; k < SHELLS; k++) {
      const mesh = this.shells[k];
      const mat = mesh.material as THREE.MeshBasicMaterial;
      const ph = (this.wavePhase + SHELL_OFFSETS[k]) % 1;
      const radius = ph * maxR;
      mesh.scale.setScalar(Math.max(0.05, radius));
      let op = (1 - ph) * this.baseOpacity * this.highlight;
      if (this.confidence < 30) op *= 0.55;
      mat.opacity = Math.min(1, op);
      mat.color.copy(this.colour);
    }

    if (this.anchor) {
      this.anchor.visible = this.confidence > 60;
    }
  }

  syncNetwork(net: WifiNetwork) {
    this.colour.copy(networkColour(net.bssid, net.frequency));
    const rawMax = rssiToDistance(net.rssi, net.frequency);
    this.maxRadius = Math.max(0.15, rawMax * 0.08);
    this.baseOpacity = shellBaseOpacity(net.rssi);
    this.rssiNorm = Math.max(0, Math.min(1, (net.rssi + 90) / 60));
  }

  dispose() {
    for (const g of this.geometries) g.dispose();
    for (const m of this.shells) {
      (m.material as THREE.MeshBasicMaterial).dispose();
    }
    if (this.anchor) {
      this.anchor.geometry.dispose();
      (this.anchor.material as THREE.MeshBasicMaterial).dispose();
    }
  }
}
