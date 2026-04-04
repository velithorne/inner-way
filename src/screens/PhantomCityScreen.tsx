import { useCallback, useMemo, useState } from 'react';
import {
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import * as THREE from 'three';
import { findDualBandPairs, isLikelySolarInverter } from '../services/deviceHeuristics';
import { rssiToDistance } from '../services/rssiToDistance';
import { useWifiStore } from '../store/useWifiStore';
import { securityLabel } from '../utils/wifiDisplay';
import { CityScene, type ScreenLabel } from '../world/CityScene';
import { layoutCityBuildings, type CityBuilding } from '../world/cityLayout';

const SPAWN = new THREE.Vector3(0, 80, 200);
const LOOK = new THREE.Vector3(0, 0, 0);

export function PhantomCityScreen() {
  const { networks, routerEstimates } = useWifiStore();
  const estimates = useMemo(() => {
    const m = new Map<string, (typeof routerEstimates)[string]>();
    for (const n of networks) {
      const e = routerEstimates[n.bssid];
      if (e) m.set(n.bssid, e);
    }
    return m;
  }, [networks, routerEstimates]);

  const buildings = useMemo(
    () => layoutCityBuildings(networks, estimates),
    [networks, estimates]
  );

  const [camPos, setCamPos] = useState(() => SPAWN.clone());
  const [camTgt, setCamTgt] = useState(() => LOOK.clone());
  const [labels, setLabels] = useState<ScreenLabel[]>([]);
  const [findQ, setFindQ] = useState('');
  const [inspector, setInspector] = useState<CityBuilding | null>(null);

  const dualPairs = useMemo(() => findDualBandPairs(networks), [networks]);

  const onScreenLabels = useCallback((ls: ScreenLabel[]) => {
    setLabels(ls);
  }, []);

  const orbit = () => {
    setCamPos(new THREE.Vector3(0, 200, 0));
    setCamTgt(new THREE.Vector3(0, 0, 0));
  };

  const reset = () => {
    setCamPos(SPAWN.clone());
    setCamTgt(LOOK.clone());
  };

  const nearest = () => {
    if (buildings.length === 0) return;
    let best = buildings[0];
    let d = camPos.distanceTo(best.position);
    for (const b of buildings) {
      const dd = camPos.distanceTo(b.position);
      if (dd < d) {
        d = dd;
        best = b;
      }
    }
    const dir = best.position.clone().sub(camPos).normalize();
    setCamPos(best.position.clone().sub(dir.multiplyScalar(35)));
    setCamTgt(best.position.clone());
    setInspector(best);
  };

  const findBuilding = () => {
    const q = findQ.trim().toLowerCase();
    if (!q) return;
    const b = buildings.find(
      (x) => x.ssid.toLowerCase().includes(q) || x.bssid.toLowerCase().includes(q)
    );
    if (b) {
      const dir = b.position.clone().sub(camPos).normalize();
      setCamPos(b.position.clone().sub(dir.multiplyScalar(28)));
      setCamTgt(b.position.clone());
      setInspector(b);
    }
  };

  return (
    <View style={styles.root}>
      <CityScene
        buildings={buildings}
        cameraPos={camPos}
        cameraTarget={camTgt}
        onScreenLabels={onScreenLabels}
      />
      <View style={styles.overlay} pointerEvents="box-none">
        {labels.map((lb) => {
          const b = buildings.find((x) => x.bssid === lb.bssid);
          if (!b || !lb.visible) return null;
          const solar = b.kind === 'solar';
          const dual = dualPairs.some(
            (p) => p[0].bssid === b.bssid || p[1].bssid === b.bssid
          );
          return (
            <View
              key={lb.bssid}
              style={[
                styles.floatingLabel,
                {
                  left: Math.max(8, lb.x - 60),
                  top: Math.max(48, lb.y - 40),
                },
              ]}
            >
              <Text style={styles.labelText} numberOfLines={1}>
                {b.ssid || b.bssid}
              </Text>
              <Text style={styles.labelRssi}>{b.rssi} dBm</Text>
              {dual ? <Text style={styles.dualBadge}>DUAL BAND</Text> : null}
              {solar ? <Text style={styles.solarIcon}>☀ IoT</Text> : null}
            </View>
          );
        })}
      </View>

      <View style={styles.destCol}>
        <Pressable style={styles.destBtn} onPress={orbit}>
          <Text style={styles.destTxt}>◉ ORBIT</Text>
        </Pressable>
        <Pressable style={styles.destBtn} onPress={reset}>
          <Text style={styles.destTxt}>⬡ RESET</Text>
        </Pressable>
        <Pressable style={styles.destBtn} onPress={nearest}>
          <Text style={styles.destTxt}>◈ NEAREST</Text>
        </Pressable>
        <View style={styles.findRow}>
          <TextInput
            style={styles.findInput}
            placeholder="SSID…"
            placeholderTextColor="#546e7a"
            value={findQ}
            onChangeText={setFindQ}
            onSubmitEditing={findBuilding}
          />
          <Pressable style={styles.findGo} onPress={findBuilding}>
            <Text style={styles.destTxt}>🔍</Text>
          </Pressable>
        </View>
      </View>

      {inspector &&
      camPos.distanceTo(inspector.position) < 20 &&
      !isLikelySolarInverter({
        ssid: inspector.ssid,
        bssid: inspector.bssid,
        rssi: inspector.rssi,
        frequency: inspector.frequency,
        channel: inspector.channel,
        capabilities: inspector.capabilities,
        timestamp: Date.now(),
      }) ? (
        <View style={styles.inspector}>
          <Text style={styles.inTitle}>◈ {inspector.ssid || '(hidden)'}</Text>
          <ScrollView style={styles.inScroll}>
            <Text style={styles.inLine}>BSSID: {inspector.bssid}</Text>
            <Text style={styles.inLine}>Signal: {inspector.rssi} dBm</Text>
            <Text style={styles.inLine}>
              Distance: ~{rssiToDistance(inspector.rssi, inspector.frequency).toFixed(0)} m
            </Text>
            <Text style={styles.inLine}>
              Band: {inspector.frequency > 4000 ? '5' : '2.4'} GHz · Ch {inspector.channel}
            </Text>
            <Text style={styles.inLine}>Security: {securityLabel(inspector.capabilities)}</Text>
            <Text style={styles.inLine}>
              Position confidence:{' '}
              {routerEstimates[inspector.bssid]?.confidence?.toFixed(0) ?? '—'}%
            </Text>
          </ScrollView>
        </View>
      ) : null}

      {inspector &&
      camPos.distanceTo(inspector.position) < 20 &&
      isLikelySolarInverter({
        ssid: inspector.ssid,
        bssid: inspector.bssid,
        rssi: inspector.rssi,
        frequency: inspector.frequency,
        channel: inspector.channel,
        capabilities: inspector.capabilities,
        timestamp: Date.now(),
      }) ? (
        <View style={styles.inspector}>
          <Text style={styles.inTitle}>☀ {inspector.ssid}</Text>
          <Text style={styles.inLine}>IoT DEVICE — Likely solar inverter</Text>
          <Text style={styles.inLine}>Signal: {inspector.rssi} dBm</Text>
        </View>
      ) : null}

      <View style={styles.hint}>
        <Text style={styles.hintText}>PHANTOM CITY · drag joystick (coming) — use destination pills</Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: '#05080c' },
  overlay: { ...StyleSheet.absoluteFillObject, pointerEvents: 'box-none' },
  floatingLabel: {
    position: 'absolute',
    backgroundColor: 'rgba(0,0,0,0.55)',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 6,
    maxWidth: 140,
  },
  labelText: { color: '#fff', fontSize: 10, fontWeight: '600' },
  labelRssi: { color: '#90a4ae', fontSize: 9 },
  dualBadge: { color: '#00ffe5', fontSize: 8, fontWeight: '700', marginTop: 2 },
  solarIcon: { color: '#ffb700', fontSize: 9, marginTop: 2 },
  destCol: {
    position: 'absolute',
    right: 8,
    top: 120,
    gap: 8,
    alignItems: 'flex-end',
  },
  destBtn: {
    backgroundColor: 'rgba(10,14,20,0.85)',
    paddingVertical: 8,
    paddingHorizontal: 12,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: '#00ffe5',
  },
  destTxt: { color: '#00ffe5', fontSize: 11, fontWeight: '700' },
  findRow: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  findInput: {
    width: 100,
    height: 32,
    backgroundColor: '#121a22',
    borderRadius: 8,
    paddingHorizontal: 8,
    color: '#eceff1',
    fontSize: 11,
  },
  findGo: {
    width: 36,
    height: 32,
    backgroundColor: 'rgba(10,14,20,0.85)',
    borderRadius: 8,
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#37474f',
  },
  inspector: {
    position: 'absolute',
    left: 12,
    bottom: 100,
    right: 12,
    maxHeight: 200,
    backgroundColor: 'rgba(10,14,20,0.94)',
    borderRadius: 12,
    padding: 12,
    borderWidth: 1,
    borderColor: '#00ffe5',
  },
  inTitle: { color: '#00ffe5', fontSize: 16, fontWeight: '700', marginBottom: 8 },
  inScroll: { maxHeight: 140 },
  inLine: { color: '#b0bec5', fontSize: 12, marginBottom: 4 },
  hint: {
    position: 'absolute',
    top: 48,
    left: 12,
    right: 120,
  },
  hintText: { color: '#546e7a', fontSize: 10 },
});
