/**
 * Static geological substrate lookups from coordinates.
 * Phase 4: replace with server-side geological database query.
 */

import { FAULT_LINES } from '../constants/faultLines';
import { haversineKm } from '../constants/sacredSites';

// Simplified bounding-box regions → geological description
const GEO_REGIONS: {
  minLat: number; maxLat: number; minLng: number; maxLng: number;
  note: string;
}[] = [
  { minLat: -44, maxLat: -10, minLng: 112, maxLng: 154, note: 'Australian continental craton. Ancient Precambrian and Proterozoic basement. High silicate and quartzite content. Known for anomalous telluric currents across the continent.' },
  { minLat: -44, maxLat: -10, minLng: 138, maxLng: 154, note: 'Eastern Australian fold belt. Precambrian metamorphic basement overlain by Mesozoic sediments. Possible quartz-bearing geology. Correlates with high piezoelectric potential.' },
  { minLat: 24, maxLat: 50, minLng: -125, maxLng: -65, note: 'North American craton and Appalachian fold belt. Complex basement geology with known seismic zones. Metamorphic and igneous lithology at depth.' },
  { minLat: 32, maxLat: 42, minLng: -125, maxLng: -114, note: 'Pacific coast subduction zone influence. Highly active tectonic margin. Basaltic and granitic terrane with strong geomagnetic gradient.' },
  { minLat: 35, maxLat: 72, minLng: -10, maxLng: 40, note: 'European platform. Caledonian and Hercynian basement. Chalk and limestone dominant in NW Europe; granite massifs in Scandinavia and Iberia.' },
  { minLat: 20, maxLat: 55, minLng: 60, maxLng: 150, note: 'Asian continental interior. Stable Siberian craton northward; Alpine-Himalayan collision belt southward. Extreme geomagnetic variation across region.' },
  { minLat: -35, maxLat: 37, minLng: -18, maxLng: 55, note: 'African platform. Ancient Archean cratons (Congo, Kaapvaal, Tanzania). Rift valley systems produce measurable EM anomalies. High magnetic basement.' },
  { minLat: -60, maxLat: 10, minLng: -80, maxLng: -35, note: 'South American craton (Guiana and Brazilian shields). Precambrian metamorphic and volcanic basement. Andean volcanic arc on western margin creates strong EM gradient.' },
];

export function getGeologyNote(lat: number, lng: number): string {
  // Check proximity to fault lines first
  const nearFaults: { name: string; distKm: number }[] = [];
  for (const fault of FAULT_LINES) {
    for (let i = 0; i < fault.coordinates.length - 1; i++) {
      const a = fault.coordinates[i];
      const b = fault.coordinates[i + 1];
      // Rough midpoint distance check
      const midLat = (a.latitude + b.latitude) / 2;
      const midLng = (a.longitude + b.longitude) / 2;
      const d = haversineKm(lat, lng, midLat, midLng);
      if (d < 200) {
        nearFaults.push({ name: fault.name, distKm: Math.round(d) });
        break;
      }
    }
  }

  const faultNote = nearFaults.length > 0
    ? `Within ${nearFaults[0].distKm}km of ${nearFaults[0].name}. Active tectonic boundary — elevated seismic EM activity likely.`
    : null;

  // Check regional geology
  const region = GEO_REGIONS.find(
    (r) => lat >= r.minLat && lat <= r.maxLat && lng >= r.minLng && lng <= r.maxLng
  );

  if (faultNote && region) return `${faultNote}\n\n${region.note}`;
  if (faultNote) return faultNote;
  if (region) return region.note;
  return 'Geological substrate: pending Phase 4 server lookup. No regional match in local database.';
}

// Nearest fault to a coordinate
export function getNearestFault(lat: number, lng: number): { name: string; distKm: number } | null {
  let best: { name: string; distKm: number } | null = null;
  for (const fault of FAULT_LINES) {
    for (const coord of fault.coordinates) {
      const d = haversineKm(lat, lng, coord.latitude, coord.longitude);
      if (!best || d < best.distKm) best = { name: fault.name, distKm: Math.round(d) };
    }
  }
  return best;
}
