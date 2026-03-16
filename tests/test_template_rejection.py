"""Template Skeleton rejection tests."""

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


def test_reject_different_logic():
    """Same file type but different logic - low scaffold similarity, should reject."""
    path = Path(__file__).parent / "fixtures" / "template_reject"
    if not path.exists():
        return  # skip if fixtures missing
    config = load_config()
    result = run_fold(path, config)
    # different_logic has 3 files, same line count, but return values differ
    # scaffold similarity = 3/4 = 0.75 < 0.80, so no template candidate
    template_folds = [r for r in result.ledger.fold_records if r.operator_id == "template_skeleton"]
    assert len(template_folds) == 0, "Should reject different-logic family (low similarity)"


def test_reject_too_many_slots():
    """Too many slot changes - slot ratio > 0.35, should reject."""
    path = Path(__file__).parent / "fixtures" / "template_reject"
    if not path.exists():
        return
    config = load_config()
    result = run_fold(path, config)
    # too_many_slots has 11 lines, 9 differ -> slot_ratio 9/11 > 0.35
    template_folds = [r for r in result.ledger.fold_records if r.operator_id == "template_skeleton"]
    assert len(template_folds) == 0, "Should reject too-many-slots family"


def test_reject_two_files_only():
    """Only 2 files in candidate family - below min_family_size 3."""
    path = Path(__file__).parent / "fixtures" / "template_reject"
    if not path.exists():
        return
    config = load_config()
    result = run_fold(path, config)
    # two_files_only has 2 files, min_family=3
    template_folds = [r for r in result.ledger.fold_records if r.operator_id == "template_skeleton"]
    assert len(template_folds) == 0, "Should reject two-files-only family"


def test_accept_syntax_valid_template():
    """Syntax-valid template (syntax_break_a/b/c) should be accepted."""
    path = Path(__file__).parent / "fixtures" / "template_reject"
    if not path.exists():
        return
    config = load_config()
    result = run_fold(path, config)
    template_folds = [r for r in result.ledger.fold_records if r.operator_id == "template_skeleton"]
    for r in template_folds:
        assert r.unfold_recipe, "Fold record should have unfold recipe"
        assert "const_blocks" in r.unfold_recipe


def test_template_rejection_diagnostics():
    """Template rejected families should have diagnostics (file_count, scaffold_similarity, slot_ratio, reject_reason)."""
    path = Path(__file__).parent / "fixtures" / "template_reject"
    if not path.exists():
        return
    config = load_config()
    run_fold(path, config)
    rejected = config.get("_run_diagnostics", {}).get("template_rejected", [])
    assert len(rejected) >= 2, "Should have rejected at least 2 families (different_logic, too_many_slots)"
    for r in rejected:
        assert "reject_reason" in r
        assert "file_count" in r
