import { VelithorneSprite } from './VelithorneSprite'
import './MonsterPreview.css'

export function MonsterPreview({ monster, onConfirm }) {
  return (
    <div className="monster-preview">
      <div className="preview-container">
        <div className="monster-display monster-display-velithorne">
          <VelithorneSprite evolutionStage={0} wandering={false} />
        </div>
        <h2 className="monster-name">{monster.name}</h2>
        <div className="monster-tags">
          <span className="tag element">{monster.element}</span>
          <span className="tag body">{monster.bodyType}</span>
          <span className="tag personality">{monster.personality}</span>
        </div>
      </div>
      <button className="btn-confirm" onClick={onConfirm}>
        This is My Monster!
      </button>
    </div>
  )
}
