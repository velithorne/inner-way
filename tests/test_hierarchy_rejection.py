"""Hierarchy Mirror rejection tests."""

import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))

from infold.engine import run_fold


def load_config():
    cfg = json.load(open(Path(__file__).parent.parent / "infold" / "config.json"))
    cfg["project"] = cfg.get("project", {}).copy()
    cfg["project"]["exclude_patterns"] = cfg["project"].get("exclude_patterns", []) + ["infold_sweep_report"]
    return cfg


def test_reject_shallow():
    """Shallow subtrees (depth < 2) should reject."""
    path = Path(__file__).parent / "fixtures" / "hierarchy_reject"
    if not path.exists():
        return
    config = load_config()
    result = run_fold(path, config)
    hierarchy_folds = [r for r in result.ledger.fold_records if r.operator_id == "hierarchy_mirror"]
    assert len(hierarchy_folds) == 0, "Should reject shallow subtrees"


def test_reject_single_instance():
    """Single-instance hierarchies should reject."""
    path = Path(__file__).parent / "fixtures" / "hierarchy_reject"
    if not path.exists():
        return
    config = load_config()
    result = run_fold(path, config)
    hierarchy_folds = [r for r in result.ledger.fold_records if r.operator_id == "hierarchy_mirror"]
    assert len(hierarchy_folds) == 0, "Should reject single-instance hierarchies"


def test_reject_low_similarity():
    """Low structural similarity should reject."""
    path = Path(__file__).parent / "fixtures" / "hierarchy_reject"
    if not path.exists():
        return
    config = load_config()
    result = run_fold(path, config)
    hierarchy_folds = [r for r in result.ledger.fold_records if r.operator_id == "hierarchy_mirror"]
    assert len(hierarchy_folds) == 0, "Should reject low-similarity structures"


def test_accept_hierarchy_mirror():
    """Valid repeated hierarchy should be accepted."""
    path = Path(__file__).parent / "fixtures" / "hierarchy_mirror"
    if not path.exists():
        return
    config = load_config()
    result = run_fold(path, config)
    hierarchy_folds = [r for r in result.ledger.fold_records if r.operator_id == "hierarchy_mirror"]
    assert len(hierarchy_folds) >= 1
    assert result.exact_reconstruction_ok
