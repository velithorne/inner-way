import { useEffect, useRef } from 'react';
import { StyleSheet, View } from 'react-native';
import type { ExpoWebGLRenderingContext } from 'expo-gl';
import { GLView } from 'expo-gl';
import { Renderer } from 'expo-three';
import * as THREE from 'three';
import { Accelerometer, Gyroscope, Magnetometer } from 'expo-sensors';

import { FlightController } from '../navigation/FlightController';
import { useWorldStore } from '../store/useWorldStore';
import { createBatterySun, updateBatterySun } from './batterySunBuild';
import { createNetworkWeather, updateNetworkWeather } from './networkWeatherBuild';
import { createProcessorCity, updateProcessorCity } from './processorCityBuild';
import { createRamOcean, ensureIslands, updateRamOcean } from './ramOceanBuild';
import { createSensorOutposts, updateSensorOutposts } from './sensorOutpostsBuild';
import { createStorageMountains, updateStorageMountains } from './storageMountainsBuild';
import { ENTRY_START_POS, INITIAL_CAMERA_POS, ORBITAL_CAMERA_POS } from './worldConstants';
import { setWorldCameraPosition } from './worldCameraBridge';

type Props = {
  entryProgress: number;
  orbital: boolean;
};

export function WorldEngine({ entryProgress, orbital }: Props) {
  const storeRef = useRef(useWorldStore.getState());
  const flightRef = useRef(new FlightController());
  const gyroRef = useRef({ x: 0, y: 0, z: 0 });
  const accRef = useRef({ x: 0, y: 0, z: 0 });
  const magRef = useRef({ x: 0, y: 0, z: 0 });
  const entryRef = useRef(entryProgress);
  const orbitalRef = useRef(orbital);
  const disposeRef = useRef<(() => void) | null>(null);
  const aliveRef = useRef(true);

  useEffect(() => {
    entryRef.current = entryProgress;
  }, [entryProgress]);
  useEffect(() => {
    orbitalRef.current = orbital;
  }, [orbital]);

  useEffect(() => {
    const unsub = useWorldStore.subscribe((s) => {
      storeRef.current = s;
    });
    Gyroscope.setUpdateInterval(16);
    Accelerometer.setUpdateInterval(16);
    Magnetometer.setUpdateInterval(32);
    const gSub = Gyroscope.addListener((e) => {
      gyroRef.current = { x: e.x, y: e.y, z: e.z };
    });
    const aSub = Accelerometer.addListener((e) => {
      accRef.current = { x: e.x, y: e.y, z: e.z };
    });
    const mSub = Magnetometer.addListener((e) => {
      magRef.current = { x: e.x, y: e.y, z: e.z };
    });
    return () => {
      unsub();
      gSub.remove();
      aSub.remove();
      mSub.remove();
      aliveRef.current = false;
      disposeRef.current?.();
      disposeRef.current = null;
    };
  }, []);

  useEffect(() => {
    flightRef.current.setOrbital(orbital);
  }, [orbital]);

  const onContextCreate = (gl: ExpoWebGLRenderingContext) => {
    const { drawingBufferWidth: w, drawingBufferHeight: h } = gl;
    const renderer = new Renderer({ gl }) as THREE.WebGLRenderer;
    renderer.setSize(w, h);
    renderer.setClearColor(0x000008, 1);

    const scene = new THREE.Scene();
    scene.fog = new THREE.FogExp2(0x000008, 0.008);

    const camera = new THREE.PerspectiveCamera(65, w / h, 0.1, 2500);
    camera.position.copy(ENTRY_START_POS);
    camera.lookAt(0, 0, 0);

    const ambient = new THREE.AmbientLight(0x112233, 0.3);
    scene.add(ambient);

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

    let lastAppsKey = '';
    let raf = 0;
    const start = performance.now();
    let entryDone = false;

    const ease = (t: number) => t * t * (3 - 2 * t);

    const loop = () => {
      if (!aliveRef.current) return;
      raf = requestAnimationFrame(loop);
      const now = (performance.now() - start) / 1000;
      const st = storeRef.current;
      const snap = st.snapshot;

      const batteryLevel = snap?.battery.level ?? st.batteryLevel;
      const powerW = snap?.battery.powerWatts ?? -1;
      const currentUa = snap?.battery.currentNow ?? -1;
      const vMv = snap?.battery.voltage ?? -1;
      const volts = vMv > 0 ? vMv / 1000 : 3.85;
      const estW =
        powerW >= 0 ? powerW : currentUa !== -1 ? volts * (Math.abs(currentUa) / 1_000_000) : 0;

      updateBatterySun(sunH, batteryLevel, estW, now);

      const cpu = snap?.cpu ?? [];
      updateProcessorCity(procH, cpu.length ? cpu : [{ usage: 0 }], now);

      if (snap?.apps?.length) {
        const key = snap.apps.map((a) => `${a.packageName}:${a.memoryBytes}`).join('|');
        if (key !== lastAppsKey) {
          lastAppsKey = key;
          ensureIslands(ramH, snap.apps);
        }
      }
      const mem = snap?.memory;
      const ramPct = mem ? (mem.usedRam / mem.totalRam) * 100 : st.ramPressure;
      updateRamOcean(ramH, ramPct, mem?.lowMemory ?? false, now);

      const usedFrac = snap?.storage
        ? snap.storage.usedBytes / Math.max(1, snap.storage.totalBytes)
        : 0.5;
      updateStorageMountains(storH, usedFrac);

      const rx = snap?.network.rxBytesPerSecond ?? 0;
      const tx = snap?.network.txBytesPerSecond ?? 0;
      updateNetworkWeather(netH, rx, tx, now);

      updateSensorOutposts(sensH, magRef.current, gyroRef.current, accRef.current);

      const dt = 1 / 60;
      const entryT = ease(Math.max(0, Math.min(1, entryRef.current)));
      const orb = orbitalRef.current;

      if (orb) {
        flightRef.current.setOrbital(true);
        camera.position.lerp(ORBITAL_CAMERA_POS, 0.08);
        camera.lookAt(0, 0, 0);
      } else {
        flightRef.current.setOrbital(false);
        if (entryT < 1) {
          camera.position.lerpVectors(ENTRY_START_POS, INITIAL_CAMERA_POS, entryT);
          camera.lookAt(0, 0, 0);
        } else {
          if (!entryDone) {
            camera.position.copy(INITIAL_CAMERA_POS);
            entryDone = true;
          }
          flightRef.current.update(gyroRef.current, dt);
          flightRef.current.applyToCamera(camera, dt, ORBITAL_CAMERA_POS.clone());
        }
      }

      setWorldCameraPosition(camera.position.x, camera.position.y, camera.position.z);

      renderer.render(scene, camera);
      gl.endFrameEXP();
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
