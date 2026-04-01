import React, { useCallback, useEffect, useRef, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  FlatList,
  Dimensions,
  Alert,
} from 'react-native';
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withTiming,
  Easing,
} from 'react-native-reanimated';
import {
  AnomalyEntry,
  loadAnomalyLog,
  clearAnomalyLog,
  verifyNode,
} from '../../services/anomalyLog';
import {
  SACRED_SITES,
  haversineKm,
  getNearestSite,
} from '../../constants/sacredSites';
import { getGeologyNote, getNearestFault } from '../../utils/geology';
import { useFieldStore } from '../../store/useFieldStore';
import { Colors, Fonts, FontSizes, Spacing, BorderWidth } from '../../constants/theme';
import { FIELD_WEAK_MAX, FIELD_NORMAL_MAX } from '../../constants/thresholds';

const { height: SH, width: SW } = Dimensions.get('window');
const SHEET_HEIGHT = SH * 0.85;

type FilterType = 'ALL' | 'UNVERIFIED' | 'VERIFIED' | 'HIGH_MAG';

function getMagColor(magnitude: number): string {
  if (magnitude < 20)  return Colors.cyan;
  if (magnitude < FIELD_WEAK_MAX) return Colors.blueField;
  if (magnitude < FIELD_NORMAL_MAX) return '#00BB99';
  if (magnitude < 65)  return Colors.blueBright;
  return Colors.gold;
}

function formatUTC(iso: string) {
  try { return new Date(iso).toUTCString().replace('GMT','UTC'); }
  catch { return iso; }
}

// ── Entry card ─────────────────────────────────────────────────────────────
interface EntryCardProps {
  entry: AnomalyEntry;
  index: number;
  onShowOnMap: (entry: AnomalyEntry) => void;
  onOpenAR: (entry: AnomalyEntry) => void;
  onVerified: () => void;
}

