import { useRef } from 'react';
import { useFrame } from '@react-three/fiber';
import * as THREE from 'three';
import { earthVertexShader, earthFragmentShader } from '../../shaders/earth.glsl';
import { useWorldPulseStore } from '../../app/store';

interface EarthMeshProps {
  radius?: number;
}

export function EarthMesh({ radius = 1 }: EarthMeshProps) {
  const meshRef = useRef<THREE.Mesh>(null);
  const materialRef = useRef<THREE.ShaderMaterial>(null);
  const glowAccumulation = useWorldPulseStore((s) => s.glowAccumulation);

  useFrame(({ clock }) => {
    if (materialRef.current) {
      materialRef.current.uniforms.uTime.value = clock.getElapsedTime();
      materialRef.current.uniforms.uGlowAccumulation.value = THREE.MathUtils.lerp(
        materialRef.current.uniforms.uGlowAccumulation.value,
        glowAccumulation,
        0.05,
      );
    }
  });

  const uniforms = {
    uTime: { value: 0 },
    uGlowAccumulation: { value: 0 },
  };

  return (
    <mesh ref={meshRef}>
      <sphereGeometry args={[radius, 128, 128]} />
      <shaderMaterial
        ref={materialRef}
        vertexShader={earthVertexShader}
        fragmentShader={earthFragmentShader}
        uniforms={uniforms}
      />
    </mesh>
  );
}
