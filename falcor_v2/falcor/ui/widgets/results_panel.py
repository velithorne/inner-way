"""Results table panel."""

from PySide6.QtWidgets import QWidget, QVBoxLayout, QTableWidget


def build_results_panel() -> tuple[QWidget, QTableWidget]:
    """Build results table."""
    w = QWidget()
    layout = QVBoxLayout(w)
    table = QTableWidget()
    table.setColumnCount(8)
    table.setHorizontalHeaderLabels(["Step", "Freq (Hz)", "Mag", "T_mean", "Slope", "B", "Vib", "Pass"])
    layout.addWidget(table)
    return w, table
