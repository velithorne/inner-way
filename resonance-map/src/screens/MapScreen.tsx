import React, { useCallback, useEffect, useRef, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Dimensions,
  Alert,
} from 'react-native';
import MapView, { Polyline, Region, LatLng as MapsLatLng } from 'react-native-maps';

import { useFocusEffect, useNavigation } from '@react-navigation/native';
import { activateKeepAwakeAsync, deactivateKeepAwake } from 'expo-keep-awake';

import { DARK_MAP_STYLE } from '../constants/mapStyle';
import { FAULT_LINES } from '../constants/faultLines';
import {
  SACRED_SITES,
  SacredSite,
  haversineKm,
  getNearestSite,
  getProximityLinksForAnomaly,
} from '../constants/sacredSites';
import * as anomalyLogModule from '../services/anomalyLog';
import { AnomalyEntry, loadAnomalyLog } from '../services/anomalyLog';
import {
  ConnectionLine,
  loadLines,
  addLine,
  clearLines,
  buildChronologicalLines,
  buildProximityLines,
} from '../services/connectionLines';
// Magnetometer lifecycle managed globally in App.tsx
import { useFieldStore } from '../store/useFieldStore';
import { bearing, midpoint } from '../utils/geo';
import { Colors, Fonts, FontSizes, Spacing, BorderWidth } from '../constants/theme';

import LayerToggle, { LayerState } from '../components/map/LayerToggle';
import CompassRose from '../components/map/CompassRose';
import NodeMarker from '../components/map/NodeMarker';
import SacredSiteMarker from '../components/map/SacredSiteMarker';
import MapBottomSheet, { SheetContent } from '../components/map/MapBottomSheet';
import SensorHUD from '../components/map/SensorHUD';
import AnomalyLogSheet from '../components/map/AnomalyLogSheet';

const { width: SW } = Dimensions.get('window');

// ── Proximity links ────────────────────────────────────────────────────────
interface ProximityLink {
  anomalyId: string; siteId: string;
  anomalyLat: number; anomalyLng: number;
  siteLat: number; siteLng: number;
}

function computeProximityLinks(entries: AnomalyEntry[]): ProximityLink[] {
  const links: ProximityLink[] = [];
  for (const e of entries) {
    if (e.coordinates.lat == null) continue;
    for (const { site } of getProximityLinksForAnomaly(e.coordinates.lat, e.coordinates.lng!)) {
      links.push({
        anomalyId: e.id, siteId: site.id,
        anomalyLat: e.coordinates.lat, anomalyLng: e.coordinates.lng!,
        siteLat: site.lat, siteLng: site.lng,
      });
    }
  }
  return links;
}

// ── Connection line colour ─────────────────────────────────────────────────
function lineColor(l: ConnectionLine): string {
  if (l.fromVerified && l.toVerified) return Colors.gold;
  return Colors.cyan;
}
function lineOpacity(l: ConnectionLine): number {
  if (l.fromVerified && l.toVerified) return 0.6;
  return 0.3;
}

// ── Auto-connect sub-toggle ────────────────────────────────────────────────
type AutoMode = 'chrono' | 'proximity';

