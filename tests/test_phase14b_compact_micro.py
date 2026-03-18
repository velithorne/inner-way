"""Phase 14B: Compact micro encoding tests."""

import json
from pathlib import Path

import pytest

from infold.engine.compact_micro import (
    decode_manifest_micro,
    decode_reconstruction_micro,
    decode_inventory_micro,
    decode_ledger_micro,
    decode_report_micro,
    encode_manifest_micro,
    encode_reconstruction_micro,
    encode_inventory_micro,
    encode_ledger_micro,
    encode_report_micro,
)


def test_compact_manifest_encode_decode():
    """Compact manifest encoding is reversible."""
    manifest = {
        "version": "1.0",
        "package_spec": "1.0",
        "compatibility": {"spec_version": "1.0", "min_infold_version": "0.2.0", "min_reader_version": "0.2.0"},
        "source_path": "/tmp/x",
        "created": "2026-01-01T00:00:00",
        "file_count": 5,
        "fold_count": 1,
        "logical_gain_bytes": 100,
        "physical_folded_size_bytes": 900,
        "fold_profile": "golem",
        "creature_enabled": False,
        "tesseract_planner_enabled": False,
        "tesseract_cooperation_enabled": False,
    }
    encoded = encode_manifest_micro(manifest)
    assert "_m" in encoded
    assert "v" in encoded
    decoded = decode_manifest_micro(encoded)
    assert decoded.get("version") == "1.0"
    assert decoded.get("compatibility", {}).get("reconstruction_mode") == "deterministic"
    assert decoded.get("fold_count") == 1


def test_compact_reconstruction_encode_decode():
    """Compact reconstruction encoding is reversible."""
    data = {
        "records": [{"index": 0, "gain": 127, "targets_refs": [2, 1, 0], "o": 0}],
        "path_table_ref": True,
        "operator_ids": ["template_skeleton"],
        "family_membranes": True,
    }
    encoded = encode_reconstruction_micro(data)
    assert "r" in encoded
    decoded = decode_reconstruction_micro(encoded)
    assert len(decoded["records"]) == 1
    assert decoded["records"][0]["operator_id"] == "template_skeleton"
    assert decoded["records"][0]["targets_refs"] == [2, 1, 0]


def test_compact_inventory_encode_decode():
    """Compact inventory encoding is reversible."""
    data = {"files": [{"path_ref": 0, "size": 100}, {"path_ref": 1, "size": 200}], "path_table_ref": True}
    encoded = encode_inventory_micro(data)
    assert "f" in encoded
    decoded = decode_inventory_micro(encoded)
    assert len(decoded["files"]) == 2
    assert decoded["files"][0]["path_ref"] == 0 and decoded["files"][0]["size"] == 100


def test_micro_archive_compact_exact_reconstruction(tmp_path):
    """Micro archive with Phase 14B compact encoding preserves exact reconstruction."""
    from infold.archive import create_archive, reconstruct_archive

    fixture = Path(__file__).parent / "fixtures" / "template_heavy"
    if not fixture.exists():
        pytest.skip("template_heavy fixture not found")
    out = tmp_path / "test.infold"
    create_archive(fixture, out, {"_micro_mode": True, "_lean_mode": True})
    restored = tmp_path / "restored"
    reconstruct_archive(out, restored)
    for f in fixture.rglob("*"):
        if f.is_file():
            rel = f.relative_to(fixture)
            rest = restored / rel
            assert rest.exists(), f"Missing {rel}"
            assert rest.read_bytes() == f.read_bytes(), f"Mismatch {rel}"


def test_micro_archive_validate_strict(tmp_path):
    """Micro archive with compact encoding passes strict validation."""
    from infold.archive import create_archive, validate_archive

    fixture = Path(__file__).parent / "fixtures" / "template_heavy"
    if not fixture.exists():
        pytest.skip("template_heavy fixture not found")
    out = tmp_path / "test.infold"
    create_archive(fixture, out, {"_micro_mode": True, "_lean_mode": True})
    ok, errors = validate_archive(out, mode="strict")
    assert ok, errors


def test_compact_ledger_encode_decode():
    """Phase 14D: Compact ledger encoding is reversible."""
    ledger = {
        "version": "1.0",
        "total_folds": 3,
        "total_bytes_saved": 300,
        "fold_records": [{"gain": 100}, {"gain": 150}, {"gain": 50}],
    }
    encoded = encode_ledger_micro(ledger)
    assert "_l" in encoded
    assert encoded["g"] == [100, 150, 50]
    decoded = decode_ledger_micro(encoded)
    assert decoded["total_folds"] == 3
    assert decoded["total_bytes_saved"] == 300
    assert [r["gain"] for r in decoded["fold_records"]] == [100, 150, 50]


def test_compact_report_encode_decode():
    """Phase 14D: Compact report encoding is reversible."""
    report = {
        "file_count": 10,
        "fold_count": 2,
        "logical_gain_bytes": 200,
        "physical_folded_size_bytes": 800,
        "exact_reconstruction_ok": True,
        "reconstruction_status": "ok",
        "errors": [],
        "rejected_candidates_count": 1,
        "scope_accounting": {"source_file_count": 12, "included_file_count": 10},
    }
    encoded = encode_report_micro(report)
    assert "_r" in encoded
    decoded = decode_report_micro(encoded)
    assert decoded["file_count"] == 10
    assert decoded["fold_count"] == 2
    assert decoded["logical_gain_bytes"] == 200
    assert len(decoded["rejected_candidates_summary"]) == 1
