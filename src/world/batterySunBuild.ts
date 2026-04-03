import * as THREE from 'three';

import { WORLD } from './worldConstants';
import { batteryLevelToEmissive } from './batteryColors';

/** Order: Processor → Display → RAM → Storage → Network → Sensors (brightness priority) */
const TARGETS = [
  WORLD.processor,
  WORLD.display,
  WORLD.ramOcean,
  WORLD.storage,
  WORLD.networkSky,
  WORLD.sensors,
];

const RIVER_BASE_OPACITY = [0.62, 0.52, 0.42, 0.34, 0.38, 0.28];

function sunLightFromBattery(level: number): { color: THREE.Color; intensity: number } {
  const lvl = Math.max(0, Math.min(100, level));
  const stops: { t: number; hex: number; i: number }[] = [
    { t: 0, hex: 0x660000, i: 0.6 },
    { t: 5, hex: 0x880000, i: 1.0 },
    { t: 10, hex: 0xcc0000, i: 1.8 },
    { t: 17, hex: 0xff2200, i: 2.8 },
    { t: 25, hex: 0xff4400, i: 3.5 },
    { t: 50, hex: 0xff8800, i: 5.0 },
    { t: 75, hex: 0xffb700, i: 6.5 },
    { t: 100, hex: 0xffd700, i: 8.0 },
  ];
  for (let k = 0; k < stops.length - 1; k++) {
    const a = stops[k];
    const b = stops[k + 1];
    if (lvl >= a.t && lvl <= b.t) {
      const u = (lvl - a.t) / Math.max(1e-6, b.t - a.t);
      const c = new THREE.Color(a.hex).lerp(new THREE.Color(b.hex), u);
      const intensity = THREE.MathUtils.lerp(a.i, b.i, u) * 1.35;
      return { color: c, intensity };
    }
  }
  const last = stops[stops.length - 1];
  return { color: new THREE.Color(last.hex), intensity: last.i * 1.35 };
}

const plasmaVertex = `
varying vec3 vNorm;
varying vec3 vPos;
void main() {
  vNorm = normalize(normalMatrix * normal);
  vPos = position;
  gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
}
`;

const plasmaFragment = `
precision mediump float;
uniform vec3 uColor;
uniform float uTime;
uniform float uIntensity;
varying vec3 vNorm;
varying vec3 vPos;
float n3(vec3 p) {
  return fract(sin(dot(p, vec3(127.1, 311.7, 74.7))) * 43758.5453);
}
void main() {
  float t = uTime * 0.8;
  float a = n3(vPos * 4.0 + vec3(t, t * 0.7, t * 1.1));
  float b = n3(vPos * 8.0 - vec3(t * 1.2, t, t * 0.9));
  float flicker = 0.75 + 0.25 * (a * 0.6 + b * 0.4);
  vec3 em = uColor * flicker * uIntensity;
  float rim = pow(1.0 - abs(dot(vNorm, vec3(0.0, 0.0, 1.0))), 2.0);
  em += uColor * rim * 0.15;
  gl_FragColor = vec4(em, 1.0);
}
`;

export type BatterySunHandles = {
  group: THREE.Group;
  sunCore: THREE.Mesh;
  sunMat: THREE.ShaderMaterial;
  corona1: THREE.Mesh;
  corona2: THREE.Mesh;
  ring1: THREE.Mesh;
  ring2: THREE.Mesh;
  tendrils: THREE.Mesh[];
  rivers: THREE.Mesh[];
  riverGlows: THREE.Mesh[];
  riverCurves: THREE.CatmullRomCurve3[];
  riverPulses: { mesh: THREE.Mesh; t: number }[];
  sunLight: THREE.PointLight;
  /** Boost illumination at each power-river destination (world-relative to sun group at origin) */
  anchorLights: THREE.PointLight[];
  dispose: () => void;
};

