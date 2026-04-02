export const pulseVertexShader = /* glsl */ `
varying vec2 vUv;

void main() {
  vUv = uv;
  gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
}
`;

export const pulseFragmentShader = /* glsl */ `
uniform vec3 uColor;
uniform float uProgress;
uniform float uOpacity;
uniform float uGlowStrength;

varying vec2 vUv;

void main() {
  // Distance from center
  vec2 center = vec2(0.5);
  float dist = distance(vUv, center) * 2.0; // 0 at center, 1 at edge
  
  // Ring shape: thin band at current radius
  float ringRadius = uProgress;
  float ringWidth = 0.06 + (1.0 - uProgress) * 0.04;
  float ring = smoothstep(ringRadius - ringWidth, ringRadius, dist) 
             * smoothstep(ringRadius + ringWidth * 0.5, ringRadius, dist);
  
  // Inner fill that fades with progress
  float innerFill = smoothstep(ringRadius * 0.95, 0.0, dist) * (1.0 - uProgress) * 0.3;
  
  // Glow bloom around the ring
  float glow = exp(-pow((dist - ringRadius) * 8.0, 2.0)) * uGlowStrength * 0.5;
  
  float intensity = ring + innerFill + glow;
  float finalAlpha = intensity * uOpacity;
  
  gl_FragColor = vec4(uColor, finalAlpha);
}
`;
