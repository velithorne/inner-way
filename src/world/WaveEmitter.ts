import * as THREE from 'three';
import { networkColour } from '../services/colourFromBssid';
import { rssiToDistance } from '../services/rssiToDistance';
import type { WifiNetwork } from '../types/wifi';

/**
 * Placeholder group for per-network AR state (position, bearing, highlight).
 * Spherical wavefront shells are disabled — signal lines are the primary visual;
 * filled/wireframe shells were tinting the camera and hurting FPS.
 */
export class WaveEmitter {
  readonly bssid: string;
  readonly group: THREE.Group;
  readonly colour: THREE.Color;
  maxRadius: number;
  baseOpacity: number;
  readonly currentPos: THREE.Vector3;
  private readonly targetPos: THREE.Vector3;
  confidence: number;

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
    this.maxRadius = Math.max(0.5, rawMax * 0.12);
    this.baseOpacity = opacityFromRssi(net.rssi);

    this.currentPos = options.position.clone();
    this.targetPos = options.position.clone();

    this.group = new THREE.Group();
    this.group.renderOrder = 1;
    this.group.position.copy(this.currentPos);
    this.group.rotation.y = options.bearingRad;
  }

  setTargetPosition(pos: THREE.Vector3) {
    this.targetPos.copy(pos);
  }

  setConfidence(c: number) {
    this.confidence = c;
  }

  setHighlight(_on: boolean) {
    /* no-op: shells removed; lines use highlight via SignalStore elsewhere */
  }

  update(_timeSec: number, deltaSec: number) {
    this.currentPos.lerp(this.targetPos, Math.min(1, deltaSec * 12));
    this.group.position.copy(this.currentPos);
  }

  syncNetwork(net: WifiNetwork) {
    this.colour.copy(networkColour(net.bssid, net.frequency));
    const rawMax = rssiToDistance(net.rssi, net.frequency);
    this.maxRadius = Math.max(0.5, rawMax * 0.12);
    this.baseOpacity = opacityFromRssi(net.rssi);
  }

  dispose() {
    /* no GPU resources */
  }
}

function opacityFromRssi(rssi: number): number {
  const norm = Math.max(0, Math.min(1, (rssi + 90) / 60));
  return 0.02 + norm * 0.06;
}
