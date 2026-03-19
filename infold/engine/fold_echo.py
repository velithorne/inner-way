"""
Fold Echo v0.1/v0.2: attach weak files as low-cost echoes to already accepted families.

- Host families: template_skeleton (optionally mutation_chain)
- Echo = passthrough file that fits host structure
- v0.2: Anchor-aware host selection, path proximity, microscope-assisted candidates
- Net-positive only, deterministic, exact reconstruction
"""

from pathlib import Path
from typing import Any

from infold.models.project_sheet import ProjectSheet


def _common_prefix_len(a: str, b: str) -> int:
    """Length of common path prefix (directory proximity). Deterministic."""
    pa = Path(a.replace("\\", "/"))
    pb = Path(b.replace("\\", "/"))
    parts_a = pa.parts
    parts_b = pb.parts
    n = 0
    for x, y in zip(parts_a, parts_b):
        if x == y:
            n += 1
        else:
            break
    return n


def _path_proximity(echo_path: str, host_paths: list[str]) -> int:
    """Max common prefix length between echo and any host path. Higher = closer."""
    if not host_paths:
        return 0
    return max(_common_prefix_len(echo_path, hp) for hp in host_paths)


def _anchor_scope_match(
    echo_path: str,
    host_paths: list[str],
    path_to_anchor_scopes: dict[str, set[str]],
) -> bool:
    """True if echo and host share at least one anchor scope."""
    echo_anchors = path_to_anchor_scopes.get(echo_path, set())
    for hp in host_paths:
        host_anchors = path_to_anchor_scopes.get(hp, set())
        if echo_anchors & host_anchors:
            return True
    return False


def _build_path_to_anchor_scopes(anchors: list[dict[str, Any]]) -> dict[str, set[str]]:
    """Map each path to set of anchor path identifiers that scope it."""
    path_to_scopes: dict[str, set[str]] = {}
    for anchor in anchors:
        anchor_id = anchor.get("path", "")
        for p in anchor.get("anchored_paths", []):
            path_to_scopes.setdefault(p, set()).add(anchor_id)
    return path_to_scopes


def _extract_slot_values_for_file(
    lines: list[str],
    const_blocks: list[str],
    slot_groups: list[list],
) -> list[str] | None:
    """
    Extract slot values for a single file that matches the template structure.
    Structure: const[0], slot[0], const[1], slot[1], ..., const[k]. Interleaved.
    Returns list of slot value strings (one per slot block), or None if file doesn't fit.
    """
    n_lines = len(lines)
    total_const = sum(len(cb.splitlines(keepends=True)) for cb in const_blocks)
    total_slot = sum(len(sg) for sg in slot_groups)
    if n_lines != total_const + total_slot:
        return None
    line_idx = 0
    slot_values: list[str] = []
    for bi, cb in enumerate(const_blocks):
        for cl in cb.splitlines(keepends=True):
            if line_idx >= n_lines or lines[line_idx] != cl:
                return None
            line_idx += 1
        if bi < len(slot_groups):
            sg = slot_groups[bi]
            block_vals: list[str] = []
            for _ in sg:
                if line_idx >= n_lines:
                    return None
                block_vals.append(lines[line_idx])
                line_idx += 1
            slot_values.append("".join(block_vals))
    return slot_values if line_idx == n_lines else None


def _is_microscope_eligible(
    path_str: str,
    project_sheet: ProjectSheet,
    max_bytes: int = 512,
    max_lines: int = 10,
) -> bool:
    """True if file is eligible for microscope (tiny, text type)."""
    path = Path(path_str)
    node = project_sheet.file_nodes.get(path)
    if not node:
        return False
    raw = node.raw_text
    if len(raw.encode("utf-8")) > max_bytes:
        return False
    lines = raw.splitlines(keepends=True)
    if len(lines) > max_lines or len(lines) < 2:
        return False
    return True