function EntryCard({ entry, index, onShowOnMap, onOpenAR, onVerified }: EntryCardProps) {
  const [expanded, setExpanded] = useState(false);
  const magnitude = useFieldStore.getState().reading.magnitude;

  const nearest = (entry.coordinates.lat !== null && entry.coordinates.lng !== null)
    ? getNearestSite(entry.coordinates.lat!, entry.coordinates.lng!)
    : null;

  const geoNote = (entry.coordinates.lat !== null && entry.coordinates.lng !== null)
    ? getGeologyNote(entry.coordinates.lat!, entry.coordinates.lng!)
    : null;

  const nearFault = (entry.coordinates.lat !== null && entry.coordinates.lng !== null)
    ? getNearestFault(entry.coordinates.lat!, entry.coordinates.lng!)
    : null;

  const proxSites = (entry.proximityLinks ?? [])
    .map((id) => SACRED_SITES.find((s) => s.id === id))
    .filter(Boolean);

  const handleVerify = async () => {
    const result = await verifyNode(entry.id, magnitude);
    if (result.success) {
      Alert.alert('NODE VERIFIED', 'Dual detection confirmed. Node marked as verified.');
      onVerified();
    } else if (result.reason === 'too_far') {
      const d = Math.round(result.distanceM);
      Alert.alert(
        'CANNOT VERIFY',
        `You must be at this location to verify.\nNavigate to: ${result.targetCoords.lat.toFixed(5)}, ${result.targetCoords.lng.toFixed(5)}\n(${d}m away)`
      );
    } else {
      Alert.alert('GPS UNAVAILABLE', 'Cannot verify without GPS fix.');
    }
  };

  const magColor = getMagColor(entry.magnitude);
  const numStr = `#${String(index + 1).padStart(3, '0')}`;

  return (
    <View style={card.container}>
      {/* Collapsed row */}
      <TouchableOpacity onPress={() => setExpanded((v) => !v)} activeOpacity={0.8}>
        <View style={card.topRow}>
          <Text style={card.num}>{numStr}</Text>
          <Text style={card.ts}>{formatUTC(entry.timestamp)}</Text>
          <Text style={[card.mag, { color: magColor }]}>{entry.magnitude.toFixed(2)} µT</Text>
        </View>
        <View style={card.midRow}>
          <Text style={card.coord}>
            {entry.coordinates.lat !== null
              ? `${entry.coordinates.lat!.toFixed(4)}°, ${entry.coordinates.lng!.toFixed(4)}°`
              : 'NO GPS'}
          </Text>
          <Text style={card.hdg}>HDG {entry.heading.toFixed(1)}°</Text>
          <Text style={[card.delta, { color: entry.delta > 0 ? Colors.gold : Colors.cyan }]}>
            {entry.delta >= 0 ? '+' : ''}{entry.delta.toFixed(2)} Δ
          </Text>
        </View>
        <View style={card.actionRow}>
          <TouchableOpacity style={card.btnMap} onPress={() => onShowOnMap(entry)}>
            <Text style={card.btnMapText}>SHOW ON MAP ▶</Text>
          </TouchableOpacity>
          {entry.verified
            ? <Text style={card.verifiedLabel}>✓ VERIFIED</Text>
            : <TouchableOpacity style={card.btnVerify} onPress={handleVerify}>
                <Text style={card.btnVerifyText}>VERIFY NODE</Text>
              </TouchableOpacity>
          }
        </View>
      </TouchableOpacity>

      {/* Expanded detail */}
      {expanded && (
        <View style={card.expanded}>
          <Text style={card.sectionHead}>── SENSOR DATA ──</Text>
          <View style={card.grid}>
            {[
              ['MAGNITUDE', `${entry.magnitude.toFixed(2)} µT`],
              ['DELTA',     `${entry.delta >= 0 ? '+' : ''}${entry.delta.toFixed(2)}`],
              ['HEADING',   `${entry.heading.toFixed(1)}° ${headingLabel(entry.heading)}`],
              ['BASELINE',  `${(entry.magnitude - entry.delta).toFixed(2)} µT`],
            ].map(([l, v]) => (
              <View key={l} style={card.gridCell}>
                <Text style={card.cellLabel}>{l}</Text>
                <Text style={[card.cellVal, { color: l === 'DELTA' && entry.delta > 0 ? Colors.gold : Colors.cyan }]}>{v}</Text>
              </View>
            ))}
          </View>
          <Text style={card.axisLine}>X: {entry.x.toFixed(2)}  Y: {entry.y.toFixed(2)}  Z: {entry.z.toFixed(2)} µT</Text>

          <Text style={card.sectionHead}>── LOCATION ──</Text>
          {entry.coordinates.lat !== null
            ? <Text style={card.bodyText}>{entry.coordinates.lat!.toFixed(5)}°, {entry.coordinates.lng!.toFixed(5)}°</Text>
            : <Text style={card.bodyText}>No GPS fix at capture time</Text>
          }
          <Text style={[card.bodyText, { opacity: 0.6, marginTop: 2 }]}>Captured: {formatUTC(entry.timestamp)}</Text>

          <Text style={card.sectionHead}>── PROXIMITY ──</Text>
          {nearest && (
            <Text style={card.bodyText}>
              Nearest sacred site: {nearest.site.name} — {Math.round(nearest.distanceKm)} km
            </Text>
          )}
          {proxSites.length > 0
            ? proxSites.map((s) => s && (
              <Text key={s.id} style={[card.bodyText, { color: Colors.gold }]}>
                ◈ PROXIMITY LINK — {s.name} ({Math.round(haversineKm(entry.coordinates.lat!, entry.coordinates.lng!, s.lat, s.lng))} km)
              </Text>
            ))
            : <Text style={[card.bodyText, { opacity: 0.5 }]}>No sacred sites within 50km</Text>
          }

          <Text style={card.sectionHead}>── GEOLOGY ──</Text>
          <Text style={card.bodyText}>{geoNote ?? 'Geological data unavailable'}</Text>
          {nearFault && nearFault.distKm < 500 && (
            <Text style={[card.bodyText, { color: '#FF6633', marginTop: 4 }]}>
              ◈ {nearFault.distKm}km from {nearFault.name}
            </Text>
          )}

          {entry.verified && entry.verifiedAt && (
            <>
              <Text style={card.sectionHead}>── VERIFICATION ──</Text>
              <Text style={[card.bodyText, { color: Colors.cyan }]}>
                Verified: {formatUTC(entry.verifiedAt)}
              </Text>
              {entry.verificationMagnitude != null && (
                <Text style={card.bodyText}>Second reading: {entry.verificationMagnitude.toFixed(2)} µT</Text>
              )}
            </>
          )}

          <View style={card.expandedActions}>
            <TouchableOpacity style={card.actBtn} onPress={() => onShowOnMap(entry)}>
              <Text style={card.actBtnText}>SHOW ON MAP</Text>
            </TouchableOpacity>
            <TouchableOpacity style={[card.actBtn, { borderColor: Colors.cyanDim }]} onPress={() => onOpenAR(entry)}>
              <Text style={[card.actBtnText, { color: Colors.cyanDim }]}>OPEN IN AR</Text>
            </TouchableOpacity>
          </View>
        </View>
      )}
    </View>
  );
}

function headingLabel(deg: number) {
  return ['N','NE','E','SE','S','SW','W','NW'][Math.round(deg / 45) % 8];
}

