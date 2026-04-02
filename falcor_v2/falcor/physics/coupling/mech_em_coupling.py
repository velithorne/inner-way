"""Mechanics -> EM: v x B induced EMF proxy. Approximation."""

from __future__ import annotations


def induced_emf_proxy(v: float, B: float, length: float) -> float:
    """
    EMF ~ v * B * L (V). Approximation for conductor moving in B field.
    Real geometry affects factor; this is order-of-magnitude.
    """
    return v * B * length
