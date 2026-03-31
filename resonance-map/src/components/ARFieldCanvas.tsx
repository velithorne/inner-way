import React, { useRef, useCallback, useEffect } from 'react';
import { StyleSheet, View } from 'react-native';
import { GLView, ExpoWebGLRenderingContext } from 'expo-gl';
import * as THREE from 'three';
import { useFieldStore } from '../store/useFieldStore';
import { Colors } from '../constants/theme';
import { FIELD_WEAK_MAX, FIELD_NORMAL_MAX } from '../constants/thresholds';

const PARTICLE_COUNT_FULL = 800;
const PARTICLE_COUNT_REDUCED = 400;
const MESH_COLS = 20;
const MESH_ROWS = 14;

// Performance monitoring — reduce particles if frame time exceeds threshold
const FRAME_TIME_HIGH_MS = 1000 / 45; // 45fps threshold

interface ARSceneRefs {
  renderer: THREE.WebGLRenderer;
  scene: THREE.Scene;
  camera: THREE.PerspectiveCamera;
  particles: THREE.Points;
  particleGeo: THREE.BufferGeometry;
  particleMat: THREE.PointsMaterial;
  meshLines: THREE.LineSegments;
  meshGeo: THREE.BufferGeometry;
  meshMat: THREE.LineBasicMaterial;
  animFrame: number | null;
  tick: number;
  particleCount: number;
  lastFrameTime: number;
  // Particle base positions — re-seeded on init
  basePositions: Float32Array;
  velocities: Float32Array;
}

function getParticleColor(magnitude: number, isAnomaly: boolean): THREE.Color {
  if (isAnomaly) return new THREE.Color(Colors.fieldAnomaly);
  if (magnitude < FIELD_WEAK_MAX) return new THREE.Color('#001433');
  if (magnitude < FIELD_NORMAL_MAX) return new THREE.Color(Colors.cyan);
  return new THREE.Color(Colors.blueBright);
}

function getParticleOpacity(magnitude: number, isAnomaly: boolean): number {
  if (isAnomaly) return 0.9;
  if (magnitude < FIELD_WEAK_MAX) return 0.15;
  if (magnitude < FIELD_NORMAL_MAX) return 0.4;
  return 0.7;
}

function seedParticles(count: number, depth: number): { positions: Float32Array; velocities: Float32Array } {
  const positions = new Float32Array(count * 3);
  const velocities = new Float32Array(count * 3);
  for (let i = 0; i < count; i++) {
    // Spread particles in a volume in front of camera
    positions[i * 3]     = (Math.random() - 0.5) * 8;
    positions[i * 3 + 1] = (Math.random() - 0.5) * 12;
    positions[i * 3 + 2] = -(Math.random() * depth);
    velocities[i * 3]     = 0;
    velocities[i * 3 + 1] = 0;
    velocities[i * 3 + 2] = 0;
  }
  return { positions, velocities };
}

function buildMeshPositions(cols: number, rows: number): Float32Array {
  // Build a grid of line segments (row lines + col lines)
  const segments: number[] = [];
  const W = 8, H = 12;
  const dx = W / (cols - 1);
  const dy = H / (rows - 1);

  // Horizontal lines
  for (let r = 0; r < rows; r++) {
    for (let c = 0; c < cols - 1; c++) {
      segments.push(
        -W / 2 + c * dx, -H / 2 + r * dy, -2,
        -W / 2 + (c + 1) * dx, -H / 2 + r * dy, -2
      );
    }
  }
  // Vertical lines
  for (let c = 0; c < cols; c++) {
    for (let r = 0; r < rows - 1; r++) {
      segments.push(
        -W / 2 + c * dx, -H / 2 + r * dy, -2,
        -W / 2 + c * dx, -H / 2 + (r + 1) * dy, -2
      );
    }
  }
  return new Float32Array(segments);
}

