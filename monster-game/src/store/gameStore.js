import { create } from 'zustand'
import { persist } from 'zustand/middleware'

const CARE_DECAY_RATE = 0.02
const MAX_STATS = 100
const EVOLUTION_LEVELS = [8, 16, 24, 32]
const STAT_POINTS_PER_LEVEL = 2
const LEVEL_UP_CARE_BONUS = 8
const RECENT_ACTION_WINDOW = 5
const DIMINISH_WINDOW_MS = 2 * 60 * 1000 // 2 minutes

export const getExpForLevel = (level) => Math.floor(100 * level + 25 * level * level)

function getMoodMultiplier(happiness) {
  if (happiness < 30) return 0.75
  if (happiness < 50) return 0.9
  if (happiness < 70) return 1.0
  if (happiness < 90) return 1.1
  return 1.2
}

function getVarietyMultiplier(lastActions, currentAction) {
  if (!lastActions?.length) return 1.0
  const recent = lastActions.slice(-2)
  const sameCount = recent.filter((a) => a === currentAction).length
  if (sameCount >= 2) return 0.85
  if (recent[recent.length - 1] !== currentAction) return 1.1
  return 1.0
}

function computeEvolutionTraits(careCounts) {
  const total = careCounts.feed + careCounts.play + careCounts.train + careCounts.clean + careCounts.rest
  if (total === 0) return { build: 'balanced', demeanor: 'calm', style: 'standard' }

  const feedRatio = careCounts.feed / total
  const playRatio = careCounts.play / total
  const trainRatio = careCounts.train / total
  const cleanRatio = careCounts.clean / total
  const restRatio = careCounts.rest / total

  const build =
    trainRatio > 0.35 ? 'athletic' : feedRatio > 0.35 ? 'plump' : restRatio > 0.3 ? 'serene' : 'balanced'
  const demeanor =
    playRatio > 0.3 ? 'playful' : trainRatio > 0.3 ? 'ferocious' : cleanRatio > 0.25 ? 'pristine' : 'calm'
  const style =
    feedRatio > playRatio && feedRatio > trainRatio ? 'nurtured' : trainRatio > 0.25 ? 'battlehardened' : 'standard'

  return { build, demeanor, style }
}

