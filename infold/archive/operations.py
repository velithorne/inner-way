"""
Infold Archive v0.1: create, inspect, validate, reconstruct, list, stats, compare, search.

Deterministic validation. Integrity hashing. Preserves exact-mode guarantees.
Infold Search v0.1: archive-first search within .infold packages.
"""

import hashlib
import json
import zipfile
from pathlib import Path
from typing import Any

from infold.engine.package_spec import (
    MANIFEST_REQUIRED_KEYS,
    REQUIRED_DIRS,
    REQUIRED_FILES,
)
from infold.engine.package_schema import (
    check_logical_vs_physical,
    check_manifest_ledger_consistency,
)


def _sha256_file(path: Path) -> str:
    """Compute SHA256 of file. Deterministic."""
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(65536), b""):
            h.update(chunk)
    return h.hexdigest()


def create_archive(
    source_path: Path | str,
    output_path: Path | str,
    config: dict[str, Any] | None = None,
    profile: str = "auto",
) -> Path:
    """
    Create an Infold archive: run fold, export package, zip to .infold.
    profile: auto (default), sparrow, fox, dragon, golem, serpent.
    """
    import shutil
    import tempfile

    from infold.engine import run_fold
    from infold.engine import export_package

    if config is None:
        from infold.cli import load_config
        config = load_config()
    config = dict(config)
    config["_fold_profile"] = profile
    source = Path(source_path).resolve()
    out = Path(output_path).resolve()
    if out.suffix != ".infold":
        out = out.with_suffix(".infold")
    result = run_fold(source, config)
    pkg_dir = Path(tempfile.mkdtemp(prefix="infold_pkg_"))
    try:
        export_package(result, config, pkg_dir)
        from infold.engine.metadata_table_fold import apply_metadata_table_fold
        apply_metadata_table_fold(pkg_dir, config)
        _write_integrity_checksums(pkg_dir, config)
        with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as zf:
            for f in sorted(pkg_dir.rglob("*")):
                if f.is_file():
                    arcname = f.relative_to(pkg_dir)
                    zf.write(f, arcname)
    finally:
        if pkg_dir.exists():
            shutil.rmtree(pkg_dir)
    return out


def _write_integrity_checksums(pkg_dir: Path, config: dict[str, Any] | None = None) -> None:
    """Write integrity.json with SHA256 checksums for key files."""
    compact = config.get("package_export", {}).get("compact", False) if config else False
    checksums: dict[str, str] = {}
    for name in ["manifest.json", "ledger.json"]:
        p = pkg_dir / name
        if p.exists():
            checksums[name] = _sha256_file(p)
    shared_dir = pkg_dir / "shared"
    if shared_dir.exists():
        for f in sorted(shared_dir.iterdir()):
            if f.is_file():
                checksums[f"shared/{f.name}"] = _sha256_file(f)
        chunks_dir = shared_dir / "chunks"
        if chunks_dir.exists():
            for cf in sorted(chunks_dir.iterdir()):
                if cf.is_file():
                    checksums[f"shared/chunks/{cf.name}"] = _sha256_file(cf)
        metadata_tables_dir = shared_dir / "metadata_tables"
        if metadata_tables_dir.exists():
            for mf in sorted(metadata_tables_dir.iterdir()):
                if mf.is_file():
                    checksums[f"shared/metadata_tables/{mf.name}"] = _sha256_file(mf)
    maps_dir = pkg_dir / "maps"
    if maps_dir.exists():
        for f in sorted(maps_dir.iterdir()):
            if f.is_file():
                checksums[f"maps/{f.name}"] = _sha256_file(f)
    (pkg_dir / "integrity.json").write_text(
        json.dumps({"checksums": checksums}, indent=None, separators=(",", ":")) if compact else json.dumps({"checksums": checksums}, indent=2),
        encoding="utf-8",
    )


def _extract_root(tmp: Path) -> Path:
    """Find package root (where manifest.json lives)."""
    manifest = tmp / "manifest.json"
    if manifest.exists():
        return tmp
    found = list(tmp.rglob("manifest.json"))
    return found[0].parent if found else tmp


def _load_archive_metadata(root: Path) -> dict[str, Any]:
    """
    Load archive metadata and package metrics from package root.
    Used for archive-level filters in search.
    """
    manifest = {}
    if (root / "manifest.json").exists():
        manifest = json.loads((root / "manifest.json").read_text(encoding="utf-8"))
    compat = manifest.get("compatibility", {})
    report = {}
    if (root / "reports" / "report.json").exists():
        report = json.loads((root / "reports" / "report.json").read_text(encoding="utf-8"))
    rejection_count = len(report.get("rejected_candidates_summary", []))
    return {
        "spec_version": compat.get("spec_version", manifest.get("package_spec", "?")),
        "reconstruction_mode": compat.get("reconstruction_mode", "?"),
        "source_path": manifest.get("source_path", ""),
        "debug_friendly": compat.get("debug_friendly", False),
        "created": manifest.get("created", ""),
        "logical_gain_bytes": manifest.get("logical_gain_bytes", 0),
        "physical_folded_size_bytes": manifest.get("physical_folded_size_bytes", 0),
        "original_size_bytes": manifest.get("original_size_bytes", 0),
        "fold_count": manifest.get("fold_count", 0),
        "rejection_count": rejection_count,
    }


def _archive_passes_metadata_filters(meta: dict[str, Any], filters: dict[str, Any]) -> bool:
    """Check if archive metadata passes filters. Deterministic."""
    if filters.get("spec_version") and str(meta.get("spec_version", "")) != str(filters["spec_version"]):
        return False
    if filters.get("reconstruction_mode") and (meta.get("reconstruction_mode") or "").lower() != (filters["reconstruction_mode"] or "").lower():
        return False
    if filters.get("source_path"):
        sp = (meta.get("source_path") or "").lower()
        if (filters["source_path"] or "").lower() not in sp:
            return False
    if filters.get("debug_friendly") is not None and meta.get("debug_friendly") != filters["debug_friendly"]:
        return False
    if filters.get("created_after") and (meta.get("created") or "") < str(filters["created_after"]):
        return False
    if filters.get("created_before") and (meta.get("created") or "") > str(filters["created_before"]):
        return False
    return True


def _archive_passes_metric_filters(meta: dict[str, Any], filters: dict[str, Any]) -> bool:
    """Check if archive package metrics pass filters. Deterministic."""
    lg = meta.get("logical_gain_bytes", 0)
    pf = meta.get("physical_folded_size_bytes", 0)
    rc = meta.get("rejection_count", 0)
    fc = meta.get("fold_count", 0)
    if filters.get("min_logical_gain") is not None and lg < filters["min_logical_gain"]:
        return False
    if filters.get("max_logical_gain") is not None and lg > filters["max_logical_gain"]:
        return False
    if filters.get("min_physical_size") is not None and pf < filters["min_physical_size"]:
        return False
    if filters.get("max_physical_size") is not None and pf > filters["max_physical_size"]:
        return False
    if filters.get("min_rejection_count") is not None and rc < filters["min_rejection_count"]:
        return False
    if filters.get("max_rejection_count") is not None and rc > filters["max_rejection_count"]:
        return False
    if filters.get("min_fold_count") is not None and fc < filters["min_fold_count"]:
        return False
    if filters.get("max_fold_count") is not None and fc > filters["max_fold_count"]:
        return False
    return True


def explain_archive(archive_path: Path | str) -> dict[str, Any]:
    """
    Explain archive: summarize package contents, fold counts by operator,
    biggest gain contributors, rejected candidates, reconstruction guarantees.
    """
    archive = Path(archive_path).resolve()
    if not archive.exists():
        raise FileNotFoundError(f"Archive not found: {archive}")
    import tempfile
    with tempfile.TemporaryDirectory(prefix="infold_") as tmp:
        with zipfile.ZipFile(archive, "r") as zf:
            zf.extractall(tmp)
        root = _extract_root(Path(tmp))
        manifest = json.loads((root / "manifest.json").read_text(encoding="utf-8"))
        ledger = json.loads((root / "ledger.json").read_text(encoding="utf-8"))
        report = {}
        if (root / "reports" / "report.json").exists():
            report = json.loads((root / "reports" / "report.json").read_text(encoding="utf-8"))
        maps_data = json.loads((root / "maps" / "reconstruction.json").read_text(encoding="utf-8"))
        records = maps_data.get("records", [])

        fold_by_op: dict[str, int] = {}
        gain_by_op: dict[str, int] = {}
        for rec in records:
            op = rec.get("operator_id", "unknown")
            fold_by_op[op] = fold_by_op.get(op, 0) + 1
            gain_by_op[op] = gain_by_op.get(op, 0) + rec.get("gain", 0)

        gain_contributors = sorted(
            [{"operator_id": op, "gain": g} for op, g in gain_by_op.items()],
            key=lambda x: -x["gain"],
        )[:10]

        compat = manifest.get("compatibility", {})
        reconstruction_guarantees = [
            f"mode={compat.get('reconstruction_mode', '?')}",
            f"min_infold={compat.get('min_infold_version', '?')}",
            f"debug_friendly={compat.get('debug_friendly', False)}",
        ]

        out = {
            "path": str(archive),
            "manifest": manifest,
            "fold_profile": manifest.get("fold_profile"),
            "fold_profile_mode": manifest.get("fold_profile_mode"),
            "fold_profile_reason": manifest.get("fold_profile_reason"),
            "fold_profile_factors": report.get("fold_profile_factors"),
            "package_summary": {
                "file_count": manifest.get("file_count", 0),
                "fold_count": manifest.get("fold_count", 0),
                "logical_gain_bytes": manifest.get("logical_gain_bytes", 0),
                "physical_folded_size_bytes": manifest.get("physical_folded_size_bytes", 0),
                "original_size_bytes": manifest.get("original_size_bytes", 0),
                "source_path": manifest.get("source_path", ""),
                "created": manifest.get("created", ""),
            },
            "fold_counts_by_operator": fold_by_op,
            "gain_by_operator": gain_by_op,
            "biggest_gain_contributors": gain_contributors,
            "rejected_candidates_summary": report.get("rejected_candidates_summary", []),
            "template_rejected_families": report.get("template_rejected_families", []),
            "template_thresholds": report.get("template_thresholds", {}),
            "symbol_table_conflict_blocked": report.get("symbol_table_conflict_blocked", []),
            "reconstruction_guarantees": reconstruction_guarantees,
            "template_families": report.get("template_families", []),
            "duplicate_families": report.get("duplicate_families", []),
            "hierarchy_templates": report.get("hierarchy_templates", []),
            "dependency_motifs": report.get("dependency_motifs", []),
            "byte_fold_families": report.get("byte_fold_families", []),
            "metadata_table_fold_metrics": report.get("metadata_table_fold_metrics"),
            "metadata_table_fold": manifest.get("metadata_table_fold", False),
            "fold_species": manifest.get("fold_species"),
            "fold_species_mode": manifest.get("fold_species_mode"),
            "fold_species_reason": manifest.get("fold_species_reason"),
            "fold_creature_traits_final": manifest.get("fold_creature_traits_final"),
            "fold_creature_adapt_reasons": manifest.get("fold_creature_adapt_reasons"),
        }
    # Phase 10: Tesseract multi-dimensional summary (after with block; archive path still valid)
    try:
        from infold.tesseract import build_tesseract_signatures_from_archive
        tess_list = build_tesseract_signatures_from_archive(archive, lineage_tracking=None)
        by_dim: dict[str, list[str]] = {"structure": [], "byte": [], "metadata": [], "time": []}
        for t in tess_list:
            for d in t.get("dimensions_present", []):
                if d in by_dim:
                    by_dim[d].append(t.get("operator", "?"))
        out["tesseract_families_by_dimension"] = {k: sorted(set(v)) for k, v in by_dim.items() if v}
        out["tesseract_family_count"] = len(tess_list)
    except Exception:
        out["tesseract_families_by_dimension"] = {}
        out["tesseract_family_count"] = 0
    tp = report.get("tesseract_planner")
    if not tp and manifest:
        route = manifest.get("tesseract_planner_route")
        if route:
            tp = {
                "route": route,
                "route_reason": manifest.get("tesseract_planner_route_reason", ""),
                "dominant_dimension": manifest.get("tesseract_planner_dominant", "?"),
                "operator_family_priority": manifest.get("tesseract_planner_operator_priority", []),
                "planner_bias": {},
            }
    out["tesseract_planner"] = tp
    return out


