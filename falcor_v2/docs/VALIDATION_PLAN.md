# FALCOR v2 Validation Plan

Sanity checks for each module.

## Core

- **Units**: Roundtrip Celsius ↔ Kelvin, RPM ↔ rad/s.
- **Manifest**: Hash deterministic; changes with config.
- **RNG**: Same seed → same sequence; different run_id → different sequence.

## Physics

### Thermal

- Step: temperatures remain non-negative.
- Fluxes: conduction/convection/radiation have correct sign.
- Heat capacity: C = m × cp.

### EM

- Coil B: positive on axis, decreases with z.
- Energy density: u_B = B²/(2μ₀).

### Mechanics

- Hoop stress: σ ∝ ρ, r², ω².

## Measurement

- **Artifacts**: Inject + detect; deterministic with seed.
- **Blind trials**: Save/load sealed truth; conditions reproducible.

## Energy Audit

- Pin = motor + coil.
- Ploss = Pin - Pmech - Pthermal - PEM.
- Negative Ploss → flag.

## Integration

- Run CLI: produces run_id, manifest, parquet, audit.
- Export: CSV readable.
- GUI: loads device, runs demo.

## Run Tests

```bash
pytest tests/ -v
```
