"""
File routing policy for fold operators.

v2: Stronger deterministic routing with diagnostics.

Classifies files as:
- structural_first: prefer structural folds (already claimed or clearly structured)
- chunk_first: target for byte/chunk folding (mixed, binary-like, unstructured)
- passthrough_only: do not fold (too small, excluded)
- low_value: skip for efficiency (too large, already compressed, etc.)
"""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Any

# Extensions that are typically structural (code, config)
STRUCTURAL_EXTENSIONS = {".py", ".js", ".ts", ".json", ".yaml", ".yml", ".md", ".txt"}

# Extensions that are often binary or opaque
BINARY_LIKE_EXTENSIONS = {".bin", ".dat", ".raw", ".blob", ".pack", ".idx"}

# Extensions often already compressed
COMPRESSED_LIKE_EXTENSIONS = {".zip", ".gz", ".tgz", ".bz2", ".xz", ".7z", ".rar"}

# Min size for chunk folding (avoid tiny files)
CHUNK_MIN_BYTES = 128

# Max size for chunk folding (avoid huge files)
CHUNK_MAX_BYTES = 2 * 1024 * 1024  # 2MB

# Parser confidence below which we treat as low-structure (chunk-first)
LOW_STRUCTURE_CONFIDENCE = 0.5

# Min lines for "clearly structured" (structural-first when high confidence)
MIN_STRUCTURED_LINES = 3


@dataclass
class RouteResult:
    """Result of file routing with diagnostic reason."""

    route: str  # structural_first | chunk_first | passthrough_only | low_value
    reason: str  # deterministic explanation


def route_file(
    path: Path | str,
    size_bytes: int,
    raw_hash: str | None,
    committed_paths: set[str],
    *,
    config: dict[str, Any] | None = None,
) -> str:
    """
    Route a file to a fold strategy. Returns route string.
    Use route_file_v2 for diagnostics.
    """
    return route_file_v2(
        path, size_bytes, raw_hash, committed_paths, config=config
    ).route


def route_file_v2(
    path: Path | str,
    size_bytes: int,
    raw_hash: str | None,
    committed_paths: set[str],
    *,
    config: dict[str, Any] | None = None,
    parser_confidence: float | None = None,
    line_count: int | None = None,
) -> RouteResult:
    """
    Route a file with deterministic diagnostics.

    Uses: extension, size, parser_confidence, line_count, committed_paths.
    """
    path_str = str(path).replace("\\", "/")
    ext = Path(path).suffix.lower()
    cfg = config or {}
    chunk_cfg = cfg.get("thresholds", {}).get("byte_fold", {})
    min_bytes = chunk_cfg.get("min_file_bytes", CHUNK_MIN_BYTES)
    max_bytes = chunk_cfg.get("max_file_bytes", CHUNK_MAX_BYTES)
    low_conf = chunk_cfg.get("low_structure_confidence", LOW_STRUCTURE_CONFIDENCE)

    if path_str in committed_paths:
        return RouteResult("structural_first", "committed_by_prior_fold")

    if ext in COMPRESSED_LIKE_EXTENSIONS:
        return RouteResult("low_value", "extension_already_compressed")

    if size_bytes < min_bytes:
        return RouteResult("passthrough_only", f"size_{size_bytes}_below_min_{min_bytes}")

    if size_bytes > max_bytes:
        return RouteResult("low_value", f"size_{size_bytes}_above_max_{max_bytes}")

    # Binary-like: strong chunk-first signal
    if ext in BINARY_LIKE_EXTENSIONS:
        return RouteResult("chunk_first", "extension_binary_like")

    # Structural extensions: use parser confidence and line count
    if ext in STRUCTURAL_EXTENSIONS:
        conf = parser_confidence if parser_confidence is not None else 1.0
        lines = line_count if line_count is not None else 0

        # Low parser confidence -> likely mixed/unstructured
        if conf < low_conf:
            return RouteResult(
                "chunk_first",
                f"parser_confidence_{conf:.2f}_below_{low_conf}",
            )

        # High confidence, many lines -> structural-first (let template/symbol try first)
        if conf >= 0.8 and lines >= MIN_STRUCTURED_LINES:
            return RouteResult(
                "structural_first",
                f"parser_confidence_{conf:.2f}_lines_{lines}_structured",
            )

        # Few lines -> chunk-first (template needs min_lines)
        if lines < MIN_STRUCTURED_LINES and lines > 0:
            return RouteResult("chunk_first", f"lines_{lines}_below_structured_threshold")

        # Default for structural ext: chunk_first (Byte Fold runs after structural)
        return RouteResult("chunk_first", "structural_ext_not_committed")

    # Unknown extension: chunk-first
    return RouteResult("chunk_first", "unknown_extension")


def get_chunk_eligible_paths(
    file_nodes: dict[Path, Any],
    committed_paths: set[str],
    config: dict[str, Any] | None = None,
) -> list[Path]:
    """
    Return paths eligible for chunk folding: not in committed_paths,
    within size bounds, routed chunk_first. Byte Fold runs after other operators.
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
        if route in ("passthrough_only", "low_value"):
            continue
        eligible.append(path)
    return eligible


def get_chunk_eligible_paths_with_diagnostics(
    file_nodes: dict[Path, Any],
    committed_paths: set[str],
    config: dict[str, Any] | None = None,
) -> tuple[list[Path], dict[str, RouteResult]]:
    """
    Return (eligible paths, path -> RouteResult for all files considered).
    Enables reporting of routing diagnostics.
    """
    path_to_route: dict[str, RouteResult] = {}
    eligible: list[Path] = []

    for path, node in file_nodes.items():
        path_str = str(path).replace("\\", "/")
        if path_str in committed_paths:
            path_to_route[path_str] = RouteResult("structural_first", "committed_by_prior_fold")
            continue
        content = getattr(node, "raw_text", None) or getattr(node, "raw_bytes", None)
        if not content:
            path_to_route[path_str] = RouteResult("passthrough_only", "no_content")
            continue
        if isinstance(content, str):
            size = len(content.encode("utf-8"))
            lines = len(content.splitlines()) if content else 0
        else:
            size = len(content)
            lines = 0

        conf = getattr(node, "parser_confidence", None)
        result = route_file_v2(
            path,
            size,
            getattr(node, "raw_hash", None),
            committed_paths,
            config=config,
            parser_confidence=conf,
            line_count=lines,
        )
        path_to_route[path_str] = result

        # chunk_first or structural_first (not committed) -> eligible for Byte Fold
        if result.route in ("chunk_first", "structural_first"):
            eligible.append(path)

    return eligible, path_to_route
