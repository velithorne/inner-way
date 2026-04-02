"""Device picker panel. Built inline in MainWindow."""

from PySide6.QtWidgets import QGroupBox, QVBoxLayout, QComboBox, QPushButton


def build_device_panel(combo: QComboBox, load_callback) -> QGroupBox:
    """Build device panel group."""
    g = QGroupBox("Device")
    layout = QVBoxLayout(g)
    layout.addWidget(combo)
    btn = QPushButton("Load...")
    btn.clicked.connect(load_callback)
    layout.addWidget(btn)
    return g
