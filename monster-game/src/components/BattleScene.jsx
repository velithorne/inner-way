import { useState, useRef, useMemo } from 'react'
import { Canvas, useFrame } from '@react-three/fiber'
import { OrbitControls } from '@react-three/drei'
import * as THREE from 'three'
import { useGameStore } from '../store/gameStore'
import './BattleScene.css'

const MOVES = [
  { name: 'Strike', power: 25, type: 'physical', isMelee: true },
  { name: 'Blast', power: 30, type: 'special', isMelee: false },
  { name: 'Defend', power: 0, type: 'defense', isMelee: false },
  { name: 'Heal', power: -20, type: 'heal', isMelee: false },
]

function BattleMonster({ basePosition, color, scale = 1, targetPosition, isHit, isHealing }) {
  const groupRef = useRef()
  const currentPos = useRef(new THREE.Vector3(...basePosition))

  useFrame((_, delta) => {
    if (!groupRef.current) return
    const pos = groupRef.current.position
    const target = targetPosition ? new THREE.Vector3(...targetPosition) : new THREE.Vector3(...basePosition)
    currentPos.current.lerp(target, delta * 6)
    pos.copy(currentPos.current)

    if (isHit) {
      groupRef.current.scale.lerp(new THREE.Vector3(scale * 1.2, scale * 1.2, scale * 1.2), delta * 10)
    } else {
      groupRef.current.scale.lerp(new THREE.Vector3(scale, scale, scale), delta * 8)
    }
  })

  return (
    <group ref={groupRef} position={basePosition} scale={scale}>
      <mesh castShadow receiveShadow>
        <sphereGeometry args={[0.5, 32, 32]} />
        <meshStandardMaterial
          color={color}
          roughness={0.4}
          metalness={0.2}
          emissive={color}
          emissiveIntensity={isHealing ? 0.6 : 0.2}
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

function BattleContent({ playerColor, enemyColor, animationPhase, evolutionStage = 0 }) {
  const playerBasePos = [0, 0, -2]
  const enemyBasePos = [0, 0, 2]
  const meleeMeetPos = [0, 0, 0]

  const playerTarget = useMemo(() => {
    if (animationPhase === 'player-melee') return meleeMeetPos
    return null
  }, [animationPhase])

  const enemyTarget = useMemo(() => {
    if (animationPhase === 'enemy-melee') return meleeMeetPos
    return null
  }, [animationPhase])

  const showHitOnEnemy = animationPhase === 'enemy-hit'
  const showHitOnPlayer = animationPhase === 'player-hit'
  const showHealOnPlayer = animationPhase === 'player-heal'

  return (
    <>
      <BattleEnvironment />
      <BattleMonster
        basePosition={playerBasePos}
        color={playerColor}
        scale={1.2 + evolutionStage * 0.15}
        targetPosition={playerTarget}
        isHit={showHitOnPlayer}
        isHealing={showHealOnPlayer}
      />
      <BattleMonster
        basePosition={enemyBasePos}
        color={enemyColor}
        scale={1}
        targetPosition={enemyTarget}
        isHit={showHitOnEnemy}
      />
      {showHitOnEnemy && (
        <mesh position={[0, 0, 2]}>
          <sphereGeometry args={[0.8, 16, 16]} />
          <meshBasicMaterial color="#ff4444" transparent opacity={0.4} />
        </mesh>
      )}
      {showHitOnPlayer && (
        <mesh position={[0, 0, -2]}>
          <sphereGeometry args={[0.8, 16, 16]} />
          <meshBasicMaterial color="#ff4444" transparent opacity={0.4} />
        </mesh>
      )}
      {showHealOnPlayer && (
        <mesh position={[0, 0, -2]}>
          <sphereGeometry args={[0.9, 16, 16]} />
          <meshBasicMaterial color="#44ff88" transparent opacity={0.3} />
        </mesh>
      )}
      <OrbitControls
        enableZoom={false}
        enablePan={false}
        minPolarAngle={Math.PI / 3}
        maxPolarAngle={Math.PI / 2}
      />
    </>
  )
}

const DEFAULT_ENEMY = { name: 'Wild Foe', hp: 80, color: '#e76f51', expReward: 50 }

export function BattleScene({ onExit, enemy: enemyProp = null, battleMode = 'wild' }) {
  const monster = useGameStore((s) => s.monster)
  const evolutionStage = useGameStore((s) => s.evolutionStage)
  const getBattleStats = useGameStore((s) => s.getBattleStats)
  const recordBattleWin = useGameStore((s) => s.recordBattleWin)
  const recordBattleLoss = useGameStore((s) => s.recordBattleLoss)
  const advanceCampaignStage = useGameStore((s) => s.advanceCampaignStage)
  const advanceAdventureNode = useGameStore((s) => s.advanceAdventureNode)

  const enemy = enemyProp || DEFAULT_ENEMY
  const enemyHpMax = enemy.hp || 80

  const playerStats = getBattleStats()
  const [playerHp, setPlayerHp] = useState(playerStats.hp)
  const [enemyHp, setEnemyHp] = useState(enemyHpMax)
  const [battleLog, setBattleLog] = useState([])
  const [turn, setTurn] = useState('player')
  const [battleOver, setBattleOver] = useState(false)
  const [result, setResult] = useState(null)
  const [animationPhase, setAnimationPhase] = useState('idle')

  const enemyColor = toThreeColor(enemy.color)
  const playerColor = toThreeColor(monster?.colors?.primary)
  const enemyAttack = enemy.attack || 40

  const addLog = (msg) => setBattleLog((prev) => [...prev.slice(-4), msg])

  const runPlayerAttack = (move, damage, isMelee) => {
    addLog(`${monster?.name} used ${move.name} for ${damage} damage!`)
    setTurn('enemy')

    const enemyDefeated = enemyHp - damage <= 0

    const applyEnemyDamage = () => {
      setEnemyHp((h) => {
        const newHp = Math.max(0, h - damage)
        if (newHp <= 0) setTimeout(() => endBattle(true), 500)
        return newHp
      })
    }

    const afterHit = () => {
      setAnimationPhase('idle')
      if (!enemyDefeated) setTimeout(() => enemyTurn(false), 400)
    }

    if (isMelee) {
      setAnimationPhase('player-melee')
      setTimeout(() => {
        setAnimationPhase('enemy-hit')
        applyEnemyDamage()
        setTimeout(afterHit, 400)
      }, 600)
    } else {
      setAnimationPhase('player-blast')
      setTimeout(() => {
        setAnimationPhase('enemy-hit')
        applyEnemyDamage()
        setTimeout(afterHit, 500)
      }, 400)
    }
  }

  const executeMove = (move, isPlayer) => {
    if (battleOver || turn !== 'player') return

    if (move.type === 'defense') {
      addLog(`${monster?.name || 'Your monster'} is defending!`)
      setAnimationPhase('player-defend')
      setTurn('enemy')
      setTimeout(() => {
        setAnimationPhase('idle')
        setTimeout(() => enemyTurn(true), 200)
      }, 800)
      return
    }

    if (move.type === 'heal') {
      const heal = Math.floor(20 * (playerStats.attack / 50))
      setPlayerHp((h) => Math.min(playerStats.hp, h + heal))
      addLog(`${monster?.name} healed for ${heal}!`)
      setAnimationPhase('player-heal')
      setTurn('enemy')
      setTimeout(() => {
        setAnimationPhase('idle')
        setTimeout(() => enemyTurn(false), 400)
      }, 1000)
      return
    }

    if (isPlayer) {
      const damage = Math.floor(
        (move.power * (playerStats.attack / 50)) * (0.8 + Math.random() * 0.4)
      )
      runPlayerAttack(move, damage, move.isMelee)
    }
  }

  const enemyTurn = (wasDefending) => {
    if (battleOver) return

    const move = MOVES[Math.floor(Math.random() * 3)]
    const damage = Math.floor(
      (move.power * (enemyAttack / 50)) * (0.8 + Math.random() * 0.4) * (wasDefending ? 0.5 : 1)
    )
    addLog(`Enemy used ${move.name} for ${damage} damage!`)

    const applyPlayerDamage = () => {
      setPlayerHp((h) => {
        const newHp = Math.max(0, h - Math.max(0, damage))
        if (newHp <= 0) setTimeout(() => endBattle(false), 500)
        return newHp
      })
    }

    if (move.isMelee) {
      setAnimationPhase('enemy-melee')
      setTimeout(() => {
        setAnimationPhase('player-hit')
        applyPlayerDamage()
        setTimeout(() => {
          setAnimationPhase('idle')
          setTurn('player')
        }, 400)
      }, 600)
    } else {
      setAnimationPhase('enemy-blast')
      setTimeout(() => {
        setAnimationPhase('player-hit')
        applyPlayerDamage()
        setTimeout(() => {
          setAnimationPhase('idle')
          setTurn('player')
        }, 500)
      }, 400)
    }
  }

  const endBattle = (won) => {
    setBattleOver(true)
    setResult(won ? 'win' : 'lose')
    if (won) {
      recordBattleWin(enemy.expReward || 50)
      if (battleMode === 'campaign') advanceCampaignStage()
      if (battleMode === 'adventure') advanceAdventureNode()
    } else {
      recordBattleLoss()
    }
  }

  return (
    <div className="battle-scene">
      <div className="battle-ui">
        <div className="battle-hp">
          <div className="hp-bar player">
            <span>{monster?.name || 'Your Monster'}</span>
            <div className="hp-track">
              <div className="hp-fill" style={{ width: `${Math.max(0, (playerHp / playerStats.hp) * 100)}%` }} />
            </div>
          </div>
          <div className="hp-bar enemy">
            <span>{enemy.name}</span>
            <div className="hp-track">
              <div className="hp-fill" style={{ width: `${Math.max(0, (enemyHp / enemyHpMax) * 100)}%` }} />
            </div>
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

      <div className="battle-3d-wrapper">
        {animationPhase === 'enemy-hit' && <div className="battle-flash battle-flash-red" />}
        {animationPhase === 'player-hit' && <div className="battle-flash battle-flash-red" />}
        {animationPhase === 'player-heal' && <div className="battle-flash battle-flash-green" />}
        <div
          className={`battle-3d ${
            animationPhase === 'player-melee' || animationPhase === 'enemy-melee'
              ? 'battle-animating'
              : ''
          }`}
        >
          <Canvas
          camera={{ position: [0, 2, 6], fov: 50 }}
          shadows
          gl={{ alpha: true, antialias: true }}
        >
          <BattleContent
            playerColor={playerColor}
            enemyColor={enemyColor}
            animationPhase={animationPhase}
            evolutionStage={evolutionStage}
          />
        </Canvas>
        </div>
      </div>

      <button className="btn-flee" onClick={onExit}>
        Flee
      </button>
    </div>
  )
}
