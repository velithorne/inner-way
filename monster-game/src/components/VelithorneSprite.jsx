import { useState, useEffect } from 'react'
import './VelithorneSprite.css'

const RAINBOW_CRYSTALS = ['#ff6b9d', '#9b5de5', '#4a90e2', '#50c878', '#ffdc50', '#ff8c42']

function VelithorneHatchling({ className }) {
  return (
    <svg className={className} viewBox="0 0 120 100" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <linearGradient id="hatch-body" x1="0%" y1="0%" x2="0%" y2="100%">
          <stop offset="0%" stopColor="#2d2d2d" />
          <stop offset="60%" stopColor="#2d2d2d" />
          <stop offset="100%" stopColor="#e8e8e8" />
        </linearGradient>
        <linearGradient id="hatch-belly" x1="0%" y1="100%" x2="0%" y2="0%">
          <stop offset="0%" stopColor="#f5f5f5" />
          <stop offset="100%" stopColor="#e8e8e8" />
        </linearGradient>
        <filter id="glow-green">
          <feGaussianBlur stdDeviation="1" result="blur" />
          <feMerge>
            <feMergeNode in="blur" />
            <feMergeNode in="SourceGraphic" />
          </feMerge>
        </filter>
      </defs>
      <ellipse cx="60" cy="55" rx="28" ry="22" fill="url(#hatch-body)" />
      <circle cx="48" cy="42" r="6" fill="#00ff88" filter="url(#glow-green)" />
      <circle cx="72" cy="42" r="6" fill="#00ff88" filter="url(#glow-green)" />
      <circle cx="49" cy="41" r="2" fill="#0a0a0a" />
      <circle cx="73" cy="41" r="2" fill="#0a0a0a" />
      {RAINBOW_CRYSTALS.map((c, i) => (
        <polygon key={i} points={`${50 + i * 4},${28 - i % 2 * 2} ${52 + i * 4},20 ${54 + i * 4},${28 - i % 2 * 2}`} fill={c} />
      ))}
      <path d="M35 65 L40 75 L45 65 L50 75 L55 65 L60 72 L65 65 L70 75 L75 65 L80 75 L85 65" stroke="#2d2d2d" strokeWidth="4" fill="none" strokeLinecap="round" />
      <rect x="82" y="58" width="12" height="8" rx="2" fill="#2d2d2d" />
      <circle cx="85" cy="62" r="1.5" fill="#00ff88" />
      <circle cx="89" cy="62" r="1.5" fill="#4a90e2" />
      <ellipse cx="38" cy="68" rx="6" ry="4" fill="#1a1a1a" />
      <ellipse cx="52" cy="72" rx="6" ry="4" fill="#1a1a1a" />
      <ellipse cx="68" cy="72" rx="6" ry="4" fill="#1a1a1a" />
      <ellipse cx="82" cy="68" rx="6" ry="4" fill="#1a1a1a" />
    </svg>
  )
}

function VelithorneStage1({ className }) {
  return (
    <svg className={className} viewBox="0 0 140 110" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <linearGradient id="s1-body" x1="0%" y1="0%" x2="0%" y2="100%">
          <stop offset="0%" stopColor="#3d3d3d" />
          <stop offset="100%" stopColor="#1a1a1a" />
        </linearGradient>
        <linearGradient id="s1-belly" x1="0%" y1="100%" x2="0%" y2="0%">
          <stop offset="0%" stopColor="#f0f0f0" />
          <stop offset="100%" stopColor="#e0e0e0" />
        </linearGradient>
        <filter id="glow-s1">
          <feGaussianBlur stdDeviation="1.5" result="blur" />
          <feMerge>
            <feMergeNode in="blur" />
            <feMergeNode in="SourceGraphic" />
          </feMerge>
        </filter>
      </defs>
      <path d="M20 85 L30 75 L45 70 L60 75 L75 70 L90 75 L105 85 L95 90 L75 85 L60 88 L45 85 L25 90 Z" fill="url(#s1-body)" />
      <ellipse cx="62" cy="58" rx="20" ry="16" fill="url(#s1-belly)" />
      <path d="M25 55 Q35 45 50 50 L55 48 Q65 45 75 50 L80 48" stroke="#00ff88" strokeWidth="2" fill="none" opacity="0.9" />
      <path d="M25 50 Q15 35 30 45 Q45 38 55 50 Q45 55 35 52 Z" fill="#2d2d2d" stroke="#00ff88" strokeWidth="1" opacity="0.9" />
      <path d="M99 50 Q109 35 94 45 Q79 38 69 50 Q79 55 89 52 Z" fill="#2d2d2d" stroke="#00ff88" strokeWidth="1" opacity="0.9" />
      <circle cx="48" cy="42" r="6" fill="#00ff88" filter="url(#glow-s1)" />
      <circle cx="76" cy="42" r="6" fill="#00ff88" filter="url(#glow-s1)" />
      <circle cx="49" cy="41" r="2" fill="#0a0a0a" />
      <circle cx="77" cy="41" r="2" fill="#0a0a0a" />
      <circle cx="38" cy="55" r="8" fill="#00ff88" filter="url(#glow-s1)" opacity="0.8" />
      <circle cx="86" cy="55" r="8" fill="#00ff88" filter="url(#glow-s1)" opacity="0.8" />
      {RAINBOW_CRYSTALS.map((c, i) => (
        <polygon key={i} points={`${48 + i * 5},${22 - i % 2 * 3} ${52 + i * 5},12 ${56 + i * 5},${22 - i % 2 * 3}`} fill={c} />
      ))}
      <path d="M35 65 L40 78 L48 68 L55 78 L62 70 L70 78 L78 68 L85 78 L95 75" stroke="#2d2d2d" strokeWidth="5" fill="none" strokeLinecap="round" />
      <path d="M90 72 L98 65 L105 72 L105 80 L98 88 Z" fill="#2d2d2d" />
      <path d="M98 72 L102 68" stroke="#00ff88" strokeWidth="1.5" />
      <path d="M98 78 L102 82" stroke="#4a90e2" strokeWidth="1" />
    </svg>
  )
}

