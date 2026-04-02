export interface SacredSite {
  id: string;
  name: string;
  lat: number;
  lng: number;
  culture: string;
  age: string;
  geology: string;
  schumannNote: string;
  radius: number;             // km — area of geological influence
  // Phase 3: live resonance data will replace this placeholder
  resonancePlaceholder: number; // 0..1 strength indicator stub
  description: string;
}

// 20 major sacred sites — extended with geological + cultural metadata
// Phase 3: resonancePlaceholder will be replaced by live server data
export const SACRED_SITES: SacredSite[] = [
  {
    id: 'stonehenge',
    name: 'Stonehenge',
    lat: 51.1789, lng: -1.8262,
    culture: 'Megalithic Britain',
    age: '~3000 BCE',
    geology: 'Chalk plain with bluestones transported from Welsh igneous outcrops; sits atop a natural avenue aligned with solstice sunrise',
    schumannNote: 'Repeated anomalous EM readings reported; chalk geology amplifies telluric currents',
    radius: 15,
    resonancePlaceholder: 0.85,
    description: 'Neolithic monument, Wiltshire, England',
  },
  {
    id: 'great_pyramid',
    name: 'Great Pyramid',
    lat: 29.9792, lng: 31.1342,
    culture: 'Ancient Egypt (Old Kingdom)',
    age: '~2560 BCE',
    geology: 'Limestone plateau above deep aquifer; granite core channels resonant frequencies; desert bedrock has high piezoelectric potential',
    schumannNote: 'Internal chambers documented to resonate at 1.5 Hz; structural geometry proposed to function as EM focusing device',
    radius: 20,
    resonancePlaceholder: 0.92,
    description: 'Giza plateau, Egypt',
  },
  {
    id: 'machu_picchu',
    name: 'Machu Picchu',
    lat: -13.1631, lng: -72.5450,
    culture: 'Inca Empire',
    age: '~1450 CE',
    geology: 'Granite ridge on two intersecting fault lines; site sits precisely above the confluence',
    schumannNote: 'Fault intersection creates measurable EM anomaly; granite under compression is piezoelectric',
    radius: 12,
    resonancePlaceholder: 0.88,
    description: 'Inca citadel, Peru',
  },
  {
    id: 'angkor_wat',
    name: 'Angkor Wat',
    lat: 13.4125, lng: 103.8667,
    culture: 'Khmer Empire',
    age: '~1150 CE',
    geology: 'Sandstone construction over laterite base; built on a vast hydraulic water management system that functions as a ground capacitor',
    schumannNote: 'Water table manipulation creates geomagnetic anomaly correlating with lunar cycle',
    radius: 18,
    resonancePlaceholder: 0.87,
    description: 'Khmer temple complex, Cambodia',
  },
  {
    id: 'gobekli_tepe',
    name: 'Göbekli Tepe',
    lat: 37.2232, lng: 38.9224,
    culture: 'Pre-Pottery Neolithic',
    age: '~9600 BCE',
    geology: 'Limestone hill on the northern edge of the Fertile Crescent; intentionally buried under deep fill — geology still largely unmapped',
    schumannNote: 'Oldest known ceremonial site; T-pillars with electromagnetic sensitivity not yet characterised',
    radius: 10,
    resonancePlaceholder: 0.90,
    description: 'Oldest known temple, Anatolia, Turkey',
  },
  {
    id: 'chichen_itza',
    name: 'Chichén Itzá',
    lat: 20.6843, lng: -88.5678,
    culture: 'Maya Civilisation',
    age: '~600–1200 CE',
    geology: 'Sits above a network of cenotes (underground sinkholes); porous limestone over water table creates measurable EM variation',
    schumannNote: 'El Castillo pyramid produces chirped-pulse acoustic echo; cenote network may amplify telluric currents',
    radius: 14,
    resonancePlaceholder: 0.82,
    description: 'Maya pyramid complex, Mexico',
  },
  {
    id: 'avebury',
    name: 'Avebury',
    lat: 51.4285, lng: -1.8544,
    culture: 'Megalithic Britain',
    age: '~2850 BCE',
    geology: 'Largest megalithic henge in the world; sarsen standing stones of high silica content on chalk substrate — known for strong dowsing readings',
    schumannNote: 'Dragon Hill adjacent; multiple ley lines converge; anomalous compass deflections documented',
    radius: 8,
    resonancePlaceholder: 0.83,
    description: 'Neolithic henge monument, Wiltshire, England',
  },
  {
    id: 'carnac',
    name: 'Carnac Stones',
    lat: 47.5983, lng: -3.0591,
    culture: 'Armorican Neolithic',
    age: '~4500 BCE',
    geology: 'Over 3,000 menhirs on granite bedrock parallel to Atlantic coast; high quartz content in stones; coastal position creates geomagnetic tidal variation',
    schumannNote: 'Linear stone alignment may function as a standing wave resonator for telluric current',
    radius: 10,
    resonancePlaceholder: 0.78,
    description: 'Megalithic stone rows, Brittany, France',
  },
  {
    id: 'easter_island',
    name: 'Easter Island',
    lat: -27.1127, lng: -109.3497,
    culture: 'Rapa Nui',
    age: '~1100–1700 CE',
    geology: 'Volcanic island; moai carved from volcanic tuff with coral eye sockets; sits above a complex submarine ridge system',
    schumannNote: 'Island positioned at confluence of Pacific Plate boundaries; basaltic magnetism measurable',
    radius: 12,
    resonancePlaceholder: 0.80,
    description: 'Rapa Nui moai, Pacific Ocean',
  },
  {
    id: 'newgrange',
    name: 'Newgrange',
    lat: 53.6947, lng: -6.4755,
    culture: 'Neolithic Ireland',
    age: '~3200 BCE',
    geology: 'River Boyne bend with waterlogged peat; passage tomb chamber constructed with quartz façade; passage aligned with winter solstice sunrise',
    schumannNote: 'Quartz-rich construction creates measurable piezoelectric effect under foot traffic; chamber resonance ~110 Hz',
    radius: 8,
    resonancePlaceholder: 0.84,
    description: 'Neolithic passage tomb, County Meath, Ireland',
  },
  {
    id: 'borobudur',
    name: 'Borobudur',
    lat: -7.6079, lng: 110.2038,
    culture: 'Sailendra Dynasty',
    age: '~800 CE',
    geology: 'Andesite construction on a natural hill; located between four active volcanoes whose EM fields intersect; volcanic soil high in magnetite',
    schumannNote: 'Situated in a geomagnetic anomaly created by volcanic arc intersection; bell-shaped stupas may be resonant cavities',
    radius: 16,
    resonancePlaceholder: 0.86,
    description: '9th-century Buddhist temple, Java, Indonesia',
  },
  {
    id: 'teotihuacan',
    name: 'Teotihuacan',
    lat: 19.6925, lng: -98.8438,
    culture: 'Teotihuacan Civilisation',
    age: '~100 BCE – 550 CE',
    geology: 'Obsidian-rich volcanic tableland; discovered tunnels beneath Pyramid of the Sun contain mica sheets — a known EM insulator/reflector',
    schumannNote: 'Thick mica layers in tunnel floor beneath pyramid; obsidian floors in tunnel; possibly engineered for EM properties',
    radius: 15,
    resonancePlaceholder: 0.89,
    description: 'Ancient Mesoamerican city, Mexico',
  },
  {
    id: 'petra',
    name: 'Petra',
    lat: 30.3285, lng: 35.4444,
    culture: 'Nabataean Kingdom',
    age: '~312 BCE',
    geology: 'Rose-red Nubian sandstone carved directly into cliff faces; Wadi Rum geology with iron oxide creating measurable magnetic variation',
    schumannNote: 'Sandstone formation naturally channels wind and acoustic resonance; EM behaviour largely unstudied',
    radius: 10,
    resonancePlaceholder: 0.77,
    description: 'Rock-cut city, Jordan',
  },
  {
    id: 'persepolis',
    name: 'Persepolis',
    lat: 29.9350, lng: 52.8907,
    culture: 'Achaemenid Empire',
    age: '~518 BCE',
    geology: 'Limestone terrace on the Zagros mountain front thrust fault; sits above one of the most seismically active regions in Iran',
    schumannNote: 'Zagros fold-thrust belt generates continuous low-level seismic activity; piezoelectric strain in limestone',
    radius: 12,
    resonancePlaceholder: 0.75,
    description: 'Achaemenid capital, Iran',
  },
  {
    id: 'uluru',
    name: 'Uluru',
    lat: -25.3444, lng: 131.0369,
    culture: 'Anangu (Australian Aboriginal)',
    age: 'Continuous occupation >30,000 years',
    geology: 'Arkosic sandstone monolith 9 km long underground; extremely high feldspar and quartz content; electrical conductivity measurements show anomaly beneath rock',
    schumannNote: 'Documented magnetic anomaly; sacred to Anangu as intersection of Tjukurpa songlines — which map onto geological fault networks',
    radius: 25,
    resonancePlaceholder: 0.91,
    description: 'Sacred sandstone monolith, Northern Territory, Australia',
  },
  {
    id: 'mount_shasta',
    name: 'Mount Shasta',
    lat: 41.4092, lng: -122.1949,
    culture: 'Multiple indigenous nations (Shasta, Wintu, Modoc)',
    age: 'Continuous sacred significance',
    geology: 'Active composite volcano; high magnetite content in basaltic lava; sits on Cascadia Subduction Zone edge',
    schumannNote: 'Multiple documented geomagnetic anomalies; volcanic magnetic field measurably different from surroundings',
    radius: 30,
    resonancePlaceholder: 0.79,
    description: 'Volcanic peak, California, USA',
  },
  {
    id: 'sedona',
    name: 'Sedona Vortex',
    lat: 34.8697, lng: -111.7609,
    culture: 'Yavapai-Apache Nation',
    age: 'Continuous sacred significance',
    geology: 'Red Entrada sandstone over Coconino sandstone; iron oxide gives magnetic properties; sits above Colorado Plateau uplift with complex fault network',
    schumannNote: 'Four documented "vortex" sites correlate with iron-rich geological outcrops; compass anomaly measurable at Airport Mesa site',
    radius: 15,
    resonancePlaceholder: 0.81,
    description: 'Energy vortex sites, Arizona, USA',
  },
  {
    id: 'glastonbury',
    name: 'Glastonbury Tor',
    lat: 51.1450, lng: -2.6988,
    culture: 'Celtic / Early Christian',
    age: 'Iron Age through medieval',
    geology: 'Isolated hill of Jurassic limestone rising from Somerset Levels; below-average electrical resistivity suggesting high water table and subsurface cavities',
    schumannNote: 'Terrestrial radiation anomaly mapped by dowsers; Michael and Mary ley lines said to intersect here',
    radius: 8,
    resonancePlaceholder: 0.82,
    description: 'Iconic hill, Somerset, England',
  },
  {
    id: 'delphi',
    name: 'Delphi',
    lat: 38.4824, lng: 22.5010,
    culture: 'Ancient Greek',
    age: '~800 BCE (oracle established)',
    geology: 'Intersection of two geological faults releases gaseous hydrocarbon vapours (ethylene); site chosen precisely for this geochemical anomaly',
    schumannNote: 'Fault intersection documented by geological survey; ethylene gas concentrations at oracle site confirmed; tectonic activity generates EM',
    radius: 10,
    resonancePlaceholder: 0.76,
    description: 'Oracle sanctuary, Mount Parnassus, Greece',
  },
  {
    id: 'mount_kailash',
    name: 'Mount Kailash',
    lat: 31.0672, lng: 81.3131,
    culture: 'Hindu, Buddhist, Jain, Bon',
    age: 'Continuous sacred significance across cultures',
    geology: 'Isolated Gangdise granite peak at the centre of the Tibetan Plateau; considered the hydrological and geomagnetic axis of the Asian continent',
    schumannNote: 'Proposed as geomagnetic axis mundi; unusually high local magnetic field intensity; four major river systems originate here',
    radius: 35,
    resonancePlaceholder: 0.95,
    description: 'Sacred peak, Tibet — considered axis mundi',
  },
];

const PROXIMITY_RADIUS_KM = 500;
const ANOMALY_PROXIMITY_KM = 50;

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
    .slice(0, 3);
}

export function getNearestSite(userLat: number, userLng: number): Nearbysite | null {
  const all = SACRED_SITES.map((site) => ({
    site,
    distanceKm: haversineKm(userLat, userLng, site.lat, site.lng),
    bearingDeg: bearingDeg(userLat, userLng, site.lat, site.lng),
  })).sort((a, b) => a.distanceKm - b.distanceKm);
  return all[0] ?? null;
}

// Check which sacred sites an anomaly coordinate is within proximity of
export function getProximityLinksForAnomaly(
  anomalyLat: number,
  anomalyLng: number
): { site: SacredSite; distanceKm: number }[] {
  return SACRED_SITES.filter((site) => {
    const d = haversineKm(anomalyLat, anomalyLng, site.lat, site.lng);
    return d <= ANOMALY_PROXIMITY_KM;
  }).map((site) => ({
    site,
    distanceKm: haversineKm(anomalyLat, anomalyLng, site.lat, site.lng),
  }));
}

export { haversineKm };
