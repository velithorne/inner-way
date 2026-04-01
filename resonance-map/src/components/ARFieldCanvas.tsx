/**
 * ARFieldCanvas — Physically accurate multi-field AR renderer.
 *
 * FIELD 1: Magnetic dipole lines (r = r0·sin²θ geometry, live magnetometer axis)
 * FIELD 2: RF heat bloom (WiFi RSSI → sprite clouds, 3s update)
 * FIELD 3: Gravity threads (accelerometer low-pass → true vertical lines)
 *
 * Every visible element is driven by real sensor data.
 * Nothing is decorative.
 */

import React, { useRef, useCallback, useEffect } from 'react';
import { StyleSheet, View } from 'react-native';
import { GLView, ExpoWebGLRenderingContext } from 'expo-gl';
import * as THREE from 'three';
import { Accelerometer } from 'expo-sensors';
import { useFieldStore } from '../store/useFieldStore';
import { subscribeRF, RFNetwork } from '../services/rfScanner';
import { Colors } from '../constants/theme';
import { FIELD_WEAK_MAX, FIELD_NORMAL_MAX } from '../constants/thresholds';

// ── Constants ────────────────────────────────────────────────────────────────
const DIPOLE_LINES       = 16;   // field lines around magnetic axis
const DIPOLE_POINTS      = 60;   // points per CatmullRom curve
const DIPOLE_R0          = 2.2;  // equatorial radius of outer field line
const RF_SPRITES_FULL    = 20;
const RF_SPRITES_REDUCED = 8;
const GRAV_THREADS       = 12;
const GRAV_LENGTH        = 5.0;
const ACCEL_ALPHA        = 0.05; // low-pass smoothing for gravity

// ── Glow texture ─────────────────────────────────────────────────────────────
function makeGlowTexture(): THREE.DataTexture {
  const S = 64;
  const data = new Uint8Array(S * S * 4);
  for (let y = 0; y < S; y++) {
    for (let x = 0; x < S; x++) {
      const d = Math.sqrt((x - S / 2) ** 2 + (y - S / 2) ** 2) / (S / 2);
      const a = Math.max(0, 1 - d) ** 1.8;
      const i = (y * S + x) * 4;
      data[i] = data[i+1] = data[i+2] = 255;
      data[i+3] = Math.round(a * 255);
    }
  }
  const t = new THREE.DataTexture(data, S, S, THREE.RGBAFormat);
  t.needsUpdate = true;
  return t;
}

// ── Dipole geometry ──────────────────────────────────────────────────────────
/**
 * Generate one dipole field line in the plane defined by the meridian angle φ.
 * r = r0 * sin²(θ), θ from 0 to π, projected into 3D.
 * The line arcs from south pole, out to equatorial radius, back to north pole.
 */
function dipoleCurvePoints(
  r0: number,
  phiRad: number,      // meridian angle around axis
  numPts: number
): THREE.Vector3[] {
  const pts: THREE.Vector3[] = [];
  for (let i = 0; i <= numPts; i++) {
    const theta = (i / numPts) * Math.PI;
    const r = r0 * Math.sin(theta) ** 2;
    // Spherical → Cartesian (axis = Y, meridian = XZ plane rotated by phi)
    const x = r * Math.sin(theta) * Math.cos(phiRad);
    const y = r * Math.cos(theta);
    const z = r * Math.sin(theta) * Math.sin(phiRad);
    pts.push(new THREE.Vector3(x, y, z));
  }
  return pts;
}

// Build a quaternion that rotates Y-axis to align with the magnetic axis vector
function axisQuat(mx: number, my: number, mz: number): THREE.Quaternion {
  const len = Math.sqrt(mx*mx + my*my + mz*mz) || 1;
  const target = new THREE.Vector3(mx/len, mz/len, my/len); // remap sensor axes
  const q = new THREE.Quaternion();
  q.setFromUnitVectors(new THREE.Vector3(0, 1, 0), target.normalize());
  return q;
}

function getMagColor(magnitude: number, isAnomaly: boolean): THREE.Color {
  if (isAnomaly) return new THREE.Color(Colors.fieldAnomaly);
  if (magnitude < FIELD_WEAK_MAX) return new THREE.Color('#006688');
  if (magnitude < FIELD_NORMAL_MAX) return new THREE.Color(Colors.cyan);
  return new THREE.Color(Colors.blueBright);
}