def list_archive(archive_path: Path | str) -> dict[str, Any]:
    """
    List shared artifacts and fold families by operator.
    Deterministic, readable output.
    """
    archive = Path(archive_path).resolve()
    if not archive.exists():
        raise FileNotFoundError(f"Archive not found: {archive}")
    import tempfile
    with tempfile.TemporaryDirectory(prefix="infold_") as tmp:
        with zipfile.ZipFile(archive, "r") as zf:
            zf.extractall(tmp)
        root = _extract_root(Path(tmp))
        shared_dir = root / "shared"
        maps_data = json.loads((root / "maps" / "reconstruction.json").read_text(encoding="utf-8"))
        records = maps_data.get("records", [])
        from infold.engine.metadata_table_fold import load_path_table
        _pt = load_path_table(root)

        def _t(rec: dict) -> list:
            if _pt is not None and "targets_refs" in rec:
                return [_pt[i] for i in rec["targets_refs"] if i < len(_pt)]
            return rec.get("targets", [])

        shared_artifacts: list[dict[str, Any]] = []
        if shared_dir.exists():
            for f in sorted(shared_dir.iterdir()):
                if f.is_file():
                    shared_artifacts.append({"path": f"shared/{f.name}", "size": f.stat().st_size})
            chunks_dir = shared_dir / "chunks"
            if chunks_dir.exists():
                for cf in sorted(chunks_dir.iterdir()):
                    if cf.is_file():
                        shared_artifacts.append({"path": f"shared/chunks/{cf.name}", "size": cf.stat().st_size})
            mt_dir = shared_dir / "metadata_tables"
            if mt_dir.exists():
                for mf in sorted(mt_dir.iterdir()):
                    if mf.is_file():
                        shared_artifacts.append({"path": f"shared/metadata_tables/{mf.name}", "size": mf.stat().st_size})

        families_by_op: dict[str, list[dict[str, Any]]] = {}
        for i, rec in enumerate(records):
            op = rec.get("operator_id", "unknown")
            targets = _t(rec)
            gain = rec.get("gain", 0)
            if op not in families_by_op:
                families_by_op[op] = []
            fam: dict[str, Any] = {"index": i, "gain": gain, "target_count": len(targets)}
            if op == "exact_repetition":
                fam["paths"] = targets[:5]
                if len(targets) > 5:
                    fam["paths"].append(f"... +{len(targets) - 5} more")
            elif op == "template_skeleton":
                tmpl = root / "shared" / f"template_{i}.json"
                if tmpl.exists():
                    data = json.loads(tmpl.read_text(encoding="utf-8"))
                    fam["paths"] = data.get("paths", targets)[:5]
                    if len(targets) > 5:
                        fam["paths"].append(f"... +{len(targets) - 5} more")
                else:
                    fam["paths"] = targets[:5]
            elif op in ("hierarchy_mirror", "dependency_motif"):
                hj = root / "shared" / (f"hierarchy_{i}.json" if op == "hierarchy_mirror" else f"dependency_motif_{i}.json")
                if hj.exists():
                    data = json.loads(hj.read_text(encoding="utf-8"))
                    roots = data.get("roots", data.get("paths", []))
                    fam["roots_or_paths"] = roots[:5] if isinstance(roots, list) else list(roots)[:5]
                else:
                    fam["paths"] = targets[:5]
            else:
                fam["paths"] = targets[:5]
            families_by_op[op].append(fam)

        manifest = {}
        if (root / "manifest.json").exists():
            manifest = json.loads((root / "manifest.json").read_text(encoding="utf-8"))
        return {
            "path": str(archive),
            "manifest": manifest,
            "fold_profile": manifest.get("fold_profile"),
            "fold_profile_mode": manifest.get("fold_profile_mode"),
            "shared_artifacts": shared_artifacts,
            "families_by_operator": families_by_op,
        }


# Family type -> operator_id mapping for search
FAMILY_TO_OPERATOR = {
    "duplicate": "exact_repetition",
    "template": "template_skeleton",
    "symbol": "symbol_table",
    "hierarchy": "hierarchy_mirror",
    "dependency": "dependency_motif",
    "byte_fold": "byte_fold",
}

OPERATOR_TO_FAMILY = {v: k for k, v in FAMILY_TO_OPERATOR.items()}


def _load_byte_fold_metrics_from_archive(root: Path) -> dict[str, Any]:
    """Load byte_fold metrics from report.json. Returns dict with chunk metrics or empty."""
    report_path = root / "reports" / "report.json"
    if not report_path.exists():
        return {}
    report = json.loads(report_path.read_text(encoding="utf-8"))
    bfm = report.get("byte_fold_metrics") or {}
    if not bfm:
        return {}
    crr = bfm.get("chunk_reuse_ratio")
    avg_crr = sum(crr) / len(crr) if isinstance(crr, list) and len(crr) > 0 else (crr if isinstance(crr, (int, float)) else None)
    return {
        "files_chunk_folded": bfm.get("files_chunk_folded"),
        "unique_chunk_count": bfm.get("unique_chunk_count"),
        "reused_chunk_count": bfm.get("reused_chunk_count"),
        "chunk_reused_bytes": bfm.get("chunk_reused_bytes"),
        "chunk_reuse_ratio": avg_crr,
        "chunk_dictionary_size_bytes": bfm.get("chunk_dictionary_size_bytes"),
        "net_bytes_saved": bfm.get("net_bytes_saved"),
    }


def _add_match_explain(m: dict[str, Any], query: dict[str, Any]) -> str:
    """Build human-readable explanation of why a match was included. Deterministic."""
    parts: list[str] = []
    mt = m.get("match_type", "fold")
    if mt == "fold":
        parts.append("fold match")
        if query.get("operator"):
            parts.append(f"operator={m.get('operator_id', '?')}")
        if query.get("path"):
            parts.append("path filter matched")
        if query.get("min_gain") is not None and m.get("gain") is not None:
            parts.append(f"gain={m.get('gain')}>=min")
        if query.get("max_gain") is not None and m.get("gain") is not None:
            parts.append(f"gain={m.get('gain')}<=max")
        if query.get("min_target_count") is not None:
            parts.append(f"targets={m.get('target_count')}>=min")
        if query.get("max_target_count") is not None:
            parts.append(f"targets={m.get('target_count')}<=max")
        if m.get("operator_id") == "byte_fold":
            cm = m.get("chunk_metrics") or {}
            if cm:
                parts.append(f"chunk_reused={cm.get('chunk_reused_bytes', '?')} unique={cm.get('unique_chunk_count', '?')}")
                if cm.get("chunk_reuse_ratio") is not None:
                    parts.append(f"reuse_ratio={cm['chunk_reuse_ratio']:.2f}")
                if cm.get("chunk_dictionary_size_bytes") is not None:
                    parts.append(f"dict_overhead={cm['chunk_dictionary_size_bytes']} net={cm.get('net_bytes_saved', '?')}")
    else:
        parts.append(f"{mt} diagnostic")
        parts.append(f"operator={m.get('operator_id', '?')}")
        if m.get("planner_decision"):
            parts.append(f"planner_decision={m['planner_decision']}")
        if m.get("reason"):
            parts.append(f"reason={m['reason']}")
    return "; ".join(parts)


def _group_matches(matches: list[dict[str, Any]], group_by: str) -> dict[str, Any]:
    """Group matches by archive, operator, or family. Deterministic."""
    if not group_by or not matches:
        return {}
    key = (group_by or "").lower()
    groups: dict[str, list[dict[str, Any]]] = {}
    for m in matches:
        if key == "archive":
            k = m.get("archive_path", "?")
        elif key == "operator":
            k = m.get("operator_id", "?")
        elif key == "family":
            k = OPERATOR_TO_FAMILY.get(m.get("operator_id", ""), m.get("operator_id", "?"))
        else:
            k = "?"
        if k not in groups:
            groups[k] = []
        groups[k].append(m)
    return dict(sorted(groups.items()))


def _load_diagnostics_from_archive(
    root: Path,
    archive_path: str,
    *,
    rejected: bool = False,
    blocked: bool = False,
    superseded: bool = False,
    planner_decision: str | None = None,
) -> list[dict[str, Any]]:
    """
    Load diagnostic entries from report.json. Returns match-like dicts with
    match_type, operator_id, planner_decision, reason, detail, target_count, archive_path.
    """
    report_path = root / "reports" / "report.json"
    if not report_path.exists():
        return []
    report = json.loads(report_path.read_text(encoding="utf-8"))
    out: list[dict[str, Any]] = []

    if rejected:
        for i, rc in enumerate(report.get("rejected_candidates_summary", [])):
            if planner_decision and rc.get("planner_decision") != planner_decision:
                continue
            out.append({
                "match_type": "rejected",
                "index": i,
                "operator_id": rc.get("operator_id", "unknown"),
                "planner_decision": rc.get("planner_decision"),
                "reason": rc.get("reason", ""),
                "detail": rc.get("detail", ""),
                "target_count": rc.get("target_count", 0),
                "archive_path": archive_path,
            })

    if blocked:
        diag = report.get("interaction_diagnostics") or {}
        for i, b in enumerate(diag.get("blocked_folds", [])):
            if planner_decision and b.get("planner_decision") != planner_decision:
                continue
            out.append({
                "match_type": "blocked",
                "index": i,
                "operator_id": b.get("operator_id", "unknown"),
                "planner_decision": b.get("planner_decision"),
                "reason": b.get("reason", ""),
                "detail": b.get("detail", ""),
                "target_count": b.get("target_count", 0),
                "archive_path": archive_path,
            })

    if superseded:
        diag = report.get("interaction_diagnostics") or {}
        for i, s in enumerate(diag.get("superseded_folds", [])):
            if planner_decision and s.get("planner_decision") != planner_decision:
                continue
            out.append({
                "match_type": "superseded",
                "index": i,
                "operator_id": s.get("operator_id", "unknown"),
                "planner_decision": s.get("planner_decision"),
                "reason": s.get("reason", ""),
                "detail": s.get("detail", ""),
                "target_count": s.get("target_count", 0),
                "archive_path": archive_path,
            })

    return out