def find_template_echo_candidates(
    project_sheet: ProjectSheet,
    ledger_fold_records: list[Any],
    committed_paths: set[str],
    min_echo_similarity: float = 0.85,
    min_net_gain: int = 16,
    anchors: list[dict[str, Any]] | None = None,
    microscope_assisted_paths: set[str] | None = None,
) -> list[dict[str, Any]]:
    """
    Find passthrough files that fit an accepted template family as echoes.
    v0.2: Anchor-aware host selection - prefer hosts in same anchor scope, closer paths.
    Returns list of echo candidates: {host_operator, host_idx, echo_path, slot_values, gain, ...}
    """
    candidates: list[dict[str, Any]] = []
    passthrough = {
        p for p in project_sheet.file_nodes
        if str(p).replace("\\", "/") not in committed_paths and not project_sheet.file_nodes[p].diagnostics
    }
    if not passthrough:
        return candidates

    path_to_anchor_scopes = _build_path_to_anchor_scopes(anchors) if anchors else {}

    # Build list of template hosts with their recipes
    template_hosts: list[tuple[int, Any, list[str], list, list[str]]] = []
    for i, rec in enumerate(ledger_fold_records):
        if rec.operator_id != "template_skeleton":
            continue
        recipe = rec.unfold_recipe
        const_blocks = recipe.get("const_blocks", [])
        slot_groups = recipe.get("slot_groups", [])
        host_paths = [str(t).replace("\\", "/") for t in rec.targets]
        if not const_blocks or not slot_groups:
            continue
        template_hosts.append((i, rec, const_blocks, slot_groups, host_paths))

    # For each passthrough file, find all matching hosts, then pick best (anchor-aware)
    for path in list(passthrough):
        path_str = str(path).replace("\\", "/")
        node = project_sheet.file_nodes.get(path)
        if not node:
            continue

        matches: list[dict[str, Any]] = []
        for i, rec, const_blocks, slot_groups, host_paths in template_hosts:
            if path_str in host_paths:
                continue
            ext = Path(host_paths[0]).suffix if host_paths else ""
            if Path(path_str).suffix != ext:
                continue
            n_lines_host = sum(len(cb.splitlines(keepends=True)) for cb in const_blocks) + sum(len(sg) for sg in slot_groups)
            lines = node.raw_text.splitlines(keepends=True)
            if len(lines) != n_lines_host:
                continue
            slot_values = _extract_slot_values_for_file(lines, const_blocks, slot_groups)
            if slot_values is None:
                continue
            file_bytes = len(node.raw_text.encode("utf-8"))
            slot_payload = sum(len(s.encode("utf-8")) for s in slot_values)
            overhead = 40 + len(path_str) + 15
            net = file_bytes - (slot_payload + overhead)
            if net < min_net_gain:
                continue

            anchor_match = _anchor_scope_match(path_str, host_paths, path_to_anchor_scopes)
            proximity = _path_proximity(path_str, host_paths)
            host_is_microscope_assisted = bool(microscope_assisted_paths and (set(host_paths) & microscope_assisted_paths))
            echo_is_microscope_eligible = _is_microscope_eligible(path_str, project_sheet)
            microscope_match = echo_is_microscope_eligible and host_is_microscope_assisted

            matches.append({
                "host_operator": "template_skeleton",
                "host_idx": i,
                "echo_path": path_str,
                "slot_values": slot_values,
                "gain": net,
                "file_bytes": file_bytes,
                "slot_payload": slot_payload,
                "_anchor_match": anchor_match,
                "_proximity": proximity,
                "_microscope_match": microscope_match,
            })

        if not matches:
            continue

        # Rank: anchor match, microscope match, proximity, gain (all descending)
        matches.sort(
            key=lambda m: (m["_anchor_match"], m["_microscope_match"], m["_proximity"], m["gain"]),
            reverse=True,
        )
        best = matches[0]
        del best["_anchor_match"]
        del best["_proximity"]
        del best["_microscope_match"]
        candidates.append(best)
        passthrough.discard(path)

    return candidates


def reconstruct_echo_from_template(
    const_blocks: list[str],
    slot_groups: list[list],
    slot_values: list[str],
) -> str:
    """Reconstruct file content from template structure + echo slot values."""
    out: list[str] = []
    for bi, const in enumerate(const_blocks):
        out.append(const)
        if bi < len(slot_groups) and bi < len(slot_values):
            out.append(slot_values[bi])
    return "".join(out)