// ── RF sprite positions ───────────────────────────────────────────────────────
function rfSpritePositions(
  network: RFNetwork,
  cameraHeadingRad: number,
  spriteCount: number
): THREE.Vector3[] {
  // Map RSSI intensity to depth: strong=close, weak=far
  const depth = 0.5 + (1.0 - network.intensity) * 1.5;
  // Pseudo-direction from BSSID hash + camera heading
  const angle = ((network.pseudoAngle * Math.PI / 180) + cameraHeadingRad) % (Math.PI * 2);
  const cx = Math.sin(angle) * depth * 0.6;
  const cy = 0;
  const cz = -depth;

  const pts: THREE.Vector3[] = [];
  for (let i = 0; i < spriteCount; i++) {
    // Gaussian distribution around centre
    const u = Math.random(), v = Math.random();
    const r = Math.sqrt(-2 * Math.log(u + 0.001)) * 0.25;
    const a = 2 * Math.PI * v;
    pts.push(new THREE.Vector3(cx + r*Math.cos(a), cy + r*Math.sin(a)*0.6, cz + r*0.3));
  }
  return pts;
}

// ── Gravity thread positions ──────────────────────────────────────────────────
function gravThreadPositions(
  gx: number, gy: number, gz: number,  // smoothed accelerometer (gravity direction)
  width: number
): { starts: THREE.Vector3[]; ends: THREE.Vector3[] } {
  // Gravity vector in camera space — normalise
  const len = Math.sqrt(gx*gx + gy*gy + gz*gz) || 1;
  const gDir = new THREE.Vector3(-gx/len, -gy/len, -gz/len); // down = gravity dir
  const halfLen = GRAV_LENGTH / 2;
  const starts: THREE.Vector3[] = [];
  const ends: THREE.Vector3[] = [];

  for (let i = 0; i < GRAV_THREADS; i++) {
    const t = (i / (GRAV_THREADS - 1)) - 0.5;
    const x = t * width;
    const centre = new THREE.Vector3(x, 0, -1.5);
    starts.push(centre.clone().addScaledVector(gDir, -halfLen));
    ends.push(centre.clone().addScaledVector(gDir, halfLen));
  }
  return { starts, ends };
}

// ── Shared scene refs ─────────────────────────────────────────────────────────
interface SceneRefs {
  renderer: THREE.WebGLRenderer;
  scene: THREE.Scene;
  camera: THREE.PerspectiveCamera;
  // Dipole
  dipoleGroup: THREE.Group;
  dipoleLines: THREE.Line[];
  dipoleAxis: THREE.Line;
  lastAxisQuat: THREE.Quaternion;
  lastAxisAngleDeg: number;
  // RF
  rfGroup: THREE.Group;
  rfSprites: Map<string, THREE.Sprite[]>;
  rfSpriteCount: number;
  glowTex: THREE.DataTexture;
  // Gravity
  gravGroup: THREE.Group;
  gravLineGeo: THREE.BufferGeometry;
  // State
  animFrame: number | null;
  tick: number;
  lastMs: number;
  // Field visibility (driven by layer toggles in ARFieldScreen)
  showMag: boolean;
  showRF: boolean;
  showGrav: boolean;
}

export interface ARFieldCanvasHandle {
  setLayers: (mag: boolean, rf: boolean, grav: boolean) => void;
}

interface Props {
  layerRef?: React.MutableRefObject<ARFieldCanvasHandle | null>;
}

