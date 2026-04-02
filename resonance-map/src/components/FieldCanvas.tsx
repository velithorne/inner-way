import React, { useRef, useCallback, useEffect } from 'react';
import { View, StyleSheet } from 'react-native';
import { GLView, ExpoWebGLRenderingContext } from 'expo-gl';
import * as THREE from 'three';
import { useFieldStore } from '../store/useFieldStore';
import { Colors } from '../constants/theme';
import { FIELD_WEAK_MAX, FIELD_NORMAL_MAX } from '../constants/thresholds';

const NUM_FIELD_LINES = 24;
const NUM_STARS = 200;

function getFieldColor(magnitude: number, isAnomaly: boolean): THREE.Color {
  if (isAnomaly) return new THREE.Color(Colors.fieldAnomaly);
  if (magnitude < FIELD_WEAK_MAX) return new THREE.Color(Colors.fieldWeak);
  if (magnitude < FIELD_NORMAL_MAX) return new THREE.Color(Colors.fieldNormal);
  return new THREE.Color(Colors.fieldStrong);
}

interface SceneRefs {
  renderer: THREE.WebGLRenderer;
  scene: THREE.Scene;
  camera: THREE.PerspectiveCamera;
  sphere: THREE.Mesh;
  fieldLines: THREE.Line[];
  pulseMaterial: THREE.LineBasicMaterial[];
  stars: THREE.Points;
  animFrame: number | null;
  cameraAngle: number;
  particleSystem: THREE.Points | null;
  particleMaterial: THREE.PointsMaterial | null;
  anomalyParticleActive: boolean;
  anomalyParticleTimer: number;
}

