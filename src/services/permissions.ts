import * as Location from 'expo-location';

export type PermissionState = {
  fine: boolean;
  coarse: boolean;
  wifi: boolean;
};

/**
 * Foreground location covers GPS (fine) and coarse; on Android, WiFi scan also
 * requires location enabled (handled at OS level).
 */
export async function requestSpectraPermissions(): Promise<PermissionState> {
  const res = await Location.requestForegroundPermissionsAsync();
  const ok = res.status === 'granted';
  return {
    fine: ok,
    coarse: ok,
    wifi: ok,
  };
}
