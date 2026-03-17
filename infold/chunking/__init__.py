"""
Infold chunking: deterministic content-defined chunking for Byte Fold.

Rolling-hash based. Configurable min/avg/max chunk sizes.
"""

from infold.chunking.roller import (
    chunk_bytes,
    ChunkResult,
)

__all__ = [
    "chunk_bytes",
    "ChunkResult",
]
