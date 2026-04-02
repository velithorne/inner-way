"""Eddy current heating approximation. Clearly labeled as approximation."""

from __future__ import annotations

# Eddy heating scales with: sigma * (dB/dt)^2 * volume
# Simplified: P_eddy ~ k_eddy * sigma * (dB_dt)^2 * V
# k_eddy is geometry-dependent; we use a nominal factor and log as approximation


def eddy_heating_approximation(
    sigma: float,
    dB_dt: float,
    volume: float,
    k_geom: float = 0.1,
) -> float:
    """
    Eddy current heating (W). APPROXIMATION.
    P ~ k_geom * sigma * (dB/dt)^2 * V
    k_geom is a nominal geometry factor; real value depends on shape.
    """
    return k_geom * sigma * (dB_dt**2) * volume
