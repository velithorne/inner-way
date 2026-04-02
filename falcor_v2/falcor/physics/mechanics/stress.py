"""Stress estimators. Documented approximations."""

from __future__ import annotations

# Hoop stress for thin rotating disk: sigma = rho * r^2 * omega^2
# Assumption: thin disk, plane stress, uniform rotation
# Ref: standard rotating disk formula


def hoop_stress(rho: float, r: float, omega: float) -> float:
    """
    Hoop stress at radius r for rotating disk (Pa).
    sigma = rho * r^2 * omega^2
    Assumptions: thin disk, plane stress, uniform angular velocity.
    """
    return rho * (r**2) * (omega**2)
