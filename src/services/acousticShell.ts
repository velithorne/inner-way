/**
 * DECODE Phase 1 — Acoustic Shell (blueprint §7, §8).
 * Log sine sweep → simultaneous recording → deconvolution → room geometry.
 */
import { Audio } from 'expo-av';
import { cacheDirectory, writeAsStringAsync, EncodingType } from 'expo-file-system/legacy';
import * as Location from 'expo-location';

import { processSweepAndRecorded } from '../workers/acousticWorker';
import type { RoomGeometry } from '../types/decode';
import { useDecodeStore } from '../store/useDecodeStore';

const SAMPLE_RATE = 44100;
const SWEEP_DURATION_SEC = 2;
const F0 = 20;
const F1 = 200;

function writeString(view: DataView, offset: number, s: string) {
  for (let i = 0; i < s.length; i++) view.setUint8(offset + i, s.charCodeAt(i));
}

function floatToWav(samples: Float32Array, sampleRate: number): Uint8Array {
  const n = samples.length;
  const buffer = new ArrayBuffer(44 + n * 2);
  const view = new DataView(buffer);
  writeString(view, 0, 'RIFF');
  view.setUint32(4, 36 + n * 2, true);
  writeString(view, 8, 'WAVE');
  writeString(view, 12, 'fmt ');
  view.setUint32(16, 16, true);
  view.setUint16(20, 1, true);
  view.setUint16(22, 1, true);
  view.setUint32(24, sampleRate, true);
  view.setUint32(28, sampleRate * 2, true);
  view.setUint16(32, 2, true);
  view.setUint16(34, 16, true);
  writeString(view, 36, 'data');
  view.setUint32(40, n * 2, true);
  let o = 44;
  for (let i = 0; i < n; i++) {
    const s = Math.max(-1, Math.min(1, samples[i]!));
    view.setInt16(o, s < 0 ? s * 0x8000 : s * 0x7fff, true);
    o += 2;
  }
  return new Uint8Array(buffer);
}

export function generateLogSweep(
  sampleRate: number,
  durationSec: number,
  f0: number,
  f1: number
): Float32Array {
  const n = Math.floor(sampleRate * durationSec);
  const out = new Float32Array(n);
  let phase = 0;
  for (let i = 0; i < n; i++) {
    const t = i / sampleRate;
    const frac = durationSec > 0 ? t / durationSec : 0;
    const f = f0 * Math.pow(f1 / f0, frac);
    phase += (2 * Math.PI * f) / sampleRate;
    out[i] = Math.sin(phase) * 0.35;
  }
  return out;
}

async function decodeFileToMonoNormalized(uri: string): Promise<Float32Array> {
  const sound = new Audio.Sound();
  const frames: number[] = [];

  await sound.loadAsync({ uri }, { shouldPlay: false, volume: 0 }, true);
  sound.setOnAudioSampleReceived((sample) => {
    const ch0 = sample.channels[0]?.frames;
    if (ch0?.length) {
      for (const f of ch0) frames.push(f);
    }
  });

  await new Promise<void>((resolve, reject) => {
    sound.setOnPlaybackStatusUpdate((s) => {
      if (!s.isLoaded) return;
      if (s.didJustFinish) resolve();
    });
    sound.playAsync().catch(reject);
    setTimeout(resolve, 12_000);
  });

  await sound.stopAsync();
  await sound.unloadAsync();

  if (frames.length === 0) {
    return new Float32Array(SAMPLE_RATE * SWEEP_DURATION_SEC);
  }
  return Float32Array.from(frames);
}

let monitoringTimer: ReturnType<typeof setInterval> | null = null;
let locationSub: Location.LocationSubscription | null = null;
let lastLat: number | null = null;
let lastLon: number | null = null;

