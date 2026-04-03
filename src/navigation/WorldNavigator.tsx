import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  Dimensions,
  PanResponder,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { Gesture, GestureDetector } from 'react-native-gesture-handler';
import * as THREE from 'three';

import { useWorldStore } from '../store/useWorldStore';
import { batteryLevelToEmissive } from '../world/batteryColors';
import { getCameraSpeed, getWorldCameraPosition } from '../world/worldCameraBridge';
import { getHudBoundary, getHudFps, getFpsOverlay, toggleFpsOverlay } from '../world/worldHudBridge';
import {
  addLookDelta,
  getSpeedMult,
  multiplySpeedMult,
  queueFlyTo,
  setJoystick,
  setVerticalDown,
  setVerticalUp,
} from '../world/worldNavigationBridge';
import { WORLD } from '../world/worldConstants';

const CYAN = '#00FFE5';
const CYAN_DIM = 'rgba(0,255,229,0.25)';
const GLASS = 'rgba(0,0,0,0.35)';
const JOYSTICK_MAX = 33;
const JOYSTICK_SIZE = 110;
const KNOB = 44;

const _look = new THREE.Vector3();
const _flyPos = new THREE.Vector3();

type Props = {
  entryComplete: boolean;
};

export function WorldNavigator({ entryComplete }: Props) {
  const snapshot = useWorldStore((s) => s.snapshot);
  const batteryLevel = useWorldStore((s) => s.batteryLevel);

  const { width: sw, height: sh } = Dimensions.get('window');
  const rightPad = 80;
  const lookW = sw * 0.5 - rightPad;
  const stripTop = sh * 0.28;

  const [knob, setKnob] = useState({ x: 0, y: 0 });
  const [joyBright, setJoyBright] = useState(false);
  const [speedHud, setSpeedHud] = useState('');
  const speedHide = useRef<ReturnType<typeof setTimeout> | null>(null);

  const [multHud, setMultHud] = useState('');
  const multHide = useRef<ReturnType<typeof setTimeout> | null>(null);

  const [crossBright, setCrossBright] = useState(0.2);
  const lookLast = useRef({ x: 0, y: 0 });

  const [boundaryText, setBoundaryText] = useState('');
  const [fpsText, setFpsText] = useState('');

  const [zoneLabel, setZoneLabel] = useState('');
  const [zoneDetail, setZoneDetail] = useState('');
  const zoneTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const [inspect, setInspect] = useState<{ title: string; body: string } | null>(null);
  const stillT = useRef(0);

  const [nearestDest, setNearestDest] = useState('');

  const pinchLast = useRef(1);

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

  const zoneForInspect = useRef('');
  useEffect(() => {
    if (zoneLabel) zoneForInspect.current = zoneLabel;
  }, [zoneLabel]);

  useEffect(() => {
    if (!entryComplete) return;
    const id = setInterval(() => {
      const sp = getCameraSpeed();
      if (sp < 0.5) {
        stillT.current += 0.2;
        if (stillT.current >= 2 && zoneForInspect.current) {
          setInspect({ title: zoneForInspect.current, body: zoneDetail || '—' });
        }
      } else {
        stillT.current = 0;
        setInspect(null);
      }
    }, 200);
    return () => clearInterval(id);
  }, [entryComplete, zoneDetail]);

  const joyResponder = useMemo(
    () =>
      PanResponder.create({
        onStartShouldSetPanResponder: () => entryComplete,
        onPanResponderGrant: () => {
          setJoyBright(true);
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
          if (prev > 0 && s > 0) multiplySpeedMult(s / prev);
          pinchLast.current = s;
          const m = getSpeedMult();
          setMultHud(`◈ ${m >= 7.5 ? '8' : m >= 4 ? '4' : m >= 2 ? '2' : m.toFixed(2)}x`);
          if (multHide.current) clearTimeout(multHide.current);
        })
        .onEnd(() => {
          multHide.current = setTimeout(() => setMultHud(''), 2000);
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

      <View
        style={[styles.lookZone, { width: lookW, height: sh }]}
        {...lookResponder.panHandlers}
      />

      <View style={styles.joystickWrap} {...joyResponder.panHandlers}>
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

      {multHud ? (
        <View style={styles.topCenter}>
          <Text style={styles.multText}>{multHud}</Text>
        </View>
      ) : null}

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

      {inspect ? (
        <View style={styles.inspect}>
          <Text style={styles.inspectTitle}>{inspect.title}</Text>
          <Text style={styles.inspectBody}>{inspect.body}</Text>
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
    right: 0,
    top: 0,
    backgroundColor: 'transparent',
  },
  joystickWrap: {
    position: 'absolute',
    left: 24,
    bottom: 24,
    width: JOYSTICK_SIZE,
    height: JOYSTICK_SIZE + 36,
    justifyContent: 'flex-end',
  },
  speedAbove: {
    position: 'absolute',
    top: 0,
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
    maxWidth: '48%',
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
  topCenter: {
    position: 'absolute',
    top: 40,
    alignSelf: 'center',
  },
  multText: {
    color: CYAN,
    fontSize: 12,
    fontFamily: 'monospace',
    opacity: 0.75,
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
  inspect: {
    position: 'absolute',
    bottom: 100,
    alignSelf: 'center',
    padding: 12,
    maxWidth: '85%',
    backgroundColor: 'rgba(0,0,0,0.4)',
    borderRadius: 8,
    borderWidth: 1,
    borderColor: 'rgba(0,255,229,0.2)',
  },
  inspectTitle: {
    color: '#FFB700',
    fontSize: 11,
    fontFamily: 'monospace',
    marginBottom: 4,
  },
  inspectBody: {
    color: 'rgba(0,255,229,0.65)',
    fontSize: 10,
    fontFamily: 'monospace',
  },
});
