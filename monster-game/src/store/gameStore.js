import { create } from 'zustand'
import { persist } from 'zustand/middleware'

const CARE_DECAY_RATE = 0.02
const MAX_STATS = 100
const EVOLUTION_LEVELS = [8, 16, 24, 32]
const STAT_POINTS_PER_LEVEL = 2

// Scaling exp: each level requires more than the last (100*level + 25*level²)
export const getExpForLevel = (level) => Math.floor(100 * level + 25 * level * level)

export const useGameStore = create(
  persist(
    (set, get) => ({
      // Monster data
      monster: null,
      monsterCreatedAt: null,

      // Care stats (Tamagotchi-style)
      hunger: 80,
      happiness: 80,
      energy: 80,
      cleanliness: 80,
      lastCareUpdate: Date.now(),

      // Battle stats
      level: 1,
      exp: 0,
      wins: 0,
      losses: 0,

      // Evolution & allocated stats
      evolutionStage: 0,
      allocatedStats: { strength: 0, agility: 0, defense: 0 },
      pendingStatPoints: 0,

      // Game flow
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
        allocatedStats: { strength: 0, agility: 0, defense: 0 },
        pendingStatPoints: 0,
      }),

      feed: () => set((state) => ({
        hunger: Math.min(MAX_STATS, state.hunger + 25),
        lastCareUpdate: Date.now(),
      })),

      play: () => set((state) => ({
        happiness: Math.min(MAX_STATS, state.happiness + 20),
        energy: Math.max(0, state.energy - 15),
        cleanliness: Math.max(0, state.cleanliness - 15),
        lastCareUpdate: Date.now(),
      })),

      train: () => set((state) => {
        const newExp = state.exp + 15
        const expForLevel = getExpForLevel(state.level)
        if (newExp >= expForLevel) {
          const newLevel = state.level + 1
          const newStage = EVOLUTION_LEVELS.filter((lvl) => newLevel >= lvl).length
          return {
            hunger: Math.max(0, state.hunger - 10),
            energy: Math.max(0, state.energy - 20),
            exp: newExp - expForLevel,
            level: newLevel,
            pendingStatPoints: state.pendingStatPoints + STAT_POINTS_PER_LEVEL,
            evolutionStage: Math.max(state.evolutionStage, newStage),
            lastCareUpdate: Date.now(),
          }
        }
        return {
          hunger: Math.max(0, state.hunger - 10),
          energy: Math.max(0, state.energy - 20),
          exp: newExp,
          lastCareUpdate: Date.now(),
        }
      }),

      clean: () => set((state) => ({
        cleanliness: Math.min(MAX_STATS, state.cleanliness + 30),
        lastCareUpdate: Date.now(),
      })),

      rest: () => set((state) => ({
        energy: Math.min(MAX_STATS, state.energy + 35),
        hunger: Math.max(0, state.hunger - 5),
        lastCareUpdate: Date.now(),
      })),

      decayStats: () => {
        const state = get()
        const now = Date.now()
        const elapsed = (now - state.lastCareUpdate) / 1000 / 60

        if (elapsed < 1) return

        const decay = Math.min(elapsed * CARE_DECAY_RATE, 0.5)

        set({
          hunger: Math.max(0, state.hunger - decay * 10),
          happiness: Math.max(0, state.happiness - decay * 5),
          energy: Math.max(0, state.energy - decay * 3),
          cleanliness: Math.max(0, state.cleanliness - decay * 2),
          lastCareUpdate: now,
        })
      },

      addExp: (amount) => set((state) => {
        const newExp = state.exp + amount
        const expForLevel = getExpForLevel(state.level)
        if (newExp >= expForLevel) {
          const newLevel = state.level + 1
          const newStage = EVOLUTION_LEVELS.filter((lvl) => newLevel >= lvl).length
          return {
            exp: newExp - expForLevel,
            level: newLevel,
            pendingStatPoints: state.pendingStatPoints + STAT_POINTS_PER_LEVEL,
            evolutionStage: Math.max(state.evolutionStage, newStage),
          }
        }
        return { exp: newExp }
      }),

      allocateStat: (stat) => set((state) => {
        if (state.pendingStatPoints <= 0) return state
        if (!state.allocatedStats[stat]) return state
        return {
          pendingStatPoints: state.pendingStatPoints - 1,
          allocatedStats: {
            ...state.allocatedStats,
            [stat]: state.allocatedStats[stat] + 1,
          },
        }
      }),

      dirtyFromBattle: () => set((state) => ({
        cleanliness: Math.max(0, state.cleanliness - 25),
        lastCareUpdate: Date.now(),
      })),

      recordBattleWin: () => set((state) => {
        const newExp = state.exp + 50
        const expForLevel = getExpForLevel(state.level)
        if (newExp >= expForLevel) {
          const newLevel = state.level + 1
          const newStage = EVOLUTION_LEVELS.filter((lvl) => newLevel >= lvl).length
          return {
            wins: state.wins + 1,
            exp: newExp - expForLevel,
            level: newLevel,
            pendingStatPoints: state.pendingStatPoints + STAT_POINTS_PER_LEVEL,
            evolutionStage: Math.max(state.evolutionStage, newStage),
          }
        }
        return { wins: state.wins + 1, exp: newExp }
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
        allocatedStats: { strength: 0, agility: 0, defense: 0 },
        pendingStatPoints: 0,
      }),
    }),
    { name: 'monster-soul-game' }
  )
)
