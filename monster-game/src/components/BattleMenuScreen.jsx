import { useGameStore } from '../store/gameStore'
import './BattleMenuScreen.css'

export function BattleMenuScreen({ onWildBattle, onCampaign, onAdventure, onBack }) {
  const level = useGameStore((s) => s.level)
  const campaignStage = useGameStore((s) => s.campaignStage)
  const energy = useGameStore((s) => s.energy)
  const hunger = useGameStore((s) => s.hunger)
  const canBattle = energy > 20 && hunger > 20

  return (
    <div className="battle-menu">
      <h1>Battle</h1>
      <p className="battle-menu-sub">Choose your challenge</p>

      {!canBattle && (
        <p className="battle-menu-warning">Need more energy and hunger to battle!</p>
      )}

      <div className="battle-options">
        <button
          className="battle-option wild"
          onClick={onWildBattle}
          disabled={!canBattle}
        >
          <span className="option-icon">⚔️</span>
          <span className="option-title">Wild Battle</span>
          <span className="option-desc">Random foes near your level (Lv.{level}±2)</span>
        </button>

        <button
          className="battle-option campaign"
          onClick={onCampaign}
          disabled={!canBattle}
        >
          <span className="option-icon">📜</span>
          <span className="option-title">Campaign</span>
          <span className="option-desc">Stage {campaignStage + 1}/20 — Boss every 5 stages</span>
        </button>

        <button
          className="battle-option adventure"
          onClick={onAdventure}
          disabled={!canBattle}
        >
          <span className="option-icon">🗺️</span>
          <span className="option-title">Adventure</span>
          <span className="option-desc">Explore a path — fight, rest, or find treasure</span>
        </button>
      </div>

      <button className="btn-back" onClick={onBack}>
        ← Back
      </button>
    </div>
  )
}
