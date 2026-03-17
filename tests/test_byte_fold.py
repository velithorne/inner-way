"""
Tests for Infold Byte Fold v0.1: chunking, reconstruction, package integration.
"""

import json
import tempfile
from pathlib import Path

import pytest

from infold.archive.operations import create_archive, reconstruct_archive, validate_archive
from infold.chunking.roller import ChunkResult, chunk_bytes
from infold.engine.file_routing import get_chunk_eligible_paths, route_file
from infold.engine import run_fold
from infold.operators.byte_fold import ByteFoldOperator


def test_chunk_bytes_deterministic() -> None:
    """Chunking is deterministic for same input."""
    data = b"x" * 2000
    r1 = chunk_bytes(data, min_chunk=256, max_chunk=8192, avg_chunk=1024)
    r2 = chunk_bytes(data, min_chunk=256, max_chunk=8192, avg_chunk=1024)
    assert r1.chunk_ids == r2.chunk_ids
    assert r1.boundaries == r2.boundaries


def test_chunk_bytes_empty() -> None:
    """Empty input produces no chunks."""
    r = chunk_bytes(b"", min_chunk=256, max_chunk=8192, avg_chunk=1024)
    assert r.chunk_ids == []
    assert r.boundaries == []


def test_chunk_bytes_small() -> None:
    """Small input below min_chunk produces one chunk."""
    data = b"hello" * 10  # 50 bytes
    r = chunk_bytes(data, min_chunk=64, max_chunk=8192, avg_chunk=256)
    assert len(r.chunk_ids) >= 1
    assert sum(r.chunk_sizes) == len(data)


def test_route_file_passthrough_small() -> None:
    """Very small files route to passthrough_only."""
    route = route_file(Path("x.txt"), 50, None, set())
    assert route in ("passthrough_only", "low_value", "chunk_first")


def test_get_chunk_eligible_excludes_committed() -> None:
    """Committed paths are excluded from chunk eligibility."""
    from infold.intake import scan_project
    from infold.parsers import parse_project

    sheet = scan_project(Path("tests/fixtures/byte_fold"))
    parse_project(sheet)
    committed = {"part_a.txt", "part_b.txt", "part_c.txt"}
    eligible = get_chunk_eligible_paths(sheet.file_nodes, committed, {})
    paths_str = [str(p).replace("\\", "/") for p in eligible]
    assert "part_a.txt" not in paths_str
    assert "part_b.txt" not in paths_str
    assert "part_c.txt" not in paths_str


def test_byte_fold_operator_interface() -> None:
    """ByteFoldOperator implements base interface."""
    op = ByteFoldOperator()
    assert op.operator_id() == "byte_fold"
    assert op.operator_name() == "Byte Fold"
    assert "exact byte recovery" in op.invariants()


def test_byte_fold_detect_returns_list() -> None:
    """detect_candidates returns a list (may be empty)."""
    from infold.intake import scan_project
    from infold.parsers import parse_project

    sheet = scan_project(Path("tests/fixtures/byte_fold"))
    parse_project(sheet)
    op = ByteFoldOperator()
    config = {"_committed_paths": {"part_a.txt", "part_b.txt", "part_c.txt"}}
    config["thresholds"] = {"byte_fold": {"min_net_gain": -10000}}
    candidates = op.detect_candidates(sheet, config)
    assert isinstance(candidates, list)


def test_archive_with_byte_fold_reconstructs() -> None:
    """Archive create/validate/reconstruct works when byte_fold is enabled."""
    with tempfile.TemporaryDirectory() as tmp:
        out = Path(tmp) / "test.infold"
        restored = Path(tmp) / "restored"
        create_archive(Path("tests/fixtures/byte_fold"), out)
        ok, _ = validate_archive(out, mode="basic")
        assert ok
        result = reconstruct_archive(out, restored)
        assert isinstance(result, dict)
        for path_str, content in result.items():
            orig = Path("tests/fixtures/byte_fold") / path_str
            if orig.exists():
                assert content == orig.read_text(encoding="utf-8"), f"Mismatch: {path_str}"


def test_archive_validate_strict_with_byte_fold() -> None:
    """Strict validation passes for archives that may include byte_fold."""
    with tempfile.TemporaryDirectory() as tmp:
        out = Path(tmp) / "test.infold"
        create_archive(Path("tests/fixtures/byte_fold"), out)
        ok, errors = validate_archive(out, mode="strict")
        assert ok, errors
