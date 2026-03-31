import * as THREE from 'three';
import {
  atmosphereVertexShader,
  atmosphereFragmentShader,
  outerGlowVertexShader,
  outerGlowFragmentShader,
} from '../../shaders/atmosphere.glsl';

interface AtmosphereLayerProps {
  radius?: number;
}

export function AtmosphereLayer({ radius = 1 }: AtmosphereLayerProps) {
  return (
    <>
      {/* Primary atmospheric rim — blue-teal inner halo */}
      <mesh>
        <sphereGeometry args={[radius * 1.03, 64, 64]} />
        <shaderMaterial
          vertexShader={atmosphereVertexShader}
          fragmentShader={atmosphereFragmentShader}
          uniforms={{
            uAtmosphereColor:    { value: new THREE.Color(0x1b6ea8) },
            uAtmosphereStrength: { value: 0.9 },
          }}
          transparent
          side={THREE.BackSide}
          blending={THREE.AdditiveBlending}
          depthWrite={false}
        />
      </mesh>

      {/* Outer diffuse corona — larger, very faint indigo */}
      <mesh>
        <sphereGeometry args={[radius * 1.18, 48, 48]} />
        <shaderMaterial
          vertexShader={outerGlowVertexShader}
          fragmentShader={outerGlowFragmentShader}
          transparent
          side={THREE.BackSide}
          blending={THREE.AdditiveBlending}
          depthWrite={false}
        />
      </mesh>
    </>
  );
}
