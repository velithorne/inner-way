import { create } from 'zustand'
import { persist } from 'zustand/middleware'

const CARE_DECAY_RATE = 0.02
const MAX_STATS = 100

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

      // Battle stats (derived from care + training)
      level: 1,
      exp: 0,
      wins: 0,
      losses: 0,

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
      }),

      feed: () => set((state) => ({
        hunger: Math.min(MAX_STATS, state.hunger + 25),
        lastCareUpdate: Date.now(),
      })),

      play: () => set((state) => ({
        happiness: Math.min(MAX_STATS, state.happiness + 20),
        energy: Math.max(0, state.energy - 15),
        lastCareUpdate: Date.now(),
      })),

      train: () => set((state) => ({
        hunger: Math.max(0, state.hunger - 10),
        energy: Math.max(0, state.energy - 20),
        exp: state.exp + 15,
        lastCareUpdate: Date.now(),
      })),

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
        const elapsed = (now - state.lastCareUpdate) / 1000 / 60 // minutes

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
        const expForLevel = state.level * 100
        if (newExp >= expForLevel) {
          return {
            exp: newExp - expForLevel,
            level: state.level + 1,
          }
        }
        return { exp: newExp }
      }),

      recordBattleWin: () => set((state) => ({
        wins: state.wins + 1,
        exp: state.exp + 50,
      })),

      recordBattleLoss: () => set((state) => ({
        losses: state.losses + 1,
      })),

      getBattleStats: () => {
        const state = get()
        const avgCare = (state.hunger + state.happiness + state.energy + state.cleanliness) / 4
        const base = 50 + (state.level * 10) + (avgCare / 4)
        return {
          hp: Math.floor(base * 1.2),
          attack: Math.floor(base * 0.8 + state.exp / 10),
          defense: Math.floor(base * 0.6 + state.cleanliness / 5),
          speed: Math.floor(base * 0.5 + state.energy / 5),
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
      }),
    }),
    { name: 'monster-soul-game' }
  )
)
