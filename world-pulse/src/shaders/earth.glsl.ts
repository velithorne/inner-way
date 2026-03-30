export const earthVertexShader = /* glsl */ `
varying vec2 vUv;
varying vec3 vNormal;
varying vec3 vPosition;

void main() {
  vUv = uv;
  vNormal = normalize(normalMatrix * normal);
  vPosition = (modelViewMatrix * vec4(position, 1.0)).xyz;
  gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
}
`;

export const earthFragmentShader = /* glsl */ `
uniform float uTime;
uniform float uGlowAccumulation;

varying vec2 vUv;
varying vec3 vNormal;
varying vec3 vPosition;

// Simplex-like noise for procedural surface variation
vec3 mod289(vec3 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
vec2 mod289(vec2 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
vec3 permute(vec3 x) { return mod289(((x * 34.0) + 1.0) * x); }

float snoise(vec2 v) {
  const vec4 C = vec4(0.211324865405187, 0.366025403784439,
                     -0.577350269189626, 0.024390243902439);
  vec2 i  = floor(v + dot(v, C.yy));
  vec2 x0 = v - i + dot(i, C.xx);
  vec2 i1 = (x0.x > x0.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);
  vec4 x12 = x0.xyxy + C.xxzz;
  x12.xy -= i1;
  i = mod289(i);
  vec3 p = permute(permute(i.y + vec3(0.0, i1.y, 1.0)) + i.x + vec3(0.0, i1.x, 1.0));
  vec3 m = max(0.5 - vec3(dot(x0, x0), dot(x12.xy, x12.xy), dot(x12.zw, x12.zw)), 0.0);
  m = m * m;
  m = m * m;
  vec3 x = 2.0 * fract(p * C.www) - 1.0;
  vec3 h = abs(x) - 0.5;
  vec3 ox = floor(x + 0.5);
  vec3 a0 = x - ox;
  m *= 1.79284291400159 - 0.85373472095314 * (a0 * a0 + h * h);
  vec3 g;
  g.x  = a0.x * x0.x + h.x * x0.y;
  g.yz = a0.yz * x12.xz + h.yz * x12.yw;
  return 130.0 * dot(m, g);
}

void main() {
  // Base dark earth surface
  vec3 baseColor = vec3(0.04, 0.06, 0.09);
  
  // Subtle surface texture using noise
  float n = snoise(vUv * 8.0) * 0.5 + 0.5;
  float n2 = snoise(vUv * 20.0 + vec2(100.0)) * 0.5 + 0.5;
  
  // Continental regions (brighter noise patches)
  float land = smoothstep(0.42, 0.62, n) * 0.8 + smoothstep(0.35, 0.55, n2) * 0.3;
  land = clamp(land, 0.0, 1.0);
  
  vec3 landColor = vec3(0.07, 0.11, 0.16);
  vec3 oceanColor = vec3(0.02, 0.04, 0.07);
  vec3 surfaceColor = mix(oceanColor, landColor, land);
  
  // Directional lighting (sun from the right)
  vec3 lightDir = normalize(vec3(2.0, 1.0, 1.5));
  float diff = max(dot(vNormal, lightDir), 0.0);
  float ambient = 0.05;
  float lighting = ambient + diff * 0.35;
  
  // Edge glow — coastline-like highlights
  vec3 viewDir = normalize(-vPosition);
  float edgeFactor = 1.0 - abs(dot(viewDir, vNormal));
  float edgeGlow = pow(edgeFactor, 6.0) * 0.4;
  
  // Pulse accumulation glow
  vec3 glowColor = vec3(0.1, 0.3, 0.5);
  float accumulatedGlow = uGlowAccumulation * 0.15;
  
  vec3 finalColor = surfaceColor * lighting;
  finalColor += vec3(edgeGlow * 0.08, edgeGlow * 0.16, edgeGlow * 0.24);
  finalColor += glowColor * accumulatedGlow;
  
  gl_FragColor = vec4(finalColor, 1.0);
}
`;
