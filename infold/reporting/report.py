"""
Reporting: JSON and text summaries, diagnostics, fold breakdown.
"""

import json
from pathlib import Path
from typing import Any

from infold.engine.ledger import FoldLedger
from infold.engine.orchestrator import FoldResult
from infold.models.project_sheet import ProjectSheet


def build_report(result: FoldResult, config: dict[str, Any]) -> dict[str, Any]:
    """Build report dict from FoldResult."""
    sheet = result.project_sheet
    ledger = result.ledger
    metrics = sheet.metrics

    # Operator breakdown
    op_breakdown: dict[str, dict[str, Any]] = {}
    template_metrics: dict[str, Any] = {}
    symbol_table_metrics: dict[str, Any] = {}
    hierarchy_metrics: dict[str, Any] = {}
    dependency_metrics: dict[str, Any] = {}
    byte_fold_metrics: dict[str, Any] = {}
    mutation_chain_metrics: dict[str, Any] = {}
    fold_echo_metrics: dict[str, Any] = {}
    anchor_metrics: dict[str, Any] = {}
    for r in ledger.fold_records:
        if r.operator_id not in op_breakdown:
            op_breakdown[r.operator_id] = {"count": 0, "gain": 0, "targets": 0}
        op_breakdown[r.operator_id]["count"] += 1
        op_breakdown[r.operator_id]["gain"] += r.gain
        op_breakdown[r.operator_id]["targets"] += len(r.targets)
        if r.operator_id == "symbol_table":
            recipe = getattr(r, "unfold_recipe", {}) or {}
            symbol_table_metrics.setdefault("shared_symbol_count", 0)
            symbol_table_metrics["shared_symbol_count"] = symbol_table_metrics.get("shared_symbol_count", 0) + recipe.get("shared_symbol_count", 0)
            symbol_table_metrics.setdefault("symbol_reuse_ratio", []).append(recipe.get("symbol_reuse_ratio"))
            symbol_table_metrics.setdefault("net_bytes_saved", 0)
            symbol_table_metrics["net_bytes_saved"] = symbol_table_metrics.get("net_bytes_saved", 0) + r.gain
        if r.operator_id == "hierarchy_mirror":
            recipe = getattr(r, "unfold_recipe", {}) or {}
            hierarchy_metrics.setdefault("hierarchy_templates_found", 0)
            hierarchy_metrics["hierarchy_templates_found"] += 1
            hierarchy_metrics.setdefault("instances_per_template", []).append(recipe.get("instance_count", 0))
            hierarchy_metrics.setdefault("net_bytes_saved", 0)
            hierarchy_metrics["net_bytes_saved"] = hierarchy_metrics.get("net_bytes_saved", 0) + r.gain
            hierarchy_metrics.setdefault("structural_reuse_ratio", []).append(recipe.get("structural_reuse_ratio"))
            hierarchy_metrics.setdefault("path_reconstruction_accuracy", []).append(recipe.get("path_reconstruction_accuracy"))
            hierarchy_metrics.setdefault("file_membership_accuracy", []).append(recipe.get("file_membership_accuracy"))
        if r.operator_id == "dependency_motif":
            recipe = getattr(r, "unfold_recipe", {}) or {}
            dependency_metrics.setdefault("dependency_motifs_found", 0)
            dependency_metrics["dependency_motifs_found"] += 1
            dependency_metrics.setdefault("motif_instances_per_family", []).append(recipe.get("instance_count", 0))
            dependency_metrics.setdefault("average_motif_size", []).append(recipe.get("motif_size", 0))
            dependency_metrics.setdefault("net_bytes_saved", 0)
            dependency_metrics["net_bytes_saved"] = dependency_metrics.get("net_bytes_saved", 0) + r.gain
            dependency_metrics.setdefault("dependency_recovery_accuracy", []).append(recipe.get("dependency_recovery_accuracy"))
            dependency_metrics.setdefault("structural_reuse_ratio", []).append(recipe.get("structural_reuse_ratio"))
        if r.operator_id == "template_skeleton":
            recipe = getattr(r, "unfold_recipe", {}) or {}
            template_metrics.setdefault("families_found", 0)
            template_metrics["families_found"] += 1
            template_metrics.setdefault("files_per_family", []).append(len(r.targets))
            template_metrics.setdefault("scaffold_lengths", []).append(
                sum(len(b) for b in recipe.get("const_blocks", []))
            )
            template_metrics.setdefault("slot_counts", []).append(len(recipe.get("slot_groups", [])))
            template_metrics.setdefault("family_purity", []).append(recipe.get("family_purity"))
            template_metrics.setdefault("slot_ambiguity", []).append(recipe.get("slot_ambiguity"))
            template_metrics.setdefault("avg_slot_size", []).append(recipe.get("avg_slot_size"))
        if r.operator_id == "mutation_chain":
            recipe = getattr(r, "unfold_recipe", {}) or {}
            mutation_chain_metrics.setdefault("families_found", 0)
            mutation_chain_metrics["families_found"] += 1
            mutation_chain_metrics.setdefault("members_per_chain", []).append(len(r.targets))
            mutation_chain_metrics.setdefault("gain_bytes", 0)
            mutation_chain_metrics["gain_bytes"] = mutation_chain_metrics.get("gain_bytes", 0) + r.gain
            if recipe.get("avg_changed_lines") is not None:
                mutation_chain_metrics.setdefault("avg_changed_lines", []).append(recipe["avg_changed_lines"])
            # Phase 16C: anchor/microscope/template handoff counts
            src = recipe.get("_source", "")
            if src == "anchor":
                mutation_chain_metrics["anchor_assisted_count"] = mutation_chain_metrics.get("anchor_assisted_count", 0) + 1
            elif src == "microscope":
                mutation_chain_metrics["microscope_assisted_count"] = mutation_chain_metrics.get("microscope_assisted_count", 0) + 1
            elif src == "template_rejected":
                mutation_chain_metrics["template_handoff_count"] = mutation_chain_metrics.get("template_handoff_count", 0) + 1
        if r.operator_id == "byte_fold":
            recipe = getattr(r, "unfold_recipe", {}) or {}
            chunk_dict = recipe.get("chunk_dict_b64", {})
            reconstruction = recipe.get("reconstruction", {})
            byte_fold_metrics.setdefault("files_chunk_folded", 0)
            byte_fold_metrics["files_chunk_folded"] += len(reconstruction)
            byte_fold_metrics.setdefault("unique_chunk_count", 0)
            byte_fold_metrics["unique_chunk_count"] += len(chunk_dict)
            total_refs = sum(len(ids) for ids in reconstruction.values())
            byte_fold_metrics.setdefault("chunk_reuse_ratio", []).append(
                total_refs / len(chunk_dict) if chunk_dict else 0
            )
            byte_fold_metrics.setdefault("net_bytes_saved", 0)
            byte_fold_metrics["net_bytes_saved"] = byte_fold_metrics.get("net_bytes_saved", 0) + r.gain
            byte_fold_metrics.setdefault("chunk_reused_bytes", 0)
            byte_fold_metrics["chunk_reused_bytes"] = byte_fold_metrics.get("chunk_reused_bytes", 0) + recipe.get("chunk_reused_bytes", 0)
            byte_fold_metrics.setdefault("reused_chunk_count", 0)
            byte_fold_metrics["reused_chunk_count"] = byte_fold_metrics.get("reused_chunk_count", 0) + recipe.get("reused_chunk_count", 0)
            byte_fold_metrics.setdefault("chunk_dictionary_size_bytes", 0)
            byte_fold_metrics["chunk_dictionary_size_bytes"] = byte_fold_metrics.get("chunk_dictionary_size_bytes", 0) + recipe.get("chunk_dictionary_size_bytes", 0)
            byte_fold_metrics.setdefault("chunk_folded_paths", [])
            byte_fold_metrics["chunk_folded_paths"].extend(list(reconstruction.keys()))
        if r.operator_id == "fold_echo":
            recipe = getattr(r, "unfold_recipe", {}) or {}
            fold_echo_metrics.setdefault("echo_count", 0)
            fold_echo_metrics["echo_count"] += 1
            fold_echo_metrics.setdefault("gain_bytes", 0)
            fold_echo_metrics["gain_bytes"] = fold_echo_metrics.get("gain_bytes", 0) + r.gain
            fold_echo_metrics.setdefault("host_operators", []).append(recipe.get("host_operator", ""))

    raw = metrics.get("original_size_bytes", 0)
    logical_gain = ledger.total_bytes_saved  # bytes saved by deduplication
    physical_folded_size = raw - logical_gain  # size of folded representation

    # Per-operator gain share
    total_gain = ledger.total_bytes_saved
    per_operator_gain: dict[str, float] = {}
    for op_id, data in op_breakdown.items():
        g = data.get("gain", 0)
        per_operator_gain[op_id] = g / total_gain if total_gain else 0

    # Family summaries from fold records
    duplicate_families = [r for r in ledger.fold_records if r.operator_id == "exact_repetition"]
    template_families = [r for r in ledger.fold_records if r.operator_id == "template_skeleton"]
    shared_symbol_families = [r for r in ledger.fold_records if r.operator_id == "symbol_table"]
    hierarchy_templates = [r for r in ledger.fold_records if r.operator_id == "hierarchy_mirror"]
    dependency_motifs = [r for r in ledger.fold_records if r.operator_id == "dependency_motif"]
    byte_fold_families = [r for r in ledger.fold_records if r.operator_id == "byte_fold"]
    mutation_chain_families = [r for r in ledger.fold_records if r.operator_id == "mutation_chain"]
    fold_echo_families = [r for r in ledger.fold_records if r.operator_id == "fold_echo"]

    lean = config.get("_lean_mode", False)
    micro = config.get("_micro_mode", False)
    report: dict[str, Any] = {
        "project_id": config.get("project", {}).get("id"),
        "source_path": str(sheet.source_path),
        "file_count": metrics.get("file_count", 0),
        "folder_count": metrics.get("folder_count", 0),
        "original_size_bytes": raw,
        "fold_count": ledger.total_folds,
        "operator_breakdown": op_breakdown,
        "total_bytes_saved": logical_gain,
        "logical_gain_bytes": logical_gain,
        "physical_folded_size_bytes": physical_folded_size,
        "exact_reconstruction_ok": getattr(result, "exact_reconstruction_ok", True),
        "fold_records": ledger.to_dict().get("fold_records", []),
        "reconstruction_status": "ok" if not result.errors and getattr(result, "exact_reconstruction_ok", True) else "errors",
        "errors": result.errors,
        "validation_failures": len(result.errors),
        "creature_enabled": config.get("_creature_adaptive", True),
        "tesseract_planner_enabled": config.get("_tesseract_planner", True),
        "tesseract_cooperation_enabled": config.get("_tesseract_cooperation", True) and config.get("_tesseract_planner", True),
    }
    if not micro:
        report["duplicate_families"] = [{"targets": len(r.targets), "gain": r.gain} for r in duplicate_families]
        report["shared_symbol_families"] = [{"targets": len(r.targets), "gain": r.gain} for r in shared_symbol_families]
        report["hierarchy_templates"] = [{"targets": len(r.targets), "gain": r.gain} for r in hierarchy_templates]
        report["dependency_motifs"] = [{"targets": len(r.targets), "gain": r.gain} for r in dependency_motifs]
        report["byte_fold_families"] = [{"targets": len(r.targets), "gain": r.gain} for r in byte_fold_families]
        report["mutation_chain_families"] = [
            {"targets": len(r.targets), "gain": r.gain, "avg_changed_lines": (r.unfold_recipe or {}).get("avg_changed_lines")}
            for r in mutation_chain_families
        ]
        report["fold_echo_families"] = [
            {"targets": len(r.targets), "gain": r.gain, "host_operator": (r.unfold_recipe or {}).get("host_operator")}
            for r in fold_echo_families
        ]
        report["per_operator_gain_share"] = per_operator_gain
        report["fold_profile"] = config.get("_fold_profile_info", {}).get("fold_profile")
        report["fold_profile_mode"] = config.get("_fold_profile_info", {}).get("fold_profile_mode")
        report["fold_profile_reason"] = config.get("_fold_profile_info", {}).get("fold_profile_reason")
    if micro:
        report["rejected_candidates_count"] = len(getattr(result, "rejected_candidates", []))
        skip_diag = config.get("_micro_skip_diagnostics", [])
        if skip_diag:
            report["micro_skip_diagnostics"] = skip_diag
    scope = config.get("_scope_metrics", {})
    if scope:
        report["scope_accounting"] = {
            "source_file_count": scope.get("source_file_count"),
            "source_bytes": scope.get("source_bytes"),
            "included_file_count": scope.get("included_file_count"),
            "included_bytes": scope.get("included_bytes"),
            "excluded_file_count": scope.get("excluded_file_count"),
            "excluded_bytes": scope.get("excluded_bytes"),
        }
    if not lean:
        report["candidate_counts"] = getattr(result, "candidate_counts", {})
        report["template_skeleton_metrics"] = template_metrics if template_metrics else None
        report["symbol_table_metrics"] = symbol_table_metrics if symbol_table_metrics else None
        report["hierarchy_metrics"] = hierarchy_metrics if hierarchy_metrics else None
        report["dependency_metrics"] = dependency_metrics if dependency_metrics else None
        report["byte_fold_metrics"] = byte_fold_metrics if byte_fold_metrics else None
        report["mutation_chain_metrics"] = mutation_chain_metrics if mutation_chain_metrics else None
        report["fold_echo_metrics"] = fold_echo_metrics if fold_echo_metrics else None
        # Phase 19A: Structural Microscope
        micro_assisted = config.get("_microscope_assisted", [])
        if micro_assisted:
            report["microscope_metrics"] = {
                "assisted_matches": len(micro_assisted),
                "operators_benefited": list({m.get("operator", "?") for m in micro_assisted}),
            }
            report["microscope_assisted"] = micro_assisted
        # Phase 18A/18B: Anchor files (metadata, context compression, operator guidance)
        if config.get("operators", {}).get("anchor_file", {}).get("enabled", True):
            try:
                from infold.engine.anchor_file import find_anchor_files, build_path_to_anchor_scopes
                anchors = find_anchor_files(sheet, sheet.source_path)
                if anchors:
                    anchor_metrics["anchors_detected"] = len(anchors)
                    anchor_metrics["anchor_types"] = list({a["anchor_type"] for a in anchors})
                    anchor_metrics["total_anchored_count"] = sum(a["anchored_count"] for a in anchors)
                    anchor_metrics["metadata_reduction_estimate"] = sum(a["metadata_reduction_estimate"] for a in anchors)
                    # Phase 18B: operator guidance usage
                    anc_mc = mutation_chain_metrics.get("anchor_assisted_count", 0) if mutation_chain_metrics else 0
                    if anc_mc:
                        anchor_metrics["anchor_assisted_mutation_chain"] = anc_mc
                    report["anchor_metrics"] = anchor_metrics
                    report["anchor_families"] = [{"path": a["path"], "type": a["anchor_type"], "anchored_count": a["anchored_count"]} for a in anchors]
            except Exception:
                pass
        report["template_families"] = [
            {
                "targets": len(r.targets),
                "gain": r.gain,
                "file_count": (r.unfold_recipe or {}).get("file_count", len(r.targets)),
                "scaffold_similarity": (r.unfold_recipe or {}).get("scaffold_similarity"),
                "slot_ratio": (r.unfold_recipe or {}).get("slot_ratio"),
                "family_purity": (r.unfold_recipe or {}).get("family_purity"),
                "reject_reason": None,
            }
            for r in template_families
        ]
        report["template_rejected_families"] = config.get("_run_diagnostics", {}).get("template_rejected", [])
        report["template_thresholds"] = config.get("thresholds", {}).get("template_skeleton", {})
        report["rejected_candidates_summary"] = getattr(result, "rejected_candidates", [])
        report["symbol_table_conflict_blocked"] = [
            rc for rc in getattr(result, "rejected_candidates", [])
            if rc.get("operator_id") == "symbol_table"
            and (rc.get("reason") == "conflict" or rc.get("planner_decision") == "reject_conflict")
        ]
        report["planner_decisions"] = getattr(result, "planner_decisions", [])
        report["interaction_diagnostics"] = _interaction_diagnostics_to_dict(
            getattr(result, "interaction_diagnostics", None)
        )
        report["byte_fold_routing"] = config.get("_run_diagnostics", {}).get("byte_fold_routing")
        report["fold_profile_factors"] = config.get("_fold_profile_info", {}).get("fold_profile_factors")
        ci = config.get("_fold_creature_info")
        if ci:
            report["fold_species"] = ci.get("fold_species")
            report["fold_species_mode"] = ci.get("fold_species_mode")
            report["fold_species_reason"] = ci.get("fold_species_reason")
            report["fold_creature_signals"] = ci.get("fold_creature_signals")
            report["fold_creature_traits_initial"] = ci.get("fold_creature_traits_initial")
            report["fold_creature_traits_final"] = ci.get("fold_creature_traits_final")
            report["fold_creature_adapt_reasons"] = ci.get("fold_creature_adapt_reasons")
            report["fold_creature_behavior_changes"] = ci.get("fold_creature_behavior_changes")
        report["tesseract_planner"] = config.get("_tesseract_planner_info")
        report["tesseract_execution_plan"] = config.get("_tesseract_execution_plan")
    elif lean:
        report["rejected_candidates_count"] = len(getattr(result, "rejected_candidates", []))
    return report