export default function FieldCanvas() {
  const sceneRef = useRef<SceneRefs | null>(null);
  const storeRef = useRef(useFieldStore.getState());

  useEffect(() => {
    const unsub = useFieldStore.subscribe((state) => {
      storeRef.current = state;
    });
    return unsub;
  }, []);

  const onContextCreate = useCallback(async (gl: ExpoWebGLRenderingContext) => {
    const { drawingBufferWidth: width, drawingBufferHeight: height } = gl;

    // Renderer — expo-gl shim for Three.js canvas
    /* eslint-disable @typescript-eslint/ban-ts-comment */
    // @ts-ignore
    const canvasShim: HTMLCanvasElement = {
      width,
      height,
      // @ts-ignore
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
    });
    renderer.setSize(width, height);
    renderer.setClearColor(new THREE.Color(Colors.background), 1);

    // Scene
    const scene = new THREE.Scene();

    // Camera
    const camera = new THREE.PerspectiveCamera(60, width / height, 0.1, 1000);
    camera.position.set(0, 0, 6);
    camera.lookAt(0, 0, 0);

    // Stars
    const starGeo = new THREE.BufferGeometry();
    const starPositions = new Float32Array(NUM_STARS * 3);
    for (let i = 0; i < NUM_STARS; i++) {
      starPositions[i * 3] = (Math.random() - 0.5) * 60;
      starPositions[i * 3 + 1] = (Math.random() - 0.5) * 60;
      starPositions[i * 3 + 2] = (Math.random() - 0.5) * 60 - 10;
    }
    starGeo.setAttribute('position', new THREE.BufferAttribute(starPositions, 3));
    const starMat = new THREE.PointsMaterial({ color: 0x334466, size: 0.06 });
    const stars = new THREE.Points(starGeo, starMat);
    scene.add(stars);

    // Earth sphere
    const sphereGeo = new THREE.SphereGeometry(0.7, 32, 32);
    const sphereMat = new THREE.MeshPhongMaterial({
      color: new THREE.Color('#010820'),
      emissive: new THREE.Color('#001850'),
      emissiveIntensity: 0.6,
      specular: new THREE.Color('#0044AA'),
      shininess: 40,
    });
    const sphere = new THREE.Mesh(sphereGeo, sphereMat);
    scene.add(sphere);

    // Ambient + point lights
    const ambient = new THREE.AmbientLight(0x111133, 1.5);
    scene.add(ambient);
    const pointLight = new THREE.PointLight(0x0044ff, 2, 20);
    pointLight.position.set(3, 3, 3);
    scene.add(pointLight);

    // Field lines
    const fieldLines: THREE.Line[] = [];
    const pulseMaterial: THREE.LineBasicMaterial[] = [];

    for (let i = 0; i < NUM_FIELD_LINES; i++) {
      const phi = Math.acos(-1 + (2 * i) / NUM_FIELD_LINES);
      const theta = Math.sqrt(NUM_FIELD_LINES * Math.PI) * phi;

      const points: THREE.Vector3[] = [];
      const segments = 20;
      for (let s = 0; s <= segments; s++) {
        const t = s / segments;
        const r = 0.7 + t * 2.8;
        const x = r * Math.sin(phi) * Math.cos(theta);
        const y = r * Math.cos(phi);
        const z = r * Math.sin(phi) * Math.sin(theta);
        points.push(new THREE.Vector3(x, y, z));
      }

      const geo = new THREE.BufferGeometry().setFromPoints(points);
      const mat = new THREE.LineBasicMaterial({
        color: new THREE.Color(Colors.fieldNormal),
        transparent: true,
        opacity: 0.6,
      });
      pulseMaterial.push(mat);
      const line = new THREE.Line(geo, mat);
      scene.add(line);
      fieldLines.push(line);
    }

    // Particle system for anomaly burst
    const particleGeo = new THREE.BufferGeometry();
    const particleCount = 120;
    const pPos = new Float32Array(particleCount * 3);
    for (let i = 0; i < particleCount; i++) {
      pPos[i * 3] = 0;
      pPos[i * 3 + 1] = 0;
      pPos[i * 3 + 2] = 0;
    }
    particleGeo.setAttribute('position', new THREE.BufferAttribute(pPos, 3));
    const particleMat = new THREE.PointsMaterial({
      color: new THREE.Color(Colors.gold),
      size: 0.08,
      transparent: true,
      opacity: 0,
    });
    const particleSystem = new THREE.Points(particleGeo, particleMat);
    scene.add(particleSystem);

    const refs: SceneRefs = {
      renderer,
      scene,
      camera,
      sphere,
      fieldLines,
      pulseMaterial,
      stars,
      animFrame: null,
      cameraAngle: 0,
      particleSystem,
      particleMaterial: particleMat,
      anomalyParticleActive: false,
      anomalyParticleTimer: 0,
    };
    sceneRef.current = refs;

    let pulsePhase = 0;
    let prevIsAnomaly = false;
    const particleVelocities: THREE.Vector3[] = Array.from({ length: particleCount }, () =>
      new THREE.Vector3(
        (Math.random() - 0.5) * 0.15,
        (Math.random() - 0.5) * 0.15,
        (Math.random() - 0.5) * 0.15
      )
    );

    function animate() {
      refs.animFrame = requestAnimationFrame(animate);

      const { reading, isAnomaly, rollingAverage } = storeRef.current;
      const { magnitude, x: mx, y: my, z: mz } = reading;
      const speed = Math.max(0.5, Math.min(3.0, magnitude / 20));

      pulsePhase = (pulsePhase + speed * 0.02) % 1;

      // Slow sphere rotation
      sphere.rotation.y += 0.004;
      sphere.rotation.x += 0.001;

      // Camera orbit
      refs.cameraAngle += 0.003;
      camera.position.x = Math.sin(refs.cameraAngle) * 6;
      camera.position.z = Math.cos(refs.cameraAngle) * 6;
      camera.position.y = Math.sin(refs.cameraAngle * 0.3) * 1.5;
      camera.lookAt(0, 0, 0);

      // Field color
      const fieldColor = getFieldColor(magnitude, isAnomaly);

      // Compute vector direction from magnetometer reading
      const magLen = Math.sqrt(mx * mx + my * my + mz * mz) || 1;
      const dir = new THREE.Vector3(mx / magLen, mz / magLen, my / magLen);

      // Update field lines
      fieldLines.forEach((line, i) => {
        const phi = Math.acos(-1 + (2 * i) / NUM_FIELD_LINES);
        const theta = Math.sqrt(NUM_FIELD_LINES * Math.PI) * phi;

        const baseDir = new THREE.Vector3(
          Math.sin(phi) * Math.cos(theta),
          Math.cos(phi),
          Math.sin(phi) * Math.sin(theta)
        );

        // Blend base direction toward magnetometer direction
        const blended = baseDir.clone().lerp(dir, 0.3).normalize();

        const positions = (line.geometry as THREE.BufferGeometry).attributes.position as THREE.BufferAttribute;
        const segments = positions.count - 1;

        for (let s = 0; s <= segments; s++) {
          const t = s / segments;
          const r = 0.7 + t * 2.8;

          // Pulse effect: wave travels outward
          const pulseFactor = Math.sin((t - pulsePhase) * Math.PI * 2) * 0.5 + 0.5;
          const warpedDir = blended.clone().multiplyScalar(r);

          positions.setXYZ(
            s,
            warpedDir.x + (Math.random() - 0.5) * 0.02 * pulseFactor,
            warpedDir.y + (Math.random() - 0.5) * 0.02 * pulseFactor,
            warpedDir.z + (Math.random() - 0.5) * 0.02 * pulseFactor
          );
        }
        positions.needsUpdate = true;

        // Pulse opacity
        const mat = pulseMaterial[i];
        const pOffset = (i / NUM_FIELD_LINES + pulsePhase) % 1;
        const pulseOpacity = isAnomaly
          ? 0.6 + 0.4 * Math.sin(pOffset * Math.PI * 2)
          : 0.3 + 0.3 * Math.sin(pOffset * Math.PI * 2);

        mat.color.copy(fieldColor);
        mat.opacity = pulseOpacity;
      });

      // Sphere pulse on anomaly
      if (isAnomaly) {
        const sphereMaterial = sphere.material as THREE.MeshPhongMaterial;
        sphereMaterial.emissive.setStyle(Colors.gold);
        sphereMaterial.emissiveIntensity = 0.5 + 0.5 * Math.sin(Date.now() * 0.01);
      } else {
        const sphereMaterial = sphere.material as THREE.MeshPhongMaterial;
        sphereMaterial.emissive.setStyle('#001850');
        sphereMaterial.emissiveIntensity = 0.6;
      }

      // Anomaly particle burst
      if (isAnomaly && !prevIsAnomaly) {
        refs.anomalyParticleActive = true;
        refs.anomalyParticleTimer = 0;
        const pos = particleGeo.attributes.position as THREE.BufferAttribute;
        for (let i = 0; i < particleCount; i++) {
          pos.setXYZ(i, 0, 0, 0);
        }
        pos.needsUpdate = true;
        particleMat.opacity = 0.9;
      }
      prevIsAnomaly = isAnomaly;

      if (refs.anomalyParticleActive) {
        refs.anomalyParticleTimer += 1;
        const pos = particleGeo.attributes.position as THREE.BufferAttribute;
        for (let i = 0; i < particleCount; i++) {
          const v = particleVelocities[i];
          pos.setXYZ(
            i,
            pos.getX(i) + v.x,
            pos.getY(i) + v.y,
            pos.getZ(i) + v.z
          );
        }
        pos.needsUpdate = true;
        particleMat.opacity = Math.max(0, particleMat.opacity - 0.015);

        if (refs.anomalyParticleTimer > 60) {
          refs.anomalyParticleActive = false;
          particleMat.opacity = 0;
        }
      }

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
    <View style={styles.container}>
      <GLView style={styles.gl} onContextCreate={onContextCreate} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  gl: {
    flex: 1,
  },
});
