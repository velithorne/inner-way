"""
Deterministic content-defined chunking using a simple rolling hash.

v0.1: windowed hash with split condition, configurable min/avg/max chunk size.
Chunk boundaries are stable and reproducible for identical input.
"""

from __future__ import annotations

import hashlib
from dataclasses import dataclass, field
from typing import Any


@dataclass
class ChunkResult:
    """Result of chunking: chunk ids, boundaries, debug info."""

    chunk_ids: list[str]  # ordered list of chunk content hashes (used as ids)
    boundaries: list[tuple[int, int]]  # (start, end) byte offsets per chunk
    chunk_sizes: list[int]  # size in bytes per chunk
    debug: dict[str, Any] = field(default_factory=dict)  # boundary hashes, etc.


_WINDOW = 64  # bytes in window for content-defined boundary


def _content_defined_split(
    data: bytes,
    min_chunk: int,
    max_chunk: int,
    avg_target: int,
) -> list[tuple[int, int]]:
    """
    Compute chunk boundaries: at each position >= min_chunk, hash the last W bytes.
    Split when (hash % divisor) == 0. divisor chosen so avg chunk ~ avg_target.
    Deterministic.
    """
    n = len(data)
    if n == 0:
        return []
    divisor = max(2, avg_target // 2)

    boundaries: list[tuple[int, int]] = []
    start = 0
    while start < n:
        end = min(start + max_chunk, n)
        found = -1
        for j in range(start + min_chunk, end):
            if j >= _WINDOW:
                window = data[j - _WINDOW : j]
            else:
                window = data[:j]
            h = int(hashlib.sha256(window).hexdigest()[:16], 16)
            if (h % divisor) == 0:
                found = j
                break
        if found > 0:
            boundaries.append((start, found))
            start = found
        else:
            boundaries.append((start, end))
            start = end

    return boundaries


def chunk_bytes(
    data: bytes,
    *,
    min_chunk: int = 256,
    max_chunk: int = 8192,
    avg_chunk: int = 1024,
) -> ChunkResult:
    """
    Chunk bytes deterministically. Returns chunk ids (content hashes), boundaries, sizes.
    """
    boundaries = _content_defined_split(data, min_chunk, max_chunk, avg_chunk)
    chunk_ids: list[str] = []
    chunk_sizes: list[int] = []
    for start, end in boundaries:
        blob = data[start:end]
        ch_id = hashlib.sha256(blob).hexdigest()[:32]
        chunk_ids.append(ch_id)
        chunk_sizes.append(len(blob))

    return ChunkResult(
        chunk_ids=chunk_ids,
        boundaries=boundaries,
        chunk_sizes=chunk_sizes,
        debug={"avg_target": avg_chunk, "min_chunk": min_chunk, "max_chunk": max_chunk},
    )