const card = StyleSheet.create({
  container: {
    backgroundColor: Colors.backgroundCard,
    borderWidth: BorderWidth.thin,
    borderColor: Colors.grey,
    borderLeftWidth: 2,
    borderLeftColor: Colors.cyanDim,
    marginBottom: Spacing.sm,
    overflow: 'hidden',
  },
  topRow: { flexDirection: 'row', alignItems: 'center', padding: Spacing.sm, paddingBottom: 3, gap: Spacing.sm },
  num: { fontFamily: Fonts.mono, fontSize: 9, color: Colors.grey, width: 32 },
  ts: { fontFamily: Fonts.mono, fontSize: 9, color: Colors.greyLight, flex: 1 },
  mag: { fontFamily: Fonts.header, fontSize: FontSizes.md },
  midRow: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: Spacing.sm, paddingBottom: 4, gap: Spacing.md },
  coord: { fontFamily: Fonts.mono, fontSize: 9, color: Colors.greyLight, flex: 1 },
  hdg: { fontFamily: Fonts.mono, fontSize: 9, color: Colors.greyLight },
  delta: { fontFamily: Fonts.mono, fontSize: 9 },
  actionRow: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: Spacing.sm, paddingBottom: Spacing.sm, gap: Spacing.sm },
  btnMap: { borderWidth: BorderWidth.thin, borderColor: Colors.cyan, paddingHorizontal: 8, paddingVertical: 3 },
  btnMapText: { fontFamily: Fonts.mono, fontSize: 9, color: Colors.cyan, letterSpacing: 1 },
  btnVerify: { borderWidth: BorderWidth.thin, borderColor: Colors.gold, paddingHorizontal: 8, paddingVertical: 3, marginLeft: 'auto' },
  btnVerifyText: { fontFamily: Fonts.mono, fontSize: 9, color: Colors.gold, letterSpacing: 1 },
  verifiedLabel: { fontFamily: Fonts.mono, fontSize: 9, color: Colors.green, marginLeft: 'auto', letterSpacing: 1 },
  expanded: { borderTopWidth: BorderWidth.thin, borderTopColor: Colors.greyDark, padding: Spacing.sm },
  sectionHead: { fontFamily: Fonts.mono, fontSize: 9, color: Colors.grey, letterSpacing: 1.5, marginTop: Spacing.sm, marginBottom: 4 },
  grid: { flexDirection: 'row', flexWrap: 'wrap', gap: Spacing.sm, marginBottom: 4 },
  gridCell: { width: '46%' },
  cellLabel: { fontFamily: Fonts.mono, fontSize: 8, color: Colors.grey, letterSpacing: 1 },
  cellVal: { fontFamily: Fonts.mono, fontSize: FontSizes.sm },
  axisLine: { fontFamily: Fonts.mono, fontSize: 9, color: Colors.greyLight, opacity: 0.7, marginBottom: 4 },
  bodyText: { fontFamily: Fonts.mono, fontSize: 9, color: Colors.greyLight, lineHeight: 16 },
  expandedActions: { flexDirection: 'row', gap: Spacing.sm, marginTop: Spacing.sm },
  actBtn: { flex: 1, borderWidth: BorderWidth.thin, borderColor: Colors.cyan, paddingVertical: 6, alignItems: 'center' },
  actBtnText: { fontFamily: Fonts.mono, fontSize: 9, color: Colors.cyan, letterSpacing: 1.5 },
});

// ── Sheet ──────────────────────────────────────────────────────────────────
interface Props {
  visible: boolean;
  onClose: () => void;
  onShowOnMap: (entry: AnomalyEntry) => void;
  onOpenAR: (entry: AnomalyEntry) => void;
}

const FILTERS: FilterType[] = ['ALL', 'UNVERIFIED', 'VERIFIED', 'HIGH_MAG'];

