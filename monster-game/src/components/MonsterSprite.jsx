import { useMemo } from 'react'
import './MonsterSprite.css'

export function MonsterSprite({ monster, size = 'medium' }) {
  const style = useMemo(() => {
    if (!monster?.colors) return {}
    return {
      '--primary': monster.colors.primary,
      '--secondary': monster.colors.secondary,
      '--accent': monster.colors.accent,
    }
  }, [monster])

  if (!monster) return null

  const bodyClass = `monster-body monster-${monster.bodyType} monster-size-${size}`

  return (
    <div className="monster-sprite" style={style}>
      <div className={bodyClass}>
        <div className="monster-eyes">
          <div className="eye left" />
          <div className="eye right" />
        </div>
        <div className="monster-mouth" />
        {monster.bodyType === 'spiky' && (
          <div className="spikes">
            {[...Array(5)].map((_, i) => (
              <div key={i} className="spike" style={{ '--i': i }} />
            ))}
          </div>
        )}
        {monster.bodyType === 'fluffy' && (
          <div className="fluff">
            {[...Array(6)].map((_, i) => (
              <div key={i} className="fluff-ball" style={{ '--i': i }} />
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
