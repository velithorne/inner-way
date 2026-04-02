export interface FaultLine {
  id: string;
  name: string;
  coordinates: { latitude: number; longitude: number }[];
}

// 15 major global fault lines — representative polylines (simplified)
export const FAULT_LINES: FaultLine[] = [
  {
    id: 'san_andreas',
    name: 'San Andreas Fault',
    coordinates: [
      { latitude: 32.5, longitude: -117.2 },
      { latitude: 34.0, longitude: -117.5 },
      { latitude: 35.3, longitude: -119.0 },
      { latitude: 36.0, longitude: -120.2 },
      { latitude: 37.0, longitude: -121.5 },
      { latitude: 38.2, longitude: -122.4 },
      { latitude: 40.2, longitude: -124.0 },
    ],
  },
  {
    id: 'north_anatolian',
    name: 'Anatolian Fault',
    coordinates: [
      { latitude: 40.8, longitude: 26.5 },
      { latitude: 40.8, longitude: 30.0 },
      { latitude: 40.5, longitude: 33.0 },
      { latitude: 39.8, longitude: 36.5 },
      { latitude: 39.5, longitude: 39.5 },
      { latitude: 39.2, longitude: 42.0 },
      { latitude: 38.8, longitude: 44.0 },
    ],
  },
  {
    id: 'alpine_fault',
    name: 'Alpine Fault (New Zealand)',
    coordinates: [
      { latitude: -44.8, longitude: 168.0 },
      { latitude: -43.5, longitude: 170.5 },
      { latitude: -42.2, longitude: 171.7 },
      { latitude: -40.8, longitude: 172.4 },
    ],
  },
  {
    id: 'dead_sea',
    name: 'Dead Sea Transform',
    coordinates: [
      { latitude: 28.5, longitude: 34.8 },
      { latitude: 29.8, longitude: 35.0 },
      { latitude: 31.0, longitude: 35.5 },
      { latitude: 32.5, longitude: 35.6 },
      { latitude: 33.5, longitude: 36.2 },
      { latitude: 36.5, longitude: 36.8 },
    ],
  },
  {
    id: 'great_rift',
    name: 'Great Rift Valley',
    coordinates: [
      { latitude: 11.5, longitude: 42.5 },
      { latitude: 8.0,  longitude: 38.0 },
      { latitude: 3.0,  longitude: 36.0 },
      { latitude: -1.0, longitude: 36.5 },
      { latitude: -6.0, longitude: 36.0 },
      { latitude: -10.0, longitude: 35.5 },
      { latitude: -15.0, longitude: 35.0 },
      { latitude: -18.0, longitude: 35.5 },
    ],
  },
  {
    id: 'himalayan_front',
    name: 'Himalayan Front Thrust',
    coordinates: [
      { latitude: 28.0, longitude: 73.0 },
      { latitude: 28.5, longitude: 77.0 },
      { latitude: 28.0, longitude: 82.0 },
      { latitude: 27.5, longitude: 86.5 },
      { latitude: 27.0, longitude: 91.0 },
      { latitude: 27.5, longitude: 95.5 },
    ],
  },
  {
    id: 'cascadia',
    name: 'Cascadia Subduction Zone',
    coordinates: [
      { latitude: 40.5, longitude: -125.5 },
      { latitude: 42.0, longitude: -125.0 },
      { latitude: 44.0, longitude: -125.5 },
      { latitude: 46.0, longitude: -125.0 },
      { latitude: 48.0, longitude: -125.5 },
      { latitude: 50.0, longitude: -128.0 },
    ],
  },
  {
    id: 'japan_trench',
    name: 'Japan Trench',
    coordinates: [
      { latitude: 32.0, longitude: 142.5 },
      { latitude: 35.0, longitude: 143.0 },
      { latitude: 38.0, longitude: 143.5 },
      { latitude: 40.0, longitude: 143.5 },
      { latitude: 42.0, longitude: 144.0 },
      { latitude: 44.5, longitude: 145.5 },
    ],
  },
  {
    id: 'mariana',
    name: 'Mariana Trench',
    coordinates: [
      { latitude: 11.0, longitude: 141.5 },
      { latitude: 13.0, longitude: 143.5 },
      { latitude: 15.5, longitude: 146.0 },
      { latitude: 17.5, longitude: 147.0 },
    ],
  },
  {
    id: 'mid_atlantic',
    name: 'Mid-Atlantic Ridge',
    coordinates: [
      { latitude: 65.0, longitude: -18.0 },
      { latitude: 55.0, longitude: -33.0 },
      { latitude: 45.0, longitude: -30.0 },
      { latitude: 30.0, longitude: -42.0 },
      { latitude: 15.0, longitude: -45.0 },
      { latitude: 0.0,  longitude: -22.0 },
      { latitude: -15.0, longitude: -14.0 },
      { latitude: -30.0, longitude: -13.0 },
      { latitude: -45.0, longitude: -12.0 },
      { latitude: -55.0, longitude: -5.0 },
    ],
  },
  {
    id: 'java_trench',
    name: 'Java Trench',
    coordinates: [
      { latitude: -9.0,  longitude: 108.0 },
      { latitude: -10.5, longitude: 113.0 },
      { latitude: -11.0, longitude: 116.0 },
      { latitude: -10.5, longitude: 120.0 },
      { latitude: -9.5,  longitude: 124.0 },
      { latitude: -8.0,  longitude: 128.0 },
    ],
  },
  {
    id: 'philippine',
    name: 'Philippine Fault',
    coordinates: [
      { latitude: 7.0,  longitude: 126.0 },
      { latitude: 10.0, longitude: 124.5 },
      { latitude: 12.5, longitude: 123.5 },
      { latitude: 15.0, longitude: 122.5 },
      { latitude: 17.5, longitude: 122.0 },
    ],
  },
  {
    id: 'denali',
    name: 'Denali Fault (Alaska)',
    coordinates: [
      { latitude: 61.0, longitude: -147.0 },
      { latitude: 62.0, longitude: -149.5 },
      { latitude: 63.0, longitude: -151.0 },
      { latitude: 63.5, longitude: -152.5 },
      { latitude: 63.0, longitude: -155.0 },
      { latitude: 62.5, longitude: -157.0 },
    ],
  },
  {
    id: 'balochistan',
    name: 'Balochistan Fault',
    coordinates: [
      { latitude: 26.0, longitude: 62.0 },
      { latitude: 27.5, longitude: 63.5 },
      { latitude: 29.0, longitude: 65.0 },
      { latitude: 30.5, longitude: 66.5 },
      { latitude: 32.0, longitude: 68.5 },
    ],
  },
  {
    id: 'new_madrid',
    name: 'New Madrid Seismic Zone',
    coordinates: [
      { latitude: 35.0, longitude: -90.5 },
      { latitude: 36.0, longitude: -89.5 },
      { latitude: 37.0, longitude: -89.0 },
      { latitude: 37.5, longitude: -88.5 },
    ],
  },
];
