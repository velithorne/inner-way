"""
Fold engine orchestration: run operators in order, validate, commit, ledger.

Runtime order: Exact Repetition, Symbol Table, Template Skeleton, Hierarchy Mirror, Dependency Motif.
Supports profiling via config["_profile"] dict.
"""

import time
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

from infold.engine.interaction_policy import (
    InteractionDiagnostics,
    conflicts_with_committed,
    record_ownership,
    update_committed_paths,
)
from infold.engine.ledger import FoldLedger
from infold.engine.planner import filter_candidates_v2
from infold.intake import scan_project
from infold.models.project_sheet import ProjectSheet
from infold.operators.base import BaseOperator
from infold.operators.byte_fold import ByteFoldOperator
from infold.operators.exact_repetition import ExactRepetitionOperator
from infold.operators.symbol_table import SymbolTableOperator
from infold.operators.dependency_motif import DependencyMotifOperator
from infold.operators.hierarchy_mirror import HierarchyMirrorOperator
from infold.operators.template_skeleton import TemplateSkeletonOperator
from infold.parsers import parse_project
from infold.validation import validate_candidate


@dataclass
class FoldResult:
    """Result of a fold run."""

    project_sheet: ProjectSheet
    ledger: FoldLedger
    folded_state: dict[str, Any] = field(default_factory=dict)  # canonical regions, etc.
    errors: list[str] = field(default_factory=list)
    candidate_counts: dict[str, int] = field(default_factory=dict)  # operator_id -> count detected
    rejected_candidates: list[dict[str, Any]] = field(default_factory=list)  # for reporting
    planner_decisions: list[dict[str, Any]] = field(default_factory=list)  # per-candidate planner outcomes
    interaction_diagnostics: InteractionDiagnostics | None = None  # blocked, superseded, reused
    exact_reconstruction_ok: bool = True  # verified byte-for-byte recovery


def _get_enabled_operators(config: dict[str, Any]) -> list[BaseOperator]:
    """Return operators that are enabled in config, in runtime order."""
    ops_config = config.get("operators", {})
    order = [
        ("exact_repetition", ExactRepetitionOperator),
        ("symbol_table", SymbolTableOperator),
        ("template_skeleton", TemplateSkeletonOperator),
        ("hierarchy_mirror", HierarchyMirrorOperator),
        ("dependency_motif", DependencyMotifOperator),
        ("byte_fold", ByteFoldOperator),
    ]
    result: list[BaseOperator] = []
    for op_id, op_class in order:
        if ops_config.get(op_id, {}).get("enabled", True):
            result.append(op_class())
    return result


