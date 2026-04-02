import { useRef, useMemo } from 'react';
import { useFrame } from '@react-three/fiber';
import { useTexture } from '@react-three/drei';
import * as THREE from 'three';
import { earthVertexShader, earthFragmentShader } from '../../shaders/earth.glsl';
import { useWorldPulseStore } from '../../app/store';

interface EarthMeshProps {
  radius?: number;
}

export function EarthMesh({ radius = 1 }: EarthMeshProps) {
  const materialRef = useRef<THREE.ShaderMaterial>(null);
  const glowAccumulation = useWorldPulseStore((s) => s.glowAccumulation);

  const [dayTexture, nightTexture, specularTexture] = useTexture([
    '/textures/earth-dark.jpg',
    '/textures/earth-night.jpg',
    '/textures/earth-specular.png',
  ]);

  // Correct texture wrapping for sphere UV mapping
  useMemo(() => {
    [dayTexture, nightTexture, specularTexture].forEach((t) => {
      t.wrapS = THREE.RepeatWrapping;
      t.wrapT = THREE.RepeatWrapping;
      t.minFilter = THREE.LinearMipmapLinearFilter;
      t.magFilter = THREE.LinearFilter;
      t.anisotropy = 4;
    });
  }, [dayTexture, nightTexture, specularTexture]);

  const uniforms = useMemo(
    () => ({
      uDayTexture:    { value: dayTexture },
      uNightTexture:  { value: nightTexture },
      uSpecularMap:   { value: specularTexture },
      uGlowAccumulation: { value: 0 },
      uTime: { value: 0 },
    }),
    [dayTexture, nightTexture, specularTexture],
  );

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

  return (
    <mesh>
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
