/** Free-space path loss distance estimate (indoor default). */
export function rssiToDistance(rssi: number, frequency: number): number {
  const n = 2.7;
  const A = frequency > 4000 ? -45 : -40;
  return Math.pow(10, (A - rssi) / (10 * n));
}
