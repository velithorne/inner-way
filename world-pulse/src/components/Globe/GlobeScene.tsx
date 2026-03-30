import { useRef } from 'react';
import { Canvas, useFrame } from '@react-three/fiber';
import { OrbitControls } from '@react-three/drei';
import * as THREE from 'three';
import { EarthMesh } from './EarthMesh';
import { AtmosphereLayer } from './AtmosphereLayer';
import { PulseSystem } from './PulseSystem';
import { StarField } from './StarField';

const GLOBE_RADIUS = 1.5;
const AUTO_ROTATION_SPEED = 0.0012;

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
      {/* Ambient fill - very low */}
      <ambientLight intensity={0.04} color="#0a1628" />
      {/* Main sun light from upper right */}
      <directionalLight
        position={[4, 2, 3]}
        intensity={0.6}
        color="#8ab4d4"
      />
      {/* Subtle blue fill from the left */}
      <pointLight position={[-6, 0, -3]} intensity={0.15} color="#1a3a6a" />
      {/* Warm back light */}
      <pointLight position={[0, -3, -5]} intensity={0.08} color="#3a1a00" />
    </>
  );
}

export function GlobeScene() {
  return (
    <Canvas
      camera={{ position: [0, 0, 4.2], fov: 42, near: 0.1, far: 500 }}
      gl={{
        antialias: true,
        alpha: false,
        powerPreference: 'high-performance',
      }}
      style={{ background: '#02040a' }}
      dpr={[1, 2]}
    >
      <SceneLights />
      <StarField />
      <GlobeGroup />
      <OrbitControls
        enablePan={false}
        enableZoom
        minDistance={2.5}
        maxDistance={8}
        zoomSpeed={0.4}
        rotateSpeed={0.35}
        dampingFactor={0.08}
        enableDamping
        makeDefault
      />
    </Canvas>
  );
}
