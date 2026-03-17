"""Phase 2D tests: Planner v2, cross-operator, package formalization."""

import json
from pathlib import Path

import pytest
import sys

sys.path.insert(0, str(Path(__file__).parent.parent))

from infold.engine import run_fold
from infold.engine.planner import (
    score_candidate_v2,
    filter_candidates_v2,
    PlannerDecisionRecord,
    ACCEPT,
    REJECT_LOW_VALUE,
    REJECT_CONFLICT,
)
from infold.engine.interaction_policy import (
    CONTENT_OPERATORS,
    METADATA_OPERATORS,
    get_operator_class,
    get_ownership_scope,
    record_ownership,
    resolve_overlap,
)
from infold.engine.package_schema import (
    build_compatibility,
    check_manifest_ledger_consistency,
    check_logical_vs_physical,
)
from infold.models import CandidateCrease, GainEstimate, StressEstimate


def test_planner_v2_scoring():
    """Planner v2 produces PlannerDecisionRecord with all fields."""
    config = {"planner": {"min_net_value": -0.5}}
    c = CandidateCrease(
        operator_id="exact_repetition",
        targets=[Path("a.py"), Path("b.py")],
        gain=GainEstimate(100, 10, 90, 0.8, 0.95),
        stress=StressEstimate(0.1, 0.2, 0.0, 0.0, 0.15),
        invariants=[],
        metadata={},
    )
    record = score_candidate_v2(c, config)
    assert isinstance(record, PlannerDecisionRecord)
    assert record.planner_decision == ACCEPT
    assert record.logical_gain == 90
    assert record.utility == 0.8
    assert record.reconstruction_confidence == 0.95
    assert record.operator_priority_bonus > 0
    assert record.final_net_value > 0


def test_planner_v2_reject_conflict():
    """Planner v2 rejects on conflict."""
    config = {"planner": {"min_net_value": -0.5}}
    c = CandidateCrease(
        operator_id="template_skeleton",
        targets=[Path("a.py")],
        gain=GainEstimate(50, 5, 45, 0.8, 1.0),
        stress=StressEstimate(0.0, 0.0, 0.0, 0.0, 0.0),
        invariants=[],
        metadata={},
    )
    record = score_candidate_v2(c, config, committed_paths={"a.py"}, has_conflict=True)
    assert record.planner_decision == REJECT_CONFLICT


def test_planner_v2_filter_returns_decisions():
    """filter_candidates_v2 returns accepted and all decisions."""
    config = {"planner": {"min_net_value": -0.5}}
    c = CandidateCrease(
        operator_id="exact_repetition",
        targets=[Path("x.py")],
        gain=GainEstimate(100, 10, 90, 0.8, 1.0),
        stress=StressEstimate(0.0, 0.0, 0.0, 0.0, 0.0),
        invariants=[],
        metadata={},
    )
    accepted, decisions = filter_candidates_v2([c], config)
    assert len(accepted) == 1
    assert len(decisions) == 1
    assert decisions[0].planner_decision == ACCEPT


def test_operator_classes():
    """Content vs metadata operator classification."""
    assert get_operator_class("exact_repetition") == "content"
    assert get_operator_class("hierarchy_mirror") == "metadata"
    assert "exact_repetition" in CONTENT_OPERATORS
    assert "byte_fold" in CONTENT_OPERATORS
    assert "hierarchy_mirror" in METADATA_OPERATORS


def test_ownership_scope():
    """Ownership scope types."""
    assert get_ownership_scope("exact_repetition") == "family"
    assert get_ownership_scope("symbol_table") == "file"
    assert get_ownership_scope("dependency_motif") == "metadata_family"


def test_resolve_overlap():
    """Overlap resolution rejects content vs content."""
    c = CandidateCrease(
        operator_id="template_skeleton",
        targets=[Path("a.py")],
        gain=GainEstimate(50, 5, 45, 0.8, 1.0),
        stress=StressEstimate(0.0, 0.0, 0.0, 0.0, 0.0),
        invariants=[],
        metadata={},
    )
    from infold.engine.interaction_policy import FoldOwnership
    ownerships = [FoldOwnership(operator_id="exact_repetition", scope_type="family", paths={"a.py"})]
    has_overlap, reason = resolve_overlap(c, ownerships)
    assert has_overlap
    assert "a.py" in (reason or "")


def test_build_compatibility():
    """Compatibility block has required fields."""
    compat = build_compatibility()
    assert "spec_version" in compat
    assert "min_reader_version" in compat
    assert "compatibility_status" in compat
    assert "upgrade_path_available" in compat


def test_check_manifest_ledger_consistency():
    """Consistency check catches mismatches."""
    manifest = {"fold_count": 5, "logical_gain_bytes": 100}
    ledger = {"total_folds": 4, "total_bytes_saved": 100}
    errors = check_manifest_ledger_consistency(manifest, ledger)
    assert any("fold_count" in e for e in errors)


def test_check_logical_vs_physical():
    """Logical vs physical check."""
    manifest = {"original_size_bytes": 1000, "logical_gain_bytes": 100, "physical_folded_size_bytes": 900}
    errors = check_logical_vs_physical(manifest)
    assert len(errors) == 0
    manifest_bad = {"original_size_bytes": 1000, "logical_gain_bytes": 100, "physical_folded_size_bytes": 800}
    errors_bad = check_logical_vs_physical(manifest_bad)
    assert len(errors_bad) >= 1


def test_run_fold_has_planner_decisions():
    """FoldResult includes planner_decisions."""
    fixtures = Path(__file__).parent / "fixtures" / "duplicate_python"
    if not fixtures.exists():
        pytest.skip("fixtures not found")
    config_path = Path(__file__).parent.parent / "infold" / "config.json"
    with open(config_path, encoding="utf-8") as f:
        config = json.load(f)
    config["project"] = {**config.get("project", {}), "id": "test", "source_path": str(fixtures)}
    config["project"]["exclude_patterns"] = config["project"].get("exclude_patterns", []) + ["infold_sweep_report"]
    result = run_fold(fixtures, config)
    assert hasattr(result, "planner_decisions")
    assert isinstance(result.planner_decisions, list)
