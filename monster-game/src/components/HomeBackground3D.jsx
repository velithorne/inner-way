import { useRef, useMemo } from 'react'
import { Canvas, useFrame } from '@react-three/fiber'
import * as THREE from 'three'

// Pseudo-noise for procedural terrain
function noise2D(x, z, seed = 0) {
  const s = Math.sin(x * 1.7 + z * 2.3 + seed) * 0.5
  const t = Math.sin(x * 3.1 - z * 1.9 + seed * 1.3) * 0.5
  const u = Math.sin((x + z) * 0.7 + seed * 0.7) * 0.3
  return s + t + u
}

function ProceduralTerrain({ element, color1, color2 }) {
  const meshRef = useRef()
  const segments = 64

  const geometry = useMemo(() => {
    const geo = new THREE.PlaneGeometry(40, 40, segments, segments)
    const pos = geo.attributes.position
    const seed = element.charCodeAt(0) * 0.1

    for (let i = 0; i < pos.count; i++) {
      const x = pos.getX(i)
      const y = pos.getY(i)
      const h =
        noise2D(x * 0.15, y * 0.15, seed) * 2.5 +
        noise2D(x * 0.4, y * 0.4, seed + 10) * 0.8 +
        Math.abs(Math.sin(x * 0.2) * Math.cos(y * 0.2)) * 1.2
      pos.setZ(i, h)
    }
    pos.needsUpdate = true
    geo.computeVertexNormals()
    geo.rotateX(-Math.PI / 2)
    return geo
  }, [element])

  return (
    <mesh ref={meshRef} geometry={geometry} position={[0, -3, -15]} receiveShadow>
      <meshStandardMaterial
        color={color1}
        roughness={0.85}
        metalness={0.05}
        flatShading={false}
      />
    </mesh>
  )
}

function SkyDome({ color }) {
  return (
    <mesh>
      <sphereGeometry args={[80, 32, 32]} />
      <meshBasicMaterial color={color} side={THREE.BackSide} />
    </mesh>
  )
}

function FloatingParticles({ color, count = 40 }) {
  const pointsRef = useRef()
  const positions = useMemo(() => {
    const pos = new Float32Array(count * 3)
    for (let i = 0; i < count; i++) {
      pos[i * 3] = (Math.random() - 0.5) * 35
      pos[i * 3 + 1] = (Math.random() - 0.5) * 25
      pos[i * 3 + 2] = (Math.random() - 0.5) * 25 - 12
    }
    return pos
  }, [count])

  useFrame((state) => {
    if (pointsRef.current) {
      pointsRef.current.rotation.y = state.clock.elapsedTime * 0.015
    }
  })

  return (
    <points ref={pointsRef}>
      <bufferGeometry>
        <bufferAttribute
          attach="attributes-position"
          count={count}
          array={positions}
          itemSize={3}
        />
      </bufferGeometry>
      <pointsMaterial
        size={0.2}
        color={color}
        transparent
        opacity={0.7}
        depthWrite={false}
        sizeAttenuation
      />
    </points>
  )
}

const ELEMENT_SCENES = {
  fire: {
    sky: '#1a0505',
    terrain: '#3d1a0a',
    fog: '#2d0a0a',
    particleColor: '#ff6b35',
  },
  water: {
    sky: '#0a1530',
    terrain: '#0d2847',
    fog: '#050a1a',
    particleColor: '#40b4ff',
  },
  earth: {
    sky: '#1a1508',
    terrain: '#2a2210',
    fog: '#0d0a05',
    particleColor: '#b49650',
  },
  air: {
    sky: '#151a30',
    terrain: '#1e2540',
    fog: '#0a0d1a',
    particleColor: '#c8dcff',
  },
  nature: {
    sky: '#0a2d12',
    terrain: '#0f4020',
    fog: '#051a08',
    particleColor: '#50c878',
  },
  electric: {
    sky: '#151a2d',
    terrain: '#1a2040',
    fog: '#0a0a15',
    particleColor: '#ffdc50',
  },
  shadow: {
    sky: '#0a0a12',
    terrain: '#151520',
    fog: '#050508',
    particleColor: '#b450ff',
  },
  light: {
    sky: '#2d2818',
    terrain: '#403a28',
    fog: '#1a1810',
    particleColor: '#ffffc8',
  },
}

function Scene({ element }) {
  const scene = ELEMENT_SCENES[element] || ELEMENT_SCENES.fire

  return (
    <>
      <color attach="background" args={[scene.sky]} />
      <fog attach="fog" args={[scene.fog, 15, 50]} />
      <ambientLight intensity={0.5} />
      <directionalLight position={[10, 15, 10]} intensity={1.3} castShadow />
      <pointLight position={[-10, 10, -5]} intensity={0.6} color={scene.particleColor} />
      <SkyDome color={scene.sky} />
      <ProceduralTerrain
        element={element}
        color1={scene.terrain}
        color2={scene.particleColor}
      />
      <FloatingParticles color={scene.particleColor} />
    </>
  )
}

export function HomeBackground3D({ element }) {
  return (
    <div className="home-bg-3d">
      <Canvas
        camera={{ position: [0, 2, 8], fov: 55 }}
        gl={{ alpha: true, antialias: true }}
      >
        <Scene element={element} />
      </Canvas>
    </div>
  )
}
