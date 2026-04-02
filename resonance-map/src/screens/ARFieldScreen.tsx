import React, { useEffect, useRef, useCallback, useState, useMemo } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Linking,
  Dimensions,
  Platform,
} from 'react-native';
import { useFocusEffect, useRoute } from '@react-navigation/native';
import { Camera, useCameraDevice, useCameraPermission } from 'react-native-vision-camera';
import * as Haptics from 'expo-haptics';
import * as Location from 'expo-location';
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withRepeat,
  withSequence,
  withTiming,
  Easing,
} from 'react-native-reanimated';

import ARFieldCanvas, { ARFieldCanvasHandle } from '../components/ARFieldCanvas';
import Waveform from '../components/Waveform';
import { useFieldStore } from '../store/useFieldStore';
// Magnetometer lifecycle managed globally in App.tsx
import { logAnomaly } from '../services/anomalyLog';
import { startRFScanner, stopRFScanner, subscribeRF, RFScanState } from '../services/rfScanner';
import { Colors, Fonts, FontSizes, Spacing, BorderWidth } from '../constants/theme';
import { FIELD_WEAK_MAX, FIELD_NORMAL_MAX } from '../constants/thresholds';
import { getNearbysSites, Nearbysite, haversineKm } from '../constants/sacredSites';
import { bearing } from '../utils/geo';

const { width: SCREEN_WIDTH } = Dimensions.get('window');

function getMagnitudeColor(magnitude: number, isAnomaly: boolean): string {
  if (isAnomaly) return Colors.gold;
  if (magnitude < FIELD_WEAK_MAX) return Colors.blueField;
  if (magnitude < FIELD_NORMAL_MAX) return Colors.cyan;
  return Colors.blueBright;
}

// ── Anomaly pulse dot ─────────────────────────────────────────────────────────
function AnomalyBadge({ visible }: { visible: boolean }) {
  const dotOpacity = useSharedValue(1);
  useEffect(() => {
    if (visible) {
      dotOpacity.value = withRepeat(
        withSequence(
          withTiming(0.2, { duration: 300 }),
          withTiming(1.0, { duration: 300 }),
        ),
        -1,
        false,
      );
    } else {
      dotOpacity.value = withTiming(1);
    }
  }, [visible]);
  const dotStyle = useAnimatedStyle(() => ({ opacity: dotOpacity.value }));

  if (!visible) return null;
  return (
    <Animated.View style={[badge.row, dotStyle]}>
      <View style={badge.dot} />
      <Text style={badge.text}>ANOMALY</Text>
    </Animated.View>
  );
}

const badge = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'center', gap: 5, marginTop: 4 },
  dot: { width: 7, height: 7, borderRadius: 3.5, backgroundColor: Colors.gold },
  text: { fontFamily: Fonts.mono, fontSize: 9, color: Colors.gold, letterSpacing: 1.5 },
});

// ── Site indicator arrow at screen edge ──────────────────────────────────────
function SiteIndicator({ entry, heading }: { entry: Nearbysite; heading: number }) {
  const relBearing = ((entry.bearingDeg - heading) + 360) % 360;
  const angle = (relBearing * Math.PI) / 180;
  const R = SCREEN_WIDTH / 2 - 48;
  const cx = SCREEN_WIDTH / 2;
  const cy = SCREEN_WIDTH * 0.7;
  const x = cx + R * Math.sin(angle);
  const y = cy - R * Math.cos(angle);

  // Clamp to screen edges
  const clampX = Math.max(24, Math.min(SCREEN_WIDTH - 24, x));
  const clampY = Math.max(80, Math.min(cy * 1.5, y));

  const strength = Math.round(entry.site.resonancePlaceholder * 100);

  return (
    <View style={[siteStyles.container, { left: clampX - 50, top: clampY - 20 }]}>
      <Text style={siteStyles.arrow}>
        {relBearing < 45 || relBearing > 315 ? '▲' :
         relBearing < 135 ? '▶' :
         relBearing < 225 ? '▼' : '◀'}
      </Text>
      <Text style={siteStyles.name}>{entry.site.name}</Text>
      <Text style={siteStyles.dist}>{Math.round(entry.distanceKm)} km</Text>
      {/* Phase 3: resonancePlaceholder replaced by live server value */}
      <View style={siteStyles.strengthBar}>
        <View style={[siteStyles.strengthFill, { width: `${strength}%` as any }]} />
      </View>
    </View>
  );
}

