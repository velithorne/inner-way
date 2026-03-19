"""
Phase 16A: Mutation Chain v0.1 — base + compact mutations for similar files.

Conservative, deterministic representation for closely related text files.
- base member: one full file
- mutated members: compact line-based diffs from base
- net-positive only
"""

from __future__ import annotations

from pathlib import Path
from typing import Any


def _pick_base(paths: list[Path], contents: dict[Path, str]) -> Path:
    """Deterministic base selection: smallest by bytes, then first by path."""
    def key(p: Path) -> tuple[int, str]:
        size = len(contents.get(p, "").encode("utf-8"))
        return (size, str(p))
    return min(paths, key=key)


def _compute_mutation(base_lines: list[str], member_lines: list[str]) -> list[tuple[int, str]]:
    """
    Compute compact mutation: list of (line_idx, line_content) for lines that differ.
    Line indices 0-based. Omit lines that match base.
    """
    mutations: list[tuple[int, str]] = []
    for i, (b, m) in enumerate(zip(base_lines, member_lines)):
        if b != m:
            mutations.append((i, m))
    if len(member_lines) > len(base_lines):
        for i in range(len(base_lines), len(member_lines)):
            mutations.append((i, member_lines[i]))
    return mutations


def _apply_mutation(base_lines: list[str], mutations: list[tuple[int, str]]) -> list[str]:
    """Apply mutation to base. Returns reconstructed lines."""
    result = list(base_lines)
    for idx, content in mutations:
        if idx < len(result):
            result[idx] = content
        else:
            result.append(content)
    return result


def build_mutation_chain(
    paths: list[Path],
    contents: dict[Path, str],
    min_family_size: int = 3,
    min_lines: int = 5,
    min_line_overlap_ratio: float = 0.85,
) -> dict[str, Any] | None:
    """
    Build mutation chain for a group of similar files.
    Returns None if not applicable or not net-positive.

    Requirements:
    - Same line count (or within 1 for edge case)
    - High line overlap (lines that match base)
    - Net-positive: base_size + sum(mutation_sizes) + overhead < sum(original_sizes)
    """
    if len(paths) < min_family_size:
        return None
    lines_map: dict[Path, list[str]] = {}
    for p in paths:
        text = contents.get(p)
        if not text:
            return None
        lines = text.splitlines(keepends=True)
        if len(lines) < min_lines:
            return None
        lines_map[p] = lines

    base = _pick_base(paths, contents)
    base_lines = lines_map[base]
    base_size = len(contents[base].encode("utf-8"))

    mutations_map: dict[str, list[tuple[int, str]]] = {}
    total_original = 0
    total_mutation_bytes = 0
    for p in paths:
        total_original += len(contents[p].encode("utf-8"))
        if p == base:
            continue
        member_lines = lines_map[p]
        if len(member_lines) != len(base_lines):
            return None
        mut = _compute_mutation(base_lines, member_lines)
        overlap = 1.0 - len(mut) / len(base_lines) if base_lines else 0
        if overlap < min_line_overlap_ratio:
            return None
        mut_bytes = sum(len(str(i).encode("utf-8")) + len(c.encode("utf-8")) + 8 for i, c in mut)
        total_mutation_bytes += mut_bytes
        mutations_map[str(p)] = mut

    overhead = 50 + len(paths) * 15
    chain_size = base_size + total_mutation_bytes + overhead
    gross_saved = total_original - chain_size
    if gross_saved <= 0:
        return None

    return {
        "base_path": str(base),
        "base_content": contents[base],
        "mutations": mutations_map,
        "paths": [str(p) for p in paths],
        "gain_bytes": gross_saved,
        "base_size": base_size,
        "mutation_count": len(mutations_map),
    }


def reconstruct_from_chain(chain: dict[str, Any]) -> dict[str, str]:
    """Reconstruct all member contents from chain. Byte-exact."""
    base_content = chain["base_content"]
    base_lines = base_content.splitlines(keepends=True)
    mutations = chain.get("mutations", {})
    result: dict[str, str] = {chain["base_path"]: base_content}
    for path_str, mut_list in mutations.items():
        lines = _apply_mutation(base_lines, mut_list)
        result[path_str] = "".join(lines)
    return result


def mutation_chain_to_unfold_recipe(chain: dict[str, Any]) -> dict[str, Any]:
    """Convert chain to unfold_recipe format for FoldRecord/package."""
    return {
        "base_path": chain["base_path"],
        "base_content": chain["base_content"],
        "mutations": chain["mutations"],
        "paths": chain["paths"],
        "mutation_chain": True,
    }


def _line_overlap_ratio(base_lines: list[str], member_lines: list[str]) -> float:
    """Fraction of lines that match between base and member."""
    if len(base_lines) != len(member_lines) or not base_lines:
        return 0.0
    matches = sum(1 for b, m in zip(base_lines, member_lines) if b == m)
    return matches / len(base_lines)


def find_mutation_chain_candidates(
    path_to_content: dict[Path, str],
    min_family_size: int = 3,
    min_lines: int = 5,
    min_line_overlap_ratio: float = 0.85,
) -> list[dict[str, Any]]:
    """
    Find groups of similar files that could form mutation chains.
    Groups by line count, then for each group picks base (smallest) and
    keeps only members with sufficient line overlap. Returns net-positive chains.
    """
    by_line_count: dict[int, list[Path]] = {}
    for p, text in path_to_content.items():
        if not text or not text.strip():
            continue
        lines = text.splitlines(keepends=True)
        if len(lines) < min_lines:
            continue
        n = len(lines)
        by_line_count.setdefault(n, []).append(p)

    chains: list[dict[str, Any]] = []
    seen_paths: set[Path] = set()

    for n_lines, paths in by_line_count.items():
        paths = [p for p in paths if p not in seen_paths]
        if len(paths) < min_family_size:
            continue
        contents = {p: path_to_content[p] for p in paths}
        base = _pick_base(paths, contents)
        base_lines = contents[base].splitlines(keepends=True)
        similar = [p for p in paths if _line_overlap_ratio(base_lines, contents[p].splitlines(keepends=True)) >= min_line_overlap_ratio]
        if len(similar) < min_family_size:
            continue
        sub_contents = {p: contents[p] for p in similar}
        chain = build_mutation_chain(
            similar,
            sub_contents,
            min_family_size=min_family_size,
            min_lines=min_lines,
            min_line_overlap_ratio=min_line_overlap_ratio,
        )
        if chain:
            for p in similar:
                seen_paths.add(p)
            chains.append(chain)
    return chains