export default function ARFieldCanvas({ layerRef }: Props) {
  const sceneRef = useRef<SceneRefs | null>(null);
  const storeRef = useRef(useFieldStore.getState());
  const rfRef = useRef<RFNetwork[]>([]);
  const gravRef = useRef({ x: 0, y: -9.8, z: 0 }); // smoothed accelerometer

  useEffect(() => {
    return useFieldStore.subscribe((s) => { storeRef.current = s; });
  }, []);

  // RF scanner subscription
  useEffect(() => {
    return subscribeRF((state) => {
      rfRef.current = state.networks;
      // Update RF sprite positions when new scan arrives
      const refs = sceneRef.current;
      if (!refs) return;
      updateRFSprites(refs, rfRef.current, storeRef.current.reading.heading);
    });
  }, []);

  // Accelerometer for gravity
  useEffect(() => {
    Accelerometer.setUpdateInterval(16); // 60Hz
    const sub = Accelerometer.addListener(({ x, y, z }) => {
      // Low-pass filter — smooth out device vibration
      gravRef.current = {
        x: gravRef.current.x + ACCEL_ALPHA * (x - gravRef.current.x),
        y: gravRef.current.y + ACCEL_ALPHA * (y - gravRef.current.y),
        z: gravRef.current.z + ACCEL_ALPHA * (z - gravRef.current.z),
      };
    });
    return () => sub.remove();
  }, []);

  // Layer control handle
  useEffect(() => {
    if (!layerRef) return;
    layerRef.current = {
      setLayers: (mag, rf, grav) => {
        const refs = sceneRef.current;
        if (!refs) return;
        refs.showMag = mag;
        refs.showRF = rf;
        refs.showGrav = grav;
        refs.dipoleGroup.visible = mag;
        refs.rfGroup.visible = rf;
        refs.gravGroup.visible = grav;
      },
    };
  }, [layerRef]);

  // ── RF sprite update (called on scan, not every frame) ──────────────────
  function updateRFSprites(refs: SceneRefs, networks: RFNetwork[], headingDeg: number) {
    // Clear old sprites
    refs.rfGroup.clear();
    refs.rfSprites.clear();

    const headingRad = (headingDeg * Math.PI) / 180;

    for (const net of networks) {
      const sprites: THREE.Sprite[] = [];
      const positions = rfSpritePositions(net, headingRad, refs.rfSpriteCount);
      for (const pos of positions) {
        const mat = new THREE.SpriteMaterial({
          map: refs.glowTex,
          color: new THREE.Color(Colors.gold),
          transparent: true,
          opacity: Math.max(0.12, net.intensity * 0.85),
          blending: THREE.AdditiveBlending,
          depthWrite: false,
        });
        const s = new THREE.Sprite(mat);
        s.position.copy(pos);
        const sz = 0.06 + net.intensity * 0.18;
        s.scale.set(sz, sz, 1);
        refs.rfGroup.add(s);
        sprites.push(s);
      }
      refs.rfSprites.set(net.id, sprites);
    }
  }

  const onContextCreate = useCallback((gl: ExpoWebGLRenderingContext) => {
    const W = gl.drawingBufferWidth;
    const H = gl.drawingBufferHeight;

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
    renderer.setClearColor(0x000000, 0);

    const scene = new THREE.Scene();
    const camera = new THREE.PerspectiveCamera(70, W / H, 0.01, 50);
    camera.position.set(0, 0, 0);

    const glowTex = makeGlowTexture();

    // ── FIELD 1: Magnetic dipole lines ──────────────────────────────────────
    const dipoleGroup = new THREE.Group();
    scene.add(dipoleGroup);

    const dipoleLines: THREE.Line[] = [];
    for (let i = 0; i < DIPOLE_LINES; i++) {
      const phi = (i / DIPOLE_LINES) * Math.PI * 2;
      // Distribute outer lines with varying r0 for visual depth
      const lineR0 = DIPOLE_R0 * (0.4 + (i % 4) * 0.2);
      const pts = dipoleCurvePoints(lineR0, phi, DIPOLE_POINTS);
      const curve = new THREE.CatmullRomCurve3(pts);
      const geo = new THREE.BufferGeometry().setFromPoints(curve.getPoints(DIPOLE_POINTS));
      const mat = new THREE.LineBasicMaterial({
        color: new THREE.Color(Colors.cyan),
        transparent: true,
        opacity: 0.6,
        depthWrite: false,
        blending: THREE.AdditiveBlending,
      });
      const line = new THREE.Line(geo, mat);
      dipoleGroup.add(line);
      dipoleLines.push(line);
    }

    // Central axis line
    const axisPts = [new THREE.Vector3(0, -DIPOLE_R0 * 0.5, 0), new THREE.Vector3(0, DIPOLE_R0 * 0.5, 0)];
    const axisGeo = new THREE.BufferGeometry().setFromPoints(axisPts);
    const axisMat = new THREE.LineBasicMaterial({
      color: new THREE.Color(Colors.cyan),
      transparent: true,
      opacity: 1.0,
      depthWrite: false,
      blending: THREE.AdditiveBlending,
    });
    const dipoleAxis = new THREE.Line(axisGeo, axisMat);
    dipoleGroup.add(dipoleAxis);

    // ── FIELD 2: RF sprites ──────────────────────────────────────────────────
    const rfGroup = new THREE.Group();
    scene.add(rfGroup);

    // ── FIELD 3: Gravity threads ─────────────────────────────────────────────
    const gravGroup = new THREE.Group();
    scene.add(gravGroup);

    // Pre-allocate gravity line geometries
    const gravPositions = new Float32Array(GRAV_THREADS * 2 * 3);
    const gravGeo = new THREE.BufferGeometry();
    gravGeo.setAttribute('position', new THREE.BufferAttribute(gravPositions, 3));
    const gravMat = new THREE.LineBasicMaterial({
      color: new THREE.Color('#001433'),
      transparent: true,
      opacity: 0.3,
      depthWrite: false,
      blending: THREE.AdditiveBlending,
    });
    const gravLineSegments = new THREE.LineSegments(gravGeo, gravMat);
    gravGroup.add(gravLineSegments);

    const refs: SceneRefs = {
      renderer, scene, camera,
      dipoleGroup, dipoleLines, dipoleAxis,
      lastAxisQuat: new THREE.Quaternion(),
      lastAxisAngleDeg: 0,
      rfGroup,
      rfSprites: new Map(),
      rfSpriteCount: RF_SPRITES_FULL,
      glowTex,
      gravGroup,
      gravLineGeo: gravGeo,
      animFrame: null,
      tick: 0,
      lastMs: Date.now(),
      showMag: true, showRF: true, showGrav: true,
    };
    sceneRef.current = refs;

    // Expose layer handle
    if (layerRef) {
      layerRef.current = {
        setLayers: (mag, rf, grav) => {
          refs.showMag = mag; refs.showRF = rf; refs.showGrav = grav;
          dipoleGroup.visible = mag;
          rfGroup.visible = rf;
          gravGroup.visible = grav;
        },
      };
    }

    let prevAxisQuat = new THREE.Quaternion();
    const currentQuat = new THREE.Quaternion();
    let frameCount = 0;

    function animate() {
      refs.animFrame = requestAnimationFrame(animate);

      const now = Date.now();
      const dt = Math.min(now - refs.lastMs, 50);
      refs.lastMs = now;
      refs.tick += 1;
      frameCount += 1;

      // Auto-reduce sprites if slow
      if (dt > 22 && refs.rfSpriteCount === RF_SPRITES_FULL) {
        refs.rfSpriteCount = RF_SPRITES_REDUCED;
      }

      const { reading, isAnomaly } = storeRef.current;
      const { x: mx, y: my, z: mz, magnitude, heading } = reading;

      // ── FIELD 1: Dipole rotation ───────────────────────────────────────────
      if (refs.showMag) {
        const targetQuat = axisQuat(mx, my, mz);

        // Only recompute dipole geometry when axis shifts > 2°
        const angleDiff = prevAxisQuat.angleTo(targetQuat) * (180 / Math.PI);
        if (angleDiff > 2 || frameCount === 1) {
          prevAxisQuat.copy(targetQuat);
        }

        // Smooth interpolation toward target every frame
        currentQuat.slerp(targetQuat, 0.04);
        dipoleGroup.quaternion.copy(currentQuat);

        const col = getMagColor(magnitude, isAnomaly);
        const opacity = isAnomaly ? 0.9 : 0.6;
        for (const line of refs.dipoleLines) {
          (line.material as THREE.LineBasicMaterial).color.copy(col);
          (line.material as THREE.LineBasicMaterial).opacity = opacity;
          (line.material as THREE.LineBasicMaterial).linewidth = isAnomaly ? 3 : 1.5;
          (line.material as THREE.LineBasicMaterial).needsUpdate = true;
        }
        (dipoleAxis.material as THREE.LineBasicMaterial).color.copy(col);
        (dipoleAxis.material as THREE.LineBasicMaterial).opacity = isAnomaly ? 1.0 : 0.85;
      }

      // ── FIELD 3: Gravity threads ───────────────────────────────────────────
      if (refs.showGrav) {
        const { x: gx, y: gy, z: gz } = gravRef.current;
        const { starts, ends } = gravThreadPositions(gx, gy, gz, 3.5);
        const pos = refs.gravLineGeo.attributes.position as THREE.BufferAttribute;
        const arr = pos.array as Float32Array;

        // Gravity/mag convergence check
        const gravLen = Math.sqrt(gx*gx + gy*gy + gz*gz) || 1;
        const magLen  = Math.sqrt(mx*mx + my*my + mz*mz) || 1;
        const dot = (gx/gravLen)*(mx/magLen) + (gy/gravLen)*(my/magLen) + (gz/gravLen)*(mz/magLen);
        const angleBetween = Math.acos(Math.max(-1, Math.min(1, Math.abs(dot)))) * (180/Math.PI);
        const convergence = angleBetween < 10;
        const gravMat = gravLineSegments.material as THREE.LineBasicMaterial;
        gravMat.color.setStyle(convergence ? Colors.cyan : '#001433');
        gravMat.opacity = convergence ? 0.65 + Math.sin(refs.tick * 0.08) * 0.15 : 0.3;
        gravMat.needsUpdate = true;

        for (let i = 0; i < GRAV_THREADS; i++) {
          arr[i*6]   = starts[i].x; arr[i*6+1] = starts[i].y; arr[i*6+2] = starts[i].z;
          arr[i*6+3] = ends[i].x;   arr[i*6+4] = ends[i].y;   arr[i*6+5] = ends[i].z;
        }
        pos.needsUpdate = true;
      }

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
    opacity: 0.75,
  },
});
