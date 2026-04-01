import React, { useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  Dimensions,
} from 'react-native';
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withTiming,
  Easing,
} from 'react-native-reanimated';
import { AnomalyEntry, verifyNode } from '../../services/anomalyLog';
import { SacredSite, SACRED_SITES, haversineKm } from '../../constants/sacredSites';
import { Colors, Fonts, FontSizes, Spacing, BorderWidth } from '../../constants/theme';
import { useFieldStore } from '../../store/useFieldStore';

const { height: SCREEN_HEIGHT } = Dimensions.get('window');
const SHEET_HEIGHT = SCREEN_HEIGHT * 0.48;

export type SheetContent =
  | { type: 'anomaly'; entry: AnomalyEntry }
  | { type: 'site'; site: SacredSite; userLat?: number; userLng?: number; nearbyAnomalies: AnomalyEntry[] };

interface Props {
  content: SheetContent | null;
  onClose: () => void;
  onNavigateToAR: () => void;
  onVerifyResult: (msg: string) => void;
}

function Section({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <View style={sec.wrap}>
      <Text style={sec.label}>{label}</Text>
      {children}
    </View>
  );
}

const sec = StyleSheet.create({
  wrap: { marginBottom: Spacing.sm },
  label: { fontFamily: Fonts.mono, fontSize: 9, color: Colors.greyLight, letterSpacing: 2, marginBottom: 3 },
});

function AnomalySheet({ entry, onNavigateToAR, onVerify }: {
  entry: AnomalyEntry;
  onNavigateToAR: () => void;
  onVerify: (msg: string) => void;
}) {
  const magnitude = useFieldStore.getState().reading.magnitude;

  const proxSites = (entry.proximityLinks ?? [])
    .map((id) => SACRED_SITES.find((s) => s.id === id))
    .filter(Boolean) as SacredSite[];

  const handleVerify = async () => {
    const result = await verifyNode(entry.id, magnitude);
    if (result.success) {
      onVerify('NODE VERIFIED — DUAL DETECTION CONFIRMED');
    } else if (result.reason === 'too_far') {
      const d = Math.round(result.distanceM);
      onVerify(`YOU MUST BE AT THIS LOCATION TO VERIFY\nNavigate to: ${result.targetCoords.lat.toFixed(5)}, ${result.targetCoords.lng.toFixed(5)}\n(${d}m away)`);
    } else {
      onVerify('GPS UNAVAILABLE — CANNOT VERIFY');
    }
  };

  return (
    <ScrollView showsVerticalScrollIndicator={false}>
      {/* Header */}
      <View style={sheet.header}>
        <View>
          <Text style={sheet.title}>{entry.verified ? '✓ VERIFIED NODE' : 'ANOMALY DETECTION'}</Text>
          <Text style={sheet.subtitle}>{new Date(entry.timestamp).toUTCString().replace('GMT', 'UTC')}</Text>
        </View>
        {entry.verified && <View style={sheet.verifiedBadge}><Text style={sheet.verifiedText}>VERIFIED</Text></View>}
      </View>

      {/* Coordinates */}
      {entry.coordinates.lat !== null && (
        <Section label="COORDINATES">
          <Text style={sheet.value}>
            {entry.coordinates.lat.toFixed(5)}°,  {entry.coordinates.lng!.toFixed(5)}°
          </Text>
        </Section>
      )}

      {/* Sensor data */}
      <Section label="SENSOR READINGS">
        <View style={sheet.grid}>
          <View style={sheet.cell}><Text style={sheet.cellLabel}>MAG</Text><Text style={[sheet.cellValue, { color: Colors.gold }]}>{entry.magnitude.toFixed(2)} µT</Text></View>
          <View style={sheet.cell}><Text style={sheet.cellLabel}>DELTA</Text><Text style={sheet.cellValue}>{entry.delta >= 0 ? '+' : ''}{entry.delta.toFixed(2)}</Text></View>
          <View style={sheet.cell}><Text style={sheet.cellLabel}>HDG</Text><Text style={sheet.cellValue}>{entry.heading.toFixed(1)}°</Text></View>
        </View>
        <Text style={sheet.axisText}>X: {entry.x.toFixed(2)}  Y: {entry.y.toFixed(2)}  Z: {entry.z.toFixed(2)} µT</Text>
      </Section>

      {/* Proximity links */}
      {proxSites.length > 0 && (
        <Section label="PROXIMITY DETECTED">
          {proxSites.map((s) => {
            const d = entry.coordinates.lat
              ? haversineKm(entry.coordinates.lat!, entry.coordinates.lng!, s.lat, s.lng)
              : 0;
            return (
              <Text key={s.id} style={sheet.proxLine}>
                ◈ {Math.round(d)}km from {s.name}
              </Text>
            );
          })}
        </Section>
      )}

      {/* Verification */}
      {entry.verifiedAt && (
        <Section label="VERIFICATION">
          <Text style={sheet.value}>Verified: {new Date(entry.verifiedAt).toUTCString().replace('GMT', 'UTC')}</Text>
          {entry.verificationMagnitude != null && (
            <Text style={sheet.value}>Second reading: {entry.verificationMagnitude.toFixed(2)} µT</Text>
          )}
        </Section>
      )}

      {/* Actions */}
      <View style={sheet.actions}>
        {!entry.verified && (
          <TouchableOpacity style={sheet.btnGold} onPress={handleVerify}>
            <Text style={sheet.btnGoldText}>VERIFY NODE</Text>
          </TouchableOpacity>
        )}
        <TouchableOpacity style={sheet.btnCyan} onPress={onNavigateToAR}>
          <Text style={sheet.btnCyanText}>VIEW IN AR</Text>
        </TouchableOpacity>
      </View>
    </ScrollView>
  );
}

