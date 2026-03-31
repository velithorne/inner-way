import React, { useCallback, useEffect, useRef, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Dimensions,
  Platform,
} from 'react-native';
import MapView, { Polyline, Marker, Region } from 'react-native-maps';
import * as Location from 'expo-location';
import { useFocusEffect, useNavigation } from '@react-navigation/native';
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withTiming,
} from 'react-native-reanimated';

import { DARK_MAP_STYLE } from '../constants/mapStyle';
import { FAULT_LINES } from '../constants/faultLines';
import {
  SACRED_SITES,
  SacredSite,
  haversineKm,
  getNearestSite,
  getProximityLinksForAnomaly,
} from '../constants/sacredSites';
import { AnomalyEntry, loadAnomalyLog } from '../services/anomalyLog';
import { useFieldStore } from '../store/useFieldStore';
import { Colors, Fonts, FontSizes, Spacing, BorderWidth } from '../constants/theme';

import LayerToggle, { LayerState } from '../components/map/LayerToggle';
import CompassRose from '../components/map/CompassRose';
import NodeMarker from '../components/map/NodeMarker';
import SacredSiteMarker from '../components/map/SacredSiteMarker';
import MapBottomSheet, { SheetContent } from '../components/map/MapBottomSheet';

const { width: SW } = Dimensions.get('window');

// ── Proximity link between anomaly and sacred site ───────────────────────────
interface ProximityLink {
  anomalyId: string;
  siteId: string;
  anomalyLat: number;
  anomalyLng: number;
  siteLat: number;
  siteLng: number;
  distanceKm: number;
}

function computeProximityLinks(entries: AnomalyEntry[]): ProximityLink[] {
  const links: ProximityLink[] = [];
  for (const entry of entries) {
    if (entry.coordinates.lat == null || entry.coordinates.lng == null) continue;
    const close = getProximityLinksForAnomaly(entry.coordinates.lat, entry.coordinates.lng);
    for (const { site, distanceKm } of close) {
      links.push({
        anomalyId: entry.id,
        siteId: site.id,
        anomalyLat: entry.coordinates.lat,
        anomalyLng: entry.coordinates.lng,
        siteLat: site.lat,
        siteLng: site.lng,
        distanceKm,
      });
    }
  }
  return links;
}

// ── Toast overlay ─────────────────────────────────────────────────────────────
function Toast({ message, visible }: { message: string; visible: boolean }) {
  const opacity = useSharedValue(0);
  useEffect(() => {
    opacity.value = withTiming(visible ? 1 : 0, { duration: 300 });
  }, [visible]);
  const style = useAnimatedStyle(() => ({ opacity: opacity.value }));
  if (!message) return null;
  return (
    <Animated.View style={[toast.container, style]} pointerEvents="none">
      <Text style={toast.text}>{message}</Text>
    </Animated.View>
  );
}

const toast = StyleSheet.create({
  container: {
    position: 'absolute',
    top: '40%',
    left: Spacing.xl,
    right: Spacing.xl,
    backgroundColor: 'rgba(0,0,10,0.92)',
    borderWidth: BorderWidth.thin,
    borderColor: Colors.cyan,
    padding: Spacing.md,
    zIndex: 100,
  },
  text: {
    fontFamily: Fonts.mono,
    fontSize: FontSizes.sm,
    color: Colors.cyan,
    textAlign: 'center',
    lineHeight: 20,
  },
});