// ── Main screen ────────────────────────────────────────────────────────────
export default function MapScreen() {
  const navigation = useNavigation<any>();
  const mapRef = useRef<MapView>(null);

  const [layers, setLayers] = useState<LayerState>({ myNodes: true, sacredSites: true, geology: false });
  const [showLayers, setShowLayers] = useState(false);
  const [entries, setEntries] = useState<AnomalyEntry[]>([]);
  const [proximityLinks, setProximityLinks] = useState<ProximityLink[]>([]);
  const [sheetContent, setSheetContent] = useState<SheetContent | null>(null);
  const [userLocation, setUserLocation] = useState<{ lat: number; lng: number } | null>(null);
  const [visibleRegion, setVisibleRegion] = useState<Region | null>(null);
  const [showLogSheet, setShowLogSheet] = useState(false);

  // Connection line mode
  const [connectMode, setConnectMode] = useState(false);
  const [connectionLines, setConnectionLines] = useState<ConnectionLine[]>([]);
  const [selectedForConnect, setSelectedForConnect] = useState<AnomalyEntry | null>(null);
  const [autoMode, setAutoMode] = useState<AutoMode>('chrono');
  const [showAutoMenu, setShowAutoMenu] = useState(false);

  // Spotlight: animated pin after "show on map"
  const [spotlightId, setSpotlightId] = useState<string | null>(null);
  const [showBackToLog, setShowBackToLog] = useState(false);

  // Read at 4Hz via interval — not 60Hz store subscription — to keep map render cheap
  const [hudMag, setHudMag] = useState(0);
  const [hudHeading, setHudHeading] = useState(0);
  useEffect(() => {
    const id = setInterval(() => {
      const s = useFieldStore.getState();
      setHudMag(s.reading.magnitude);
      setHudHeading(s.reading.heading);
    }, 250);
    return () => clearInterval(id);
  }, []);

  const anomaliesNearSite = useCallback((site: SacredSite) =>
    entries.filter((e) => {
      if (e.coordinates.lat == null) return false;
      return haversineKm(e.coordinates.lat, e.coordinates.lng!, site.lat, site.lng) <= site.radius;
    }), [entries]);

  const reload = useCallback(() => {
    loadAnomalyLog().then((data) => {
      setEntries(data);
      setProximityLinks(computeProximityLinks(data));
    });
    loadLines().then(setConnectionLines);
  }, []);

  useFocusEffect(
    useCallback(() => {
      activateKeepAwakeAsync();
      reload();

      // Use cached GPS from background watcher — never block on a live fix
      const lat = anomalyLogModule.lastKnownLat;
      const lng = anomalyLogModule.lastKnownLng;
      if (lat !== null && lng !== null) {
        setUserLocation({ lat, lng });
      }

      return () => {
        deactivateKeepAwake();
      };
    }, [])
  );

  // Keep user location fresh from cached watcher without blocking
  useEffect(() => {
    const id = setInterval(() => {
      const lat = anomalyLogModule.lastKnownLat;
      const lng = anomalyLogModule.lastKnownLng;
      if (lat !== null && lng !== null) {
        setUserLocation((prev) => {
          if (prev?.lat === lat && prev?.lng === lng) return prev;
          return { lat, lng };
        });
      }
    }, 5000);
    return () => clearInterval(id);
  }, []);

  const visibleEntries = visibleRegion
    ? entries.filter((e) => {
        if (e.coordinates.lat == null) return false;
        const { latitudeDelta, longitudeDelta, latitude, longitude } = visibleRegion;
        return (
          Math.abs(e.coordinates.lat - latitude) < latitudeDelta * 0.7 &&
          Math.abs(e.coordinates.lng! - longitude) < longitudeDelta * 0.7
        );
      })
    : entries;

  const nearestSite = userLocation ? getNearestSite(userLocation.lat, userLocation.lng) : null;
  const magnitude = hudMag;
  const magneticHeading = hudHeading;

  // ── Show-on-map handler (from log sheet) ─────────────────────────────────
  const handleShowOnMap = useCallback((entry: AnomalyEntry) => {
    if (entry.coordinates.lat == null) return;
    // Close log sheet first, wait for its 320ms animation, then fly
    setShowLogSheet(false);
    setShowBackToLog(true);
    setSpotlightId(entry.id);
    setTimeout(() => {
      mapRef.current?.animateToRegion({
        latitude: entry.coordinates.lat!,
        longitude: entry.coordinates.lng!,
        latitudeDelta: 0.01,
        longitudeDelta: 0.01,
      }, 600);
    }, 350); // after sheet close animation
    setTimeout(() => {
      setSheetContent({ type: 'anomaly', entry });
    }, 1000);
    setTimeout(() => setSpotlightId(null), 4000);
  }, []);

  // ── AR target overlay (passed to AR screen via navigation param) ──────────
  const handleOpenAR = (entry: AnomalyEntry) => {
    navigation.navigate('AR', {
      targetLat: entry.coordinates.lat,
      targetLng: entry.coordinates.lng,
    });
  };

  // ── Connect mode pin tap ──────────────────────────────────────────────────
  const handleNodePressConnect = async (entry: AnomalyEntry) => {
    if (!connectMode) {
      setSheetContent({ type: 'anomaly', entry });
      return;
    }
    if (!selectedForConnect) {
      setSelectedForConnect(entry);
      return;
    }
    if (selectedForConnect.id === entry.id) {
      setSelectedForConnect(null);
      return;
    }
    // Draw line
    const line = await addLine({
      fromId: selectedForConnect.id, toId: entry.id,
      fromLat: selectedForConnect.coordinates.lat!, fromLng: selectedForConnect.coordinates.lng!,
      toLat: entry.coordinates.lat!, toLng: entry.coordinates.lng!,
      fromVerified: !!selectedForConnect.verified, toVerified: !!entry.verified,
      kind: 'node-node',
    });
    setConnectionLines((prev) => [...prev, line]);
    setSelectedForConnect(null);
  };

  const handleAutoConnect = async () => {
    const valid = entries
      .filter((e) => e.coordinates.lat != null)
      .map((e) => ({ id: e.id, lat: e.coordinates.lat!, lng: e.coordinates.lng!, verified: !!e.verified }));
    const lines = autoMode === 'chrono'
      ? await buildChronologicalLines(valid)
      : await buildProximityLines(valid);
    setConnectionLines(lines);
    setShowAutoMenu(false);
  };

  const handleClearConnect = async () => {
    await clearLines();
    setConnectionLines([]);
    setConnectMode(false);
    setSelectedForConnect(null);
  };

  const unsyncedCount = entries.filter((e) => !e.synced).length;

  return (
    <View style={styles.root}>
      {/* ── Map ───────────────────────────────────────────────────────── */}
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
        onRegionChangeComplete={setVisibleRegion}
        initialRegion={{
          latitude: userLocation?.lat ?? 20,
          longitude: userLocation?.lng ?? 0,
          latitudeDelta: 60,
          longitudeDelta: 60,
        }}
      >
        {/* Geology fault lines */}
        {layers.geology && FAULT_LINES.map((f) => (
          <Polyline key={f.id} coordinates={f.coordinates}
            strokeColor="rgba(255,68,0,0.4)" strokeWidth={2} lineDashPattern={[8,4]} />
        ))}

        {/* Proximity links */}
        {layers.myNodes && layers.sacredSites && proximityLinks.map((pl) => (
          <Polyline
            key={`${pl.anomalyId}-${pl.siteId}`}
            coordinates={[{ latitude: pl.anomalyLat, longitude: pl.anomalyLng },
                          { latitude: pl.siteLat, longitude: pl.siteLng }]}
            strokeColor={Colors.gold + '33'} strokeWidth={1} lineDashPattern={[6,4]} />
        ))}

        {/* User connection lines */}
        {connectionLines.map((l) => {
          const mid = midpoint(
            { latitude: l.fromLat, longitude: l.fromLng },
            { latitude: l.toLat,   longitude: l.toLng }
          );
          const hasAlignment = l.alignment.passesNearSites.length > 0 ||
                               l.alignment.passesNearFaults.length > 0 ||
                               l.alignment.alignedBearing;
          return (
            <React.Fragment key={l.id}>
              <Polyline
                coordinates={[{ latitude: l.fromLat, longitude: l.fromLng },
                              { latitude: l.toLat,   longitude: l.toLng }]}
                strokeColor={lineColor(l) + Math.round(lineOpacity(l) * 255).toString(16).padStart(2, '0')}
                strokeWidth={hasAlignment ? 1.5 : 1}
                lineDashPattern={l.fromVerified && l.toVerified ? undefined : [6,3]}
              />
            </React.Fragment>
          );
        })}

        {/* Node markers */}
        {layers.myNodes && visibleEntries.map((e) => (
          <NodeMarker
            key={e.id}
            entry={e}
            onPress={handleNodePressConnect}
            showProximity={(e.proximityLinks?.length ?? 0) > 0}
          />
        ))}

        {/* Sacred site markers */}
        {layers.sacredSites && SACRED_SITES.map((site) => (
          <SacredSiteMarker
            key={site.id}
            site={site}
            onPress={(s) => setSheetContent({
              type: 'site', site: s,
              userLat: userLocation?.lat, userLng: userLocation?.lng,
              nearbyAnomalies: anomaliesNearSite(s),
            })}
            anomalyCount={anomaliesNearSite(site).length}
          />
        ))}
      </MapView>

      {/* ── Sensor HUD (top) ─────────────────────────────────────────── */}
      <SensorHUD anomalyCount={entries.length} key="sensor-hud" />

      {/* ── Top-right controls ───────────────────────────────────────── */}
      <View style={styles.topRight} pointerEvents="box-none">
        <TouchableOpacity style={styles.ctrlBtn} onPress={() => setShowLayers((v) => !v)}>
          <Text style={styles.ctrlText}>LAYERS</Text>
        </TouchableOpacity>

        {!connectMode
          ? <TouchableOpacity style={styles.ctrlBtn} onPress={() => setConnectMode(true)}>
              <Text style={styles.ctrlText}>◈ CONNECT</Text>
            </TouchableOpacity>
          : <View style={styles.connectPanel}>
              <TouchableOpacity style={[styles.ctrlBtn, styles.ctrlBtnActive]} onPress={handleClearConnect}>
                <Text style={[styles.ctrlText, { color: Colors.gold }]}>✕ CLEAR LINES</Text>
              </TouchableOpacity>
              <TouchableOpacity style={styles.ctrlBtn} onPress={() => setShowAutoMenu((v) => !v)}>
                <Text style={styles.ctrlText}>AUTO-CONNECT</Text>
              </TouchableOpacity>
            </View>
        }

        {/* Layer panel */}
        {showLayers && (
          <View style={styles.layerPanel}>
            <LayerToggle layers={layers} onChange={setLayers} />
          </View>
        )}

        {/* Auto-connect sub-menu */}
        {connectMode && showAutoMenu && (
          <View style={styles.autoMenu}>
            <TouchableOpacity
              style={[styles.autoMenuItem, autoMode === 'chrono' && styles.autoMenuActive]}
              onPress={() => setAutoMode('chrono')}
            >
              <Text style={styles.autoMenuText}>CHRONOLOGICAL</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[styles.autoMenuItem, autoMode === 'proximity' && styles.autoMenuActive]}
              onPress={() => setAutoMode('proximity')}
            >
              <Text style={styles.autoMenuText}>PROXIMITY</Text>
            </TouchableOpacity>
            <TouchableOpacity style={[styles.autoMenuItem, styles.autoMenuGo]} onPress={handleAutoConnect}>
              <Text style={[styles.autoMenuText, { color: Colors.cyan }]}>DRAW ALL</Text>
            </TouchableOpacity>
          </View>
        )}
      </View>

      {/* Connect mode: selected pin indicator */}
      {connectMode && selectedForConnect && (
        <View style={styles.connectHint} pointerEvents="none">
          <Text style={styles.connectHintText}>
            PIN SELECTED — TAP SECOND NODE TO CONNECT
          </Text>
        </View>
      )}
      {connectMode && !selectedForConnect && (
        <View style={styles.connectHint} pointerEvents="none">
          <Text style={styles.connectHintText}>CONNECT MODE — TAP TWO NODES TO DRAW A LINE</Text>
        </View>
      )}

      {/* ── Compass bottom-right ─────────────────────────────────────── */}
      <View style={styles.compass} pointerEvents="none">
        <CompassRose mapHeading={0} magneticHeading={magneticHeading} />
      </View>

      {/* ── Bottom info bar ──────────────────────────────────────────── */}
      <View style={styles.bottomBar} pointerEvents="box-none">
        <Text style={styles.coordText}>
          {userLocation ? `${userLocation.lat.toFixed(4)}°, ${userLocation.lng.toFixed(4)}°` : '-- GPS --'}
        </Text>
        {nearestSite && (
          <TouchableOpacity
            onPress={() => mapRef.current?.animateToRegion({
              latitude: nearestSite.site.lat, longitude: nearestSite.site.lng,
              latitudeDelta: 0.5, longitudeDelta: 0.5,
            }, 800)}
          >
            <Text style={styles.siteText}>◆ {nearestSite.site.name} — {Math.round(nearestSite.distanceKm)} km</Text>
          </TouchableOpacity>
        )}
        <Text style={styles.magMini}>{magnitude.toFixed(1)} µT</Text>
      </View>

      {/* ── Bottom-left: log button ───────────────────────────────────── */}
      <TouchableOpacity
        style={styles.logFab}
        onPress={() => { setShowLogSheet(true); setSheetContent(null); }}
      >
        <Text style={styles.logFabText}>◆ LOG</Text>
        {entries.length > 0 && (
          <View style={styles.logBadge}>
            <Text style={styles.logBadgeText}>{entries.length}</Text>
          </View>
        )}
        {unsyncedCount > 0 && (
          <View style={[styles.logBadge, styles.unsyncedBadge]}>
            <Text style={styles.logBadgeText}>{unsyncedCount}</Text>
          </View>
        )}
      </TouchableOpacity>

      {/* ── Back to log button (after show-on-map) ───────────────────── */}
      {showBackToLog && (
        <TouchableOpacity
          style={styles.backToLog}
          onPress={() => { setShowBackToLog(false); setSheetContent(null); setShowLogSheet(true); }}
        >
          <Text style={styles.backToLogText}>← BACK TO LOG</Text>
        </TouchableOpacity>
      )}

      {/* ── Map bottom sheet (site / node detail) ────────────────────── */}
      <MapBottomSheet
        content={sheetContent}
        onClose={() => setSheetContent(null)}
        onNavigateToAR={() => { setSheetContent(null); navigation.navigate('AR'); }}
        onVerifyResult={(msg) => {
          setSheetContent(null);
          Alert.alert(
            msg.startsWith('NODE VERIFIED') ? 'VERIFIED' : 'VERIFY NODE',
            msg
          );
          reload();
        }}
      />

      {/* ── Anomaly log sheet ────────────────────────────────────────── */}
      <AnomalyLogSheet
        visible={showLogSheet}
        onClose={() => setShowLogSheet(false)}
        onShowOnMap={handleShowOnMap}
        onOpenAR={handleOpenAR}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: Colors.background },

  topRight: {
    position: 'absolute',
    top: 150,   // below HUD
    right: Spacing.md,
    alignItems: 'flex-end',
    gap: Spacing.sm,
    zIndex: 20,
  },
  ctrlBtn: {
    backgroundColor: 'rgba(0,0,10,0.82)',
    borderWidth: BorderWidth.thin,
    borderColor: Colors.grey,
    paddingHorizontal: Spacing.sm,
    paddingVertical: 5,
  },
  ctrlBtnActive: { borderColor: Colors.gold },
  ctrlText: { fontFamily: Fonts.mono, fontSize: 9, color: Colors.cyan, letterSpacing: 1.5 },
  connectPanel: { gap: Spacing.xs, alignItems: 'flex-end' },
  layerPanel: { marginTop: Spacing.xs },
  autoMenu: {
    backgroundColor: 'rgba(0,0,10,0.9)',
    borderWidth: BorderWidth.thin,
    borderColor: Colors.grey,
    minWidth: 130,
    overflow: 'hidden',
  },
  autoMenuItem: { paddingHorizontal: Spacing.sm, paddingVertical: 8 },
  autoMenuActive: { backgroundColor: Colors.greyDark },
  autoMenuGo: { borderTopWidth: BorderWidth.thin, borderTopColor: Colors.grey },
  autoMenuText: { fontFamily: Fonts.mono, fontSize: 9, color: Colors.greyLight, letterSpacing: 1 },

  connectHint: {
    position: 'absolute',
    top: 200,
    left: Spacing.md,
    right: 160,
    backgroundColor: 'rgba(0,0,10,0.8)',
    borderWidth: BorderWidth.thin,
    borderColor: Colors.cyan,
    paddingHorizontal: Spacing.sm,
    paddingVertical: 5,
  },
  connectHintText: { fontFamily: Fonts.mono, fontSize: 9, color: Colors.cyan, letterSpacing: 1 },

  compass: {
    position: 'absolute',
    bottom: 104,
    right: Spacing.md,
  },

  bottomBar: {
    position: 'absolute',
    bottom: 56,
    left: 0,
    right: 0,
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: Spacing.md,
    paddingVertical: 6,
    backgroundColor: 'rgba(0,0,10,0.78)',
    borderTopWidth: BorderWidth.thin,
    borderTopColor: Colors.greyDark,
    gap: Spacing.sm,
  },
  coordText: { fontFamily: Fonts.mono, fontSize: 9, color: Colors.greyLight, opacity: 0.7 },
  siteText: { fontFamily: Fonts.mono, fontSize: 9, color: Colors.gold, opacity: 0.75, flex: 1, textAlign: 'center' },
  magMini: { fontFamily: Fonts.mono, fontSize: 9, color: Colors.cyan, opacity: 0.7 },

  logFab: {
    position: 'absolute',
    bottom: 76,
    left: Spacing.md,
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(0,0,10,0.88)',
    borderWidth: BorderWidth.thin,
    borderColor: Colors.gold,
    paddingHorizontal: Spacing.sm,
    paddingVertical: 7,
    gap: 6,
    zIndex: 15,
  },
  logFabText: { fontFamily: Fonts.mono, fontSize: 11, color: Colors.gold, letterSpacing: 1.5 },
  logBadge: {
    backgroundColor: Colors.gold,
    borderRadius: 8,
    minWidth: 16,
    height: 16,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 3,
  },
  unsyncedBadge: { backgroundColor: Colors.cyan, marginLeft: -3 },
  logBadgeText: { fontSize: 9, fontWeight: 'bold', color: '#000' },

  backToLog: {
    position: 'absolute',
    top: 155,
    left: Spacing.md,
    backgroundColor: 'rgba(0,0,10,0.85)',
    borderWidth: BorderWidth.thin,
    borderColor: Colors.cyanDim,
    paddingHorizontal: Spacing.sm,
    paddingVertical: 5,
    zIndex: 20,
  },
  backToLogText: { fontFamily: Fonts.mono, fontSize: 9, color: Colors.cyan, letterSpacing: 1 },
});
