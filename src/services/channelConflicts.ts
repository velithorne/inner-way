import type { WifiNetwork } from '../types/wifi';

export type ChannelConflict = {
  a: WifiNetwork;
  b: WifiNetwork;
  channel: number;
};

/** Same WiFi channel (2.4 or 5 GHz) — potential co-channel interference. */
export function findChannelConflicts(networks: WifiNetwork[]): ChannelConflict[] {
  const byCh = new Map<number, WifiNetwork[]>();
  for (const n of networks) {
    if (n.channel <= 0) continue;
    if (!byCh.has(n.channel)) byCh.set(n.channel, []);
    byCh.get(n.channel)!.push(n);
  }
  const out: ChannelConflict[] = [];
  for (const [, list] of byCh) {
    if (list.length < 2) continue;
    for (let i = 0; i < list.length; i++) {
      for (let j = i + 1; j < list.length; j++) {
        if (list[i].bssid !== list[j].bssid) {
          out.push({ a: list[i], b: list[j], channel: list[i].channel });
        }
      }
    }
  }
  return out;
}
