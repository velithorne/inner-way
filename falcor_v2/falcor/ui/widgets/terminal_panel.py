"""Terminal/log panel."""

from PySide6.QtWidgets import QWidget, QVBoxLayout, QTextEdit


def build_terminal_panel() -> QWidget:
    """Build terminal/log panel."""
    w = QWidget()
    layout = QVBoxLayout(w)
    text = QTextEdit()
    text.setReadOnly(True)
    text.setPlaceholderText("Log output will appear here.")
    layout.addWidget(text)
    return w
