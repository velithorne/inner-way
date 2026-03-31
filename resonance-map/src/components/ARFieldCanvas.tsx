/**
 * ARFieldCanvas — Three.js transparent GL overlay for AR mode.
 *
 * Bugs fixed:
 *  1. RAF loop used Date.now() instead of performance.now() (safe in RN)
 *  2. Store values read every frame via zustand.subscribe ref pattern
 *  3. Round glow texture built from a DataTexture (no canvas API)
 *  4. Particles flow continuously — reset when z > boundary, not just > 5
 */

import React, { useRef, useCallback, useEffect } from 'react';
import { StyleSheet, View } from 'react-native';
import { GLView, ExpoWebGLRenderingContext } from 'expo-gl';
import * as THREE from 'three';
import { useFieldStore } from '../store/useFieldStore';
import { Colors } from '../constants/theme';
import { FIELD_WEAK_MAX, FIELD_NORMAL_MAX } from '../constants/thresholds';

// ── Constants ────────────────────────────────────────────────────────────────

const PARTICLE_COUNT_FULL = 800;
const PARTICLE_COUNT_LOW  = 400;
const MESH_COLS = 18;
const MESH_ROWS = 12;
const FRAME_SLOW_MS = 1000 / 45;  // below 45fps → reduce particles

// Particle volume bounds (NDC-ish units in camera space)
const BOUND_X = 2.0;
const BOUND_Y = 3.0;
const BOUND_Z_FAR  = -6.0;
const BOUND_Z_NEAR =  1.5;

// ── Helpers ───────────────────────────────────────────────────────────────────

/** Build a 64×64 radial gradient DataTexture — white centre, transparent edge. */
function makeGlowTexture(): THREE.DataTexture {
  const SIZE = 64;
  const data = new Uint8Array(SIZE * SIZE * 4);
  const cx = SIZE / 2, cy = SIZE / 2, r = SIZE / 2;
  for (let y = 0; y < SIZE; y++) {
    for (let x = 0; x < SIZE; x++) {
      const dist = Math.sqrt((x - cx) ** 2 + (y - cy) ** 2);
      const alpha = Math.max(0, 1 - dist / r);
      const v = Math.round(alpha * 255);
      const idx = (y * SIZE + x) * 4;
      data[idx]     = 255; // R
      data[idx + 1] = 255; // G
      data[idx + 2] = 255; // B
      data[idx + 3] = v;   // A — drives the glow shape
    }
  }
  const tex = new THREE.DataTexture(data, SIZE, SIZE, THREE.RGBAFormat);
  tex.needsUpdate = true;
  return tex;
}

function randInBound(half: number) { return (Math.random() - 0.5) * half * 2; }

/** Seed all particle positions randomly within the view volume. */
function seedPositions(buf: Float32Array, count: number) {
  for (let i = 0; i < count; i++) {
    buf[i * 3]     = randInBound(BOUND_X);
    buf[i * 3 + 1] = randInBound(BOUND_Y);
    buf[i * 3 + 2] = BOUND_Z_FAR + Math.random() * (BOUND_Z_NEAR - BOUND_Z_FAR);
  }
}

/** Build the base flat mesh grid as line-segment pairs. */
function buildMeshBase(cols: number, rows: number): Float32Array {
  const segs: number[] = [];
  const dx = (BOUND_X * 2) / (cols - 1);
  const dy = (BOUND_Y * 2) / (rows - 1);
  for (let r = 0; r < rows; r++) {
    for (let c = 0; c < cols - 1; c++) {
      segs.push(-BOUND_X + c * dx, -BOUND_Y + r * dy, -1.5,
                -BOUND_X + (c + 1) * dx, -BOUND_Y + r * dy, -1.5);
    }
  }
  for (let c = 0; c < cols; c++) {
    for (let r = 0; r < rows - 1; r++) {
      segs.push(-BOUND_X + c * dx, -BOUND_Y + r * dy, -1.5,
                -BOUND_X + c * dx, -BOUND_Y + (r + 1) * dy, -1.5);
    }
  }
  return new Float32Array(segs);
}

// ── Component ─────────────────────────────────────────────────────────────────

interface SceneRefs {
  renderer: THREE.WebGLRenderer;
  scene: THREE.Scene;
  camera: THREE.PerspectiveCamera;
  particleGeo: THREE.BufferGeometry;
  particleMat: THREE.PointsMaterial;
  meshGeo: THREE.BufferGeometry;
  meshMat: THREE.LineBasicMaterial;
  meshBase: Float32Array;
  animFrame: number | null;
  activeCount: number;
  lastMs: number;
  tick: number;
  // smooth direction
  curDirX: number;
  curDirY: number;
  curDirZ: number;
  // anomaly slow-motion lerp
  slowFactor: number;
  targetSlowFactor: number;
}

