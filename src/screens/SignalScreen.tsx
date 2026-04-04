import { useCallback, useRef, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { Camera, useCameraDevice, useCameraPermission } from 'react-native-vision-camera';
import { WaveScene } from '../world/WaveScene';
import { useWifiStore } from '../store/useWifiStore';

export function SignalScreen() {
  const device = useCameraDevice('back');
  const { hasPermission, requestPermission } = useCameraPermission();
  const { networks, networkCount } = useWifiStore();
  const [showFps, setShowFps] = useState(false);
  const [fps, setFps] = useState<number | null>(null);
  const tapCount = useRef(0);
  const tapTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const onFps = useCallback((v: number) => {
    setFps(v);
  }, []);

  const onTripleTapHeader = () => {
    tapCount.current += 1;
    if (tapTimer.current) clearTimeout(tapTimer.current);
    tapTimer.current = setTimeout(() => {
      tapCount.current = 0;
    }, 450);
    if (tapCount.current >= 3) {
      setShowFps((s) => !s);
      tapCount.current = 0;
    }
  };

  if (!hasPermission) {
    return (
      <View style={styles.center}>
        <Text style={styles.msg}>Camera access is required for the AR wave field.</Text>
        <Pressable style={styles.btn} onPress={() => void requestPermission()}>
          <Text style={styles.btnText}>Grant camera</Text>
        </Pressable>
      </View>
    );
  }

  if (device == null) {
    return (
      <View style={styles.center}>
        <Text style={styles.msg}>No camera device found.</Text>
      </View>
    );
  }

  return (
    <View style={styles.root}>
      <Camera style={StyleSheet.absoluteFill} device={device} isActive />
      <WaveScene networks={networks} onFps={onFps} />
      <Pressable style={styles.hud} onPress={onTripleTapHeader}>
        <Text style={styles.hudTitle}>SIGNAL</Text>
        <Text style={styles.hudSub}>{networkCount} networks · wave rings</Text>
        {showFps ? (
          <Text style={styles.fps}>FPS {fps != null ? fps.toFixed(0) : '…'}</Text>
        ) : (
          <Text style={styles.hint}>Triple-tap for FPS</Text>
        )}
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: '#000' },
  center: {
    flex: 1,
    backgroundColor: '#0a0e14',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 24,
  },
  msg: {
    color: '#b0bec5',
    fontSize: 15,
    textAlign: 'center',
    marginBottom: 16,
  },
  btn: {
    backgroundColor: '#00838f',
    paddingVertical: 12,
    paddingHorizontal: 24,
    borderRadius: 10,
  },
  btnText: { color: '#fff', fontWeight: '700' },
  hud: {
    position: 'absolute',
    top: 48,
    left: 16,
    right: 16,
    padding: 12,
    backgroundColor: 'rgba(10, 14, 20, 0.72)',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: 'rgba(0, 255, 229, 0.25)',
  },
  hudTitle: {
    color: '#00ffe5',
    fontSize: 18,
    fontWeight: '800',
    letterSpacing: 2,
  },
  hudSub: {
    color: '#90a4ae',
    fontSize: 12,
    marginTop: 4,
  },
  fps: {
    color: '#00ffe5',
    fontSize: 13,
    fontWeight: '600',
    marginTop: 6,
  },
  hint: {
    color: '#546e7a',
    fontSize: 11,
    marginTop: 6,
  },
});