def _interaction_diagnostics_to_dict(diag: Any) -> dict[str, Any] | None:
    """Convert InteractionDiagnostics to report-serializable dict."""
    if diag is None:
        return None
    return {
        "blocked_folds": getattr(diag, "blocked_folds", []),
        "superseded_folds": getattr(diag, "superseded_folds", []),
        "reused_artifacts": getattr(diag, "reused_artifacts", []),
    }


def report_to_json(result: FoldResult, config: dict[str, Any]) -> str:
    """Export report as JSON string."""
    report = build_report(result, config)
    return json.dumps(report, indent=2)


def report_to_text(result: FoldResult, config: dict[str, Any]) -> str:
    """Export report as human-readable text."""
    report = build_report(result, config)
    lines = [
        "Infold Report",
        "=============",
        "",
        f"Project: {report['source_path']}",
        f"Files: {report['file_count']}",
        f"Folders: {report['folder_count']}",
        f"Original size (raw): {report['original_size_bytes']:,} bytes",
        "",
    ]
    fp = report.get("fold_profile")
    if fp:
        mode = report.get("fold_profile_mode", "?")
        reason = report.get("fold_profile_reason", "?")
        lines.append(f"Fold profile: {fp} ({mode}, reason={reason})")
        factors = report.get("fold_profile_factors", {})
        if factors.get("scores"):
            lines.append(f"  Profile scores: {factors.get('scores', {})}")
        if factors.get("selected_score") is not None:
            lines.append(f"  Selected score: {factors.get('selected_score')}")
        if factors.get("score_breakdown"):
            lines.append(f"  Score breakdown: {factors.get('score_breakdown')}")
        lines.append("")
    ci = report.get("fold_species")
    if ci:
        lines.append(f"Fold species: {report.get('fold_species', '?')} ({report.get('fold_species_mode', '?')}, {report.get('fold_species_reason', '?')})")
        adapt = report.get("fold_creature_adapt_reasons") or []
        if adapt:
            lines.append("  Adaptation reasons:")
            for r in adapt:
                lines.append(f"    - {r}")
        traits_f = report.get("fold_creature_traits_final") or {}
        if traits_f:
            lines.append(f"  Final traits: {traits_f}")
        changes = report.get("fold_creature_behavior_changes") or []
        if changes:
            lines.append("  Trait shifts:")
            for c in changes:
                lines.append(f"    - {c}")
        lines.append("")
    tp = report.get("tesseract_planner")
    if tp:
        from infold.tesseract.planner import tesseract_planner_summary
        lines.append(f"Tesseract Planner: {tesseract_planner_summary(tp)}")
        lines.append(f"  route={tp.get('route')} ({tp.get('route_reason')})")
        lines.append(f"  dominant={tp.get('dominant_dimension')} secondary={tp.get('secondary_dimension')}")
        lines.append(f"  operator_priority={'>'.join(tp.get('operator_family_priority', []))}")
        bias = tp.get("planner_bias", {})
        active = [f"{k}={v}" for k, v in bias.items() if v and v > 0]
        if active:
            lines.append(f"  planner_bias={', '.join(active)}")
        lines.append("")
    ep = report.get("tesseract_execution_plan")
    if ep:
        from infold.tesseract.execution_plan import execution_plan_summary
        lines.append(f"Tesseract Execution Plan: {execution_plan_summary(ep)}")
        lines.append(f"  steps={'->'.join(ep.get('execution_steps', []))}")
        lines.append(f"  cooperation_mode={ep.get('cooperation_mode')}")
        lines.append(f"  plan_reason={ep.get('plan_reason')}")
        lines.append("")
    lines.extend([
        f"Folds: {report['fold_count']}",
        f"Logical gain (bytes saved): {report['total_bytes_saved']:,}",
        f"Physical folded size: {report.get('physical_folded_size_bytes', report['original_size_bytes'] - report['total_bytes_saved']):,} bytes",
        f"Exact reconstruction: {'OK' if report.get('exact_reconstruction_ok', True) else 'FAILED'}",
        "",
        "Candidate counts:",
    ])
    for op_id, count in report.get("candidate_counts", {}).items():
        lines.append(f"  {op_id}: {count}")
    lines.append("")
    lines.append("Operator breakdown:")
    for op_id, data in report["operator_breakdown"].items():
        lines.append(f"  {op_id}: {data['count']} folds, {data['gain']} bytes saved, {data['targets']} targets")
    lines.append("")
    lines.append("Families:")
    for name, fams in [
        ("duplicate_families", report.get("duplicate_families", [])),
        ("template_families", report.get("template_families", [])),
        ("shared_symbol_families", report.get("shared_symbol_families", [])),
        ("hierarchy_templates", report.get("hierarchy_templates", [])),
        ("dependency_motifs", report.get("dependency_motifs", [])),
        ("byte_fold_families", report.get("byte_fold_families", [])),
    ]:
        if fams:
            lines.append(f"  {name}: {len(fams)} families")
            for i, f in enumerate(fams[:5]):
                lines.append(f"    [{i}] targets={f.get('targets', 0)}, gain={f.get('gain', 0)}")
            if len(fams) > 5:
                lines.append(f"    ... and {len(fams) - 5} more")
    if report.get("template_skeleton_metrics"):
        tm = report["template_skeleton_metrics"]
        lines.append("")
        lines.append("Template Skeleton metrics:")
        lines.append(f"  families_found: {tm.get('families_found', 0)}")
        if tm.get("files_per_family"):
            lines.append(f"  files_per_family: {tm['files_per_family']}")
        if tm.get("scaffold_lengths"):
            lines.append(f"  scaffold_lengths: {tm['scaffold_lengths']}")
        if tm.get("slot_counts"):
            lines.append(f"  slot_counts: {tm['slot_counts']}")
        if tm.get("family_purity"):
            lines.append(f"  family_purity: {tm['family_purity']}")
        if tm.get("slot_ambiguity"):
            lines.append(f"  slot_ambiguity: {tm['slot_ambiguity']}")
        if tm.get("avg_slot_size"):
            lines.append(f"  avg_slot_size: {tm['avg_slot_size']}")
    rejected_tm = report.get("template_rejected_families", [])
    tm_thresh = report.get("template_thresholds", {})
    if rejected_tm or tm_thresh:
        lines.append("")
        if tm_thresh:
            lines.append("Template thresholds (min_family_size, min_scaffold_similarity, max_slot_ratio):")
            lines.append(f"  {tm_thresh.get('min_family_size', '?')}, {tm_thresh.get('min_scaffold_similarity', '?')}, {tm_thresh.get('max_slot_ratio', '?')}")
        if rejected_tm:
            lines.append("Template rejected families (diagnostics):")
            for i, r in enumerate(rejected_tm[:10]):
                lines.append(
                    f"  [{i}] file_count={r.get('file_count')} "
                    f"scaffold_sim={r.get('scaffold_similarity')} slot_ratio={r.get('slot_ratio')} "
                    f"purity={r.get('family_purity')} -> {r.get('reject_reason', '?')}"
                )
            if len(rejected_tm) > 10:
                lines.append(f"  ... and {len(rejected_tm) - 10} more")
    if report.get("symbol_table_metrics"):
        stm = report["symbol_table_metrics"]
        lines.append("")
        lines.append("Symbol Table metrics:")
        lines.append(f"  shared_symbol_count: {stm.get('shared_symbol_count', 0)}")
        lines.append(f"  net_bytes_saved: {stm.get('net_bytes_saved', 0)}")
        if stm.get("symbol_reuse_ratio"):
            lines.append(f"  symbol_reuse_ratio: {stm['symbol_reuse_ratio']}")
    if report.get("hierarchy_metrics"):
        hm = report["hierarchy_metrics"]
        lines.append("")
        lines.append("Hierarchy Mirror metrics:")
        lines.append(f"  hierarchy_templates_found: {hm.get('hierarchy_templates_found', 0)}")
        lines.append(f"  instances_per_template: {hm.get('instances_per_template', [])}")
        lines.append(f"  net_bytes_saved: {hm.get('net_bytes_saved', 0)}")
        lines.append(f"  structural_reuse_ratio: {hm.get('structural_reuse_ratio', [])}")
        lines.append(f"  path_reconstruction_accuracy: {hm.get('path_reconstruction_accuracy', [])}")
        lines.append(f"  file_membership_accuracy: {hm.get('file_membership_accuracy', [])}")
    if report.get("dependency_metrics"):
        dm = report["dependency_metrics"]
        lines.append("")
        lines.append("Dependency Motif metrics:")
        lines.append(f"  dependency_motifs_found: {dm.get('dependency_motifs_found', 0)}")
        lines.append(f"  motif_instances_per_family: {dm.get('motif_instances_per_family', [])}")
        lines.append(f"  average_motif_size: {dm.get('average_motif_size', [])}")
        lines.append(f"  net_bytes_saved: {dm.get('net_bytes_saved', 0)}")
        lines.append(f"  dependency_recovery_accuracy: {dm.get('dependency_recovery_accuracy', [])}")
        lines.append(f"  structural_reuse_ratio: {dm.get('structural_reuse_ratio', [])}")
    if report.get("byte_fold_metrics"):
        bfm = report["byte_fold_metrics"]
        lines.append("")
        lines.append("Byte Fold metrics:")
        lines.append(f"  files_chunk_folded: {bfm.get('files_chunk_folded', 0)}")
        lines.append(f"  unique_chunk_count: {bfm.get('unique_chunk_count', 0)}")
        lines.append(f"  reused_chunk_count: {bfm.get('reused_chunk_count', 0)}")
        lines.append(f"  chunk_reused_bytes: {bfm.get('chunk_reused_bytes', 0)}")
        lines.append(f"  chunk_dictionary_size_bytes: {bfm.get('chunk_dictionary_size_bytes', 0)}")
        lines.append(f"  net_bytes_saved: {bfm.get('net_bytes_saved', 0)}")
        if bfm.get("chunk_reuse_ratio"):
            lines.append(f"  chunk_reuse_ratio: {bfm['chunk_reuse_ratio']}")
        if bfm.get("chunk_folded_paths"):
            paths = bfm["chunk_folded_paths"][:10]
            lines.append(f"  chunk_folded_paths (sample): {paths}")
            if len(bfm["chunk_folded_paths"]) > 10:
                lines.append(f"    ... and {len(bfm['chunk_folded_paths']) - 10} more")
    if report.get("byte_fold_routing"):
        lines.append("")
        lines.append("Byte Fold routing (by file):")
        routing = report["byte_fold_routing"]
        by_route: dict[str, list[str]] = {}
        for path, info in routing.items():
            route = info.get("route", "?")
            by_route.setdefault(route, []).append(f"{path} ({info.get('reason', '')})")
        for route in ("chunk_first", "structural_first", "passthrough_only", "low_value"):
            if route in by_route:
                lines.append(f"  {route}: {len(by_route[route])} files")
                for item in by_route[route][:5]:
                    lines.append(f"    {item}")
                if len(by_route[route]) > 5:
                    lines.append(f"    ... and {len(by_route[route]) - 5} more")
    lines.append("")
    lines.append("Per-operator gain share:")
    for op_id, share in report.get("per_operator_gain_share", {}).items():
        lines.append(f"  {op_id}: {share:.1%}")
    lines.append("")
    lines.append("Rejected candidates summary:")
    conflict_blocked: list[dict] = []
    for rc in report.get("rejected_candidates_summary", [])[:20]:
        decision = rc.get("planner_decision", rc.get("reason", "?"))
        lines.append(f"  {rc.get('operator_id', '?')}: {decision} - {rc.get('detail', '')[:50]}")
        if rc.get("reason") == "conflict" and rc.get("operator_id") == "symbol_table":
            conflict_blocked.append(rc)
    if conflict_blocked:
        lines.append("")
        lines.append("Symbol table conflict (blocked by earlier content folds):")
        lines.append("  symbol_table targets whole-project files; paths already folded by exact_repetition/template_skeleton are excluded.")
        lines.append("  Blocking is correct: content operators cannot double-fold the same file content.")
        for cb in conflict_blocked[:3]:
            lines.append(f"  Blocked paths (sample): {cb.get('detail', '')[:70]}")
    if len(report.get("rejected_candidates_summary", [])) > 20:
        lines.append(f"  ... and {len(report['rejected_candidates_summary']) - 20} more")
    planner = report.get("planner_decisions", [])
    if planner:
        accept_count = sum(1 for p in planner if p.get("planner_decision") == "accept")
        lines.append("")
        lines.append("Planner decisions:")
        lines.append(f"  total: {len(planner)}, accepted: {accept_count}")
        for pd in planner[:5]:
            lines.append(f"  {pd.get('operator_id', '?')}: {pd.get('planner_decision', '?')} net={pd.get('final_net_value', 0):.3f}")
        if len(planner) > 5:
            lines.append(f"  ... and {len(planner) - 5} more")
    diag = report.get("interaction_diagnostics")
    if diag and (diag.get("blocked_folds") or diag.get("superseded_folds") or diag.get("reused_artifacts")):
        lines.append("")
        lines.append("Interaction diagnostics:")
        for b in diag.get("blocked_folds", [])[:5]:
            lines.append(f"  blocked: {b.get('operator_id', '?')} - {b.get('detail', '')[:50]}")
        for s in diag.get("superseded_folds", [])[:5]:
            lines.append(f"  superseded: {s.get('operator_id', '?')}")
        for r in diag.get("reused_artifacts", [])[:5]:
            lines.append(f"  reused: {r.get('operator_id', '?')}")
    lines.extend(["", f"Reconstruction: {report['reconstruction_status']}"])
    if report["errors"]:
        lines.append("Errors:")
        for e in report["errors"]:
            lines.append(f"  - {e}")
    return "\n".join(lines)


def export_report(
    result: FoldResult,
    config: dict[str, Any],
    output_dir: Path | str | None = None,
) -> tuple[str, str]:
    """
    Export JSON and text reports. Returns (json_path, text_path).
    Creates output_dir if needed. Uses config report.output_dir if not specified.
    """
    out = output_dir or config.get("report", {}).get("output_dir", "infold_reports")
    out_path = Path(out)
    out_path.mkdir(parents=True, exist_ok=True)

    json_path = out_path / "report.json"
    text_path = out_path / "report.txt"

    if config.get("report", {}).get("export_json", True):
        json_path.write_text(report_to_json(result, config), encoding="utf-8")
    if config.get("report", {}).get("export_text", True):
        text_path.write_text(report_to_text(result, config), encoding="utf-8")

    return str(json_path), str(text_path)
