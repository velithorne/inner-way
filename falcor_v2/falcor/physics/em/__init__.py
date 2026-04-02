"""EM physics: quasistatic, FDTD placeholder, coils, eddy currents."""

from falcor.physics.em.coils import coil_B_axial, coil_B_magnitude
from falcor.physics.em.quasistatic_solver import QuasistaticSolver
from falcor.physics.em.energy_density import magnetic_energy_density

__all__ = ["coil_B_axial", "coil_B_magnitude", "QuasistaticSolver", "magnetic_energy_density"]
