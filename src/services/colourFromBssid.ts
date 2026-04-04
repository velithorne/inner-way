import { Color } from 'three';

/** Deterministic hue from BSSID; avoids pure red/green bands per blueprint. */
export function bssidToHue(bssid: string): number {
  const hex = bssid.replace(/:/g, '').toLowerCase();
  let hash = 0;
  for (const c of hex) {
    hash = (hash * 31 + c.charCodeAt(0)) % 360;
  }
  let h = hash;
  if (h >= 355 || h <= 15) h = 15;
  if (h >= 110 && h <= 130) h = 130;
  return h;
}

export function networkColour(bssid: string, frequency: number): Color {
  const hue = bssidToHue(bssid);
  const is5 = frequency > 4000;
  const s = is5 ? 0.6 : 0.8;
  const l = is5 ? 0.75 : 0.55;
  return new Color().setHSL(hue / 360, s, l);
}
