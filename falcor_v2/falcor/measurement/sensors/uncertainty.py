"""Uncertainty propagation. Linear approximation."""

from __future__ import annotations

import numpy as np


def propagate_linear(coeffs: list[float], sigmas: list[float]) -> float:
    """Propagate uncertainty: sigma_f^2 = sum (df/dxi * sigma_xi)^2. For linear f = sum ci*xi, sigma = sqrt(sum (ci*si)^2)."""
    return float(np.sqrt(sum((c * s) ** 2 for c, s in zip(coeffs, sigmas))))


def product_uncertainty(a: float, sigma_a: float, b: float, sigma_b: float) -> float:
    """Uncertainty of a*b: sigma_ab ≈ |ab| * sqrt((sigma_a/a)^2 + (sigma_b/b)^2)."""
    if a == 0 and b == 0:
        return 0.0
    if a == 0:
        return abs(b) * sigma_a
    if b == 0:
        return abs(a) * sigma_b
    return abs(a * b) * np.sqrt((sigma_a / a) ** 2 + (sigma_b / b) ** 2)
