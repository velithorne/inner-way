export interface SacredSite {
  id: string;
  name: string;
  lat: number;
  lng: number;
  // Phase 3: live resonance data will replace this placeholder
  resonancePlaceholder: number; // 0..1 strength indicator stub
  description: string;
}

// Starter database — 20 major sites
// Phase 3: this will be replaced by a server-synced database with
// live field anomaly data contributed by users worldwide.
export const SACRED_SITES: SacredSite[] = [
  { id: 'stonehenge',      name: 'Stonehenge',         lat: 51.1789,  lng: -1.8262,   resonancePlaceholder: 0.85, description: 'Neolithic monument, Wiltshire, England' },
  { id: 'great_pyramid',   name: 'Great Pyramid',      lat: 29.9792,  lng: 31.1342,   resonancePlaceholder: 0.92, description: 'Giza plateau, Egypt' },
  { id: 'machu_picchu',    name: 'Machu Picchu',        lat: -13.1631, lng: -72.5450,  resonancePlaceholder: 0.88, description: 'Inca citadel, Peru' },
  { id: 'angkor_wat',      name: 'Angkor Wat',          lat: 13.4125,  lng: 103.8667,  resonancePlaceholder: 0.87, description: 'Khmer temple complex, Cambodia' },
  { id: 'gobekli_tepe',    name: 'Göbekli Tepe',        lat: 37.2232,  lng: 38.9224,   resonancePlaceholder: 0.90, description: 'Oldest known temple, Anatolia, Turkey' },
  { id: 'chichen_itza',    name: 'Chichén Itzá',        lat: 20.6843,  lng: -88.5678,  resonancePlaceholder: 0.82, description: 'Maya pyramid complex, Mexico' },
  { id: 'avebury',         name: 'Avebury',             lat: 51.4285,  lng: -1.8544,   resonancePlaceholder: 0.83, description: 'Neolithic henge monument, Wiltshire, England' },
  { id: 'carnac',          name: 'Carnac Stones',       lat: 47.5983,  lng: -3.0591,   resonancePlaceholder: 0.78, description: 'Megalithic stone rows, Brittany, France' },
  { id: 'easter_island',   name: 'Easter Island',       lat: -27.1127, lng: -109.3497, resonancePlaceholder: 0.80, description: 'Rapa Nui moai, Pacific Ocean' },
  { id: 'newgrange',       name: 'Newgrange',           lat: 53.6947,  lng: -6.4755,   resonancePlaceholder: 0.84, description: 'Neolithic passage tomb, County Meath, Ireland' },
  { id: 'borobudur',       name: 'Borobudur',           lat: -7.6079,  lng: 110.2038,  resonancePlaceholder: 0.86, description: '9th-century Buddhist temple, Java, Indonesia' },
  { id: 'teotihuacan',     name: 'Teotihuacan',         lat: 19.6925,  lng: -98.8438,  resonancePlaceholder: 0.89, description: 'Ancient Mesoamerican city, Mexico' },
  { id: 'petra',           name: 'Petra',               lat: 30.3285,  lng: 35.4444,   resonancePlaceholder: 0.77, description: 'Rock-cut city, Jordan' },
  { id: 'persepolis',      name: 'Persepolis',          lat: 29.9350,  lng: 52.8907,   resonancePlaceholder: 0.75, description: 'Achaemenid capital, Iran' },
  { id: 'uluru',           name: 'Uluru',               lat: -25.3444, lng: 131.0369,  resonancePlaceholder: 0.91, description: 'Sacred sandstone monolith, Northern Territory, Australia' },
  { id: 'mount_shasta',    name: 'Mount Shasta',        lat: 41.4092,  lng: -122.1949, resonancePlaceholder: 0.79, description: 'Volcanic peak, California, USA' },
  { id: 'sedona',          name: 'Sedona Vortex',       lat: 34.8697,  lng: -111.7609, resonancePlaceholder: 0.81, description: 'Energy vortex sites, Arizona, USA' },
  { id: 'glastonbury',     name: 'Glastonbury Tor',     lat: 51.1450,  lng: -2.6988,   resonancePlaceholder: 0.82, description: 'Iconic hill, Somerset, England' },
  { id: 'delphi',          name: 'Delphi',              lat: 38.4824,  lng: 22.5010,   resonancePlaceholder: 0.76, description: 'Oracle sanctuary, Mount Parnassus, Greece' },
  { id: 'mount_kailash',   name: 'Mount Kailash',       lat: 31.0672,  lng: 81.3131,   resonancePlaceholder: 0.95, description: 'Sacred peak, Tibet — considered axis mundi' },
];

const PROXIMITY_RADIUS_KM = 500;

function haversineKm(lat1: number, lng1: number, lat2: number, lng2: number): number {
  const R = 6371;
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLng = ((lng2 - lng1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos((lat1 * Math.PI) / 180) *
      Math.cos((lat2 * Math.PI) / 180) *
      Math.sin(dLng / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

function bearingDeg(lat1: number, lng1: number, lat2: number, lng2: number): number {
  const dLng = ((lng2 - lng1) * Math.PI) / 180;
  const la1 = (lat1 * Math.PI) / 180;
  const la2 = (lat2 * Math.PI) / 180;
  const y = Math.sin(dLng) * Math.cos(la2);
  const x = Math.cos(la1) * Math.sin(la2) - Math.sin(la1) * Math.cos(la2) * Math.cos(dLng);
  return ((Math.atan2(y, x) * 180) / Math.PI + 360) % 360;
}

export interface Nearbysite {
  site: SacredSite;
  distanceKm: number;
  bearingDeg: number;
}

export function getNearbysSites(userLat: number, userLng: number): Nearbysite[] {
  return SACRED_SITES.map((site) => ({
    site,
    distanceKm: haversineKm(userLat, userLng, site.lat, site.lng),
    bearingDeg: bearingDeg(userLat, userLng, site.lat, site.lng),
  }))
    .filter((s) => s.distanceKm <= PROXIMITY_RADIUS_KM)
    .sort((a, b) => a.distanceKm - b.distanceKm)
    .slice(0, 3); // show closest 3
}
