import { useRef, Suspense } from 'react';
import { Canvas, useFrame } from '@react-three/fiber';
import { OrbitControls } from '@react-three/drei';
import * as THREE from 'three';
import { EarthMesh } from './EarthMesh';
import { AtmosphereLayer } from './AtmosphereLayer';
import { PulseSystem } from './PulseSystem';
import { StarField } from './StarField';

const GLOBE_RADIUS = 1.5;
const AUTO_ROTATION_SPEED = 0.0008;

function GlobeGroup() {
  const groupRef = useRef<THREE.Group>(null);

  useFrame(({ clock }) => {
    if (groupRef.current) {
      groupRef.current.rotation.y = clock.getElapsedTime() * AUTO_ROTATION_SPEED;
    }
  });

  return (
    <group ref={groupRef}>
      <EarthMesh radius={GLOBE_RADIUS} />
      <AtmosphereLayer radius={GLOBE_RADIUS} />
      <PulseSystem globeRadius={GLOBE_RADIUS} />
    </group>
  );
}

function SceneLights() {
  return (
    <>
      {/* Very low ambient — just enough to lift the shadow side above pure black */}
      <ambientLight intensity={0.06} color="#0d1a30" />
      {/* Primary sun — warm-white, strong enough to show continents clearly */}
      <directionalLight position={[5, 2, 3]} intensity={1.1} color="#c8d8f0" />
      {/* Cool blue fill from the dark side — makes night side visible */}
      <pointLight position={[-8, 1, -4]} intensity={0.18} color="#1a3a6a" />
      {/* Subtle warm back-scatter */}
      <pointLight position={[0, -4, -6]} intensity={0.06} color="#3a2510" />
    </>
  );
}

// Fallback sphere shown while textures load
function EarthFallback() {
  return (
    <mesh>
      <sphereGeometry args={[GLOBE_RADIUS, 64, 64]} />
      <meshStandardMaterial color="#050c1a" roughness={1} metalness={0} />
    </mesh>
  );
}

export function GlobeScene() {
  return (
    <Canvas
      camera={{ position: [0, 0, 4.0], fov: 40, near: 0.1, far: 500 }}
      gl={{
        antialias: true,
        alpha: false,
        powerPreference: 'high-performance',
        toneMapping: THREE.ACESFilmicToneMapping,
        toneMappingExposure: 1.0,
      }}
      style={{ background: '#02040a' }}
      dpr={[1, 2]}
    >
      <SceneLights />
      <StarField />
      <Suspense fallback={<EarthFallback />}>
        <GlobeGroup />
      </Suspense>
      <OrbitControls
        enablePan={false}
        enableZoom
        minDistance={2.4}
        maxDistance={9}
        zoomSpeed={0.4}
        rotateSpeed={0.32}
        dampingFactor={0.07}
        enableDamping
        makeDefault
      />
    </Canvas>
  );
}
