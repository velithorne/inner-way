import * as THREE from 'three';

const spherical = new THREE.Spherical();
const v = new THREE.Vector3();

/**
 * Pan = adjust spherical theta/phi around origin (y-up). Zoom = multiply radius.
 */
export function applyOrbitPanAndZoom(
  position: THREE.Vector3,
  deltaX: number,
  deltaY: number,
  zoomFactor: number,
  panSensitivity: number,
): void {
  spherical.setFromVector3(position);
  spherical.theta -= deltaX * panSensitivity;
  spherical.phi += deltaY * panSensitivity;
  spherical.phi = THREE.MathUtils.clamp(spherical.phi, 0.12, Math.PI - 0.12);

  if (zoomFactor !== 1 && zoomFactor > 0) {
    spherical.radius *= zoomFactor;
  }

  spherical.radius = THREE.MathUtils.clamp(spherical.radius, 40, 520);

  v.setFromSpherical(spherical);
  position.copy(v);
}
