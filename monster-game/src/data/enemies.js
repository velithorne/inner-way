const ENEMY_NAMES = [
  'Shadow', 'Ember', 'Frost', 'Storm', 'Thorn', 'Mist', 'Flare', 'Breeze',
  'Cinder', 'Blitz', 'Vex', 'Grim', 'Sludge', 'Spark', 'Shade', 'Bolt',
]
const ENEMY_COLORS = ['#e76f51', '#e9c46a', '#2a9d8f', '#9b5de5', '#f15bb5', '#00bbf9']

function getEnemyStats(level, type = 'balanced') {
  const base = 40 + level * 12
  const multipliers = {
    aggressive: { hp: 0.9, attack: 1.2, defense: 0.8 },
    defensive: { hp: 1.2, attack: 0.8, defense: 1.2 },
    balanced: { hp: 1, attack: 1, defense: 1 },
  }
  const m = multipliers[type] || multipliers.balanced
  return {
    hp: Math.floor(base * 1.2 * m.hp),
    attack: Math.floor(base * 0.9 * m.attack),
    defense: Math.floor(base * 0.7 * m.defense),
  }
}

export function generateWildEnemy(playerLevel) {
  const variance = Math.floor(Math.random() * 5) - 2
  const level = Math.max(1, Math.min(50, playerLevel + variance))
  const types = ['aggressive', 'defensive', 'balanced']
  const type = types[Math.floor(Math.random() * types.length)]
  const name = ENEMY_NAMES[Math.floor(Math.random() * ENEMY_NAMES.length)] + (level % 100)
  const color = ENEMY_COLORS[Math.floor(Math.random() * ENEMY_COLORS.length)]
  const stats = getEnemyStats(level, type)
  const expReward = 30 + level * 2 + (type === 'aggressive' ? 10 : 0)
  return { name, level, color, ...stats, expReward, type: 'wild' }
}

export function getCampaignEnemy(stageId) {
  const stage = CAMPAIGN_STAGES[stageId]
  if (!stage) return null
  const stats = getEnemyStats(stage.enemyLevel, stage.enemyType || 'balanced')
  return {
    name: stage.enemyName,
    level: stage.enemyLevel,
    color: stage.enemyColor || '#e76f51',
    ...stats,
    expReward: stage.expReward,
    isBoss: stage.isBoss,
    type: 'campaign',
  }
}

export const CAMPAIGN_STAGES = [
  { id: 0, enemyName: 'Shadow Pup', enemyLevel: 1, enemyColor: '#4a4a6a', expReward: 40, isBoss: false },
  { id: 1, enemyName: 'Ember Spark', enemyLevel: 2, enemyColor: '#e76f51', expReward: 50, isBoss: false },
  { id: 2, enemyName: 'Frost Wisp', enemyLevel: 3, enemyColor: '#74b9ff', expReward: 60, isBoss: false },
  { id: 3, enemyName: 'Thorn Sprout', enemyLevel: 4, enemyColor: '#00b894', expReward: 70, isBoss: false },
  { id: 4, enemyName: 'Shadow Alpha', enemyLevel: 5, enemyColor: '#6c5ce7', expReward: 100, isBoss: true },
  { id: 5, enemyName: 'Blitz', enemyLevel: 6, enemyColor: '#fdcb6e', expReward: 80, isBoss: false },
  { id: 6, enemyName: 'Sludge', enemyLevel: 7, enemyColor: '#55efc4', expReward: 90, isBoss: false },
  { id: 7, enemyName: 'Vex', enemyLevel: 8, enemyColor: '#fd79a8', expReward: 100, isBoss: false },
  { id: 8, enemyName: 'Grim', enemyLevel: 9, enemyColor: '#2d3436', expReward: 110, isBoss: false },
  { id: 9, enemyName: 'Bolt Master', enemyLevel: 10, enemyColor: '#ffeaa7', expReward: 150, isBoss: true },
  { id: 10, enemyName: 'Storm Caller', enemyLevel: 11, enemyColor: '#81ecec', expReward: 120, isBoss: false },
  { id: 11, enemyName: 'Flare Beast', enemyLevel: 12, enemyColor: '#ff7675', expReward: 130, isBoss: false },
  { id: 12, enemyName: 'Shade Lord', enemyLevel: 13, enemyColor: '#a29bfe', expReward: 140, isBoss: false },
  { id: 13, enemyName: 'Mist Phantom', enemyLevel: 14, enemyColor: '#b2bec3', expReward: 150, isBoss: false },
  { id: 14, enemyName: 'Breeze Titan', enemyLevel: 15, enemyColor: '#dfe6e9', expReward: 200, isBoss: true },
  { id: 15, enemyName: 'Cinder King', enemyLevel: 16, enemyColor: '#d63031', expReward: 170, isBoss: false },
  { id: 16, enemyName: 'Frost Queen', enemyLevel: 18, enemyColor: '#0984e3', expReward: 190, isBoss: false },
  { id: 17, enemyName: 'Thorn Emperor', enemyLevel: 20, enemyColor: '#00cec9', expReward: 210, isBoss: false },
  { id: 18, enemyName: 'Storm Sovereign', enemyLevel: 22, enemyColor: '#6c5ce7', expReward: 230, isBoss: false },
  { id: 19, enemyName: 'Void Champion', enemyLevel: 25, enemyColor: '#2d3436', expReward: 300, isBoss: true },
]

export const ADVENTURE_NODES = [
  { id: 0, type: 'battle', difficulty: 1, expBonus: 0 },
  { id: 1, type: 'rest', reward: { energy: 20, hunger: 10 } },
  { id: 2, type: 'battle', difficulty: 2, expBonus: 10 },
  { id: 3, type: 'treasure', reward: { exp: 25 } },
  { id: 4, type: 'battle', difficulty: 3, expBonus: 20 },
  { id: 5, type: 'rest', reward: { energy: 15, hunger: 15 } },
  { id: 6, type: 'battle', difficulty: 4, expBonus: 30, isBoss: true },
]

export function getAdventureEnemy(playerLevel, node) {
  const level = Math.max(1, playerLevel + node.difficulty - 2)
  const baseStats = getEnemyStats(level, 'balanced')
  const name = (node.isBoss ? 'Adventure Boss ' : 'Wild ') + ENEMY_NAMES[level % ENEMY_NAMES.length]
  const expReward = 40 + level * 2 + (node.expBonus || 0)
  return {
    name,
    level,
    color: ENEMY_COLORS[node.id % ENEMY_COLORS.length],
    ...baseStats,
    expReward,
    isBoss: node.isBoss,
    type: 'adventure',
  }
}