export const useGameStore = create(
  persist(
    (set, get) => ({
      monster: null,
      monsterCreatedAt: null,
      hunger: 80,
      happiness: 80,
      energy: 80,
      cleanliness: 80,
      lastCareUpdate: Date.now(),
      level: 1,
      exp: 0,
      wins: 0,
      losses: 0,
      evolutionStage: 0,
      evolutionTraits: { build: 'balanced', demeanor: 'calm', style: 'standard' },
      allocatedStats: { strength: 0, agility: 0, defense: 0 },
      pendingStatPoints: 0,
      feedbackMessage: null,
      careCounts: { feed: 0, play: 0, train: 0, clean: 0, rest: 0 },
      lastActions: [],
      lastActionTimes: {},
      campaignStage: 0,
      adventureNode: 0,

      hasMonster: () => !!get().monster,

      createMonster: (monsterData) => set({
        monster: monsterData,
        monsterCreatedAt: Date.now(),
        hunger: 80,
        happiness: 80,
        energy: 80,
        cleanliness: 80,
        lastCareUpdate: Date.now(),
        level: 1,
        exp: 0,
        evolutionStage: 0,
        evolutionTraits: { build: 'balanced', demeanor: 'calm', style: 'standard' },
        allocatedStats: { strength: 0, agility: 0, defense: 0 },
        pendingStatPoints: 0,
        feedbackMessage: null,
        careCounts: { feed: 0, play: 0, train: 0, clean: 0, rest: 0 },
        lastActions: [],
        lastActionTimes: {},
        campaignStage: 0,
        adventureNode: 0,
      }),

      feed: () => set((state) => {
        const mood = getMoodMultiplier(state.happiness)
        const variety = getVarietyMultiplier(state.lastActions, 'feed')
        let gain = Math.floor(25 * mood * variety)

        const lastTwo = state.lastActions.slice(-2)
        if (lastTwo.filter((a) => a === 'feed').length >= 2) {
          gain = Math.floor(gain * 0.7)
        }
        if (state.hunger > 85) {
          gain = Math.floor(gain * 0.5)
          return {
            hunger: Math.min(MAX_STATS, state.hunger + gain),
            happiness: Math.max(0, state.happiness - 8),
            careCounts: { ...state.careCounts, feed: state.careCounts.feed + 1 },
            lastActions: [...state.lastActions.slice(-RECENT_ACTION_WINDOW), 'feed'],
            lastActionTimes: { ...state.lastActionTimes, feed: Date.now() },
            feedbackMessage: 'Overfed!',
            lastCareUpdate: Date.now(),
          }
        }

        return {
          hunger: Math.min(MAX_STATS, state.hunger + gain),
          careCounts: { ...state.careCounts, feed: state.careCounts.feed + 1 },
          lastActions: [...state.lastActions.slice(-RECENT_ACTION_WINDOW), 'feed'],
          lastActionTimes: { ...state.lastActionTimes, feed: Date.now() },
          feedbackMessage: null,
          lastCareUpdate: Date.now(),
        }
      }),

      play: () => set((state) => {
        const mood = getMoodMultiplier(state.happiness)
        const variety = getVarietyMultiplier(state.lastActions, 'play')
        let happinessGain = Math.floor(20 * mood * variety)
        let energyCost = 15
        let cleanlinessCost = 15

        const lastTwo = state.lastActions.slice(-2)
        if (lastTwo.filter((a) => a === 'play').length >= 2) {
          cleanlinessCost += 8
        }
        if (state.lastActions[state.lastActions.length - 1] === 'feed') {
          energyCost += 5
        }

        return {
          happiness: Math.min(MAX_STATS, state.happiness + happinessGain),
          energy: Math.max(0, state.energy - energyCost),
          cleanliness: Math.max(0, state.cleanliness - cleanlinessCost),
          careCounts: { ...state.careCounts, play: state.careCounts.play + 1 },
          lastActions: [...state.lastActions.slice(-RECENT_ACTION_WINDOW), 'play'],
          lastActionTimes: { ...state.lastActionTimes, play: Date.now() },
          feedbackMessage: null,
          lastCareUpdate: Date.now(),
        }
      }),

      train: () => set((state) => {
        const noEnergy = state.energy <= 0
        const noHunger = state.hunger <= 0

        if (noEnergy || noHunger) {
          let happiness = state.happiness
          let cleanliness = state.cleanliness
          const messages = []
          if (noEnergy) {
            happiness -= 25
            cleanliness -= 10
            messages.push('Too exhausted!')
          }
          if (noHunger) {
            happiness -= 20
            messages.push('Too hungry!')
          }
          return {
            happiness: Math.max(0, happiness),
            cleanliness: Math.max(0, cleanliness),
            feedbackMessage: messages.join(' '),
            lastCareUpdate: Date.now(),
          }
        }

        const mood = getMoodMultiplier(state.happiness)
        const variety = getVarietyMultiplier(state.lastActions, 'train')
        let expGain = 15
        if (state.happiness < 40) expGain = Math.floor(expGain * 0.7)
        expGain = Math.floor(expGain * mood * variety)

        const newExp = state.exp + expGain
        const expForLevel = getExpForLevel(state.level)
        if (newExp >= expForLevel) {
          const newLevel = state.level + 1
          const newStage = EVOLUTION_LEVELS.filter((lvl) => newLevel >= lvl).length
          const newTraits = newStage > state.evolutionStage ? computeEvolutionTraits(state.careCounts) : state.evolutionTraits
          return {
            hunger: Math.min(MAX_STATS, Math.max(0, state.hunger - 10) + LEVEL_UP_CARE_BONUS),
            energy: Math.min(MAX_STATS, Math.max(0, state.energy - 20) + LEVEL_UP_CARE_BONUS),
            happiness: Math.min(MAX_STATS, state.happiness + LEVEL_UP_CARE_BONUS),
            cleanliness: Math.min(MAX_STATS, state.cleanliness + LEVEL_UP_CARE_BONUS),
            exp: newExp - expForLevel,
            level: newLevel,
            evolutionStage: Math.max(state.evolutionStage, newStage),
            evolutionTraits: newTraits,
            pendingStatPoints: state.pendingStatPoints + STAT_POINTS_PER_LEVEL,
            careCounts: { ...state.careCounts, train: state.careCounts.train + 1 },
            lastActions: [...state.lastActions.slice(-RECENT_ACTION_WINDOW), 'train'],
            lastActionTimes: { ...state.lastActionTimes, train: Date.now() },
            lastCareUpdate: Date.now(),
          }
        }
        return {
          hunger: Math.max(0, state.hunger - 10),
          energy: Math.max(0, state.energy - 20),
          exp: newExp,
          careCounts: { ...state.careCounts, train: state.careCounts.train + 1 },
          lastActions: [...state.lastActions.slice(-RECENT_ACTION_WINDOW), 'train'],
          lastActionTimes: { ...state.lastActionTimes, train: Date.now() },
          lastCareUpdate: Date.now(),
        }
      }),

      clean: () => set((state) => {
        const mood = getMoodMultiplier(state.happiness)
        const variety = getVarietyMultiplier(state.lastActions, 'clean')
        let gain = Math.floor(30 * mood * variety)
        if (state.energy < 30) gain = Math.floor(gain * 0.75)

        return {
          cleanliness: Math.min(MAX_STATS, state.cleanliness + gain),
          careCounts: { ...state.careCounts, clean: state.careCounts.clean + 1 },
          lastActions: [...state.lastActions.slice(-RECENT_ACTION_WINDOW), 'clean'],
          lastActionTimes: { ...state.lastActionTimes, clean: Date.now() },
          feedbackMessage: null,
          lastCareUpdate: Date.now(),
        }
      }),

      rest: () => set((state) => {
        const mood = getMoodMultiplier(state.happiness)
        const variety = getVarietyMultiplier(state.lastActions, 'rest')

        let energyGain = 22
        let hungerCost = 15

        if (state.hunger < 30) energyGain = Math.floor(energyGain * 0.6)
        if (state.cleanliness < 30) energyGain = Math.floor(energyGain * 0.6)
        energyGain = Math.floor(energyGain * mood * variety)

        const lastTwo = state.lastActions.slice(-2)
        if (lastTwo.filter((a) => a === 'rest').length >= 2) {
          energyGain = Math.floor(energyGain * 0.5)
        }

        let happinessChange = 0
        if (state.energy > 85) {
          happinessChange = -12
        }

        return {
          energy: Math.min(MAX_STATS, state.energy + energyGain),
          hunger: Math.max(0, state.hunger - hungerCost),
          happiness: Math.max(0, state.happiness + happinessChange),
          careCounts: { ...state.careCounts, rest: state.careCounts.rest + 1 },
          lastActions: [...state.lastActions.slice(-RECENT_ACTION_WINDOW), 'rest'],
          lastActionTimes: { ...state.lastActionTimes, rest: Date.now() },
          feedbackMessage: state.energy > 85 ? 'Too much rest!' : null,
          lastCareUpdate: Date.now(),
        }
      }),

      decayStats: () => {
        const state = get()
        const now = Date.now()
        const elapsed = (now - state.lastCareUpdate) / 1000 / 60

        if (elapsed < 1) return

        let decay = Math.min(elapsed * CARE_DECAY_RATE, 0.5)
        let hungerDecay = decay * 10
        let happinessDecay = decay * 5
        let energyDecay = decay * 3
        let cleanlinessDecay = decay * 2

        if (state.hunger < 20) happinessDecay *= 2
        if (state.energy < 20) cleanlinessDecay *= 1.5

        set({
          hunger: Math.max(0, state.hunger - hungerDecay),
          happiness: Math.max(0, state.happiness - happinessDecay),
          energy: Math.max(0, state.energy - energyDecay),
          cleanliness: Math.max(0, state.cleanliness - cleanlinessDecay),
          lastCareUpdate: now,
        })
      },

      addExp: (amount) => set((state) => {
        const newExp = state.exp + amount
        const expForLevel = getExpForLevel(state.level)
        if (newExp >= expForLevel) {
          const newLevel = state.level + 1
          const newStage = EVOLUTION_LEVELS.filter((lvl) => newLevel >= lvl).length
          const newTraits = newStage > state.evolutionStage ? computeEvolutionTraits(state.careCounts) : state.evolutionTraits
          return {
            exp: newExp - expForLevel,
            level: newLevel,
            hunger: Math.min(MAX_STATS, state.hunger + LEVEL_UP_CARE_BONUS),
            happiness: Math.min(MAX_STATS, state.happiness + LEVEL_UP_CARE_BONUS),
            energy: Math.min(MAX_STATS, state.energy + LEVEL_UP_CARE_BONUS),
            cleanliness: Math.min(MAX_STATS, state.cleanliness + LEVEL_UP_CARE_BONUS),
            pendingStatPoints: state.pendingStatPoints + STAT_POINTS_PER_LEVEL,
            evolutionStage: Math.max(state.evolutionStage, newStage),
            evolutionTraits: newTraits,
            lastCareUpdate: Date.now(),
          }
        }
        return { exp: newExp }
      }),

      allocateStat: (stat) => set((state) => {
        if (state.pendingStatPoints <= 0) return state
        if (!(stat in state.allocatedStats)) return state
        return {
          pendingStatPoints: state.pendingStatPoints - 1,
          allocatedStats: {
            ...state.allocatedStats,
            [stat]: state.allocatedStats[stat] + 1,
          },
        }
      }),

      dirtyFromBattle: () => set((state) => ({
        energy: Math.max(0, state.energy - 25),
        hunger: Math.max(0, state.hunger - 15),
        cleanliness: Math.max(0, state.cleanliness - 25),
        lastCareUpdate: Date.now(),
      })),

      recordBattleWin: (customExp = 50) => set((state) => {
        const newExp = state.exp + customExp
        const expForLevel = getExpForLevel(state.level)
        if (newExp >= expForLevel) {
          const newLevel = state.level + 1
          const newStage = EVOLUTION_LEVELS.filter((lvl) => newLevel >= lvl).length
          const newTraits = newStage > state.evolutionStage ? computeEvolutionTraits(state.careCounts) : state.evolutionTraits
          return {
            wins: state.wins + 1,
            exp: newExp - expForLevel,
            level: newLevel,
            hunger: Math.min(MAX_STATS, state.hunger + LEVEL_UP_CARE_BONUS),
            happiness: Math.min(MAX_STATS, state.happiness + LEVEL_UP_CARE_BONUS),
            energy: Math.min(MAX_STATS, state.energy + LEVEL_UP_CARE_BONUS),
            cleanliness: Math.min(MAX_STATS, state.cleanliness + LEVEL_UP_CARE_BONUS),
            pendingStatPoints: state.pendingStatPoints + STAT_POINTS_PER_LEVEL,
            evolutionStage: Math.max(state.evolutionStage, newStage),
            evolutionTraits: newTraits,
            lastCareUpdate: Date.now(),
          }
        }
        return { wins: state.wins + 1, exp: newExp }
      }),

      advanceCampaignStage: () => set((state) => ({
        campaignStage: Math.min(state.campaignStage + 1, 20),
      })),

      advanceAdventureNode: () => set((state) => ({
        adventureNode: Math.min(state.adventureNode + 1, 7),
      })),

      resetAdventure: () => set({ adventureNode: 0 }),

      applyAdventureReward: (reward) => set((state) => {
        const updates = {}
        if (reward.energy) updates.energy = Math.min(MAX_STATS, state.energy + reward.energy)
        if (reward.hunger) updates.hunger = Math.min(MAX_STATS, state.hunger + reward.hunger)
        return updates
      }),

      recordBattleLoss: () => set((state) => ({
        losses: state.losses + 1,
      })),

      getBattleStats: () => {
        const state = get()
        const avgCare = (state.hunger + state.happiness + state.energy + state.cleanliness) / 4
        const base = 50 + (state.level * 10) + (avgCare / 4)
        const { strength, agility, defense } = state.allocatedStats || { strength: 0, agility: 0, defense: 0 }
        const evoBonus = 1 + state.evolutionStage * 0.15
        return {
          hp: Math.floor((base * 1.2 + defense * 8) * evoBonus),
          attack: Math.floor((base * 0.8 + state.exp / 10 + strength * 6) * evoBonus),
          defense: Math.floor((base * 0.6 + state.cleanliness / 5 + defense * 5) * evoBonus),
          speed: Math.floor((base * 0.5 + state.energy / 5 + agility * 6) * evoBonus),
        }
      },

      resetGame: () => set({
        monster: null,
        monsterCreatedAt: null,
        hunger: 80,
        happiness: 80,
        energy: 80,
        cleanliness: 80,
        lastCareUpdate: Date.now(),
        level: 1,
        exp: 0,
        wins: 0,
        losses: 0,
        evolutionStage: 0,
        evolutionTraits: { build: 'balanced', demeanor: 'calm', style: 'standard' },
        allocatedStats: { strength: 0, agility: 0, defense: 0 },
        pendingStatPoints: 0,
        feedbackMessage: null,
        careCounts: { feed: 0, play: 0, train: 0, clean: 0, rest: 0 },
        lastActions: [],
        lastActionTimes: {},
        campaignStage: 0,
        adventureNode: 0,
      }),
    }),
    {
      name: 'monster-soul-game',
      partialize: (state) => {
        const { feedbackMessage, lastActionTimes, ...rest } = state
        return rest
      },
    }
  )
)