def _apply_post_filters(
    matches: list[dict[str, Any]],
    *,
    min_gain: int | None = None,
    max_gain: int | None = None,
    min_target_count: int | None = None,
    max_target_count: int | None = None,
    min_chunk_reused_bytes: int | None = None,
    max_chunk_reused_bytes: int | None = None,
    min_chunk_reuse_ratio: float | None = None,
    max_chunk_reuse_ratio: float | None = None,
    min_files_chunk_folded: int | None = None,
    max_files_chunk_folded: int | None = None,
) -> list[dict[str, Any]]:
    """Apply numeric range filters. Gain filters apply only to matches with gain (folds). Deterministic."""
    out: list[dict[str, Any]] = []
    for m in matches:
        gain = m.get("gain")
        tc = m.get("target_count", 0)
        has_gain = gain is not None
        if has_gain and min_gain is not None and gain < min_gain:
            continue
        if has_gain and max_gain is not None and gain > max_gain:
            continue
        if min_target_count is not None and tc < min_target_count:
            continue
        if max_target_count is not None and tc > max_target_count:
            continue
        if m.get("operator_id") == "byte_fold":
            cm = m.get("chunk_metrics") or {}
            crb = cm.get("chunk_reused_bytes")
            if crb is not None:
                if min_chunk_reused_bytes is not None and crb < min_chunk_reused_bytes:
                    continue
                if max_chunk_reused_bytes is not None and crb > max_chunk_reused_bytes:
                    continue
            crr = cm.get("chunk_reuse_ratio")
            if crr is not None:
                if min_chunk_reuse_ratio is not None and crr < min_chunk_reuse_ratio:
                    continue
                if max_chunk_reuse_ratio is not None and crr > max_chunk_reuse_ratio:
                    continue
            fcf = cm.get("files_chunk_folded") or tc
            if min_files_chunk_folded is not None and fcf < min_files_chunk_folded:
                continue
            if max_files_chunk_folded is not None and fcf > max_files_chunk_folded:
                continue
        out.append(m)
    return out


def _sort_matches(
    matches: list[dict[str, Any]],
    sort_by: str | None,
) -> list[dict[str, Any]]:
    """Sort matches deterministically. Keys: gain, operator, archive, target_count."""
    if not sort_by or not matches:
        return matches
    key = (sort_by or "").lower()
    # Secondary sort by index then archive for stability
    def keyfn(m: dict[str, Any]) -> tuple:
        if key == "gain":
            return (-m.get("gain", 0), m.get("archive_path", ""), m.get("index", 0))
        if key == "operator":
            return (m.get("operator_id", ""), m.get("archive_path", ""), m.get("index", 0))
        if key == "archive":
            return (m.get("archive_path", ""), m.get("index", 0))
        if key == "target_count":
            return (-m.get("target_count", 0), m.get("archive_path", ""), m.get("index", 0))
        return (m.get("archive_path", ""), m.get("index", 0))
    return sorted(matches, key=keyfn)


def _build_search_summary(
    matches: list[dict[str, Any]],
    archives_searched: int,
    *,
    include_tops: bool = True,
) -> dict[str, Any]:
    """Build search summary: total matches, by operator, by family, top archives/operators/families."""
    by_op: dict[str, int] = {}
    by_family: dict[str, int] = {}
    by_archive: dict[str, int] = {}  # gain sum per archive
    for m in matches:
        op = m.get("operator_id", "unknown")
        by_op[op] = by_op.get(op, 0) + 1
        fam = OPERATOR_TO_FAMILY.get(op, op)
        by_family[fam] = by_family.get(fam, 0) + 1
        arch = m.get("archive_path", "")
        if arch:
            by_archive[arch] = by_archive.get(arch, 0) + m.get("gain", 0)

    summary: dict[str, Any] = {
        "total_archives_searched": archives_searched,
        "total_matches": len(matches),
        "matches_by_operator": dict(sorted(by_op.items())),
        "matches_by_family": dict(sorted(by_family.items())),
    }
    if include_tops and matches:
        if any(m.get("gain") for m in matches):
            summary["top_archives_by_gain"] = sorted(
                [{"archive_path": k, "total_gain": v} for k, v in by_archive.items()],
                key=lambda x: -x["total_gain"],
            )[:10]
        summary["top_operators"] = sorted(
            [{"operator_id": k, "count": v} for k, v in by_op.items()],
            key=lambda x: -x["count"],
        )[:10]
        summary["top_families"] = sorted(
            [{"family": k, "count": v} for k, v in by_family.items()],
            key=lambda x: -x["count"],
        )[:10]
    return summary


def search_archive(
    archive_path: Path | str,
    *,
    operator: str | None = None,
    path: str | None = None,
    family: str | None = None,
    family_id: int | None = None,
    artifact_id: int | None = None,
    min_gain: int | None = None,
    max_gain: int | None = None,
    min_target_count: int | None = None,
    max_target_count: int | None = None,
    min_chunk_reused_bytes: int | None = None,
    max_chunk_reused_bytes: int | None = None,
    min_chunk_reuse_ratio: float | None = None,
    max_chunk_reuse_ratio: float | None = None,
    min_files_chunk_folded: int | None = None,
    max_files_chunk_folded: int | None = None,
    sort_by: str | None = None,
    rejected: bool = False,
    blocked: bool = False,
    superseded: bool = False,
    planner_decision: str | None = None,
    diagnostics_only: bool = False,
    group_by: str | None = None,
    explain: bool = False,
    with_tesseract: bool = False,
) -> dict[str, Any]:
    """
    Search within one archive. Deterministic, archive-focused.
    Filters: operator, path, family, family_id, artifact_id, min/max gain, min/max target_count.
    When rejected/blocked/superseded: include diagnostic entries. diagnostics_only: skip folds.
    Returns matching records with metadata. Each match includes archive_path.
    """
    archive = Path(archive_path).resolve()
    if not archive.exists():
        raise FileNotFoundError(f"Archive not found: {archive}")
    import tempfile
    with tempfile.TemporaryDirectory(prefix="infold_") as tmp:
        with zipfile.ZipFile(archive, "r") as zf:
            zf.extractall(tmp)
        root = _extract_root(Path(tmp))
        archive_str = str(archive)

        # Resolve family -> operator
        op_filter = operator
        if family:
            op_filter = FAMILY_TO_OPERATOR.get(family.lower(), family)

        matches: list[dict[str, Any]] = []
        path_lower = (path or "").lower().replace("\\", "/")

        if not diagnostics_only:
            maps_data = json.loads((root / "maps" / "reconstruction.json").read_text(encoding="utf-8"))
            records = maps_data.get("records", [])
            from infold.engine.metadata_table_fold import load_path_table
            _path_table = load_path_table(root)

            def _targets(rec: dict) -> list:
                if _path_table is not None and "targets_refs" in rec:
                    return [_path_table[i] for i in rec["targets_refs"] if i < len(_path_table)]
                return rec.get("targets", [])

            for i, rec in enumerate(records):
                op = rec.get("operator_id", "unknown")
                targets = _targets(rec)
                gain = rec.get("gain", 0)

                # Apply filters
                if op_filter and op != op_filter:
                    continue
                if family_id is not None and i != family_id:
                    continue
                if artifact_id is not None and i != artifact_id:
                    continue
                if path_lower:
                    targets_norm = [str(t).replace("\\", "/").lower() for t in targets]
                    if not any(path_lower in t or t in path_lower for t in targets_norm):
                        if op == "hierarchy_mirror" and (root / "shared" / f"hierarchy_{i}.json").exists():
                            data = json.loads((root / "shared" / f"hierarchy_{i}.json").read_text(encoding="utf-8"))
                            roots = data.get("roots", data.get("paths", []))
                            if isinstance(roots, list):
                                roots_norm = [str(r).replace("\\", "/").lower() for r in roots]
                                if not any(path_lower in r or r in path_lower for r in roots_norm):
                                    continue
                            else:
                                continue
                        elif op == "dependency_motif" and (root / "shared" / f"dependency_motif_{i}.json").exists():
                            data = json.loads((root / "shared" / f"dependency_motif_{i}.json").read_text(encoding="utf-8"))
                            paths = data.get("paths", targets)
                            paths_norm = [str(p).replace("\\", "/").lower() for p in paths]
                            if not any(path_lower in p or p in path_lower for p in paths_norm):
                                continue
                        else:
                            continue

                # Build match entry (include archive_path for schema consistency)
                entry: dict[str, Any] = {
                    "match_type": "fold",
                    "index": i,
                    "operator_id": op,
                    "artifact_id": i,
                    "gain": gain,
                    "target_count": len(targets),
                    "paths": [str(t) for t in targets],
                    "archive_path": archive_str,
                }
                if op == "template_skeleton":
                    tmpl = root / "shared" / f"template_{i}.json"
                    if tmpl.exists():
                        data = json.loads(tmpl.read_text(encoding="utf-8"))
                        entry["paths"] = data.get("paths", [str(t) for t in targets])
                elif op in ("hierarchy_mirror", "dependency_motif"):
                    hj = root / "shared" / (f"hierarchy_{i}.json" if op == "hierarchy_mirror" else f"dependency_motif_{i}.json")
                    if hj.exists():
                        data = json.loads(hj.read_text(encoding="utf-8"))
                        entry["roots_or_paths"] = data.get("roots", data.get("paths", [str(t) for t in targets]))
                matches.append(entry)

        # Add diagnostics when requested
        if rejected or blocked or superseded:
            diag_matches = _load_diagnostics_from_archive(
                root, archive_str,
                rejected=rejected,
                blocked=blocked,
                superseded=superseded,
                planner_decision=planner_decision,
            )
            # Filter diagnostics by operator/family
            if op_filter:
                diag_matches = [d for d in diag_matches if d.get("operator_id") == op_filter]
            matches.extend(diag_matches)

        # Enrich byte_fold matches with chunk metrics from report
        bfm = _load_byte_fold_metrics_from_archive(root)
        if bfm:
            for m in matches:
                if m.get("operator_id") == "byte_fold":
                    m["chunk_metrics"] = dict(bfm)
                    m["chunk_metrics"]["files_chunk_folded"] = m.get("target_count", bfm.get("files_chunk_folded"))

        # Enrich with Tesseract when requested (Phase 10)
        if with_tesseract:
            from infold.tesseract import build_tesseract_signatures_from_archive, get_tesseract_for_match, tesseract_summary
            tess_list = build_tesseract_signatures_from_archive(archive, lineage_tracking=None)
            for m in matches:
                if m.get("match_type") != "fold":
                    continue
                tess = get_tesseract_for_match(m, tess_list)
                if tess:
                    m["tesseract"] = tess
                    m["dimensions_present"] = tess.get("dimensions_present", [])
                    m["tesseract_summary"] = tesseract_summary(tess)

    matches = _apply_post_filters(
        matches,
        min_gain=min_gain,
        max_gain=max_gain,
        min_target_count=min_target_count,
        max_target_count=max_target_count,
        min_chunk_reused_bytes=min_chunk_reused_bytes,
        max_chunk_reused_bytes=max_chunk_reused_bytes,
        min_chunk_reuse_ratio=min_chunk_reuse_ratio,
        max_chunk_reuse_ratio=max_chunk_reuse_ratio,
        min_files_chunk_folded=min_files_chunk_folded,
        max_files_chunk_folded=max_files_chunk_folded,
    )
    matches = _sort_matches(matches, sort_by)

    query = {
        "operator": operator,
        "path": path,
        "family": family,
        "family_id": family_id,
        "artifact_id": artifact_id,
        "min_gain": min_gain,
        "max_gain": max_gain,
        "min_target_count": min_target_count,
        "max_target_count": max_target_count,
        "min_chunk_reused_bytes": min_chunk_reused_bytes,
        "max_chunk_reused_bytes": max_chunk_reused_bytes,
        "min_chunk_reuse_ratio": min_chunk_reuse_ratio,
        "max_chunk_reuse_ratio": max_chunk_reuse_ratio,
        "min_files_chunk_folded": min_files_chunk_folded,
        "max_files_chunk_folded": max_files_chunk_folded,
        "sort_by": sort_by,
        "rejected": rejected,
        "blocked": blocked,
        "superseded": superseded,
        "planner_decision": planner_decision,
        "diagnostics_only": diagnostics_only,
        "group_by": group_by,
        "explain": explain,
        "with_tesseract": with_tesseract,
    }
    if explain:
        for m in matches:
            m["match_explain"] = _add_match_explain(m, query)
    summary = _build_search_summary(matches, archives_searched=1)
    out: dict[str, Any] = {
        "path": archive_str,
        "query": query,
        "match_count": len(matches),
        "matches": matches,
        "summary": summary,
    }
    if group_by:
        out["groups"] = _group_matches(matches, group_by)
    return out


