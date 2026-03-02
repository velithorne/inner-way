"""Lumped thermal solver: explicit finite difference on component nodes."""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any

from falcor.physics.thermal.conduction import conductive_flux
from falcor.physics.thermal.convection import convective_flux
from falcor.physics.thermal.radiation import radiative_flux
from falcor.physics.thermal.boundary_conditions import (
    ambient_temperature,
    convective_coefficient_natural_air,
)
from falcor.physics.thermal.thermodynamics import heat_capacity, dT_from_energy


@dataclass
class ThermalNode:
    """Lumped thermal node."""

    id: str
    mass: float
    cp: float
    area: float
    k: float
    epsilon: float = 0.9
    neighbors: list[tuple[str, float, float]] = field(default_factory=list)  # (id, length, contact_area)


class LumpedThermalSolver:
    """Lumped-parameter thermal solver. Explicit Euler."""

    def __init__(
        self,
        nodes: list[ThermalNode],
        T_ambient: float | None = None,
        h_conv: float | None = None,
    ):
        self.nodes = {n.id: n for n in nodes}
        self.T = {n.id: T_ambient or ambient_temperature() for n in nodes}
        self.T_ambient = T_ambient or ambient_temperature()
        self.h_conv = h_conv or convective_coefficient_natural_air()
        self.Q_eddy: dict[str, float] = {n.id: 0.0 for n in nodes}

    def set_eddy_heating(self, component_id: str, power_w: float) -> None:
        """Set eddy current heating for a component."""
        if component_id in self.Q_eddy:
            self.Q_eddy[component_id] = power_w

    def compute_heat_fluxes(self) -> dict[str, float]:
        """Compute net heat flux into each node (W)."""
        fluxes: dict[str, float] = {}
        for nid, node in self.nodes.items():
            Q_net = 0.0
            # Conduction to neighbors
            for neighbor_id, length, contact_area in node.neighbors:
                if neighbor_id in self.T:
                    dT = self.T[neighbor_id] - self.T[nid]
                    Q_net += conductive_flux(node.k, contact_area, dT, length)
            # Convection to ambient
            Q_conv = convective_flux(self.h_conv, node.area, self.T[nid], self.T_ambient)
            Q_net -= Q_conv
            # Radiation to ambient
            Q_rad = radiative_flux(node.epsilon, node.area, self.T[nid], self.T_ambient)
            Q_net -= Q_rad
            # Eddy heating
            Q_net += self.Q_eddy.get(nid, 0.0)
            fluxes[nid] = Q_net
        return fluxes

    def compute_net_delta(self, component_id: str, T_component: float, T_amb: float) -> float:
        """Compute dT/dt for a component (K/s). Approximate."""
        if component_id not in self.nodes:
            return 0.0
        node = self.nodes[component_id]
        fluxes = self.compute_heat_fluxes()
        Q = fluxes.get(component_id, 0.0)
        C = heat_capacity(node.mass, node.cp)
        return dT_from_energy(Q, C) if C > 0 else 0.0

    def step_temperature(self, dt: float) -> None:
        """Advance temperatures by dt (explicit Euler)."""
        fluxes = self.compute_heat_fluxes()
        for nid, node in self.nodes.items():
            Q = fluxes.get(nid, 0.0)
            C = heat_capacity(node.mass, node.cp)
            dT = dT_from_energy(Q * dt, C) if C > 0 else 0.0
            self.T[nid] += dT
            if self.T[nid] < 0:
                self.T[nid] = 0  # Clamp impossible temps

    def get_temperatures(self) -> dict[str, float]:
        """Return current temperatures (K)."""
        return self.T.copy()
