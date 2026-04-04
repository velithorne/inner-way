import React, { useEffect, useRef, useState } from 'react';
import { Platform, Pressable, StyleSheet, Text, View } from 'react-native';
import * as Location from 'expo-location';
import { SignalScreen } from '../screens/SignalScreen';
import { ValidationScreen } from '../screens/ValidationScreen';
import { PlaceholderScreen } from '../screens/PlaceholderScreen';
import { WifiScanner } from '../services/wifiScanner';
import { useWifiStore } from '../store/useWifiStore';

type TabId = 'signal' | 'networks' | 'city' | 'field';

const TABS: { id: TabId; label: string }[] = [
  { id: 'signal', label: 'SIGNAL' },
  { id: 'networks', label: 'NETWORKS' },
  { id: 'city', label: 'CITY' },
  { id: 'field', label: 'FIELD' },
];

export function AppShell() {
  const [tab, setTab] = useState<TabId>('signal');
  const scannerRef = useRef(new WifiScanner());
  const { setFromScan, setScanning } = useWifiStore();

  useEffect(() => {
    (async () => {
      if (Platform.OS === 'android') {
        await Location.requestForegroundPermissionsAsync();
      }
    })();
  }, []);

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
        {tab === 'city' ? (
          <PlaceholderScreen title="Phantom City" phase="Phase 5 — 3D buildings (next)" />
        ) : null}
        {tab === 'field' ? (
          <PlaceholderScreen title="Field Map" phase="Phase 7 — voxel heatmap (next)" />
        ) : null}
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
