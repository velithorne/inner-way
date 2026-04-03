import * as THREE from 'three';

const DEAD = 0.02;
const DAMP = 0.92;
const MAX_SPD = 40;
const GAIN = 0.8;

/**
 * Gyroscope-driven flight. Values from expo-sensors Gyroscope (rad/s).
 */
export class FlightController {
  readonly velocity = new THREE.Vector3();
  private orbital = false;

  setOrbital(active: boolean): void {
    this.orbital = active;
  }

  isOrbital(): boolean {
    return this.orbital;
  }

  update(gyro: { x: number; y: number; z: number } | null, dt: number): void {
    if (this.orbital) {
      this.velocity.multiplyScalar(0.85);
      return;
    }
    if (!gyro) return;
    const gx = Math.abs(gyro.x) < DEAD ? 0 : gyro.x;
    const gy = Math.abs(gyro.y) < DEAD ? 0 : gyro.y;
    const gz = Math.abs(gyro.z) < DEAD ? 0 : gyro.z;
    this.velocity.x += gx * GAIN * dt * 60;
    this.velocity.y += gz * GAIN * 0.5 * dt * 60;
    this.velocity.z += gy * GAIN * dt * 60;
    this.velocity.x = THREE.MathUtils.clamp(this.velocity.x, -MAX_SPD, MAX_SPD);
    this.velocity.y = THREE.MathUtils.clamp(this.velocity.y, -MAX_SPD, MAX_SPD);
    this.velocity.z = THREE.MathUtils.clamp(this.velocity.z, -MAX_SPD, MAX_SPD);
    this.velocity.multiplyScalar(Math.pow(DAMP, dt * 60));
  }

  applyToCamera(camera: THREE.PerspectiveCamera, dt: number, orbitalTarget: THREE.Vector3): void {
    if (this.orbital) {
      camera.position.lerp(orbitalTarget, 0.06);
      camera.lookAt(0, 0, 0);
      return;
    }
    camera.position.addScaledVector(this.velocity, dt);
    camera.position.y = THREE.MathUtils.clamp(camera.position.y, 10, 450);
    const dist = camera.position.length();
    if (dist > 520) camera.position.setLength(520);
    camera.lookAt(0, 0, 0);
  }
}
