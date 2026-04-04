import * as THREE from 'three';
import type { RouterEstimate } from '../services/routerTriangulator';

export function estimateToWorldPosition(est: RouterEstimate): THREE.Vector3 {
  const br = (est.bearing * Math.PI) / 180;
  const depth = est.distance * 0.05;
  return new THREE.Vector3(Math.sin(br) * depth, 0, -Math.cos(br) * depth);
}
