"""Smoke tests for EM solver."""

import pytest
from falcor.physics.em.coils import coil_B_axial, coil_B_magnitude
from falcor.physics.em.quasistatic_solver import QuasistaticSolver, CoilSpec
from falcor.physics.em.energy_density import magnetic_energy_density


def test_coil_B_axial():
    B = coil_B_axial(z=0.0, R=0.1, I=1.0, N=100)
    assert B > 0


def test_quasistatic_solver():
    coils = [CoilSpec(radius_m=0.08, turns=100, height_m=0.02)]
    solver = QuasistaticSolver(coils)
    B = solver.B_axial(0.1, I_per_coil=1.0)
    assert B >= 0


def test_magnetic_energy_density():
    u = magnetic_energy_density(0.001)
    assert u > 0
