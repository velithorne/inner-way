import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  Dimensions,
  PanResponder,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Gesture, GestureDetector } from 'react-native-gesture-handler';
import * as THREE from 'three';
import NetInfo from '@react-native-community/netinfo';

import { useWorldStore } from '../store/useWorldStore';
import { batteryLevelToEmissive } from '../world/batteryColors';
import { getCameraSpeed, getWorldCameraPosition } from '../world/worldCameraBridge';
import { getHudBoundary, getHudFps, getFpsOverlay, getHudForwardDir, toggleFpsOverlay } from '../world/worldHudBridge';
import {
  addLookDelta,
  applyPinchToTargetFov,
  getSpeedMult,
  queueFlyTo,
  setJoystick,
  setVerticalDown,
  setVerticalUp,
  toggleMovementSpeed,
} from '../world/worldNavigationBridge';
import { WORLD } from '../world/worldConstants';

const CYAN = '#00FFE5';
const CYAN_DIM = 'rgba(0,255,229,0.25)';
const GLASS = 'rgba(0,0,0,0.35)';
const JOYSTICK_MAX = 33;
const JOYSTICK_SIZE = 110;
const KNOB = 44;

const HINTS_KEY = '@silicon_control_hints_seen';
const _look = new THREE.Vector3();
const _flyPos = new THREE.Vector3();
const _origin = new THREE.Vector3();
const _dir = new THREE.Vector3();
const _toC = new THREE.Vector3();
const _closest = new THREE.Vector3();

type Props = {
  entryComplete: boolean;
};

type InspectZone = 'bat' | 'cpu' | 'ram' | 'stor' | 'net' | 'sen' | null;

function estimateBatteryMinutes(level: number, currentUa: number): string {
  if (level <= 0 || currentUa >= 0) return '—';
  const drainMahPerH = Math.abs(currentUa) / 1e6 * 1000;
  if (drainMahPerH < 1) return '—';
  const remPct = level / 100;
  const hours = (remPct * 3000) / drainMahPerH;
  if (!Number.isFinite(hours) || hours > 48) return '—';
  return `~${Math.round(hours * 60)} min`;
}

