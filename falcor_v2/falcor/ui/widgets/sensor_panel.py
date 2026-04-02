"""Sensor status panel."""

from PySide6.QtWidgets import QGroupBox, QVBoxLayout, QLabel


def build_sensor_panel(mode: str = "SIM") -> QGroupBox:
    """Build sensor status group."""
    g = QGroupBox("Sensor Status")
    layout = QVBoxLayout(g)
    layout.addWidget(QLabel(f"Mode: {mode} — sensors simulated" if mode == "SIM" else f"Mode: {mode} — field sensors"))
    return g
