/**
 * General geometry utilities for connection lines and ley-line checks.
 */

import { haversineKm } from '../constants/sacredSites';
import { SACRED_SITES, SacredSite } from '../constants/sacredSites';
import { FAULT_LINES, FaultLine } from '../constants/faultLines';

export interface LatLng { latitude: number; longitude: number }

/** Bearing in degrees from point A to point B */
export function bearing(a: LatLng, b: LatLng): number {
  const la1 = (a.latitude * Math.PI) / 180;
  const la2 = (b.latitude * Math.PI) / 180;
  const dLng = ((b.longitude - a.longitude) * Math.PI) / 180;
  const y = Math.sin(dLng) * Math.cos(la2);
  const x = Math.cos(la1) * Math.sin(la2) - Math.sin(la1) * Math.cos(la2) * Math.cos(dLng);
  return ((Math.atan2(y, x) * 180) / Math.PI + 360) % 360;
}

/** Midpoint of two coords */
export function midpoint(a: LatLng, b: LatLng): LatLng {
  return {
    latitude: (a.latitude + b.latitude) / 2,
    longitude: (a.longitude + b.longitude) / 2,
  };
}

/** Minimum distance in km from a point to a line segment (approximate) */
export function distToSegmentKm(p: LatLng, a: LatLng, b: LatLng): number {
  // Parameterise the segment with t in [0,1]
  const dx = b.latitude - a.latitude;
  const dy = b.longitude - a.longitude;
  const lenSq = dx * dx + dy * dy;
  if (lenSq === 0) return haversineKm(p.latitude, p.longitude, a.latitude, a.longitude);
  const t = Math.max(0, Math.min(1,
    ((p.latitude - a.latitude) * dx + (p.longitude - a.longitude) * dy) / lenSq
  ));
  const proj: LatLng = { latitude: a.latitude + t * dx, longitude: a.longitude + t * dy };
  return haversineKm(p.latitude, p.longitude, proj.latitude, proj.longitude);
}

const SITE_PASS_KM = 50;
const FAULT_PASS_KM = 50;
const BEARING_ALIGN_DEG = 5;

export interface LineAlignmentResult {
  passesNearSites: SacredSite[];
  passesNearFaults: { fault: FaultLine; distKm: number }[];
  alignedBearing: boolean; // within 5° of another line bearing
}

export function checkLineAlignment(
  a: LatLng,
  b: LatLng,
  otherBearings: number[]
): LineAlignmentResult {
  const mid = midpoint(a, b);
  const lineBearing = bearing(a, b);

  // Sacred sites within SITE_PASS_KM of the line's midpoint
  const passesNearSites = SACRED_SITES.filter(
    (site) => haversineKm(mid.latitude, mid.longitude, site.lat, site.lng) <= SITE_PASS_KM
  );

  // Fault segments within FAULT_PASS_KM of midpoint
  const passesNearFaults: { fault: FaultLine; distKm: number }[] = [];
  for (const fault of FAULT_LINES) {
    let minDist = Infinity;
    for (let i = 0; i < fault.coordinates.length - 1; i++) {
      const d = distToSegmentKm(
        mid,
        { latitude: fault.coordinates[i].latitude, longitude: fault.coordinates[i].longitude },
        { latitude: fault.coordinates[i + 1].latitude, longitude: fault.coordinates[i + 1].longitude }
      );
      if (d < minDist) minDist = d;
    }
    if (minDist <= FAULT_PASS_KM) {
      passesNearFaults.push({ fault, distKm: Math.round(minDist) });
    }
  }

  // Bearing alignment with other drawn lines
  const alignedBearing = otherBearings.some((ob) => {
    const diff = Math.abs(((lineBearing - ob) + 360) % 360);
    return diff <= BEARING_ALIGN_DEG || diff >= 360 - BEARING_ALIGN_DEG;
  });

  return { passesNearSites, passesNearFaults, alignedBearing };
}
