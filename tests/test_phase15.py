"""
Phase 1.5 tests: Fold Planner, interaction policy, report upgrade, package spec.
"""

import json
import shutil
from pathlib import Path

import pytest
import sys

sys.path.insert(0, str(Path(__file__).parent.parent))

from infold.engine import run_fold
from infold.engine.interaction_policy import (
    METADATA_OPERATORS,
    conflicts_with_committed,
    get_candidate_paths,
    update_committed_paths,
)
from infold.engine.planner import filter_candidates, score_candidate
from infold.engine.package_spec import (
    PACKAGE_SPEC_VERSION,
    REQUIRED_FILES,
    REQUIRED_DIRS,
    COMPATIBILITY_METADATA,
)
from infold.models import CandidateCrease, GainEstimate, StressEstimate


def test_planner_scores_candidate():
    """Planner produces deterministic scores."""
    config = {"planner": {"min_net_value": -0.5}}
    c = CandidateCrease(
        operator_id="exact_repetition",
        targets=[Path("a.py"), Path("b.py")],
        gain=GainEstimate(100, 10, 90, 0.8, 1.0),
        stress=StressEstimate(0.1, 0.2, 0.0, 0.0, 0.15),
        invariants=[],
        metadata={},
    )
    score = score_candidate(c, config)
    assert score.logical_gain == 90
    assert score.utility == 0.8
    assert score.stress == 0.15
    assert not score.skip


def test_planner_filters_low_value():
    """Planner skips candidates with net_value below threshold."""
    config = {"planner": {"min_net_value": 10.0}}
    c = CandidateCrease(
        operator_id="exact_repetition",
        targets=[Path("a.py")],
        gain=GainEstimate(5, 5, 0, 0.0, 0.5),
        stress=StressEstimate(0.9, 0.9, 0.0, 0.0, 0.9),
        invariants=[],
        metadata={},
    )
    score = score_candidate(c, config)
    assert score.skip
    accepted, rejected = filter_candidates([c], config)
    assert len(accepted) == 0
    assert len(rejected) == 1
    assert rejected[0][1] is not None


def test_interaction_conflict():
    """Conflict detection rejects content operators targeting already-folded paths."""
    c = CandidateCrease(
        operator_id="template_skeleton",
        targets=[Path("a.py"), Path("b.py")],
        gain=GainEstimate(50, 5, 45, 0.7, 1.0),
        stress=StressEstimate(0.1, 0.1, 0.0, 0.0, 0.1),
        invariants=[],
        metadata={},
    )
    committed = {"a.py", "x.py"}
    conflict, reason = conflicts_with_committed(c, committed)
    assert conflict
    assert "a.py" in (reason or "")


def test_interaction_no_conflict_metadata():
    """Metadata operators do not conflict."""
    c = CandidateCrease(
        operator_id="hierarchy_mirror",
        targets=[Path("src/a"), Path("tests/a")],
        gain=GainEstimate(20, 5, 15, 0.6, 1.0),
        stress=StressEstimate(0.0, 0.0, 0.0, 0.0, 0.0),
        invariants=[],
        metadata={},
    )
    committed = {"a.py", "b.py"}
    conflict, _ = conflicts_with_committed(c, committed)
    assert not conflict


def test_update_committed_paths():
    """Committed paths are updated for content operators only."""
    committed = set()
    c = CandidateCrease(
        operator_id="exact_repetition",
        targets=[Path("a.py"), Path("b.py")],
        gain=GainEstimate(100, 10, 90, 0.8, 1.0),
        stress=StressEstimate(0.0, 0.0, 0.0, 0.0, 0.0),
        invariants=[],
        metadata={},
    )
    update_committed_paths(committed, c, [Path("a.py"), Path("b.py")])
    assert len(committed) >= 1


def test_package_spec_constants():
    """Package spec defines required structure."""
    assert PACKAGE_SPEC_VERSION == "1.0"
    assert "manifest.json" in REQUIRED_FILES
    assert "ledger.json" in REQUIRED_FILES
    assert "shared" in REQUIRED_DIRS
    assert "maps" in REQUIRED_DIRS
    assert COMPATIBILITY_METADATA.get("reconstruction_mode") == "deterministic"


def test_report_has_families_and_rejected():
    """Report includes family sections and rejected candidates summary."""
    fixtures = Path(__file__).parent / "fixtures" / "duplicate_python"
    if not fixtures.exists():
        pytest.skip("fixtures not found")
    config_path = Path(__file__).parent.parent / "infold" / "config.json"
    with open(config_path, encoding="utf-8") as f:
        config = json.load(f)
    config["project"] = {**config.get("project", {}), "id": "test", "source_path": str(fixtures)}
    config["project"]["exclude_patterns"] = config["project"].get("exclude_patterns", []) + ["infold_sweep_report"]
    result = run_fold(fixtures, config)
    from infold.reporting import build_report

    report = build_report(result, config)
    assert "duplicate_families" in report
    assert "per_operator_gain_share" in report
    assert "rejected_candidates_summary" in report


def test_manifest_has_compatibility():
    """Exported package manifest includes compatibility metadata."""
    fixtures = Path(__file__).parent / "fixtures" / "duplicate_python"
    if not fixtures.exists():
        pytest.skip("fixtures not found")
    config_path = Path(__file__).parent.parent / "infold" / "config.json"
    with open(config_path, encoding="utf-8") as f:
        config = json.load(f)
    config["project"] = {**config.get("project", {}), "id": "test", "source_path": str(fixtures)}
    config["project"]["exclude_patterns"] = config["project"].get("exclude_patterns", []) + ["infold_sweep_report"]
    result = run_fold(fixtures, config)
    from infold.engine import export_package

    pkg = Path(__file__).parent.parent / "infold_package_test"
    export_package(result, config, pkg)
    manifest_path = pkg / "manifest.json"
    assert manifest_path.exists()
    with open(manifest_path, encoding="utf-8") as f:
        manifest = json.load(f)
    assert "compatibility" in manifest
    assert manifest["compatibility"].get("reconstruction_mode") == "deterministic"
    assert "required_files" in manifest
    assert "required_dirs" in manifest
    if pkg.exists():
        shutil.rmtree(pkg)
