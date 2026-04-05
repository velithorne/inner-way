export const COLORS = {
  bg: '#000000',
  cyan: '#00FFFF',
  magenta: '#FF00FF',
  green: '#00FF88',
  hud: 'rgba(0, 255, 200, 0.85)',
  ring: 'rgba(0, 80, 40, 0.45)',
  white: '#FFFFFF',
} as const;

export const TX_POWER_DBM = -59;
export const PATH_LOSS_N = 2.0;

/** Meters from RSSI using log-distance path loss. */
export function estimateDistanceMeters(rssi: number): number {
  return Math.pow(10, (TX_POWER_DBM - rssi) / (10 * PATH_LOSS_N));
}

export function bandFromFrequencyMhz(freq: number): '2.4' | '5' | 'unknown' {
  if (!freq || freq <= 0) return 'unknown';
  if (freq < 3000) return '2.4';
  return '5';
}

export function bandColor(freq: number): string {
  const b = bandFromFrequencyMhz(freq);
  if (b === '2.4') return COLORS.cyan;
  if (b === '5') return COLORS.magenta;
  return COLORS.green;
}
