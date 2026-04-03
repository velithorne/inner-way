import { useEffect, useRef } from 'react';
import { StyleSheet, View } from 'react-native';
import type { ExpoWebGLRenderingContext } from 'expo-gl';
import { GLView } from 'expo-gl';
import { Renderer } from 'expo-three';
import * as THREE from 'three';
import { Accelerometer, Barometer, Gyroscope, Magnetometer } from 'expo-sensors';

import { useWorldStore } from '../store/useWorldStore';
import { createBatterySun, updateBatterySun } from './batterySunBuild';
import { createNetworkWeather, updateNetworkWeather } from './networkWeatherBuild';
import { createProcessorCity, updateProcessorCity } from './processorCityBuild';
import { createRamOcean, ensureIslands, updateRamOcean } from './ramOceanBuild';
import { createSensorOutposts, updateSensorOutposts } from './sensorOutpostsBuild';
import { createStorageMountains, updateStorageMountains } from './storageMountainsBuild';
import { createAtmosphere, updateAtmosphere } from './worldAtmosphereBuild';
import { ENTRY_START_POS, INITIAL_CAMERA_POS, WORLD } from './worldConstants';
import { setWorldCameraPosition, setWorldCameraQuaternion } from './worldCameraBridge';
import { setHudBoundary, setHudFps, setHudForwardDir } from './worldHudBridge';
import {
  applyFovLerp,
  applyFreeCamera,
  applyWorldBoundary,
  processQueuedFly,
  setNavigationEnabled,
  stepFly,
  syncYawPitchFromCamera,
} from './worldNavigationBridge';

type Props = {
  entryProgress: number;
};

function entryAlpha(t: number, start: number, end: number): number {
  if (t >= 1) return 1;
  return THREE.MathUtils.smoothstep(t, start, end);
}

