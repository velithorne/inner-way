"""
Phase 10: Tesseract Signature schema and generation.

Multi-dimensional family identity: structure, byte reuse, metadata reuse, temporal persistence.
Additive layer; does not replace existing signature systems.
"""

from __future__ import annotations

import hashlib
import json
from pathlib import Path
from typing import Any

TESSERACT_DIMENSIONS = ("structure", "byte", "metadata", "time")


def build_tesseract_signature(
    *,
    family_type: str,
    operator: str,
    structure_sig: str | None = None,
    byte_sig: str | None = None,
    metadata_sig: str | None = None,
    time_sig: str | None = None,
    structure_strength: float | None = None,
    byte_strength: float | None = None,
    metadata_strength: float | None = None,
    time_strength: float | None = None,
) -> dict[str, Any]:
    """
    Build a Tesseract Signature dict. Deterministic.
    dimensions_present lists which dimensions have non-empty sigs.
    """
    dims: list[str] = []
    if structure_sig:
        dims.append("structure")
    if byte_sig:
        dims.append("byte")
    if metadata_sig:
        dims.append("metadata")
    if time_sig:
        dims.append("time")

    out: dict[str, Any] = {
        "family_type": family_type,
        "operator": operator,
        "structure_sig": structure_sig or None,
        "byte_sig": byte_sig or None,
        "metadata_sig": metadata_sig or None,
        "time_sig": time_sig or None,
        "dimensions_present": dims,
    }
    if structure_strength is not None:
        out["structure_strength"] = round(structure_strength, 4)
    if byte_strength is not None:
        out["byte_strength"] = round(byte_strength, 4)
    if metadata_strength is not None:
        out["metadata_strength"] = round(metadata_strength, 4)
    if time_strength is not None:
        out["time_strength"] = round(time_strength, 4)
    return out


def tesseract_summary(tess: dict[str, Any]) -> str:
    """Concise human-readable summary of dimensions."""
    dims = tess.get("dimensions_present", [])
    if not dims:
        return "no dimensions"
    parts = []
    if "structure" in dims:
        parts.append("structural")
    if "byte" in dims:
        parts.append("byte_reuse")
    if "metadata" in dims:
        parts.append("metadata_reuse")
    if "time" in dims:
        parts.append("temporal")
    return "+".join(parts)


def build_tesseract_from_lineage_entry(
    operator: str,
    signature: str,
    entry: dict[str, Any],
) -> dict[str, Any]:
    """
    Build minimal Tesseract from lineage tracking entry (no archive).
    Has structure_sig and time_sig. Deterministic.
    """
    family_type = {
        "exact_repetition": "duplicate",
        "template_skeleton": "template",
        "hierarchy_mirror": "hierarchy",
        "dependency_motif": "dependency",
        "byte_fold": "byte_fold",
    }.get(operator, operator)
    first = entry.get("first_seen_snapshot", "")
    last = entry.get("last_seen_snapshot", "")
    count = entry.get("snapshot_count", 0)
    present = entry.get("present_in_latest", False)
    time_parts = [first or "", last or "", str(count), "1" if present else "0"]
    time_sig = hashlib.sha256(json.dumps(time_parts).encode()).hexdigest()[:12]
    time_strength = min(1.0, 0.3 + (count * 0.1)) if count else 0.2
    return build_tesseract_signature(
        family_type=family_type,
        operator=operator,
        structure_sig=signature,
        time_sig=time_sig,
        structure_strength=0.6,
        time_strength=time_strength,
    )


