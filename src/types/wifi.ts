export interface WifiNetwork {
  ssid: string;
  bssid: string;
  rssi: number;
  frequency: number;
  channel: number;
  capabilities: string;
  timestamp: number;
}
