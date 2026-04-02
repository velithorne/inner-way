"""EM energy density. SI units."""

from __future__ import annotations

from falcor.core.units import MU_0, EPS_0


def magnetic_energy_density(B: float) -> float:
    """u_B = B^2 / (2*mu0) (J/m^3)."""
    return (B**2) / (2 * MU_0)


def electric_energy_density(E: float) -> float:
    """u_E = eps0 * E^2 / 2 (J/m^3)."""
    return 0.5 * EPS_0 * (E**2)


def total_em_energy_density(E: float, B: float) -> float:
    """u = 1/2 (eps0 E^2 + B^2/mu0). If only B, E=0."""
    return electric_energy_density(E) + magnetic_energy_density(B)
