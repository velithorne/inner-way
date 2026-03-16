import { useRef, useEffect } from 'react'

function hexToRgb(hex) {
  const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex || '#ff6b9d')
  return result
    ? { r: parseInt(result[1], 16), g: parseInt(result[2], 16), b: parseInt(result[3], 16) }
    : { r: 255, g: 107, b: 157 }
}

export function MonsterPortrait({ monster, size = 160 }) {
  const canvasRef = useRef(null)

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas || !monster?.colors) return

    const ctx = canvas.getContext('2d')
    const dpr = window.devicePixelRatio || 1
    canvas.width = size * dpr
    canvas.height = size * dpr
    canvas.style.width = `${size}px`
    canvas.style.height = `${size}px`
    ctx.scale(dpr, dpr)

    const primary = hexToRgb(monster.colors.primary)
    const secondary = hexToRgb(monster.colors.secondary)
    const accent = hexToRgb(monster.colors.accent)

    const cx = size / 2
    const cy = size / 2
    const bodyRadius = size * 0.35

    ctx.clearRect(0, 0, size, size)

    const gradient = ctx.createRadialGradient(cx, cy, 0, cx, cy, bodyRadius * 1.5)
    gradient.addColorStop(0, `rgba(${primary.r}, ${primary.g}, ${primary.b}, 0.4)`)
    gradient.addColorStop(0.5, `rgba(${primary.r}, ${primary.g}, ${primary.b}, 0.2)`)
    gradient.addColorStop(1, 'transparent')
    ctx.fillStyle = gradient
    ctx.beginPath()
    ctx.arc(cx, cy, bodyRadius * 1.5, 0, Math.PI * 2)
    ctx.fill()

    ctx.fillStyle = `rgb(${primary.r}, ${primary.g}, ${primary.b})`
    ctx.beginPath()
    ctx.ellipse(cx, cy + size * 0.02, bodyRadius * 1.1, bodyRadius, 0, 0, Math.PI * 2)
    ctx.fill()

    const highlight = ctx.createRadialGradient(
      cx - bodyRadius * 0.3, cy - bodyRadius * 0.3, 0,
      cx, cy, bodyRadius
    )
    highlight.addColorStop(0, `rgba(255, 255, 255, 0.35)`)
    highlight.addColorStop(0.5, 'transparent')
    ctx.fillStyle = highlight
    ctx.beginPath()
    ctx.ellipse(cx, cy + size * 0.02, bodyRadius * 1.1, bodyRadius, 0, 0, Math.PI * 2)
    ctx.fill()

    ctx.fillStyle = '#1a0a2e'
    ctx.beginPath()
    ctx.arc(cx - bodyRadius * 0.25, cy - bodyRadius * 0.1, bodyRadius * 0.15, 0, Math.PI * 2)
    ctx.arc(cx + bodyRadius * 0.25, cy - bodyRadius * 0.1, bodyRadius * 0.15, 0, Math.PI * 2)
    ctx.fill()

    if (monster.bodyType === 'spiky') {
      for (let i = 0; i < 6; i++) {
        const angle = (i / 6) * Math.PI * 0.8 - Math.PI * 0.4
        const x = cx + Math.cos(angle) * bodyRadius * 1.2
        const y = cy - Math.sin(angle) * bodyRadius * 0.8
        ctx.fillStyle = `rgb(${accent.r}, ${accent.g}, ${accent.b})`
        ctx.beginPath()
        ctx.moveTo(x, y)
        ctx.lineTo(x + 8, y - 12)
        ctx.lineTo(x + 16, y)
        ctx.closePath()
        ctx.fill()
      }
    }

    if (monster.bodyType === 'fluffy') {
      for (let i = 0; i < 5; i++) {
        const angle = (i / 5) * Math.PI * 0.6 - Math.PI * 0.3
        const x = cx + Math.cos(angle) * bodyRadius * 1.3
        const y = cy - Math.sin(angle) * bodyRadius * 0.9
        ctx.fillStyle = `rgba(${secondary.r}, ${secondary.g}, ${secondary.b}, 0.9)`
        ctx.beginPath()
        ctx.arc(x, y, bodyRadius * 0.2, 0, Math.PI * 2)
        ctx.fill()
      }
    }
  }, [monster, size])

  if (!monster) return null

  return (
    <canvas
      ref={canvasRef}
      className="monster-portrait"
      style={{ borderRadius: '50%', display: 'block' }}
    />
  )
}
