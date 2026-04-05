import type { WifiNetwork } from '../types/wifi';

const SOLAR_PATTERNS = [
  /SOLAR/i,
  /ZEVERSOLAR/i,
  /TX_/i,
  /INVERTER/i,
  /\bPV\b/i,
  /ENPHASE/i,
  /FRONIUS/i,
];

export function isLikelySolarInverter(net: WifiNetwork): boolean {
  const s = net.ssid;
  return SOLAR_PATTERNS.some((re) => re.test(s));
}

/** Parse last octet of MAC. */
function lastOctet(bssid: string): number {
  const parts = bssid.toLowerCase().split(':');
  const last = parts[parts.length - 1];
  return parseInt(last, 16) || 0;
}

/** Same router dual-radio: same SSID, BSSIDs differ only in last octet. */
export function findDualBandPairs(networks: WifiNetwork[]): [WifiNetwork, WifiNetwork][] {
  const bySsid = new Map<string, WifiNetwork[]>();
  for (const n of networks) {
    const k = n.ssid.trim() || n.bssid;
    if (!bySsid.has(k)) bySsid.set(k, []);
    bySsid.get(k)!.push(n);
  }
  const pairs: [WifiNetwork, WifiNetwork][] = [];
  for (const group of bySsid.values()) {
    if (group.length < 2) continue;
    for (let i = 0; i < group.length; i++) {
      for (let j = i + 1; j < group.length; j++) {
        const a = group[i].bssid.replace(/:/g, '').toLowerCase();
        const b = group[j].bssid.replace(/:/g, '').toLowerCase();
        if (a.length !== 12 || b.length !== 12) continue;
        const preA = a.slice(0, 10);
        const preB = b.slice(0, 10);
        if (preA === preB && a.slice(10) !== b.slice(10)) {
          pairs.push([group[i], group[j]]);
        }
      }
    }
  }
  return pairs;
}