export async function sweepAndMeasure(): Promise<RoomGeometry> {
  useDecodeStore.getState().setSweepStatus('measuring');

  await Audio.setAudioModeAsync({
    allowsRecordingIOS: true,
    playsInSilentModeIOS: true,
    staysActiveInBackground: false,
    shouldDuckAndroid: true,
    playThroughEarpieceAndroid: false,
  });

  const perm = await Audio.requestPermissionsAsync();
  if (!perm.granted) {
    useDecodeStore.getState().setSweepStatus('idle');
    throw new Error('Microphone permission is required for the acoustic shell.');
  }

  const sweep = generateLogSweep(SAMPLE_RATE, SWEEP_DURATION_SEC, F0, F1);
  const wav = floatToWav(sweep, SAMPLE_RATE);
  if (!cacheDirectory) {
    useDecodeStore.getState().setSweepStatus('idle');
    throw new Error('Cache directory unavailable; cannot write reference sweep.');
  }
  const sweepPath = `${cacheDirectory}decode_ref_sweep.wav`;
  await writeAsStringAsync(sweepPath, uint8ToBase64(wav), {
    encoding: EncodingType.Base64,
  });

  const recording = new Audio.Recording();
  await recording.prepareToRecordAsync(Audio.RecordingOptionsPresets.HIGH_QUALITY);
  await recording.startAsync();

  const playSound = new Audio.Sound();
  await playSound.loadAsync({ uri: sweepPath }, { shouldPlay: true, volume: 0.45 }, true);

  await new Promise<void>((resolve) => {
    playSound.setOnPlaybackStatusUpdate((s) => {
      if (s.isLoaded && s.didJustFinish) resolve();
    });
    void playSound.playAsync();
    setTimeout(resolve, (SWEEP_DURATION_SEC + 0.5) * 1000);
  });

  await playSound.stopAsync();
  await playSound.unloadAsync();

  await recording.stopAndUnloadAsync();
  const uri = recording.getURI();
  if (!uri) {
    useDecodeStore.getState().setSweepStatus('idle');
    throw new Error('Recording produced no file.');
  }

  const recordedNorm = await decodeFileToMonoNormalized(uri);
  const targetLen = Math.min(sweep.length, recordedNorm.length);
  const sweepTrim = sweep.slice(0, targetLen);
  const recTrim = recordedNorm.slice(0, targetLen);

  const geom = processSweepAndRecorded(sweepTrim, recTrim, SAMPLE_RATE);
  useDecodeStore.getState().setRoomGeometry(geom);
  useDecodeStore.getState().setSweepStatus(geom.confidence >= 40 ? 'locked' : 'idle');
  return geom;
}

export function stopContinuousMonitoring(): void {
  if (monitoringTimer) {
    clearInterval(monitoringTimer);
    monitoringTimer = null;
  }
  locationSub?.remove();
  locationSub = null;
}

export async function startContinuousMonitoring(): Promise<void> {
  stopContinuousMonitoring();

  const { status } = await Location.requestForegroundPermissionsAsync();
  if (status === 'granted') {
    locationSub = await Location.watchPositionAsync(
      { accuracy: Location.Accuracy.Balanced, distanceInterval: 5 },
      (pos) => {
        const lat = pos.coords.latitude;
        const lon = pos.coords.longitude;
        if (lastLat != null && lastLon != null) {
          const d = haversineM(lastLat, lastLon, lat, lon);
          if (d > 5) {
            void sweepAndMeasure();
          }
        }
        lastLat = lat;
        lastLon = lon;
      }
    );
  }

  monitoringTimer = setInterval(() => {
    void sweepAndMeasure();
  }, 30_000);

  void sweepAndMeasure();
}

function uint8ToBase64(bytes: Uint8Array): string {
  let bin = '';
  for (let i = 0; i < bytes.length; i++) bin += String.fromCharCode(bytes[i]!);
  return btoa(bin);
}

function haversineM(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const R = 6371000;
  const p1 = (lat1 * Math.PI) / 180;
  const p2 = (lat2 * Math.PI) / 180;
  const dp = ((lat2 - lat1) * Math.PI) / 180;
  const dl = ((lon2 - lon1) * Math.PI) / 180;
  const a =
    Math.sin(dp / 2) * Math.sin(dp / 2) +
    Math.cos(p1) * Math.cos(p2) * Math.sin(dl / 2) * Math.sin(dl / 2);
  return 2 * R * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}
