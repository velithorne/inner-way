import * as THREE from 'three';

/**
 * Convert geographic lat/lon to a 3D point on a unit sphere.
 * lat: degrees, -90 (south) to +90 (north)
 * lon: degrees, -180 to +180
 */
export function latLonToVector3(lat: number, lon: number, radius = 1): THREE.Vector3 {
  const phi = (90 - lat) * (Math.PI / 180);
  const theta = (lon + 180) * (Math.PI / 180);

  return new THREE.Vector3(
    -radius * Math.sin(phi) * Math.cos(theta),
    radius * Math.cos(phi),
    radius * Math.sin(phi) * Math.sin(theta),
  );
}

/**
 * Get a tangent quaternion that orients a pulse ring perpendicular to the surface.
 */
export function surfaceQuaternion(lat: number, lon: number): THREE.Quaternion {
  const pos = latLonToVector3(lat, lon);
  const up = new THREE.Vector3(0, 1, 0);
  const q = new THREE.Quaternion();
  q.setFromUnitVectors(up, pos.normalize());
  return q;
}

/**
 * Clamp lat/lon to valid ranges.
 */
export function clampGeo(lat: number, lon: number): { lat: number; lon: number } {
  return {
    lat: Math.max(-90, Math.min(90, lat)),
    lon: ((lon + 180) % 360) - 180,
  };
}