export default function ARFieldCanvas() {
  const sceneRef = useRef<ARSceneRefs | null>(null);
  const storeRef = useRef(useFieldStore.getState());
  const prevAnomalyRef = useRef(false);

  useEffect(() => {
    const unsub = useFieldStore.subscribe((s) => { storeRef.current = s; });
    return unsub;
  }, []);

  const onContextCreate = useCallback(async (gl: ExpoWebGLRenderingContext) => {
    const { drawingBufferWidth: width, drawingBufferHeight: height } = gl;

    // @ts-ignore
    const canvasShim: HTMLCanvasElement = {
      width, height,
      style: {} as CSSStyleDeclaration,
      addEventListener: () => {},
      removeEventListener: () => {},
      clientHeight: height,
      // @ts-ignore
      getContext: () => gl,
    };

    const renderer = new THREE.WebGLRenderer({
      canvas: canvasShim,
      context: gl as unknown as WebGLRenderingContext,
      antialias: false,
      alpha: true,           // transparent background so camera shows through
    });
    renderer.setSize(width, height);
    renderer.setClearColor(0x000000, 0);  // fully transparent
    renderer.setPixelRatio(1);

    const scene = new THREE.Scene();

    const camera = new THREE.PerspectiveCamera(75, width / height, 0.01, 100);
    camera.position.set(0, 0, 4);
    camera.lookAt(0, 0, 0);

    // ── Particle system ──────────────────────────────────────────────
    const count = PARTICLE_COUNT_FULL;
    const { positions, velocities } = seedParticles(count, 8);

    const particleGeo = new THREE.BufferGeometry();
    particleGeo.setAttribute('position', new THREE.BufferAttribute(positions.slice(), 3));
    const particleMat = new THREE.PointsMaterial({
      color: new THREE.Color(Colors.cyan),
      size: 0.045,
      transparent: true,
      opacity: 0.4,
      depthWrite: false,
      blending: THREE.AdditiveBlending,
    });
    const particles = new THREE.Points(particleGeo, particleMat);
    scene.add(particles);

    // ── Mesh deformation grid ─────────────────────────────────────────
    const baseMeshPositions = buildMeshPositions(MESH_COLS, MESH_ROWS);
    const meshGeo = new THREE.BufferGeometry();
    meshGeo.setAttribute(
      'position',
      new THREE.BufferAttribute(baseMeshPositions.slice(), 3)
    );
    const meshMat = new THREE.LineBasicMaterial({
      color: new THREE.Color(Colors.cyan),
      transparent: true,
      opacity: 0.15,
      depthWrite: false,
      blending: THREE.AdditiveBlending,
    });
    const meshLines = new THREE.LineSegments(meshGeo, meshMat);
    scene.add(meshLines);

    const refs: ARSceneRefs = {
      renderer, scene, camera,
      particles, particleGeo, particleMat,
      meshLines, meshGeo, meshMat,
      animFrame: null,
      tick: 0,
      particleCount: count,
      lastFrameTime: performance.now(),
      basePositions: baseMeshPositions.slice(),
      velocities: velocities.slice(),
    };
    sceneRef.current = refs;

    // Working arrays for deformed mesh
    const deformedMesh = baseMeshPositions.slice();
    const currentDir = new THREE.Vector3(0, 0, -1);
    const targetDir = new THREE.Vector3(0, 0, -1);

    let anomalySlowFactor = 1.0;
    let targetSlowFactor = 1.0;

    function animate() {
      refs.animFrame = requestAnimationFrame(animate);

      const now = performance.now();
      const dt = now - refs.lastFrameTime;
      refs.lastFrameTime = now;

      // Auto-reduce particles if running slow
      if (dt > FRAME_TIME_HIGH_MS && refs.particleCount === PARTICLE_COUNT_FULL) {
        refs.particleCount = PARTICLE_COUNT_REDUCED;
      }

      refs.tick += 1;
      const { reading, isAnomaly, rollingAverage } = storeRef.current;
      const { x: mx, y: my, z: mz, magnitude } = reading;

      // Anomaly transition
      if (isAnomaly && !prevAnomalyRef.current) targetSlowFactor = 0.5;
      if (!isAnomaly && prevAnomalyRef.current) targetSlowFactor = 1.0;
      prevAnomalyRef.current = isAnomaly;
      anomalySlowFactor += (targetSlowFactor - anomalySlowFactor) * 0.05;

      const speed = (Math.max(0.3, Math.min(2.5, magnitude / 25))) * anomalySlowFactor;

      // Build normalised flow direction from magnetometer
      const magLen = Math.sqrt(mx * mx + my * my + mz * mz) || 1;
      targetDir.set(mx / magLen * 0.8, mz / magLen * 0.4, -1).normalize();
      currentDir.lerp(targetDir, 0.03);

      // Update particles
      const posAttr = refs.particleGeo.attributes.position as THREE.BufferAttribute;
      const arr = posAttr.array as Float32Array;
      const baseSeeds = refs.basePositions; // we re-use this as seed reference

      for (let i = 0; i < refs.particleCount; i++) {
        const ix = i * 3, iy = ix + 1, iz = ix + 2;

        // Flow toward camera along field direction
        arr[ix]  += currentDir.x * speed * 0.03;
        arr[iy]  += currentDir.y * speed * 0.03;
        arr[iz]  += speed * 0.06;  // always moving toward camera (positive z)

        // Anomaly: explode radially outward
        if (isAnomaly) {
          const rx = arr[ix], ry = arr[iy];
          const r = Math.sqrt(rx * rx + ry * ry) + 0.001;
          arr[ix] += (rx / r) * 0.04;
          arr[iy] += (ry / r) * 0.04;
        }

        // Reset particle when it passes behind camera
        if (arr[iz] > 5) {
          arr[ix] = (Math.random() - 0.5) * 8;
          arr[iy] = (Math.random() - 0.5) * 12;
          arr[iz] = -(6 + Math.random() * 4);
          // Vignette density: edges more particles
          const edgeBias = Math.random() < 0.4;
          if (edgeBias) {
            arr[ix] *= 1.5 + Math.random() * 0.5;
            arr[iy] *= 1.5 + Math.random() * 0.5;
          }
        }
      }
      posAttr.needsUpdate = true;

      // Particle colour & size
      const col = getParticleColor(magnitude, isAnomaly);
      const opa = getParticleOpacity(magnitude, isAnomaly);
      refs.particleMat.color.copy(col);
      refs.particleMat.opacity = opa;

      // Anomaly: bigger particles
      refs.particleMat.size = isAnomaly
        ? 0.045 * 1.4 + Math.sin(refs.tick * 0.15) * 0.01
        : 0.045;

      // ── Mesh deformation ────────────────────────────────────────────
      const mPosAttr = refs.meshGeo.attributes.position as THREE.BufferAttribute;
      const mArr = mPosAttr.array as Float32Array;
      const base = refs.basePositions;
      const vertCount = base.length / 3;

      for (let v = 0; v < vertCount; v++) {
        const bx = base[v * 3], by = base[v * 3 + 1], bz = base[v * 3 + 2];
        // Deform: pull vertices toward field vector direction
        const fieldInfluence = Math.sin(refs.tick * 0.02 + bx * 0.5 + by * 0.3) * 0.3;
        const tx = bx + currentDir.x * fieldInfluence;
        const ty = by + currentDir.y * fieldInfluence;
        mArr[v * 3]     += (tx - mArr[v * 3]) * 0.08;
        mArr[v * 3 + 1] += (ty - mArr[v * 3 + 1]) * 0.08;
        mArr[v * 3 + 2] += (bz - mArr[v * 3 + 2]) * 0.08;
      }
      mPosAttr.needsUpdate = true;

      refs.meshMat.opacity = isAnomaly
        ? 0.5 + Math.sin(refs.tick * 0.12) * 0.15
        : 0.15;

      renderer.render(scene, camera);
      gl.endFrameEXP();
    }

    animate();
  }, []);

  useEffect(() => {
    return () => {
      if (sceneRef.current?.animFrame) {
        cancelAnimationFrame(sceneRef.current.animFrame);
      }
    };
  }, []);

  return (
    <View style={styles.container} pointerEvents="none">
      <GLView style={StyleSheet.absoluteFill} onContextCreate={onContextCreate} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    ...StyleSheet.absoluteFillObject,
    opacity: 0.65,   // AR overlay transparency
  },
});
