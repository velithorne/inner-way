"""Material property definitions. All values in SI units."""

from __future__ import annotations

# Default materials: density (kg/m³), cp (J/(kg·K)), k (W/(m·K)), resistivity (Ω·m), susceptibility
MATERIALS: dict[str, dict[str, float]] = {
    "aluminum": {
        "density": 2700.0,
        "cp": 900.0,
        "k": 237.0,
        "resistivity": 2.82e-8,
        "susceptibility": 2.2e-5,
    },
    "steel": {
        "density": 7850.0,
        "cp": 502.0,
        "k": 43.0,
        "resistivity": 1.7e-7,
        "susceptibility": 1000.0,  # ferromagnetic
    },
    "copper": {
        "density": 8960.0,
        "cp": 385.0,
        "k": 401.0,
        "resistivity": 1.68e-8,
        "susceptibility": -9.6e-6,
    },
    "plastic": {
        "density": 1200.0,
        "cp": 1500.0,
        "k": 0.2,
        "resistivity": 1e12,
        "susceptibility": -1e-5,
    },
    "air": {
        "density": 1.2,
        "cp": 1005.0,
        "k": 0.026,
        "resistivity": 1e16,
        "susceptibility": 3.6e-7,
    },
}


def get_material(name: str) -> dict[str, float]:
    """Get material properties by name."""
    if name not in MATERIALS:
        raise KeyError(f"Unknown material: {name}")
    return MATERIALS[name].copy()