function VelithorneStage2({ className }) {
  return (
    <svg className={className} viewBox="0 0 160 130" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <linearGradient id="s2-body" x1="0%" y1="0%" x2="0%" y2="100%">
          <stop offset="0%" stopColor="#404040" />
          <stop offset="100%" stopColor="#1a1a1a" />
        </linearGradient>
        <linearGradient id="s2-belly" x1="0%" y1="100%" x2="0%" y2="0%">
          <stop offset="0%" stopColor="#eee" />
          <stop offset="100%" stopColor="#ddd" />
        </linearGradient>
        <filter id="glow-s2">
          <feGaussianBlur stdDeviation="2" result="blur" />
          <feMerge>
            <feMergeNode in="blur" />
            <feMergeNode in="SourceGraphic" />
          </feMerge>
        </filter>
      </defs>
      <path d="M15 95 L28 82 L48 75 L68 78 L88 75 L108 82 L125 95 L112 102 L88 98 L68 100 L48 98 L28 102 Z" fill="url(#s2-body)" />
      <ellipse cx="68" cy="68" rx="32" ry="26" fill="url(#s2-body)" />
      <ellipse cx="68" cy="62" rx="24" ry="18" fill="url(#s2-belly)" />
      <path d="M25 58 Q45 35 68 45 Q90 35 110 58" stroke="#00ff88" strokeWidth="2" fill="none" opacity="0.9" />
      <path d="M20 58 Q35 40 55 48" stroke="#ffdc50" strokeWidth="1" fill="none" opacity="0.8" />
      <path d="M115 58 Q100 40 80 48" stroke="#ffdc50" strokeWidth="1" fill="none" opacity="0.8" />
      <circle cx="52" cy="48" r="7" fill="#00ff88" filter="url(#glow-s2)" />
      <circle cx="84" cy="48" r="7" fill="#00ff88" filter="url(#glow-s2)" />
      <circle cx="53" cy="47" r="2.5" fill="#0a0a0a" />
      <circle cx="85" cy="47" r="2.5" fill="#0a0a0a" />
      <circle cx="42" cy="62" r="10" fill="#00ff88" filter="url(#glow-s2)" opacity="0.85" />
      <circle cx="94" cy="62" r="10" fill="#00ff88" filter="url(#glow-s2)" opacity="0.85" />
      {RAINBOW_CRYSTALS.map((c, i) => (
        <polygon key={i} points={`${52 + i * 6},${18 - i % 2 * 4} ${57 + i * 6},5 ${62 + i * 6},${18 - i % 2 * 4}`} fill={c} />
      ))}
      <path d="M22 55 Q8 38 35 50 Q55 42 68 58" fill="#2d2d2d" stroke="#00ff88" strokeWidth="1.5" opacity="0.95" />
      <path d="M118 55 Q132 38 105 50 Q85 42 72 58" fill="#2d2d2d" stroke="#00ff88" strokeWidth="1.5" opacity="0.95" />
      <path d="M22 55 Q8 38 35 50" stroke="#ffdc50" strokeWidth="0.8" fill="none" opacity="0.9" />
      <path d="M118 55 Q132 38 105 50" stroke="#ffdc50" strokeWidth="0.8" fill="none" opacity="0.9" />
      <path d="M35 72 L42 85 L52 75 L62 85 L68 72 L75 85 L85 75 L95 85 L105 72" stroke="#2d2d2d" strokeWidth="6" fill="none" strokeLinecap="round" />
      <path d="M100 78 L105 68 L115 78 L115 90 L105 100 Z" fill="#2d2d2d" />
      <ellipse cx="108" cy="85" rx="4" ry="6" fill="#00ff88" filter="url(#glow-s2)" opacity="0.9" />
    </svg>
  )
}

