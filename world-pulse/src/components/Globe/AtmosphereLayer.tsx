import { useRef } from 'react';
import * as THREE from 'three';
import { atmosphereVertexShader, atmosphereFragmentShader } from '../../shaders/atmosphere.glsl';

interface AtmosphereLayerProps {
  radius?: number;
}

export function AtmosphereLayer({ radius = 1 }: AtmosphereLayerProps) {
  const materialRef = useRef<THREE.ShaderMaterial>(null);

  const uniforms = {
    uAtmosphereColor: { value: new THREE.Color(0x1a6fa8) },
    uAtmosphereStrength: { value: 0.85 },
  };

  return (
    <mesh>
      <sphereGeometry args={[radius * 1.12, 64, 64]} />
      <shaderMaterial
        ref={materialRef}
        vertexShader={atmosphereVertexShader}
        fragmentShader={atmosphereFragmentShader}
        uniforms={uniforms}
        transparent
        side={THREE.BackSide}
        blending={THREE.AdditiveBlending}
        depthWrite={false}
      />
    </mesh>
  );
}
