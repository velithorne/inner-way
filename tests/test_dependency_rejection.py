"""Dependency Motif rejection tests."""

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


def test_reject_motif_too_small():
    """Motif with < 3 dependencies should reject."""
    path = Path(__file__).parent / "fixtures" / "dependency_reject"
    if not path.exists():
        return
    config = load_config()
    result = run_fold(path, config)
    dep_folds = [r for r in result.ledger.fold_records if r.operator_id == "dependency_motif"]
    assert len(dep_folds) == 0, "Should reject motif too small"


def test_reject_single_instance():
    """Single-instance dependency pattern should reject."""
    path = Path(__file__).parent / "fixtures" / "dependency_reject"
    if not path.exists():
        return
    config = load_config()
    result = run_fold(path, config)
    dep_folds = [r for r in result.ledger.fold_records if r.operator_id == "dependency_motif"]
    assert len(dep_folds) == 0, "Should reject single instance"


def test_reject_low_similarity():
    """Low-similarity dependency sets should reject."""
    path = Path(__file__).parent / "fixtures" / "dependency_reject"
    if not path.exists():
        return
    config = load_config()
    result = run_fold(path, config)
    dep_folds = [r for r in result.ledger.fold_records if r.operator_id == "dependency_motif"]
    assert len(dep_folds) == 0, "Should reject low similarity"


def test_accept_dependency_motif():
    """Valid repeated dependency motif should be accepted."""
    path = Path(__file__).parent / "fixtures" / "dependency_motif"
    if not path.exists():
        return
    config = load_config()
    result = run_fold(path, config)
    dep_folds = [r for r in result.ledger.fold_records if r.operator_id == "dependency_motif"]
    assert len(dep_folds) >= 1
    assert result.exact_reconstruction_ok
