import { useCallback, useEffect, useRef } from 'react';
import { StyleSheet, View } from 'react-native';
import { GLView } from 'expo-gl';
import type { ExpoWebGLRenderingContext } from 'expo-gl';
import { Renderer } from 'expo-three';
import * as THREE from 'three';
import { networkColour } from '../services/colourFromBssid';
import type { CityBuilding } from './cityLayout';

export type ScreenLabel = { bssid: string; x: number; y: number; visible: boolean };

type Props = {
  buildings: CityBuilding[];
  cameraPos: THREE.Vector3;
  cameraTarget: THREE.Vector3;
  onFps?: (fps: number) => void;
  onScreenLabels?: (labels: ScreenLabel[]) => void;
};

function nowMs(): number {
  const p = globalThis.performance;
  return typeof p?.now === 'function' ? p.now() : Date.now();
}

export function CityScene({
  buildings,
  cameraPos,
  cameraTarget,
  onFps,
  onScreenLabels,
}: Props) {
  const rafRef = useRef<number | null>(null);
  const mountedRef = useRef(true);
  const buildingsRef = useRef(buildings);
  const camPosRef = useRef(cameraPos.clone());
  const camTgtRef = useRef(cameraTarget.clone());
  const sceneRef = useRef<THREE.Scene | null>(null);
  const cameraRef = useRef<THREE.PerspectiveCamera | null>(null);
  const buildingMeshesRef = useRef<THREE.Mesh[]>([]);
  const edgeLinesRef = useRef<THREE.LineSegments[]>([]);
  const bridgeLinesRef = useRef<THREE.Line[]>([]);
  const solarRef = useRef<THREE.Mesh[]>([]);
  const sizeRef = useRef({ w: 1, h: 1 });

  buildingsRef.current = buildings;
  camPosRef.current.copy(cameraPos);
  camTgtRef.current.copy(cameraTarget);

  const clearBuildings = useCallback((scene: THREE.Scene) => {
    for (const m of buildingMeshesRef.current) {
      scene.remove(m);
      m.geometry.dispose();
      (m.material as THREE.MeshStandardMaterial).dispose();
    }
    buildingMeshesRef.current = [];
    for (const e of edgeLinesRef.current) {
      scene.remove(e);
      e.geometry.dispose();
      (e.material as THREE.LineBasicMaterial).dispose();
    }
    edgeLinesRef.current = [];
    for (const s of solarRef.current) {
      scene.remove(s);
      s.geometry.dispose();
      (s.material as THREE.MeshStandardMaterial).dispose();
    }
    solarRef.current = [];
    for (const l of bridgeLinesRef.current) {
      scene.remove(l);
      l.geometry.dispose();
      (l.material as THREE.LineBasicMaterial).dispose();
    }
    bridgeLinesRef.current = [];
  }, []);

  const rebuildScene = useCallback(
    (scene: THREE.Scene) => {
      clearBuildings(scene);
      const list = buildingsRef.current;

      for (const b of list) {
        if (b.kind === 'solar') {
          const geo = new THREE.CylinderGeometry(4, 4, b.height, 6);
          const mat = new THREE.MeshStandardMaterial({
            color: new THREE.Color(0xffb700),
            emissive: new THREE.Color(0xffb700),
            emissiveIntensity: 0.45,
            roughness: 0.45,
            metalness: 0.35,
          });
          const mesh = new THREE.Mesh(geo, mat);
          mesh.position.copy(b.position);
          scene.add(mesh);
          solarRef.current.push(mesh);
          continue;
        }

        const geo = new THREE.BoxGeometry(b.width, b.height, b.depth);
        const nc = networkColour(b.bssid, b.frequency);
        const is5 = b.frequency > 4000;
        const mat = new THREE.MeshStandardMaterial({
          color: is5 ? new THREE.Color(0x002244) : new THREE.Color(0x6b3300),
          emissive: nc.clone(),
          emissiveIntensity: is5 ? 0.4 : 0.3,
          roughness: is5 ? 0.1 : 0.7,
          metalness: is5 ? 0.9 : 0.2,
          transparent: is5,
          opacity: is5 ? 0.88 : 1,
        });
        const mesh = new THREE.Mesh(geo, mat);
        mesh.position.copy(b.position);
        scene.add(mesh);
        buildingMeshesRef.current.push(mesh);

        const edges = new THREE.EdgesGeometry(geo);
        const line = new THREE.LineSegments(
          edges,
          new THREE.LineBasicMaterial({
            color: 0x00ffe5,
            transparent: true,
            opacity: 0.15,
          })
        );
        line.position.copy(b.position);
        scene.add(line);
        edgeLinesRef.current.push(line);
      }

      const pairDone = new Set<string>();
      for (const b of list) {
        if (!b.dualPartnerBssid) continue;
        const key = [b.bssid, b.dualPartnerBssid].sort().join('|');
        if (pairDone.has(key)) continue;
        pairDone.add(key);
        const other = list.find((x) => x.bssid === b.dualPartnerBssid);
        if (!other) continue;
        const y = Math.min(b.height, other.height) * 0.45;
        const p0 = new THREE.Vector3(b.position.x, y, b.position.z);
        const p1 = new THREE.Vector3(other.position.x, y, other.position.z);
        const g = new THREE.BufferGeometry().setFromPoints([p0, p1]);
        const br = new THREE.Line(
          g,
          new THREE.LineBasicMaterial({ color: 0x00ffe5, transparent: true, opacity: 0.4 })
        );
        scene.add(br);
        bridgeLinesRef.current.push(br);
      }
    },
    [clearBuildings]
  );

  useEffect(() => {
    const scene = sceneRef.current;
    if (scene) rebuildScene(scene);
  }, [buildings, rebuildScene]);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
      if (rafRef.current != null) cancelAnimationFrame(rafRef.current);
    };
  }, []);

  const onContextCreate = useCallback(
    async (gl: ExpoWebGLRenderingContext) => {
      const w = gl.drawingBufferWidth;
      const h = gl.drawingBufferHeight;
      sizeRef.current = { w, h };

      const scene = new THREE.Scene();
      scene.background = new THREE.Color(0x080d18);
      sceneRef.current = scene;

      const ambient = new THREE.AmbientLight(0x334455, 2.0);
      scene.add(ambient);
      const mainLight = new THREE.PointLight(0x4488ff, 3.0, 600, 1.2);
      mainLight.position.set(0, 100, 0);
      scene.add(mainLight);
      const fillLight = new THREE.PointLight(0x224433, 1.5, 400);
      fillLight.position.set(0, -20, 0);
      scene.add(fillLight);

      const ground = new THREE.Mesh(
        new THREE.PlaneGeometry(600, 600),
        new THREE.MeshStandardMaterial({
          color: 0x080d18,
          roughness: 0.95,
          metalness: 0.05,
        })
      );
      ground.rotation.x = -Math.PI / 2;
      scene.add(ground);
      const grid = new THREE.GridHelper(600, 60, 0x001133, 0x000d22);
      grid.position.y = 0.1;
      scene.add(grid);

      const camera = new THREE.PerspectiveCamera(55, w / Math.max(h, 1), 0.5, 500);
      camera.position.copy(camPosRef.current);
      camera.lookAt(camTgtRef.current);
      cameraRef.current = camera;

      const renderer = new Renderer({ gl });
      renderer.setSize(w, h);
      renderer.setClearColor(0x080d18, 1);

      rebuildScene(scene);

      let frames = 0;
      let lastTick = nowMs();
      let lastFrame = nowMs();

      const loop = () => {
        if (!mountedRef.current) return;
        rafRef.current = requestAnimationFrame(loop);
        const wall = nowMs();
        const deltaSec = Math.min(0.05, (wall - lastFrame) / 1000);
        lastFrame = wall;

        const cam = cameraRef.current!;
        cam.position.lerp(camPosRef.current, Math.min(1, deltaSec * 2.5));
        cam.lookAt(camTgtRef.current);
        cam.updateMatrixWorld(true);

        let ri = 0;
        let si = 0;
        for (const b of buildingsRef.current) {
          if (b.kind === 'solar') {
            const mesh = solarRef.current[si++];
            if (mesh) {
              const target = ((b.rssi + 100) / 70) * 0.55;
              const mat = mesh.material as THREE.MeshStandardMaterial;
              mat.emissiveIntensity = THREE.MathUtils.lerp(mat.emissiveIntensity, target, 0.08);
            }
          } else {
            const mesh = buildingMeshesRef.current[ri];
            const line = edgeLinesRef.current[ri];
            ri++;
            if (mesh) {
              const target = ((b.rssi + 100) / 70) * 0.6;
              const mat = mesh.material as THREE.MeshStandardMaterial;
              mat.emissiveIntensity = THREE.MathUtils.lerp(mat.emissiveIntensity, target, 0.12);
              if (line) line.position.copy(mesh.position);
            }
          }
        }

        renderer.render(scene, cam);
        gl.endFrameEXP();

        if (onScreenLabels && cameraRef.current) {
          const labels: ScreenLabel[] = [];
          for (const b of buildingsRef.current) {
            const top = b.position.clone();
            top.y += b.height / 2 + 2.5;
            const projected = top.clone();
            projected.project(cam);
            const near =
              projected.z > -1 &&
              projected.z < 1 &&
              cam.position.distanceTo(b.position) < 60;
            const sx = (projected.x * 0.5 + 0.5) * sizeRef.current.w;
            const sy = (-projected.y * 0.5 + 0.5) * sizeRef.current.h;
            labels.push({ bssid: b.bssid, x: sx, y: sy, visible: near });
          }
          onScreenLabels(labels);
        }

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
    [onFps, onScreenLabels, rebuildScene]
  );

  return (
    <View style={styles.wrap}>
      <GLView style={styles.gl} onContextCreate={onContextCreate} />
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { flex: 1, backgroundColor: '#05080c' },
  gl: { flex: 1 },
});