def search_archives(
    archive_paths: list[Path | str],
    *,
    operator: str | None = None,
    path: str | None = None,
    family: str | None = None,
    family_id: int | None = None,
    artifact_id: int | None = None,
    min_gain: int | None = None,
    max_gain: int | None = None,
    min_target_count: int | None = None,
    max_target_count: int | None = None,
    archive_filter: str | None = None,
    sort_by: str | None = None,
    rejected: bool = False,
    blocked: bool = False,
    superseded: bool = False,
    planner_decision: str | None = None,
    diagnostics_only: bool = False,
    spec_version: str | None = None,
    reconstruction_mode: str | None = None,
    source_path: str | None = None,
    debug_friendly: bool | None = None,
    created_after: str | None = None,
    created_before: str | None = None,
    min_logical_gain: int | None = None,
    max_logical_gain: int | None = None,
    min_physical_size: int | None = None,
    max_physical_size: int | None = None,
    min_rejection_count: int | None = None,
    max_rejection_count: int | None = None,
    min_fold_count: int | None = None,
    max_fold_count: int | None = None,
    min_chunk_reused_bytes: int | None = None,
    max_chunk_reused_bytes: int | None = None,
    min_chunk_reuse_ratio: float | None = None,
    max_chunk_reuse_ratio: float | None = None,
    min_files_chunk_folded: int | None = None,
    max_files_chunk_folded: int | None = None,
    group_by: str | None = None,
    explain: bool = False,
    with_tesseract: bool = False,
) -> dict[str, Any]:
    """
    Search across multiple archives. Aggregates matches, deterministic.
    archive_filter: substring match in archive path (case-insensitive).
    Metadata/metric filters apply to archive-level before searching.
    Each match includes archive_path.
    """
    import tempfile
    archive_filter_lower = (archive_filter or "").lower()
    meta_filters = {
        "spec_version": spec_version,
        "reconstruction_mode": reconstruction_mode,
        "source_path": source_path,
        "debug_friendly": debug_friendly,
        "created_after": created_after,
        "created_before": created_before,
    }
    metric_filters = {
        "min_logical_gain": min_logical_gain,
        "max_logical_gain": max_logical_gain,
        "min_physical_size": min_physical_size,
        "max_physical_size": max_physical_size,
        "min_rejection_count": min_rejection_count,
        "max_rejection_count": max_rejection_count,
        "min_fold_count": min_fold_count,
        "max_fold_count": max_fold_count,
    }
    all_matches: list[dict[str, Any]] = []
    searched: list[str] = []

    for ap in sorted(str(p) for p in archive_paths):
        arch = Path(ap).resolve()
        if not arch.exists():
            continue
        if archive_filter_lower and archive_filter_lower not in str(arch).lower():
            continue
        # Apply metadata/metric filters (need to peek inside archive)
        if meta_filters.get("spec_version") or meta_filters.get("reconstruction_mode") or meta_filters.get("source_path") or meta_filters.get("debug_friendly") is not None or meta_filters.get("created_after") or meta_filters.get("created_before") or any(v is not None for v in metric_filters.values()):
            with tempfile.TemporaryDirectory(prefix="infold_") as tmp:
                with zipfile.ZipFile(arch, "r") as zf:
                    zf.extractall(tmp)
                root = _extract_root(Path(tmp))
                meta = _load_archive_metadata(root)
                if not _archive_passes_metadata_filters(meta, meta_filters):
                    continue
                if not _archive_passes_metric_filters(meta, metric_filters):
                    continue
        searched.append(str(arch))
        r = search_archive(
            arch,
            operator=operator,
            path=path,
            family=family,
            family_id=family_id,
            artifact_id=artifact_id,
            min_gain=min_gain,
            max_gain=max_gain,
            min_target_count=min_target_count,
            max_target_count=max_target_count,
            min_chunk_reused_bytes=min_chunk_reused_bytes,
            max_chunk_reused_bytes=max_chunk_reused_bytes,
            min_chunk_reuse_ratio=min_chunk_reuse_ratio,
            max_chunk_reuse_ratio=max_chunk_reuse_ratio,
            min_files_chunk_folded=min_files_chunk_folded,
            max_files_chunk_folded=max_files_chunk_folded,
            sort_by=None,
            rejected=rejected,
            blocked=blocked,
            superseded=superseded,
            planner_decision=planner_decision,
            diagnostics_only=diagnostics_only,
            explain=explain,
            with_tesseract=with_tesseract,
        )
        for m in r.get("matches", []):
            m["archive_path"] = str(arch)
            all_matches.append(m)

    all_matches = _sort_matches(all_matches, sort_by)
    query = {
        "operator": operator,
        "path": path,
        "family": family,
        "family_id": family_id,
        "artifact_id": artifact_id,
        "min_gain": min_gain,
        "max_gain": max_gain,
        "min_target_count": min_target_count,
        "max_target_count": max_target_count,
        "archive_filter": archive_filter,
        "sort_by": sort_by,
        "rejected": rejected,
        "blocked": blocked,
        "superseded": superseded,
        "planner_decision": planner_decision,
        "spec_version": spec_version,
        "reconstruction_mode": reconstruction_mode,
        "source_path": source_path,
        "min_logical_gain": min_logical_gain,
        "max_logical_gain": max_logical_gain,
        "min_physical_size": min_physical_size,
        "max_physical_size": max_physical_size,
        "min_rejection_count": min_rejection_count,
        "max_rejection_count": max_rejection_count,
        "min_fold_count": min_fold_count,
        "max_fold_count": max_fold_count,
        "min_chunk_reused_bytes": min_chunk_reused_bytes,
        "max_chunk_reused_bytes": max_chunk_reused_bytes,
        "min_chunk_reuse_ratio": min_chunk_reuse_ratio,
        "max_chunk_reuse_ratio": max_chunk_reuse_ratio,
        "min_files_chunk_folded": min_files_chunk_folded,
        "max_files_chunk_folded": max_files_chunk_folded,
        "group_by": group_by,
        "explain": explain,
        "with_tesseract": with_tesseract,
    }
    if explain:
        for m in all_matches:
            m["match_explain"] = _add_match_explain(m, query)
    summary = _build_search_summary(all_matches, archives_searched=len(searched))
    out: dict[str, Any] = {
        "paths": searched,
        "query": query,
        "match_count": len(all_matches),
        "matches": all_matches,
        "summary": summary,
    }
    if group_by:
        out["groups"] = _group_matches(all_matches, group_by)
    return out


def search_to_text(result: dict[str, Any]) -> str:
    """Human-readable search output. Supports single-archive and multi-archive results."""
    lines = [
        "Archive Search",
        "==============",
        "",
    ]
    path_or_paths = result.get("path") or result.get("paths", [])
    if isinstance(path_or_paths, list):
        lines.append(f"Archives: {len(path_or_paths)} searched")
        for p in path_or_paths[:5]:
            lines.append(f"  - {p}")
        if len(path_or_paths) > 5:
            lines.append(f"  ... +{len(path_or_paths) - 5} more")
    else:
        lines.append(f"Archive: {path_or_paths}")
    lines.append("")
    summary = result.get("summary", {})
    if summary:
        lines.append(f"Summary: {summary.get('total_matches', 0)} matches from {summary.get('total_archives_searched', 0)} archives")
        if summary.get("matches_by_operator"):
            lines.append(f"  By operator: {summary['matches_by_operator']}")
        if summary.get("top_archives_by_gain"):
            lines.append("  Top archives by gain:")
            for t in summary["top_archives_by_gain"][:3]:
                lines.append(f"    {t.get('archive_path', '?')}: {t.get('total_gain', 0)}")
        lines.append("")
    lines.append(f"Query: {result.get('query', {})}")
    lines.append(f"Matches: {result.get('match_count', 0)}")
    lines.append("")
    # Grouped output
    groups = result.get("groups", {})
    group_by = result.get("query", {}).get("group_by", "")
    if groups:
        for group_key, group_matches in groups.items():
            lines.append(f"--- {group_by or '?'} = {group_key} ({len(group_matches)} matches) ---")
            for m in group_matches[:10]:
                _append_match_line(lines, m, result)
            if len(group_matches) > 10:
                lines.append(f"  ... +{len(group_matches) - 10} more")
            lines.append("")
    else:
        for m in result.get("matches", []):
            _append_match_line(lines, m, result)
    return "\n".join(lines).rstrip()