function VelithorneStage3({ className }) {
  return (
    <svg className={className} viewBox="0 0 180 150" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <linearGradient id="s3-body" x1="0%" y1="0%" x2="0%" y2="100%">
          <stop offset="0%" stopColor="#4a4a4a" />
          <stop offset="100%" stopColor="#1a1a1a" />
        </linearGradient>
        <linearGradient id="s3-belly" x1="0%" y1="100%" x2="0%" y2="0%">
          <stop offset="0%" stopColor="#f5f5f5" />
          <stop offset="100%" stopColor="#e5e5e5" />
        </linearGradient>
        <filter id="glow-s3">
          <feGaussianBlur stdDeviation="2.5" result="blur" />
          <feMerge>
            <feMergeNode in="blur" />
            <feMergeNode in="SourceGraphic" />
          </feMerge>
        </filter>
      </defs>
      <path d="M10 110 L25 92 L50 82 L75 85 L100 82 L125 92 L140 110 L122 120 L100 115 L75 118 L50 115 L28 120 Z" fill="url(#s3-body)" />
      <ellipse cx="75" cy="78" rx="38" ry="30" fill="url(#s3-body)" />
      <ellipse cx="75" cy="70" rx="28" ry="20" fill="url(#s3-belly)" />
      <path d="M20 65 Q50 35 75 48 Q100 35 130 65" stroke="#00ff88" strokeWidth="2.5" fill="none" opacity="0.95" />
      <path d="M20 65 Q40 45 60 55" stroke="#ffdc50" strokeWidth="1.5" fill="none" opacity="0.9" />
      <path d="M130 65 Q110 45 90 55" stroke="#ffdc50" strokeWidth="1.5" fill="none" opacity="0.9" />
      <path d="M20 65 Q40 45 60 55" stroke="#ff8c42" strokeWidth="0.8" fill="none" opacity="0.9" />
      <path d="M130 65 Q110 45 90 55" stroke="#ff8c42" strokeWidth="0.8" fill="none" opacity="0.9" />
      <path d="M25 58 Q5 25 45 48 L65 52 Z" fill="#2d2d2d" stroke="#00ff88" strokeWidth="2" opacity="0.95" />
      <path d="M125 58 Q145 25 105 48 L85 52 Z" fill="#2d2d2d" stroke="#00ff88" strokeWidth="2" opacity="0.95" />
      <path d="M25 58 Q5 25 45 48" stroke="#ffdc50" strokeWidth="1" fill="none" opacity="0.95" />
      <path d="M125 58 Q145 25 105 48" stroke="#ffdc50" strokeWidth="1" fill="none" opacity="0.95" />
      <circle cx="58" cy="55" r="8" fill="#00ff88" filter="url(#glow-s3)" />
      <circle cx="92" cy="55" r="8" fill="#00ff88" filter="url(#glow-s3)" />
      <circle cx="59" cy="54" r="3" fill="#0a0a0a" />
      <circle cx="93" cy="54" r="3" fill="#0a0a0a" />
      <path d="M58 62 L62 68 L58 72 L54 68 Z" fill="#fff" opacity="0.9" />
      <path d="M92 62 L96 68 L92 72 L88 68 Z" fill="#fff" opacity="0.9" />
      <circle cx="48" cy="72" r="12" fill="#00ff88" filter="url(#glow-s3)" opacity="0.9" />
      <circle cx="102" cy="72" r="12" fill="#00ff88" filter="url(#glow-s3)" opacity="0.9" />
      {RAINBOW_CRYSTALS.map((c, i) => (
        <polygon key={i} points={`${58 + i * 7},${12 - i % 2 * 5} ${64 + i * 7},-2 ${70 + i * 7},${12 - i % 2 * 5}`} fill={c} />
      ))}
      <path d="M35 78 L45 95 L58 82 L70 95 L75 78 L80 95 L92 82 L105 95 L115 78" stroke="#2d2d2d" strokeWidth="7" fill="none" strokeLinecap="round" />
      <path d="M110 85 L120 72 L135 85 L135 105 L120 120 Z" fill="#2d2d2d" />
      <ellipse cx="128" cy="95" rx="6" ry="10" fill="#00ff88" filter="url(#glow-s3)" opacity="0.95" />
      <ellipse cx="128" cy="95" rx="8" ry="12" fill="#00ff88" opacity="0.3" />
    </svg>
  )
}

export function VelithorneSprite({ evolutionStage = 0, wandering = true }) {
  const [position, setPosition] = useState({ x: 50, y: 45 })
  const stage = Math.min(Math.max(evolutionStage, 0), 3)

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

  const StageComponent = [VelithorneHatchling, VelithorneStage1, VelithorneStage2, VelithorneStage3][stage]
  const animClass = `velithorne-${stage}`

  return (
    <div
      className={`velithorne-sprite ${wandering ? 'velithorne-wandering' : ''} ${animClass}`}
      style={{
        position: 'absolute',
        left: `${position.x}%`,
        top: `${position.y}%`,
        transform: 'translate(-50%, -50%)',
        transition: 'left 2.5s ease-out, top 2.5s ease-out',
      }}
    >
      <StageComponent className="velithorne-svg" />
    </div>
  )
}
