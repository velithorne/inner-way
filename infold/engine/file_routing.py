"""
File routing policy for fold operators.

Classifies files as:
- structural_first: prefer structural folds (exact_rep, template, symbol, hierarchy, dependency)
- chunk_first: target for byte/chunk folding (mixed, binary-like, unstructured)
- passthrough_only: do not fold (too small, already compressed, etc.)
- low_value: skip for efficiency
"""

from __future__ import annotations

from pathlib import Path
from typing import Any

# Extensions that are typically structural (code, config)
STRUCTURAL_EXTENSIONS = {".py", ".js", ".ts", ".json", ".yaml", ".yml", ".md", ".txt"}

# Extensions that are often binary or opaque
BINARY_LIKE_EXTENSIONS = {".bin", ".dat", ".raw", ".blob", ".pack", ".idx"}

# Min size for chunk folding (avoid tiny files)
CHUNK_MIN_BYTES = 128

# Max size for chunk folding (avoid huge files in v0.1)
CHUNK_MAX_BYTES = 2 * 1024 * 1024  # 2MB


def route_file(
    path: Path | str,
    size_bytes: int,
    raw_hash: str | None,
    committed_paths: set[str],
    *,
    config: dict[str, Any] | None = None,
) -> str:
    """
    Route a file to a fold strategy. Returns:
    - structural_first: already claimed by structural fold, or is structural type
    - chunk_first: eligible for chunk folding (mixed, binary-like, or structural but not claimed)
    - passthrough_only: too small, excluded, or low value
    - low_value: skip for efficiency
    """
    path_str = str(path).replace("\\", "/")
    if path_str in committed_paths:
        return "structural_first"  # already claimed

    ext = Path(path).suffix.lower()
    cfg = config or {}
    chunk_cfg = cfg.get("thresholds", {}).get("byte_fold", {})
    min_bytes = chunk_cfg.get("min_file_bytes", CHUNK_MIN_BYTES)
    max_bytes = chunk_cfg.get("max_file_bytes", CHUNK_MAX_BYTES)

    if size_bytes < min_bytes:
        return "passthrough_only"
    if size_bytes > max_bytes:
        return "low_value"

    if ext in BINARY_LIKE_EXTENSIONS:
        return "chunk_first"
    if ext in STRUCTURAL_EXTENSIONS:
        # Structural but not committed - byte_fold can target partial repetition
        return "chunk_first"
    return "chunk_first"  # unknown/mixed


def get_chunk_eligible_paths(
    file_nodes: dict[Path, Any],
    committed_paths: set[str],
    config: dict[str, Any] | None = None,
) -> list[Path]:
    """
    Return paths eligible for chunk folding: not in committed_paths,
    within size bounds, with content. Byte Fold runs after other operators.
    """
    eligible: list[Path] = []
    for path, node in file_nodes.items():
        path_str = str(path).replace("\\", "/")
        if path_str in committed_paths:
            continue
        content = getattr(node, "raw_text", None) or getattr(node, "raw_bytes", None)
        if not content:
            continue
        if isinstance(content, str):
            size = len(content.encode("utf-8"))
        else:
            size = len(content)
        route = route_file(path, size, getattr(node, "raw_hash", None), committed_paths, config=config)
        if route == "passthrough_only" or route == "low_value":
            continue
        eligible.append(path)
    return eligible
