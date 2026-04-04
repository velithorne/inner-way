import { useCallback, useEffect, useRef } from 'react';
import { StyleSheet, View } from 'react-native';
import { GLView } from 'expo-gl';
import type { ExpoWebGLRenderingContext } from 'expo-gl';
import { Renderer } from 'expo-three';
import * as THREE from 'three';
import type { RouterEstimate } from '../services/routerTriangulator';
import type { WifiNetwork } from '../types/wifi';
import { estimateToWorldPosition } from './space';
import { SignalLineStream } from './SignalLineStream';
import { WaveEmitter } from './WaveEmitter';

const MAX_NETWORKS = 12;
const MAX_LINE_STREAMS = 7;
const CONFLICT_PARTICLES = 24;

type Props = {
  networks: WifiNetwork[];
  estimates: Map<string, RouterEstimate>;
  highlightBssid: string | null;
  conflictMidpoints: THREE.Vector3[];
  onFps?: (fps: number) => void;
};

function nowMs(): number {
  const p = globalThis.performance;
  return typeof p?.now === 'function' ? p.now() : Date.now();
}

function topNetworks(nets: WifiNetwork[]): WifiNetwork[] {
  return [...nets].sort((a, b) => b.rssi - a.rssi).slice(0, MAX_NETWORKS);
}

function topNetworksForLines(nets: WifiNetwork[]): WifiNetwork[] {
  return [...nets].sort((a, b) => b.rssi - a.rssi).slice(0, MAX_LINE_STREAMS);
}

function makeConflictCloud(mid: THREE.Vector3, color: THREE.Color): THREE.Points {
  const positions = new Float32Array(CONFLICT_PARTICLES * 3);
  for (let i = 0; i < CONFLICT_PARTICLES; i++) {
    positions[i * 3] = mid.x + (Math.random() - 0.5) * 0.4;
    positions[i * 3 + 1] = mid.y + (Math.random() - 0.5) * 0.4;
    positions[i * 3 + 2] = mid.z + (Math.random() - 0.5) * 0.4;
  }
  const geo = new THREE.BufferGeometry();
  geo.setAttribute('position', new THREE.BufferAttribute(positions, 3));
  const mat = new THREE.PointsMaterial({
    color,
    size: 0.7,
    transparent: true,
    opacity: 0.28,
    depthWrite: false,
    blending: THREE.AdditiveBlending,
  });
  return new THREE.Points(geo, mat);
}

export function WaveScene({
  networks,
  estimates,
  highlightBssid,
  conflictMidpoints,
  onFps,
}: Props) {
  const rafRef = useRef<number | null>(null);
  const mountedRef = useRef(true);
  const sceneRef = useRef<THREE.Scene | null>(null);
  const emittersRef = useRef<Map<string, WaveEmitter>>(new Map());
  const lineStreamsRef = useRef<Map<string, SignalLineStream>>(new Map());
  const conflictRefs = useRef<THREE.Points[]>([]);
  const networksRef = useRef(networks);
  const estimatesRef = useRef(estimates);
  const highlightRef = useRef(highlightBssid);
  const conflictsRef = useRef(conflictMidpoints);

  networksRef.current = networks;
  estimatesRef.current = estimates;
  highlightRef.current = highlightBssid;
  conflictsRef.current = conflictMidpoints;

  const syncEmitters = useCallback((scene: THREE.Scene) => {
    const list = topNetworks(networksRef.current);
    const want = new Set(list.map((n) => n.bssid));
    const map = emittersRef.current;
    const estMap = estimatesRef.current;

    for (const [id, em] of map) {
      if (!want.has(id)) {
        scene.remove(em.group);
        em.dispose();
        map.delete(id);
      }
    }

    for (const n of list) {
      const est = estMap.get(n.bssid) ?? {
        bearing: 0,
        distance: 1,
        confidence: 10,
      };
      const pos = estimateToWorldPosition(est, n.frequency);
      const bearingRad = (est.bearing * Math.PI) / 180;

      let em = map.get(n.bssid);
      if (!em) {
        em = new WaveEmitter(n, {
          position: pos,
          bearingRad,
          confidence: est.confidence,
        });
        map.set(n.bssid, em);
        scene.add(em.group);
      } else {
        em.syncNetwork(n);
        em.setConfidence(est.confidence);
        em.setTargetPosition(pos);
        em.group.rotation.y = bearingRad;
      }
      em.setHighlight(highlightRef.current === n.bssid);
    }
  }, []);

  const syncLineStreams = useCallback((scene: THREE.Scene) => {
    const list = topNetworksForLines(networksRef.current);
    const want = new Set(list.map((n) => n.bssid));
    const map = lineStreamsRef.current;
    const estMap = estimatesRef.current;

    for (const [id, stream] of map) {
      if (!want.has(id)) {
        scene.remove(stream.group);
        stream.dispose();
        map.delete(id);
      }
    }

    for (const n of list) {
      const est = estMap.get(n.bssid) ?? {
        bearing: 0,
        distance: 1,
        confidence: 10,
      };
      let stream = map.get(n.bssid);
      if (!stream) {
        stream = new SignalLineStream(n, est);
        map.set(n.bssid, stream);
        scene.add(stream.group);
      } else {
        stream.syncNetwork(n, est);
      }
    }
  }, []);

  const syncConflictParticles = useCallback((scene: THREE.Scene) => {
    for (const p of conflictRefs.current) {
      scene.remove(p);
      p.geometry.dispose();
      (p.material as THREE.PointsMaterial).dispose();
    }
    conflictRefs.current = [];
    const mids = conflictsRef.current;
    for (const mid of mids) {
      const c = new THREE.Color(0xffaa66).lerp(new THREE.Color(0xaa66ff), 0.35);
      const cloud = makeConflictCloud(mid, c);
      scene.add(cloud);
      conflictRefs.current.push(cloud);
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
        for (const ls of lineStreamsRef.current.values()) {
          scene.remove(ls.group);
          ls.dispose();
        }
        lineStreamsRef.current.clear();
        for (const p of conflictRefs.current) {
          scene.remove(p);
          p.geometry.dispose();
          (p.material as THREE.PointsMaterial).dispose();
        }
        conflictRefs.current = [];
      }
    };
  }, []);

  useEffect(() => {
    const scene = sceneRef.current;
    if (scene) {
      syncEmitters(scene);
      syncLineStreams(scene);
    }
  }, [networks, estimates, highlightBssid, syncEmitters, syncLineStreams]);

  useEffect(() => {
    const scene = sceneRef.current;
    if (scene) {
      syncConflictParticles(scene);
    }
  }, [conflictMidpoints, syncConflictParticles]);

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
      syncLineStreams(scene);
      syncConflictParticles(scene);

      let frames = 0;
      let lastTick = nowMs();
      let lastFrame = nowMs();

      const loop = () => {
        if (!mountedRef.current) return;
        rafRef.current = requestAnimationFrame(loop);
        const wall = nowMs();
        const deltaSec = Math.min(0.1, (wall - lastFrame) / 1000);
        lastFrame = wall;

        for (const ls of lineStreamsRef.current.values()) {
          ls.update(deltaSec);
        }

        for (const em of emittersRef.current.values()) {
          em.update(wall / 1000, deltaSec);
        }

        renderer.render(scene, camera);
        gl.endFrameEXP();

        frames++;
        const dt = wall - lastTick;
        if (dt >= 500) {
          onFps?.((frames / dt) * 1000);
          frames = 0;
          lastTick = wall;
        }
      };
      rafRef.current = requestAnimationFrame(loop);
    },
    [onFps, syncEmitters, syncLineStreams, syncConflictParticles]
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