def _append_match_line(lines: list[str], m: dict[str, Any], result: dict[str, Any]) -> None:
    """Append one match to text output lines."""
    mt = m.get("match_type", "fold")
    arch = m.get("archive_path", "")
    arch_part = f" [{arch}]" if arch and (result.get("paths") or len(result.get("matches", [])) > 1) else ""
    if mt == "fold":
        lines.append(f"[{m.get('index', '?')}] {m.get('operator_id', '?')} gain={m.get('gain', 0)} targets={m.get('target_count', 0)}{arch_part}")
        for p in m.get("paths", [])[:5]:
            lines.append(f"  - {p}")
        if len(m.get("paths", [])) > 5:
            lines.append(f"  ... +{len(m['paths']) - 5} more")
        if m.get("roots_or_paths"):
            lines.append(f"  roots: {m['roots_or_paths'][:3]}")
    else:
        lines.append(f"[{mt}] {m.get('operator_id', '?')} {m.get('planner_decision', m.get('reason', ''))} targets={m.get('target_count', 0)}{arch_part}")
        if m.get("detail"):
            lines.append(f"  detail: {str(m['detail'])[:80]}")
    if m.get("match_explain"):
        lines.append(f"  explain: {m['match_explain']}")
    if m.get("tesseract_summary"):
        lines.append(f"  tesseract: {m['tesseract_summary']}")
    lines.append("")


def search_to_csv(result: dict[str, Any]) -> str:
    """Export search results as CSV. Deterministic."""
    import csv
    import io
    matches = result.get("matches", [])
    keys = ["match_type", "operator_id", "gain", "target_count", "archive_path", "paths", "planner_decision", "reason", "detail"]
    out = io.StringIO()
    w = csv.writer(out)
    w.writerow(keys)
    for m in matches:
        row = []
        for k in keys:
            v = m.get(k)
            if isinstance(v, list):
                v = ";".join(str(x) for x in v[:10])
            row.append(str(v) if v is not None else "")
        w.writerow(row)
    return out.getvalue()


def write_search_results(
    result: dict[str, Any],
    path: Path | str,
    *,
    format: str = "json",
) -> Path:
    """
    Write search results to file. format: json, csv, markdown.
    Returns the path written.
    """
    p = Path(path).resolve()
    p.parent.mkdir(parents=True, exist_ok=True)
    fmt = (format or "json").lower()
    if fmt == "csv":
        p.write_text(search_to_csv(result), encoding="utf-8")
    elif fmt == "markdown":
        p.write_text(search_to_markdown(result), encoding="utf-8")
    else:
        import json
        p.write_text(json.dumps(result, indent=2), encoding="utf-8")
    return p


def search_to_markdown(result: dict[str, Any]) -> str:
    """Export search results as Markdown summary. Deterministic."""
    lines = [
        "# Infold Search Results",
        "",
        f"**Matches:** {result.get('match_count', 0)}",
        f"**Archives searched:** {result.get('summary', {}).get('total_archives_searched', 0)}",
        "",
        "## Summary",
        "",
    ]
    s = result.get("summary", {})
    if s.get("matches_by_operator"):
        lines.append("### By operator")
        for op, cnt in s["matches_by_operator"].items():
            lines.append(f"- {op}: {cnt}")
        lines.append("")
    if s.get("top_archives_by_gain"):
        lines.append("### Top archives by gain")
        for t in s["top_archives_by_gain"][:10]:
            lines.append(f"- `{t.get('archive_path', '')}`: {t.get('total_gain', 0)} bytes")
        lines.append("")
    lines.append("## Matches")
    lines.append("")
    for m in result.get("matches", [])[:50]:
        mt = m.get("match_type", "fold")
        op = m.get("operator_id", "?")
        gain = m.get("gain", "-")
        tc = m.get("target_count", 0)
        arch = m.get("archive_path", "")
        lines.append(f"- **{mt}** {op} | gain={gain} targets={tc} | {arch}")
    if len(result.get("matches", [])) > 50:
        lines.append(f"- ... and {len(result['matches']) - 50} more")
    return "\n".join(lines)


def stats_archive(archive_path: Path | str) -> dict[str, Any]:
    """
    Detailed archive metrics: logical gain, physical folded size, per-operator contributions,
    rejection counts, template purity/slot ratios, hierarchy/dependency family metrics.
    """
    archive = Path(archive_path).resolve()
    if not archive.exists():
        raise FileNotFoundError(f"Archive not found: {archive}")
    info = explain_archive(archive)
    import tempfile
    with tempfile.TemporaryDirectory(prefix="infold_") as tmp:
        with zipfile.ZipFile(archive, "r") as zf:
            zf.extractall(tmp)
        root = _extract_root(Path(tmp))
        report = {}
        if (root / "reports" / "report.json").exists():
            report = json.loads((root / "reports" / "report.json").read_text(encoding="utf-8"))

    stats: dict[str, Any] = {
        "path": str(archive),
        "logical_gain_bytes": info["package_summary"]["logical_gain_bytes"],
        "physical_folded_size_bytes": info["package_summary"]["physical_folded_size_bytes"],
        "original_size_bytes": info["package_summary"]["original_size_bytes"],
        "per_operator_contributions": info["gain_by_operator"],
        "per_operator_gain_share": {},
        "rejection_count": len(info.get("rejected_candidates_summary", [])),
    }
    total = info["package_summary"]["logical_gain_bytes"]
    for op, g in info["gain_by_operator"].items():
        stats["per_operator_gain_share"][op] = round(g / total, 4) if total else 0

    tm = report.get("template_skeleton_metrics") or {}
    stats["template_metrics"] = {
        "families_found": tm.get("families_found", 0),
        "family_purity": tm.get("family_purity", []),
        "slot_ambiguity": tm.get("slot_ambiguity", []),
        "avg_slot_size": tm.get("avg_slot_size", []),
    } if tm else None

    hm = report.get("hierarchy_metrics") or {}
    stats["hierarchy_metrics"] = {
        "templates_found": hm.get("hierarchy_templates_found", 0),
        "instances_per_template": hm.get("instances_per_template", []),
        "structural_reuse_ratio": hm.get("structural_reuse_ratio", []),
    } if hm else None

    dm = report.get("dependency_metrics") or {}
    stats["dependency_metrics"] = {
        "motifs_found": dm.get("dependency_motifs_found", 0),
        "instances_per_family": dm.get("motif_instances_per_family", []),
        "average_motif_size": dm.get("average_motif_size", []),
    } if dm else None

    mtf = report.get("metadata_table_fold_metrics") or {}
    stats["metadata_table_fold_metrics"] = mtf if mtf else None

    return stats


def inspect_archive(archive_path: Path | str) -> dict[str, Any]:
    """
    Inspect archive: read manifest, return summary.
    """
    archive = Path(archive_path).resolve()
    if not archive.exists():
        raise FileNotFoundError(f"Archive not found: {archive}")
    import tempfile
    with tempfile.TemporaryDirectory(prefix="infold_") as tmp:
        with zipfile.ZipFile(archive, "r") as zf:
            zf.extractall(tmp)
        root = Path(tmp)
        manifest_path = root / "manifest.json"
        if not manifest_path.exists():
            found = list(root.rglob("manifest.json"))
            manifest_path = found[0] if found else manifest_path
        if not manifest_path.exists():
            raise ValueError("Invalid archive: missing manifest.json")
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        return {
            "path": str(archive),
            "manifest": manifest,
            "file_count": manifest.get("file_count", 0),
            "fold_count": manifest.get("fold_count", 0),
            "logical_gain_bytes": manifest.get("logical_gain_bytes", 0),
            "physical_folded_size_bytes": manifest.get("physical_folded_size_bytes", 0),
            "source_path": manifest.get("source_path", ""),
            "created": manifest.get("created", ""),
        }


VALIDATION_MODES = ("basic", "strict", "integrity-only", "schema-only")