export function WorldEngine({ entryProgress }: Props) {
  const storeRef = useRef(useWorldStore.getState());
  const accRef = useRef({ x: 0, y: 0, z: 0 });
  const magRef = useRef({ x: 0, y: 0, z: 0 });
  const gyroRef = useRef({ x: 0, y: 0, z: 0 });
  const baroRef = useRef({ pressure: 1013, pressureDelta: 0 });
  const entryRef = useRef(entryProgress);
  const disposeRef = useRef<(() => void) | null>(null);
  const aliveRef = useRef(true);
  const baroSubRef = useRef<{ remove: () => void } | null>(null);

  useEffect(() => {
    entryRef.current = entryProgress;
  }, [entryProgress]);

  useEffect(() => {
    const unsub = useWorldStore.subscribe((s) => {
      storeRef.current = s;
    });
    Accelerometer.setUpdateInterval(16);
    Magnetometer.setUpdateInterval(32);
    Gyroscope.setUpdateInterval(16);
    let lastP = 1013;
    const aSub = Accelerometer.addListener((e) => {
      accRef.current = { x: e.x, y: e.y, z: e.z };
    });
    const mSub = Magnetometer.addListener((e) => {
      magRef.current = { x: e.x, y: e.y, z: e.z };
    });
    const gSub = Gyroscope.addListener((e) => {
      gyroRef.current = { x: e.x, y: e.y, z: e.z };
    });
    Barometer.isAvailableAsync().then((ok) => {
      if (!ok || !aliveRef.current) return;
      Barometer.setUpdateInterval(500);
      baroSubRef.current = Barometer.addListener((e) => {
        const d = Math.abs(e.pressure - lastP);
        baroRef.current = { pressure: e.pressure, pressureDelta: d };
        lastP = e.pressure;
      });
    });
    return () => {
      unsub();
      aSub.remove();
      mSub.remove();
      gSub.remove();
      baroSubRef.current?.remove();
      baroSubRef.current = null;
      aliveRef.current = false;
      disposeRef.current?.();
      disposeRef.current = null;
    };
  }, []);

  const onContextCreate = (gl: ExpoWebGLRenderingContext) => {
    const { drawingBufferWidth: w, drawingBufferHeight: h } = gl;
    const renderer = new Renderer({ gl }) as THREE.WebGLRenderer;
    renderer.setSize(w, h);
    renderer.setClearColor(0x000008, 1);
    renderer.shadowMap.enabled = true;
    renderer.shadowMap.type = THREE.PCFSoftShadowMap;

    const scene = new THREE.Scene();
    const fog = new THREE.FogExp2(0x050814, 0.0055);
    scene.fog = fog;

    const camera = new THREE.PerspectiveCamera(65, w / h, 0.1, 2500);
    camera.position.copy(ENTRY_START_POS);
    camera.lookAt(0, 0, 0);

    const ambient = new THREE.AmbientLight(0x223355, 1.5);
    scene.add(ambient);

    const fillLight = new THREE.PointLight(0x112244, 1.2, 800, 1.0);
    fillLight.position.set(0, 50, 200);
    scene.add(fillLight);

    const cpuLight = new THREE.PointLight(0xff6600, 0, 420);
    cpuLight.position.copy(WORLD.processor);
    cpuLight.position.y += 40;
    scene.add(cpuLight);

    const sunH = createBatterySun();
    scene.add(sunH.group);

    const procH = createProcessorCity();
    scene.add(procH.group);

    const ramH = createRamOcean();
    scene.add(ramH.group);

    const storH = createStorageMountains();
    scene.add(storH.group);

    const netH = createNetworkWeather();
    scene.add(netH.group);

    const sensH = createSensorOutposts();
    scene.add(sensH.group);

    const atmH = createAtmosphere();
    scene.add(atmH.dust, atmH.ground, atmH.viaDots);

    let lastAppsKey = '';
    let raf = 0;
    const start = performance.now();
    let entryDone = false;
    let lastFrame = performance.now();
    let fpsAcc = 0;
    let frameCount = 0;
    const fwdHud = new THREE.Vector3();

    const ease = (t: number) => t * t * (3 - 2 * t);

    const loop = () => {
      if (!aliveRef.current) return;
      raf = requestAnimationFrame(loop);
      const nowMs = performance.now();
      const dt = Math.min(0.05, (nowMs - lastFrame) / 1000);
      lastFrame = nowMs;
      const now = (nowMs - start) / 1000;
      const st = storeRef.current;
      const snap = st.snapshot;
      const wifi = st.wifiStrength;

      const batteryLevel = snap?.battery.level ?? st.batteryLevel;
      const powerW = snap?.battery.powerWatts ?? -1;
      const currentUa = snap?.battery.currentNow ?? -1;
      const vMv = snap?.battery.voltage ?? -1;
      const volts = vMv > 0 ? vMv / 1000 : 3.85;
      const estW =
        powerW >= 0 ? powerW : currentUa !== -1 ? volts * (Math.abs(currentUa) / 1_000_000) : 0;

      const entryT = ease(Math.max(0, Math.min(1, entryRef.current)));
      const aSun = entryAlpha(entryT, 0, 0.12);
      const aCity = entryAlpha(entryT, 0.08, 0.28);
      const aRam = entryAlpha(entryT, 0.15, 0.38);
      const aStor = entryAlpha(entryT, 0.22, 0.45);
      const aNet = entryAlpha(entryT, 0.28, 0.5);
      const aSens = entryAlpha(entryT, 0.32, 0.55);
      const aAtm = entryAlpha(entryT, 0.35, 0.6);

      sunH.group.visible = aSun > 0.02;
      procH.group.visible = aCity > 0.02;
      procH.group.scale.setScalar(Math.max(0.001, aCity));
      ramH.group.visible = aRam > 0.02;
      ramH.group.scale.setScalar(Math.max(0.001, aRam));
      storH.group.visible = aStor > 0.02;
      storH.group.scale.setScalar(Math.max(0.001, aStor));
      netH.group.visible = aNet > 0.02;
      netH.group.scale.setScalar(Math.max(0.001, aNet));
      sensH.group.visible = aSens > 0.02;
      sensH.group.scale.setScalar(Math.max(0.001, aSens));
      atmH.ground.visible = aAtm > 0.02;
      atmH.dust.visible = aAtm > 0.02;
      atmH.viaDots.visible = aAtm > 0.02;
      atmH.ground.scale.setScalar(Math.max(0.001, aAtm));
      atmH.dust.scale.setScalar(Math.max(0.001, aAtm));
      atmH.viaDots.scale.setScalar(Math.max(0.001, aAtm));

      updateBatterySun(sunH, batteryLevel, estW, now, aSun);

      const cpu = snap?.cpu ?? [];
      const avgCpu = cpu.length ? cpu.reduce((a, c) => a + c.usage, 0) / cpu.length : 0;
      updateProcessorCity(procH, cpu.length ? cpu : [{ usage: 0 }], now);

      if (avgCpu > 80) {
        cpuLight.intensity = ((avgCpu - 80) / 20) * 2.5 * aCity;
      } else {
        cpuLight.intensity = 0;
      }

      if (snap?.apps?.length) {
        const key = snap.apps.map((a) => `${a.packageName}:${a.memoryBytes}`).join('|');
        if (key !== lastAppsKey) {
          lastAppsKey = key;
          ensureIslands(ramH, snap.apps);
        }
      }
      const mem = snap?.memory;
      const ramPct = mem ? (mem.usedRam / mem.totalRam) * 100 : st.ramPressure;
      updateRamOcean(ramH, ramPct, mem?.lowMemory ?? false, now, camera, sunH.sunLight);

      const usedFrac = snap?.storage
        ? snap.storage.usedBytes / Math.max(1, snap.storage.totalBytes)
        : 0.5;
      const appDataFrac = snap?.storage?.totalBytes
        ? Math.min(1, (snap.storage.appDataBytes ?? 0) / snap.storage.totalBytes)
        : 0.2;
      updateStorageMountains(storH, usedFrac, appDataFrac, camera);

      const rx = snap?.network.rxBytesPerSecond ?? 0;
      const tx = snap?.network.txBytesPerSecond ?? 0;
      updateNetworkWeather(netH, rx, tx, now, camera, wifi);

      updateSensorOutposts(
        sensH,
        magRef.current,
        gyroRef.current,
        accRef.current,
        baroRef.current,
        camera,
      );

      updateAtmosphere(atmH, now, camera.position, batteryLevel);

      const fogD = 0.006 + (avgCpu / 100) * 0.0012 - (batteryLevel / 100) * 0.0008;
      fog.density = THREE.MathUtils.clamp(fogD, 0.004, 0.012);

      if (entryT < 1) {
        const camT = ease(entryT);
        camera.position.lerpVectors(ENTRY_START_POS, INITIAL_CAMERA_POS, camT);
        camera.lookAt(0, 0, 0);
        setNavigationEnabled(false);
        setHudBoundary('ok');
      } else {
        if (!entryDone) {
          camera.position.copy(INITIAL_CAMERA_POS);
          camera.lookAt(0, 0, 0);
          syncYawPitchFromCamera(camera);
          setNavigationEnabled(true);
          entryDone = true;
        }
        processQueuedFly(camera);
        stepFly(camera, dt);
        applyFreeCamera(camera, dt, { entryComplete: true });
        applyFovLerp(camera, dt);
        const b = applyWorldBoundary(camera);
        setHudBoundary(b.state);
      }

      camera.getWorldDirection(fwdHud);
      setHudForwardDir(fwdHud.x, fwdHud.y, fwdHud.z);

      setWorldCameraPosition(camera.position.x, camera.position.y, camera.position.z);
      setWorldCameraQuaternion(
        camera.quaternion.x,
        camera.quaternion.y,
        camera.quaternion.z,
        camera.quaternion.w,
      );

      renderer.render(scene, camera);
      gl.endFrameEXP();

      frameCount += 1;
      fpsAcc += dt;
      if (fpsAcc >= 0.5) {
        setHudFps(frameCount / fpsAcc, renderer.info.render.calls);
        frameCount = 0;
        fpsAcc = 0;
      }
    };
    loop();

    disposeRef.current = () => {
      cancelAnimationFrame(raf);
      sunH.dispose();
      procH.dispose();
      ramH.dispose();
      storH.dispose();
      netH.dispose();
      sensH.dispose();
      atmH.dispose();
      renderer.dispose();
    };
  };

  return (
    <View style={styles.fill}>
      <GLView style={styles.gl} onContextCreate={onContextCreate} />
    </View>
  );
}

const styles = StyleSheet.create({
  fill: { flex: 1, backgroundColor: '#000008' },
  gl: { flex: 1 },
});
