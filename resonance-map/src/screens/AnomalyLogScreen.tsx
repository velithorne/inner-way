import React, { useEffect, useState, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TouchableOpacity,
  StatusBar,
  Platform,
  Alert,
} from 'react-native';
import { useFocusEffect, useNavigation } from '@react-navigation/native';
import { loadAnomalyLog, clearAnomalyLog, AnomalyEntry } from '../services/anomalyLog';
import { Colors, Fonts, FontSizes, Spacing, BorderWidth } from '../constants/theme';
import { FIELD_WEAK_MAX, FIELD_NORMAL_MAX } from '../constants/thresholds';

function getMagnitudeColor(magnitude: number): string {
  if (magnitude < FIELD_WEAK_MAX) return Colors.blueField;
  if (magnitude < FIELD_NORMAL_MAX) return Colors.cyan;
  return Colors.gold;
}

function formatTimestamp(iso: string): string {
  try {
    const d = new Date(iso);
    return d.toUTCString().replace('GMT', 'UTC');
  } catch {
    return iso;
  }
}

function formatCoords(coords: AnomalyEntry['coordinates']): string {
  if (coords.lat === null || coords.lng === null) return 'NO GPS FIX';
  return `${coords.lat.toFixed(4)}°, ${coords.lng.toFixed(4)}°`;
}

interface EntryCardProps {
  entry: AnomalyEntry;
  index: number;
}

function EntryCard({ entry, index }: EntryCardProps) {
  const magColor = getMagnitudeColor(entry.magnitude);
  const deltaSign = entry.delta >= 0 ? '+' : '';

  return (
    <View style={card.container}>
      <View style={card.topRow}>
        <Text style={card.index}>#{(index + 1).toString().padStart(3, '0')}</Text>
        <Text style={card.timestamp}>{formatTimestamp(entry.timestamp)}</Text>
        {!entry.synced && <Text style={card.unsynced}>●</Text>}
      </View>
      <View style={card.dataRow}>
        <View style={card.dataItem}>
          <Text style={card.dataLabel}>MAG</Text>
          <Text style={[card.dataValue, { color: magColor }]}>
            {entry.magnitude.toFixed(2)} µT
          </Text>
        </View>
        <View style={card.dataItem}>
          <Text style={card.dataLabel}>DELTA</Text>
          <Text style={[card.dataValue, { color: Colors.gold }]}>
            {deltaSign}{entry.delta.toFixed(2)}
          </Text>
        </View>
        <View style={card.dataItem}>
          <Text style={card.dataLabel}>HDG</Text>
          <Text style={card.dataValue}>{entry.heading.toFixed(1)}°</Text>
        </View>
      </View>
      <View style={card.axisRow}>
        <Text style={card.axisText}>
          X:{entry.x.toFixed(1)}  Y:{entry.y.toFixed(1)}  Z:{entry.z.toFixed(1)}  µT
        </Text>
      </View>
      <Text style={card.coords}>{formatCoords(entry.coordinates)}</Text>
    </View>
  );
}

const card = StyleSheet.create({
  container: {
    backgroundColor: Colors.backgroundCard,
    borderWidth: BorderWidth.thin,
    borderColor: Colors.grey,
    marginHorizontal: Spacing.md,
    marginBottom: Spacing.sm,
    padding: Spacing.sm,
    borderLeftWidth: 2,
    borderLeftColor: Colors.gold,
  },
  topRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: Spacing.xs,
    gap: Spacing.sm,
  },
  index: {
    fontFamily: Fonts.mono,
    fontSize: FontSizes.xs,
    color: Colors.grey,
  },
  timestamp: {
    fontFamily: Fonts.mono,
    fontSize: FontSizes.xs,
    color: Colors.greyLight,
    flex: 1,
  },
  unsynced: {
    fontSize: 8,
    color: Colors.gold,
  },
  dataRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: Spacing.xs,
  },
  dataItem: {
    alignItems: 'center',
  },
  dataLabel: {
    fontFamily: Fonts.mono,
    fontSize: FontSizes.xs,
    color: Colors.grey,
    letterSpacing: 1,
    marginBottom: 1,
  },
  dataValue: {
    fontFamily: Fonts.mono,
    fontSize: FontSizes.sm,
    color: Colors.cyan,
  },
  axisRow: {
    marginBottom: Spacing.xs,
  },
  axisText: {
    fontFamily: Fonts.mono,
    fontSize: FontSizes.xs,
    color: Colors.greyLight,
    opacity: 0.7,
  },
  coords: {
    fontFamily: Fonts.mono,
    fontSize: FontSizes.xs,
    color: Colors.grey,
    letterSpacing: 0.5,
  },
});

