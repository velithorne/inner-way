/**
 * ARFieldCanvas — Deep realism multi-field AR renderer.
 *
 * FIELD 1: Magnetic dipole — true r=L·sin²(θ) geometry, tube geometry with
 *          vertex colour gradient, travelling highlight nodes, pole spheres,
 *          compression on anomaly.
 *
 * FIELD 2: RF radiation — expanding concentric wavefront shells per WiFi
 *          source (frequency determines shell spacing), interference plane
 *          particles where two networks' wavefronts bisect, surface echo.
 *
 * FIELD 3: Gravity — deforming 3D wireframe mesh + falling particles always
 *          in true gravitational direction.
 *
 * Depth fog, chromatic separation post-process (high-RAM devices only),
 * magnetic/gravity convergence detection.
 */

import React, { useRef, useCallback, useEffect } from 'react';
import { StyleSheet, View, Platform } from 'react-native';
import { GLView, ExpoWebGLRenderingContext } from 'expo-gl';
import * as THREE from 'three';
import { Accelerometer } from 'expo-sensors';
import DeviceInfo from 'react-native-device-info';
import { useFieldStore } from '../store/useFieldStore';
import { subscribeRF, RFNetwork } from '../services/rfScanner';
import { Colors } from '../constants/theme';
import { FIELD_WEAK_MAX, FIELD_NORMAL_MAX } from '../constants/thresholds';

// ── Constants ────────────────────────────────────────────────────────────────
const MAG_L_VALUES  = [0.3, 0.45, 0.6, 0.75, 0.9, 1.1, 1.35, 1.7, 2.1, 2.6, 3.2, 3.8];
const MAG_AZ_COUNT  = 2;   // lines per L value (symmetric pair)
const MAG_CURVE_PTS = 80;
const MAG_TUBE_SEGS = 40;
const HIGHLIGHT_PER_LINE = 3;

const RF_SHELLS_PER_NET = 6;
const RF_MAX_NETWORKS   = 4;
const RF_MAX_RADIUS     = 1.8;
const INTERFERENCE_PTS  = 40;
const RF_LOOKATA_EVERY  = 6; // frames between ring.lookAt calls

const GRAV_GRID_W   = 20;
const GRAV_GRID_H   = 14;
const GRAV_GRID_D   = 4;
const GRAV_PARTICLES_FULL    = 80;   // additive stacking — 80 is plenty
const GRAV_PARTICLES_REDUCED = 40;
const ACCEL_ALPHA   = 0.04;
const FRAME_SLOW_MS = 1000 / 45;

// ── Textures ──────────────────────────────────────────────────────────────────
function makeGlowTex(size = 64): THREE.DataTexture {
  const d = new Uint8Array(size * size * 4);
  for (let y = 0; y < size; y++) {
    for (let x = 0; x < size; x++) {
      const r = Math.sqrt((x - size/2)**2 + (y - size/2)**2) / (size/2);
      const a = Math.max(0, 1 - r) ** 1.6;
      const i = (y*size+x)*4;
      d[i]=d[i+1]=d[i+2]=255; d[i+3]=Math.round(a*255);
    }
  }
  const t = new THREE.DataTexture(d,size,size,THREE.RGBAFormat);
  t.needsUpdate=true; return t;
}

// ── Dipole geometry ───────────────────────────────────────────────────────────
function dipolePoints(L: number, azPhi: number, nPts: number): THREE.Vector3[] {
  const pts: THREE.Vector3[] = [];
  for (let i = 0; i <= nPts; i++) {
    const theta = (i / nPts) * Math.PI;
    const r = L * Math.sin(theta) ** 2;
    pts.push(new THREE.Vector3(
      r * Math.sin(theta) * Math.cos(azPhi),
      r * Math.cos(theta),
      r * Math.sin(theta) * Math.sin(azPhi)
    ));
  }
  return pts;
}

function magAxisQuat(mx: number, my: number, mz: number): THREE.Quaternion {
  const len = Math.sqrt(mx*mx+my*my+mz*mz)||1;
  const target = new THREE.Vector3(mx/len, mz/len, my/len).normalize();
  const q = new THREE.Quaternion();
  q.setFromUnitVectors(new THREE.Vector3(0,1,0), target);
  return q;
}

function getMagColor(magnitude: number, isAnomaly: boolean): THREE.Color {
  if (isAnomaly) return new THREE.Color(Colors.fieldAnomaly);
  if (magnitude < FIELD_WEAK_MAX) return new THREE.Color('#006688');
  if (magnitude < FIELD_NORMAL_MAX) return new THREE.Color(Colors.cyan);
  return new THREE.Color(Colors.blueBright);
}

