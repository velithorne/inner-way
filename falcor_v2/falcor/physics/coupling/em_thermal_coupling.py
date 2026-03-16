"""EM -> thermal coupling: eddy heating."""

from __future__ import annotations

from falcor.physics.em.eddy_currents import eddy_heating_approximation


class EMThermalCoupling:
    """Couple EM (dB/dt) to thermal (eddy heating)."""

    def __init__(self, component_volumes: dict[str, float], conductivities: dict[str, float]):
        self.volumes = component_volumes
        self.conductivities = conductivities

    def compute_eddy_power(self, component_id: str, dB_dt: float) -> float:
        """Eddy heating for component (W). Approximation."""
        sigma = self.conductivities.get(component_id, 0.0)
        V = self.volumes.get(component_id, 0.0)
        return eddy_heating_approximation(sigma, dB_dt, V)
