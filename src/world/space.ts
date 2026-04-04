import * as THREE from 'three';
import { rssiToDistance } from '../services/rssiToDistance';
import type { RouterEstimate } from '../services/routerTriangulator';

/** Emitter anchor: spread networks by RSSI-derived depth so shells don't stack. */
export function estimateToWorldPosition(est: RouterEstimate, frequencyMHz: number): THREE.Vector3 {
  const br = (est.bearing * Math.PI) / 180;
  const dEst = est.distance > 0 ? est.distance : rssiToDistance(-75, frequencyMHz);
  const depth = Math.max(0.8, dEst * 0.08);
  return new THREE.Vector3(Math.sin(br) * depth, 0, -Math.cos(br) * depth);
}
