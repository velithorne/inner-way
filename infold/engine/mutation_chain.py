"""
Phase 16A/16B/16C: Mutation Chain — base + compact mutations for similar files.

Conservative, deterministic representation for closely related text files.
- base member: one full file
- mutated members: compact line-based diffs (single or contiguous blocks)
- net-positive only
- Phase 16B: better grouping, cost-aware base selection, compact encoding
- Phase 16C: anchor-aware grouping, microscope-aware activation, template handoff
"""

from __future__ import annotations

from pathlib import Path
from typing import Any


def _path_to_anchor_scopes(anchors: list[dict[str, Any]]) -> dict[str, set[str]]:
    """Map path to set of anchor path ids. Delegates to shared anchor utilities."""
    from infold.engine.anchor_file import build_path_to_anchor_scopes
    return build_path_to_anchor_scopes(anchors)


def _paths_share_anchor(paths: list[Path], path_to_scopes: dict[str, set[str]]) -> str | None:
    """Return anchor id if all paths share at least one anchor scope. Uses shared utilities."""
    from infold.engine.anchor_file import paths_share_anchor
    return paths_share_anchor(paths, path_to_scopes)


def _compute_mutation(base_lines: list[str], member_lines: list[str]) -> list:
    """
    Compute mutation: list of (line_idx, line_content) or (start_idx, [lines]) for contiguous blocks.
    Phase 16B: Use contiguous block encoding when 2+ adjacent lines differ.
    """
    diffs: list = []
    i = 0
    while i < len(member_lines):
        if i < len(base_lines) and base_lines[i] == member_lines[i]:
            i += 1
            continue
        block_start = i
        block: list[str] = []
        while i < len(member_lines):
            if i < len(base_lines) and base_lines[i] == member_lines[i]:
                break
            block.append(member_lines[i])
            i += 1
        if len(block) >= 2:
            diffs.append((block_start, block))
        elif block:
            diffs.append((block_start, block[0]))
    return diffs


def _mutation_payload_bytes(mut: list) -> int:
    """Estimate bytes for mutation encoding."""
    total = 0
    for item in mut:
        if isinstance(item[1], list):
            total += 6 + len(str(item[0])) + sum(len(c.encode("utf-8")) for c in item[1])
        else:
            total += 8 + len(str(item[0])) + len(item[1].encode("utf-8"))
    return total


def _apply_mutation(base_lines: list[str], mutations: list) -> list[str]:
    """Apply mutation to base. Handles single (idx, str) and block (idx, [lines])."""
    result = list(base_lines)
    for item in mutations:
        idx = item[0]
        content = item[1]
        if isinstance(content, list):
            for k, line in enumerate(content):
                if idx + k < len(result):
                    result[idx + k] = line
                else:
                    result.append(line)
        else:
            if idx < len(result):
                result[idx] = content
            else:
                result.append(content)
    return result


def _pick_base(paths: list[Path], contents: dict[Path, str]) -> Path:
    """Legacy: smallest by bytes, then first by path."""
    return min(paths, key=lambda p: (len(contents.get(p, "").encode("utf-8")), str(p)))


def _pick_base_min_mutation_payload(
    paths: list[Path],
    contents: dict[Path, str],
    lines_map: dict[Path, list[str]],
    path_to_anchor_scopes: dict[str, set[str]] | None = None,
) -> Path:
    """
    Phase 16B/16C: Choose base that minimizes total mutation payload.
    Tie-breakers: (1) total mutation cost lower, (2) same-anchor locality preferred,
    (3) smallest base size, (4) first path lexicographically.
    """
    def score(base: Path) -> tuple[int, int, int, int, str]:
        base_lines = lines_map[base]
        total_mut = 0
        for p in paths:
            if p == base:
                continue
            member_lines = lines_map[p]
            if len(member_lines) != len(base_lines):
                return (999999, 999999, 999999, 999999, str(base))
            mut = _compute_mutation(base_lines, member_lines)
            total_mut += _mutation_payload_bytes(mut)
        base_size = len(contents[base].encode("utf-8"))
        # Anchor locality: 0 if base shares anchor with all, else 1
        anchor_ok = 0
        if path_to_anchor_scopes and paths:
            common = path_to_anchor_scopes.get(str(base).replace("\\", "/"), set())
            for p in paths:
                if p != base:
                    common &= path_to_anchor_scopes.get(str(p).replace("\\", "/"), set())
            anchor_ok = 0 if common else 1
        return (total_mut, anchor_ok, base_size, 0, str(base))

    return min(paths, key=lambda p: score(p))


