import { useEffect, useMemo, useRef, useState } from 'react';
import { Platform, StyleSheet, Text, View } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import * as Device from 'expo-device';
import { Gesture, GestureDetector, GestureHandlerRootView } from 'react-native-gesture-handler';

import { isSystemDataAvailable } from '../native/systemData';
import { startPolling, stopPolling } from '../services/systemPoller';
import { useWorldStore } from '../store/useWorldStore';
import { WorldEngine } from '../world/WorldEngine';
import { WORLD } from '../world/worldConstants';
import { batteryLevelToEmissive } from '../world/batteryColors';
import { getWorldCameraPosition } from '../world/worldCameraBridge';
import * as THREE from 'three';

const ENTRY_KEY = '@silicon_entry_done';

const ZONES: { name: string; pos: THREE.Vector3; r: number }[] = [
  { name: 'PROCESSOR CITY', pos: WORLD.processor.clone(), r: 140 },
  { name: 'RAM OCEAN', pos: WORLD.ramOcean.clone(), r: 220 },
  { name: 'STORAGE RANGE', pos: WORLD.storage.clone(), r: 160 },
  { name: 'SENSOR OUTPOSTS', pos: WORLD.sensors.clone(), r: 90 },
  { name: 'NETWORK SKY', pos: WORLD.networkSky.clone(), r: 180 },
  { name: 'DISPLAY / GLASS', pos: WORLD.display.clone(), r: 120 },
  { name: 'BATTERY SUN', pos: new THREE.Vector3(0, 0, 0), r: 80 },
];

export function SiliconScreen() {
  const snapshot = useWorldStore((s) => s.snapshot);
  const batteryLevel = useWorldStore((s) => s.batteryLevel);
  const telemetryOk = useWorldStore((s) => s.telemetryState === 'ok');

  const [entryProgress, setEntryProgress] = useState(0);
  const [orbital, setOrbital] = useState(false);
  const [showEntryText, setShowEntryText] = useState(true);
  const [zoneLabel, setZoneLabel] = useState('');
  const zoneTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    if (Platform.OS !== 'android' || !isSystemDataAvailable()) return;
    startPolling();
    return () => stopPolling();
  }, []);

  useEffect(() => {
    const id = setInterval(() => {
      const p = getWorldCameraPosition();
      let best: { name: string; d: number } | null = null;
      for (const z of ZONES) {
        const d = p.distanceTo(z.pos);
        if (d < z.r && (!best || d < best.d)) best = { name: z.name, d };
      }
      if (best) {
        setZoneLabel(best.name);
        if (zoneTimer.current) clearTimeout(zoneTimer.current);
        zoneTimer.current = setTimeout(() => setZoneLabel(''), 3000);
      }
    }, 200);
    return () => {
      clearInterval(id);
      if (zoneTimer.current) clearTimeout(zoneTimer.current);
    };
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

  const pinch = useMemo(
    () =>
      Gesture.Pinch()
        .onUpdate((e) => {
          if (e.scale > 1.35) setOrbital(true);
        })
        .onEnd(() => {
          setOrbital(false);
        }),
    [],
  );

  const cores = snapshot?.cpu?.length ?? 0;
  const totalRamGb = snapshot?.memory ? snapshot.memory.totalRam / (1024 ** 3) : 0;
  const storageGb = snapshot?.storage ? snapshot.storage.totalBytes / (1024 ** 3) : 0;

  const batColor = useMemo(() => {
    const c = batteryLevelToEmissive(batteryLevel);
    return `#${c.getHexString()}`;
  }, [batteryLevel]);

  if (Platform.OS !== 'android' || !isSystemDataAvailable()) {
    return (
      <View style={styles.fallback}>
        <Text style={styles.fallbackText}>SILICON world requires Android with the native SystemData build.</Text>
      </View>
    );
  }

  return (
    <GestureHandlerRootView style={styles.root}>
      <GestureDetector gesture={pinch}>
        <View style={styles.flex}>
          <WorldEngine entryProgress={entryProgress} orbital={orbital} />

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

          <View style={styles.hudLeft} pointerEvents="none">
            <Text style={[styles.zoneText, { opacity: zoneLabel ? 1 : 0 }]}>{zoneLabel}</Text>
          </View>

          <View style={styles.hudRight} pointerEvents="none">
            <Text style={[styles.batHud, { color: batColor }]}>
              [{batteryLevel}%]{telemetryOk ? ' ▼' : ''}
            </Text>
          </View>

          <View style={styles.hint} pointerEvents="none">
            <Text style={styles.hintText}>Tilt to fly · pinch out for orbital</Text>
          </View>
        </View>
      </GestureDetector>
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
  hudLeft: {
    position: 'absolute',
    top: 48,
    left: 16,
    maxWidth: '55%',
  },
  zoneText: {
    color: '#00bcd4',
    fontSize: 12,
    fontWeight: '700',
    letterSpacing: 1,
  },
  hudRight: {
    position: 'absolute',
    top: 48,
    right: 16,
  },
  batHud: {
    fontSize: 14,
    fontWeight: '700',
    fontVariant: ['tabular-nums'],
  },
  hint: {
    position: 'absolute',
    bottom: 28,
    alignSelf: 'center',
  },
  hintText: {
    color: '#546e7a',
    fontSize: 11,
  },
  fallback: {
    flex: 1,
    backgroundColor: '#000008',
    justifyContent: 'center',
    padding: 24,
  },
  fallbackText: { color: '#90a4ae', fontSize: 14, textAlign: 'center' },
});
