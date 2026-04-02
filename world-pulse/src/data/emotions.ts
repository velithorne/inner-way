import type { EmotionDefinition } from '../types';

export const EMOTIONS: EmotionDefinition[] = [
  {
    key: 'heavy',
    label: 'Heavy',
    color: '#3b1f6e',
    glowStrength: 0.7,
    soundCategory: 'low-drone',
  },
  {
    key: 'electric',
    label: 'Electric',
    color: '#00f5ff',
    glowStrength: 1.0,
    soundCategory: 'high-spark',
  },
  {
    key: 'hollow',
    label: 'Hollow',
    color: '#6b7280',
    glowStrength: 0.3,
    soundCategory: 'ambient-void',
  },
  {
    key: 'tender',
    label: 'Tender',
    color: '#f472b6',
    glowStrength: 0.6,
    soundCategory: 'soft-warm',
  },
  {
    key: 'anxious',
    label: 'Anxious',
    color: '#f59e0b',
    glowStrength: 0.85,
    soundCategory: 'staccato-pulse',
  },
  {
    key: 'calm',
    label: 'Calm',
    color: '#2dd4bf',
    glowStrength: 0.5,
    soundCategory: 'gentle-wave',
  },
  {
    key: 'raw',
    label: 'Raw',
    color: '#991b1b',
    glowStrength: 0.9,
    soundCategory: 'deep-rumble',
  },
  {
    key: 'wonder',
    label: 'Wonder',
    color: '#f6c90e',
    glowStrength: 0.8,
    soundCategory: 'shimmer',
  },
  {
    key: 'numb',
    label: 'Numb',
    color: '#475569',
    glowStrength: 0.25,
    soundCategory: 'flat-tone',
  },
  {
    key: 'alive',
    label: 'Alive',
    color: '#22c55e',
    glowStrength: 0.95,
    soundCategory: 'vivid-pulse',
  },
];

export const EMOTION_MAP = new Map(EMOTIONS.map((e) => [e.key, e]));

export function getEmotion(key: string): EmotionDefinition | undefined {
  return EMOTION_MAP.get(key as EmotionDefinition['key']);
}
