import type { MapStyleElement } from 'react-native-maps';

export const DARK_MAP_STYLE: MapStyleElement[] = [
  { elementType: 'geometry',           stylers: [{ color: '#0a0f1e' }] },
  { elementType: 'labels.text.fill',   stylers: [{ color: '#00ffe5' }, { opacity: 0.6 }] },
  { elementType: 'labels.text.stroke', stylers: [{ color: '#000000' }] },
  { featureType: 'water',      elementType: 'geometry',         stylers: [{ color: '#000d1a' }] },
  { featureType: 'landscape',  elementType: 'geometry',         stylers: [{ color: '#050d1a' }] },
  { featureType: 'road',                                        stylers: [{ visibility: 'simplified' }, { color: '#0a1a2e' }] },
  { featureType: 'administrative', elementType: 'geometry.stroke', stylers: [{ color: '#00ffe5' }, { opacity: 0.15 }] },
  { featureType: 'poi',                                         stylers: [{ visibility: 'off' }] },
];
