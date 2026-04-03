import { useEffect, useRef } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import type { ExpoWebGLRenderingContext } from 'expo-gl';
import { GLView } from 'expo-gl';
import { Renderer, THREE } from 'expo-three';

import { useWorldStore } from '../store/useWorldStore';

/**
 * Phase 2 preview: battery charge as a simple emissive sphere (the "sun").
 * Driven by live `batteryLevel` from the native poller (0–100).
 */
export function BatterySunScene() {
  const batteryLevel = useWorldStore((s) => s.batteryLevel);
  const telemetryOk = useWorldStore((s) => s.telemetryState === 'ok');
  const levelRef = useRef(batteryLevel);
  const alive = useRef(true);
  const disposeGl = useRef<(() => void) | null>(null);

  useEffect(() => {
    alive.current = true;
    return () => {
      alive.current = false;
      disposeGl.current?.();
      disposeGl.current = null;
    };
  }, []);

  useEffect(() => {
    levelRef.current = batteryLevel;
  }, [batteryLevel]);

  const onContextCreate = (gl: ExpoWebGLRenderingContext) => {
    const { drawingBufferWidth: width, drawingBufferHeight: height } = gl;

    const renderer = new Renderer({ gl }) as InstanceType<typeof THREE.WebGLRenderer>;
    renderer.setSize(width, height);
    renderer.setClearColor(0x0a0e14, 1);

    const scene = new THREE.Scene();
    const camera = new THREE.PerspectiveCamera(50, width / height, 0.1, 100);
    camera.position.z = 4;

    const geometry = new THREE.SphereGeometry(1, 48, 48);
    const material = new THREE.MeshStandardMaterial({
      color: 0xffb300,
      emissive: 0xffa000,
      emissiveIntensity: 1.2,
      metalness: 0.2,
      roughness: 0.35,
    });
    const sun = new THREE.Mesh(geometry, material);
    scene.add(sun);

    const ambient = new THREE.AmbientLight(0x404040, 0.4);
    scene.add(ambient);
    const key = new THREE.DirectionalLight(0xffffff, 0.9);
    key.position.set(2, 2, 4);
    scene.add(key);

    let raf = 0;
    const render = () => {
      if (!alive.current) return;
      raf = requestAnimationFrame(render);
      const pct = Math.max(0, Math.min(100, levelRef.current)) / 100;
      const scale = 0.55 + pct * 0.65;
      sun.scale.setScalar(scale);

      const r = 0.35 + 0.65 * pct;
      const gCol = 0.2 + 0.55 * pct;
      const bCol = 0.05 + 0.2 * pct;
      material.emissive.setRGB(r, gCol, bCol);
      material.emissiveIntensity = 0.7 + pct * 0.9;

      renderer.render(scene, camera);
      gl.endFrameEXP();
    };
    render();

    disposeGl.current = () => {
      cancelAnimationFrame(raf);
      geometry.dispose();
      material.dispose();
      renderer.dispose();
    };
  };

  return (
    <View style={styles.wrap}>
      <Text style={styles.label}>Phase 2 preview — Battery sun</Text>
      {!telemetryOk ? (
        <Text style={styles.hint}>Waiting for telemetry… preview uses 0% until data arrives.</Text>
      ) : null}
      <GLView style={styles.gl} onContextCreate={onContextCreate} />
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: {
    marginTop: 24,
    alignSelf: 'stretch',
  },
  label: {
    color: '#00bcd4',
    fontSize: 13,
    fontWeight: '600',
    marginBottom: 8,
  },
  hint: {
    color: '#78909c',
    fontSize: 11,
    marginBottom: 8,
  },
  gl: {
    height: 220,
    borderRadius: 12,
    overflow: 'hidden',
    backgroundColor: '#05070a',
  },
});