// ── Main screen ───────────────────────────────────────────────────────────────
export default function MapScreen() {
  const navigation = useNavigation<any>();
  const mapRef = useRef<MapView>(null);

  const [layers, setLayers] = useState<LayerState>({
    myNodes: true,
    sacredSites: true,
    geology: false,
  });
  const [showLayerPanel, setShowLayerPanel] = useState(false);
  const [entries, setEntries] = useState<AnomalyEntry[]>([]);
  const [proximityLinks, setProximityLinks] = useState<ProximityLink[]>([]);
  const [sheetContent, setSheetContent] = useState<SheetContent | null>(null);
  const [userLocation, setUserLocation] = useState<{ lat: number; lng: number } | null>(null);
  const [mapHeading, setMapHeading] = useState(0);
  const [toastMsg, setToastMsg] = useState('');
  const [toastVisible, setToastVisible] = useState(false);
  const [visibleRegion, setVisibleRegion] = useState<Region | null>(null);

  const magneticHeading = useFieldStore((s) => s.reading.heading);
  const magnitude = useFieldStore((s) => s.reading.magnitude);

  const showToast = (msg: string) => {
    setToastMsg(msg);
    setToastVisible(true);
    setTimeout(() => setToastVisible(false), 3500);
  };

  useFocusEffect(
    useCallback(() => {
      // Load anomaly log
      loadAnomalyLog().then((data) => {
        setEntries(data);
        setProximityLinks(computeProximityLinks(data));
      });

      // Get user location
      (async () => {
        try {
          const { status } = await Location.requestForegroundPermissionsAsync();
          if (status === 'granted') {
            const loc = await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.Balanced });
            setUserLocation({ lat: loc.coords.latitude, lng: loc.coords.longitude });
          }
        } catch {}
      })();
    }, [])
  );

  // Filter entries to those within current map viewport (performance)
  const visibleEntries = visibleRegion
    ? entries.filter((e) => {
        if (e.coordinates.lat == null) return false;
        const { latitudeDelta, longitudeDelta, latitude, longitude } = visibleRegion;
        return (
          Math.abs(e.coordinates.lat - latitude) < latitudeDelta * 0.6 &&
          Math.abs(e.coordinates.lng! - longitude) < longitudeDelta * 0.6
        );
      })
    : entries;

  const nearestSite = userLocation
    ? getNearestSite(userLocation.lat, userLocation.lng)
    : null;

  // Anomalies near each sacred site
  const anomaliesNearSite = useCallback(
    (site: SacredSite) =>
      entries.filter((e) => {
        if (e.coordinates.lat == null) return false;
        return haversineKm(e.coordinates.lat, e.coordinates.lng!, site.lat, site.lng) <= site.radius;
      }),
    [entries]
  );

  const handleNodePress = (entry: AnomalyEntry) => {
    setSheetContent({ type: 'anomaly', entry });
  };

  const handleSitePress = (site: SacredSite) => {
    setSheetContent({
      type: 'site',
      site,
      userLat: userLocation?.lat,
      userLng: userLocation?.lng,
      nearbyAnomalies: anomaliesNearSite(site),
    });
  };

  const handleNavigateToAR = () => {
    setSheetContent(null);
    // Switch to AR tab
    navigation.navigate('AR' as never);
  };

  const unsyncedCount = entries.filter((e) => !e.synced).length;

  return (
    <View style={styles.root}>
      {/* ── Map ─────────────────────────────────────────────────────────── */}
      <MapView
        ref={mapRef}
        style={StyleSheet.absoluteFill}
        provider="google"
        mapType="hybrid"
        customMapStyle={DARK_MAP_STYLE}
        showsUserLocation
        showsCompass={false}
        rotateEnabled
        pitchEnabled
        onRegionChangeComplete={(region) => {
          setVisibleRegion(region);
          setMapHeading(0); // MapView heading not directly exposed — use 0
        }}
        initialRegion={{
          latitude: userLocation?.lat ?? 20,
          longitude: userLocation?.lng ?? 0,
          latitudeDelta: 60,
          longitudeDelta: 60,
        }}
      >
        {/* ── Geology layer: fault lines ──────────────────────────────── */}
        {layers.geology &&
          FAULT_LINES.map((fault) => (
            <Polyline
              key={fault.id}
              coordinates={fault.coordinates}
              strokeColor="rgba(255,68,0,0.4)"
              strokeWidth={2}
              lineDashPattern={[8, 4]}
            />
          ))}

        {/* ── Proximity links ─────────────────────────────────────────── */}
        {layers.myNodes &&
          layers.sacredSites &&
          proximityLinks.map((link) => (
            <Polyline
              key={`${link.anomalyId}-${link.siteId}`}
              coordinates={[
                { latitude: link.anomalyLat, longitude: link.anomalyLng },
                { latitude: link.siteLat, longitude: link.siteLng },
              ]}
              strokeColor={Colors.gold + '33'}
              strokeWidth={1}
              lineDashPattern={[6, 4]}
            />
          ))}

        {/* ── My nodes layer ──────────────────────────────────────────── */}
        {layers.myNodes &&
          visibleEntries.map((entry) => (
            <NodeMarker
              key={entry.id}
              entry={entry}
              onPress={handleNodePress}
              showProximity={(entry.proximityLinks?.length ?? 0) > 0}
            />
          ))}

        {/* ── Sacred sites layer ──────────────────────────────────────── */}
        {layers.sacredSites &&
          SACRED_SITES.map((site) => (
            <SacredSiteMarker
              key={site.id}
              site={site}
              onPress={handleSitePress}
              anomalyCount={anomaliesNearSite(site).length}
            />
          ))}
      </MapView>

      {/* ── Top HUD ─────────────────────────────────────────────────────── */}
      <View style={styles.topBar} pointerEvents="box-none">
        <View style={styles.topLeft}>
          <Text style={styles.logo}>◈ RESONANCE MAP</Text>
        </View>
        <View style={styles.topCenter}>
          <Text style={styles.nodeCount}>
            [ {entries.length} ] NODES LOGGED
          </Text>
          {unsyncedCount > 0 && (
            <Text style={styles.unsyncedNote}>{unsyncedCount} PENDING SYNC · PHASE 4</Text>
          )}
        </View>
        <TouchableOpacity
          style={styles.layerBtn}
          onPress={() => setShowLayerPanel((v) => !v)}
        >
          <Text style={styles.layerBtnText}>LAYERS</Text>
        </TouchableOpacity>
      </View>

      {/* ── Layer panel ─────────────────────────────────────────────────── */}
      {showLayerPanel && (
        <View style={styles.layerPanel}>
          <LayerToggle layers={layers} onChange={setLayers} />
        </View>
      )}

      {/* ── Compass ─────────────────────────────────────────────────────── */}
      <View style={styles.compass} pointerEvents="none">
        <CompassRose mapHeading={mapHeading} magneticHeading={magneticHeading} />
      </View>

      {/* ── Bottom info strip ───────────────────────────────────────────── */}
      {sheetContent == null && (
        <View style={styles.bottomStrip} pointerEvents="none">
          <View style={styles.stripRow}>
            {userLocation && (
              <Text style={styles.coordText}>
                {userLocation.lat.toFixed(4)}°, {userLocation.lng.toFixed(4)}°
              </Text>
            )}
            <Text style={styles.magSmall}>
              {magnitude.toFixed(1)} µT
            </Text>
          </View>
          {nearestSite && (
            <Text style={styles.nearestSiteText}>
              ◆ {nearestSite.site.name}  —  {Math.round(nearestSite.distanceKm)} km
            </Text>
          )}
        </View>
      )}

      {/* ── Bottom sheet ────────────────────────────────────────────────── */}
      <MapBottomSheet
        content={sheetContent}
        onClose={() => setSheetContent(null)}
        onNavigateToAR={handleNavigateToAR}
        onVerifyResult={(msg) => {
          setSheetContent(null);
          showToast(msg);
          // Reload entries to pick up verified status
          loadAnomalyLog().then((data) => {
            setEntries(data);
            setProximityLinks(computeProximityLinks(data));
          });
        }}
      />

      {/* ── Toast ───────────────────────────────────────────────────────── */}
      <Toast message={toastMsg} visible={toastVisible} />
    </View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: Colors.background,
  },

  // Top HUD
  topBar: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    height: 80,
    paddingTop: Platform.OS === 'android' ? 32 : 48,
    paddingHorizontal: Spacing.md,
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(0,0,10,0.8)',
    borderBottomWidth: BorderWidth.thin,
    borderBottomColor: Colors.grey,
  },
  topLeft: { flex: 1 },
  logo: {
    fontFamily: Fonts.header,
    fontSize: FontSizes.sm,
    color: Colors.cyan,
    letterSpacing: 3,
  },
  topCenter: { flex: 1.5, alignItems: 'center' },
  nodeCount: {
    fontFamily: Fonts.mono,
    fontSize: FontSizes.xs,
    color: Colors.cyan,
    letterSpacing: 1.5,
  },
  unsyncedNote: {
    fontFamily: Fonts.mono,
    fontSize: 8,
    color: Colors.gold,
    opacity: 0.7,
    letterSpacing: 1,
    marginTop: 2,
  },
  layerBtn: {
    borderWidth: BorderWidth.thin,
    borderColor: Colors.cyanDim,
    paddingHorizontal: Spacing.sm,
    paddingVertical: 4,
  },
  layerBtnText: {
    fontFamily: Fonts.mono,
    fontSize: 9,
    color: Colors.cyan,
    letterSpacing: 1.5,
  },

  // Layer panel
  layerPanel: {
    position: 'absolute',
    top: 88,
    right: Spacing.md,
    zIndex: 20,
  },

  // Compass
  compass: {
    position: 'absolute',
    bottom: 80 + Spacing.md,
    right: Spacing.md,
  },

  // Bottom info strip
  bottomStrip: {
    position: 'absolute',
    bottom: 60,
    left: 0,
    right: 0,
    paddingHorizontal: Spacing.md,
    paddingVertical: Spacing.sm,
    backgroundColor: 'rgba(0,0,10,0.75)',
    borderTopWidth: BorderWidth.thin,
    borderTopColor: Colors.grey,
  },
  stripRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  coordText: {
    fontFamily: Fonts.mono,
    fontSize: FontSizes.xs,
    color: Colors.greyLight,
    opacity: 0.8,
  },
  magSmall: {
    fontFamily: Fonts.mono,
    fontSize: FontSizes.xs,
    color: Colors.cyan,
    opacity: 0.8,
  },
  nearestSiteText: {
    fontFamily: Fonts.mono,
    fontSize: 9,
    color: Colors.gold,
    opacity: 0.7,
    marginTop: 2,
    letterSpacing: 0.5,
  },
});