export default function ARFieldCanvas() {
  const sceneRef = useRef<SceneRefs | null>(null);
  // Mirror of store — updated synchronously via subscribe
  const storeRef = useRef(useFieldStore.getState());
  const prevAnomalyRef = useRef(false);

  useEffect(() => {
    return useFieldStore.subscribe((s) => { storeRef.current = s; });
  }, []);

  const onContextCreate = useCallback((gl: ExpoWebGLRenderingContext) => {
    const W = gl.drawingBufferWidth;
    const H = gl.drawingBufferHeight;

    // ── Renderer ──────────────────────────────────────────────────────
    // @ts-ignore expo-gl canvas shim
    const canvasShim: HTMLCanvasElement = {
      width: W, height: H,
      style: {} as CSSStyleDeclaration,
      addEventListener: () => {}, removeEventListener: () => {},
      clientHeight: H,
      // @ts-ignore
      getContext: () => gl,
    };
    const renderer = new THREE.WebGLRenderer({
      canvas: canvasShim,
      context: gl as unknown as WebGLRenderingContext,
      antialias: false,
      alpha: true,
    });
    renderer.setSize(W, H);
    renderer.setPixelRatio(1);
    renderer.setClearColor(0x000000, 0); // fully transparent — camera shows through

    // ── Scene + Camera ────────────────────────────────────────────────
    const scene = new THREE.Scene();
    const camera = new THREE.PerspectiveCamera(70, W / H, 0.01, 50);
    camera.position.set(0, 0, 0);
    camera.lookAt(0, 0, -1);

    // ── Glow texture ──────────────────────────────────────────────────
    const glowTex = makeGlowTexture();

    // ── Particles ─────────────────────────────────────────────────────
    const posArr = new Float32Array(PARTICLE_COUNT_FULL * 3);
    seedPositions(posArr, PARTICLE_COUNT_FULL);

    const particleGeo = new THREE.BufferGeometry();
    particleGeo.setAttribute('position', new THREE.BufferAttribute(posArr, 3));

    const particleMat = new THREE.PointsMaterial({
      map: glowTex,
      size: 0.12,
      sizeAttenuation: true,
      transparent: true,
      opacity: 0.6,
      blending: THREE.AdditiveBlending,
      depthWrite: false,
      color: new THREE.Color(Colors.cyan),
    });

    const points = new THREE.Points(particleGeo, particleMat);
    scene.add(points);

    // ── Mesh grid ─────────────────────────────────────────────────────
    const meshBase = buildMeshBase(MESH_COLS, MESH_ROWS);
    const meshGeo = new THREE.BufferGeometry();
    meshGeo.setAttribute('position', new THREE.BufferAttribute(meshBase.slice(), 3));
    const meshMat = new THREE.LineBasicMaterial({
      color: new THREE.Color(Colors.cyan),
      transparent: true,
      opacity: 0.12,
      blending: THREE.AdditiveBlending,
      depthWrite: false,
    });
    scene.add(new THREE.LineSegments(meshGeo, meshMat));

    // ── Scene refs ────────────────────────────────────────────────────
    const refs: SceneRefs = {
      renderer, scene, camera,
      particleGeo, particleMat,
      meshGeo, meshMat, meshBase,
      animFrame: null,
      activeCount: PARTICLE_COUNT_FULL,
      lastMs: Date.now(),
      tick: 0,
      curDirX: 0, curDirY: 0, curDirZ: -1,
      slowFactor: 1, targetSlowFactor: 1,
    };
    sceneRef.current = refs;

    // ── Animation loop ────────────────────────────────────────────────
    function animate() {
      refs.animFrame = requestAnimationFrame(animate);

      const nowMs = Date.now();
      const dtMs  = Math.min(nowMs - refs.lastMs, 50); // cap at 50ms
      refs.lastMs = nowMs;
      refs.tick  += 1;

      // Auto-reduce particle count when frame is slow
      if (dtMs > FRAME_SLOW_MS) refs.activeCount = PARTICLE_COUNT_LOW;

      // ── Read live sensor data ────────────────────────────────────────
      const { reading, isAnomaly } = storeRef.current;
      const { x: mx, y: my, z: mz, magnitude } = reading;

      // Anomaly slow-motion
      if (isAnomaly !== prevAnomalyRef.current) {
        refs.targetSlowFactor = isAnomaly ? 0.45 : 1.0;
        prevAnomalyRef.current = isAnomaly;
      }
      refs.slowFactor += (refs.targetSlowFactor - refs.slowFactor) * 0.06;

      // Flow speed from magnitude, scaled by slow factor
      const rawSpeed = Math.max(0.4, Math.min(3.0, magnitude / 20));
      const speed = rawSpeed * refs.slowFactor * (dtMs / 16.67);

      // Smooth direction toward magnetic vector (x,y from horizontal plane, z = depth)
      const magLen = Math.sqrt(mx * mx + my * my + mz * mz) || 1;
      const tDirX = (mx / magLen) * 0.6;
      const tDirY = (mz / magLen) * 0.3;
      const tDirZ = -1.0;
      const lerpT = 0.04;
      refs.curDirX += (tDirX - refs.curDirX) * lerpT;
      refs.curDirY += (tDirY - refs.curDirY) * lerpT;
      refs.curDirZ += (tDirZ - refs.curDirZ) * lerpT;

      // Normalise
      const dLen = Math.sqrt(refs.curDirX**2 + refs.curDirY**2 + refs.curDirZ**2) || 1;
      const dx = refs.curDirX / dLen;
      const dy = refs.curDirY / dLen;
      const dz = refs.curDirZ / dLen;

      // ── Update particle positions ─────────────────────────────────────
      const posAttr = refs.particleGeo.attributes.position as THREE.BufferAttribute;
      const pos = posAttr.array as Float32Array;

      for (let i = 0; i < refs.activeCount; i++) {
        const ix = i * 3;

        pos[ix]     += dx * speed * 0.05;
        pos[ix + 1] += dy * speed * 0.05;
        pos[ix + 2] += dz * speed * 0.08;

        // Anomaly: also push radially outward from screen centre
        if (isAnomaly) {
          const px = pos[ix], py = pos[ix + 1];
          const pr = Math.sqrt(px * px + py * py) + 0.001;
          pos[ix]     += (px / pr) * 0.025 * speed;
          pos[ix + 1] += (py / pr) * 0.025 * speed;
        }

        // Wrap particle back to far plane when it exits near bound
        if (pos[ix + 2] > BOUND_Z_NEAR) {
          pos[ix]     = randInBound(BOUND_X);
          pos[ix + 1] = randInBound(BOUND_Y);
          pos[ix + 2] = BOUND_Z_FAR;
        }
        // Also wrap x/y if they fly way off screen
        if (Math.abs(pos[ix]) > BOUND_X * 2) pos[ix] = randInBound(BOUND_X);
        if (Math.abs(pos[ix + 1]) > BOUND_Y * 2) pos[ix + 1] = randInBound(BOUND_Y);
      }
      posAttr.needsUpdate = true;

      // ── Particle colour & size ────────────────────────────────────────
      let col: string;
      let opacity: number;
      let size: number;
      if (isAnomaly) {
        col = Colors.fieldAnomaly;
        opacity = 0.9;
        size = 0.17 + Math.sin(refs.tick * 0.18) * 0.02;
      } else if (magnitude < FIELD_WEAK_MAX) {
        col = '#2255AA';
        opacity = 0.25;
        size = 0.09;
      } else if (magnitude < FIELD_NORMAL_MAX) {
        col = Colors.cyan;
        opacity = 0.55;
        size = 0.12;
      } else {
        col = Colors.blueBright;
        opacity = 0.75;
        size = 0.14;
      }
      refs.particleMat.color.setStyle(col);
      refs.particleMat.opacity = opacity;
      refs.particleMat.size = size;

      // ── Mesh deformation ──────────────────────────────────────────────
      const mAttr = refs.meshGeo.attributes.position as THREE.BufferAttribute;
      const mPos  = mAttr.array as Float32Array;
      const base  = refs.meshBase;
      const verts = base.length / 3;

      for (let v = 0; v < verts; v++) {
        const bx = base[v * 3], by = base[v * 3 + 1], bz = base[v * 3 + 2];
        const wave = Math.sin(refs.tick * 0.025 + bx * 0.8 + by * 0.5) * 0.25;
        mPos[v * 3]     += (bx + dx * wave - mPos[v * 3]) * 0.08;
        mPos[v * 3 + 1] += (by + dy * wave - mPos[v * 3 + 1]) * 0.08;
        mPos[v * 3 + 2] += (bz - mPos[v * 3 + 2]) * 0.08;
      }
      mAttr.needsUpdate = true;
      refs.meshMat.opacity = isAnomaly
        ? 0.45 + Math.sin(refs.tick * 0.14) * 0.12
        : 0.12;

      renderer.render(scene, camera);
      gl.endFrameEXP();
    }

    animate();
  }, []);

  useEffect(() => {
    return () => {
      if (sceneRef.current?.animFrame != null) {
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
    opacity: 0.7,
  },
});