def validate_archive(
    archive_path: Path | str,
    mode: str = "strict",
) -> tuple[bool, list[str]]:
    """
    Validate archive. Modes:
    - basic: required files/dirs, manifest required keys
    - strict: basic + ledger consistency, maps, report, manifest-ledger, integrity
    - integrity-only: only integrity checksums
    - schema-only: manifest/ledger/integrity schema structure, no content checks
    Returns (ok, list of error messages). Deterministic and readable.
    """
    if mode not in VALIDATION_MODES:
        return False, [f"Invalid validation mode: {mode}"]
    errors: list[str] = []
    archive = Path(archive_path).resolve()
    if not archive.exists():
        return False, [f"Archive not found: {archive}"]
    try:
        with zipfile.ZipFile(archive, "r") as zf:
            names = set(zf.namelist())
    except zipfile.BadZipFile:
        return False, ["Invalid zip file"]
    root_prefix = ""
    for n in names:
        if "/" in n:
            root_prefix = n.split("/")[0] + "/"
            break

    def has_file(p: str) -> bool:
        return p in names or (root_prefix and (root_prefix + p) in names)

    def has_dir(d: str) -> bool:
        return any(n == d or n.startswith(d + "/") or n == root_prefix + d or n.startswith(root_prefix + d + "/") for n in names)

    if mode in ("basic", "strict", "schema-only"):
        for f in REQUIRED_FILES:
            if not has_file(f):
                errors.append(f"Missing required file: {f}")
        for d in REQUIRED_DIRS:
            if not has_dir(d):
                errors.append(f"Missing required directory: {d}/")
    if errors and mode != "integrity-only":
        return False, errors

    import tempfile
    with tempfile.TemporaryDirectory(prefix="infold_") as tmp:
        with zipfile.ZipFile(archive, "r") as zf:
            zf.extractall(tmp)
        root = _extract_root(Path(tmp))

        manifest_path = root / "manifest.json"
        if not manifest_path.exists():
            if mode != "integrity-only":
                return False, errors + ["Could not find manifest.json"]
            manifest = {}
        else:
            try:
                manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
            except json.JSONDecodeError as e:
                errors.append(f"Manifest invalid JSON: {e}")
                manifest = {}
        if mode in ("basic", "strict", "schema-only") and manifest:
            for key in MANIFEST_REQUIRED_KEYS:
                if key not in manifest:
                    errors.append(f"Manifest missing required key: {key}")
            compat = manifest.get("compatibility", {})
            if compat.get("reconstruction_mode") != "deterministic":
                errors.append("Compatibility: reconstruction_mode must be 'deterministic'")
            if mode == "schema-only":
                if not isinstance(manifest.get("fold_count"), (int, type(None))):
                    errors.append("Manifest: fold_count must be int")
                if not isinstance(manifest.get("logical_gain_bytes"), (int, type(None))):
                    errors.append("Manifest: logical_gain_bytes must be int")

        ledger_path = root / "ledger.json"
        if ledger_path.exists() and mode in ("strict", "schema-only"):
            try:
                ledger = json.loads(ledger_path.read_text(encoding="utf-8"))
            except json.JSONDecodeError as e:
                errors.append(f"Ledger invalid JSON: {e}")
                ledger = {}
            lr = ledger.get("fold_records", [])
            ltotal = ledger.get("total_folds", 0)
            lbytes = ledger.get("total_bytes_saved", 0)
            if mode == "strict":
                if len(lr) != ltotal:
                    errors.append(f"Ledger inconsistency: fold_records count {len(lr)} != total_folds {ltotal}")
                computed = sum(r.get("gain", 0) for r in lr)
                if computed != lbytes:
                    errors.append(f"Ledger inconsistency: sum(gain) {computed} != total_bytes_saved {lbytes}")
                consistency_errors = check_manifest_ledger_consistency(manifest, ledger)
                errors.extend(consistency_errors)
            logical_physical_errors = check_logical_vs_physical(manifest)
            errors.extend(logical_physical_errors)

        maps_path = root / "maps" / "reconstruction.json"
        if maps_path.exists() and mode == "strict":
            maps_data = json.loads(maps_path.read_text(encoding="utf-8"))
            if maps_data.get("path_table_ref"):
                pt_path = root / "shared" / "metadata_tables" / "path_table.json"
                if not pt_path.exists():
                    errors.append("path_table_ref set but shared/metadata_tables/path_table.json missing")
            records = maps_data.get("records", [])
            manifest_fold = manifest.get("fold_count", 0)
            if len(records) != manifest_fold:
                errors.append(f"Maps/manifest inconsistency: maps records {len(records)} != manifest fold_count {manifest_fold}")
            shared_dir = root / "shared"
            for i, rec in enumerate(records):
                op_id = rec.get("operator_id", "")
                if op_id == "exact_repetition":
                    if not (shared_dir / f"exact_{i}.txt").exists():
                        errors.append(f"Missing shared artifact: shared/exact_{i}.txt for record {i}")
                elif op_id == "template_skeleton":
                    if not (shared_dir / f"template_{i}.json").exists():
                        errors.append(f"Missing shared artifact: shared/template_{i}.json for record {i}")
                elif op_id == "symbol_table":
                    if not (shared_dir / f"symbols_{i}.json").exists():
                        errors.append(f"Missing shared artifact: shared/symbols_{i}.json for record {i}")
                elif op_id == "hierarchy_mirror":
                    if not (shared_dir / f"hierarchy_{i}.json").exists():
                        errors.append(f"Missing shared artifact: shared/hierarchy_{i}.json for record {i}")
                elif op_id == "dependency_motif":
                    if not (shared_dir / f"dependency_motif_{i}.json").exists():
                        errors.append(f"Missing shared artifact: shared/dependency_motif_{i}.json for record {i}")
            if records:
                indices = [r.get("index", -1) for r in records]
                for j, idx in enumerate(indices):
                    if idx != j:
                        errors.append(f"Maps integrity: record at position {j} has index {idx}, expected {j}")

        report_path = root / "reports" / "report.json"
        if report_path.exists() and mode == "strict":
            report = json.loads(report_path.read_text(encoding="utf-8"))
            rfold = report.get("fold_count", -1)
            mfold = manifest.get("fold_count", -1)
            if rfold != mfold:
                errors.append(f"Manifest/report inconsistency: report fold_count {rfold} != manifest {mfold}")
            rgain = report.get("logical_gain_bytes", report.get("total_bytes_saved", -1))
            mgain = manifest.get("logical_gain_bytes", -1)
            if rgain != mgain:
                errors.append(f"Manifest/report inconsistency: report gain {rgain} != manifest {mgain}")

        integrity_path = root / "integrity.json"
        if integrity_path.exists() and mode in ("strict", "integrity-only"):
            integrity = json.loads(integrity_path.read_text(encoding="utf-8"))
            stored = integrity.get("checksums", {})
            for rel_path, expected in stored.items():
                fp = root / rel_path
                if not fp.exists():
                    errors.append(f"Integrity: checksum file missing: {rel_path}")
                else:
                    actual = _sha256_file(fp)
                    if actual != expected:
                        errors.append(f"Integrity mismatch: {rel_path} (expected {expected[:16]}..., got {actual[:16]}...)")

    return len(errors) == 0, errors


def reconstruct_archive(
    archive_path: Path | str,
    output_path: Path | str,
) -> dict[str, str]:
    """
    Reconstruct project from archive. Writes files to output_path.
    Returns {path: content} for reconstructed files.
    """
    archive = Path(archive_path).resolve()
    out_root = Path(output_path).resolve()
    out_root.mkdir(parents=True, exist_ok=True)
    import tempfile
    with tempfile.TemporaryDirectory(prefix="infold_") as tmp:
        with zipfile.ZipFile(archive, "r") as zf:
            zf.extractall(tmp)
        root = Path(tmp)
        manifest_candidates = list(root.rglob("manifest.json"))
        if manifest_candidates:
            root = manifest_candidates[0].parent
        maps_path = root / "maps" / "reconstruction.json"
        if not maps_path.exists():
            raise ValueError("Invalid archive: missing maps/reconstruction.json")
        maps_data = json.loads(maps_path.read_text(encoding="utf-8"))
        records = maps_data.get("records", [])
        shared_dir = root / "shared"
        from infold.engine.metadata_table_fold import load_path_table
        path_table = load_path_table(root)
        result: dict[str, str] = {}

        def _resolve_targets(rec: dict) -> list[str]:
            if path_table is not None and "targets_refs" in rec:
                return [path_table[i] for i in rec["targets_refs"] if i < len(path_table)]
            return rec.get("targets", [])

        for rec in records:
            idx = rec.get("index", 0)
            op_id = rec.get("operator_id", "")
            targets = _resolve_targets(rec)
            if op_id == "exact_repetition":
                content_path = shared_dir / f"exact_{idx}.txt"
                if content_path.exists():
                    content = content_path.read_text(encoding="utf-8")
                    for t in targets:
                        p = out_root / t
                        p.parent.mkdir(parents=True, exist_ok=True)
                        p.write_text(content, encoding="utf-8")
                        result[t] = content
            elif op_id == "template_skeleton":
                tmpl_path = shared_dir / f"template_{idx}.json"
                if tmpl_path.exists():
                    data = json.loads(tmpl_path.read_text(encoding="utf-8"))
                    const_blocks = data.get("const_blocks", [])
                    slot_groups = data.get("slot_groups", [])
                    paths = data.get("paths", targets)
                    for j, path_str in enumerate(paths):
                        out_parts = []
                        for bi, const in enumerate(const_blocks):
                            out_parts.append(const)
                            if bi < len(slot_groups):
                                sg = slot_groups[bi]
                                if sg and isinstance(sg[0], list):
                                    out_parts.append("".join(sg[k][j] for k in range(len(sg))))
                                else:
                                    out_parts.append(sg[j] if j < len(sg) else "")
                        content = "".join(out_parts)
                        p = out_root / path_str
                        p.parent.mkdir(parents=True, exist_ok=True)
                        p.write_text(content, encoding="utf-8")
                        result[path_str] = content
            elif op_id == "symbol_table":
                sym_path = shared_dir / f"symbols_{idx}.json"
                if sym_path.exists():
                    data = json.loads(sym_path.read_text(encoding="utf-8"))
                    id_to_symbol = data.get("id_to_symbol", {})
                    folded_files = data.get("folded_files", {})
                    def _ph(pid): return f"\uE000{pid}\uE001"
                    for path_str, folded in folded_files.items():
                        content = folded
                        for pid, name in sorted(id_to_symbol.items(), key=lambda x: -len(_ph(x[0]))):
                            content = content.replace(_ph(pid), name)
                        p = out_root / path_str
                        p.parent.mkdir(parents=True, exist_ok=True)
                        p.write_text(content, encoding="utf-8")
                        result[path_str] = content
            elif op_id == "hierarchy_mirror":
                hier_path = shared_dir / f"hierarchy_{idx}.json"
                if hier_path.exists():
                    data = json.loads(hier_path.read_text(encoding="utf-8"))
                    file_contents = data.get("file_contents", {})
                    for path_str, content in file_contents.items():
                        if path_str in result:
                            continue
                        p = out_root / path_str
                        p.parent.mkdir(parents=True, exist_ok=True)
                        p.write_text(content, encoding="utf-8")
                        result[path_str] = content
            elif op_id == "dependency_motif":
                dep_path = shared_dir / f"dependency_motif_{idx}.json"
                if dep_path.exists():
                    data = json.loads(dep_path.read_text(encoding="utf-8"))
                    file_contents = data.get("file_contents", {})
                    for path_str, content in file_contents.items():
                        if path_str in result:
                            continue
                        p = out_root / path_str
                        p.parent.mkdir(parents=True, exist_ok=True)
                        p.write_text(content, encoding="utf-8")
                        result[path_str] = content
            elif op_id == "byte_fold":
                chunk_recon_path = root / "maps" / "chunk_reconstruction.json"
                chunk_index_path = shared_dir / "chunk_index.json"
                chunks_dir = shared_dir / "chunks"
                if chunk_recon_path.exists() and chunks_dir.exists() and chunk_index_path.exists():
                    recon_data = json.loads(chunk_recon_path.read_text(encoding="utf-8"))
                    index_data = json.loads(chunk_index_path.read_text(encoding="utf-8"))
                    chunk_ids = index_data.get("ids", index_data.get("chunk_ids", []))
                    recs = recon_data.get("records", [])
                    crec = recs[idx] if idx < len(recs) else {}
                    paths_list = crec.get("paths", [])
                    if path_table is not None and "path_refs" in crec:
                        paths_list = [path_table[i] for i in crec["path_refs"] if i < len(path_table)]
                    if (paths_list or crec.get("path_refs")) and "seqs" in crec:
                        for path_idx, path_str in enumerate(paths_list):
                            if path_str in result:
                                continue
                            seq = crec["seqs"][path_idx] if path_idx < len(crec["seqs"]) else []
                            parts = []
                            for ch_idx in seq:
                                ch_id = chunk_ids[ch_idx] if ch_idx < len(chunk_ids) else None
                                if ch_id:
                                    bin_path = chunks_dir / f"{ch_id}.bin"
                                    if bin_path.exists():
                                        parts.append(bin_path.read_bytes())
                            content = b"".join(parts).decode("utf-8")
                            p = out_root / path_str
                            p.parent.mkdir(parents=True, exist_ok=True)
                            p.write_text(content, encoding="utf-8")
                            result[path_str] = content
                    else:
                        path_to_ids = crec.get("path_to_chunk_ids", {})
                        for path_str, ch_ids in path_to_ids.items():
                            if path_str in result:
                                continue
                            parts = []
                            for ch_id in ch_ids:
                                bin_path = chunks_dir / f"{ch_id}.bin"
                                if bin_path.exists():
                                    parts.append(bin_path.read_bytes())
                            content = b"".join(parts).decode("utf-8")
                            p = out_root / path_str
                            p.parent.mkdir(parents=True, exist_ok=True)
                            p.write_text(content, encoding="utf-8")
                            result[path_str] = content
        passthrough_path = root / "snapshots" / "passthrough.json"
        if passthrough_path.exists():
            passthrough = json.loads(passthrough_path.read_text(encoding="utf-8"))
            for k, content in passthrough.items():
                path_str = path_table[int(k)] if path_table is not None and k.isdigit() else k
                if path_str not in result:
                    p = out_root / path_str
                    p.parent.mkdir(parents=True, exist_ok=True)
                    p.write_text(content, encoding="utf-8")
                    result[path_str] = content
        return result


