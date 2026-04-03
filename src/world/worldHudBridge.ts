import type { BoundaryState } from './worldNavigationBridge';

let boundaryState: BoundaryState = 'ok';
let fps = 0;
let drawCalls = 0;
let fpsOverlay = false;
let forwardDir = { x: 0, y: 0, z: -1 };

export function setHudBoundary(s: BoundaryState): void {
  boundaryState = s;
}

export function getHudBoundary(): BoundaryState {
  return boundaryState;
}

export function setHudFps(f: number, draws: number): void {
  fps = f;
  drawCalls = draws;
}

export function getHudFps(): { fps: number; drawCalls: number } {
  return { fps, drawCalls };
}

export function setFpsOverlay(v: boolean): void {
  fpsOverlay = v;
}

export function getFpsOverlay(): boolean {
  return fpsOverlay;
}

export function toggleFpsOverlay(): boolean {
  fpsOverlay = !fpsOverlay;
  return fpsOverlay;
}

export function setHudForwardDir(x: number, y: number, z: number): void {
  forwardDir = { x, y, z };
}

export function getHudForwardDir(): { x: number; y: number; z: number } {
  return { ...forwardDir };
}
