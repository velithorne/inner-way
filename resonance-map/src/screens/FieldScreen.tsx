import React, { useEffect, useRef, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  StatusBar,
  Platform,
} from 'react-native';
import { useFocusEffect, useNavigation } from '@react-navigation/native';
import { activateKeepAwakeAsync, deactivateKeepAwake } from 'expo-keep-awake';
import { startMagnetometer, stopMagnetometer } from '../services/magnetometer';
import { logAnomaly } from '../services/anomalyLog';
import { useFieldStore } from '../store/useFieldStore';
import FieldCanvas from '../components/FieldCanvas';
import Waveform from '../components/Waveform';
import DataReadout from '../components/DataReadout';
import { Colors, Fonts, FontSizes, Spacing, BorderWidth } from '../constants/theme';

function UTCClock() {
  const [time, setTime] = React.useState('');

  useEffect(() => {
    const updateTime = () => {
      const now = new Date();
      setTime(now.toUTCString().replace('GMT', 'UTC'));
    };
    updateTime();
    const interval = setInterval(updateTime, 1000);
    return () => clearInterval(interval);
  }, []);

  return <Text style={styles.timestamp}>{time}</Text>;
}

export default function FieldScreen() {
  const navigation = useNavigation<any>();
  const isAnomaly = useFieldStore((s) => s.isAnomaly);
  const reading = useFieldStore((s) => s.reading);
  const anomalyDelta = useFieldStore((s) => s.anomalyDelta);
  const isCalibrated = useFieldStore((s) => s.isCalibrated);
  const prevAnomalyRef = useRef(false);

  useFocusEffect(
    useCallback(() => {
      activateKeepAwakeAsync();
      startMagnetometer();

      return () => {
        deactivateKeepAwake();
        stopMagnetometer();
      };
    }, [])
  );

  // Log anomaly transitions
  useEffect(() => {
    if (isAnomaly && !prevAnomalyRef.current) {
      logAnomaly({
        magnitude: reading.magnitude,
        delta: anomalyDelta,
        x: reading.x,
        y: reading.y,
        z: reading.z,
        heading: reading.heading,
      }).catch(() => {});
    }
    prevAnomalyRef.current = isAnomaly;
  }, [isAnomaly]);

  return (
    <View style={styles.root}>
      <StatusBar barStyle="light-content" backgroundColor={Colors.background} />

      {/* Top 15%: Header */}
      <View style={styles.header}>
        <Text style={styles.headerTitle}>RESONANCE MAP</Text>
        <UTCClock />
        <View style={styles.headerActions}>
          {!isCalibrated && (
            <TouchableOpacity
              style={styles.calibrateButton}
              onPress={() => navigation.navigate('Calibration')}
            >
              <Text style={styles.calibrateButtonText}>⬡ CALIBRATE</Text>
            </TouchableOpacity>
          )}
          <TouchableOpacity
            style={styles.logButton}
            onPress={() => navigation.navigate('AnomalyLog')}
          >
            <Text style={styles.logButtonText}>◈ LOG</Text>
          </TouchableOpacity>
        </View>
      </View>

      {/* Middle 55%: 3D Visualisation */}
      <View style={styles.canvas}>
        <FieldCanvas />
      </View>

      {/* Bottom 30%: Data panel + waveform */}
      <View style={styles.dataPanel}>
        <DataReadout />
        <Waveform />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  header: {
    height: '15%',
    backgroundColor: Colors.backgroundPanel,
    paddingHorizontal: Spacing.md,
    paddingTop: Platform.OS === 'android' ? Spacing.lg : Spacing.sm,
    paddingBottom: Spacing.xs,
    justifyContent: 'space-between',
    borderBottomWidth: BorderWidth.thin,
    borderBottomColor: Colors.cyan,
    flexDirection: 'row',
    alignItems: 'center',
    flexWrap: 'wrap',
  },
  headerTitle: {
    fontFamily: Fonts.header,
    fontSize: FontSizes.lg,
    color: Colors.cyan,
    letterSpacing: 4,
    textShadowColor: Colors.cyan,
    textShadowRadius: 8,
    textShadowOffset: { width: 0, height: 0 },
  },
  timestamp: {
    fontFamily: Fonts.mono,
    fontSize: FontSizes.xs,
    color: Colors.greyLight,
    letterSpacing: 0.5,
  },
  headerActions: {
    flexDirection: 'row',
    gap: Spacing.sm,
  },
  calibrateButton: {
    borderWidth: BorderWidth.thin,
    borderColor: Colors.gold,
    paddingHorizontal: Spacing.sm,
    paddingVertical: 3,
  },
  calibrateButtonText: {
    fontFamily: Fonts.mono,
    fontSize: FontSizes.xs,
    color: Colors.gold,
    letterSpacing: 1,
  },
  logButton: {
    borderWidth: BorderWidth.thin,
    borderColor: Colors.cyanDim,
    paddingHorizontal: Spacing.sm,
    paddingVertical: 3,
  },
  logButtonText: {
    fontFamily: Fonts.mono,
    fontSize: FontSizes.xs,
    color: Colors.cyan,
    letterSpacing: 1,
  },
  canvas: {
    height: '55%',
    backgroundColor: Colors.background,
    overflow: 'hidden',
  },
  dataPanel: {
    height: '30%',
    backgroundColor: Colors.backgroundPanel,
    justifyContent: 'space-between',
    overflow: 'hidden',
  },
});
