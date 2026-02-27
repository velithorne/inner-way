/**
 * Generates a unique monster based on face image data and personality answers
 */

const BODY_TYPES = ['round', 'spiky', 'slim', 'fluffy', 'angular', 'bubbly']
const ELEMENT_TYPES = ['fire', 'water', 'earth', 'air', 'nature', 'electric', 'shadow', 'light']
const PERSONALITY_TRAITS = ['brave', 'playful', 'curious', 'loyal', 'mischievous', 'calm', 'energetic', 'shy']
const MONSTER_NAMES = ['Blaze', 'Splash', 'Rocky', 'Gale', 'Leaf', 'Spark', 'Shadow', 'Luna', 'Ember', 'Frost', 'Storm', 'Thorn', 'Mist', 'Flare', 'Breeze', 'Crystal']

function hashString(str) {
  let hash = 0
  for (let i = 0; i < str.length; i++) {
    const char = str.charCodeAt(i)
    hash = ((hash << 5) - hash) + char
    hash = hash & hash
  }
  return Math.abs(hash)
}

function hashArray(arr) {
  return arr.reduce((acc, val) => acc + hashString(String(val)), 0)
}

export function extractColorsFromImage(imageData) {
  if (!imageData || !imageData.data) return null

  const colorCounts = {}
  const data = imageData.data
  const step = 20 // Sample every 20th pixel for performance

  for (let i = 0; i < data.length; i += step * 4) {
    const r = Math.floor(data[i] / 32) * 32
    const g = Math.floor(data[i + 1] / 32) * 32
    const b = Math.floor(data[i + 2] / 32) * 32
    const a = data[i + 3]

    if (a < 128) continue

    const key = `${r},${g},${b}`
    colorCounts[key] = (colorCounts[key] || 0) + 1
  }

  const sorted = Object.entries(colorCounts)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 5)
    .map(([key]) => key.split(',').map(Number))

  return sorted.length > 0 ? sorted : [[180, 120, 80], [100, 60, 40]]
}

export function generateMonsterFromData(faceData, answers) {
  const seed = hashArray([
    faceData ? hashArray(faceData.colors?.flat() || [0]) : 0,
    ...(answers || []).map(a => hashString(String(a))),
  ])

  const rng = (max) => (seed % (max || 1)) % max
  const rngSeq = (arr) => arr[seed % arr.length]

  const primaryColor = faceData?.colors?.[0]
    ? `rgb(${faceData.colors[0][0]}, ${faceData.colors[0][1]}, ${faceData.colors[0][2]})`
    : `hsl(${(seed % 360)}, 70%, 60%)`

  const secondaryColor = faceData?.colors?.[1]
    ? `rgb(${faceData.colors[1][0]}, ${faceData.colors[1][1]}, ${faceData.colors[1][2]})`
    : `hsl(${(seed + 120) % 360}, 60%, 50%)`

  const accentColor = `hsl(${(seed + 240) % 360}, 80%, 80%)`

  const bodyType = rngSeq(BODY_TYPES)
  const element = rngSeq(ELEMENT_TYPES)
  const trait = rngSeq(PERSONALITY_TRAITS)
  const name = rngSeq(MONSTER_NAMES) + (seed % 100)

  return {
    id: `monster_${Date.now()}_${seed}`,
    name,
    bodyType,
    element,
    personality: trait,
    colors: {
      primary: primaryColor,
      secondary: secondaryColor,
      accent: accentColor,
    },
    faceImageData: faceData?.faceImageData || null,
    createdAt: Date.now(),
  }
}
