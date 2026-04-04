import React, { useCallback, useEffect, useRef, useState } from 'react';
import { Platform, Pressable, StyleSheet, Text, View } from 'react-native';
import * as Location from 'expo-location';
import { Magnetometer } from 'expo-sensors';
import { FieldMapScreen } from '../screens/FieldMapScreen';
import { PhantomCityScreen } from '../screens/PhantomCityScreen';
import { SignalScreen } from '../screens/SignalScreen';
import { ValidationScreen } from '../screens/ValidationScreen';
import { RouterTriangulator } from '../services/routerTriangulator';
import { WifiScanner } from '../services/wifiScanner';
import { useWifiStore } from '../store/useWifiStore';
import { haversineM } from '../utils/geo';

type TabId = 'signal' | 'networks' | 'city' | 'field';

const TABS: { id: TabId; label: string }[] = [
  { id: 'signal', label: 'SIGNAL' },
  { id: 'networks', label: 'NETWORKS' },
  { id: 'city', label: 'CITY' },
  { id: 'field', label: 'FIELD' },
];

function headingFromMag(x: number, y: number): number {
  let deg = (Math.atan2(y, x) * 180) / Math.PI;
  deg = (deg + 360) % 360;
  return deg;
}

function headingDelta(a: number, b: number): number {
  const d = Math.abs(a - b) % 360;
  return d > 180 ? 360 - d : d;
}

export function AppShell() {
  const [tab, setTab] = useState<TabId>('signal');
  const scannerRef = useRef(new WifiScanner());
  const triRef = useRef(new RouterTriangulator());
  const { networks, lastScan, setFromScan, setScanning, setRouterEstimates } = useWifiStore();

  const headingDegRef = useRef(0);
  const prevHeadingRef = useRef<number | null>(null);
  const lastLocRef = useRef<{ lat: number; lng: number } | null>(null);

  const refreshEstimates = useCallback(() => {
    const nets = useWifiStore.getState().networks;
    const rec: Record<string, ReturnType<RouterTriangulator['estimatePosition']>> = {};
    for (const n of nets) {
      rec[n.bssid] = triRef.current.estimatePosition(n.bssid, n.rssi, n.frequency);
    }
    setRouterEstimates(rec);
  }, [setRouterEstimates]);

  const pushObservationsForNetworks = useCallback(
    (nets: typeof networks, t: number) => {
      const pos = lastLocRef.current ?? undefined;
      for (const n of nets) {
        triRef.current.addObservation({
          bssid: n.bssid,
          rssi: n.rssi,
          bearing: headingDegRef.current,
          timestamp: t,
          frequencyMHz: n.frequency,
          userPosition: pos,
        });
      }
      refreshEstimates();
    },
    [refreshEstimates]
  );

  useEffect(() => {
    (async () => {
      if (Platform.OS === 'android') {
        await Location.requestForegroundPermissionsAsync();
      }
    })();
  }, []);

  useEffect(() => {
    Magnetometer.setUpdateInterval(200);
    const sub = Magnetometer.addListener((m) => {
      const h = headingFromMag(m.x, m.y);
      headingDegRef.current = h;
      if (prevHeadingRef.current === null) prevHeadingRef.current = h;
    });
    return () => sub.remove();
  }, []);

  useEffect(() => {
    let sub: Location.LocationSubscription | null = null;
    (async () => {
      const { status } = await Location.requestForegroundPermissionsAsync();
      if (status !== 'granted') return;
      sub = await Location.watchPositionAsync(
        {
          accuracy: Location.Accuracy.Balanced,
          distanceInterval: 2,
          timeInterval: 4000,
        },
        (loc) => {
          const p = { lat: loc.coords.latitude, lng: loc.coords.longitude };
          const prev = lastLocRef.current;
          lastLocRef.current = p;
          if (!prev) return;
          const d = haversineM(prev, p);
          if (d < 2) return;
          pushObservationsForNetworks(useWifiStore.getState().networks, Date.now());
        }
      );
    })();
    return () => {
      void sub?.remove();
    };
  }, [pushObservationsForNetworks]);

  useEffect(() => {
    const id = setInterval(() => {
      const prev = prevHeadingRef.current;
      if (prev == null) return;
      const cur = headingDegRef.current;
      if (headingDelta(prev, cur) > 15) {
        prevHeadingRef.current = cur;
        pushObservationsForNetworks(useWifiStore.getState().networks, Date.now());
      }
    }, 400);
    return () => clearInterval(id);
  }, [pushObservationsForNetworks]);

  useEffect(() => {
    if (!lastScan) return;
    pushObservationsForNetworks(networks, Date.now());
  }, [lastScan, networks, pushObservationsForNetworks]);

  useEffect(() => {
    setScanning(true);
    scannerRef.current.start((nets) => {
      setFromScan(nets, Date.now());
    });
    return () => {
      scannerRef.current.stop();
      setScanning(false);
    };
  }, [setFromScan, setScanning]);

  return (
    <View style={styles.root}>
      <View style={styles.body}>
        {tab === 'signal' ? <SignalScreen /> : null}
        {tab === 'networks' ? <ValidationScreen /> : null}
        {tab === 'city' ? <PhantomCityScreen /> : null}
        {tab === 'field' ? <FieldMapScreen /> : null}
      </View>
      <View style={styles.tabBar}>
        {TABS.map((t) => (
          <Pressable
            key={t.id}
            style={[styles.tab, tab === t.id && styles.tabActive]}
            onPress={() => setTab(t.id)}
          >
            <Text style={[styles.tabText, tab === t.id && styles.tabTextActive]}>{t.label}</Text>
          </Pressable>
        ))}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: '#000' },
  body: { flex: 1 },
  tabBar: {
    flexDirection: 'row',
    borderTopWidth: 1,
    borderTopColor: '#1e2a36',
    backgroundColor: '#0a0e14',
    paddingBottom: 20,
    paddingTop: 8,
  },
  tab: {
    flex: 1,
    alignItems: 'center',
    paddingVertical: 10,
  },
  tabActive: {
    borderTopWidth: 2,
    borderTopColor: '#00ffe5',
    marginTop: -2,
  },
  tabText: {
    color: '#78909c',
    fontSize: 10,
    fontWeight: '700',
    letterSpacing: 0.5,
  },
  tabTextActive: {
    color: '#00ffe5',
  },
});