/**
 * Build a 3-pass glowing line from dipole curve points.
 * Returns [core, mid, outer] THREE.Line objects with LineBasicMaterial.
 *
 * Tubes were causing solid opaque planes on mobile WebGL — replaced with
 * three overlapping lines at additive blending creating a soft glow core.
 * More visible on dark surfaces, nearly invisible on white (physically correct).
 */
function buildGlowLines(pts: THREE.Vector3[]): THREE.Line[] {
  const curve = new THREE.CatmullRomCurve3(pts);
  const curvePoints = curve.getPoints(MAG_CURVE_PTS);

  const passes = [
    { opacity: 0.60, color: 0x00FFE5, offset: 0.000 },  // core — cyan
    { opacity: 0.28, color: 0xAFFFFF, offset: 0.0018 }, // mid  — pale cyan
    { opacity: 0.12, color: 0xFFFFFF, offset: 0.0038 }, // outer — white diffuse
  ];

  return passes.map(pass => {
    const positions = new Float32Array(curvePoints.length * 3);
    for (let i = 0; i < curvePoints.length; i++) {
      const p    = curvePoints[i];
      const next = curvePoints[Math.min(i + 1, curvePoints.length - 1)];
      // Small normal offset for mid/outer passes (XY plane normal to tangent)
      const tx = next.x - p.x, ty = next.y - p.y;
      const tlen = Math.sqrt(tx*tx + ty*ty) || 1;
      const nx = -ty / tlen, ny = tx / tlen;
      positions[i*3]   = p.x + nx * pass.offset;
      positions[i*3+1] = p.y + ny * pass.offset;
      positions[i*3+2] = p.z;
    }
    const geo = new THREE.BufferGeometry();
    geo.setAttribute('position', new THREE.BufferAttribute(positions, 3));
    const mat = new THREE.LineBasicMaterial({
      color: pass.color,
      transparent: true,
      opacity: pass.opacity,
      blending: THREE.AdditiveBlending,
      depthWrite: false,
      depthTest: false,
    });
    return new THREE.Line(geo, mat);
  });
}

// ── Exports ───────────────────────────────────────────────────────────────────
export interface ARFieldCanvasHandle {
  setLayers: (mag: boolean, rf: boolean, grav: boolean) => void;
}

interface Props {
  layerRef?: React.MutableRefObject<ARFieldCanvasHandle | null>;
  onConvergence?: (isConverging: boolean) => void;
}

