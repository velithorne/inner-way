# FALCOR v2 Science Model

Equations, assumptions, and limitations. No hype.

## Thermal

### Lumped model

Components are lumped nodes. Heat equation:

\[
C_i \frac{dT_i}{dt} = Q_{cond} + Q_{conv} + Q_{rad} + Q_{eddy}
\]

- **Conduction**: \(Q = k A \Delta T / L\)
- **Convection**: \(Q = h A (T_s - T_a)\)
- **Radiation**: \(Q = \epsilon \sigma A (T_s^4 - T_a^4)\)
- **Eddy heating**: \(P \approx k_{geom} \sigma (dB/dt)^2 V\) (approximation)

**Assumptions**: Lumped nodes, uniform properties, constant h.

**Limitations**: No spatial gradients within components; coarse geometry.

---

## Mechanics

### Rigid body rotation

- \(\omega\) (rad/s) ↔ RPM: \(\omega = 2\pi \cdot \text{RPM} / 60\)

### Hoop stress (rotating disk)

\[
\sigma = \rho r^2 \omega^2
\]

**Assumptions**: Thin disk, plane stress, uniform rotation.

**Limitations**: Does not account for radial variation, plasticity.

### Vibration

- Synthetic model: base + harmonics.
- FFT features: optional.

---

## EM

### Quasi-static coil (default)

On-axis B field:

\[
B = \frac{\mu_0 N I R^2}{2(R^2 + z^2)^{3/2}}
\]

**Assumptions**: Axisymmetric, single coil, superposition.

**Limitations**: Off-axis is approximation; no eddy reaction.

### Eddy current heating

\[
P_{eddy} \approx k_{geom} \sigma (dB/dt)^2 V
\]

**Label**: APPROXIMATION. Geometry factor nominal.

### Energy density

\[
u_B = \frac{B^2}{2\mu_0}
\]

---

## Coupling

### EM → Thermal

- Eddy heating contribution to thermal nodes.

### Mechanics → EM

- Induced EMF proxy: \( \mathcal{E} \approx v B L \) (approximation).

---

## FDTD

- Placeholder only. Minimal grid stepping.
- Smoke test level in v1.

---

## Falsification

- **Permutation test**: Compare ON vs OFF conditions.
- **t-test**: Welch t-test for effect size.
- **Evidence grade**: A (survives controls), B (signal exists), C (likely artifact).
