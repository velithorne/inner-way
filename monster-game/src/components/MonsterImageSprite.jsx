import { useState, useEffect } from 'react'
import './MonsterImageSprite.css'

/**
 * Maps level to stage index for sprite selection.
 * stage0: levels 1–5 (baby)
 * stage1: levels 6–14
 * stage2: levels 15–24
 * stage3: levels 25+
 */
export function getStageIndexFromLevel(level) {
  if (level <= 5) return 0
  if (level <= 14) return 1
  if (level <= 24) return 2
  return 3
}

export function MonsterImageSprite({ monsterId = 'flare', stageIndex, level, sizePx = 140, wandering = false, className = '' }) {
  const [imageError, setImageError] = useState(false)
  const [position, setPosition] = useState({ x: 50, y: 45 })

  const resolvedStage = stageIndex ?? (level != null ? getStageIndexFromLevel(level) : 0)
  const stage = Math.max(0, Math.min(3, resolvedStage))
  const [useSvg, setUseSvg] = useState(false)
  const src = `/assets/monsters/${monsterId}/stage${stage}.${useSvg ? 'svg' : 'png'}`

  useEffect(() => {
    setUseSvg(false)
    setImageError(false)
  }, [stage, monsterId])

  useEffect(() => {
    if (!wandering) return
    const interval = setInterval(() => {
      setPosition((prev) => ({
        x: Math.max(20, Math.min(80, prev.x + (Math.random() - 0.5) * 18)),
        y: Math.max(25, Math.min(65, prev.y + (Math.random() - 0.5) * 14)),
      }))
    }, 2500)
    return () => clearInterval(interval)
  }, [wandering])

  const wrapperStyle = wandering
    ? {
        position: 'absolute',
        left: `${position.x}%`,
        top: `${position.y}%`,
        transform: 'translate(-50%, -50%)',
        transition: 'left 2.5s ease-out, top 2.5s ease-out',
      }
    : {}

  if (imageError) {
    return (
      <div
        className={`monster-image-sprite monster-image-fallback ${className}`}
        style={{ ...wrapperStyle, width: sizePx, height: sizePx }}
      >
        <span>Stage {stage}</span>
        <span className="fallback-hint">Add {monsterId}/stage{stage}.png</span>
      </div>
    )
  }

  return (
    <div
      className={`monster-image-sprite ${className}`}
      style={wrapperStyle}
    >
      <img
        src={src}
        alt={`Monster stage ${stage}`}
        className="monster-image"
        style={{ width: sizePx, height: sizePx }}
        onError={() => {
          if (!useSvg) {
            setUseSvg(true)
          } else {
            setImageError(true)
          }
        }}
      />
    </div>
  )
}
