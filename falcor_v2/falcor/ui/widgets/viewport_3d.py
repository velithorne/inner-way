"""3D viewport using PyVistaQt."""

from __future__ import annotations

from pathlib import Path

from PySide6.QtWidgets import QWidget, QVBoxLayout
from PySide6.QtCore import Qt

from falcor.core.logging import get_logger

logger = get_logger("viewport_3d")


class Viewport3D(QWidget):
    """3D viewport for virtual assembly rendering."""

    def __init__(self, parent=None):
        super().__init__(parent)
        self.setMinimumSize(400, 300)
        layout = QVBoxLayout(self)
        layout.setContentsMargins(0, 0, 0, 0)
        try:
            from pyvistaqt import QtInteractor
            self._plotter = QtInteractor(self)
            layout.addWidget(self._plotter.interactor)
            self._plotter.set_background("black")
            self._plotter.add_axes()
        except ImportError as e:
            logger.warning("PyVistaQt not available: %s", e)
            from PySide6.QtWidgets import QLabel
            self._placeholder = QLabel("3D viewport unavailable (install pyvistaqt)")
            layout.addWidget(self._placeholder)
            self._plotter = None

    def load_assembly(self, path: Path | str) -> None:
        """Load and render virtual assembly from JSON."""
        if self._plotter is None:
            return
        try:
            import json
            from pyvista import Cylinder, Box, Sphere
            with open(path, encoding="utf-8") as f:
                data = json.load(f)
            self._plotter.clear()
            self._plotter.add_axes()
            for comp in data.get("components", []):
                shape = comp.get("shape", "box")
                t = comp.get("transform", {})
                x, y, z = t.get("x", 0), t.get("y", 0), t.get("z", 0)
                params = comp.get("params", {})
                if shape == "cylinder":
                    r = params.get("radius_m", 0.02)
                    h = params.get("height_m", 0.05)
                    mesh = Cylinder(radius=r, height=h, center=(x, y, z))
                elif shape == "disk":
                    r = params.get("radius_m", 0.1)
                    th = params.get("thickness_m", 0.01)
                    mesh = Cylinder(radius=r, height=th, center=(x, y, z))
                elif shape == "coil":
                    r = params.get("radius_m", 0.08)
                    mesh = Cylinder(radius=r, height=0.02, center=(x, y, z))
                elif shape == "sphere":
                    r = params.get("radius_m", 0.05)
                    mesh = Sphere(radius=r, center=(x, y, z))
                else:
                    w = params.get("width_m", 0.1)
                    d = params.get("depth_m", 0.1)
                    h = params.get("height_m", 0.02)
                    mesh = Box(bounds=(x - w/2, x + w/2, y - d/2, y + d/2, z - h/2, z + h/2))
                self._plotter.add_mesh(mesh, color="tan", show_edges=True)
            self._plotter.reset_camera()
        except Exception as e:
            logger.exception("Failed to render assembly: %s", e)
