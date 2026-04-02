"""Smoke tests for thermal solver."""

import pytest
from falcor.physics.thermal.heat_solver import LumpedThermalSolver, ThermalNode


def test_thermal_solver_step():
    nodes = [
        ThermalNode("a", mass=1.0, cp=1000.0, area=0.1, k=1.0),
        ThermalNode("b", mass=1.0, cp=1000.0, area=0.1, k=1.0),
    ]
    solver = LumpedThermalSolver(nodes, T_ambient=293.15)
    solver.step_temperature(0.1)
    temps = solver.get_temperatures()
    assert "a" in temps
    assert "b" in temps
    assert temps["a"] >= 0


def test_compute_heat_fluxes():
    nodes = [ThermalNode("x", mass=1.0, cp=1000.0, area=0.1, k=1.0)]
    solver = LumpedThermalSolver(nodes)
    fluxes = solver.compute_heat_fluxes()
    assert "x" in fluxes
