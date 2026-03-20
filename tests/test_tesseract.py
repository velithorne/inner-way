"""Tests for Phase 10 Tesseract Fold v0.1."""

from pathlib import Path

import pytest

from infold.tesseract import (
    build_tesseract_signature,
    build_tesseract_signatures_from_archive,
    build_tesseract_from_lineage_entry,
    get_tesseract_for_match,
    tesseract_summary,
    TESSERACT_DIMENSIONS,
)


def test_tesseract_signature_schema():
    """Tesseract signature has required fields and dimensions_present."""
    t = build_tesseract_signature(
        family_type="duplicate",
        operator="exact_repetition",
        structure_sig="abc123",
        time_sig="def456",
    )
    assert t["family_type"] == "duplicate"
    assert t["operator"] == "exact_repetition"
    assert t["structure_sig"] == "abc123"
    assert t["time_sig"] == "def456"
    assert t["byte_sig"] is None
    assert t["metadata_sig"] is None
    assert "structure" in t["dimensions_present"]
    assert "time" in t["dimensions_present"]
    assert "byte" not in t["dimensions_present"]


def test_tesseract_signature_deterministic():
    """Same inputs produce same output."""
    t1 = build_tesseract_signature(family_type="template", operator="template_skeleton", structure_sig="x")
    t2 = build_tesseract_signature(family_type="template", operator="template_skeleton", structure_sig="x")
    assert t1 == t2


def test_tesseract_summary():
    """tesseract_summary returns concise dimension string."""
    t = build_tesseract_signature(
        family_type="byte_fold",
        operator="byte_fold",
        structure_sig="a",
        byte_sig="b",
    )
    s = tesseract_summary(t)
    assert "structural" in s
    assert "byte_reuse" in s


def test_build_tesseract_from_lineage_entry():
    """Lineage entry produces structure+time tesseract."""
    entry = {
        "first_seen_snapshot": "v1",
        "last_seen_snapshot": "v3",
        "snapshot_count": 3,
        "present_in_latest": True,
    }
    t = build_tesseract_from_lineage_entry("exact_repetition", "abc123", entry)
    assert t["operator"] == "exact_repetition"
    assert t["structure_sig"] == "abc123"
    assert "structure" in t["dimensions_present"]
    assert "time" in t["dimensions_present"]
    assert t.get("time_sig")


def test_get_tesseract_for_match():
    """Match lookup finds tesseract by operator and paths."""
    import hashlib
    import json

    match_paths = ("a.py", "b.py")
    sig = hashlib.sha256(json.dumps(match_paths).encode()).hexdigest()[:16]
    tess_list = [
        build_tesseract_signature(family_type="duplicate", operator="exact_repetition", structure_sig=sig),
    ]
    match = {"operator_id": "exact_repetition", "paths": ["a.py", "b.py"]}
    found = get_tesseract_for_match(match, tess_list)
    assert found is not None
    assert found["operator"] == "exact_repetition"
    assert found["structure_sig"] == sig


def test_tesseract_from_archive(tmp_path):
    """build_tesseract_signatures_from_archive returns list for valid archive."""
    from infold.archive.operations import create_archive
    from infold.cli import load_config

    src = tmp_path / "src"
    src.mkdir()
    (src / "a.py").write_text("x = 1\n")
    (src / "b.py").write_text("x = 1\n")
    cfg = load_config()
    archive = tmp_path / "out.infold"
    create_archive(src, archive, cfg, profile="fox")
    tess_list = build_tesseract_signatures_from_archive(archive, lineage_tracking=None)
    assert isinstance(tess_list, list)
    if tess_list:
        t = tess_list[0]
        assert "family_type" in t
        assert "operator" in t
        assert "dimensions_present" in t
        assert "structure" in t.get("dimensions_present", []) or not t.get("dimensions_present")


def test_sync_trace_has_tesseract(tmp_path):
    """sync trace includes tesseract in items."""
    from infold.sync import init_sync, create_and_add_snapshot
    from infold.sync.operations import sync_trace
    from infold.cli import load_config

    init_sync(tmp_path / ".infold-sync", source_path=str(tmp_path))
    src = tmp_path / "src"
    src.mkdir()
    (src / "a.py").write_text("x = 1\n")
    cfg = load_config()
    create_and_add_snapshot(tmp_path / ".infold-sync", src, config=cfg)
    r = sync_trace(tmp_path / ".infold-sync")
    for item in r.get("items", []):
        assert "tesseract" in item
        assert "tesseract_summary" in item
        assert "structure" in item["tesseract"].get("dimensions_present", [])


def test_search_with_tesseract(tmp_path):
    """Archive search --with-tesseract adds tesseract to fold matches when available."""
    from infold.archive.operations import create_archive, search_archive
    from infold.cli import load_config

    src = tmp_path / "src"
    src.mkdir()
    (src / "a.py").write_text("x = 1\n")
    (src / "b.py").write_text("x = 1\n")
    cfg = load_config()
    archive = tmp_path / "out.infold"
    create_archive(src, archive, cfg, profile="fox")
    r = search_archive(archive, with_tesseract=True)
    assert "query" in r
    assert r.get("query", {}).get("with_tesseract") is True
    fold_matches = [m for m in r.get("matches", []) if m.get("match_type") == "fold"]
    if fold_matches:
        has_tesseract = any("tesseract" in m or "dimensions_present" in m for m in fold_matches)
        assert has_tesseract, "expected at least one fold match with tesseract"


def test_explain_has_tesseract(tmp_path):
    """Archive explain includes tesseract summary."""
    from infold.archive.operations import create_archive, explain_archive, explain_to_text
    from infold.cli import load_config

    src = tmp_path / "src"
    src.mkdir()
    (src / "a.py").write_text("x = 1\n")
    (src / "b.py").write_text("x = 1\n")
    cfg = load_config()
    archive = tmp_path / "out.infold"
    create_archive(src, archive, cfg, profile="fox")
    info = explain_archive(archive)
    assert "tesseract_families_by_dimension" in info
    assert "tesseract_family_count" in info
    text = explain_to_text(info)
    assert "Tesseract" in text or "tesseract" in text.lower()