// ── Component ─────────────────────────────────────────────────────────────────
export default function ARFieldCanvas({ layerRef, onConvergence }: Props) {
  const sceneRef   = useRef<any>(null);
  const storeRef   = useRef(useFieldStore.getState());
  const rfRef      = useRef<RFNetwork[]>([]);
  const gravRef    = useRef({ x: 0, y: -9.8, z: 0 });
  const highRamRef = useRef(false);

  useEffect(() => {
    return useFieldStore.subscribe(s => { storeRef.current = s; });
  }, []);

  useEffect(() => {
    return subscribeRF(s => { rfRef.current = s.networks.slice(0, RF_MAX_NETWORKS); });
  }, []);

  useEffect(() => {
    Accelerometer.setUpdateInterval(16);
    const sub = Accelerometer.addListener(({ x, y, z }) => {
      const g = gravRef.current;
      gravRef.current = {
        x: g.x + ACCEL_ALPHA*(x-g.x),
        y: g.y + ACCEL_ALPHA*(y-g.y),
        z: g.z + ACCEL_ALPHA*(z-g.z),
      };
    });
    return () => sub.remove();
  }, []);

  useEffect(() => {
    DeviceInfo.getTotalMemory().then(mem => {
      highRamRef.current = mem > 4 * 1024 * 1024 * 1024; // > 4GB
    }).catch(() => {});
  }, []);

  const onContextCreate = useCallback((gl: ExpoWebGLRenderingContext) => {
    const W = gl.drawingBufferWidth, H = gl.drawingBufferHeight;

    // @ts-ignore expo-gl canvas shim
    const canvas: HTMLCanvasElement = {
      width:W, height:H, style:{} as CSSStyleDeclaration,
      addEventListener:()=>{}, removeEventListener:()=>{},
      clientHeight:H, getContext:()=>gl as any,
    };

    const renderer = new THREE.WebGLRenderer({
      canvas, context: gl as unknown as WebGLRenderingContext,
      antialias: false, alpha: true,
    });
    renderer.setSize(W, H);
    renderer.setPixelRatio(1);
    renderer.setClearColor(0x000000, 0);
    renderer.setClearAlpha(0);
    // Ensure GL buffer is cleared to fully transparent each frame
    const glCtx = gl as any;
    glCtx.clearColor(0, 0, 0, 0);

    const scene  = new THREE.Scene();
    scene.background = null; // critical — any colour value kills camera transparency
    // No fog — FogExp2 has a colour that bleeds into the transparent background

    const camera = new THREE.PerspectiveCamera(70, W/H, 0.01, 20);
    camera.position.set(0,0,0);

    const glowTex = makeGlowTex();

    // No ambient light — field lines use AdditiveBlending and don't need lighting.
    // Pole spheres use emissive material which works without lights.

    // ══════════════════════════════════════════════════════════════════════════
    // FIELD 1 — MAGNETIC DIPOLE
    // ══════════════════════════════════════════════════════════════════════════
    const magGroup = new THREE.Group();
    scene.add(magGroup);

    // Each field line = 3 Line objects (core/mid/outer glow passes)
    const fieldLineSets: THREE.Line[][] = [];
    const highlightNodes: { mesh: THREE.Sprite; lineIndex: number; phase: number }[] = [];

    // Azimuthal density distribution: arcsin(sqrt(i/n))*2 for natural clustering
    const nL = MAG_L_VALUES.length;
    for (let li = 0; li < nL; li++) {
      const L = MAG_L_VALUES[li];
      for (let az = 0; az < MAG_AZ_COUNT; az++) {
        const frac = (li * MAG_AZ_COUNT + az) / (nL * MAG_AZ_COUNT - 1);
        const phi  = Math.asin(Math.sqrt(frac)) * 2 * Math.PI * (az === 0 ? 1 : -1);
        const pts  = dipolePoints(L, phi, MAG_CURVE_PTS);
        const glowSet = buildGlowLines(pts);
        glowSet.forEach(l => magGroup.add(l));
        fieldLineSets.push(glowSet);

        // Highlight travelling nodes
        for (let h = 0; h < HIGHLIGHT_PER_LINE; h++) {
          const hMat = new THREE.SpriteMaterial({
            map: glowTex, color: new THREE.Color('#FFFFFF'),
            transparent: true, opacity: 0.9,
            blending: THREE.AdditiveBlending, depthWrite: false,
          });
          const hMesh = new THREE.Sprite(hMat);
          hMesh.scale.set(0.04, 0.04, 1);
          magGroup.add(hMesh);
          highlightNodes.push({ mesh: hMesh, lineIndex: fieldLineSets.length - 1, phase: h / HIGHLIGHT_PER_LINE });
        }
      }
    }

    // Pole spheres — 0.03 radius, additive, marble-scale
    const poleGeo = new THREE.SphereGeometry(0.03, 12, 8);
    const northMat = new THREE.MeshBasicMaterial({
      color: 0x0044FF, transparent: true, opacity: 0.65,
      blending: THREE.AdditiveBlending, depthWrite: false,
    });
    const southMat = new THREE.MeshBasicMaterial({
      color: 0xFF2200, transparent: true, opacity: 0.65,
      blending: THREE.AdditiveBlending, depthWrite: false,
    });
    const northPole = new THREE.Mesh(poleGeo, northMat);
    const southPole = new THREE.Mesh(poleGeo, southMat);
    // Initialise at non-zero positions to prevent screen-centre render before data
    northPole.position.set(0,  0.3, 0);
    southPole.position.set(0, -0.3, 0);
    magGroup.add(northPole, southPole);

    // Small halo sprite — 0.09 scale max (not 0.12)
    const nHaloMat = new THREE.SpriteMaterial({ map: glowTex, color: new THREE.Color('#0044FF'), transparent: true, opacity: 0.20, blending: THREE.AdditiveBlending, depthWrite: false });
    const sHaloMat = new THREE.SpriteMaterial({ map: glowTex, color: new THREE.Color('#FF2200'), transparent: true, opacity: 0.20, blending: THREE.AdditiveBlending, depthWrite: false });
    const nHalo = new THREE.Sprite(nHaloMat); nHalo.scale.set(0.09, 0.09, 1); northPole.add(nHalo);
    const sHalo = new THREE.Sprite(sHaloMat); sHalo.scale.set(0.09, 0.09, 1); southPole.add(sHalo);

    // Precompute curve points arrays for highlight animation
    const lineCurves: THREE.Vector3[][] = MAG_L_VALUES.flatMap((L, li) =>
      Array.from({ length: MAG_AZ_COUNT }, (_, az) => {
        const frac = (li * MAG_AZ_COUNT + az) / (nL * MAG_AZ_COUNT - 1);
        const phi  = Math.asin(Math.sqrt(frac)) * 2 * Math.PI * (az === 0 ? 1 : -1);
        const pts  = dipolePoints(L, phi, MAG_CURVE_PTS);
        const curve = new THREE.CatmullRomCurve3(pts);
        return curve.getPoints(MAG_CURVE_PTS);
      })
    );

    // No large lens-flare sprites — pole halos (0.09 scale) are the only glow

    // ══════════════════════════════════════════════════════════════════════════
    // FIELD 2 — RF WAVEFRONT SHELLS
    // ══════════════════════════════════════════════════════════════════════════
    const rfGroup = new THREE.Group();
    scene.add(rfGroup);

    interface ShellEntry {
      mesh: THREE.Mesh;
      phase: number;       // current expansion radius (0 → maxRadius)
      maxRadius: number;
      expandRate: number;
      sourcePos: THREE.Vector3;
      color: number;
    }

    let shellEntries: ShellEntry[] = [];
    let interferenceParts: THREE.Points | null = null;
    let surfaceEchoGroup: THREE.Group | null = null;

    function rebuildRFShells(networks: RFNetwork[], headingDeg: number) {
      rfGroup.clear();
      shellEntries = [];
      interferenceParts = null;

      const headingRad = (headingDeg * Math.PI) / 180;
      const limited = networks.slice(0, RF_MAX_NETWORKS);

      const sourcePosArr: THREE.Vector3[] = [];

      for (const net of limited) {
        // Signal depth: strong=close, weak=far
        const depth   = 0.5 + (1 - net.intensity) * 1.8;
        const angle   = ((net.pseudoAngle * Math.PI / 180) + headingRad) % (Math.PI * 2);
        const srcPos  = new THREE.Vector3(Math.sin(angle) * depth * 0.5, 0, -depth);
        sourcePosArr.push(srcPos.clone());

        // Colour by frequency
        let col = new THREE.Color(Colors.gold);
        if (net.frequency > 4900)      col = new THREE.Color('#FFDDAA'); // 5GHz
        else if (net.frequency === 0)  col = new THREE.Color('#FF6600'); // cellular

        // 6 rings evenly phased — expanding halos, not filled volumes
        const RING_COUNT = 6;
        const MAX_RING_RADIUS = 1.8;
        // Shell spacing maps to WiFi frequency (wavelength difference)
        const phaseSpacing = net.frequency > 4900 ? MAX_RING_RADIUS / 8 : MAX_RING_RADIUS / 5;

        for (let s = 0; s < RING_COUNT; s++) {
          // RingGeometry: thin annulus — inner 94% of outer
          const ringGeo = new THREE.RingGeometry(0.001 * 0.94, 0.001, 64);
          const ringMat = new THREE.MeshBasicMaterial({
            color: col,
            transparent: true,
            opacity: 0.06,   // 6 rings stack to ~0.28 at any point — visible but not flooding
            side: THREE.DoubleSide,
            blending: THREE.AdditiveBlending,
            depthWrite: false,
          });
          const ring = new THREE.Mesh(ringGeo, ringMat);
          ring.position.copy(srcPos);
          rfGroup.add(ring);

          shellEntries.push({
            mesh: ring,
            phase: (s / RING_COUNT) * MAX_RING_RADIUS + phaseSpacing * s * 0.1,
            maxRadius: MAX_RING_RADIUS,
            expandRate: 0.008 + net.intensity * 0.005,
            sourcePos: srcPos.clone(),
            color: col.getHex(),
          });
        }

        // Volumetric source glow
        const vMat = new THREE.SpriteMaterial({
          map: glowTex, color: new THREE.Color('#442200'),
          transparent: true, opacity: Math.min(0.12, net.intensity * 0.15),
          blending: THREE.AdditiveBlending, depthWrite: false,
        });
        const vSprite = new THREE.Sprite(vMat);
        vSprite.position.copy(srcPos);
        const vSz = 0.1 + net.intensity * 0.35;
        vSprite.scale.set(vSz, vSz, 1);
        rfGroup.add(vSprite);
      }

      // Interference particles between first two networks
      if (sourcePosArr.length >= 2) {
        const sA = sourcePosArr[0], sB = sourcePosArr[1];
        const midpoint = new THREE.Vector3().addVectors(sA, sB).multiplyScalar(0.5);
        const bisector = new THREE.Vector3().subVectors(sB, sA).normalize();
        const perp1    = new THREE.Vector3().crossVectors(bisector, new THREE.Vector3(0,1,0)).normalize();
        const perp2    = new THREE.Vector3().crossVectors(bisector, perp1).normalize();

        const iPos = new Float32Array(INTERFERENCE_PTS * 3);
        for (let i = 0; i < INTERFERENCE_PTS; i++) {
          const u = (Math.random() - 0.5) * 1.2;
          const v = (Math.random() - 0.5) * 1.2;
          iPos[i*3]   = midpoint.x + perp1.x*u + perp2.x*v;
          iPos[i*3+1] = midpoint.y + perp1.y*u + perp2.y*v;
          iPos[i*3+2] = midpoint.z + perp1.z*u + perp2.z*v;
        }
        const iGeo  = new THREE.BufferGeometry();
        iGeo.setAttribute('position', new THREE.BufferAttribute(iPos, 3));
        const iMat  = new THREE.PointsMaterial({
          map: glowTex, color: new THREE.Color(Colors.gold),
          size: 0.04, transparent: true, opacity: 0.15,
          blending: THREE.AdditiveBlending, depthWrite: false, sizeAttenuation: true,
        });
        interferenceParts = new THREE.Points(iGeo, iMat);
        rfGroup.add(interferenceParts);
      }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FIELD 3 — GRAVITY
    // ══════════════════════════════════════════════════════════════════════════
    const gravGroup = new THREE.Group();
    scene.add(gravGroup);

    // Deforming wireframe mesh
    const GW = GRAV_GRID_W, GH = GRAV_GRID_H;
    const gridPosBase = new Float32Array(GW * GH * 3);
    const gridPosCurr = new Float32Array(GW * GH * 3);
    const gridIndices: number[] = [];

    for (let row = 0; row < GH; row++) {
      for (let col = 0; col < GW; col++) {
        const i = (row * GW + col) * 3;
        gridPosBase[i]   = (col / (GW-1) - 0.5) * 4.0;
        gridPosBase[i+1] = (row / (GH-1) - 0.5) * 2.4;
        gridPosBase[i+2] = -1.8;
        gridPosCurr[i] = gridPosBase[i];
        gridPosCurr[i+1] = gridPosBase[i+1];
        gridPosCurr[i+2] = gridPosBase[i+2];
        if (col < GW-1) { gridIndices.push(row*GW+col, row*GW+col+1); }
        if (row < GH-1) { gridIndices.push(row*GW+col, (row+1)*GW+col); }
      }
    }
    const gridGeo = new THREE.BufferGeometry();
    gridGeo.setAttribute('position', new THREE.BufferAttribute(gridPosCurr, 3));
    gridGeo.setIndex(new THREE.BufferAttribute(new Uint16Array(gridIndices), 1));
    const gridMat = new THREE.LineBasicMaterial({
      color: new THREE.Color('#001433'), transparent: true, opacity: 0.06,
      depthWrite: false, blending: THREE.AdditiveBlending,
    });
    const gravMesh = new THREE.LineSegments(gridGeo, gridMat);
    gravGroup.add(gravMesh);

    // Gravity particles
    let gravParticleCount = GRAV_PARTICLES_FULL;
    const gravPosArr  = new Float32Array(GRAV_PARTICLES_FULL * 3);
    const gravVelArr  = new Float32Array(GRAV_PARTICLES_FULL * 3);

    for (let i = 0; i < GRAV_PARTICLES_FULL; i++) {
      gravPosArr[i*3]   = (Math.random()-0.5)*4;
      gravPosArr[i*3+1] = (Math.random()-0.5)*6;
      gravPosArr[i*3+2] = -(0.5 + Math.random()*3);
      gravVelArr[i*3]=gravVelArr[i*3+1]=gravVelArr[i*3+2]=0;
    }
    const gravPartGeo = new THREE.BufferGeometry();
    gravPartGeo.setAttribute('position', new THREE.BufferAttribute(gravPosArr.slice(), 3));
    const gravPartMat = new THREE.PointsMaterial({
      map: glowTex, color: new THREE.Color('#002266'),
      size: 0.8, sizeAttenuation: true, transparent: true, opacity: 0.04,
      blending: THREE.AdditiveBlending, depthWrite: false,
    });
    const gravParticles = new THREE.Points(gravPartGeo, gravPartMat);
    gravGroup.add(gravParticles);

    // ── Scene refs ─────────────────────────────────────────────────────────────
    const refs: any = {
      renderer, scene, camera,
      magGroup, fieldLineSets, highlightNodes, lineCurves, northPole, southPole,
      rfGroup, shellEntries: [] as ShellEntry[], interferenceParts: null as THREE.Points | null,
      gravGroup, gravMesh, gravParticles, gravPartGeo, gridPosBase, gridPosCurr,
      gravPosArr, gravVelArr, gravParticleCount,
      animFrame: null as number | null, tick: 0, lastMs: Date.now(),
      showMag: true, showRF: true, showGrav: false,
      currentQuat: new THREE.Quaternion(),
      prevAxisQuat: new THREE.Quaternion(),
      anomalyCompression: 0.0,
      lineOpacity: 0.50,
      convergenceFired: false,
      lastConvergenceMs: 0,      // cooldown for convergence firing
      smoothDt: 16,              // EMA frame time for perf mode
      perfMode: false,           // low-end performance mode
      perfModeGoodFrames: 0,     // frames above 50fps before exiting perf mode
    };
    sceneRef.current = refs;

    // Gravity off by default
    gravGroup.visible = false;

    // Expose layer handle
    if (layerRef) {
      layerRef.current = {
        setLayers: (mag, rf, grav) => {
          refs.showMag = mag; refs.showRF = rf; refs.showGrav = grav;
          magGroup.visible = mag; rfGroup.visible = rf; gravGroup.visible = grav;
        },
      };
    }

    let rfRebuildPending = true;
    let lastHeadingForRF = 0;

    // ── Animation loop ─────────────────────────────────────────────────────────
    function animate() {
      refs.animFrame = requestAnimationFrame(animate);
      const now = Date.now();
      const dtMs = Math.min(now - refs.lastMs, 50);
      refs.lastMs = now;
      refs.tick += 1;

      // EMA frame time → perf mode detection
      refs.smoothDt = refs.smoothDt * 0.95 + dtMs * 0.05;
      if (!refs.perfMode && refs.smoothDt > 28) {
        refs.perfMode = true;
        refs.perfModeGoodFrames = 0;
      } else if (refs.perfMode && refs.smoothDt < 20) {
        refs.perfModeGoodFrames = (refs.perfModeGoodFrames || 0) + 1;
        if (refs.perfModeGoodFrames > 150) refs.perfMode = false; // ~3s at 50fps
      }

      // Reduce gravity particles when slow
      if (refs.smoothDt > FRAME_SLOW_MS && refs.gravParticleCount === GRAV_PARTICLES_FULL) {
        refs.gravParticleCount = GRAV_PARTICLES_REDUCED;
      }

      const { reading, isAnomaly } = storeRef.current;
      const { x: mx, y: my, z: mz, magnitude, heading } = reading;
      const { x: gx, y: gy, z: gz } = gravRef.current;

      // ── MAG FIELD ────────────────────────────────────────────────────────────
      if (refs.showMag) {
        // Rotate dipole toward live magnetic axis
        const targetQ = magAxisQuat(mx, my, mz);
        const angleDiff = refs.prevAxisQuat.angleTo(targetQ) * (180/Math.PI);
        if (angleDiff > 3) refs.prevAxisQuat.copy(targetQ);
        refs.currentQuat.slerp(targetQ, 0.035);
        magGroup.quaternion.copy(refs.currentQuat);

        // Anomaly compression
        const compTarget = isAnomaly ? 1.0 : 0.0;
        refs.anomalyCompression += (compTarget - refs.anomalyCompression) * 0.04;
        const comp = refs.anomalyCompression;

        // Adaptive line opacity: dimmer outdoors — capped at 0.50 per spec
        const envTarget = magnitude > 80 ? 0.25 : magnitude > 40 ? 0.38 : 0.50;
        refs.lineOpacity = (refs.lineOpacity ?? 0.50) + (envTarget - (refs.lineOpacity ?? 0.50)) * 0.02;
        const lo = refs.lineOpacity as number;

        // Scale and colour field lines; in perf mode render core pass only
        const col = getMagColor(magnitude, isAnomaly);
        refs.fieldLineSets.forEach((set: THREE.Line[], i: number) => {
          const lFrac = i / (refs.fieldLineSets.length - 1);
          const inner = lFrac < 0.4;
          const compScale = inner ? (1 - comp * 0.5) : (1 + comp * 0.25);
          set.forEach((line, pass) => {
            // In perf mode: only render core (pass 0), hide glow passes
            if (refs.perfMode && pass > 0) { line.visible = false; return; }
            line.visible = true;
            line.scale.setScalar(compScale);
            const mat = line.material as THREE.LineBasicMaterial;
            if (pass === 0) mat.color.copy(col);
            const passMultiplier = pass === 0 ? 1.0 : pass === 1 ? 0.40 : 0.18;
            mat.opacity = isAnomaly
              ? (pass === 0 ? 0.50 : pass === 1 ? 0.20 : 0.08)
              : lo * passMultiplier;
            mat.needsUpdate = true;
          });
        });

        // Pole position (±L_max*pole_offset along Y in local space)
        const poleY = MAG_L_VALUES[MAG_L_VALUES.length-1] * 0.12;
        northPole.position.set(0,  poleY, 0);
        southPole.position.set(0, -poleY, 0);

        // Pole halo opacity scales with camera dot product (stronger when facing camera)
        const camDir = new THREE.Vector3(0,0,-1);
        const nWorld = northPole.position.clone().applyQuaternion(refs.currentQuat).normalize();
        const nDot = Math.max(0, nWorld.dot(camDir));
        (northPole.children[0] as THREE.Sprite).material.opacity = nDot * 0.20;

        // Travelling highlight nodes
        const speed = Math.max(0.3, Math.min(3.0, magnitude / 20)) * (isAnomaly ? 2.0 : 1.0);
        refs.highlightNodes.forEach((hn: any) => {
          hn.phase = (hn.phase + speed * 0.004) % 1;
          const curvePts = refs.lineCurves[Math.min(hn.lineIndex, refs.lineCurves.length-1)];
          const idx = Math.min(Math.floor(hn.phase * curvePts.length), curvePts.length-1);
          const pos = curvePts[idx];
          const worldPos = pos.clone().applyQuaternion(refs.currentQuat);
          hn.mesh.position.copy(worldPos);
          const col = getMagColor(magnitude, isAnomaly);
          (hn.mesh.material as THREE.SpriteMaterial).color.copy(col);
          (hn.mesh.material as THREE.SpriteMaterial).opacity = isAnomaly ? 0.95 : 0.7;
        });
      }

      // ── RF FIELD ─────────────────────────────────────────────────────────────
      if (refs.showRF) {
        // Rebuild shell pool when heading shifts or networks change
        const headingDrift = Math.abs(heading - lastHeadingForRF);
        if (rfRebuildPending || headingDrift > 15) {
          rebuildRFShells(rfRef.current, heading);
          refs.shellEntries = shellEntries;
          refs.interferenceParts = interferenceParts;
          rfRebuildPending = false;
          lastHeadingForRF = heading;
        }

        // Expand rings — lookAt throttled to every 6 frames (cheap enough)
        const allShells = refs.shellEntries as ShellEntry[];
        // In perf mode: limit to 4 rings per source
        const maxShells = refs.perfMode ? allShells.length * (4 / RF_SHELLS_PER_NET) : allShells.length;
        for (let si = 0; si < allShells.length; si++) {
          const se = allShells[si];
          if (si >= maxShells) { se.mesh.visible = false; continue; }
          se.mesh.visible = true;
          se.phase += se.expandRate;
          if (se.phase > se.maxRadius) se.phase = 0;
          const r = Math.max(0.001, se.phase);
          se.mesh.scale.setScalar(r);
          se.mesh.position.copy(se.sourcePos);
          if (refs.tick % RF_LOOKATA_EVERY === 0) se.mesh.lookAt(camera.position);
          const fade = Math.max(0, 1 - r / se.maxRadius);
          (se.mesh.material as THREE.MeshBasicMaterial).opacity = 0.06 * fade;
        }

        // Interference flicker
        if (refs.interferenceParts) {
          (refs.interferenceParts.material as THREE.PointsMaterial).opacity =
            0.10 + Math.sin(refs.tick * 0.12) * 0.05;
        }

        // Surface echo: Z-axis spike = facing wall
        const facingWall = Math.abs(mz) / (Math.sqrt(mx*mx+my*my+mz*mz)||1) > 0.7;
        if (surfaceEchoGroup) { rfGroup.remove(surfaceEchoGroup); surfaceEchoGroup = null; }
        if (facingWall && rfRef.current.length > 0) {
          surfaceEchoGroup = new THREE.Group();
          const echoMat = new THREE.MeshBasicMaterial({
            color: new THREE.Color('#FF6600'), transparent: true, opacity: 0.18,
            wireframe: false, depthWrite: false, blending: THREE.AdditiveBlending, side: THREE.DoubleSide,
          });
          for (let s = 0; s < 3; s++) {
            const sg = new THREE.SphereGeometry(0.3 + s * 0.4, 12, 8);
            const sm = new THREE.Mesh(sg, echoMat.clone());
            sm.position.set(0, 0, -1.5);
            surfaceEchoGroup.add(sm);
          }
          rfGroup.add(surfaceEchoGroup);
        }
      }

      // ── AXIS CONVERGENCE (always checked, not gated on gravity being visible) ──
      {
        const gLen = Math.sqrt(gx*gx+gy*gy+gz*gz)||1;
        const magLen = Math.sqrt(mx*mx+my*my+mz*mz)||1;
        const dot = Math.abs((gx/gLen)*(mx/magLen)+(gy/gLen)*(my/magLen)+(gz/gLen)*(mz/magLen));
        const angleDeg = Math.acos(Math.min(1, dot)) * (180/Math.PI);
        // Strict 4° threshold + 45s cooldown to prevent constant firing
        const converging = angleDeg < 4;
        if (converging && !refs.convergenceFired &&
            (now - refs.lastConvergenceMs) > 45000) {
          refs.convergenceFired = true;
          refs.lastConvergenceMs = now;
          onConvergence?.(true);
        } else if (!converging && refs.convergenceFired) {
          refs.convergenceFired = false;
          onConvergence?.(false);
        }
      }

      // ── GRAVITY FIELD ────────────────────────────────────────────────────────
      if (refs.showGrav) {
        const gLen = Math.sqrt(gx*gx+gy*gy+gz*gz)||1;
        const gdx = gx/gLen, gdy = gy/gLen, gdz = gz/gLen;
        const t = refs.tick * 0.018;

        // Deform grid vertices
        const posAttr = refs.gravMesh.geometry.attributes.position as THREE.BufferAttribute;
        const arr = posAttr.array as Float32Array;
        const base = refs.gridPosBase;
        const n = (GRAV_GRID_W * GRAV_GRID_H);
        for (let vi = 0; vi < n; vi++) {
          const bx = base[vi*3], by = base[vi*3+1], bz = base[vi*3+2];
          const dist = Math.sqrt(bx*bx+by*by);
          const wave = Math.sin(t + dist * 1.8) * 0.10 * (1 + Math.abs(by) * 0.5);
          arr[vi*3]   = bx + gdx * wave;
          arr[vi*3+1] = by + gdy * wave;
          arr[vi*3+2] = bz;
        }
        posAttr.needsUpdate = true;

        const gridMaterial = refs.gravMesh.material as THREE.LineBasicMaterial;
        gridMaterial.opacity = refs.convergenceFired ? 0.18 : 0.06;
        gridMaterial.color.setStyle(refs.convergenceFired ? Colors.cyan : '#001433');
        gridMaterial.needsUpdate = true;

        // Skip particles in perf mode entirely
        if (!refs.perfMode) {
          const partPos = refs.gravPartGeo.attributes.position as THREE.BufferAttribute;
          const pArr = partPos.array as Float32Array;
          const vel  = refs.gravVelArr as Float32Array;
          const count = refs.gravParticleCount;
          const terminalSq = 0.08 * 0.08;

          for (let i = 0; i < count; i++) {
            const ix=i*3, iy=ix+1, iz=ix+2;
            vel[ix] += gdx*0.015; vel[iy] += gdy*0.015; vel[iz] += gdz*0.015;
            const vSq = vel[ix]**2+vel[iy]**2+vel[iz]**2;
            if (vSq > terminalSq) {
              const vS = Math.sqrt(vSq);
              vel[ix]/=vS*0.08; vel[iy]/=vS*0.08; vel[iz]/=vS*0.08;
            }
            pArr[ix]+=vel[ix]; pArr[iy]+=vel[iy]; pArr[iz]+=vel[iz];
            if (Math.abs(pArr[ix])>2.5||Math.abs(pArr[iy])>4||pArr[iz]>0||pArr[iz]<-4) {
              pArr[ix]=(Math.random()-0.5)*4;
              pArr[iy]=(Math.random()-0.5)*6;
              pArr[iz]=-(0.5+Math.random()*3);
              vel[ix]=vel[iy]=vel[iz]=0;
            }
          }
          partPos.needsUpdate = true;
          (refs.gravParticles.material as THREE.PointsMaterial).opacity = 0.04;
        }
      }

      renderer.render(scene, camera);
      gl.endFrameEXP();
    }

    // Initial RF build
    rebuildRFShells(rfRef.current, storeRef.current.reading.heading);
    refs.shellEntries = shellEntries;
    refs.interferenceParts = interferenceParts;

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
      <GLView
        style={styles.gl}
        onContextCreate={onContextCreate}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'transparent',  // no tint on any RN layer
    opacity: 1.0,  // opacity handled per-material via AdditiveBlending + alpha
  },
  gl: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'transparent',
  },
});