const siteStyles = StyleSheet.create({
  container: {
    position: 'absolute',
    alignItems: 'center',
    width: 100,
    backgroundColor: 'rgba(0,0,10,0.55)',
    borderWidth: 0.5,
    borderColor: Colors.gold,
    padding: 4,
  },
  arrow: { fontSize: 12, color: Colors.gold },
  name: { fontFamily: Fonts.mono, fontSize: 8, color: Colors.gold, letterSpacing: 0.5, textAlign: 'center' },
  dist: { fontFamily: Fonts.mono, fontSize: 8, color: Colors.greyLight, marginTop: 1 },
  strengthBar: { width: '100%', height: 2, backgroundColor: Colors.greyDark, marginTop: 3 },
  strengthFill: { height: 2, backgroundColor: Colors.gold, opacity: 0.7 },
});

// ── Permission denied view ────────────────────────────────────────────────────
function CameraPermissionDenied() {
  return (
    <View style={permStyles.container}>
      <Text style={permStyles.icon}>◈</Text>
      <Text style={permStyles.title}>CAMERA ACCESS REQUIRED</Text>
      <Text style={permStyles.body}>
        AR MODE REQUIRES CAMERA ACCESS{'\n'}TAP TO ENABLE IN SETTINGS
      </Text>
      <TouchableOpacity style={permStyles.btn} onPress={() => Linking.openSettings()}>
        <Text style={permStyles.btnText}>OPEN SETTINGS</Text>
      </TouchableOpacity>
    </View>
  );
}

const permStyles = StyleSheet.create({
  container: {
    flex: 1, backgroundColor: Colors.background,
    alignItems: 'center', justifyContent: 'center', gap: Spacing.md,
    paddingHorizontal: Spacing.xl,
  },
  icon: { fontSize: 40, color: Colors.grey },
  title: { fontFamily: Fonts.header, fontSize: FontSizes.md, color: Colors.cyan, letterSpacing: 3, textAlign: 'center' },
  body: { fontFamily: Fonts.mono, fontSize: FontSizes.sm, color: Colors.greyLight, textAlign: 'center', lineHeight: 22 },
  btn: { borderWidth: BorderWidth.normal, borderColor: Colors.cyan, paddingHorizontal: Spacing.xl, paddingVertical: Spacing.sm, marginTop: Spacing.md },
  btnText: { fontFamily: Fonts.mono, fontSize: FontSizes.sm, color: Colors.cyan, letterSpacing: 2 },
});

// ── Main AR screen ────────────────────────────────────────────────────────────
// ── AR target directional overlay ────────────────────────────────────────
function TargetOverlay({ targetLat, targetLng, userLat, userLng, heading }: {
  targetLat: number; targetLng: number;
  userLat: number; userLng: number;
  heading: number;
}) {
  const targetBearing = bearing(
    { latitude: userLat, longitude: userLng },
    { latitude: targetLat, longitude: targetLng }
  );
  const relAngle = ((targetBearing - heading) + 360) % 360;
  const distKm = haversineKm(userLat, userLng, targetLat, targetLng);
  const arrowLabel = relAngle < 22.5 || relAngle > 337.5 ? '▲' :
                     relAngle < 67.5  ? '↗' :
                     relAngle < 112.5 ? '▶' :
                     relAngle < 157.5 ? '↘' :
                     relAngle < 202.5 ? '▼' :
                     relAngle < 247.5 ? '↙' :
                     relAngle < 292.5 ? '◀' : '↖';
  return (
    <View style={tgt.container} pointerEvents="none">
      <Text style={tgt.arrow}>{arrowLabel}</Text>
      <Text style={tgt.label}>TARGET NODE</Text>
      <Text style={tgt.dist}>{distKm < 1 ? `${Math.round(distKm * 1000)}m` : `${distKm.toFixed(1)}km`}</Text>
      <Text style={tgt.bearing}>{Math.round(targetBearing)}° / {Math.round(relAngle)}° rel</Text>
    </View>
  );
}