export default function AnomalyLogSheet({ visible, onClose, onShowOnMap, onOpenAR }: Props) {
  const translateY = useSharedValue(SHEET_HEIGHT);
  const [entries, setEntries] = useState<AnomalyEntry[]>([]);
  const [filter, setFilter] = useState<FilterType>('ALL');

  useEffect(() => {
    translateY.value = withTiming(visible ? 0 : SHEET_HEIGHT, {
      duration: 320,
      easing: Easing.out(Easing.cubic),
    });
    if (visible) {
      loadAnomalyLog().then(setEntries);
    }
  }, [visible]);

  const reload = useCallback(() => {
    loadAnomalyLog().then(setEntries);
  }, []);

  const filtered = entries.filter((e) => {
    if (filter === 'UNVERIFIED') return !e.verified;
    if (filter === 'VERIFIED')   return !!e.verified;
    if (filter === 'HIGH_MAG')   return e.magnitude > 60;
    return true;
  });

  const sheetStyle = useAnimatedStyle(() => ({
    transform: [{ translateY: translateY.value }],
  }));

  const handleClear = () => {
    Alert.alert('CLEAR LOG', 'Delete all anomaly records? This cannot be undone.', [
      { text: 'CANCEL', style: 'cancel' },
      {
        text: 'CLEAR', style: 'destructive',
        onPress: async () => { await clearAnomalyLog(); setEntries([]); },
      },
    ]);
  };

  return (
    <Animated.View style={[styles.sheet, sheetStyle]}>
      {/* Header */}
      <View style={styles.header}>
        <Text style={styles.title}>ANOMALY LOG</Text>
        <Text style={styles.count}>{entries.length} DETECTIONS</Text>
        <View style={styles.headerRight}>
          <TouchableOpacity onPress={handleClear} style={styles.clearBtn}>
            <Text style={styles.clearText}>CLEAR</Text>
          </TouchableOpacity>
          <TouchableOpacity onPress={onClose} style={styles.closeBtn}>
            <Text style={styles.closeText}>✕</Text>
          </TouchableOpacity>
        </View>
      </View>

      {/* Filter tabs */}
      <View style={styles.filterRow}>
        {FILTERS.map((f) => (
          <TouchableOpacity
            key={f}
            style={[styles.filterTab, filter === f && styles.filterTabActive]}
            onPress={() => setFilter(f)}
          >
            <Text style={[styles.filterText, filter === f && styles.filterTextActive]}>
              {f.replace('_', ' ')}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      {/* List */}
      <FlatList
        data={filtered}
        keyExtractor={(item) => item.id}
        renderItem={({ item, index }) => (
          <EntryCard
            entry={item}
            index={index}
            onShowOnMap={(e) => { onClose(); onShowOnMap(e); }}
            onOpenAR={(e) => { onClose(); onOpenAR(e); }}
            onVerified={reload}
          />
        )}
        contentContainerStyle={styles.list}
        showsVerticalScrollIndicator={false}
        ListEmptyComponent={
          <View style={styles.empty}>
            <Text style={styles.emptyIcon}>◈</Text>
            <Text style={styles.emptyText}>NO DETECTIONS</Text>
            <Text style={styles.emptySubText}>
              Anomaly detections will appear here automatically once the sensor baseline is established.
            </Text>
          </View>
        }
      />
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  sheet: {
    position: 'absolute',
    bottom: 56,
    left: 0,
    right: 0,
    height: SHEET_HEIGHT,
    backgroundColor: '#00000F',
    borderTopWidth: BorderWidth.thin,
    borderTopColor: Colors.cyan,
    zIndex: 50,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: Spacing.md,
    paddingVertical: Spacing.sm,
    borderBottomWidth: BorderWidth.thin,
    borderBottomColor: Colors.grey,
    gap: Spacing.sm,
  },
  title: { fontFamily: Fonts.header, fontSize: FontSizes.sm, color: Colors.cyan, letterSpacing: 3, flex: 1 },
  count: { fontFamily: Fonts.mono, fontSize: FontSizes.xs, color: Colors.greyLight },
  headerRight: { flexDirection: 'row', gap: Spacing.sm, alignItems: 'center' },
  clearBtn: { padding: 4 },
  clearText: { fontFamily: Fonts.mono, fontSize: 10, color: Colors.red, letterSpacing: 1 },
  closeBtn: { padding: 4, paddingLeft: 8 },
  closeText: { fontFamily: Fonts.mono, fontSize: 16, color: Colors.greyLight },
  filterRow: {
    flexDirection: 'row',
    borderBottomWidth: BorderWidth.thin,
    borderBottomColor: Colors.greyDark,
  },
  filterTab: { flex: 1, paddingVertical: 8, alignItems: 'center' },
  filterTabActive: { borderBottomWidth: 1.5, borderBottomColor: Colors.cyan },
  filterText: { fontFamily: Fonts.mono, fontSize: 9, color: Colors.grey, letterSpacing: 1 },
  filterTextActive: { color: Colors.cyan },
  list: { padding: Spacing.sm, paddingBottom: Spacing.xxl },
  empty: { alignItems: 'center', paddingTop: Spacing.xxl, gap: Spacing.md },
  emptyIcon: { fontSize: 32, color: Colors.grey },
  emptyText: { fontFamily: Fonts.header, fontSize: FontSizes.sm, color: Colors.grey, letterSpacing: 3 },
  emptySubText: { fontFamily: Fonts.mono, fontSize: FontSizes.xs, color: Colors.greyLight, textAlign: 'center', lineHeight: 18, opacity: 0.6, paddingHorizontal: Spacing.xl },
});
