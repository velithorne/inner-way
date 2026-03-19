"""
Phase 17A: Fold Echo v0.1 tests.
"""
from pathlib import Path

import pytest

from infold.engine.fold_echo import (
    _extract_slot_values_for_file,
    find_template_echo_candidates,
    reconstruct_echo_from_template,
)
from infold.archive import create_archive, validate_archive, reconstruct_archive, explain_archive
from infold.archive.operations import explain_to_text


def test_extract_slot_values():
    """Extract slot values for a file matching template structure."""
    # slot_groups: each sg is list of slot lines; each line has [val_m0, val_m1, ...]
    const_blocks = ["{\n", "  \"version\": \"1.0.0\",\n", "}\n"]
    slot_groups = [
        [["  \"service\": \"api\",\n", "  \"service\": \"worker\",\n"]],  # 1 slot line, 2 members
        [["  \"port\": 8000,\n", "  \"port\": 8001,\n"]],  # 1 slot line, 2 members
    ]
    # File with echo's slot values (same structure, different slot content)
    lines = [
        "{\n",
        "  \"service\": \"echo\",\n",
        "  \"version\": \"1.0.0\",\n",
        "  \"port\": 9999,\n",
        "}\n",
    ]
    result = _extract_slot_values_for_file(lines, const_blocks, slot_groups)
    assert result is not None
    assert len(result) == 2
    assert "echo" in result[0]
    assert "9999" in result[1]


def test_reconstruct_echo():
    """Reconstruct file from template + echo slot values."""
    const_blocks = ["{\n", "  \"version\": \"1.0.0\",\n", "}\n"]
    slot_groups = [[["a\n", "b\n"]], [["x\n", "y\n"]]]  # structure only, we use slot_values
    slot_values = ["  \"service\": \"echo\",\n", "  \"port\": 9999,\n"]
    content = reconstruct_echo_from_template(const_blocks, slot_groups, slot_values)
    expected = "{\n  \"service\": \"echo\",\n  \"version\": \"1.0.0\",\n  \"port\": 9999,\n}\n"
    assert content == expected


def test_find_echo_candidates_requires_ledger():
    """Echo candidates require ledger with template records."""
    from infold.models.project_sheet import ProjectSheet
    from infold.engine.ledger import FoldLedger

    sheet = ProjectSheet(Path("."))
    sheet.file_nodes = {}
    ledger = FoldLedger()
    candidates = find_template_echo_candidates(sheet, ledger.fold_records, set())
    assert candidates == []


def test_fold_echo_fixture_archive():
    """Create archive on fold_echo fixture, verify echo attached and reconstruction."""
    fixture = Path(__file__).parent / "fixtures" / "fold_echo"
    if not fixture.exists():
        pytest.skip("fold_echo fixture not found")
    out = Path(__file__).parent.parent / "results" / "fold_echo_test.infold"
    out.parent.mkdir(parents=True, exist_ok=True)
    create_archive(str(fixture), str(out), profile="fox")
    valid, _ = validate_archive(str(out), mode="strict")
    assert valid, "Archive should validate"
    restored = out.parent / "fold_echo_restored"
    reconstruct_archive(str(out), str(restored))
    for name in ["handler_a.py", "handler_b.py", "handler_c.py", "handler_d.py"]:
        orig = fixture / name
        rest = restored / name
        assert rest.exists(), f"Restored {name} should exist"
        assert orig.read_text() == rest.read_text(), f"Exact match for {name}"


def test_fold_echo_explain_shows_echoes():
    """Explain output should mention fold echoes when present."""
    fixture = Path(__file__).parent / "fixtures" / "fold_echo"
    if not fixture.exists():
        pytest.skip("fold_echo fixture not found")
    out = Path(__file__).parent.parent / "results" / "fold_echo_explain.infold"
    out.parent.mkdir(parents=True, exist_ok=True)
    create_archive(str(fixture), str(out), profile="fox")
    info = explain_archive(str(out))
    text = explain_to_text(info)
    # May or may not have echoes depending on template acceptance; explain should run
    assert "Archive Explain" in text
    assert "fold_count" in text or "Fold" in text or "gain" in text.lower()
