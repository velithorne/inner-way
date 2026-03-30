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
  // Rim lighting effect — brighter at grazing angles
  vec3 viewDir = normalize(-vPosition);
  float rim = 1.0 - max(dot(viewDir, vNormal), 0.0);
  rim = pow(rim, 3.5);
  
  float alpha = rim * uAtmosphereStrength;
  gl_FragColor = vec4(uAtmosphereColor, alpha);
}
`;
