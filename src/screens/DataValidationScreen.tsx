import { useEffect } from 'react';
import { Platform, ScrollView, StyleSheet, Text, View } from 'react-native';

import { isSystemDataAvailable } from '../native/systemData';
import { startPolling, stopPolling } from '../services/systemPoller';
import { useWorldStore } from '../store/useWorldStore';

function formatMb(bytes: number): string {
  return (bytes / (1024 * 1024)).toFixed(1);
}

function formatGb(bytes: number): string {
  return (bytes / (1024 * 1024 * 1024)).toFixed(2);
}

function formatKbPerSec(bytesPerSec: number): string {
  return (bytesPerSec / 1024).toFixed(1);
}

export function DataValidationScreen() {
  const snapshot = useWorldStore((s) => s.snapshot);
  const topApps = useWorldStore((s) => s.topApps);
  const telemetryState = useWorldStore((s) => s.telemetryState);
  const telemetryMessage = useWorldStore((s) => s.telemetryMessage);

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
          System data needs the native SystemData module. This screen does not populate in Expo Go.
          Install the release APK or run{' '}
          <Text style={styles.codeInline}>npx expo run:android</Text> for a dev build.
        </Text>
      </View>
    );
  }

  const cpu = snapshot?.cpu ?? [];
  const mem = snapshot?.memory;
  const net = snapshot?.network;
  const bat = snapshot?.battery;
  const storage = snapshot?.storage;

  return (
    <ScrollView contentContainerStyle={styles.scroll} keyboardShouldPersistTaps="handled">
      <Text style={styles.title}>SILICON — Phase 1 validation</Text>
      <Text style={styles.sub}>
        Live readouts (500ms). Compare with Developer Options and system settings.
      </Text>

      {telemetryState === 'loading' && !snapshot ? (
        <Text style={styles.hint}>Reading device telemetry…</Text>
      ) : null}

      {telemetryState === 'error' && telemetryMessage ? (
        <View style={styles.errorBox}>
          <Text style={styles.errorTitle}>Telemetry error</Text>
          <Text style={styles.errorBody}>{telemetryMessage}</Text>
        </View>
      ) : null}

      <Text style={styles.section}>CPU (cpufreq proxy %)</Text>
      <Text style={styles.caption}>cur/max GHz per core — not CPU time; good for world “load” visuals.</Text>
      <Text style={styles.mono}>
        {cpu.length
          ? cpu
              .map((c) => {
                const ghz =
                  c.curFreqKhz != null && c.maxFreqKhz != null
                    ? `${(c.curFreqKhz / 1_000_000).toFixed(2)}/${(c.maxFreqKhz / 1_000_000).toFixed(2)}`
                    : null;
                return `[${c.core}] ${c.usage}%${ghz ? ` ${ghz}GHz` : ''}`;
              })
              .join('  ')
          : '—'}
      </Text>

      <Text style={styles.section}>RAM</Text>
      <Text style={styles.mono}>
        {mem
          ? `${formatMb(mem.usedRam)} MB / ${formatMb(mem.totalRam)} MB (${formatMb(mem.availableRam)} MB free)${mem.lowMemory ? '  LOW_MEMORY' : ''}${mem.memoryClassMb != null ? ` · memoryClass ${mem.memoryClassMb} MB` : ''}`
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
          ? `${bat.level}% ${bat.isCharging ? 'charging' : 'discharging'} · ${bat.currentNow !== -1 ? `${bat.currentNow} µA` : 'current n/a'}${bat.powerWatts >= 0 ? ` · ~${bat.powerWatts.toFixed(2)} W` : ''}`
          : '—'}
      </Text>

      <Text style={styles.section}>Storage</Text>
      <Text style={styles.mono}>
        {storage
          ? `${formatGb(storage.usedBytes)} / ${formatGb(storage.totalBytes)} GB (data partition) · app ${formatMb(storage.appDataBytes)} MB${
              storage.externalTotalBytes != null &&
              storage.externalTotalBytes > 0 &&
              storage.externalFreeBytes != null
                ? ` · ext ${formatGb(storage.externalTotalBytes - storage.externalFreeBytes)} / ${formatGb(storage.externalTotalBytes)} GB used`
                : ''
            }`
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
  caption: {
    color: '#546e7a',
    fontSize: 11,
    lineHeight: 16,
    marginBottom: 6,
    marginTop: -8,
  },
  hint: {
    color: '#90a4ae',
    fontSize: 13,
    marginBottom: 12,
  },
  errorBox: {
    backgroundColor: '#1b1f24',
    borderWidth: 1,
    borderColor: '#c62828',
    borderRadius: 8,
    padding: 12,
    marginBottom: 16,
  },
  errorTitle: {
    color: '#ef9a9a',
    fontSize: 13,
    fontWeight: '600',
    marginBottom: 6,
  },
  errorBody: {
    color: '#ffcdd2',
    fontSize: 12,
    lineHeight: 18,
  },
  codeInline: {
    fontFamily: 'monospace',
    color: '#80deea',
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
