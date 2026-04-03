import { useEffect, useRef, useState } from 'react';
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
const ENTRY_MS = 6000;

export function SiliconScreen() {
  const snapshot = useWorldStore((s) => s.snapshot);
  const batteryLevel = useWorldStore((s) => s.batteryLevel);

  const [entryProgress, setEntryProgress] = useState(0);
  const [showEntryOverlay, setShowEntryOverlay] = useState(true);
  const [typewriter, setTypewriter] = useState('');
  const [chromatic, setChromatic] = useState(false);
  const [tagline, setTagline] = useState(false);
  const flashDone = useRef(false);

  useEffect(() => {
    if (Platform.OS !== 'android' || !isSystemDataAvailable()) return;
    startPolling();
    return () => stopPolling();
  }, []);

  useEffect(() => {
    let mounted = true;
    let intervalId: ReturnType<typeof setInterval> | null = null;
    const fullTitle = 'ENTERING SILICON';
    (async () => {
      const done = await AsyncStorage.getItem(ENTRY_KEY);
      if (!mounted) return;
      if (done === '1') {
        setEntryProgress(1);
        setShowEntryOverlay(false);
        return;
      }
      const start = Date.now();
      let tw = 0;
      intervalId = setInterval(() => {
        const elapsed = Date.now() - start;
        const t = Math.min(1, elapsed / ENTRY_MS);
        if (mounted) setEntryProgress(t);
        if (tw < fullTitle.length && elapsed > 400) {
          tw = Math.min(fullTitle.length, Math.floor((elapsed - 400) / 80));
          setTypewriter(fullTitle.slice(0, tw));
        }
        if (t > 0.42 && t < 0.5 && !flashDone.current) {
          flashDone.current = true;
          setChromatic(true);
          setTimeout(() => mounted && setChromatic(false), 200);
        }
        if (t >= 0.92 && mounted) setTagline(true);
        if (t >= 1 && intervalId) {
          clearInterval(intervalId);
          intervalId = null;
          AsyncStorage.setItem(ENTRY_KEY, '1').catch(() => {});
          setTimeout(() => {
            if (mounted) {
              setShowEntryOverlay(false);
              setTagline(false);
            }
          }, 1200);
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

        {showEntryOverlay && entryProgress < 1 ? (
          <View style={styles.entryOverlay} pointerEvents="none">
            <Text style={styles.entryTitle}>{typewriter}</Text>
            {entryProgress > 0.15 ? (
              <Text style={styles.entryLine}>
                {Device.modelName ?? Device.deviceName ?? 'Android'}
              </Text>
            ) : null}
            {entryProgress > 0.22 ? (
              <Text style={styles.entryLine}>
                {cores} CORES · {totalRamGb.toFixed(2)}GB RAM · {storageGb.toFixed(2)}GB STORAGE
              </Text>
            ) : null}
            {entryProgress > 0.28 ? (
              <Text style={styles.entryLine}>BATTERY [{batteryLevel}%]</Text>
            ) : null}
            {tagline ? (
              <Text style={styles.tagline}>YOUR SILICON WORLD — TAKE CONTROL</Text>
            ) : null}
          </View>
        ) : null}

        {chromatic ? (
          <View style={styles.chromatic} pointerEvents="none">
            <View style={[styles.chromaBand, { left: -4, backgroundColor: 'rgba(255,0,0,0.12)' }]} />
            <View style={[styles.chromaBand, { left: 4, backgroundColor: 'rgba(0,255,100,0.1)' }]} />
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
    backgroundColor: 'rgba(0,0,8,0.55)',
  },
  entryTitle: {
    color: '#00ffe5',
    fontSize: 18,
    fontWeight: '700',
    letterSpacing: 3,
    marginBottom: 16,
    fontFamily: 'monospace',
  },
  entryLine: { color: '#90a4ae', fontSize: 12, marginBottom: 6, textAlign: 'center', fontFamily: 'monospace' },
  tagline: {
    marginTop: 24,
    color: 'rgba(0,255,229,0.55)',
    fontSize: 11,
    letterSpacing: 2,
    fontFamily: 'monospace',
  },
  chromatic: {
    ...StyleSheet.absoluteFillObject,
    overflow: 'hidden',
  },
  chromaBand: {
    ...StyleSheet.absoluteFillObject,
    position: 'absolute',
    width: '102%',
  },
  fallback: {
    flex: 1,
    backgroundColor: '#000008',
    justifyContent: 'center',
    padding: 24,
  },
  fallbackText: { color: '#90a4ae', fontSize: 14, textAlign: 'center' },
});