export function createBatterySun(): BatterySunHandles {
  const group = new THREE.Group();

  const sunMat = new THREE.ShaderMaterial({
    uniforms: {
      uColor: { value: new THREE.Color(0xffa000) },
      uTime: { value: 0 },
      uIntensity: { value: 2 },
    },
    vertexShader: plasmaVertex,
    fragmentShader: plasmaFragment,
  });

  const geo = new THREE.SphereGeometry(18, 48, 48);
  const sunCore = new THREE.Mesh(geo, sunMat);
  sunCore.castShadow = true;
  group.add(sunCore);

  const c1 = new THREE.Mesh(
    new THREE.SphereGeometry(18 * 1.4, 32, 32),
    new THREE.MeshBasicMaterial({
      transparent: true,
      opacity: 0.06,
      depthWrite: false,
      blending: THREE.AdditiveBlending,
    }),
  );
  const c2 = new THREE.Mesh(
    new THREE.SphereGeometry(18 * 1.8, 24, 24),
    new THREE.MeshBasicMaterial({
      transparent: true,
      opacity: 0.03,
      depthWrite: false,
      blending: THREE.AdditiveBlending,
    }),
  );
  group.add(c1, c2);

  const r = 18 * 1.3;
  const ring1 = new THREE.Mesh(
    new THREE.TorusGeometry(r, 0.8, 12, 64),
    new THREE.MeshBasicMaterial({
      transparent: true,
      opacity: 0.6,
      depthWrite: false,
      blending: THREE.AdditiveBlending,
    }),
  );
  ring1.rotation.x = Math.PI / 2;
  const ring2 = ring1.clone();
  ring2.rotation.x = Math.PI / 2;
  ring2.rotation.z = Math.PI / 4;
  ring2.scale.setScalar(1.05);
  ;(ring2.material as THREE.MeshBasicMaterial).opacity = 0.4;
  group.add(ring1, ring2);

  const tendrils: THREE.Mesh[] = [];
  const tendMat = new THREE.MeshBasicMaterial({
    transparent: true,
    opacity: 0.35,
    blending: THREE.AdditiveBlending,
    depthWrite: false,
  });
  for (let i = 0; i < 8; i++) {
    const ang = (i / 8) * Math.PI * 2;
    const pts = [
      new THREE.Vector3(Math.cos(ang) * 12, Math.sin(ang * 2) * 4, Math.sin(ang) * 12),
      new THREE.Vector3(Math.cos(ang) * 22, 8 + (i % 3) * 3, Math.sin(ang) * 22),
      new THREE.Vector3(Math.cos(ang + 0.4) * 16, 2, Math.sin(ang + 0.4) * 16),
    ];
    const curve = new THREE.CatmullRomCurve3(pts);
    const tube = new THREE.TubeGeometry(curve, 16, 0.12, 6, false);
    const m = new THREE.Mesh(tube, tendMat.clone());
    tendrils.push(m);
    group.add(m);
  }

  const sunLight = new THREE.PointLight(0xffcc66, 3.5, 600, 1.2);
  sunLight.position.set(0, 0, 0);
  sunLight.castShadow = true;
  sunLight.shadow.mapSize.width = 1024;
  sunLight.shadow.mapSize.height = 1024;
  sunLight.shadow.camera.near = 0.5;
  sunLight.shadow.camera.far = 600;
  group.add(sunLight);

  const anchorLights: THREE.PointLight[] = [];
  for (let i = 0; i < TARGETS.length; i++) {
    const pl = new THREE.PointLight(0xffaa66, 0, 160, 1.5);
    pl.position.copy(TARGETS[i]);
    pl.castShadow = false;
    group.add(pl);
    anchorLights.push(pl);
  }

  const rivers: THREE.Mesh[] = [];
  const riverGlows: THREE.Mesh[] = [];
  const riverCurves: THREE.CatmullRomCurve3[] = [];
  const riverPulses: { mesh: THREE.Mesh; t: number }[] = [];

  const pulseGeo = new THREE.SphereGeometry(0.8, 12, 12);
  const pulseMat = new THREE.MeshBasicMaterial({
    color: 0xffffff,
    transparent: true,
    opacity: 0.95,
    blending: THREE.AdditiveBlending,
    depthWrite: false,
  });

  for (let i = 0; i < TARGETS.length; i++) {
    const end = TARGETS[i];
    const mid = new THREE.Vector3(
      (end.x * 0.5 + 0) / 2,
      Math.max(40, end.y * 0.5 + 25),
      (end.z * 0.5 + 0) / 2,
    );
    const pts = [new THREE.Vector3(0, 0, 0), mid, end.clone()];
    const curve = new THREE.CatmullRomCurve3(pts);
    riverCurves.push(curve);

    const tube = new THREE.TubeGeometry(curve, 20, 0.6, 8, false);
    const curveMat = new THREE.MeshBasicMaterial({
      transparent: true,
      opacity: RIVER_BASE_OPACITY[i] * 0.85,
      blending: THREE.AdditiveBlending,
      depthWrite: false,
    });
    const mesh = new THREE.Mesh(tube, curveMat);
    rivers.push(mesh);
    group.add(mesh);

    const glowTube = new THREE.TubeGeometry(curve, 20, 2.0, 8, false);
    const glowMat = new THREE.MeshBasicMaterial({
      transparent: true,
      opacity: 0.04,
      blending: THREE.AdditiveBlending,
      depthWrite: false,
    });
    const glow = new THREE.Mesh(glowTube, glowMat);
    riverGlows.push(glow);
    group.add(glow);

    const pulse = new THREE.Mesh(pulseGeo, pulseMat.clone());
    riverPulses.push({ mesh: pulse, t: i / TARGETS.length });
    group.add(pulse);
  }

  return {
    group,
    sunCore,
    sunMat,
    corona1: c1,
    corona2: c2,
    ring1,
    ring2,
    tendrils,
    rivers,
    riverGlows,
    riverCurves,
    riverPulses,
    sunLight,
    anchorLights,
    dispose: () => {
      geo.dispose();
      sunMat.dispose();
      c1.geometry.dispose();
      (c1.material as THREE.Material).dispose();
      c2.geometry.dispose();
      (c2.material as THREE.Material).dispose();
      ring1.geometry.dispose();
      (ring1.material as THREE.Material).dispose();
      ring2.geometry.dispose();
      (ring2.material as THREE.Material).dispose();
      tendrils.forEach((m) => {
        m.geometry.dispose();
        (m.material as THREE.Material).dispose();
      });
      rivers.forEach((m) => {
        m.geometry.dispose();
        (m.material as THREE.Material).dispose();
      });
      riverGlows.forEach((m) => {
        m.geometry.dispose();
        (m.material as THREE.Material).dispose();
      });
      riverPulses.forEach((p) => {
        p.mesh.geometry.dispose();
        (p.mesh.material as THREE.Material).dispose();
      });
      pulseGeo.dispose();
      pulseMat.dispose();
      anchorLights.forEach((l) => l.dispose());
    },
  };
}

