"""Structured logging for FALCOR."""

from __future__ import annotations

import logging
import sys
from pathlib import Path
from typing import Any


def setup_logging(
    level: int = logging.INFO,
    log_file: Path | None = None,
) -> logging.Logger:
    """Configure and return root logger."""
    root = logging.getLogger("falcor")
    root.setLevel(level)

    if root.handlers:
        return root

    fmt = logging.Formatter(
        "%(asctime)s | %(levelname)-7s | %(name)s | %(message)s",
        datefmt="%Y-%m-%d %H:%M:%S",
    )

    h = logging.StreamHandler(sys.stdout)
    h.setFormatter(fmt)
    root.addHandler(h)

    if log_file:
        fh = logging.FileHandler(log_file, encoding="utf-8")
        fh.setFormatter(fmt)
        root.addHandler(fh)

    return root


def get_logger(name: str) -> logging.Logger:
    """Get a child logger."""
    return logging.getLogger(f"falcor.{name}")
