"""
Fold Echo v0.1: attach weak files as low-cost echoes to already accepted families.

- Host families: template_skeleton (optionally mutation_chain)
- Echo = passthrough file that fits host structure
- Net-positive only, deterministic, exact reconstruction
"""

from pathlib import Path
from typing import Any

from infold.models.project_sheet import ProjectSheet


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


def find_template_echo_candidates(
    project_sheet: ProjectSheet,
    ledger_fold_records: list[Any],
    committed_paths: set[str],
    min_echo_similarity: float = 0.85,
    min_net_gain: int = 16,
) -> list[dict[str, Any]]:
    """
    Find passthrough files that fit an accepted template family as echoes.
    Returns list of echo candidates: {host_operator, host_idx, echo_path, slot_values, gain, ...}
    """
    candidates: list[dict[str, Any]] = []
    passthrough = {
        p for p in project_sheet.file_nodes
        if str(p).replace("\\", "/") not in committed_paths and not project_sheet.file_nodes[p].diagnostics
    }
    if not passthrough:
        return candidates

    for i, rec in enumerate(ledger_fold_records):
        if rec.operator_id != "template_skeleton":
            continue
        recipe = rec.unfold_recipe
        const_blocks = recipe.get("const_blocks", [])
        slot_groups = recipe.get("slot_groups", [])
        host_paths = [str(t).replace("\\", "/") for t in rec.targets]
        if not const_blocks or not slot_groups:
            continue
        # Get host extension from first path
        ext = Path(host_paths[0]).suffix if host_paths else ""
        n_lines_host = sum(len(cb.splitlines(keepends=True)) for cb in const_blocks) + sum(len(sg) for sg in slot_groups)

        for path in list(passthrough):
            path_str = str(path).replace("\\", "/")
            if path_str in host_paths:
                continue
            if Path(path_str).suffix != ext:
                continue
            node = project_sheet.file_nodes.get(path)
            if not node:
                continue
            lines = node.raw_text.splitlines(keepends=True)
            if len(lines) != n_lines_host:
                continue
            slot_values = _extract_slot_values_for_file(lines, const_blocks, slot_groups)
            if slot_values is None:
                continue
            # Net-positive check
            file_bytes = len(node.raw_text.encode("utf-8"))
            slot_payload = sum(len(s.encode("utf-8")) for s in slot_values)
            overhead = 40 + len(path_str) + 15  # path, host ref, wrapper (reduced for small echoes)
            net = file_bytes - (slot_payload + overhead)
            if net < min_net_gain:
                continue
            candidates.append({
                "host_operator": "template_skeleton",
                "host_idx": i,
                "echo_path": path_str,
                "slot_values": slot_values,
                "gain": net,
                "file_bytes": file_bytes,
                "slot_payload": slot_payload,
            })
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