export function updateBatterySun(
  h: BatterySunHandles,
  batteryLevel: number,
  powerWatts: number,
  time: number,
  reveal = 1,
): void {
  const lvl = Math.max(0, Math.min(100, batteryLevel));
  const r = 18 * (lvl / 100) * 0.6 + 7.2;
  h.sunCore.scale.setScalar(r / 18);
  h.corona1.scale.setScalar((r / 18) * 1.4);
  h.corona2.scale.setScalar((r / 18) * 1.8);

  const col = batteryLevelToEmissive(lvl);
  const sunParams = sunLightFromBattery(lvl);
  h.sunMat.uniforms.uColor.value.copy(col);
  h.sunMat.uniforms.uTime.value = time;
  h.sunMat.uniforms.uIntensity.value = 2;

  ;(h.corona1.material as THREE.MeshBasicMaterial).color.copy(col);
  ;(h.corona2.material as THREE.MeshBasicMaterial).color.copy(col);

  const rr = r * 1.3;
  h.ring1.scale.setScalar(rr / (18 * 1.3));
  h.ring2.scale.setScalar((rr * 1.05) / (18 * 1.3));

  h.ring1.rotation.z += 0.01;
  h.ring2.rotation.z -= 0.008;

  h.sunLight.color.copy(sunParams.color);
  h.sunLight.intensity = sunParams.intensity * reveal;

  h.anchorLights.forEach((pl, i) => {
    pl.color.copy(sunParams.color);
    const weight = RIVER_BASE_OPACITY[i];
    pl.intensity = sunParams.intensity * weight * 0.28 * reveal;
  });

  const pulse = (1 + Math.sin(time * Math.PI * 2 * 0.25) * 0.02) * reveal;
  h.group.scale.setScalar(Math.max(0.001, pulse));

  h.group.rotation.y += 0.002;

  h.tendrils.forEach((m, i) => {
    m.rotation.y = time * 0.15 + i * 0.4;
    const tm = m.material as THREE.MeshBasicMaterial;
    tm.color.copy(col);
    tm.opacity = 0.25 + (i % 3) * 0.05;
  });

  const pNorm = powerWatts > 0 ? Math.min(1, powerWatts / 8) : 0.05;
  const lowBat = 0.35 + (lvl / 100) * 0.65;
  const speedMul = 0.15 * lowBat * (0.5 + pNorm * 0.5);

  h.rivers.forEach((river, i) => {
    const m = river.material as THREE.MeshBasicMaterial;
    m.color.copy(col);
    const baseOp = RIVER_BASE_OPACITY[i];
    const netBoost = i === 4 ? 1 + pNorm * 0.4 : 1;
    m.opacity = Math.min(0.95, baseOp * (0.65 + pNorm * 0.45) * netBoost * reveal);
  });

  h.riverGlows.forEach((glow, i) => {
    const m = glow.material as THREE.MeshBasicMaterial;
    m.color.copy(col);
    m.opacity = 0.08 * reveal * (0.55 + (lvl / 100) * 0.45);
  });

  h.riverPulses.forEach((rp, i) => {
    rp.t = (rp.t + speedMul * (1 / 60)) % 1;
    const curve = h.riverCurves[i];
    const pt = curve.getPointAt(rp.t);
    rp.mesh.position.copy(pt);
    const pm = rp.mesh.material as THREE.MeshBasicMaterial;
    pm.color.setHex(0xffffee);
    pm.opacity = (0.55 + pNorm * 0.4) * (0.4 + (lvl / 100) * 0.6) * reveal;
  });
}
