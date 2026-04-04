import { useCallback, useEffect, useRef } from 'react';
import { StyleSheet, View } from 'react-native';
import { GLView } from 'expo-gl';
import type { ExpoWebGLRenderingContext } from 'expo-gl';
import { Renderer } from 'expo-three';
import * as THREE from 'three';
import type { WifiNetwork } from '../types/wifi';
import { WaveEmitter } from './WaveEmitter';

const MAX_NETWORKS = 20;

type Props = {
  networks: WifiNetwork[];
  onFps?: (fps: number) => void;
};

function nowMs(): number {
  const p = globalThis.performance;
  return typeof p?.now === 'function' ? p.now() : Date.now();
}

function topNetworks(nets: WifiNetwork[]): WifiNetwork[] {
  return [...nets].sort((a, b) => b.rssi - a.rssi).slice(0, MAX_NETWORKS);
}

export function WaveScene({ networks, onFps }: Props) {
  const rafRef = useRef<number | null>(null);
  const mountedRef = useRef(true);
  const sceneRef = useRef<THREE.Scene | null>(null);
  const emittersRef = useRef<Map<string, WaveEmitter>>(new Map());
  const networksRef = useRef(networks);

  networksRef.current = networks;

  const syncEmitters = useCallback((scene: THREE.Scene) => {
    const list = topNetworks(networksRef.current);
    const want = new Set(list.map((n) => n.bssid));
    const map = emittersRef.current;

    for (const [id, em] of map) {
      if (!want.has(id)) {
        scene.remove(em.group);
        em.dispose();
        map.delete(id);
      }
    }

    for (const n of list) {
      let em = map.get(n.bssid);
      if (!em) {
        em = new WaveEmitter(n);
        map.set(n.bssid, em);
        scene.add(em.group);
      } else {
        em.syncNetwork(n);
      }
    }
  }, []);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
      if (rafRef.current != null) {
        cancelAnimationFrame(rafRef.current);
        rafRef.current = null;
      }
      const scene = sceneRef.current;
      if (scene) {
        for (const em of emittersRef.current.values()) {
          scene.remove(em.group);
          em.dispose();
        }
        emittersRef.current.clear();
      }
    };
  }, []);

  useEffect(() => {
    const scene = sceneRef.current;
    if (scene) {
      syncEmitters(scene);
    }
  }, [networks, syncEmitters]);

  const onContextCreate = useCallback(
    async (gl: ExpoWebGLRenderingContext) => {
      const { drawingBufferWidth: width, drawingBufferHeight: height } = gl;

      const scene = new THREE.Scene();
      sceneRef.current = scene;

      const camera = new THREE.PerspectiveCamera(58, width / Math.max(height, 1), 0.1, 120);
      camera.position.set(0, 0, 0);
      camera.lookAt(0, 0, -1);

      const renderer = new Renderer({
        gl,
        alpha: true,
        clearColor: 0x000000,
      });
      renderer.setSize(width, height);
      renderer.setClearColor(0x000000, 0);
      renderer.autoClear = true;

      syncEmitters(scene);

      let frames = 0;
      let lastTick = nowMs();
      const t0 = nowMs();

      const loop = () => {
        if (!mountedRef.current) return;
        rafRef.current = requestAnimationFrame(loop);

        const elapsedSec = (nowMs() - t0) / 1000;
        for (const em of emittersRef.current.values()) {
          em.update(elapsedSec);
        }

        renderer.render(scene, camera);
        gl.endFrameEXP();

        frames++;
        const wall = nowMs();
        const dt = wall - lastTick;
        if (dt >= 500) {
          onFps?.((frames / dt) * 1000);
          frames = 0;
          lastTick = wall;
        }
      };
      rafRef.current = requestAnimationFrame(loop);
    },
    [onFps, syncEmitters]
  );

  return (
    <View style={styles.wrap} pointerEvents="none">
      <GLView style={styles.gl} onContextCreate={onContextCreate} />
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'transparent',
  },
  gl: {
    flex: 1,
    backgroundColor: 'transparent',
  },
});
