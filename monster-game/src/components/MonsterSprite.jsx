import { useMemo, useState, useEffect } from 'react'
import './MonsterSprite.css'

export function MonsterSprite({ monster, size = 'medium', wandering = false }) {
  const [position, setPosition] = useState({ x: 50, y: 50 })

  useEffect(() => {
    if (!wandering) return
    const interval = setInterval(() => {
      setPosition((prev) => ({
        x: Math.max(15, Math.min(85, prev.x + (Math.random() - 0.5) * 25)),
        y: Math.max(20, Math.min(80, prev.y + (Math.random() - 0.5) * 20)),
      }))
    }, 2500)
    return () => clearInterval(interval)
  }, [wandering])

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

  const wrapperStyle = wandering
    ? {
        position: 'absolute',
        left: `${position.x}%`,
        top: `${position.y}%`,
        transform: 'translate(-50%, -50%)',
        transition: 'left 2s ease-out, top 2s ease-out',
      }
    : {}

  return (
    <div className={`monster-sprite ${wandering ? 'monster-wandering' : ''}`} style={{ ...style, ...wrapperStyle }}>
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
