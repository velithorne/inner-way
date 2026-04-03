import * as THREE from 'three';

import { WORLD } from './worldConstants';
import { batteryLevelToEmissive } from './batteryColors';

const TARGETS = [
  WORLD.processor,
  WORLD.ramOcean,
  WORLD.storage,
  WORLD.sensors,
  WORLD.networkSky,
  WORLD.display,
];

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
  sunLight: THREE.PointLight;
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

  const sunLight = new THREE.PointLight(0xffcc66, 1.2, 800, 2);
  sunLight.position.set(0, 0, 0);
  group.add(sunLight);

  const rivers: THREE.Mesh[] = [];
  const curveMat = new THREE.MeshBasicMaterial({
    transparent: true,
    opacity: 0.15,
    blending: THREE.AdditiveBlending,
    depthWrite: false,
  });

  for (let i = 0; i < TARGETS.length; i++) {
    const end = TARGETS[i];
    const pts = [
      new THREE.Vector3(0, 0, 0),
      new THREE.Vector3(end.x * 0.35, end.y * 0.35, end.z * 0.35),
      end.clone(),
    ];
    const curve = new THREE.CatmullRomCurve3(pts);
    const tube = new THREE.TubeGeometry(curve, 24, 0.3, 8, false);
    const mesh = new THREE.Mesh(tube, curveMat.clone());
    rivers.push(mesh);
    group.add(mesh);
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
    sunLight,
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

  h.sunLight.color.copy(col);
  h.sunLight.intensity = (0.6 + (lvl / 100) * 1.4) * reveal;

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
  const riverBright = 0.08 + pNorm * 0.35;
  h.rivers.forEach((river, i) => {
    const m = river.material as THREE.MeshBasicMaterial;
    m.color.copy(col);
    const boost = i === 0 ? 1.25 : i === 5 ? 1.1 : 1;
    m.opacity = Math.min(0.45, riverBright * boost);
  });
}