def build_mutation_chain(
    paths: list[Path],
    contents: dict[Path, str],
    min_family_size: int = 3,
    min_lines: int = 5,
    min_line_overlap_ratio: float = 0.85,
    use_cost_aware_base: bool = True,
    path_to_anchor_scopes: dict[str, set[str]] | None = None,
) -> dict[str, Any] | None:
    """
    Build mutation chain for a group of similar files.
    Phase 16B/16C: cost-aware base selection, anchor locality tie-breaker.
    Returns None if not net-positive.
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

    if use_cost_aware_base:
        base = _pick_base_min_mutation_payload(paths, contents, lines_map, path_to_anchor_scopes)
    else:
        base = min(paths, key=lambda p: (len(contents[p].encode("utf-8")), str(p)))

    base_lines = lines_map[base]
    base_size = len(contents[base].encode("utf-8"))

    mutations_map: dict[str, list] = {}
    total_original = 0
    total_mutation_bytes = 0
    total_changed_lines = 0
    for p in paths:
        total_original += len(contents[p].encode("utf-8"))
        if p == base:
            continue
        member_lines = lines_map[p]
        if len(member_lines) != len(base_lines):
            return None
        mut = _compute_mutation(base_lines, member_lines)
        overlap = 1.0 - sum(len(m[1]) if isinstance(m[1], list) else 1 for m in mut) / len(base_lines) if base_lines else 0
        if overlap < min_line_overlap_ratio:
            return None
        mb = _mutation_payload_bytes(mut)
        total_mutation_bytes += mb
        total_changed_lines += sum(len(m[1]) if isinstance(m[1], list) else 1 for m in mut)
        mutations_map[str(p)] = _encode_mutation_for_storage(mut)

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
        "total_changed_lines": total_changed_lines,
        "avg_changed_lines": total_changed_lines / len(mutations_map) if mutations_map else 0,
    }


def _encode_mutation_for_storage(mut: list) -> list:
    """Encode mutation for JSON: [idx, str] or [idx, [lines]] for blocks."""
    out: list[Any] = []
    for item in mut:
        out.append([item[0], item[1]])
    return out


def _decode_mutation_from_storage(stored: list) -> list:
    """Decode mutation from JSON. item[1] is str or list of str."""
    out: list = []
    for item in stored:
        if isinstance(item, (list, tuple)) and len(item) >= 2:
            idx, content = item[0], item[1]
            out.append((idx, content))
        else:
            out.append(item)
    return out


def reconstruct_from_chain(chain: dict[str, Any]) -> dict[str, str]:
    """Reconstruct all member contents from chain. Byte-exact."""
    base_content = chain["base_content"]
    base_lines = base_content.splitlines(keepends=True)
    mutations = chain.get("mutations", {})
    result: dict[str, str] = {chain["base_path"]: base_content}
    for path_str, mut_stored in mutations.items():
        mut = _decode_mutation_from_storage(mut_stored) if isinstance(mut_stored, list) else mut_stored
        lines = _apply_mutation(base_lines, mut)
        result[path_str] = "".join(lines)
    return result


def mutation_chain_to_unfold_recipe(chain: dict[str, Any]) -> dict[str, Any]:
    """Convert chain to unfold_recipe format for FoldRecord/package."""
    out: dict[str, Any] = {
        "base_path": chain["base_path"],
        "base_content": chain["base_content"],
        "mutations": chain["mutations"],
        "paths": chain["paths"],
        "mutation_chain": True,
    }
    if "avg_changed_lines" in chain:
        out["avg_changed_lines"] = chain["avg_changed_lines"]
    if "total_changed_lines" in chain:
        out["total_changed_lines"] = chain["total_changed_lines"]
    if "_source" in chain:
        out["_source"] = chain["_source"]  # for reporting only
    return out


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
    group_by_extension: bool = True,
    anchors: list[dict[str, Any]] | None = None,
    template_rejected_groups: list[tuple[list[Path], str]] | None = None,
    microscope_groups: list[tuple[list[Path], list[list[str]]]] | None = None,
    committed_paths: set[str] | None = None,
) -> list[dict[str, Any]]:
    """
    Find groups of similar files that could form mutation chains.
    Phase 16C: Anchor-aware grouping, microscope groups, template rejection handoff.
    """
    committed = committed_paths or set()
    path_to_scopes = _path_to_anchor_scopes(anchors) if anchors else None

    def _try_chain(
        paths: list[Path],
        relaxed_min_lines: int,
        source: str,
    ) -> dict[str, Any] | None:
        paths = [p for p in paths if str(p).replace("\\", "/") not in committed and p in path_to_content]
        if len(paths) < min_family_size:
            return None
        contents = {p: path_to_content[p] for p in paths}
        lines_map_local = {p: path_to_content[p].splitlines(keepends=True) for p in paths}
        base = _pick_base_min_mutation_payload(paths, contents, lines_map_local, path_to_scopes)
        base_lines = contents[base].splitlines(keepends=True)
        similar = [p for p in paths if _line_overlap_ratio(base_lines, contents[p].splitlines(keepends=True)) >= min_line_overlap_ratio]
        if len(similar) < min_family_size:
            return None
        sub_contents = {p: contents[p] for p in similar}
        chain = build_mutation_chain(
            similar,
            sub_contents,
            min_family_size=min_family_size,
            min_lines=relaxed_min_lines,
            min_line_overlap_ratio=min_line_overlap_ratio,
            use_cost_aware_base=True,
            path_to_anchor_scopes=path_to_scopes,
        )
        if chain:
            chain["_source"] = source
        return chain

    chains: list[dict[str, Any]] = []
    seen_paths: set[Path] = set()

    # 1. Template rejection handoff: try rejected groups with relaxed min_lines
    if template_rejected_groups:
        for paths, reason in template_rejected_groups:
            if "exact_duplicates" in reason:
                continue
            if len(paths) < min_family_size:
                continue
            path_objs = [Path(p) if isinstance(p, str) else p for p in paths]
            chain = _try_chain(path_objs, 2, "template_rejected")
            if chain:
                for p in chain["paths"]:
                    seen_paths.add(Path(p))
                chains.append(chain)

    # 2. Microscope groups: tiny files with relaxed min_lines
    if microscope_groups:
        for paths, lines_list in microscope_groups:
            path_objs = [Path(p) if isinstance(p, str) else p for p in paths]
            chain = _try_chain(path_objs, 2, "microscope")
            if chain:
                for p in chain["paths"]:
                    seen_paths.add(Path(p))
                chains.append(chain)

    # 3. Anchor-aware buckets: (ext, line_count, anchor_id) for same-scope grouping
    buckets: dict[tuple[str, int, str | None], list[Path]] = {}
    for p, text in path_to_content.items():
        if p in seen_paths or not text or not text.strip():
            continue
        if str(p).replace("\\", "/") in committed:
            continue
        lines = text.splitlines(keepends=True)
        if len(lines) < min_lines:
            continue
        ext = p.suffix.lower() if group_by_extension else ""
        anchor_id = None
        if path_to_scopes:
            scopes = path_to_scopes.get(str(p).replace("\\", "/"), set())
            anchor_id = min(scopes) if scopes else None
        key = (ext, len(lines), anchor_id)
        buckets.setdefault(key, []).append(p)

    for key, paths in buckets.items():
        paths = [p for p in paths if p not in seen_paths]
        if len(paths) < min_family_size:
            continue
        chain = _try_chain(paths, min_lines, "anchor" if key[2] else "default")
        if chain:
            for p in chain["paths"]:
                seen_paths.add(Path(p))
            chains.append(chain)

    # 4. Fallback: (ext, line_count) without anchor split for remaining paths
    fallback_buckets: dict[tuple[str, int], list[Path]] = {}
    for p, text in path_to_content.items():
        if p in seen_paths or not text or not text.strip():
            continue
        if str(p).replace("\\", "/") in committed:
            continue
        lines = text.splitlines(keepends=True)
        if len(lines) < min_lines:
            continue
        ext = p.suffix.lower() if group_by_extension else ""
        key = (ext, len(lines))
        fallback_buckets.setdefault(key, []).append(p)

    for key, paths in fallback_buckets.items():
        paths = [p for p in paths if p not in seen_paths]
        if len(paths) < min_family_size:
            continue
        chain = _try_chain(paths, min_lines, "fallback")
        if chain:
            for p in chain["paths"]:
                seen_paths.add(Path(p))
            chains.append(chain)

    return chains
