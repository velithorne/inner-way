export const atmosphereVertexShader = /* glsl */ `
varying vec3 vNormal;
varying vec3 vPosition;

void main() {
  vNormal = normalize(normalMatrix * normal);
  vPosition = (modelViewMatrix * vec4(position, 1.0)).xyz;
  gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
}
`;

export const atmosphereFragmentShader = /* glsl */ `
uniform vec3 uAtmosphereColor;
uniform float uAtmosphereStrength;
varying vec3 vNormal;
varying vec3 vPosition;

void main() {
  vec3 viewDir = normalize(-vPosition);
  float rim = 1.0 - max(dot(viewDir, vNormal), 0.0);

  // Sharper inner rim, softer outer halo
  float innerRim = pow(rim, 2.8) * 0.7;
  float outerHalo = pow(rim, 1.4) * 0.3;
  float combined = innerRim + outerHalo;

  float alpha = combined * uAtmosphereStrength;
  gl_FragColor = vec4(uAtmosphereColor, alpha);
}
`;

// Thin outer corona — barely visible, just depth
export const outerGlowVertexShader = /* glsl */ `
varying vec3 vNormal;
varying vec3 vPosition;

void main() {
  vNormal = normalize(normalMatrix * normal);
  vPosition = (modelViewMatrix * vec4(position, 1.0)).xyz;
  gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
}
`;

export const outerGlowFragmentShader = /* glsl */ `
varying vec3 vNormal;
varying vec3 vPosition;

void main() {
  vec3 viewDir = normalize(-vPosition);
  float rim = 1.0 - max(dot(viewDir, vNormal), 0.0);
  float halo = pow(rim, 1.2);
  // Deep indigo corona, very faint
  gl_FragColor = vec4(0.06, 0.12, 0.28, halo * 0.18);
}
`;
