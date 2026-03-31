export const earthVertexShader = /* glsl */ `
varying vec2 vUv;
varying vec3 vNormal;
varying vec3 vPosition;
varying vec3 vWorldNormal;

void main() {
  vUv = uv;
  vNormal = normalize(normalMatrix * normal);
  vWorldNormal = normalize((modelMatrix * vec4(normal, 0.0)).xyz);
  vPosition = (modelViewMatrix * vec4(position, 1.0)).xyz;
  gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
}
`;

export const earthFragmentShader = /* glsl */ `
uniform sampler2D uDayTexture;
uniform sampler2D uNightTexture;
uniform sampler2D uSpecularMap;
uniform float uGlowAccumulation;
uniform float uTime;

varying vec2 vUv;
varying vec3 vNormal;
varying vec3 vPosition;
varying vec3 vWorldNormal;

void main() {
  // Sample textures
  vec4 dayColor   = texture2D(uDayTexture,  vUv);
  vec4 nightColor = texture2D(uNightTexture, vUv);
  vec4 specular   = texture2D(uSpecularMap,  vUv);

  // ── Cinematic grading of the day texture ──────────────────
  // The raw texture is too pale/bright; we push it toward dark navy slate.
  // Desaturate slightly, then tint toward deep space palette.
  float lum = dot(dayColor.rgb, vec3(0.299, 0.587, 0.114));

  // Land = darker slate-charcoal, Ocean = deep blue-black
  // The specular map is bright where there is ocean water (high reflectivity)
  float oceanMask  = specular.r;                      // 1 = ocean, 0 = land
  float landMask   = 1.0 - oceanMask;

  vec3 landBase    = vec3(0.13, 0.155, 0.19);         // charcoal slate-blue
  vec3 oceanBase   = vec3(0.025, 0.05, 0.10);         // deep navy-black

  // Use the luminance from the day texture to add tonal variation within continents
  vec3 landColor   = mix(landBase * 0.55, landBase * 1.35, lum);
  vec3 oceanColor  = mix(oceanBase * 0.4, oceanBase * 1.3, lum * 0.4);

  vec3 surfaceColor = mix(oceanColor, landColor, landMask);

  // ── Lighting ─────────────────────────────────────────────
  // Sun from upper right, slightly warm
  vec3 sunDir  = normalize(vec3(3.5, 1.2, 2.5));
  float NdotL  = dot(vNormal, sunDir);

  // Day/night blend: smooth terminator
  float dayBlend  = smoothstep(-0.25, 0.35, NdotL);

  // Night side: city lights from the night texture, dimmed
  vec3 nightGlow  = nightColor.rgb * 0.55 * (1.0 - dayBlend);

  // Day illumination: diffuse + subtle specular sheen on oceans
  float diffuse   = max(NdotL, 0.0);
  float specSheen = pow(max(dot(reflect(-sunDir, vNormal), normalize(-vPosition)), 0.0), 28.0)
                    * oceanMask * 0.25;

  float ambient   = 0.04;
  float lighting  = ambient + diffuse * 0.72 + specSheen;

  vec3 litDay     = surfaceColor * lighting;

  // ── Blend day and night ───────────────────────────────────
  vec3 color = mix(nightGlow, litDay, dayBlend);

  // ── Pulse glow accumulation ───────────────────────────────
  vec3 pulseGlow  = vec3(0.08, 0.22, 0.42) * uGlowAccumulation * 0.18;
  color += pulseGlow;

  // ── Very subtle coastline shimmer ─────────────────────────
  // Sample neighbor UV to detect edges between land/ocean
  float dx = 0.0015;
  float neighborSpec = texture2D(uSpecularMap, vUv + vec2(dx, 0.0)).r
                     + texture2D(uSpecularMap, vUv + vec2(-dx, 0.0)).r
                     + texture2D(uSpecularMap, vUv + vec2(0.0, dx)).r
                     + texture2D(uSpecularMap, vUv + vec2(0.0, -dx)).r;
  float coastEdge = abs(oceanMask * 4.0 - neighborSpec) * 0.25;
  color += vec3(0.15, 0.35, 0.55) * coastEdge * 0.35 * dayBlend;

  gl_FragColor = vec4(color, 1.0);
}
`;