export default function AnomalyLogScreen() {
  const navigation = useNavigation();
  const [entries, setEntries] = useState<AnomalyEntry[]>([]);
  const [loading, setLoading] = useState(true);

  const loadData = useCallback(async () => {
    setLoading(true);
    const data = await loadAnomalyLog();
    setEntries(data);
    setLoading(false);
  }, []);

  useFocusEffect(
    useCallback(() => {
      loadData();
    }, [loadData])
  );

  const handleClear = () => {
    Alert.alert(
      'CLEAR LOG',
      'Delete all anomaly records? This cannot be undone.',
      [
        { text: 'CANCEL', style: 'cancel' },
        {
          text: 'CLEAR',
          style: 'destructive',
          onPress: async () => {
            await clearAnomalyLog();
            setEntries([]);
          },
        },
      ]
    );
  };

  return (
    <View style={styles.root}>
      <StatusBar barStyle="light-content" backgroundColor={Colors.background} />

      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backBtn}>
          <Text style={styles.backText}>← BACK</Text>
        </TouchableOpacity>
        <Text style={styles.headerTitle}>ANOMALY LOG</Text>
        <TouchableOpacity onPress={handleClear} style={styles.clearBtn}>
          <Text style={styles.clearText}>CLEAR</Text>
        </TouchableOpacity>
      </View>

      <View style={styles.statsBar}>
        <Text style={styles.statsText}>
          {entries.length} DETECTIONS RECORDED
        </Text>
        <Text style={styles.statsSubText}>
          {entries.filter((e) => !e.synced).length} PENDING SYNC  ·  PHASE 3
        </Text>
      </View>

      {loading ? (
        <View style={styles.empty}>
          <Text style={styles.emptyText}>LOADING...</Text>
        </View>
      ) : entries.length === 0 ? (
        <View style={styles.empty}>
          <Text style={styles.emptyIcon}>◈</Text>
          <Text style={styles.emptyText}>NO ANOMALIES DETECTED</Text>
          <Text style={styles.emptySubText}>
            The log will populate automatically when anomalies occur during field scanning.
          </Text>
        </View>
      ) : (
        <FlatList
          data={entries}
          keyExtractor={(item) => item.id}
          renderItem={({ item, index }) => <EntryCard entry={item} index={index} />}
          contentContainerStyle={styles.listContent}
          showsVerticalScrollIndicator={false}
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: Spacing.md,
    paddingTop: Platform.OS === 'android' ? Spacing.xl : Spacing.lg,
    paddingBottom: Spacing.sm,
    borderBottomWidth: BorderWidth.thin,
    borderBottomColor: Colors.grey,
  },
  backBtn: {
    padding: Spacing.xs,
    width: 80,
  },
  backText: {
    fontFamily: Fonts.mono,
    fontSize: FontSizes.sm,
    color: Colors.cyanDim,
    letterSpacing: 1,
  },
  headerTitle: {
    fontFamily: Fonts.header,
    fontSize: FontSizes.md,
    color: Colors.cyan,
    letterSpacing: 4,
  },
  clearBtn: {
    padding: Spacing.xs,
    width: 80,
    alignItems: 'flex-end',
  },
  clearText: {
    fontFamily: Fonts.mono,
    fontSize: FontSizes.xs,
    color: Colors.red,
    letterSpacing: 1,
  },
  statsBar: {
    paddingHorizontal: Spacing.md,
    paddingVertical: Spacing.sm,
    borderBottomWidth: BorderWidth.thin,
    borderBottomColor: Colors.greyDark,
    backgroundColor: Colors.backgroundPanel,
  },
  statsText: {
    fontFamily: Fonts.mono,
    fontSize: FontSizes.sm,
    color: Colors.gold,
    letterSpacing: 1,
  },
  statsSubText: {
    fontFamily: Fonts.mono,
    fontSize: FontSizes.xs,
    color: Colors.grey,
    marginTop: 2,
    letterSpacing: 0.5,
  },
  listContent: {
    paddingTop: Spacing.md,
    paddingBottom: Spacing.xxl,
  },
  empty: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: Spacing.xl,
    gap: Spacing.md,
  },
  emptyIcon: {
    fontSize: 40,
    color: Colors.grey,
  },
  emptyText: {
    fontFamily: Fonts.header,
    fontSize: FontSizes.md,
    color: Colors.grey,
    letterSpacing: 3,
    textAlign: 'center',
  },
  emptySubText: {
    fontFamily: Fonts.mono,
    fontSize: FontSizes.sm,
    color: Colors.greyLight,
    textAlign: 'center',
    lineHeight: 20,
    opacity: 0.6,
  },
});
