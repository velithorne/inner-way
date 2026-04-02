import { useRef, useMemo } from 'react';
import { useFrame } from '@react-three/fiber';
import * as THREE from 'three';
import { latLonToVector3 } from '../../lib/geo';
import { emotionToColor, emotionGlowStrength } from '../../lib/colors';
import { pulseProgress, pulseOpacity, ringScale, easeOutExpo } from '../../lib/animation';
import { pulseVertexShader, pulseFragmentShader } from '../../shaders/pulse.glsl';
import type { PulseEvent } from '../../types';

interface PulseMarkerProps {
  pulse: PulseEvent;
  globeRadius?: number;
}

interface RingUniformSet {
  uniforms: { [key: string]: THREE.IUniform };
  offset: number;
}

const RING_COUNT = 3;

export function PulseMarker({ pulse, globeRadius = 1 }: PulseMarkerProps) {
  const groupRef = useRef<THREE.Group>(null);
  const ringRefs = useRef<Array<THREE.Mesh | null>>([]);
  const dotRef = useRef<THREE.Mesh>(null);

  const position = useMemo(
    () => latLonToVector3(pulse.lat, pulse.lon, globeRadius),
    [pulse.lat, pulse.lon, globeRadius],
  );

  const color = useMemo(() => emotionToColor(pulse.emotion), [pulse.emotion]);
  const glowStrength = useMemo(() => emotionGlowStrength(pulse.emotion), [pulse.emotion]);

  const ringSets = useMemo<RingUniformSet[]>(
    () =>
      Array.from({ length: RING_COUNT }, (_, i) => ({
        uniforms: {
          uColor: { value: color },
          uProgress: { value: 0 },
          uOpacity: { value: 0 },
          uGlowStrength: { value: glowStrength },
        },
        offset: i / RING_COUNT,
      })),
    [color, glowStrength],
  );

  useFrame(() => {
    const now = Date.now();
    const baseProgress = pulseProgress(pulse.startTime, pulse.duration, now);

    ringSets.forEach((set, i) => {
      const offset = set.offset * 0.6;
      const localProgress = Math.max(0, (baseProgress - offset) / (1 - offset));
      const p = Math.min(localProgress, 1);
      const scale = ringScale(p, 2.2 + i * 0.3) * 0.18;
      const opacity = pulseOpacity(p) * pulse.intensity * (1 - i * 0.2);

      set.uniforms.uProgress.value = p;
      set.uniforms.uOpacity.value = opacity;

      const mesh = ringRefs.current[i];
      if (mesh) {
        mesh.scale.setScalar(scale);
      }
    });

    // Dot fade
    if (dotRef.current) {
      const dotOpacity = pulseOpacity(baseProgress) * pulse.intensity;
      const dotMat = dotRef.current.material as THREE.MeshBasicMaterial;
      dotMat.opacity = dotOpacity * 0.9;

      const dotScale = easeOutExpo(Math.min(baseProgress * 3, 1)) * 0.015;
      dotRef.current.scale.setScalar(dotScale);
    }
  });

  // Orient group toward surface normal
  const normal = useMemo(() => position.clone().normalize(), [position]);
  const quaternion = useMemo(() => {
    const q = new THREE.Quaternion();
    q.setFromUnitVectors(new THREE.Vector3(0, 0, 1), normal);
    return q;
  }, [normal]);

  const surfacePos = position.clone().multiplyScalar(1.002);

  return (
    <group ref={groupRef} position={surfacePos} quaternion={quaternion}>
      {/* Central dot */}
      <mesh ref={dotRef}>
        <planeGeometry args={[1, 1]} />
        <meshBasicMaterial
          color={color}
          transparent
          opacity={0}
          depthWrite={false}
          blending={THREE.AdditiveBlending}
        />
      </mesh>

      {/* Expanding rings */}
      {ringSets.map((set, i) => (
        <mesh
          key={i}
          ref={(el) => {
            ringRefs.current[i] = el;
          }}
        >
          <planeGeometry args={[1, 1]} />
          <shaderMaterial
            vertexShader={pulseVertexShader}
            fragmentShader={pulseFragmentShader}
            uniforms={set.uniforms}
            transparent
            depthWrite={false}
            blending={THREE.AdditiveBlending}
            side={THREE.DoubleSide}
          />
        </mesh>
      ))}
    </group>
  );
}
