"""Plot panel."""

from PySide6.QtWidgets import QWidget, QVBoxLayout, QLabel, QPushButton


def build_plot_panel(export_callback) -> QWidget:
    """Build plot panel."""
    w = QWidget()
    layout = QVBoxLayout(w)
    layout.addWidget(QLabel("Select a run and metric to plot. Export PNG via button."))
    btn = QPushButton("Export PNG")
    btn.clicked.connect(export_callback)
    layout.addWidget(btn)
    return w