function SiteSheet({ site, userLat, userLng, nearbyAnomalies }: {
  site: SacredSite;
  userLat?: number;
  userLng?: number;
  nearbyAnomalies: AnomalyEntry[];
}) {
  const distKm = (userLat != null && userLng != null)
    ? haversineKm(userLat, userLng, site.lat, site.lng)
    : null;

  return (
    <ScrollView showsVerticalScrollIndicator={false}>
      <View style={sheet.header}>
        <View style={{ flex: 1 }}>
          <Text style={sheet.title}>◆ {site.name.toUpperCase()}</Text>
          <Text style={sheet.subtitle}>{site.culture}  ·  {site.age}</Text>
        </View>
      </View>

      {distKm !== null && (
        <Section label="YOUR DISTANCE">
          <Text style={[sheet.value, { color: Colors.cyan }]}>{Math.round(distKm)} km</Text>
        </Section>
      )}

      <Section label="GEOLOGY">
        <Text style={sheet.bodyText}>{site.geology}</Text>
      </Section>

      <Section label="EM / SCHUMANN NOTES">
        <Text style={sheet.bodyText}>{site.schumannNote}</Text>
      </Section>

      {/* Phase 3 live resonance placeholder */}
      <Section label="RESONANCE STRENGTH">
        <View style={sheet.strengthBar}>
          <View style={[sheet.strengthFill, { width: `${Math.round(site.resonancePlaceholder * 100)}%` as any }]} />
        </View>
        <Text style={sheet.bodyText}>
          {Math.round(site.resonancePlaceholder * 100)}%  ·  Phase 4 will show live crowd-sourced readings
        </Text>
      </Section>

      {nearbyAnomalies.length > 0 && (
        <Section label={`ANOMALIES NEAR THIS SITE (${nearbyAnomalies.length})`}>
          {nearbyAnomalies.map((a, i) => (
            <View key={a.id} style={sheet.anomalyRow}>
              <Text style={sheet.anomalyNum}>#{i + 1}</Text>
              <Text style={sheet.anomalyMag}>{a.magnitude.toFixed(1)} µT</Text>
              <Text style={sheet.anomalyTs}>{new Date(a.timestamp).toLocaleDateString()}</Text>
              {a.verified && <Text style={sheet.verifiedSmall}>✓</Text>}
            </View>
          ))}
        </Section>
      )}
    </ScrollView>
  );
}

