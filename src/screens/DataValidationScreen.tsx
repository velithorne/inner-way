import { useEffect } from 'react';
import { Platform, ScrollView, StyleSheet, Text, View } from 'react-native';

import { isSystemDataAvailable } from '../native/systemData';
import { startPolling, stopPolling } from '../services/systemPoller';
import { useWorldStore } from '../store/useWorldStore';

function formatMb(bytes: number): string {
  return (bytes / (1024 * 1024)).toFixed(1);
}

function formatKbPerSec(bytesPerSec: number): string {
  return (bytesPerSec / 1024).toFixed(1);
}

export function DataValidationScreen() {
  const snapshot = useWorldStore((s) => s.snapshot);
  const topApps = useWorldStore((s) => s.topApps);

  useEffect(() => {
    if (Platform.OS !== 'android' || !isSystemDataAvailable()) {
      return;
    }
    startPolling();
    return () => stopPolling();
  }, []);

  if (Platform.OS !== 'android' || !isSystemDataAvailable()) {
    return (
      <View style={styles.root}>
        <Text style={styles.title}>SILICON — Data validation</Text>
        <Text style={styles.body}>
          System data collection runs on Android with the native SystemData module. Use a device or
          emulator with this build installed.
        </Text>
      </View>
    );
  }

  const cpu = snapshot?.cpu ?? [];
  const mem = snapshot?.memory;
  const net = snapshot?.network;
  const bat = snapshot?.battery;

  return (
    <ScrollView contentContainerStyle={styles.scroll} keyboardShouldPersistTaps="handled">
      <Text style={styles.title}>SILICON — Phase 1 validation</Text>
      <Text style={styles.sub}>
        Live readouts (500ms). Compare with Developer Options and system settings.
      </Text>

      <Text style={styles.section}>CPU (per core %)</Text>
      <Text style={styles.mono}>
        {cpu.length
          ? cpu.map((c) => `[${c.core}] ${c.usage}%`).join('  ')
          : '—'}
      </Text>

      <Text style={styles.section}>RAM</Text>
      <Text style={styles.mono}>
        {mem
          ? `${formatMb(mem.usedRam)} MB / ${formatMb(mem.totalRam)} MB (${formatMb(mem.availableRam)} MB free)${mem.lowMemory ? '  LOW_MEMORY' : ''}`
          : '—'}
      </Text>

      <Text style={styles.section}>Top apps by memory</Text>
      {topApps.slice(0, 5).map((a) => (
        <Text key={`${a.pid}-${a.packageName}`} style={styles.row}>
          {a.appName} · {formatMb(a.memoryBytes)} MB · {a.importance}
        </Text>
      ))}
      {!topApps.length ? <Text style={styles.mono}>—</Text> : null}

      <Text style={styles.section}>Network</Text>
      <Text style={styles.mono}>
        {net
          ? `RX ${formatKbPerSec(net.rxBytesPerSecond)} KB/s  TX ${formatKbPerSec(net.txBytesPerSecond)} KB/s`
          : '—'}
      </Text>

      <Text style={styles.section}>Battery</Text>
      <Text style={styles.mono}>
        {bat
          ? `${bat.level}% ${bat.isCharging ? 'charging' : 'discharging'} · ${bat.currentNow !== -1 ? `${bat.currentNow} µA` : 'current n/a'}`
          : '—'}
      </Text>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: '#0a0e14',
    padding: 20,
    justifyContent: 'center',
  },
  scroll: {
    paddingTop: 48,
    paddingBottom: 32,
    paddingHorizontal: 20,
  },
  title: {
    color: '#eceff1',
    fontSize: 20,
    fontWeight: '600',
    marginBottom: 8,
  },
  sub: {
    color: '#78909c',
    fontSize: 12,
    lineHeight: 18,
    marginBottom: 20,
  },
  body: {
    color: '#b0bec5',
    fontSize: 14,
    lineHeight: 20,
  },
  section: {
    color: '#00bcd4',
    fontSize: 13,
    fontWeight: '600',
    marginTop: 16,
    marginBottom: 6,
  },
  mono: {
    color: '#cfd8dc',
    fontSize: 13,
    fontVariant: ['tabular-nums'],
  },
  row: {
    color: '#b0bec5',
    fontSize: 13,
    marginBottom: 4,
  },
});
