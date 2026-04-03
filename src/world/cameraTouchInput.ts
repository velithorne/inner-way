/** Written from gestures each frame; consumed once per GL frame in WorldEngine. */
let pendingPanX = 0;
let pendingPanY = 0;
let pendingZoom = 1;

export function addCameraPan(dx: number, dy: number): void {
  pendingPanX += dx;
  pendingPanY += dy;
}

export function multiplyCameraZoom(factor: number): void {
  pendingZoom *= factor;
}

export function consumeTouchCameraInput(): { panX: number; panY: number; zoom: number } {
  const out = { panX: pendingPanX, panY: pendingPanY, zoom: pendingZoom };
  pendingPanX = 0;
  pendingPanY = 0;
  pendingZoom = 1;
  return out;
}
