import { useState, useRef } from 'react'
import { Canvas } from '@react-three/fiber'
import { OrbitControls } from '@react-three/drei'
import * as THREE from 'three'
import { useGameStore } from '../store/gameStore'
import './BattleScene.css'

const MOVES = [
  { name: 'Strike', power: 25, type: 'physical' },
  { name: 'Blast', power: 30, type: 'special' },
  { name: 'Defend', power: 0, type: 'defense' },
  { name: 'Heal', power: -20, type: 'heal' },
]

function BattleMonster({ position, color, scale = 1 }) {
  const meshRef = useRef()

  return (
    <group position={position} scale={scale}>
      <mesh ref={meshRef} castShadow receiveShadow>
        <sphereGeometry args={[0.5, 32, 32]} />
        <meshStandardMaterial
          color={color}
          roughness={0.4}
          metalness={0.2}
          emissive={color}
          emissiveIntensity={0.2}
        />
      </mesh>
      <mesh position={[0, 0, 0.55]} castShadow>
        <sphereGeometry args={[0.08, 16, 16]} />
        <meshStandardMaterial color="#1a0a2e" />
      </mesh>
      <mesh position={[-0.15, 0.1, 0.55]} castShadow>
        <sphereGeometry args={[0.08, 16, 16]} />
        <meshStandardMaterial color="#1a0a2e" />
      </mesh>
    </group>
  )
}

function BattleEnvironment() {
  return (
    <>
      <ambientLight intensity={0.4} />
      <directionalLight
        position={[5, 8, 5]}
        intensity={1}
        castShadow
        shadow-mapSize={[1024, 1024]}
        shadow-camera-far={50}
        shadow-camera-left={-10}
        shadow-camera-right={10}
        shadow-camera-top={10}
        shadow-camera-bottom={-10}
      />
      <pointLight position={[-5, 5, -5]} intensity={0.5} color="#40e0d0" />
      <pointLight position={[5, 3, 5]} intensity={0.5} color="#ff6b9d" />
      <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, -1, 0]} receiveShadow>
        <planeGeometry args={[20, 20]} />
        <meshStandardMaterial color="#0d0218" />
      </mesh>
    </>
  )
}

function toThreeColor(colorStr) {
  try {
    return new THREE.Color(colorStr || '#40e0d0')
  } catch {
    return new THREE.Color('#40e0d0')
  }
}

export function BattleScene({ onExit }) {
  const monster = useGameStore((s) => s.monster)
  const getBattleStats = useGameStore((s) => s.getBattleStats)
  const addExp = useGameStore((s) => s.addExp)
  const recordBattleWin = useGameStore((s) => s.recordBattleWin)
  const recordBattleLoss = useGameStore((s) => s.recordBattleLoss)

  const playerStats = getBattleStats()
  const [playerHp, setPlayerHp] = useState(playerStats.hp)
  const [enemyHp, setEnemyHp] = useState(80)
  const [battleLog, setBattleLog] = useState([])
  const [turn, setTurn] = useState('player')
  const [battleOver, setBattleOver] = useState(false)
  const [result, setResult] = useState(null)

  const enemyColor = '#e76f51'
  const playerColor = toThreeColor(monster?.colors?.primary)

  const addLog = (msg) => setBattleLog((prev) => [...prev.slice(-4), msg])

  const executeMove = (move, isPlayer) => {
    if (battleOver || turn !== 'player') return

    if (move.type === 'defense') {
      addLog(`${monster?.name || 'Your monster'} is defending!`)
      setTurn('enemy')
      setTimeout(() => enemyTurn(true), 1000)
      return
    }

    if (move.type === 'heal') {
      const heal = Math.floor(20 * (playerStats.attack / 50))
      setPlayerHp((h) => Math.min(playerStats.hp, h + heal))
      addLog(`${monster?.name} healed for ${heal}!`)
      setTurn('enemy')
      setTimeout(() => enemyTurn(false), 1500)
      return
    }

    if (isPlayer) {
      const damage = Math.floor(
        (move.power * (playerStats.attack / 50)) * (0.8 + Math.random() * 0.4)
      )
      setEnemyHp((h) => {
        const newHp = Math.max(0, h - damage)
        if (newHp <= 0) setTimeout(() => endBattle(true), 500)
        return newHp
      })
      addLog(`${monster?.name} used ${move.name} for ${damage} damage!`)
      setTurn('enemy')
      setTimeout(() => enemyTurn(false), 1500)
    }
  }

  const enemyTurn = (wasDefending) => {
    if (battleOver) return

    const move = MOVES[Math.floor(Math.random() * 3)]
    const damage = Math.floor(
      (move.power * 0.8) * (0.8 + Math.random() * 0.4) * (wasDefending ? 0.5 : 1)
    )
    setPlayerHp((h) => {
      const newHp = Math.max(0, h - Math.max(0, damage))
      if (newHp <= 0) setTimeout(() => endBattle(false), 500)
      return newHp
    })
    addLog(`Enemy used ${move.name} for ${damage} damage!`)
    setTurn('player')
  }

  const endBattle = (won) => {
    setBattleOver(true)
    setResult(won ? 'win' : 'lose')
    if (won) {
      recordBattleWin()
      addExp(50)
    } else {
      recordBattleLoss()
    }
  }

  return (
    <div className="battle-scene">
      <div className="battle-ui">
        <div className="battle-hp">
          <div className="hp-bar player">
            <div className="hp-fill" style={{ width: `${(playerHp / playerStats.hp) * 100}%` }} />
            <span>{monster?.name || 'Your Monster'}</span>
          </div>
          <div className="hp-bar enemy">
            <div className="hp-fill" style={{ width: `${(enemyHp / 80) * 100}%` }} />
            <span>Wild Foe</span>
          </div>
        </div>

        <div className="battle-log">
          {battleLog.map((msg, i) => (
            <p key={i}>{msg}</p>
          ))}
        </div>

        {battleOver ? (
          <div className="battle-result">
            <h2>{result === 'win' ? 'Victory!' : 'Defeated...'}</h2>
            <button className="btn-exit" onClick={onExit}>
              Return Home
            </button>
          </div>
        ) : (
          <div className="battle-moves">
            {MOVES.map((move) => (
              <button
                key={move.name}
                className="move-btn"
                onClick={() => executeMove(move, true)}
                disabled={turn !== 'player'}
              >
                {move.name}
              </button>
            ))}
          </div>
        )}
      </div>

      <div className="battle-3d">
        <Canvas
          camera={{ position: [0, 2, 6], fov: 50 }}
          shadows
          gl={{ alpha: true, antialias: true }}
        >
          <BattleEnvironment />
          <BattleMonster
            position={[0, 0, -2]}
            color={playerColor}
            scale={1.2}
          />
          <BattleMonster
            position={[0, 0, 2]}
            color={enemyColor}
            scale={1}
          />
          <OrbitControls
            enableZoom={false}
            enablePan={false}
            minPolarAngle={Math.PI / 3}
            maxPolarAngle={Math.PI / 2}
          />
        </Canvas>
      </div>

      <button className="btn-flee" onClick={onExit}>
        Flee
      </button>
    </div>
  )
}
