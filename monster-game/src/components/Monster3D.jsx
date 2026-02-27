import { useRef } from 'react'
import { useFrame } from '@react-three/fiber'
import * as THREE from 'three'

function toColor(str) {
  try {
    return new THREE.Color(str || '#ff6b9d')
  } catch {
    return new THREE.Color('#ff6b9d')
  }
}

export function Monster3D({ monster, evolutionStage = 0, cleanliness = 100 }) {
  const groupRef = useRef()
  const primary = toColor(monster?.colors?.primary)
  const secondary = toColor(monster?.colors?.secondary)
  const accent = toColor(monster?.colors?.accent)
  const isDirty = cleanliness < 60
  const stage = Math.min(evolutionStage, 4)
  const scale = 0.8 + stage * 0.15

  useFrame((state) => {
    if (groupRef.current) {
      groupRef.current.position.y = Math.sin(state.clock.elapsedTime * 0.8) * 0.08
      groupRef.current.rotation.y = Math.sin(state.clock.elapsedTime * 0.3) * 0.1
    }
  })

  return (
    <group ref={groupRef} position={[0, -0.05, 0]} scale={scale}>
      <mesh castShadow receiveShadow>
        <sphereGeometry args={[0.5, 32, 32]} />
        <meshStandardMaterial
          color={primary}
          roughness={0.35}
          metalness={0.1}
          emissive={primary}
          emissiveIntensity={0.15}
        />
      </mesh>
      <mesh position={[0.15, 0.12, 0.45]} castShadow>
        <sphereGeometry args={[0.08, 16, 16]} />
        <meshStandardMaterial color="#1a0a2e" />
      </mesh>
      <mesh position={[-0.15, 0.12, 0.45]} castShadow>
        <sphereGeometry args={[0.08, 16, 16]} />
        <meshStandardMaterial color="#1a0a2e" />
      </mesh>
      {stage >= 1 && (
        <>
          <mesh position={[-0.45, 0, 0]} rotation={[0, 0, 0.4]} castShadow>
            <cylinderGeometry args={[0.08, 0.08, 0.2, 12]} />
            <meshStandardMaterial color={secondary} roughness={0.5} />
          </mesh>
          <mesh position={[0.45, 0, 0]} rotation={[0, 0, -0.4]} castShadow>
            <cylinderGeometry args={[0.08, 0.08, 0.2, 12]} />
            <meshStandardMaterial color={secondary} roughness={0.5} />
          </mesh>
          <mesh position={[-0.2, -0.45, 0.3]} rotation={[0.2, 0, 0.1]} castShadow>
            <cylinderGeometry args={[0.07, 0.07, 0.18, 12]} />
            <meshStandardMaterial color={secondary} roughness={0.5} />
          </mesh>
          <mesh position={[0.2, -0.45, 0.3]} rotation={[0.2, 0, -0.1]} castShadow>
            <cylinderGeometry args={[0.07, 0.07, 0.18, 12]} />
            <meshStandardMaterial color={secondary} roughness={0.5} />
          </mesh>
        </>
      )}
      {stage >= 2 && (
        <>
          <mesh position={[-0.2, 0.55, 0.5]} rotation={[0.3, 0, 0.2]} castShadow>
            <coneGeometry args={[0.06, 0.2, 12]} />
            <meshStandardMaterial color={accent} roughness={0.4} />
          </mesh>
          <mesh position={[0.2, 0.55, 0.5]} rotation={[0.3, 0, -0.2]} castShadow>
            <coneGeometry args={[0.06, 0.2, 12]} />
            <meshStandardMaterial color={accent} roughness={0.4} />
          </mesh>
        </>
      )}
      {stage >= 3 && (
        <mesh position={[0, 0, 0.6]}>
          <sphereGeometry args={[0.8, 16, 16]} />
          <meshBasicMaterial
            color={accent}
            transparent
            opacity={0.12}
            side={THREE.BackSide}
          />
        </mesh>
      )}
      {isDirty && (
        <mesh position={[0, 0, 0.55]}>
          <sphereGeometry args={[0.52, 16, 16]} />
          <meshBasicMaterial
            color="#4a3728"
            transparent
            opacity={0.25}
          />
        </mesh>
      )}
    </group>
  )
}