export function WorldNavigator({ entryComplete }: Props) {
  const snapshot = useWorldStore((s) => s.snapshot);
  const batteryLevel = useWorldStore((s) => s.batteryLevel);
  const setWifi = useWorldStore((s) => s.setWifiStrength);

  const { width: sw, height: sh } = Dimensions.get('window');
  const halfH = sh * 0.5;
  const stripTop = sh * 0.28;

  const [knob, setKnob] = useState({ x: 0, y: 0 });
  const [joyBright, setJoyBright] = useState(false);
  const [speedHud, setSpeedHud] = useState('');
  const speedHide = useRef<ReturnType<typeof setTimeout> | null>(null);
  const [speedToggleHud, setSpeedToggleHud] = useState('◈ 1x');

  const [crossBright, setCrossBright] = useState(0.2);
  const lookLast = useRef({ x: 0, y: 0 });

  const [boundaryText, setBoundaryText] = useState('');
  const [fpsText, setFpsText] = useState('');

  const [zoneLabel, setZoneLabel] = useState('');
  const [zoneDetail, setZoneDetail] = useState('');
  const zoneTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const [inspectLines, setInspectLines] = useState<string[]>([]);
  const [inspectBorder, setInspectBorder] = useState(CYAN);
  const stillT = useRef(0);

  const [nearestDest, setNearestDest] = useState('');

  const pinchLast = useRef(1);
  const joyTapRef = useRef(0);

  const [showHints, setShowHints] = useState(false);

  useEffect(() => {
    const sub = NetInfo.addEventListener((state) => {
      if (state.type === 'wifi' && typeof state.details === 'object' && state.details !== null) {
        const str = (state.details as { strength?: number }).strength;
        if (typeof str === 'number') setWifi(Math.max(0, Math.min(1, str / 100)));
        else setWifi(state.isConnected ? 0.75 : 0);
      } else {
        setWifi(state.isConnected ? 0.5 : 0);
      }
    });
    return () => sub();
  }, [setWifi]);

  useEffect(() => {
    (async () => {
      const v = await AsyncStorage.getItem(HINTS_KEY);
      if (v !== '1') setShowHints(true);
    })();
  }, []);

  useEffect(() => {
    if (!showHints || !entryComplete) return;
    const t = setTimeout(() => {
      setShowHints(false);
      AsyncStorage.setItem(HINTS_KEY, '1').catch(() => {});
    }, 10000);
    return () => clearTimeout(t);
  }, [showHints, entryComplete]);

  useEffect(() => {
    setSpeedToggleHud(getSpeedMult() >= 2 ? '◈ 4x' : '◈ 1x');
  }, [entryComplete]);

  const resolveInspectZone = useCallback((): InspectZone => {
    _origin.copy(getWorldCameraPosition());
    const fd = getHudForwardDir();
    _dir.set(fd.x, fd.y, fd.z).normalize();
    const zones: { key: InspectZone; pos: THREE.Vector3; r: number }[] = [
      { key: 'bat', pos: new THREE.Vector3(0, 0, 0), r: 90 },
      { key: 'cpu', pos: WORLD.processor.clone(), r: 160 },
      { key: 'ram', pos: WORLD.ramOcean.clone(), r: 240 },
      { key: 'stor', pos: WORLD.storage.clone(), r: 180 },
      { key: 'net', pos: WORLD.networkSky.clone(), r: 200 },
      { key: 'sen', pos: WORLD.sensors.clone(), r: 100 },
    ];
    let best: { key: InspectZone; d: number } | null = null;
    for (const z of zones) {
      _toC.copy(z.pos).sub(_origin);
      const t = _toC.dot(_dir);
      if (t < 0) continue;
      _closest.copy(_origin).addScaledVector(_dir, t);
      const dist = _closest.distanceTo(z.pos);
      if (dist < z.r && (!best || dist < best.d)) best = { key: z.key, d: dist };
    }
    return best?.key ?? null;
  }, []);

  const buildInspector = useCallback((zone: InspectZone) => {
    const snap = useWorldStore.getState().snapshot;
    const lines: string[] = [];
    let border = CYAN;
    if (zone === 'bat' && snap?.battery) {
      border = `#${batteryLevelToEmissive(snap.battery.level).getHexString()}`;
      lines.push('◈ BATTERY REACTOR');
      lines.push(`Charge: ${snap.battery.level}%`);
      lines.push(`Draw: ${snap.battery.currentNow} µA`);
      lines.push(`State: ${snap.battery.isCharging ? 'CHARGING' : 'DISCHARGING'}`);
      lines.push(`Est. remaining: ${estimateBatteryMinutes(snap.battery.level, snap.battery.currentNow)}`);
    } else if (zone === 'cpu' && snap?.cpu?.length) {
      lines.push('◈ PROCESSOR COMPLEX');
      snap.cpu.slice(0, 8).forEach((c, i) => {
        const ghz = c.maxFreqKhz ? (c.maxFreqKhz / 1e6).toFixed(2) : '—';
        lines.push(`Core ${i}: ${c.usage}% @ ${ghz}GHz`);
      });
      const avg = snap.cpu.reduce((a, c) => a + c.usage, 0) / snap.cpu.length;
      lines.push(`Aggregate: ${avg.toFixed(0)}% load`);
    } else if (zone === 'ram' && snap?.memory) {
      border = '#8899ff';
      const u = snap.memory.usedRam / (1024 * 1024);
      const f = snap.memory.availableRam / (1024 * 1024);
      const t = snap.memory.totalRam / (1024 * 1024);
      const pr = u / Math.max(1, t) * 100;
      lines.push('◈ MEMORY OCEAN');
      lines.push(`Used: ${u.toFixed(0)} MB`);
      lines.push(`Free: ${f.toFixed(0)} MB`);
      lines.push(`Total: ${t.toFixed(0)} MB`);
      lines.push(`Pressure: ${pr > 80 ? 'HIGH' : pr > 55 ? 'NORMAL' : 'LOW'}`);
      lines.push(`Islands: ${snap.apps?.length ?? 0} processes mapped`);
    } else if (zone === 'stor' && snap?.storage) {
      lines.push('◈ DATA RANGE');
      const u = snap.storage.usedBytes / (1024 ** 3);
      const f = snap.storage.freeBytes / (1024 ** 3);
      const tot = snap.storage.totalBytes / (1024 ** 3);
      lines.push(`Used: ${u.toFixed(2)} GB`);
      lines.push(`Free: ${f.toFixed(2)} GB`);
      lines.push(`Total: ${tot.toFixed(2)} GB`);
      lines.push(`Fill: ${((u / tot) * 100).toFixed(0)}%`);
    } else if (zone === 'net' && snap?.network) {
      lines.push('◈ NETWORK LAYER');
      lines.push(`Download: ${(snap.network.rxBytesPerSecond / 1024).toFixed(1)} KB/s`);
      lines.push(`Upload: ${(snap.network.txBytesPerSecond / 1024).toFixed(1)} KB/s`);
      lines.push(`WiFi: see device status`);
    } else if (zone === 'sen') {
      lines.push('◈ SENSOR CLUSTER');
      lines.push('Magnetometer · Gyro · Accel · Baro');
    } else {
      return;
    }
    setInspectBorder(border);
    setInspectLines(lines);
  }, []);

  const updateZoneHud = useCallback(() => {
    const p = getWorldCameraPosition();
    const zones: { name: string; pos: THREE.Vector3; r: number; key: string }[] = [
      { name: 'PROCESSOR CITY', key: 'cpu', pos: WORLD.processor.clone(), r: 140 },
      { name: 'RAM OCEAN', key: 'ram', pos: WORLD.ramOcean.clone(), r: 220 },
      { name: 'STORAGE RANGE', key: 'stor', pos: WORLD.storage.clone(), r: 160 },
      { name: 'SENSOR OUTPOSTS', key: 'sen', pos: WORLD.sensors.clone(), r: 90 },
      { name: 'NETWORK SKY', key: 'net', pos: WORLD.networkSky.clone(), r: 180 },
      { name: 'BATTERY SUN', key: 'bat', pos: new THREE.Vector3(0, 0, 0), r: 80 },
    ];
    let best: (typeof zones)[0] | null = null;
    let bd = Infinity;
    for (const z of zones) {
      const d = p.distanceTo(z.pos);
      if (d < z.r && d < bd) {
        bd = d;
        best = z;
      }
    }
    if (!best) {
      setZoneLabel('');
      setZoneDetail('');
      return;
    }
    setZoneLabel(best.name);
    const snap = useWorldStore.getState().snapshot;
    if (best.key === 'cpu' && snap?.cpu?.length) {
      const avg = snap.cpu.reduce((a, c) => a + c.usage, 0) / snap.cpu.length;
      setZoneDetail(`${avg.toFixed(0)}% avg load`);
    } else if (best.key === 'ram' && snap?.memory) {
      const u = snap.memory.usedRam / (1024 * 1024);
      const t = snap.memory.totalRam / (1024 * 1024);
      setZoneDetail(`${u.toFixed(0)}MB / ${t.toFixed(0)}MB`);
    } else if (best.key === 'stor' && snap?.storage) {
      const u = snap.storage.usedBytes / (1024 ** 3);
      const tot = snap.storage.totalBytes / (1024 ** 3);
      setZoneDetail(`${u.toFixed(2)}GB / ${tot.toFixed(2)}GB`);
    } else if (best.key === 'bat' && snap?.battery) {
      setZoneDetail(`${snap.battery.level}% · ${snap.battery.currentNow}µA`);
    } else {
      setZoneDetail('');
    }
    if (zoneTimer.current) clearTimeout(zoneTimer.current);
    zoneTimer.current = setTimeout(() => setZoneLabel(''), 3000);
  }, []);

  useEffect(() => {
    if (!entryComplete) return;
    const id = setInterval(updateZoneHud, 400);
    return () => clearInterval(id);
  }, [entryComplete, updateZoneHud]);

  useEffect(() => {
    if (!entryComplete) return;
    const id = setInterval(() => {
      const p = getWorldCameraPosition();
      const dests: { k: string; pos: THREE.Vector3 }[] = [
        { k: 'CORE', pos: WORLD.processor.clone() },
        { k: 'RAM', pos: WORLD.ramOcean.clone() },
        { k: 'STORE', pos: WORLD.storage.clone() },
        { k: 'SUN', pos: new THREE.Vector3(0, 0, 0) },
        { k: 'NET', pos: WORLD.networkSky.clone() },
        { k: 'ORBIT', pos: new THREE.Vector3(0, 300, 0) },
      ];
      let best = dests[0];
      let bd = p.distanceTo(best.pos);
      for (const d of dests) {
        const dist = p.distanceTo(d.pos);
        if (dist < bd) {
          bd = dist;
          best = d;
        }
      }
      setNearestDest(best.k);
    }, 500);
    return () => clearInterval(id);
  }, [entryComplete]);

  useEffect(() => {
    if (!entryComplete) return;
    const id = setInterval(() => {
      const b = getHudBoundary();
      if (b === 'soft') setBoundaryText('APPROACHING BOUNDARY');
      else if (b === 'hard') {
        setBoundaryText('BOUNDARY');
        setTimeout(() => setBoundaryText(''), 400);
      } else setBoundaryText('');
      if (getFpsOverlay()) {
        const { fps, drawCalls } = getHudFps();
        setFpsText(`${fps.toFixed(0)} FPS · ${drawCalls} calls`);
      } else setFpsText('');
    }, 200);
    return () => clearInterval(id);
  }, [entryComplete]);

  useEffect(() => {
    if (!entryComplete) return;
    const id = setInterval(() => {
      const sp = getCameraSpeed();
      if (sp < 0.5) {
        stillT.current += 0.2;
        if (stillT.current >= 2) {
          const z = resolveInspectZone();
          if (z) buildInspector(z);
          else setInspectLines([]);
        }
      } else {
        stillT.current = 0;
        setInspectLines([]);
      }
    }, 200);
    return () => clearInterval(id);
  }, [entryComplete, resolveInspectZone, buildInspector]);

  const joyResponder = useMemo(
    () =>
      PanResponder.create({
        onStartShouldSetPanResponder: () => entryComplete,
        onPanResponderGrant: (e) => {
          setJoyBright(true);
          const now = Date.now();
          if (now - joyTapRef.current < 280) {
            toggleMovementSpeed();
            setSpeedToggleHud(getSpeedMult() >= 2 ? '◈ 4x' : '◈ 1x');
            joyTapRef.current = 0;
          } else {
            joyTapRef.current = now;
          }
        },
        onPanResponderMove: (_e, g) => {
          if (!entryComplete) return;
          let ox = g.dx;
          let oy = g.dy;
          const len = Math.hypot(ox, oy);
          if (len > JOYSTICK_MAX) {
            ox = (ox / len) * JOYSTICK_MAX;
            oy = (oy / len) * JOYSTICK_MAX;
          }
          setKnob({ x: ox, y: oy });
          setJoystick(ox / JOYSTICK_MAX, oy / JOYSTICK_MAX);
          const base = 1.2 + (4 - 1.2) * Math.min(1, len / JOYSTICK_MAX);
          const s = base * getSpeedMult();
          setSpeedHud(`${s.toFixed(1)} u/s`);
          if (speedHide.current) clearTimeout(speedHide.current);
        },
        onPanResponderRelease: () => {
          setJoyBright(false);
          setKnob({ x: 0, y: 0 });
          setJoystick(0, 0);
          speedHide.current = setTimeout(() => setSpeedHud(''), 1000);
        },
      }),
    [entryComplete],
  );

  const lookResponder = useMemo(
    () =>
      PanResponder.create({
        onStartShouldSetPanResponder: () => entryComplete,
        onPanResponderGrant: (e) => {
          lookLast.current = { x: e.nativeEvent.pageX, y: e.nativeEvent.pageY };
        },
        onPanResponderMove: (e) => {
          if (!entryComplete) return;
          const dx = e.nativeEvent.pageX - lookLast.current.x;
          const dy = e.nativeEvent.pageY - lookLast.current.y;
          lookLast.current = { x: e.nativeEvent.pageX, y: e.nativeEvent.pageY };
          addLookDelta(dx, dy);
          setCrossBright(0.55);
        },
        onPanResponderRelease: () => {
          setTimeout(() => setCrossBright(0.2), 120);
        },
      }),
    [entryComplete],
  );

  const pinchGesture = useMemo(
    () =>
      Gesture.Pinch()
        .enabled(entryComplete)
        .onBegin(() => {
          pinchLast.current = 1;
        })
        .onUpdate((e) => {
          const prev = pinchLast.current;
          const s = e.scale;
          if (prev > 0 && s > 0) applyPinchToTargetFov(prev, s);
          pinchLast.current = s;
        }),
    [entryComplete],
  );

  const tripleTap = useMemo(
    () =>
      Gesture.Tap()
        .numberOfTaps(3)
        .maxDuration(500)
        .onEnd(() => {
          toggleFpsOverlay();
        }),
    [],
  );

  const globalGestures = useMemo(
    () => Gesture.Simultaneous(pinchGesture, tripleTap),
    [pinchGesture, tripleTap],
  );

  const fly = (key: string) => {
    switch (key) {
      case 'CORE':
        queueFlyTo(_flyPos.set(-120, 45, 35), _look.copy(WORLD.processor));
        break;
      case 'RAM':
        queueFlyTo(_flyPos.set(50, -15, 60), _look.copy(WORLD.ramOcean));
        break;
      case 'STORE':
        queueFlyTo(_flyPos.set(55, 40, 140), _look.copy(WORLD.storage));
        break;
      case 'SUN':
        queueFlyTo(_flyPos.set(65, 35, 65), _look.set(0, 0, 0));
        break;
      case 'NET':
        queueFlyTo(_flyPos.set(0, 75, 45), _look.copy(WORLD.networkSky));
        break;
      case 'ORBIT':
        queueFlyTo(_flyPos.set(0, 300, 0.1), _look.set(0, 0, 0));
        break;
      default:
        break;
    }
  };

  const cores = snapshot?.cpu ?? [];
  const batHex = `#${batteryLevelToEmissive(batteryLevel).getHexString()}`;

  if (!entryComplete) {
    return null;
  }

  return (
    <View style={styles.overlay} pointerEvents="box-none">
      <GestureDetector gesture={globalGestures}>
        <View style={StyleSheet.absoluteFill} pointerEvents="box-none" />
      </GestureDetector>

      <View style={[styles.lookZone, { width: sw, height: halfH }]} {...lookResponder.panHandlers} />

      {showHints ? (
        <>
          <View style={[styles.hintTop, { width: sw - 8, height: halfH - 4 }]} pointerEvents="none">
            <Text style={styles.hintLabel}>LOOK</Text>
          </View>
          <View style={[styles.hintBottom, { width: sw - 8, top: halfH + 4, height: sh - halfH - 8 }]} pointerEvents="none">
            <Text style={styles.hintLabelMove}>MOVE</Text>
          </View>
        </>
      ) : null}

      <View style={styles.joystickWrap} {...joyResponder.panHandlers}>
        <Text style={styles.speedToggle}>{speedToggleHud}</Text>
        {speedHud ? <Text style={styles.speedAbove}>{speedHud}</Text> : null}
        <View style={[styles.joystickRing, joyBright && styles.joystickRingActive]}>
          <View
            style={[
              styles.knob,
              {
                transform: [{ translateX: knob.x }, { translateY: knob.y }],
              },
            ]}
          />
        </View>
      </View>

      <View style={styles.vertBtns}>
        <Text
          style={styles.vertBtn}
          onPressIn={() => setVerticalUp(true)}
          onPressOut={() => setVerticalUp(false)}
        >
          ↑
        </Text>
        <Text
          style={styles.vertBtn}
          onPressIn={() => setVerticalDown(true)}
          onPressOut={() => setVerticalDown(false)}
        >
          ↓
        </Text>
      </View>

      <View style={styles.topLeft}>
        <Text style={[styles.zoneTitle, { opacity: zoneLabel ? 0.65 : 0 }]}>{zoneLabel}</Text>
        {zoneDetail ? <Text style={styles.zoneDetail}>{zoneDetail}</Text> : null}
      </View>

      {inspectLines.length > 0 ? (
        <View style={[styles.inspectPanel, { borderLeftColor: inspectBorder }]}>
          {inspectLines.map((line, i) => (
            <Text key={i} style={i === 0 ? styles.inspectHead : styles.inspectLine}>
              {line}
            </Text>
          ))}
        </View>
      ) : null}

      <View style={styles.topRight}>
        <Text style={[styles.bat, { color: batHex }]}>[{batteryLevel}%]</Text>
        <View style={styles.coreBar}>
          {Array.from({ length: Math.min(8, cores.length || 8) }).map((_, i) => {
            const u = cores[i]?.usage ?? 0;
            return (
              <View
                key={i}
                style={[
                  styles.coreSeg,
                  { opacity: 0.15 + (u / 100) * 0.85 },
                ]}
              />
            );
          })}
        </View>
      </View>

      <View style={styles.crosshair} pointerEvents="none">
        <View style={[styles.crossH, { opacity: crossBright }]} />
        <View style={[styles.crossV, { opacity: crossBright }]} />
      </View>

      <View style={[styles.destStrip, { top: stripTop }]}>
        {(
          [
            ['CORE', '⬡ CORE'],
            ['RAM', '〰 RAM'],
            ['STORE', '⛰ STORE'],
            ['SUN', '☀ SUN'],
            ['NET', '📡 NET'],
            ['ORBIT', '◉ ORBIT'],
          ] as const
        ).map(([k, label]) => (
          <Text
            key={k}
            style={[styles.destBtn, nearestDest === k && styles.destBtnNear]}
            onPress={() => fly(k)}
          >
            {label}
          </Text>
        ))}
      </View>

      {boundaryText ? (
        <View style={[styles.vignette, boundaryText === 'BOUNDARY' && styles.vignetteHard]}>
          <Text style={styles.boundaryTxt}>{boundaryText}</Text>
        </View>
      ) : null}

      {fpsText ? (
        <View style={styles.fpsBox}>
          <Text style={styles.fpsTxt}>{fpsText}</Text>
        </View>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  overlay: {
    ...StyleSheet.absoluteFillObject,
  },
  lookZone: {
    position: 'absolute',
    left: 0,
    top: 0,
    backgroundColor: 'transparent',
  },
  hintTop: {
    position: 'absolute',
    left: 4,
    top: 4,
    borderWidth: 1,
    borderColor: 'rgba(0,255,229,0.2)',
    borderRadius: 6,
    justifyContent: 'flex-start',
    padding: 8,
  },
  hintBottom: {
    position: 'absolute',
    left: 4,
    borderWidth: 1,
    borderColor: 'rgba(0,255,229,0.12)',
    borderRadius: 6,
    justifyContent: 'flex-end',
    padding: 8,
    paddingBottom: 120,
  },
  hintLabel: {
    color: CYAN,
    fontSize: 10,
    opacity: 0.45,
    fontFamily: 'monospace',
  },
  hintLabelMove: {
    color: CYAN,
    fontSize: 10,
    opacity: 0.35,
    fontFamily: 'monospace',
  },
  joystickWrap: {
    position: 'absolute',
    left: 24,
    bottom: 24,
    width: JOYSTICK_SIZE,
    height: JOYSTICK_SIZE + 52,
    justifyContent: 'flex-end',
  },
  speedToggle: {
    position: 'absolute',
    top: 18,
    alignSelf: 'center',
    color: CYAN,
    fontSize: 11,
    fontFamily: 'monospace',
    opacity: 0.75,
  },
  speedAbove: {
    position: 'absolute',
    top: 36,
    alignSelf: 'center',
    color: CYAN,
    fontSize: 10,
    fontFamily: 'monospace',
    opacity: 0.7,
  },
  joystickRing: {
    width: JOYSTICK_SIZE,
    height: JOYSTICK_SIZE,
    borderRadius: JOYSTICK_SIZE / 2,
    borderWidth: 1.5,
    borderColor: CYAN_DIM,
    backgroundColor: GLASS,
    alignItems: 'center',
    justifyContent: 'center',
  },
  joystickRingActive: {
    borderColor: 'rgba(0,255,229,0.6)',
  },
  knob: {
    width: KNOB,
    height: KNOB,
    borderRadius: KNOB / 2,
    backgroundColor: 'rgba(0,255,229,0.35)',
    shadowColor: CYAN,
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0.4,
    shadowRadius: 12,
  },
  vertBtns: {
    position: 'absolute',
    left: 24 + JOYSTICK_SIZE + 10,
    bottom: 40,
    gap: 8,
  },
  vertBtn: {
    width: 36,
    height: 32,
    textAlign: 'center',
    lineHeight: 30,
    borderWidth: 1,
    borderColor: CYAN_DIM,
    backgroundColor: GLASS,
    color: CYAN,
    fontSize: 16,
    overflow: 'hidden',
    borderRadius: 6,
  },
  topLeft: {
    position: 'absolute',
    top: 48,
    left: 16,
    maxWidth: '42%',
  },
  inspectPanel: {
    position: 'absolute',
    top: 120,
    left: 16,
    width: 220,
    padding: 10,
    backgroundColor: 'rgba(0,0,0,0.7)',
    borderWidth: 1,
    borderColor: 'rgba(0,255,229,0.3)',
    borderLeftWidth: 3,
    borderRadius: 4,
  },
  inspectHead: {
    color: CYAN,
    fontSize: 11,
    fontFamily: 'monospace',
    marginBottom: 6,
    fontWeight: '700',
  },
  inspectLine: {
    color: '#b0c8d0',
    fontSize: 10,
    fontFamily: 'monospace',
    marginBottom: 3,
  },
  zoneTitle: {
    color: CYAN,
    fontSize: 11,
    fontWeight: '700',
    letterSpacing: 1,
    fontFamily: 'monospace',
  },
  zoneDetail: {
    color: 'rgba(0,255,229,0.55)',
    fontSize: 10,
    marginTop: 4,
    fontFamily: 'monospace',
  },
  topRight: {
    position: 'absolute',
    top: 48,
    right: 72,
    alignItems: 'flex-end',
  },
  bat: {
    fontSize: 13,
    fontWeight: '700',
    fontVariant: ['tabular-nums'],
    fontFamily: 'monospace',
  },
  coreBar: {
    flexDirection: 'row',
    gap: 3,
    marginTop: 6,
  },
  coreSeg: {
    width: 5,
    height: 10,
    backgroundColor: CYAN,
    borderRadius: 1,
  },
  crosshair: {
    ...StyleSheet.absoluteFillObject,
    justifyContent: 'center',
    alignItems: 'center',
  },
  crossH: {
    position: 'absolute',
    width: 16,
    height: 1,
    backgroundColor: 'rgba(255,255,255,0.25)',
  },
  crossV: {
    position: 'absolute',
    width: 1,
    height: 16,
    backgroundColor: 'rgba(255,255,255,0.25)',
  },
  destStrip: {
    position: 'absolute',
    right: 8,
    gap: 6,
  },
  destBtn: {
    width: 64,
    height: 32,
    borderWidth: 1,
    borderColor: 'rgba(0,255,229,0.2)',
    backgroundColor: 'rgba(0,0,0,0.5)',
    color: CYAN,
    fontSize: 9,
    textAlign: 'center',
    lineHeight: 30,
    fontFamily: 'monospace',
    overflow: 'hidden',
    borderRadius: 4,
  },
  destBtnNear: {
    borderColor: 'rgba(0,255,229,0.55)',
  },
  vignette: {
    ...StyleSheet.absoluteFillObject,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: 'rgba(255,80,0,0.08)',
    pointerEvents: 'none',
  },
  vignetteHard: {
    backgroundColor: 'rgba(255,0,0,0.15)',
  },
  boundaryTxt: {
    color: 'rgba(255,180,120,0.7)',
    fontSize: 11,
    letterSpacing: 2,
    fontFamily: 'monospace',
  },
  fpsBox: {
    position: 'absolute',
    top: 100,
    alignSelf: 'center',
    paddingHorizontal: 8,
    paddingVertical: 4,
    backgroundColor: 'rgba(0,0,0,0.45)',
    borderRadius: 4,
  },
  fpsTxt: {
    color: CYAN,
    fontSize: 11,
    fontFamily: 'monospace',
  },
});
