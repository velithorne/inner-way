"""UDP adapter stub. Interface for future hardware."""

from __future__ import annotations


def open_udp(host: str, port: int):
    """Stub: would open UDP socket."""
    raise NotImplementedError("UDP adapter: hardware not connected (stub)")


def recv_udp(sock, size: int = 1024) -> bytes:
    """Stub: would receive UDP packet."""
    raise NotImplementedError("UDP adapter: hardware not connected (stub)")
