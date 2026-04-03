import * as THREE from 'three';

/** Joystick -1..1 each axis; up on stick = forward (-y in screen = forward) */
let joyX = 0;
let joyY = 0;

/** FPS look (radians), YXZ order */
let yaw = 0;
let pitch = 0;

let verticalUp = false;
let verticalDown = false;

/** Movement speed: 1 or 4 only (double-tap joystick toggles) */
let speedMult = 1;

const DEFAULT_FOV = 65;
const MIN_FOV = 25;
const MAX_FOV = 100;
let targetFov = DEFAULT_FOV;

const tmpV = new THREE.Vector3();
const tmpE = new THREE.Euler();

const fly = {
  active: false,
  t: 0,
  duration: 1.5,
  fromPos: new THREE.Vector3(),
  toPos: new THREE.Vector3(),
  fromQuat: new THREE.Quaternion(),
  toQuat: new THREE.Quaternion(),
};

const savedPos = new THREE.Vector3();
const savedQuat = new THREE.Quaternion();

let navigationEnabled = false;

export function setNavigationEnabled(v: boolean): void {
  navigationEnabled = v;
}

export function isNavigationEnabled(): boolean {
  return navigationEnabled;
}

export function setJoystick(x: number, y: number): void {
  joyX = x;
  joyY = y;
}

export function getJoystick(): { x: number; y: number } {
  return { x: joyX, y: joyY };
}

export function addLookDelta(dx: number, dy: number): void {
  yaw -= dx * 0.0025;
  pitch -= dy * 0.002;
  pitch = THREE.MathUtils.clamp(pitch, -Math.PI / 2.2, Math.PI / 2.2);
}

export function getYawPitch(): { yaw: number; pitch: number } {
  return { yaw, pitch };
}

export function syncYawPitchFromCamera(camera: THREE.PerspectiveCamera): void {
  tmpE.setFromQuaternion(camera.quaternion, 'YXZ');
  yaw = tmpE.y;
  pitch = tmpE.x;
}

export function setVerticalUp(v: boolean): void {
  verticalUp = v;
}

export function setVerticalDown(v: boolean): void {
  verticalDown = v;
}

export function toggleMovementSpeed(): void {
  speedMult = speedMult >= 2 ? 1 : 4;
}

export function getSpeedMult(): number {
  return speedMult;
}

/** Pinch: spread increases scale → zoom in → lower FOV */
export function applyPinchToTargetFov(prevScale: number, scale: number): void {
  if (prevScale <= 0 || scale <= 0) return;
  const ratio = prevScale / scale;
  targetFov *= ratio;
  targetFov = THREE.MathUtils.clamp(targetFov, MIN_FOV, MAX_FOV);
}

export function getTargetFov(): number {
  return targetFov;
}

/**
 * Smooth FOV toward target each frame (exponential smoothing).
 */
export function applyFovLerp(camera: THREE.PerspectiveCamera, dt: number): void {
  const k = 1 - Math.pow(0.1, Math.min(1, dt * 60));
  camera.fov += (targetFov - camera.fov) * k;
  camera.updateProjectionMatrix();
}

export function startFlyTo(toPos: THREE.Vector3, lookAt: THREE.Vector3, camera: THREE.PerspectiveCamera): void {
  fly.active = true;
  fly.t = 0;
  fly.duration = 1.5;
  fly.fromPos.copy(camera.position);
  fly.fromQuat.copy(camera.quaternion);
  fly.toPos.copy(toPos);
  savedPos.copy(camera.position);
  savedQuat.copy(camera.quaternion);
  camera.position.copy(toPos);
  camera.lookAt(lookAt);
  fly.toQuat.copy(camera.quaternion);
  camera.position.copy(savedPos);
  camera.quaternion.copy(savedQuat);
}

export function isFlyActive(): boolean {
  return fly.active;
}

let queuedFly: { to: THREE.Vector3; look: THREE.Vector3 } | null = null;

export function queueFlyTo(to: THREE.Vector3, lookAt: THREE.Vector3): void {
  queuedFly = { to: to.clone(), look: lookAt.clone() };
}

export function processQueuedFly(camera: THREE.PerspectiveCamera): void {
  if (!queuedFly) return;
  startFlyTo(queuedFly.to, queuedFly.look, camera);
  queuedFly = null;
}

export function stepFly(
  camera: THREE.PerspectiveCamera,
  dt: number,
): void {
  if (!fly.active) return;
  fly.t += dt;
  const u = Math.min(1, fly.t / fly.duration);
  const ease = 1 - Math.pow(1 - u, 3);
  camera.position.lerpVectors(fly.fromPos, fly.toPos, ease);
  camera.quaternion.slerpQuaternions(fly.fromQuat, fly.toQuat, ease);
  if (u >= 1) {
    fly.active = false;
    tmpE.setFromQuaternion(camera.quaternion, 'YXZ');
    yaw = tmpE.y;
    pitch = tmpE.x;
  }
}

const forward = new THREE.Vector3();
const right = new THREE.Vector3();

export function applyFreeCamera(
  camera: THREE.PerspectiveCamera,
  dt: number,
  opts: { entryComplete: boolean },
): void {
  if (!opts.entryComplete || !navigationEnabled) return;
  if (fly.active) return;

  tmpE.set(pitch, yaw, 0, 'YXZ');
  camera.quaternion.setFromEuler(tmpE);

  const mag = Math.hypot(joyX, joyY);
  const base = 1.2;
  const max = 4;
  const speed = (base + (max - base) * Math.min(1, mag)) * speedMult;

  camera.getWorldDirection(forward);
  right.crossVectors(forward, camera.up).normalize();

  camera.position.addScaledVector(forward, -joyY * speed);
  camera.position.addScaledVector(right, joyX * speed);

  const vSpeed = speed * 0.85;
  if (verticalUp) camera.position.y += vSpeed;
  if (verticalDown) camera.position.y -= vSpeed;
}

const ORIGIN = new THREE.Vector3(0, 0, 0);

export type BoundaryState = 'ok' | 'soft' | 'hard';

export function applyWorldBoundary(
  camera: THREE.PerspectiveCamera,
): { state: BoundaryState; dist: number } {
  const dist = camera.position.distanceTo(ORIGIN);
  if (dist > 400) {
    const toOrigin = tmpV.copy(ORIGIN).sub(camera.position).normalize();
    camera.position.addScaledVector(toOrigin, dist - 400);
    return { state: 'hard', dist: camera.position.distanceTo(ORIGIN) };
  }
  if (dist > 350) {
    const toOrigin = tmpV.copy(ORIGIN).sub(camera.position).normalize();
    camera.position.addScaledVector(toOrigin, 0.5);
    return { state: 'soft', dist: camera.position.distanceTo(ORIGIN) };
  }
  return { state: 'ok', dist };
}
