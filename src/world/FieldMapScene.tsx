import { useCallback, useEffect, useRef } from 'react';
import { StyleSheet, View } from 'react-native';
import { GLView } from 'expo-gl';
import type { ExpoWebGLRenderingContext } from 'expo-gl';
import { Renderer } from 'expo-three';
import * as THREE from 'three';
import type { DeadZone, VoxelAgg } from './fieldMapHeatmap';
import { sceneCentre, energyToColor, voxelHeightForEnergy } from './fieldMapHeatmap';

type Props = {
  voxels: VoxelAgg[];
  deadZones: DeadZone[];
  /** Plan vs 3D oblique view */
  view3d: boolean;
  /** New voxel keys flash when map updates (live recording) */
  liveMode: boolean;
};

function nowMs(): number {
  const p = globalThis.performance;
  return typeof p?.now === 'function' ? p.now() : Date.now();
}

export function FieldMapScene({ voxels, deadZones, view3d, liveMode }: Props) {
  const rafRef = useRef<number | null>(null);
  const mountedRef = useRef(true);
  const sceneRef = useRef<THREE.Scene | null>(null);
  const cameraRef = useRef<THREE.PerspectiveCamera | null>(null);
  const rendererRef = useRef<Renderer | null>(null);
  const glRef = useRef<ExpoWebGLRenderingContext | null>(null);

  const meshRef = useRef<THREE.InstancedMesh | null>(null);
  const deadGroupRef = useRef<THREE.Group | null>(null);
  const deadMatRef = useRef<THREE.LineBasicMaterial | null>(null);
  const startMarkerRef = useRef<THREE.Mesh | null>(null);
  const groundRef = useRef<THREE.Mesh | null>(null);
  const gridRef = useRef<THREE.GridHelper | null>(null);

  const voxelKeysRef = useRef<string[]>([]);
  const flashStartRef = useRef<Map<string, number>>(new Map());
  const prevVoxelKeySetRef = useRef<Set<string>>(new Set());
  const baseColorsRef = useRef<Float32Array | null>(null);

  const propsRef = useRef({ voxels, deadZones, view3d, liveMode });
  propsRef.current = { voxels, deadZones, view3d, liveMode };

  const dummyRef = useRef(new THREE.Object3D());

  const disposeSceneContent = useCallback((scene: THREE.Scene) => {
    if (meshRef.current) {
      scene.remove(meshRef.current);
      meshRef.current.geometry.dispose();
      (meshRef.current.material as THREE.MeshLambertMaterial).dispose();
      meshRef.current = null;
    }
    if (deadGroupRef.current) {
      scene.remove(deadGroupRef.current);
      for (const c of deadGroupRef.current.children) {
        if (c instanceof THREE.LineSegments) c.geometry.dispose();
      }
      deadMatRef.current?.dispose();
      deadMatRef.current = null;
      deadGroupRef.current = null;
    }
    if (startMarkerRef.current) {
      scene.remove(startMarkerRef.current);
      startMarkerRef.current.geometry.dispose();
      (startMarkerRef.current.material as THREE.MeshBasicMaterial).dispose();
      startMarkerRef.current = null;
    }
    if (groundRef.current) {
      scene.remove(groundRef.current);
      groundRef.current.geometry.dispose();
      (groundRef.current.material as THREE.MeshBasicMaterial).dispose();
      groundRef.current = null;
    }
    if (gridRef.current) {
      scene.remove(gridRef.current);
      gridRef.current.dispose();
      gridRef.current = null;
    }
  }, []);

  const rebuildFromState = useCallback(
    (scene: THREE.Scene, camera: THREE.PerspectiveCamera) => {
      const { voxels: vox, deadZones: dz, view3d: v3d } = propsRef.current;

      disposeSceneContent(scene);

      const { cx: centreX, cz: centreZ } = sceneCentre(vox);
      if (vox.length === 0) {
        voxelKeysRef.current = [];
        baseColorsRef.current = null;
        flashStartRef.current.clear();
        prevVoxelKeySetRef.current.clear();
        camera.position.set(0, 80, 0.1);
        camera.lookAt(0, 0, 0);
        const ground = new THREE.Mesh(
          new THREE.PlaneGeometry(200, 200),
          new THREE.MeshBasicMaterial({ color: 0x080d18 })
        );
        ground.rotation.x = -Math.PI / 2;
        groundRef.current = ground;
        scene.add(ground);
        const grid = new THREE.GridHelper(200, 100, 0x001133, 0x000d22);
        gridRef.current = grid;
        scene.add(grid);
        return;
      }

      const ground = new THREE.Mesh(
        new THREE.PlaneGeometry(200, 200),
        new THREE.MeshBasicMaterial({ color: 0x080d18 })
      );
      ground.rotation.x = -Math.PI / 2;
      groundRef.current = ground;
      scene.add(ground);

      const grid = new THREE.GridHelper(200, 100, 0x001133, 0x000d22);
      gridRef.current = grid;
      scene.add(grid);

      const startGeom = new THREE.SphereGeometry(0.35, 16, 16);
      const startMat = new THREE.MeshBasicMaterial({ color: 0x00ffe5 });
      const startMesh = new THREE.Mesh(startGeom, startMat);
      startMesh.position.set(0, 0.5, 0);
      startMarkerRef.current = startMesh;
      scene.add(startMesh);

      const sorted = [...vox].sort((a, b) => a.key.localeCompare(b.key));
      const n = sorted.length;
      voxelKeysRef.current = sorted.map((x) => x.key);

      const baseGeom = new THREE.BoxGeometry(1.8, 1, 1.8);
      const mat = new THREE.MeshLambertMaterial({
        vertexColors: true,
        transparent: true,
        opacity: 1,
        depthWrite: true,
      });
      const mesh = new THREE.InstancedMesh(baseGeom, mat, n);
      mesh.instanceMatrix.setUsage(THREE.DynamicDrawUsage);
      mesh.instanceColor = new THREE.InstancedBufferAttribute(new Float32Array(n * 3), 3);
      meshRef.current = mesh;

      const dummy = dummyRef.current;
      const col = new THREE.Color();
      const baseRgba = new Float32Array(n * 3);

      const newKeySet = new Set(sorted.map((x) => x.key));
      if (propsRef.current.liveMode) {
        for (const k of newKeySet) {
          if (!prevVoxelKeySetRef.current.has(k)) {
            flashStartRef.current.set(k, Date.now());
          }
        }
      }
      prevVoxelKeySetRef.current = newKeySet;

      for (let i = 0; i < n; i++) {
        const v = sorted[i];
        const h = v3d ? voxelHeightForEnergy(v.energyDbm) : 0.4;
        dummy.position.set(v.cx, h / 2, v.cz);
        dummy.scale.set(1, h, 1);
        dummy.updateMatrix();
        mesh.setMatrixAt(i, dummy.matrix);
        col.copy(energyToColor(v.energyDbm));
        const dim = 0.65 + 0.35 * (1 - v.lowPrecisionFrac);
        col.multiplyScalar(dim);
        mesh.instanceColor!.setXYZ(i, col.r, col.g, col.b);
        baseRgba[i * 3] = col.r;
        baseRgba[i * 3 + 1] = col.g;
        baseRgba[i * 3 + 2] = col.b;
      }
      baseColorsRef.current = baseRgba;
      mesh.instanceMatrix.needsUpdate = true;
      mesh.instanceColor.needsUpdate = true;
      scene.add(mesh);

      const deadGroup = new THREE.Group();
      deadGroupRef.current = deadGroup;
      const box = new THREE.BoxGeometry(1.85, 0.45, 1.85);
      const lineMat = new THREE.LineBasicMaterial({ color: 0xff2200, transparent: true, opacity: 0.95 });
      deadMatRef.current = lineMat;
      for (const d of dz) {
        const edges = new THREE.EdgesGeometry(box);
        const line = new THREE.LineSegments(edges, lineMat);
        line.position.set(d.cx, 0.22, d.cz);
        deadGroup.add(line);
      }
      box.dispose();
      scene.add(deadGroup);

      if (v3d) {
        camera.position.set(centreX - 20, 40, centreZ + 60);
      } else {
        camera.position.set(centreX, 80, centreZ);
      }
      camera.lookAt(centreX, 0, centreZ);
    },
    [disposeSceneContent]
  );

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
      if (rafRef.current != null) {
        cancelAnimationFrame(rafRef.current);
        rafRef.current = null;
      }
    };
  }, []);

  const onContextCreate = useCallback(
    async (gl: ExpoWebGLRenderingContext) => {
      glRef.current = gl;
      const { drawingBufferWidth: width, drawingBufferHeight: height } = gl;

      const scene = new THREE.Scene();
      scene.background = null;
      sceneRef.current = scene;

      const camera = new THREE.PerspectiveCamera(50, width / Math.max(height, 1), 0.5, 500);
      cameraRef.current = camera;

      const amb = new THREE.AmbientLight(0xffffff, 0.85);
      scene.add(amb);
      const dir = new THREE.DirectionalLight(0xffffff, 0.5);
      dir.position.set(20, 40, 10);
      scene.add(dir);

      const renderer = new Renderer({ gl, alpha: true, clearColor: 0x000000 });
      renderer.setSize(width, height);
      renderer.setClearColor(0x000000, 0);
      rendererRef.current = renderer;

      rebuildFromState(scene, camera);

      let lastFrame = nowMs();

      const loop = () => {
        if (!mountedRef.current) return;
        rafRef.current = requestAnimationFrame(loop);
        const wall = nowMs();
        lastFrame = wall;

        const mesh = meshRef.current;
        const ic = mesh?.instanceColor;
        const base = baseColorsRef.current;
        const keys = voxelKeysRef.current;
        if (mesh && ic && base && keys.length > 0 && propsRef.current.liveMode) {
          let dirty = false;
          for (let i = 0; i < keys.length; i++) {
            const k = keys[i];
            const t0 = flashStartRef.current.get(k);
            if (t0 == null) continue;
            const age = (Date.now() - t0) / 1000;
            if (age >= 1) {
              flashStartRef.current.delete(k);
              const br = base[i * 3];
              const bg = base[i * 3 + 1];
              const bb = base[i * 3 + 2];
              ic.setXYZ(i, br, bg, bb);
              dirty = true;
              continue;
            }
            /** Brightness pulse 1.0 → 0.7 over 1s (new voxel) */
            const brightness = 1 - 0.3 * age;
            ic.setXYZ(
              i,
              base[i * 3] * brightness,
              base[i * 3 + 1] * brightness,
              base[i * 3 + 2] * brightness
            );
            dirty = true;
          }
          if (dirty) ic.needsUpdate = true;
        }

        renderer.render(scene, camera);
        gl.endFrameEXP();
      };
      rafRef.current = requestAnimationFrame(loop);
    },
    [rebuildFromState, liveMode]
  );

  useEffect(() => {
    const scene = sceneRef.current;
    const camera = cameraRef.current;
    if (scene && camera) {
      rebuildFromState(scene, camera);
    }
  }, [voxels, deadZones, view3d, rebuildFromState]);

  return (
    <View style={styles.wrap}>
      <GLView style={styles.gl} onContextCreate={onContextCreate} />
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: {
    height: 280,
    borderRadius: 12,
    overflow: 'hidden',
    borderWidth: 1,
    borderColor: '#1e2a36',
    backgroundColor: '#05080c',
  },
  gl: { flex: 1 },
});
