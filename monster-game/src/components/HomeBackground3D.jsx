import { useRef, useMemo } from 'react'
import { Canvas, useFrame } from '@react-three/fiber'
import * as THREE from 'three'
import { Monster3D } from './Monster3D'

// Pseudo-noise for procedural terrain
function noise2D(x, z, seed = 0) {
  const s = Math.sin(x * 1.7 + z * 2.3 + seed) * 0.5
  const t = Math.sin(x * 3.1 - z * 1.9 + seed * 1.3) * 0.5
  const u = Math.sin((x + z) * 0.7 + seed * 0.7) * 0.3
  return s + t + u
}

function ProceduralRocks({ element, terrainColor }) {
  const seed = element.charCodeAt(0)
  const rocks = useMemo(() => {
    const arr = []
    for (let i = 0; i < 12; i++) {
      const x = (Math.sin(seed + i * 2.1) * 0.5 + 0.5) * 18 - 9
      const z = (Math.cos(seed + i * 1.7) * 0.5 + 0.5) * 12 - 18
      const s = 0.15 + (Math.sin(seed + i) * 0.5 + 0.5) * 0.2
      arr.push({ x, z, s })
    }
    return arr
  }, [element, seed])

  return (
    <group position={[0, -4, -20]}>
      {rocks.map((r, i) => (
        <mesh key={i} position={[r.x, r.s * 0.5, r.z]} castShadow receiveShadow>
          <dodecahedronGeometry args={[r.s, 0]} />
          <meshStandardMaterial
            color={terrainColor}
            roughness={0.9}
            metalness={0}
          />
        </mesh>
      ))}
    </group>
  )
}

function ProceduralTerrain({ element, color1 }) {
  const segments = 80

  const geometry = useMemo(() => {
    const geo = new THREE.PlaneGeometry(50, 50, segments, segments)
    const pos = geo.attributes.position
    const seed = element.charCodeAt(0) * 0.1

    for (let i = 0; i < pos.count; i++) {
      const x = pos.getX(i)
      const y = pos.getY(i)
      const h =
        noise2D(x * 0.12, y * 0.12, seed) * 3 +
        noise2D(x * 0.35, y * 0.35, seed + 10) * 1.2 +
        Math.abs(Math.sin(x * 0.15) * Math.cos(y * 0.15)) * 1.5
      pos.setZ(i, h)
    }
    pos.needsUpdate = true
    geo.computeVertexNormals()
    geo.rotateX(-Math.PI / 2)
    return geo
  }, [element])

  return (
    <mesh geometry={geometry} position={[0, -4, -20]} receiveShadow>
      <meshStandardMaterial
        color={color1}
        roughness={0.8}
        metalness={0.08}
        flatShading={false}
      />
    </mesh>
  )
}

function SkyDome({ color, horizonColor }) {
  const horizon = horizonColor || color
  const vertexShader = `
    varying vec3 vWorldPosition;
    void main() {
      vec4 worldPos = modelMatrix * vec4(position, 1.0);
      vWorldPosition = worldPos.xyz;
      gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
    }
  `
  const fragmentShader = `
    uniform vec3 uZenith;
    uniform vec3 uHorizon;
    varying vec3 vWorldPosition;
    void main() {
      float h = normalize(vWorldPosition).y;
      float t = smoothstep(-0.1, 0.5, h);
      vec3 col = mix(uHorizon, uZenith, t);
      gl_FragColor = vec4(col, 1.0);
    }
  `
  return (
    <mesh>
      <sphereGeometry args={[100, 32, 32]} />
      <shaderMaterial
        side={THREE.BackSide}
        vertexShader={vertexShader}
        fragmentShader={fragmentShader}
        uniforms={{
          uZenith: { value: new THREE.Color(color) },
          uHorizon: { value: new THREE.Color(horizon) },
        }}
      />
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
    horizon: '#4a2010',
    terrain: '#3d1a0a',
    fog: '#2d0a0a',
    particleColor: '#ff6b35',
  },
  water: {
    sky: '#0a1530',
    horizon: '#0d2847',
    terrain: '#0d2847',
    fog: '#050a1a',
    particleColor: '#40b4ff',
  },
  earth: {
    sky: '#1a1508',
    horizon: '#2a2210',
    terrain: '#2a2210',
    fog: '#0d0a05',
    particleColor: '#b49650',
  },
  air: {
    sky: '#151a30',
    horizon: '#2a3050',
    terrain: '#1e2540',
    fog: '#0a0d1a',
    particleColor: '#c8dcff',
  },
  nature: {
    sky: '#0a2d12',
    horizon: '#0f4020',
    terrain: '#0f4020',
    fog: '#051a08',
    particleColor: '#50c878',
  },
  electric: {
    sky: '#151a2d',
    horizon: '#252a45',
    terrain: '#1a2040',
    fog: '#0a0a15',
    particleColor: '#ffdc50',
  },
  shadow: {
    sky: '#0a0a12',
    horizon: '#1a1a2a',
    terrain: '#151520',
    fog: '#050508',
    particleColor: '#b450ff',
  },
  light: {
    sky: '#2d2818',
    horizon: '#504a38',
    terrain: '#403a28',
    fog: '#1a1810',
    particleColor: '#ffffc8',
  },
}

function Scene({ element, monster, evolutionStage, cleanliness }) {
  const scene = ELEMENT_SCENES[element] || ELEMENT_SCENES.fire

  return (
    <>
      <color attach="background" args={[scene.sky]} />
      <fog attach="fog" args={[scene.fog, 12, 45]} />
      <ambientLight intensity={0.6} />
      <directionalLight
        position={[8, 12, 8]}
        intensity={1.2}
        castShadow
        shadow-mapSize={[2048, 2048]}
        shadow-camera-far={50}
        shadow-camera-left={-15}
        shadow-camera-right={15}
        shadow-camera-top={15}
        shadow-camera-bottom={-15}
      />
      <pointLight position={[-8, 8, -5]} intensity={0.5} color={scene.particleColor} />
      <pointLight position={[8, 5, 5]} intensity={0.3} color="#ffffff" />
      <SkyDome color={scene.sky} horizonColor={scene.horizon} />
      <ProceduralTerrain element={element} color1={scene.terrain} />
      <ProceduralRocks element={element} terrainColor={scene.terrain} />
      <FloatingParticles color={scene.particleColor} count={50} />
      {monster && (
        <>
          <mesh position={[0, -0.6, 0]} receiveShadow>
            <cylinderGeometry args={[0.8, 1, 0.15, 32]} />
            <meshStandardMaterial
              color="#1a0a2e"
              roughness={0.9}
              metalness={0.05}
            />
          </mesh>
          <Monster3D
            monster={monster}
            evolutionStage={evolutionStage}
            cleanliness={cleanliness}
          />
        </>
      )}
    </>
  )
}

export function HomeBackground3D({ element, monster, evolutionStage, cleanliness }) {
  return (
    <div className="home-bg-3d">
      <Canvas
        camera={{ position: [0, 0.4, 6], fov: 50 }}
        gl={{ alpha: true, antialias: true }}
        shadows
      >
        <Scene
          element={element}
          monster={monster}
          evolutionStage={evolutionStage}
          cleanliness={cleanliness}
        />
      </Canvas>
    </div>
  )
}
