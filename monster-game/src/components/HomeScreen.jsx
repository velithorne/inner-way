import { useEffect, useState } from 'react'
import { MonsterSprite } from './MonsterSprite'
import { StatAllocationModal } from './StatAllocationModal'
import { useGameStore } from '../store/gameStore'
import { ELEMENT_BACKGROUNDS } from '../data/elementBackgrounds'
import './HomeScreen.css'

export function HomeScreen({ onBattle, onNewMonster }) {
  const [showReset, setShowReset] = useState(false)
  const monster = useGameStore((s) => s.monster)
  const hunger = useGameStore((s) => s.hunger)
  const happiness = useGameStore((s) => s.happiness)
  const energy = useGameStore((s) => s.energy)
  const cleanliness = useGameStore((s) => s.cleanliness)
  const level = useGameStore((s) => s.level)
  const evolutionStage = useGameStore((s) => s.evolutionStage)
  const feed = useGameStore((s) => s.feed)
  const play = useGameStore((s) => s.play)
  const train = useGameStore((s) => s.train)
  const clean = useGameStore((s) => s.clean)
  const rest = useGameStore((s) => s.rest)
  const decayStats = useGameStore((s) => s.decayStats)
  const resetGame = useGameStore((s) => s.resetGame)

  useEffect(() => {
    decayStats()
  }, [decayStats])

  const getMood = () => {
    const avg = (hunger + happiness + energy + cleanliness) / 4
    if (avg >= 80) return { emoji: '😊', text: 'Happy' }
    if (avg >= 60) return { emoji: '🙂', text: 'Good' }
    if (avg >= 40) return { emoji: '😐', text: 'Okay' }
    if (avg >= 20) return { emoji: '😟', text: 'Sad' }
    return { emoji: '😢', text: 'Needs care!' }
  }

  const mood = getMood()
  const canBattle = energy > 20 && hunger > 20

  const element = monster?.element || 'fire'
  const bg = ELEMENT_BACKGROUNDS[element] || ELEMENT_BACKGROUNDS.fire

  return (
    <div
      className="home-screen"
      style={{
        background: bg.gradient,
      }}
    >
      <div className="home-screen-overlay" style={{ background: bg.overlay }} />
      <div className="home-screen-content">
        <header className="home-header">
          <h1>{monster?.name || 'Monster'}</h1>
          <span className="level-badge">Lv.{level}</span>
          {evolutionStage > 0 && <span className="evolution-badge">Evolved</span>}
        </header>

        <div className="monster-area">
          <div className="monster-stage" style={{ borderColor: `${bg.accent}40` }}>
            <MonsterSprite
              monster={monster}
              size="large"
              wandering
              evolutionStage={evolutionStage}
            />
          </div>
          <div className="mood-display">
            <span className="mood-emoji">{mood.emoji}</span>
            <span className="mood-text">{mood.text}</span>
          </div>
        </div>

        <div className="stats-grid">
          <div className="stat-bar">
            <span className="stat-icon">🍖</span>
            <div className="stat-track">
              <div className="stat-fill hunger" style={{ width: `${hunger}%` }} />
            </div>
            <span className="stat-value">{Math.round(hunger)}</span>
          </div>
          <div className="stat-bar">
            <span className="stat-icon">🎮</span>
            <div className="stat-track">
              <div className="stat-fill happiness" style={{ width: `${happiness}%` }} />
            </div>
            <span className="stat-value">{Math.round(happiness)}</span>
          </div>
          <div className="stat-bar">
            <span className="stat-icon">⚡</span>
            <div className="stat-track">
              <div className="stat-fill energy" style={{ width: `${energy}%` }} />
            </div>
            <span className="stat-value">{Math.round(energy)}</span>
          </div>
          <div className="stat-bar">
            <span className="stat-icon">✨</span>
            <div className="stat-track">
              <div className="stat-fill cleanliness" style={{ width: `${cleanliness}%` }} />
            </div>
            <span className="stat-value">{Math.round(cleanliness)}</span>
          </div>
        </div>

        <div className="care-actions">
          <button className="care-btn" onClick={feed} title="Feed">
            <span className="care-icon">🍖</span>
            <span>Feed</span>
          </button>
          <button className="care-btn" onClick={play} title="Play">
            <span className="care-icon">🎮</span>
            <span>Play</span>
          </button>
          <button className="care-btn" onClick={train} title="Train">
            <span className="care-icon">💪</span>
            <span>Train</span>
          </button>
          <button className="care-btn" onClick={clean} title="Clean">
            <span className="care-icon">✨</span>
            <span>Clean</span>
          </button>
          <button className="care-btn" onClick={rest} title="Rest">
            <span className="care-icon">😴</span>
            <span>Rest</span>
          </button>
        </div>

        <button
          className="battle-btn"
          onClick={onBattle}
          disabled={!canBattle}
        >
          ⚔️ Battle
        </button>

        <button
          className="reset-btn"
          onClick={() => setShowReset(true)}
        >
          New Monster
        </button>
      </div>

      {showReset && (
        <div className="reset-modal">
          <p>Create a new monster? Your current monster will be lost.</p>
          <div className="reset-actions">
            <button onClick={() => { resetGame(); onNewMonster(); setShowReset(false); }}>Yes, reset</button>
            <button onClick={() => setShowReset(false)}>Cancel</button>
          </div>
        </div>
      )}

      <StatAllocationModal onClose={() => {}} />
    </div>
  )
}