def build_tesseract_signatures_from_archive(
    archive_path: Path,
    *,
    lineage_tracking: dict[str, Any] | None = None,
) -> list[dict[str, Any]]:
    """
    Generate Tesseract Signatures for all fold families in an archive.
    Uses existing _family_signatures, byte_fold metrics, metadata_table_fold, lineage.
    lineage_tracking: from compute_lineage_tracking when available (for time_sig).
    """
    import tempfile
    import zipfile

    from infold.archive.operations import _family_signatures
    from infold.engine.metadata_table_fold import load_path_table as _load_pt

    result: list[dict[str, Any]] = []
    with tempfile.TemporaryDirectory(prefix="infold_tess_") as tmp:
        with zipfile.ZipFile(archive_path, "r") as zf:
            zf.extractall(tmp)
        def _extract_root(p: Path) -> Path:
            if (p / "manifest.json").exists():
                return p
            found = list(p.rglob("manifest.json"))
            return found[0].parent if found else p

        root = _extract_root(Path(tmp))
        maps_path = root / "maps" / "reconstruction.json"
        if not maps_path.exists():
            return []
        maps_data = json.loads(maps_path.read_text(encoding="utf-8"))
        records = maps_data.get("records", [])
        path_table = None
        if maps_data.get("path_table_ref"):
            path_table = _load_pt(root)
        sigs = _family_signatures(root, records)

        report_path = root / "reports" / "report.json"
        report = {}
        if report_path.exists():
            report = json.loads(report_path.read_text(encoding="utf-8"))
        bfm = report.get("byte_fold_metrics") or {}
        mtf = report.get("metadata_table_fold_metrics") or {}
        has_metadata_table = path_table is not None and len(path_table) > 0

        ops_map = lineage_tracking.get("operators", {}) if lineage_tracking else {}
        latest_id = lineage_tracking.get("latest_snapshot_id", "") if lineage_tracking else ""

        recon_chunk_path = root / "maps" / "chunk_reconstruction.json"
        chunk_recs = []
        if recon_chunk_path.exists():
            chunk_data = json.loads(recon_chunk_path.read_text(encoding="utf-8"))
            chunk_recs = chunk_data.get("records", [])

        for op, sig_map in sigs.items():
            family_type = {
                "exact_repetition": "duplicate",
                "template_skeleton": "template",
                "hierarchy_mirror": "hierarchy",
                "dependency_motif": "dependency",
                "byte_fold": "byte_fold",
            }.get(op, op)
            for sig, paths in sig_map.items():
                structure_sig = sig
                byte_sig_val: str | None = None
                metadata_sig_val: str | None = None
                time_sig_val: str | None = None
                byte_strength = None
                metadata_strength = None
                time_strength = None

                if op == "byte_fold":
                    byte_sig_val = sig
                    crr = bfm.get("chunk_reuse_ratio")
                    if isinstance(crr, list) and crr:
                        byte_strength = sum(crr) / len(crr)
                    elif isinstance(crr, (int, float)):
                        byte_strength = float(crr)

                if has_metadata_table and paths:
                    path_refs = tuple(sorted(str(p) for p in paths))
                    metadata_sig_val = hashlib.sha256(json.dumps(path_refs).encode()).hexdigest()[:12]
                    metadata_strength = 0.5 if mtf else 0.3

                if lineage_tracking and ops_map:
                    entry = ops_map.get(op, {}).get(sig)
                    if entry:
                        first = entry.get("first_seen_snapshot", "")
                        last = entry.get("last_seen_snapshot", "")
                        count = entry.get("snapshot_count", 0)
                        present = entry.get("present_in_latest", False)
                        time_parts = [first or "", last or "", str(count), "1" if present else "0"]
                        time_sig_val = hashlib.sha256(json.dumps(time_parts).encode()).hexdigest()[:12]
                        time_strength = min(1.0, 0.3 + (count * 0.1)) if count else 0.2

                structure_strength = 0.8 if op in ("template_skeleton", "hierarchy_mirror", "dependency_motif") else 0.6

                tess = build_tesseract_signature(
                    family_type=family_type,
                    operator=op,
                    structure_sig=structure_sig,
                    byte_sig=byte_sig_val,
                    metadata_sig=metadata_sig_val,
                    time_sig=time_sig_val,
                    structure_strength=structure_strength,
                    byte_strength=byte_strength,
                    metadata_strength=metadata_strength,
                    time_strength=time_strength,
                )
                result.append(tess)
    return result


def get_tesseract_for_match(
    match: dict[str, Any],
    tesseract_list: list[dict[str, Any]],
) -> dict[str, Any] | None:
    """
    Find Tesseract Signature for a search match. Match has operator_id, paths or roots_or_paths.
    Returns tesseract dict or None.
    """
    op = match.get("operator_id", "")
    paths = match.get("paths") or match.get("roots_or_paths") or []
    if not paths:
        return None
    paths_tuple = tuple(sorted(str(p) for p in paths))
    sig = hashlib.sha256(json.dumps(paths_tuple).encode()).hexdigest()[:16]
    for t in tesseract_list:
        if t.get("operator") != op:
            continue
        if t.get("structure_sig") == sig:
            return t
    return None
