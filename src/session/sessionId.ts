import * as Crypto from 'expo-crypto';

let cached: string | null = null;

export function getSessionId(): string {
  if (!cached) {
    cached = Crypto.randomUUID();
  }
  return cached;
}
