"""
Infold Archive v0.1: create, inspect, validate, reconstruct.

Deterministic validation. Preserves exact-mode guarantees.
"""

import json
import zipfile
from pathlib import Path
from typing import Any

from infold.engine.package_spec import (
    MANIFEST_REQUIRED_KEYS,
    REQUIRED_DIRS,
    REQUIRED_FILES,
)


def create_archive(
    source_path: Path | str,
    output_path: Path | str,
    config: dict[str, Any] | None = None,
) -> Path:
    """
    Create an Infold archive: run fold, export package, zip to .infold.
    """
    import shutil
    import tempfile

    from infold.engine import run_fold
    from infold.engine import export_package

    if config is None:
        from infold.cli import load_config
        config = load_config()
    source = Path(source_path).resolve()
    out = Path(output_path).resolve()
    if out.suffix != ".infold":
        out = out.with_suffix(".infold")
    result = run_fold(source, config)
    pkg_dir = Path(tempfile.mkdtemp(prefix="infold_pkg_"))
    try:
        export_package(result, config, pkg_dir)
        with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as zf:
            for f in sorted(pkg_dir.rglob("*")):
                if f.is_file():
                    arcname = f.relative_to(pkg_dir)
                    zf.write(f, arcname)
    finally:
        if pkg_dir.exists():
            shutil.rmtree(pkg_dir)
    return out


def _extract_root(tmp: Path) -> Path:
    """Find package root (where manifest.json lives)."""
    manifest = tmp / "manifest.json"
    if manifest.exists():
        return tmp
    found = list(tmp.rglob("manifest.json"))
    return found[0].parent if found else tmp


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

        return {
            "path": str(archive),
            "manifest": manifest,
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
            "reconstruction_guarantees": reconstruction_guarantees,
            "template_families": report.get("template_families", []),
            "duplicate_families": report.get("duplicate_families", []),
            "hierarchy_templates": report.get("hierarchy_templates", []),
            "dependency_motifs": report.get("dependency_motifs", []),
        }


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


def validate_archive(archive_path: Path | str) -> tuple[bool, list[str]]:
    """
    Validate archive against package spec v1. Stronger checks:
    - Ledger consistency (total_folds, total_bytes_saved)
    - Shared artifact references (each record has corresponding shared file)
    - Maps/reconstruction integrity (indices, operator_ids)
    - Manifest/report consistency
    Returns (ok, list of error messages). Deterministic and readable.
    """
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

    for f in REQUIRED_FILES:
        if not has_file(f):
            errors.append(f"Missing required file: {f}")
    for d in REQUIRED_DIRS:
        if not has_dir(d):
            errors.append(f"Missing required directory: {d}/")
    if errors:
        return False, errors

    import tempfile
    with tempfile.TemporaryDirectory(prefix="infold_") as tmp:
        with zipfile.ZipFile(archive, "r") as zf:
            zf.extractall(tmp)
        root = _extract_root(Path(tmp))

        manifest_path = root / "manifest.json"
        if not manifest_path.exists():
            return False, errors + ["Could not find manifest.json"]
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        for key in MANIFEST_REQUIRED_KEYS:
            if key not in manifest:
                errors.append(f"Manifest missing required key: {key}")
        compat = manifest.get("compatibility", {})
        if compat.get("reconstruction_mode") != "deterministic":
            errors.append("Compatibility: reconstruction_mode must be 'deterministic'")

        ledger_path = root / "ledger.json"
        if ledger_path.exists():
            ledger = json.loads(ledger_path.read_text(encoding="utf-8"))
            lr = ledger.get("fold_records", [])
            ltotal = ledger.get("total_folds", 0)
            lbytes = ledger.get("total_bytes_saved", 0)
            if len(lr) != ltotal:
                errors.append(f"Ledger inconsistency: fold_records count {len(lr)} != total_folds {ltotal}")
            computed = sum(r.get("gain", 0) for r in lr)
            if computed != lbytes:
                errors.append(f"Ledger inconsistency: sum(gain) {computed} != total_bytes_saved {lbytes}")

        maps_path = root / "maps" / "reconstruction.json"
        if maps_path.exists():
            maps_data = json.loads(maps_path.read_text(encoding="utf-8"))
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
        if report_path.exists():
            report = json.loads(report_path.read_text(encoding="utf-8"))
            rfold = report.get("fold_count", -1)
            mfold = manifest.get("fold_count", -1)
            if rfold != mfold:
                errors.append(f"Manifest/report inconsistency: report fold_count {rfold} != manifest {mfold}")
            rgain = report.get("logical_gain_bytes", report.get("total_bytes_saved", -1))
            mgain = manifest.get("logical_gain_bytes", -1)
            if rgain != mgain:
                errors.append(f"Manifest/report inconsistency: report gain {rgain} != manifest {mgain}")

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
        result: dict[str, str] = {}
        for rec in records:
            idx = rec.get("index", 0)
            op_id = rec.get("operator_id", "")
            targets = rec.get("targets", [])
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
        passthrough_path = root / "snapshots" / "passthrough.json"
        if passthrough_path.exists():
            passthrough = json.loads(passthrough_path.read_text(encoding="utf-8"))
            for path_str, content in passthrough.items():
                if path_str not in result:
                    p = out_root / path_str
                    p.parent.mkdir(parents=True, exist_ok=True)
                    p.write_text(content, encoding="utf-8")
                    result[path_str] = content
        return result


def compare_archives(
    archive_a_path: Path | str,
    archive_b_path: Path | str,
) -> dict[str, Any]:
    """
    Compare two .infold archives. Returns diff summary:
    - logical_gain diff
    - physical_folded_size diff
    - operator fold counts diff
    - template/symbol/hierarchy/dependency families
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
    return diff


def explain_to_text(info: dict[str, Any]) -> str:
    """Format explain output for human reading."""
    lines = [
        "Archive Explain",
        "===============",
        "",
        f"Path: {info.get('path', '?')}",
        "",
        "Package summary:",
    ]
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
    lines.append(f"  template: A={tf.get('a_count', 0)} B={tf.get('b_count', 0)}")
    lines.append(f"  duplicate: A={df.get('a_count', 0)} B={df.get('b_count', 0)}")
    lines.append(f"  hierarchy: A={hf.get('a_count', 0)} B={hf.get('b_count', 0)}")
    lines.append(f"  dependency: A={mf.get('a_count', 0)} B={mf.get('b_count', 0)}")
    return "\n".join(lines)
