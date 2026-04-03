import { useEffect, useState } from 'react';
import { Platform, StyleSheet, Text, View } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import * as Device from 'expo-device';
import { GestureHandlerRootView } from 'react-native-gesture-handler';

import { WorldNavigator } from '../navigation/WorldNavigator';
import { isSystemDataAvailable } from '../native/systemData';
import { startPolling, stopPolling } from '../services/systemPoller';
import { useWorldStore } from '../store/useWorldStore';
import { WorldEngine } from '../world/WorldEngine';

const ENTRY_KEY = '@silicon_entry_done';

export function SiliconScreen() {
  const snapshot = useWorldStore((s) => s.snapshot);

  const [entryProgress, setEntryProgress] = useState(0);
  const [showEntryText, setShowEntryText] = useState(true);

  useEffect(() => {
    if (Platform.OS !== 'android' || !isSystemDataAvailable()) return;
    startPolling();
    return () => stopPolling();
  }, []);

  useEffect(() => {
    let mounted = true;
    let intervalId: ReturnType<typeof setInterval> | null = null;
    (async () => {
      const done = await AsyncStorage.getItem(ENTRY_KEY);
      if (!mounted) return;
      if (done === '1') {
        setEntryProgress(1);
        setShowEntryText(false);
        return;
      }
      const start = Date.now();
      intervalId = setInterval(() => {
        const t = Math.min(1, (Date.now() - start) / 4000);
        if (mounted) setEntryProgress(t);
        if (t >= 1 && intervalId) {
          clearInterval(intervalId);
          intervalId = null;
          AsyncStorage.setItem(ENTRY_KEY, '1').catch(() => {});
          setTimeout(() => mounted && setShowEntryText(false), 800);
        }
      }, 16);
    })();
    return () => {
      mounted = false;
      if (intervalId) clearInterval(intervalId);
    };
  }, []);

  const cores = snapshot?.cpu?.length ?? 0;
  const totalRamGb = snapshot?.memory ? snapshot.memory.totalRam / (1024 ** 3) : 0;
  const storageGb = snapshot?.storage ? snapshot.storage.totalBytes / (1024 ** 3) : 0;

  if (Platform.OS !== 'android' || !isSystemDataAvailable()) {
    return (
      <View style={styles.fallback}>
        <Text style={styles.fallbackText}>SILICON world requires Android with the native SystemData build.</Text>
      </View>
    );
  }

  const entryComplete = entryProgress >= 1;

  return (
    <GestureHandlerRootView style={styles.root}>
      <View style={styles.flex}>
        <WorldEngine entryProgress={entryProgress} />

        {showEntryText && entryProgress < 1 ? (
          <View style={styles.entryOverlay} pointerEvents="none">
            <Text style={styles.entryTitle}>ENTERING SILICON</Text>
            <Text style={styles.entryLine}>
              DEVICE: {Device.modelName ?? Device.deviceName ?? 'Android'}
            </Text>
            <Text style={styles.entryLine}>
              {cores} CORES · {totalRamGb.toFixed(1)}GB RAM · {storageGb.toFixed(0)}GB STORAGE
            </Text>
          </View>
        ) : null}

        <WorldNavigator entryComplete={entryComplete} />
      </View>
    </GestureHandlerRootView>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: '#000008' },
  flex: { flex: 1 },
  entryOverlay: {
    ...StyleSheet.absoluteFillObject,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: 'rgba(0,0,8,0.35)',
  },
  entryTitle: {
    color: '#eceff1',
    fontSize: 22,
    fontWeight: '700',
    letterSpacing: 4,
    marginBottom: 16,
  },
  entryLine: { color: '#90a4ae', fontSize: 13, marginBottom: 6, textAlign: 'center' },
  fallback: {
    flex: 1,
    backgroundColor: '#000008',
    justifyContent: 'center',
    padding: 24,
  },
  fallbackText: { color: '#90a4ae', fontSize: 14, textAlign: 'center' },
});
