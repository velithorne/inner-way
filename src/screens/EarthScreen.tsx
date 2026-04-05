import firestore from '@react-native-firebase/firestore';
import {
  Fragment,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import { Dimensions, Platform, StyleSheet, Text, View } from 'react-native';
import MapView, {
  Circle,
  Marker,
  PROVIDER_GOOGLE,
  Region,
} from 'react-native-maps';
import Animated, {
  Easing,
  useAnimatedStyle,
  useSharedValue,
  withRepeat,
  withSequence,
  withTiming,
} from 'react-native-reanimated';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { COLORS } from '../constants/theme';
import { useSpectraLocation } from '../hooks/useSpectraLocation';

const { width } = Dimensions.get('window');

type SignalDoc = {
  id: string;
  lat: number;
  lng: number;
  bssid: string;
  frequency: number;
  sessionId: string;
};

function regionToBounds(r: Region) {
  const latMin = r.latitude - r.latitudeDelta / 2;
  const latMax = r.latitude + r.latitudeDelta / 2;
  const lngMin = r.longitude - r.longitudeDelta / 2;
  const lngMax = r.longitude + r.longitudeDelta / 2;
  return { latMin, latMax, lngMin, lngMax };
}

export function EarthScreen() {
  const insets = useSafeAreaInsets();
  const loc = useSpectraLocation(true);
  const mapRef = useRef<MapView>(null);
  const [signals, setSignals] = useState<SignalDoc[]>([]);
  const [sessionCount, setSessionCount] = useState(0);
  const hasCentered = useRef(false);

  const pulse = useSharedValue(1);
  useEffect(() => {
    pulse.value = withRepeat(
      withSequence(
        withTiming(0.35, { duration: 900, easing: Easing.inOut(Easing.ease) }),
        withTiming(1, { duration: 900, easing: Easing.inOut(Easing.ease) }),
      ),
      -1,
      true,
    );
  }, [pulse]);

  const dotPulse = useAnimatedStyle(() => ({
    opacity: pulse.value,
    transform: [{ scale: 0.85 + pulse.value * 0.15 }],
  }));

  const initialRegion = useMemo((): Region => {
    if (loc.lat != null && loc.lng != null) {
      return {
        latitude: loc.lat,
        longitude: loc.lng,
        latitudeDelta: 0.05,
        longitudeDelta: 0.05,
      };
    }
    return {
      latitude: 37.7749,
      longitude: -122.4194,
      latitudeDelta: 0.2,
      longitudeDelta: 0.2,
    };
  }, [loc.lat, loc.lng]);

  useEffect(() => {
    if (
      loc.lat != null &&
      loc.lng != null &&
      mapRef.current &&
      !hasCentered.current
    ) {
      hasCentered.current = true;
      mapRef.current.animateToRegion(
        {
          latitude: loc.lat,
          longitude: loc.lng,
          latitudeDelta: 0.04,
          longitudeDelta: 0.04,
        },
        800,
      );
    }
  }, [loc.lat, loc.lng]);

  const fetchSignals = useCallback(async (r: Region) => {
    const { latMin, latMax, lngMin, lngMax } = regionToBounds(r);
    try {
      const snap = await firestore()
        .collection('signals')
        .where('lat', '>=', latMin)
        .where('lat', '<=', latMax)
        .orderBy('lat')
        .limit(500)
        .get();

      const rows: SignalDoc[] = [];
      snap.forEach((doc) => {
        const d = doc.data() as {
          lat: number;
          lng: number;
          bssid: string;
          frequency: number;
          sessionId: string;
        };
        if (d.lng >= lngMin && d.lng <= lngMax) {
          rows.push({
            id: doc.id,
            lat: d.lat,
            lng: d.lng,
            bssid: d.bssid || '',
            frequency: d.frequency ?? 0,
            sessionId: d.sessionId || '',
          });
        }
      });

      const sessions = new Set<string>();
      for (const s of rows) {
        if (s.sessionId) sessions.add(s.sessionId);
      }
      setSessionCount(sessions.size);
      setSignals(rows);
    } catch {
      setSignals([]);
      setSessionCount(0);
    }
  }, []);

  const onRegionChangeComplete = useCallback(
    (r: Region) => {
      void fetchSignals(r);
    },
    [fetchSignals],
  );

  const merged = useMemo(() => {
    const m = new Map<
      string,
      { lat: number; lng: number; count: number; frequency: number }
    >();
    for (const s of signals) {
      const key = s.bssid || s.id;
      const prev = m.get(key);
      if (!prev) {
        m.set(key, {
          lat: s.lat,
          lng: s.lng,
          count: 1,
          frequency: s.frequency,
        });
      } else {
        m.set(key, {
          lat: (prev.lat * prev.count + s.lat) / (prev.count + 1),
          lng: (prev.lng * prev.count + s.lng) / (prev.count + 1),
          count: prev.count + 1,
          frequency: s.frequency,
        });
      }
    }
    return Array.from(m.entries()).map(([k, v]) => ({
      key: k,
      ...v,
    }));
  }, [signals]);

  const formatCount = (n: number) =>
    n.toLocaleString('en-US', { maximumFractionDigits: 0 });

  return (
    <View style={styles.root}>
      <MapView
        ref={mapRef}
        style={StyleSheet.absoluteFill}
        provider={PROVIDER_GOOGLE}
        mapType="none"
        initialRegion={initialRegion}
        onMapReady={() => void fetchSignals(initialRegion)}
        onRegionChangeComplete={onRegionChangeComplete}
        rotateEnabled
        pitchEnabled={false}
        showsUserLocation={false}
        showsMyLocationButton={false}
        showsCompass={false}
        toolbarEnabled={false}
      >
        {merged.map((m) => {
          const is24 = m.frequency > 0 && m.frequency < 3000;
          const color = is24 ? COLORS.cyan : COLORS.magenta;
          const baseR = 4;
          const rPx = baseR + Math.min(12, (m.count - 1) * 2);
          const glowM = Math.min(45, 12 + m.count * 4);
          const fillGlow = is24
            ? `rgba(0,255,255,${Math.min(0.22, 0.06 + m.count * 0.02)})`
            : `rgba(255,0,255,${Math.min(0.22, 0.06 + m.count * 0.02)})`;

          return (
            <Fragment key={m.key}>
              <Circle
                center={{ latitude: m.lat, longitude: m.lng }}
                radius={glowM}
                fillColor={fillGlow}
                strokeWidth={0}
              />
              <Marker
                coordinate={{ latitude: m.lat, longitude: m.lng }}
                anchor={{ x: 0.5, y: 0.5 }}
              >
                <View
                  style={[
                    styles.dot,
                    {
                      width: rPx * 2,
                      height: rPx * 2,
                      borderRadius: rPx,
                      backgroundColor: color,
                      opacity: 0.4,
                    },
                  ]}
                  pointerEvents="none"
                />
              </Marker>
            </Fragment>
          );
        })}
        {loc.lat != null && loc.lng != null ? (
          <Marker
            coordinate={{ latitude: loc.lat, longitude: loc.lng }}
            anchor={{ x: 0.5, y: 0.5 }}
          >
            <Animated.View style={[styles.youDot, dotPulse]} />
          </Marker>
        ) : null}
      </MapView>

      <View
        style={[styles.hudTop, { paddingTop: insets.top + 8 }]}
        pointerEvents="none"
      >
        <Text style={styles.hudTitle}>SPECTRA EARTH</Text>
        <Text style={styles.hudRight}>
          {formatCount(signals.length)} SIGNALS
        </Text>
      </View>

      <View
        style={[styles.hudBottom, { paddingBottom: insets.bottom + 8 }]}
        pointerEvents="none"
      >
        <View>
          <Text style={styles.legend}>
            <Text style={{ color: COLORS.cyan }}>●</Text> CYAN = WiFi 2.4GHz
          </Text>
          <Text style={styles.legend}>
            <Text style={{ color: COLORS.magenta }}>●</Text> MAGENTA = WiFi 5GHz
          </Text>
        </View>
        <Text style={styles.sessions}>
          CONTRIBUTED BY {sessionCount} SESSIONS
        </Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: '#000000',
  },
  hudTop: {
    position: 'absolute',
    left: 12,
    right: 12,
    top: 0,
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  hudTitle: {
    color: COLORS.cyan,
    fontFamily: Platform.select({ ios: 'Menlo', android: 'monospace' }),
    fontSize: 14,
    letterSpacing: 1,
  },
  hudRight: {
    color: COLORS.hud,
    fontFamily: Platform.select({ ios: 'Menlo', android: 'monospace' }),
    fontSize: 12,
  },
  hudBottom: {
    position: 'absolute',
    left: 12,
    right: 12,
    bottom: 0,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-end',
  },
  legend: {
    color: 'rgba(0, 255, 200, 0.85)',
    fontFamily: Platform.select({ ios: 'Menlo', android: 'monospace' }),
    fontSize: 10,
    marginBottom: 4,
  },
  sessions: {
    color: 'rgba(0, 255, 200, 0.65)',
    fontFamily: Platform.select({ ios: 'Menlo', android: 'monospace' }),
    fontSize: 10,
    maxWidth: width * 0.45,
    textAlign: 'right',
  },
  dot: {},
  youDot: {
    width: 14,
    height: 14,
    borderRadius: 7,
    backgroundColor: '#FFFFFF',
    shadowColor: '#fff',
    shadowOpacity: 0.9,
    shadowRadius: 8,
  },
});