const tgt = StyleSheet.create({
  container: {
    position: 'absolute',
    top: '35%',
    alignSelf: 'center',
    alignItems: 'center',
    backgroundColor: 'rgba(0,0,10,0.7)',
    borderWidth: 1,
    borderColor: Colors.gold,
    paddingHorizontal: Spacing.md,
    paddingVertical: Spacing.sm,
  },
  arrow: { fontSize: 32, color: Colors.gold },
  label: { fontFamily: Fonts.mono, fontSize: 9, color: Colors.gold, letterSpacing: 2, marginTop: 2 },
  dist: { fontFamily: Fonts.header, fontSize: FontSizes.xl, color: Colors.gold, marginTop: 2 },
  bearing: { fontFamily: Fonts.mono, fontSize: 9, color: Colors.greyLight, marginTop: 2 },
});

export default function ARFieldScreen() {
  const { hasPermission, requestPermission } = useCameraPermission();
  const device = useCameraDevice('back');
  const route = useRoute<any>();

  // Throttled to 4Hz for the HUD display layer
  const [hudData, setHudData] = useState({
    magnitude: 0, heading: 0, isAnomaly: false, isBaselineReady: false, isSimulationMode: false,
  });
  useEffect(() => {
    const id = setInterval(() => {
      const s = useFieldStore.getState();
      setHudData({
        magnitude: s.reading.magnitude,
        heading: s.reading.heading,
        isAnomaly: s.isAnomaly,
        isBaselineReady: s.isBaselineReady,
        isSimulationMode: s.isSimulationMode,
      });
    }, 250);
    return () => clearInterval(id);
  }, []);
  const { isAnomaly, isBaselineReady, isSimulationMode } = hudData;

  const [nearbySites, setNearbySites] = useState<Nearbysite[]>([]);
  const [userLat, setUserLat] = useState<number | null>(null);
  const [userLng, setUserLng] = useState<number | null>(null);
  const prevAnomalyRef = useRef(false);
  const anomalyTripleRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Field layer toggles
  const [layerMag, setLayerMag] = useState(true);
  const [layerRF, setLayerRF] = useState(true);
  const [layerGrav, setLayerGrav] = useState(false); // off by default — particles flood screen when on
  const canvasLayerRef = useRef<ARFieldCanvasHandle | null>(null);

  // RF scan state (for HUD readout)
  const [rfState, setRfState] = useState<RFScanState>({ networks: [], isScanning: false, isIOS: false, lastScanMs: 0 });

  // Convergence badge from canvas
  const [showConvergence, setShowConvergence] = useState(false);
  const convergenceTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const handleConvergence = useCallback((converging: boolean) => {
    if (converging) {
      setShowConvergence(true);
      Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium).catch(() => {});
      if (convergenceTimerRef.current) clearTimeout(convergenceTimerRef.current);
      // Auto-dismiss after 3 seconds — never blocks the view
      convergenceTimerRef.current = setTimeout(() => setShowConvergence(false), 3000);
    } else {
      if (convergenceTimerRef.current) clearTimeout(convergenceTimerRef.current);
      setShowConvergence(false);
    }
  }, []);

  // Surface echo / RF wall badge (derived from Z-axis spike)
  const [showWallRF, setShowWallRF] = useState(false);

  // First-launch calibration overlay — shows live sensor values
  const [showCalibOverlay, setShowCalibOverlay] = useState(true);
  useEffect(() => {
    const t = setTimeout(() => setShowCalibOverlay(false), 5000);
    return () => clearTimeout(t);
  }, []);

  // Layer toggle handler — tells canvas which fields to show
  const setLayer = useCallback((field: 'mag' | 'rf' | 'grav' | 'all', val: boolean) => {
    const newMag  = field === 'all' ? val : field === 'mag'  ? val : layerMag;
    const newRF   = field === 'all' ? val : field === 'rf'   ? val : layerRF;
    const newGrav = field === 'all' ? val : field === 'grav' ? val : layerGrav;
    setLayerMag(newMag); setLayerRF(newRF); setLayerGrav(newGrav);
    canvasLayerRef.current?.setLayers(newMag, newRF, newGrav);
  }, [layerMag, layerRF, layerGrav]);

  // Request camera permission
  useEffect(() => {
    if (!hasPermission) requestPermission();
  }, [hasPermission]);

  useFocusEffect(
    useCallback(() => {
      Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium).catch(() => {});
      startRFScanner();

      (async () => {
        try {
          const { status } = await Location.requestForegroundPermissionsAsync();
          if (status === 'granted') {
            const loc = await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.Balanced });
            setUserLat(loc.coords.latitude);
            setUserLng(loc.coords.longitude);
            setNearbySites(getNearbysSites(loc.coords.latitude, loc.coords.longitude));
          }
        } catch {}
      })();

      const unsubRF = subscribeRF(setRfState);
      return () => {
        stopRFScanner();
        unsubRF();
        if (anomalyTripleRef.current) clearTimeout(anomalyTripleRef.current);
      };
    }, [])
  );

  // Anomaly: real-time subscribe (not throttled) for haptics + logging
  useEffect(() => {
    return useFieldStore.subscribe((s) => {
      const nowAnomaly = s.isAnomaly;

      if (nowAnomaly && !prevAnomalyRef.current) {
        // Log to AsyncStorage
        logAnomaly({
          magnitude: s.reading.magnitude,
          delta: s.anomalyDelta,
          x: s.reading.x,
          y: s.reading.y,
          z: s.reading.z,
          heading: s.reading.heading,
        }).catch(() => {});

        // Haptics: heavy + triple-pulse
        Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Heavy).catch(() => {});
        const pulse = (n: number) => {
          if (n <= 0) return;
          anomalyTripleRef.current = setTimeout(() => {
            Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light).catch(() => {});
            pulse(n - 1);
          }, 200);
        };
        pulse(3);
      }

      if (!nowAnomaly && prevAnomalyRef.current) {
        Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Soft).catch(() => {});
      }

      prevAnomalyRef.current = nowAnomaly;
    });
  }, []);

  const magnitudeColor = getMagnitudeColor(hudData.magnitude, isAnomaly);

  if (!hasPermission) {
    return <CameraPermissionDenied />;
  }

  if (!device) {
    return (
      <View style={styles.noCamera}>
        <Text style={styles.noCameraText}>NO REAR CAMERA DETECTED</Text>
      </View>
    );
  }

  return (
    <View style={styles.root}>
      {/* Layer 0: Live camera feed */}
      <Camera
        style={StyleSheet.absoluteFill}
        device={device}
        isActive={true}
        photo={false}
        video={false}
        audio={false}
      />

      {/* Layer 1: Multi-field canvas — magnetic dipole + RF + gravity */}
      <ARFieldCanvas layerRef={canvasLayerRef} onConvergence={handleConvergence} />

      {/* Target node directional overlay (from MAP tab "OPEN IN AR") */}
      {route.params?.targetLat != null && userLat !== null && userLng !== null && (
        <TargetOverlay
          targetLat={route.params.targetLat as number}
          targetLng={route.params.targetLng as number}
          userLat={userLat}
          userLng={userLng}
          heading={hudData.heading}
        />
      )}

      {/* Layer 2: HUD */}

      {/* Top-left: app label + active fields */}
      <View style={styles.topLeft} pointerEvents="none">
        <Text style={styles.appLabel}>◈ RESONANCE MAP  FIELD SCANNER</Text>
        <View style={styles.activeFields}>
          {layerMag  && <Text style={[styles.fieldTag, { color: Colors.cyan }]}>MAG</Text>}
          {layerMag  && layerRF  && <Text style={styles.fieldDot}> · </Text>}
          {layerRF   && <Text style={[styles.fieldTag, { color: Colors.gold }]}>RF</Text>}
          {(layerRF || layerMag) && layerGrav && <Text style={styles.fieldDot}> · </Text>}
          {layerGrav && <Text style={[styles.fieldTag, { color: '#1a4a8a' }]}>GRAV</Text>}
        </View>
        {isSimulationMode && (
          <Text style={styles.simLabel}>SIMULATION</Text>
        )}
        {!isBaselineReady && (
          <Text style={styles.calibratingLabel}>CALIBRATING...</Text>
        )}
      </View>

      {/* Top-right: multi-field readout */}
      <View style={styles.topRight} pointerEvents="none">
        <Text style={[styles.magnitudeValue, { color: magnitudeColor }]}>
          {hudData.magnitude.toFixed(1)}
        </Text>
        <Text style={styles.magnitudeUnit}>µT MAG</Text>
        {rfState.networks.length > 0 && (
          <Text style={styles.rfReadout}>
            {rfState.networks[0].rssi} dBm RF
          </Text>
        )}
        {isBaselineReady
          ? <AnomalyBadge visible={isAnomaly} />
          : <Text style={styles.warmupLabel}>WARMUP</Text>
        }
      </View>

      {/* Right-side field layer controls */}
      <View style={styles.layerControls}>
        {[
          { key: 'mag',  icon: '🔵', label: 'MAG',  active: layerMag,  color: Colors.cyan },
          { key: 'rf',   icon: '🟡', label: 'RF',   active: layerRF,   color: Colors.gold },
          { key: 'grav', icon: '🔷', label: 'GRAV', active: layerGrav, color: '#1a4a8a' },
        ].map(({ key, icon, label, active, color }) => (
          <TouchableOpacity
            key={key}
            style={[styles.layerBtn, active && { borderColor: color }]}
            onPress={() => setLayer(key as any, !active)}
            activeOpacity={0.7}
          >
            <View style={[styles.layerDot, { backgroundColor: active ? color : Colors.grey }]} />
            <Text style={[styles.layerBtnText, { color: active ? color : Colors.grey }]}>{label}</Text>
          </TouchableOpacity>
        ))}
        <TouchableOpacity
          style={styles.layerBtn}
          onPress={() => setLayer('all', !(layerMag && layerRF && layerGrav))}
          activeOpacity={0.7}
        >
          <Text style={styles.layerBtnText}>ALL</Text>
        </TouchableOpacity>
      </View>

      {/* Nearby sacred site indicators */}
      {nearbySites.map((entry) => (
        <SiteIndicator key={entry.site.id} entry={entry} heading={hudData.heading} />
      ))}

      {/* Bottom bar: waveform + heading */}
      <View style={styles.bottomBar} pointerEvents="none">
        <View style={styles.waveformWrapper}>
          <Waveform />
        </View>
        <View style={styles.bottomStats}>
          <Text style={styles.headingLabel}>HDG</Text>
          <Text style={[styles.headingValue, { color: magnitudeColor }]}>
            {hudData.heading.toFixed(0)}°
          </Text>
          <Text style={styles.baselineLabel}>
            BASE  {useFieldStore.getState().rollingAverage.toFixed(1)} µT
          </Text>
          {rfState.isIOS && (
            <Text style={styles.iosNote}>iOS: cellular RF only</Text>
          )}
        </View>
      </View>

      {/* Convergence toast — small, 3s, non-blocking */}
      {showConvergence && (
        <View style={styles.convergenceToast} pointerEvents="none">
          <Text style={styles.convergenceToastText}>
            ◈ AXIS CONVERGENCE  Mag/Grav aligned
          </Text>
        </View>
      )}

      {/* First-launch calibration overlay with live sensor values */}
      {showCalibOverlay && (
        <View style={styles.calibOverlay} pointerEvents="none">
          <Text style={styles.calibTitle}>◈ FIELD SCANNER — ACTIVE</Text>

          <Text style={[styles.calibLegendLine, { color: Colors.cyan, marginBottom: 4 }]}>
            CYAN   Magnetic dipole field
          </Text>
          <Text style={styles.calibSubLine}>
            axis  {hudData.magnitude.toFixed(1)} µT  {hudData.heading.toFixed(0)}°
          </Text>

          <Text style={[styles.calibLegendLine, { color: Colors.gold, marginTop: 10, marginBottom: 4 }]}>
            GOLD   RF radiation wavefronts
          </Text>
          <Text style={styles.calibSubLine}>
            {rfState.networks.length} source{rfState.networks.length !== 1 ? 's' : ''} detected
            {rfState.networks.length > 0 ? `  ·  ${rfState.networks[0].rssi} dBm` : ''}
          </Text>

          <Text style={[styles.calibLegendLine, { color: '#4488cc', marginTop: 10, marginBottom: 4 }]}>
            BLUE   Gravitational field
          </Text>
          <Text style={styles.calibSubLine}>
            {Math.sqrt(
              useFieldStore.getState().reading.x**2 +
              useFieldStore.getState().reading.y**2 +
              useFieldStore.getState().reading.z**2
            ).toFixed(1)} µT total field
          </Text>

          <Text style={[styles.calibFooter, { marginTop: 20 }]}>
            All fields are physically real.{'\n'}
            Invisible to human sight.{'\n'}
            Rendered here at true geometry.
          </Text>
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  noCamera: {
    flex: 1,
    backgroundColor: Colors.background,
    alignItems: 'center',
    justifyContent: 'center',
  },
  noCameraText: {
    fontFamily: Fonts.mono,
    fontSize: FontSizes.sm,
    color: Colors.grey,
    letterSpacing: 2,
  },

  // HUD top-left
  topLeft: {
    position: 'absolute',
    top: Platform.OS === 'android' ? 48 : 56,
    left: Spacing.md,
  },
  appLabel: {
    fontFamily: Fonts.mono,
    fontSize: 10,
    color: Colors.cyan,
    opacity: 0.6,
    letterSpacing: 1,
  },
  simLabel: {
    fontFamily: Fonts.mono,
    fontSize: 9,
    color: Colors.gold,
    opacity: 0.8,
    letterSpacing: 1.5,
    marginTop: 2,
  },
  calibratingLabel: {
    fontFamily: Fonts.mono,
    fontSize: 9,
    color: Colors.greyLight,
    opacity: 0.7,
    letterSpacing: 1.5,
    marginTop: 2,
  },
  warmupLabel: {
    fontFamily: Fonts.mono,
    fontSize: 9,
    color: Colors.greyLight,
    opacity: 0.6,
    letterSpacing: 1.5,
    marginTop: 3,
  },

  // HUD top-right
  topRight: {
    position: 'absolute',
    top: Platform.OS === 'android' ? 44 : 52,
    right: Spacing.md,
    alignItems: 'flex-end',
  },
  magnitudeValue: {
    fontFamily: Fonts.header,
    fontSize: 28,
    lineHeight: 32,
  },
  magnitudeUnit: {
    fontFamily: Fonts.mono,
    fontSize: 11,
    color: Colors.greyLight,
    opacity: 0.7,
  },

  // Bottom bar
  bottomBar: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    height: '28%',
    backgroundColor: 'transparent',
    // Gradient via a dark overlay at bottom
    borderTopWidth: 0,
    paddingBottom: Spacing.lg,
    justifyContent: 'flex-end',
  },
  waveformWrapper: {
    opacity: 0.75,
    marginBottom: 4,
    // Scale down the waveform height
    transform: [{ scaleY: 0.5 }],
    transformOrigin: 'bottom',
  },
  bottomStats: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: Spacing.lg,
    gap: Spacing.md,
    backgroundColor: 'rgba(0,0,10,0.6)',
    paddingVertical: Spacing.xs,
  },
  headingLabel: {
    fontFamily: Fonts.mono,
    fontSize: FontSizes.xs,
    color: Colors.greyLight,
    letterSpacing: 1.5,
  },
  headingValue: {
    fontFamily: Fonts.header,
    fontSize: FontSizes.lg,
  },
  baselineLabel: {
    fontFamily: Fonts.mono,
    fontSize: FontSizes.xs,
    color: Colors.greyLight,
    opacity: 0.7,
    letterSpacing: 1,
    marginLeft: 'auto',
  },
  iosNote: {
    fontFamily: Fonts.mono,
    fontSize: 8,
    color: Colors.grey,
    opacity: 0.6,
    marginLeft: Spacing.sm,
  },

  // Active field tags
  activeFields: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 3,
  },
  fieldTag: {
    fontFamily: Fonts.mono,
    fontSize: 9,
    letterSpacing: 1.5,
  },
  fieldDot: {
    fontFamily: Fonts.mono,
    fontSize: 9,
    color: Colors.grey,
  },

  // RF readout
  rfReadout: {
    fontFamily: Fonts.mono,
    fontSize: 10,
    color: Colors.gold,
    opacity: 0.8,
    marginTop: 2,
  },

  // Right-side layer controls
  layerControls: {
    position: 'absolute',
    right: Spacing.sm,
    top: '35%',
    gap: Spacing.xs,
    alignItems: 'flex-end',
  },
  layerBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 5,
    backgroundColor: 'rgba(0,0,10,0.72)',
    borderWidth: BorderWidth.thin,
    borderColor: Colors.grey,
    paddingHorizontal: 8,
    paddingVertical: 5,
  },
  layerDot: {
    width: 6,
    height: 6,
    borderRadius: 3,
  },
  layerBtnText: {
    fontFamily: Fonts.mono,
    fontSize: 9,
    letterSpacing: 1.5,
  },

  // Calibration overlay
  calibOverlay: {
    position: 'absolute',
    top: 0, left: 0, right: 0, bottom: 0,
    backgroundColor: 'rgba(0,0,10,0.78)',
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: Spacing.xl,
    zIndex: 100,
  },
  calibTitle: {
    fontFamily: Fonts.header,
    fontSize: FontSizes.lg,
    color: Colors.cyan,
    letterSpacing: 3,
    textAlign: 'center',
    marginBottom: Spacing.md,
  },
  calibBody: {
    fontFamily: Fonts.mono,
    fontSize: FontSizes.sm,
    color: Colors.greyLight,
    textAlign: 'center',
    lineHeight: 22,
    marginBottom: Spacing.lg,
  },
  calibLegend: {
    gap: Spacing.sm,
    marginBottom: Spacing.lg,
    alignItems: 'flex-start',
  },
  calibLegendLine: {
    fontFamily: Fonts.mono,
    fontSize: FontSizes.sm,
    letterSpacing: 1,
  },
  calibSubLine: {
    fontFamily: Fonts.mono,
    fontSize: 9,
    color: Colors.greyLight,
    opacity: 0.7,
    letterSpacing: 0.5,
    marginLeft: 12,
  },
  calibFooter: {
    fontFamily: Fonts.mono,
    fontSize: FontSizes.xs,
    color: Colors.greyLight,
    opacity: 0.5,
    letterSpacing: 1,
    textAlign: 'center',
    lineHeight: 18,
  },

  // Convergence toast — compact, 48px, top of screen below HUD
  convergenceToast: {
    position: 'absolute',
    top: 155,
    alignSelf: 'center',
    backgroundColor: 'rgba(0,0,0,0.7)',
    paddingHorizontal: Spacing.md,
    paddingVertical: 8,
    height: 48,
    justifyContent: 'center',
  },
  convergenceToastText: {
    fontFamily: Fonts.mono,
    fontSize: FontSizes.xs,
    color: Colors.cyan,
    letterSpacing: 1.5,
  },
});
