import { CameraView, useCameraPermissions } from 'expo-camera';
import { StatusBar } from 'expo-status-bar';
import { useEffect } from 'react';
import { Platform, StyleSheet, Text, TouchableOpacity, View } from 'react-native';

import { AcousticWireframe } from '../components/AcousticWireframe';
import { startContinuousMonitoring, stopContinuousMonitoring } from '../services/acousticShell';
import { useDecodeStore } from '../store/useDecodeStore';

export function DecodeScreen() {
  const [camPerm, requestCam] = useCameraPermissions();
  const roomGeometry = useDecodeStore((s) => s.roomGeometry);
  const sweepStatus = useDecodeStore((s) => s.sweepStatus);
  const layerAcoustic = useDecodeStore((s) => s.layerAcoustic);
  const layerMag = useDecodeStore((s) => s.layerMag);
  const layerRF = useDecodeStore((s) => s.layerRF);
  const layerGravity = useDecodeStore((s) => s.layerGravity);
  const layerDataRain = useDecodeStore((s) => s.layerDataRain);
  const setLayer = useDecodeStore((s) => s.setLayer);

  useEffect(() => {
    void requestCam();
  }, [requestCam]);

  useEffect(() => {
    if (camPerm?.granted) {
      void startContinuousMonitoring();
    }
    return () => stopContinuousMonitoring();
  }, [camPerm?.granted]);

  const hud =
    roomGeometry && !roomGeometry.isOpen ? (
      <Text style={styles.hud}>
        W: {roomGeometry.width.toFixed(1)}m H: {roomGeometry.height.toFixed(1)}m D:{' '}
        {roomGeometry.depth.toFixed(1)}m CONF: {Math.round(roomGeometry.confidence)}%
      </Text>
    ) : roomGeometry?.isOpen ? (
      <Text style={styles.hud}>OPEN FIELD · hem shell · CONF: {Math.round(roomGeometry.confidence)}%</Text>
    ) : (
      <Text style={styles.hud}>—</Text>
    );

  const sweepLabel =
    sweepStatus === 'measuring'
      ? 'MEASURING…'
      : sweepStatus === 'locked'
        ? 'ACOUSTIC SHELL LOCKED'
        : 'WAITING';

  return (
    <View style={styles.root}>
      <StatusBar style="light" />
      {camPerm?.granted ? (
        <CameraView style={StyleSheet.absoluteFill} facing="back" />
      ) : (
        <View style={styles.placeholder}>
          <Text style={styles.placeholderText}>Camera access is needed for DECODE overlay.</Text>
        </View>
      )}
      {layerAcoustic ? <AcousticWireframe geometry={roomGeometry} /> : null}

      <View style={styles.topLeft}>
        {hud}
        <Text style={styles.sweep}>{sweepLabel}</Text>
      </View>

      <View style={styles.layerBar}>
        <LayerBtn label="MAG" active={layerMag} dimmed onPress={() => setLayer('layerMag', !layerMag)} />
        <LayerBtn label="RF" active={layerRF} dimmed onPress={() => setLayer('layerRF', !layerRF)} />
        <LayerBtn label="ACOUSTIC" active={layerAcoustic} onPress={() => setLayer('layerAcoustic', !layerAcoustic)} />
        <LayerBtn label="GRAV" active={layerGravity} dimmed onPress={() => setLayer('layerGravity', !layerGravity)} />
        <LayerBtn label="RAIN" active={layerDataRain} dimmed onPress={() => setLayer('layerDataRain', !layerDataRain)} />
      </View>

      {Platform.OS === 'web' ? (
        <Text style={styles.webHint}>DECODE Phase 1 requires iOS or Android (camera + mic).</Text>
      ) : null}
    </View>
  );
}

function LayerBtn({
  label,
  active,
  dimmed,
  onPress,
}: {
  label: string;
  active: boolean;
  dimmed?: boolean;
  onPress: () => void;
}) {
  return (
    <TouchableOpacity
      style={[styles.layerBtn, active && styles.layerBtnOn, dimmed && styles.layerBtnDim]}
      onPress={onPress}
      activeOpacity={0.85}
    >
      <Text style={[styles.layerBtnText, active && styles.layerBtnTextOn, dimmed && styles.layerBtnTextDim]}>
        {label}
      </Text>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: '#000',
  },
  placeholder: {
    ...StyleSheet.absoluteFillObject,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#0a0e14',
  },
  placeholderText: {
    color: '#90a4ae',
    paddingHorizontal: 24,
    textAlign: 'center',
  },
  topLeft: {
    position: 'absolute',
    top: 52,
    left: 12,
    right: 12,
    gap: 6,
  },
  hud: {
    fontFamily: Platform.select({ ios: 'Menlo', android: 'monospace', default: 'monospace' }),
    fontSize: 11,
    color: '#00ffe5',
  },
  sweep: {
    fontFamily: Platform.select({ ios: 'Menlo', android: 'monospace', default: 'monospace' }),
    fontSize: 11,
    color: '#78909c',
  },
  layerBar: {
    position: 'absolute',
    bottom: 28,
    left: 8,
    right: 8,
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'center',
    gap: 6,
  },
  layerBtn: {
    paddingVertical: 8,
    paddingHorizontal: 10,
    borderRadius: 8,
    backgroundColor: 'rgba(28,37,46,0.85)',
    borderWidth: 1,
    borderColor: '#37474f',
  },
  layerBtnOn: {
    borderColor: '#00bcd4',
    backgroundColor: 'rgba(26,42,50,0.9)',
  },
  layerBtnDim: {
    opacity: 0.45,
  },
  layerBtnText: {
    color: '#90a4ae',
    fontSize: 11,
    fontWeight: '600',
  },
  layerBtnTextOn: {
    color: '#00bcd4',
  },
  layerBtnTextDim: {
    color: '#546e7a',
  },
  webHint: {
    position: 'absolute',
    bottom: 120,
    alignSelf: 'center',
    color: '#546e7a',
    fontSize: 12,
  },
});
