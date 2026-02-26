import { useGameStore } from '../store/gameStore'
import './StatAllocationModal.css'

const STATS = [
  { id: 'strength', label: 'Strength', icon: '💪', desc: '+Attack' },
  { id: 'agility', label: 'Agility', icon: '⚡', desc: '+Speed' },
  { id: 'defense', label: 'Defense', icon: '🛡️', desc: '+HP & Defense' },
]

export function StatAllocationModal({ onClose }) {
  const pendingStatPoints = useGameStore((s) => s.pendingStatPoints)
  const allocatedStats = useGameStore((s) => s.allocatedStats)
  const allocateStat = useGameStore((s) => s.allocateStat)
  const level = useGameStore((s) => s.level)

  if (pendingStatPoints <= 0) return null

  return (
    <div className="stat-modal-overlay">
      <div className="stat-modal">
        <h2>Level Up! 🎉</h2>
        <p className="stat-modal-subtitle">Level {level} — Choose where to put {pendingStatPoints} stat point{pendingStatPoints > 1 ? 's' : ''}</p>
        <div className="stat-options">
          {STATS.map((stat) => (
            <button
              key={stat.id}
              className="stat-option"
              onClick={() => allocateStat(stat.id)}
            >
              <span className="stat-icon">{stat.icon}</span>
              <span className="stat-label">{stat.label}</span>
              <span className="stat-desc">{stat.desc}</span>
              <span className="stat-current">+{allocatedStats[stat.id] || 0}</span>
            </button>
          ))}
        </div>
        <p className="stat-remaining">{pendingStatPoints} point{pendingStatPoints > 1 ? 's' : ''} remaining</p>
        <button className="stat-done-btn" onClick={onClose} disabled={pendingStatPoints > 0}>
          {pendingStatPoints > 0 ? `Allocate ${pendingStatPoints} more to continue` : 'Done'}
        </button>
      </div>
    </div>
  )
}
