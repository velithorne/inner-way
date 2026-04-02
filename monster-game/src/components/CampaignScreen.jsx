import { useGameStore } from '../store/gameStore'
import { CAMPAIGN_STAGES, getCampaignEnemy } from '../data/enemies'
import './CampaignScreen.css'

export function CampaignScreen({ onStartBattle, onBack }) {
  const campaignStage = useGameStore((s) => s.campaignStage)
  const level = useGameStore((s) => s.level)

  const currentStage = CAMPAIGN_STAGES[campaignStage]
  const enemy = currentStage ? getCampaignEnemy(campaignStage) : null

  const handleStart = () => {
    if (enemy) onStartBattle(enemy)
  }

  return (
    <div className="campaign-screen">
      <h1>Campaign</h1>
      <p className="campaign-sub">Stage {Math.min(campaignStage + 1, 20)} of 20</p>

      {currentStage ? (
        <div className="campaign-stage-card">
          <div className={`stage-header ${currentStage.isBoss ? 'boss' : ''}`}>
            {currentStage.isBoss && <span className="boss-badge">BOSS</span>}
            <h2>{currentStage.enemyName}</h2>
            <p>Level {currentStage.enemyLevel}</p>
          </div>
          <div className="stage-rewards">
            <span>Reward: {currentStage.expReward} EXP</span>
          </div>
          <button className="btn-start-battle" onClick={handleStart}>
            Fight
          </button>
        </div>
      ) : (
        <p className="campaign-complete">Campaign complete! 🎉</p>
      )}

      <div className="campaign-progress">
        {CAMPAIGN_STAGES.slice(0, 10).map((s, i) => (
          <div
            key={s.id}
            className={`progress-dot ${i < campaignStage ? 'done' : ''} ${i === campaignStage ? 'current' : ''} ${s.isBoss ? 'boss' : ''}`}
          />
        ))}
      </div>

      <button className="btn-back" onClick={onBack}>
        ← Back
      </button>
    </div>
  )
}