export default function MapBottomSheet({ content, onClose, onNavigateToAR, onVerifyResult }: Props) {
  const translateY = useSharedValue(SHEET_HEIGHT);

  useEffect(() => {
    translateY.value = withTiming(content ? 0 : SHEET_HEIGHT, {
      duration: 280,
      easing: Easing.out(Easing.cubic),
    });
  }, [content]);

  const animStyle = useAnimatedStyle(() => ({
    transform: [{ translateY: translateY.value }],
  }));

  return (
    <Animated.View
      style={[styles.sheet, animStyle]}
      pointerEvents={content ? 'auto' : 'none'}
    >
      {/* Handle */}
      <View style={styles.handleRow}>
        <View style={styles.handle} />
        <TouchableOpacity onPress={onClose} style={styles.closeBtn}>
          <Text style={styles.closeText}>✕</Text>
        </TouchableOpacity>
      </View>

      <View style={styles.content}>
        {content?.type === 'anomaly' && (
          <AnomalySheet
            entry={content.entry}
            onNavigateToAR={onNavigateToAR}
            onVerify={onVerifyResult}
          />
        )}
        {content?.type === 'site' && (
          <SiteSheet
            site={content.site}
            userLat={content.userLat}
            userLng={content.userLng}
            nearbyAnomalies={content.nearbyAnomalies}
          />
        )}
      </View>
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  sheet: {
    position: 'absolute',
    bottom: 56, // above tab bar
    left: 0,
    right: 0,
    height: SHEET_HEIGHT,
    backgroundColor: 'rgba(6,6,18,0.97)',
    borderTopWidth: BorderWidth.thin,
    borderTopColor: Colors.cyan,
    borderLeftWidth: BorderWidth.thin,
    borderLeftColor: Colors.grey,
    borderRightWidth: BorderWidth.thin,
    borderRightColor: Colors.grey,
  },
  handleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: Spacing.md,
    paddingTop: 10,
    paddingBottom: 6,
  },
  handle: {
    flex: 1,
    height: 3,
    backgroundColor: Colors.grey,
    borderRadius: 2,
    marginRight: Spacing.md,
    opacity: 0.5,
  },
  closeBtn: { padding: 4 },
  closeText: { fontFamily: Fonts.mono, fontSize: 14, color: Colors.greyLight },
  content: { flex: 1, paddingHorizontal: Spacing.md, paddingBottom: Spacing.md },
});

const sheet = StyleSheet.create({
  header: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    marginBottom: Spacing.md,
    paddingBottom: Spacing.sm,
    borderBottomWidth: BorderWidth.thin,
    borderBottomColor: Colors.grey,
  },
  title: { fontFamily: Fonts.header, fontSize: FontSizes.md, color: Colors.cyan, letterSpacing: 2 },
  subtitle: { fontFamily: Fonts.mono, fontSize: FontSizes.xs, color: Colors.greyLight, marginTop: 3 },
  verifiedBadge: {
    borderWidth: BorderWidth.thin,
    borderColor: Colors.gold,
    paddingHorizontal: 6,
    paddingVertical: 2,
  },
  verifiedText: { fontFamily: Fonts.mono, fontSize: 8, color: Colors.gold, letterSpacing: 1 },
  value: { fontFamily: Fonts.mono, fontSize: FontSizes.sm, color: Colors.cyan },
  axisText: { fontFamily: Fonts.mono, fontSize: FontSizes.xs, color: Colors.greyLight, opacity: 0.7, marginTop: 3 },
  bodyText: { fontFamily: Fonts.mono, fontSize: FontSizes.xs, color: Colors.greyLight, lineHeight: 18 },
  grid: { flexDirection: 'row', gap: Spacing.lg, marginBottom: Spacing.xs },
  cell: { alignItems: 'center' },
  cellLabel: { fontFamily: Fonts.mono, fontSize: 9, color: Colors.grey, letterSpacing: 1, marginBottom: 2 },
  cellValue: { fontFamily: Fonts.mono, fontSize: FontSizes.sm, color: Colors.cyan },
  proxLine: { fontFamily: Fonts.mono, fontSize: FontSizes.xs, color: Colors.gold, marginBottom: 3 },
  actions: { flexDirection: 'row', gap: Spacing.sm, marginTop: Spacing.md },
  btnGold: {
    flex: 1, borderWidth: BorderWidth.thin, borderColor: Colors.gold,
    paddingVertical: Spacing.sm, alignItems: 'center',
  },
  btnGoldText: { fontFamily: Fonts.mono, fontSize: FontSizes.xs, color: Colors.gold, letterSpacing: 2 },
  btnCyan: {
    flex: 1, borderWidth: BorderWidth.thin, borderColor: Colors.cyan,
    paddingVertical: Spacing.sm, alignItems: 'center',
  },
  btnCyanText: { fontFamily: Fonts.mono, fontSize: FontSizes.xs, color: Colors.cyan, letterSpacing: 2 },
  strengthBar: { height: 4, backgroundColor: Colors.greyDark, marginBottom: 5, borderRadius: 2 },
  strengthFill: { height: 4, backgroundColor: Colors.gold, opacity: 0.7, borderRadius: 2 },
  anomalyRow: { flexDirection: 'row', alignItems: 'center', gap: Spacing.sm, paddingVertical: 3 },
  anomalyNum: { fontFamily: Fonts.mono, fontSize: 9, color: Colors.grey, width: 28 },
  anomalyMag: { fontFamily: Fonts.mono, fontSize: FontSizes.xs, color: Colors.gold, width: 60 },
  anomalyTs: { fontFamily: Fonts.mono, fontSize: 9, color: Colors.greyLight, flex: 1 },
  verifiedSmall: { fontSize: 10, color: Colors.gold },
});
