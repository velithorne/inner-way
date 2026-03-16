"""Serial adapter stub. Interface for future hardware."""

from __future__ import annotations


def open_serial(port: str, baud: int = 9600):
    """Stub: would open serial port."""
    raise NotImplementedError("Serial adapter: hardware not connected (stub)")


def read_serial_line(conn) -> str:
    """Stub: would read line."""
    raise NotImplementedError("Serial adapter: hardware not connected (stub)")