def _family_signatures(root: Path, records: list) -> dict[str, dict[str, set[str]]]:
    """Extract family signatures for exact_repetition, template, hierarchy, dependency, byte_fold. Returns {op: {sig: set of paths/roots}}."""
    from infold.engine.metadata_table_fold import load_path_table as _load_pt
    maps_path = root / "maps" / "reconstruction.json"
    path_table = None
    if maps_path.exists():
        maps_data = json.loads(maps_path.read_text(encoding="utf-8"))
        if maps_data.get("path_table_ref"):
            path_table = _load_pt(root)
    sigs: dict[str, dict[str, set[str]]] = {
        "exact_repetition": {},
        "template_skeleton": {},
        "hierarchy_mirror": {},
        "dependency_motif": {},
        "byte_fold": {},
    }
    shared = root / "shared"

    def _resolve_targets(rec: dict) -> list:
        if path_table is not None and "targets_refs" in rec:
            return [path_table[r] for r in rec["targets_refs"] if r < len(path_table)]
        return rec.get("targets", [])

    for i, rec in enumerate(records):
        op = rec.get("operator_id", "")
        if op not in sigs:
            continue
        targets = _resolve_targets(rec)
        if op == "exact_repetition":
            paths = tuple(sorted(str(t) for t in targets))
            if paths:
                sig = hashlib.sha256(json.dumps(paths).encode()).hexdigest()[:16]
                sigs[op][sig] = set(paths)
        elif op == "template_skeleton":
            tmpl = shared / f"template_{i}.json"
            if tmpl.exists():
                data = json.loads(tmpl.read_text(encoding="utf-8"))
                paths = tuple(sorted(data.get("paths", targets)))
            else:
                paths = tuple(sorted(str(t) for t in targets))
            sig = hashlib.sha256(json.dumps(paths).encode()).hexdigest()[:16]
            sigs[op][sig] = set(paths)
        elif op == "hierarchy_mirror":
            hj = shared / f"hierarchy_{i}.json"
            if hj.exists():
                data = json.loads(hj.read_text(encoding="utf-8"))
                roots = tuple(sorted(str(r) for r in data.get("roots", [])))
                sig = data.get("structure_sig", "") or hashlib.sha256(json.dumps(roots).encode()).hexdigest()[:16]
                sigs[op][sig] = set(roots)
        elif op == "dependency_motif":
            dj = shared / f"dependency_motif_{i}.json"
            if dj.exists():
                data = json.loads(dj.read_text(encoding="utf-8"))
                paths = tuple(sorted(str(p) for p in data.get("paths", targets)))
                sig = data.get("signature", "") or hashlib.sha256(json.dumps(paths).encode()).hexdigest()[:16]
                sigs[op][sig] = set(paths)
        elif op == "byte_fold":
            recon_path = root / "maps" / "chunk_reconstruction.json"
            if recon_path.exists() and i < len(records):
                recon_data = json.loads(recon_path.read_text(encoding="utf-8"))
                recs = recon_data.get("records", [])
                crec = recs[i] if i < len(recs) else {}
                from infold.engine.metadata_table_fold import load_path_table as _load_pt
                pt = _load_pt(root) if recon_data.get("path_table_ref") else None
                if pt is not None and "path_refs" in crec:
                    paths = tuple(sorted(pt[ref] for ref in crec["path_refs"] if ref < len(pt)))
                else:
                    paths = tuple(sorted(crec.get("paths", [str(t) for t in targets])))
                if paths:
                    sig = hashlib.sha256(json.dumps(paths).encode()).hexdigest()[:16]
                    sigs[op][sig] = set(paths)
    return sigs


def compare_archives(
    archive_a_path: Path | str,
    archive_b_path: Path | str,
) -> dict[str, Any]:
    """
    Compare two .infold archives. Returns diff summary:
    - logical_gain, physical_folded_size, operator fold counts
    - which template families, hierarchy templates, dependency motifs changed (added/removed)
    - compatibility metadata
    """
    a = Path(archive_a_path).resolve()
    b = Path(archive_b_path).resolve()
    if not a.exists():
        raise FileNotFoundError(f"Archive not found: {a}")
    if not b.exists():
        raise FileNotFoundError(f"Archive not found: {b}")
    info_a = explain_archive(a)
    info_b = explain_archive(b)
    m_a = info_a["manifest"]
    m_b = info_b["manifest"]
    pkg_a = info_a["package_summary"]
    pkg_b = info_b["package_summary"]
    diff = {
        "archive_a": str(a),
        "archive_b": str(b),
        "logical_gain": {
            "a": pkg_a["logical_gain_bytes"],
            "b": pkg_b["logical_gain_bytes"],
            "diff": pkg_b["logical_gain_bytes"] - pkg_a["logical_gain_bytes"],
        },
        "physical_folded_size": {
            "a": pkg_a["physical_folded_size_bytes"],
            "b": pkg_b["physical_folded_size_bytes"],
            "diff": pkg_b["physical_folded_size_bytes"] - pkg_a["physical_folded_size_bytes"],
        },
        "fold_counts_by_operator": {
            "a": info_a["fold_counts_by_operator"],
            "b": info_b["fold_counts_by_operator"],
            "diff": {},
        },
        "gain_by_operator": {
            "a": info_a["gain_by_operator"],
            "b": info_b["gain_by_operator"],
            "diff": {},
        },
        "template_families": {"a_count": len(info_a["template_families"]), "b_count": len(info_b["template_families"])},
        "duplicate_families": {"a_count": len(info_a["duplicate_families"]), "b_count": len(info_b["duplicate_families"])},
        "hierarchy_templates": {"a_count": len(info_a["hierarchy_templates"]), "b_count": len(info_b["hierarchy_templates"])},
        "dependency_motifs": {"a_count": len(info_a["dependency_motifs"]), "b_count": len(info_b["dependency_motifs"])},
        "byte_fold_families": {"a_count": len(info_a["byte_fold_families"]), "b_count": len(info_b["byte_fold_families"])},
        "template_families_changed": {"added": [], "removed": []},
        "hierarchy_templates_changed": {"added": [], "removed": []},
        "dependency_motifs_changed": {"added": [], "removed": []},
        "byte_fold_families_changed": {"added": [], "removed": []},
        "duplicate_families_changed": {"added": [], "removed": []},
        "compatibility": {
            "a": m_a.get("compatibility", {}),
            "b": m_b.get("compatibility", {}),
        },
    }
    all_ops = set(info_a["fold_counts_by_operator"]) | set(info_b["fold_counts_by_operator"])
    for op in all_ops:
        diff["fold_counts_by_operator"]["diff"][op] = info_b["fold_counts_by_operator"].get(op, 0) - info_a["fold_counts_by_operator"].get(op, 0)
    for op in all_ops:
        diff["gain_by_operator"]["diff"][op] = info_b["gain_by_operator"].get(op, 0) - info_a["gain_by_operator"].get(op, 0)

    import tempfile
    with tempfile.TemporaryDirectory(prefix="infold_") as tmp:
        with zipfile.ZipFile(a, "r") as zf:
            zf.extractall(tmp)
        root_a = _extract_root(Path(tmp))
        maps_a = json.loads((root_a / "maps" / "reconstruction.json").read_text(encoding="utf-8"))
        recs_a = maps_a.get("records", [])
        sigs_a = _family_signatures(root_a, recs_a)
    with tempfile.TemporaryDirectory(prefix="infold_") as tmp:
        with zipfile.ZipFile(b, "r") as zf:
            zf.extractall(tmp)
        root_b = _extract_root(Path(tmp))
        maps_b = json.loads((root_b / "maps" / "reconstruction.json").read_text(encoding="utf-8"))
        recs_b = maps_b.get("records", [])
        sigs_b = _family_signatures(root_b, recs_b)

    for op in ["exact_repetition", "template_skeleton", "hierarchy_mirror", "dependency_motif", "byte_fold"]:
        sa = sigs_a.get(op, {})
        sb = sigs_b.get(op, {})
        added = [sig for sig in sb if sig not in sa]
        removed = [sig for sig in sa if sig not in sb]
        if op == "exact_repetition":
            diff["duplicate_families_changed"] = {"added": added, "removed": removed}
        elif op == "template_skeleton":
            diff["template_families_changed"]["added"] = added
            diff["template_families_changed"]["removed"] = removed
        elif op == "hierarchy_mirror":
            diff["hierarchy_templates_changed"]["added"] = added
            diff["hierarchy_templates_changed"]["removed"] = removed
        elif op == "dependency_motif":
            diff["dependency_motifs_changed"]["added"] = added
            diff["dependency_motifs_changed"]["removed"] = removed
        elif op == "byte_fold":
            diff["byte_fold_families_changed"]["added"] = added
            diff["byte_fold_families_changed"]["removed"] = removed

    return diff