def run_fold(
    source_path: Path | str,
    config: dict[str, Any],
) -> FoldResult:
    """
    Run the full fold pipeline: scan, parse, run operators, validate, commit, ledger.

    Returns FoldResult with project sheet, ledger, and folded state.
    If config["_profile"] is a dict, populates it with scan_time_s, parse_time_s,
    operator_times_s, total_fold_time_s.
    """
    source_path = Path(source_path).resolve()
    profile = config.get("_profile") if isinstance(config.get("_profile"), dict) else None
    t0_total = time.perf_counter()

    t0_scan = time.perf_counter()
    sheet = scan_project(
        source_path,
        exclude_patterns=config.get("project", {}).get("exclude_patterns"),
        include_extensions=config.get("project", {}).get("include_extensions"),
    )
    if profile is not None:
        profile["scan_time_s"] = time.perf_counter() - t0_scan

    t0_parse = time.perf_counter()
    parse_project(sheet)
    if profile is not None:
        profile["parse_time_s"] = time.perf_counter() - t0_parse

    # Fold profile: resolve auto or apply manual
    fold_profile = config.get("_fold_profile", "auto")
    from infold.profiles.definitions import VALID_PROFILES, get_profile_config

    def _merge(d: dict, o: dict) -> None:
        for k, v in o.items():
            if k in d and isinstance(d[k], dict) and isinstance(v, dict):
                _merge(d[k], v)
            else:
                d[k] = v

    if fold_profile == "auto":
        from infold.profiles.selector import select_profile_auto
        selected, reason, factors = select_profile_auto(sheet, config, source_path)
        overrides = get_profile_config(selected)
        for k, v in overrides.items():
            if k not in config:
                config[k] = {}
            if isinstance(v, dict) and isinstance(config.get(k), dict):
                _merge(config[k], v)
            else:
                config[k] = v
        config["_fold_profile_info"] = {
            "fold_profile": selected,
            "fold_profile_mode": "auto",
            "fold_profile_reason": reason,
            "fold_profile_factors": factors,
        }
    elif fold_profile in VALID_PROFILES:
        overrides = get_profile_config(fold_profile)
        for k, v in overrides.items():
            if k not in config:
                config[k] = {}
            if isinstance(v, dict) and isinstance(config.get(k), dict):
                _merge(config[k], v)
            else:
                config[k] = v
        config["_fold_profile_info"] = {
            "fold_profile": fold_profile,
            "fold_profile_mode": "manual",
            "fold_profile_reason": "user_selected",
            "fold_profile_factors": {},
        }
    else:
        raise ValueError(f"Invalid profile: {fold_profile}. Valid: auto, {sorted(VALID_PROFILES)}")

    # Phase 6D: Creature adaptation (bounded deterministic trait shifts)
    if config.get("_creature_adaptive", True):
        from infold.profiles.creatures import run_creature

        pf = config.get("_fold_profile_info", {})
        species = pf.get("fold_profile", "fox")
        mode = pf.get("fold_profile_mode", "manual")
        reason = pf.get("fold_profile_reason", "user_selected")
        creature = run_creature(species, sheet, source_path, mode, reason)
        config["_fold_creature_info"] = creature
        for k, v in creature.get("fold_creature_behavior_overrides", {}).items():
            if k not in config:
                config[k] = {}
            if isinstance(v, dict) and isinstance(config.get(k), dict):
                _merge(config[k], v)
            else:
                config[k] = v
    else:
        config["_fold_creature_info"] = None

    # Phase 10B: Tesseract Planner (meta-planning from dimension strengths)
    if config.get("_tesseract_planner", True):
        from infold.profiles.creatures import compute_signals
        from infold.tesseract.planner import compute_tesseract_planner_profile

        signals = compute_signals(sheet, source_path)
        config["_tesseract_planner_info"] = compute_tesseract_planner_profile(signals)
    else:
        config["_tesseract_planner_info"] = None

    ledger = FoldLedger(
        project_id=config.get("project", {}).get("id"),
        config_snapshot=config,
    )
    config.setdefault("_run_diagnostics", {})["template_rejected"] = []
    folded_state: dict[str, Any] = {"canonicals": [], "references": []}
    errors: list[str] = []
    candidate_counts: dict[str, int] = {}
    rejected_candidates: list[dict[str, Any]] = []
    committed_paths: set[str] = set()
    committed_ownerships: list[Any] = []
    interaction_diag = InteractionDiagnostics()
    planner_decisions: list[dict[str, Any]] = []
    op_times: dict[str, dict[str, float]] = {}
    for op in _get_enabled_operators(config):
        op_id = op.operator_id()
        config["_committed_paths"] = committed_paths
        sim_apply_s = 0.0
        t0 = time.perf_counter()
        candidates = op.detect_candidates(sheet, config)
        detect_s = time.perf_counter() - t0
        candidate_counts[op_id] = len(candidates)
        accepted, decisions = filter_candidates_v2(candidates, config, committed_paths)
        for d in decisions:
            planner_decisions.append({
                "operator_id": d.candidate.operator_id,
                "planner_decision": d.planner_decision,
                "planner_reason": d.planner_reason,
                "final_net_value": d.final_net_value,
                "target_count": len(d.candidate.targets),
            })
            if d.planner_decision != "accept":
                rejected_candidates.append({
                    "operator_id": d.candidate.operator_id,
                    "reason": "planner",
                    "planner_decision": d.planner_decision,
                    "detail": d.planner_reason,
                    "target_count": len(d.candidate.targets),
                })
        sim_apply_s = 0.0
        for c in accepted:
            conflict, conflict_reason = conflicts_with_committed(c, committed_paths)
            if conflict:
                interaction_diag.blocked_folds.append({
                    "operator_id": op.operator_id(),
                    "reason": "conflict",
                    "detail": conflict_reason,
                    "target_count": len(c.targets),
                })
                rejected_candidates.append({
                    "operator_id": op.operator_id(),
                    "reason": "conflict",
                    "detail": conflict_reason,
                    "target_count": len(c.targets),
                })
                continue
            t0_val = time.perf_counter()
            vr = validate_candidate(op, c, sheet, config)
            if not vr.accepted:
                sim_apply_s += time.perf_counter() - t0_val
                errors.append(f"{op.operator_id()}: candidate rejected: {vr.hard_failures}")
                rejected_candidates.append({
                    "operator_id": op.operator_id(),
                    "reason": "validation",
                    "detail": str(vr.hard_failures),
                    "target_count": len(c.targets),
                })
                continue
            sim_apply_s += time.perf_counter() - t0_val
            try:
                t0_apply = time.perf_counter()
                record = op.apply(c, sheet, config)
                sim_apply_s += time.perf_counter() - t0_apply
                ledger.append(record)
                update_committed_paths(committed_paths, c, record.targets)
                committed_ownerships.append(record_ownership(op.operator_id(), record.targets))
                folded_state["canonicals"].append(
                    {"operator": op.operator_id(), "targets": len(record.targets), "gain": record.gain}
                )
            except Exception as e:
                sim_apply_s += time.perf_counter() - t0_apply
                errors.append(f"{op.operator_id()}: apply failed: {e}")
                rejected_candidates.append({
                    "operator_id": op.operator_id(),
                    "reason": "apply_failed",
                    "detail": str(e),
                    "target_count": len(c.targets),
                })
        if profile is not None:
            op_times[op_id] = {"detect_s": detect_s, "simulate_validate_apply_s": sim_apply_s}

    # Verify exact reconstruction for committed folds
    exact_reconstruction_ok = True
    ops_by_id = {op.operator_id(): op for op in _get_enabled_operators(config)}
    for record in ledger.fold_records:
        op = ops_by_id.get(record.operator_id)
        if not op:
            continue
        try:
            unfolded = op.unfold(record, folded_state, config)
            if unfolded is None:
                continue
            for path, content in (unfolded.items() if isinstance(unfolded, dict) else []):
                p = path if isinstance(path, Path) else Path(path)
                orig_node = sheet.file_nodes.get(p)
                if orig_node is None:
                    for k, n in sheet.file_nodes.items():
                        if str(k) == str(p) or k == p:
                            orig_node = n
                            break
                if orig_node and content != orig_node.raw_text:
                    exact_reconstruction_ok = False
                    errors.append(f"Exact reconstruction failed: {p}")
        except Exception:
            exact_reconstruction_ok = False

    if profile is not None:
        profile["operator_times_s"] = op_times
        profile["total_fold_time_s"] = time.perf_counter() - t0_total

    return FoldResult(
        project_sheet=sheet,
        ledger=ledger,
        folded_state=folded_state,
        errors=errors,
        candidate_counts=candidate_counts,
        rejected_candidates=rejected_candidates,
        planner_decisions=planner_decisions,
        interaction_diagnostics=interaction_diag,
        exact_reconstruction_ok=exact_reconstruction_ok,
    )
