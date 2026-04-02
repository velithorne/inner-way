import { StatusBar } from 'expo-status-bar';
import { useEffect, useRef, useState } from 'react';
import {
  Platform,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import { BPSGauge } from './src/components/BPSGauge';
import { startBioSensorFusion } from './src/services/bioSensor';
import { startAccelerometerCardiac } from './src/services/accelerometerCardiac';
import { startMagnetometerCardiac } from './src/services/magnetometerCardiac';
import { useBioStore } from './src/store/useBioStore';
import type { ScanMode } from './src/store/useBioStore';

export default function App() {
  const {
    bps,
    bpsLabel,
    accBpm,
    accConfidence,
    magFreqHz,
    magSnr,
    coherenceDetected,
    scanMode,
    setFusion,
  } = useBioStore();

  const fusionRef = useRef<ReturnType<typeof startBioSensorFusion> | null>(null);
  const accRef = useRef<ReturnType<typeof startAccelerometerCardiac> | null>(null);
  const magRef = useRef<ReturnType<typeof startMagnetometerCardiac> | null>(null);
  const [running, setRunning] = useState(false);

  useEffect(() => {
    return () => {
      fusionRef.current?.stop();
      accRef.current?.stop();
      magRef.current?.stop();
    };
  }, []);

  const toggleScan = () => {
    if (running) {
      fusionRef.current?.stop();
      accRef.current?.stop();
      magRef.current?.stop();
      fusionRef.current = null;
      accRef.current = null;
      magRef.current = null;
      setRunning(false);
      useBioStore.getState().reset();
      return;
    }

    const acc = startAccelerometerCardiac();
    const mag = startMagnetometerCardiac();
    accRef.current = acc;
    magRef.current = mag;
    fusionRef.current = startBioSensorFusion(acc, mag, { scanMode });
    setFusion({ isCalibrated: false });
    setRunning(true);
  };

  const setMode = (m: ScanMode) => {
    setFusion({ scanMode: m });
    if (running) {
      fusionRef.current?.stop();
      const acc = accRef.current!;
      const mag = magRef.current!;
      fusionRef.current = startBioSensorFusion(acc, mag, { scanMode: m });
    }
  };

  return (
    <View style={styles.root}>
      <StatusBar style="light" />
      <ScrollView contentContainerStyle={styles.scroll} keyboardShouldPersistTaps="handled">
        <Text style={styles.title}>Biofield Scanner</Text>
        <Text style={styles.disclaimer}>
          Experimental citizen science instrument. Readings are not medical diagnoses and do not
          prove biofields. Use only with informed consent when scanning others.
        </Text>

        <BPSGauge bps={bps} label={bpsLabel} />

        <View style={styles.row}>
          <Text style={styles.meta}>Mode: {scanMode}</Text>
          {coherenceDetected ? (
            <Text style={styles.coherence}>Coherence</Text>
          ) : null}
        </View>

        <View style={styles.stats}>
          <Text style={styles.statLine}>
            Acc: {accBpm.toFixed(0)} BPM · conf {accConfidence.toFixed(0)}%
          </Text>
          <Text style={styles.statLine}>
            Mag: {magFreqHz > 0 ? `${magFreqHz.toFixed(2)} Hz` : '—'} · SNR {magSnr.toFixed(1)}
          </Text>
        </View>

        <View style={styles.modes}>
          {(['CONTACT', 'PROXIMITY', 'SWEEP'] as const).map((m) => (
            <TouchableOpacity
              key={m}
              style={[styles.modeBtn, scanMode === m && styles.modeBtnActive]}
              onPress={() => setMode(m)}
            >
              <Text style={[styles.modeText, scanMode === m && styles.modeTextActive]}>{m}</Text>
            </TouchableOpacity>
          ))}
        </View>

        <TouchableOpacity
          style={[styles.scanBtn, running && styles.scanBtnStop]}
          onPress={toggleScan}
          activeOpacity={0.85}
        >
          <Text style={styles.scanBtnText}>{running ? 'STOP' : 'START'}</Text>
        </TouchableOpacity>

        {!running && Platform.OS === 'web' ? (
          <Text style={styles.hint}>Sensors require a physical device (Expo Go on Android/iOS).</Text>
        ) : null}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: '#0a0e14',
  },
  scroll: {
    paddingTop: 48,
    paddingBottom: 32,
    paddingHorizontal: 20,
    alignItems: 'center',
  },
  title: {
    color: '#eceff1',
    fontSize: 22,
    fontWeight: '600',
    marginBottom: 12,
  },
  disclaimer: {
    color: '#78909c',
    fontSize: 12,
    lineHeight: 18,
    textAlign: 'center',
    marginBottom: 16,
    maxWidth: 360,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    marginTop: 8,
  },
  meta: {
    color: '#90a4ae',
    fontSize: 13,
  },
  coherence: {
    color: '#00ffe5',
    fontSize: 13,
    fontWeight: '600',
  },
  stats: {
    marginTop: 16,
    alignSelf: 'stretch',
    maxWidth: 360,
  },
  statLine: {
    color: '#b0bec5',
    fontSize: 14,
    marginBottom: 6,
    fontVariant: ['tabular-nums'],
  },
  modes: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'center',
    gap: 8,
    marginTop: 20,
    marginBottom: 8,
  },
  modeBtn: {
    paddingVertical: 8,
    paddingHorizontal: 14,
    borderRadius: 8,
    backgroundColor: '#1c252e',
    borderWidth: 1,
    borderColor: '#37474f',
  },
  modeBtnActive: {
    borderColor: '#00bcd4',
    backgroundColor: '#1a2a32',
  },
  modeText: {
    color: '#90a4ae',
    fontSize: 12,
    fontWeight: '600',
  },
  modeTextActive: {
    color: '#00bcd4',
  },
  scanBtn: {
    marginTop: 20,
    backgroundColor: '#00838f',
    paddingVertical: 16,
    paddingHorizontal: 48,
    borderRadius: 12,
    minWidth: 200,
    alignItems: 'center',
  },
  scanBtnStop: {
    backgroundColor: '#c62828',
  },
  scanBtnText: {
    color: '#fff',
    fontSize: 18,
    fontWeight: '700',
    letterSpacing: 2,
  },
  hint: {
    marginTop: 16,
    color: '#546e7a',
    fontSize: 12,
    textAlign: 'center',
  },
});