def explain_to_text(info: dict[str, Any]) -> str:
    """Format explain output for human reading."""
    lines = [
        "Archive Explain",
        "===============",
        "",
        f"Path: {info.get('path', '?')}",
        "",
    ]
    fp = info.get("fold_profile")
    if fp:
        mode = info.get("fold_profile_mode", "?")
        reason = info.get("fold_profile_reason", "?")
        lines.append(f"Fold profile: {fp} ({mode}, reason={reason})")
        factors = info.get("fold_profile_factors") or {}
        if factors.get("scores"):
            lines.append(f"  Profile scores: {factors.get('scores', {})}")
        if factors.get("selected_score") is not None:
            lines.append(f"  Selected score: {factors.get('selected_score')}")
        lines.append("")
    species = info.get("fold_species")
    if species:
        mode = info.get("fold_species_mode", "?")
        reason = info.get("fold_species_reason", "?")
        lines.append(f"Fold species: {species} ({mode}, reason={reason})")
        adapt = info.get("fold_creature_adapt_reasons") or []
        if adapt:
            lines.append("  Adaptation reasons:")
            for r in adapt:
                lines.append(f"    - {r}")
        traits = info.get("fold_creature_traits_final") or {}
        if traits:
            lines.append(f"  Final traits: {traits}")
        lines.append("")
    lines.append("Package summary:")
    pkg = info.get("package_summary", {})
    for k, v in pkg.items():
        if isinstance(v, int) and "bytes" in str(k):
            lines.append(f"  {k}: {v:,}")
        else:
            lines.append(f"  {k}: {v}")
    lines.append("")
    lines.append("Fold counts by operator:")
    for op, cnt in info.get("fold_counts_by_operator", {}).items():
        lines.append(f"  {op}: {cnt}")
    lines.append("")
    lines.append("Biggest gain contributors:")
    for c in info.get("biggest_gain_contributors", [])[:5]:
        lines.append(f"  {c.get('operator_id', '?')}: {c.get('gain', 0):,} bytes")
    rejected = info.get("rejected_candidates_summary", [])
    if rejected:
        lines.append("")
        lines.append("Rejected candidates:")
        for r in rejected[:10]:
            lines.append(f"  {r.get('operator_id', '?')}: {r.get('reason', '?')} - {r.get('detail', '')[:50]}")
        if len(rejected) > 10:
            lines.append(f"  ... and {len(rejected) - 10} more")
    sym_blocked = info.get("symbol_table_conflict_blocked", [])
    if sym_blocked:
        lines.append("")
        lines.append("Symbol table blocked (conflict with earlier content folds):")
        lines.append("  symbol_table targets project-wide files; paths already folded by exact_repetition/template_skeleton are excluded.")
        lines.append("  Blocking is correct: content operators cannot double-fold the same file.")
        for sb in sym_blocked[:2]:
            lines.append(f"  Detail: {sb.get('detail', '')[:60]}")
    mtf = info.get("metadata_table_fold_metrics")
    if mtf:
        lines.append("")
        lines.append("Metadata Table Fold:")
        lines.append(f"  unique_paths: {mtf.get('metadata_table_unique_paths', 0)}")
        lines.append(f"  table_size_bytes: {mtf.get('metadata_table_table_size_bytes', 0):,}")
        lines.append(f"  net_bytes_saved: {mtf.get('metadata_table_net_bytes_saved', 0):,}")
    tess_dim = info.get("tesseract_families_by_dimension", {})
    if tess_dim:
        lines.append("")
        lines.append("Tesseract (multi-dimensional family identity):")
        lines.append(f"  Families: {info.get('tesseract_family_count', 0)}")
        for dim, ops in sorted(tess_dim.items()):
            label = {"structure": "mainly structural", "byte": "byte reuse", "metadata": "metadata reuse", "time": "temporal persistence"}.get(dim, dim)
            lines.append(f"  {label}: {', '.join(ops)}")
    tp = info.get("tesseract_planner")
    if tp:
        lines.append("")
        lines.append("Tesseract Planner:")
        lines.append(f"  route={tp.get('route')} ({tp.get('route_reason')})")
        lines.append(f"  dominant={tp.get('dominant_dimension')} secondary={tp.get('secondary_dimension')}")
        lines.append(f"  operator_priority={'>'.join(tp.get('operator_family_priority', []))}")
        bias = tp.get("planner_bias", {})
        active = [f"{k}={v}" for k, v in bias.items() if v and v > 0]
        if active:
            lines.append(f"  planner_bias={', '.join(active)}")
    lines.append("")
    lines.append("Reconstruction guarantees:")
    for g in info.get("reconstruction_guarantees", []):
        lines.append(f"  {g}")
    return "\n".join(lines)


def compare_to_text(diff: dict[str, Any]) -> str:
    """Format compare output for human reading."""
    lines = [
        "Archive Compare",
        "===============",
        "",
        f"A: {diff.get('archive_a', '?')}",
        f"B: {diff.get('archive_b', '?')}",
        "",
        "Logical gain:",
        f"  A: {diff.get('logical_gain', {}).get('a', 0):,} bytes",
        f"  B: {diff.get('logical_gain', {}).get('b', 0):,} bytes",
        f"  diff (B-A): {diff.get('logical_gain', {}).get('diff', 0):+,} bytes",
        "",
        "Physical folded size:",
        f"  A: {diff.get('physical_folded_size', {}).get('a', 0):,} bytes",
        f"  B: {diff.get('physical_folded_size', {}).get('b', 0):,} bytes",
        f"  diff (B-A): {diff.get('physical_folded_size', {}).get('diff', 0):+,} bytes",
        "",
        "Fold counts by operator (diff B-A):",
    ]
    for op, d in diff.get("fold_counts_by_operator", {}).get("diff", {}).items():
        lines.append(f"  {op}: {d:+d}")
    lines.append("")
    lines.append("Families:")
    tf = diff.get("template_families", {})
    df = diff.get("duplicate_families", {})
    hf = diff.get("hierarchy_templates", {})
    mf = diff.get("dependency_motifs", {})
    bf = diff.get("byte_fold_families", {})
    lines.append(f"  template: A={tf.get('a_count', 0)} B={tf.get('b_count', 0)}")
    lines.append(f"  duplicate: A={df.get('a_count', 0)} B={df.get('b_count', 0)}")
    lines.append(f"  hierarchy: A={hf.get('a_count', 0)} B={hf.get('b_count', 0)}")
    lines.append(f"  dependency: A={mf.get('a_count', 0)} B={mf.get('b_count', 0)}")
    lines.append(f"  byte_fold: A={bf.get('a_count', 0)} B={bf.get('b_count', 0)}")
    tfc = diff.get("template_families_changed", {})
    htc = diff.get("hierarchy_templates_changed", {})
    dmc = diff.get("dependency_motifs_changed", {})
    if tfc.get("added") or tfc.get("removed"):
        lines.append("")
        lines.append("Template families changed:")
        if tfc.get("added"):
            lines.append(f"  added: {tfc['added']}")
        if tfc.get("removed"):
            lines.append(f"  removed: {tfc['removed']}")
    if htc.get("added") or htc.get("removed"):
        lines.append("")
        lines.append("Hierarchy templates changed:")
        if htc.get("added"):
            lines.append(f"  added: {htc['added']}")
        if htc.get("removed"):
            lines.append(f"  removed: {htc['removed']}")
    if dmc.get("added") or dmc.get("removed"):
        lines.append("")
        lines.append("Dependency motifs changed:")
        if dmc.get("added"):
            lines.append(f"  added: {dmc['added']}")
        if dmc.get("removed"):
            lines.append(f"  removed: {dmc['removed']}")
    bfc = diff.get("byte_fold_families_changed", {})
    if bfc.get("added") or bfc.get("removed"):
        lines.append("")
        lines.append("Byte Fold families changed:")
        if bfc.get("added"):
            lines.append(f"  added: {bfc['added']}")
        if bfc.get("removed"):
            lines.append(f"  removed: {bfc['removed']}")
    return "\n".join(lines)


def list_to_text(info: dict[str, Any]) -> str:
    """Format list output for human reading."""
    lines = [
        "Archive List",
        "============",
        "",
        f"Path: {info.get('path', '?')}",
        "",
        "Shared artifacts:",
    ]
    for a in info.get("shared_artifacts", []):
        lines.append(f"  {a.get('path', '?')} ({a.get('size', 0):,} bytes)")
    lines.append("")
    lines.append("Fold families by operator:")
    for op, fams in info.get("families_by_operator", {}).items():
        lines.append(f"  {op}:")
        for f in fams:
            paths = f.get("paths", f.get("roots_or_paths", []))
            lines.append(f"    [{f.get('index', '?')}] gain={f.get('gain', 0):,} targets={f.get('target_count', 0)}")
            if paths:
                for p in paths[:3]:
                    lines.append(f"      - {p}")
                if len(paths) > 3:
                    lines.append(f"      - ...")
    return "\n".join(lines)


def stats_to_text(stats: dict[str, Any]) -> str:
    """Format stats output for human reading."""
    lines = [
        "Archive Stats",
        "=============",
        "",
        f"Path: {stats.get('path', '?')}",
        "",
        "Metrics:",
        f"  logical_gain_bytes: {stats.get('logical_gain_bytes', 0):,}",
        f"  physical_folded_size_bytes: {stats.get('physical_folded_size_bytes', 0):,}",
        f"  original_size_bytes: {stats.get('original_size_bytes', 0):,}",
        f"  rejection_count: {stats.get('rejection_count', 0)}",
        "",
        "Per-operator contributions:",
    ]
    for op, g in stats.get("per_operator_contributions", {}).items():
        share = stats.get("per_operator_gain_share", {}).get(op, 0)
        lines.append(f"  {op}: {g:,} bytes ({share:.1%})")
    tm = stats.get("template_metrics")
    if tm and tm.get("families_found", 0) > 0:
        lines.append("")
        lines.append("Template metrics:")
        lines.append(f"  families_found: {tm.get('families_found', 0)}")
        if tm.get("family_purity"):
            lines.append(f"  family_purity: {tm['family_purity']}")
        if tm.get("slot_ambiguity"):
            lines.append(f"  slot_ambiguity: {tm['slot_ambiguity']}")
    hm = stats.get("hierarchy_metrics")
    if hm and hm.get("templates_found", 0) > 0:
        lines.append("")
        lines.append("Hierarchy metrics:")
        lines.append(f"  templates_found: {hm.get('templates_found', 0)}")
        if hm.get("instances_per_template"):
            lines.append(f"  instances_per_template: {hm['instances_per_template']}")
    dm = stats.get("dependency_metrics")
    if dm and dm.get("motifs_found", 0) > 0:
        lines.append("")
        lines.append("Dependency metrics:")
        lines.append(f"  motifs_found: {dm.get('motifs_found', 0)}")
        if dm.get("average_motif_size"):
            lines.append(f"  average_motif_size: {dm['average_motif_size']}")
    mtf = stats.get("metadata_table_fold_metrics")
    if mtf:
        lines.append("")
        lines.append("Metadata Table Fold:")
        lines.append(f"  unique_paths: {mtf.get('metadata_table_unique_paths', 0)}")
        lines.append(f"  reused_refs: {mtf.get('metadata_table_reused_refs', 0)}")
        lines.append(f"  table_size_bytes: {mtf.get('metadata_table_table_size_bytes', 0):,}")
        lines.append(f"  net_bytes_saved: {mtf.get('metadata_table_net_bytes_saved', 0):,}")
    return "\n".join(lines)
