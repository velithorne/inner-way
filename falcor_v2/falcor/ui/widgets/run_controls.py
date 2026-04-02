"""Run controls widget. Built inline in MainWindow."""

from PySide6.QtWidgets import QGroupBox, QVBoxLayout, QPushButton, QLabel


def build_run_controls(run_callback, run_id_label: QLabel) -> QGroupBox:
    """Build run controls group."""
    g = QGroupBox("Run Controls")
    layout = QVBoxLayout(g)
    btn = QPushButton("Run")
    btn.clicked.connect(run_callback)
    layout.addWidget(btn)
    layout.addWidget(run_id_label)
    return g
