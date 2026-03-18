"""
Phase 14A: Path DNA Folding.

Compact reversible representation for repeated path ancestry.
Encodes shared roots once, members as (root_idx, leaf) where beneficial.
Deterministic, exactly reversible. Used when net positive (micro mode).
"""

from __future__ import annotations


def _directory_prefixes(path: str) -> list[str]:
    """Return path prefixes that end with / (directory-like). Deterministic."""
    parts = path.replace("\\", "/").split("/")
    prefixes: list[str] = []
    acc = ""
    for i, p in enumerate(parts):
        if i < len(parts) - 1:  # not the last (filename)
            acc = acc + p + "/" if acc else p + "/"
            prefixes.append(acc)
    return prefixes


def build_path_dna(paths: list[str]) -> dict | None:
    """
    Build Path DNA encoding: shared roots + (root_idx, leaf) per path.
    Returns None if encoding would not save bytes.
    Deterministic. paths must be in the order of the path_table (sorted).
    """
    if not paths:
        return None
    paths = [p.replace("\\", "/") for p in paths]

    # Count prefix occurrences; roots = prefixes appearing in >= 2 paths
    prefix_count: dict[str, int] = {}
    for p in paths:
        for pre in _directory_prefixes(p):
            prefix_count[pre] = prefix_count.get(pre, 0) + 1

    roots = sorted(pre for pre, cnt in prefix_count.items() if cnt >= 2)
    if not roots:
        return None

    # Encode each path: use longest matching root
    encoded: list[list] = []
    for p in paths:
        best_root_idx = -1
        best_root_len = 0
        for i, r in enumerate(roots):
            if p.startswith(r) and len(r) > best_root_len:
                best_root_idx = i
                best_root_len = len(r)
        if best_root_idx >= 0:
            leaf = p[best_root_len:]
            encoded.append([best_root_idx, leaf])
        else:
            encoded.append([-1, p])  # no root match, store full path

    # Estimate sizes
    import json
    orig_json = json.dumps(paths, separators=(",", ":"))
    dna = {"r": roots, "p": encoded}
    enc_json = json.dumps(dna, separators=(",", ":"))
    if len(enc_json) >= len(orig_json):
        return None
    return dna


def expand_path_dna(dna: dict) -> list[str]:
    """Expand Path DNA back to full paths. Deterministic."""
    roots = dna.get("r", [])
    encoded = dna.get("p", [])
    out: list[str] = []
    for item in encoded:
        if isinstance(item, list) and len(item) >= 2:
            idx, leaf = item[0], item[1]
            if 0 <= idx < len(roots):
                out.append(roots[idx] + leaf)
            else:
                out.append(leaf if isinstance(leaf, str) else str(leaf))
        else:
            out.append(str(item))
    return out


def path_dna_net_saved(paths: list[str], dna: dict | None) -> int:
    """Bytes saved by Path DNA vs raw paths. 0 if dna is None."""
    if not dna:
        return 0
    import json
    orig = len(json.dumps(paths, separators=(",", ":")).encode("utf-8"))
    enc = len(json.dumps(dna, separators=(",", ":")).encode("utf-8"))
    return orig - enc
