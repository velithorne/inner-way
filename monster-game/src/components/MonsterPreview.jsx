import { MonsterSprite } from './MonsterSprite'
import './MonsterPreview.css'

export function MonsterPreview({ monster, onConfirm }) {
  return (
    <div className="monster-preview">
      <div className="preview-container">
        <div className="monster-display">
          <MonsterSprite monster={monster} size="large" />
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
